package com.example

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class FastBinaryStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val productsMutex = Mutex()
    private val requestsMutex = Mutex()
    private val configMutex = Mutex()
    private val distributorsMutex = Mutex()

    private val productsFile = File(appContext.filesDir, "store_products_v1.bin")
    private val requestsFile = File(appContext.filesDir, "store_requests_v1.bin")
    private val configFile = File(appContext.filesDir, "store_config_v1.bin")
    private val distributorsFile = File(appContext.filesDir, "store_distributors_v1.bin")

    @Volatile
    private var productIndex: Map<String, Product> = emptyMap()

    private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: StateFlow<List<Product>> = _productsFlow.asStateFlow()

    private val _requestsFlow = MutableStateFlow<List<PriceRequest>>(emptyList())
    val requestsFlow: StateFlow<List<PriceRequest>> = _requestsFlow.asStateFlow()

    private val _workplaceNameFlow = MutableStateFlow(DEFAULT_WORKPLACE_NAME)
    val workplaceNameFlow: StateFlow<String> = _workplaceNameFlow.asStateFlow()

    private val _distributorsFlow = MutableStateFlow<List<DistributorInvoice>>(emptyList())
    val distributorsFlow: StateFlow<List<DistributorInvoice>> = _distributorsFlow.asStateFlow()

    private val _activeDistributorIdFlow = MutableStateFlow<String?>(null)
    val activeDistributorIdFlow: StateFlow<String?> = _activeDistributorIdFlow.asStateFlow()

    private val productsWriteSignal = Channel<Unit>(Channel.CONFLATED)
    private val requestsWriteSignal = Channel<Unit>(Channel.CONFLATED)
    private val configWriteSignal = Channel<Unit>(Channel.CONFLATED)
    private val distributorsWriteSignal = Channel<Unit>(Channel.CONFLATED)

    init {
        ioScope.launch {
            coroutineScope {
                val nameDeferred = async(Dispatchers.IO) { readWorkplaceNameFromDisk() }
                val productsDeferred = async(Dispatchers.IO) { readProductsFromDisk() }
                val requestsDeferred = async(Dispatchers.IO) { readRequestsFromDisk() }
                val distributorsDeferred = async(Dispatchers.IO) { readDistributorsFromDisk() }

                _workplaceNameFlow.value = nameDeferred.await()

                val loadedProducts = productsDeferred.await()
                val initialIndex = HashMap<String, Product>(loadedProducts.size)
                for (p in loadedProducts) {
                    initialIndex[p.id] = p
                }
                productIndex = initialIndex
                _productsFlow.value = loadedProducts

                _requestsFlow.value = requestsDeferred.await()

                val (loadedDistributors, savedActiveId) = distributorsDeferred.await()
                _distributorsFlow.value = loadedDistributors
                _activeDistributorIdFlow.value = savedActiveId ?: loadedDistributors.firstOrNull()?.id
            }
        }
        startWriters()
    }

    private fun startWriters() {
        ioScope.launch {
            for (signal in productsWriteSignal) {
                delay(WRITE_DEBOUNCE_MS)
                drain(productsWriteSignal)
                val latestProducts = productsMutex.withLock { _productsFlow.value }
                writeProductsToDisk(latestProducts)
            }
        }
        ioScope.launch {
            for (signal in requestsWriteSignal) {
                delay(WRITE_DEBOUNCE_MS)
                drain(requestsWriteSignal)
                val latestRequests = requestsMutex.withLock { _requestsFlow.value }
                writeRequestsToDisk(latestRequests)
            }
        }
        ioScope.launch {
            for (signal in configWriteSignal) {
                delay(WRITE_DEBOUNCE_MS)
                drain(configWriteSignal)
                val latestName = configMutex.withLock { _workplaceNameFlow.value }
                writeWorkplaceNameToDisk(latestName)
            }
        }
        ioScope.launch {
            for (signal in distributorsWriteSignal) {
                delay(WRITE_DEBOUNCE_MS)
                drain(distributorsWriteSignal)
                val snapshot = distributorsMutex.withLock {
                    _distributorsFlow.value to _activeDistributorIdFlow.value
                }
                writeDistributorsToDisk(snapshot.first, snapshot.second)
            }
        }
    }

    private fun drain(ch: Channel<Unit>) {
        while (ch.tryReceive().isSuccess) {
            Unit
        }
    }

    suspend fun saveWorkplaceName(newName: String) {
        val cleaned = newName.trim().ifBlank { DEFAULT_WORKPLACE_NAME }
        configMutex.withLock {
            _workplaceNameFlow.value = cleaned
        }
        configWriteSignal.trySend(Unit)
    }

    suspend fun saveProduct(product: Product) {
        productsMutex.withLock {
            val current = _productsFlow.value
            val idx = indexOfProductId(current, product.id)
            val updated = if (idx >= 0) {
                val old = current[idx]
                val list = ArrayList<Product>(current)
                list[idx] = product
                if (old.sortKey != product.sortKey) {
                    list.sortWith(PRODUCT_SORT_COMPARATOR)
                }
                list
            } else {
                val list = ArrayList<Product>(current.size + 1)
                list.addAll(current)
                val rawPos = list.binarySearchBy(product.sortKey) { it.sortKey }
                val insertPos = if (rawPos < 0) -rawPos - 1 else rawPos
                list.add(insertPos.coerceIn(0, list.size), product)
                list
            }

            val newIndex = HashMap<String, Product>(productIndex.size + 1)
            newIndex.putAll(productIndex)
            newIndex[product.id] = product
            productIndex = newIndex
            _productsFlow.value = updated
        }
        productsWriteSignal.trySend(Unit)
    }

    suspend fun deleteProduct(productId: String) {
        productsMutex.withLock {
            val current = _productsFlow.value
            if (!productIndex.containsKey(productId)) return@withLock
            val updated = current.filterNot { it.id == productId }
            val newIndex = HashMap<String, Product>(productIndex)
            newIndex.remove(productId)
            productIndex = newIndex
            _productsFlow.value = updated
        }
        productsWriteSignal.trySend(Unit)
    }

    suspend fun clearAllProducts() {
        productsMutex.withLock {
            _productsFlow.value = emptyList()
            productIndex = emptyMap()
            withContext(Dispatchers.IO) {
                if (productsFile.exists()) productsFile.delete()
            }
        }
    }

    fun getProductById(id: String): Product? = productIndex[id]

    suspend fun savePriceRequest(request: PriceRequest) {
        requestsMutex.withLock {
            val current = _requestsFlow.value
            val idx = indexOfRequestId(current, request.id)
            val updated = if (idx >= 0) {
                ArrayList<PriceRequest>(current).also { it[idx] = request }
            } else {
                ArrayList<PriceRequest>(current.size + 1).apply {
                    add(request)
                    addAll(current)
                }
            }
            _requestsFlow.value = updated
        }
        requestsWriteSignal.trySend(Unit)
    }

    suspend fun deletePriceRequest(requestId: String) {
        requestsMutex.withLock {
            _requestsFlow.value = _requestsFlow.value.filterNot { it.id == requestId }
        }
        requestsWriteSignal.trySend(Unit)
    }

    suspend fun setActiveDistributorId(distributorId: String?) {
        distributorsMutex.withLock {
            _activeDistributorIdFlow.value = distributorId
        }
        distributorsWriteSignal.trySend(Unit)
    }

    suspend fun saveDistributor(distributor: DistributorInvoice) {
        distributorsMutex.withLock {
            val current = _distributorsFlow.value
            val idx = current.indexOfFirst { it.id == distributor.id }
            val normalizedDistributor = distributor.copy(
                nombre = distributor.nombre.trim().ifBlank { "Distribuidor" },
                items = distributor.sortedItems,
                updatedAt = System.currentTimeMillis()
            )
            val updated = if (idx >= 0) {
                ArrayList<DistributorInvoice>(current).also { it[idx] = normalizedDistributor }
            } else {
                ArrayList<DistributorInvoice>(current.size + 1).apply {
                    addAll(current)
                    add(normalizedDistributor)
                }
            }
            _distributorsFlow.value = updated
            if (_activeDistributorIdFlow.value == null || idx < 0) {
                _activeDistributorIdFlow.value = normalizedDistributor.id
            }
        }
        distributorsWriteSignal.trySend(Unit)
    }

    suspend fun deleteDistributor(distributorId: String) {
        distributorsMutex.withLock {
            val updated = _distributorsFlow.value.filterNot { it.id == distributorId }
            _distributorsFlow.value = updated
            if (_activeDistributorIdFlow.value == distributorId) {
                _activeDistributorIdFlow.value = updated.firstOrNull()?.id
            }
        }
        distributorsWriteSignal.trySend(Unit)
    }

    suspend fun upsertDistributorItem(distributorId: String, item: DistributorItem) {
        distributorsMutex.withLock {
            val current = _distributorsFlow.value
            val distIdx = current.indexOfFirst { it.id == distributorId }
            if (distIdx < 0) return@withLock

            val dist = current[distIdx]
            val existingItems = dist.items.toMutableList()
            val itemCode = item.codigoEfectivo
            val itemSortKey = item.sortKey

            val matchIdx = existingItems.indexOfFirst { existing ->
                existing.id == item.id ||
                    (!itemCode.isNullOrBlank() &&
                        existing.codigoEfectivo?.equals(itemCode, ignoreCase = true) == true) ||
                    existing.sortKey == itemSortKey
            }

            if (matchIdx >= 0) {
                val prev = existingItems[matchIdx]
                existingItems[matchIdx] = item.copy(
                    id = prev.id,
                    ordenFila = if (item.ordenFila > 0) item.ordenFila else prev.ordenFila,
                    precioCostoAnterior = item.precioCostoAnterior ?: prev.precioCostoNuevo.takeIf { it > 0L } ?: prev.precioCostoAnterior,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                existingItems.add(
                    item.copy(
                        ordenFila = if (item.ordenFila > 0) item.ordenFila else dist.nextRowOrder,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            existingItems.sortWith(DistributorItem.ROW_COMPARATOR)

            val updatedDist = dist.copy(
                items = existingItems,
                updatedAt = System.currentTimeMillis()
            )
            val updatedList = ArrayList<DistributorInvoice>(current).also { it[distIdx] = updatedDist }
            _distributorsFlow.value = updatedList
        }
        distributorsWriteSignal.trySend(Unit)
    }

    suspend fun deleteDistributorItem(distributorId: String, itemId: String) {
        distributorsMutex.withLock {
            val current = _distributorsFlow.value
            val distIdx = current.indexOfFirst { it.id == distributorId }
            if (distIdx < 0) return@withLock

            val dist = current[distIdx]
            val updatedItems = dist.items.filterNot { it.id == itemId }
            val updatedDist = dist.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            )
            val updatedList = ArrayList<DistributorInvoice>(current).also { it[distIdx] = updatedDist }
            _distributorsFlow.value = updatedList
        }
        distributorsWriteSignal.trySend(Unit)
    }

    suspend fun shiftDistributorItemOrder(distributorId: String, itemId: String, delta: Int) {
        distributorsMutex.withLock {
            val current = _distributorsFlow.value
            val distIdx = current.indexOfFirst { it.id == distributorId }
            if (distIdx < 0) return@withLock

            val dist = current[distIdx]
            val ordered = dist.sortedItems.toMutableList()
            val pos = ordered.indexOfFirst { it.id == itemId }
            if (pos < 0) return@withLock

            val targetPos = (pos + delta).coerceIn(0, ordered.lastIndex)
            if (targetPos == pos) {
                val singleAdjusted = ordered[pos].copy(
                    ordenFila = (ordered[pos].ordenFila + delta).coerceAtLeast(1)
                )
                ordered[pos] = singleAdjusted
            } else {
                val a = ordered[pos]
                val b = ordered[targetPos]
                val rowA = a.ordenFila
                val rowB = b.ordenFila
                if (rowA == rowB) {
                    ordered[pos] = b.copy(ordenFila = if (delta < 0) rowA else rowA + 1)
                    ordered[targetPos] = a.copy(ordenFila = if (delta < 0) (rowA - 1).coerceAtLeast(1) else rowA + 1)
                } else {
                    ordered[pos] = b.copy(ordenFila = rowA)
                    ordered[targetPos] = a.copy(ordenFila = rowB)
                }
            }

            ordered.sortWith(DistributorItem.ROW_COMPARATOR)

            val updatedDist = dist.copy(
                items = ordered,
                updatedAt = System.currentTimeMillis()
            )
            val updatedList = ArrayList<DistributorInvoice>(current).also { it[distIdx] = updatedDist }
            _distributorsFlow.value = updatedList
        }
        distributorsWriteSignal.trySend(Unit)
    }

    suspend fun syncDistributorItemsToCatalog(items: List<DistributorItem>): Int {
        if (items.isEmpty()) return 0
        var syncedCount = 0
        productsMutex.withLock {
            val current = _productsFlow.value
            val byId = LinkedHashMap<String, Product>(current.size + items.size)
            val codeToId = HashMap<String, String>(current.size + items.size)
            val normDescToId = HashMap<String, String>(current.size + items.size)

            for (p in current) {
                byId[p.id] = p
                val cleanCode = p.codigo?.trim()?.lowercase()
                if (!cleanCode.isNullOrEmpty()) {
                    codeToId[cleanCode] = p.id
                }
                normDescToId[p.sortKey] = p.id
            }

            val now = System.currentTimeMillis()
            for (dItem in items) {
                if (dItem.precioCostoNuevo <= 0L || dItem.precioVentaResultante <= 0L) continue
                val cleanCode = dItem.codigoEfectivo?.lowercase()
                val normDesc = dItem.sortKey

                val matchedId = (if (!cleanCode.isNullOrEmpty()) codeToId[cleanCode] else null)
                    ?: normDescToId[normDesc]

                val realMargin = MathUtils.calcularPorcentajeRedondeado(
                    dItem.precioCostoNuevo,
                    dItem.precioVentaResultante
                )
                val customPct = dItem.porcentajeMargen.takeIf {
                    it == 20 || it == 25 || it == 30 || it == 35 || it == 50
                }
                val finalCode = dItem.codigoEfectivo ?: matchedId?.let { byId[it]?.codigo }
                val status = if (finalCode.isNullOrBlank()) "SIN_CODIGO" else "COMPLETO"

                val targetId = matchedId ?: UUID.randomUUID().toString()
                val oldProd = byId[targetId]
                val updatedProd = Product(
                    id = targetId,
                    codigo = finalCode,
                    codigoAlternativo = oldProd?.codigoAlternativo,
                    descripcion = dItem.descripcion.trim(),
                    precioCosto = dItem.precioCostoNuevo,
                    precioVenta = dItem.precioVentaResultante,
                    porcentajePersonalizado = customPct,
                    porcentajeRealRedondeado = realMargin,
                    estadoRegistro = status,
                    notaRedondeo = oldProd?.notaRedondeo,
                    updatedAt = now
                )
                byId[targetId] = updatedProd
                if (!cleanCode.isNullOrEmpty()) {
                    codeToId[cleanCode] = targetId
                }
                normDescToId[normDesc] = targetId
                syncedCount++
            }

            if (syncedCount > 0) {
                val finalProducts = ArrayList<Product>(byId.values)
                finalProducts.sortWith(PRODUCT_SORT_COMPARATOR)
                val newIndex = HashMap<String, Product>(finalProducts.size)
                for (p in finalProducts) {
                    newIndex[p.id] = p
                }
                productIndex = newIndex
                _productsFlow.value = finalProducts
            }
        }
        if (syncedCount > 0) {
            productsWriteSignal.trySend(Unit)
        }
        return syncedCount
    }

    suspend fun exportCatalogBinary(): ByteArray = withContext(Dispatchers.IO) {
        val current = productsMutex.withLock { _productsFlow.value }
        val baos = ByteArrayOutputStream(current.size * 96 + 16)
        DataOutputStream(BufferedOutputStream(baos, BUFFER_SIZE)).use { dos ->
            writeProductsToStream(dos, current)
            dos.flush()
        }
        baos.toByteArray()
    }

    suspend fun importCatalogBinary(bytes: ByteArray, replaceAll: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        if (bytes.size < 8) {
            return@withContext Result.failure(IllegalArgumentException("El archivo binario está vacío o incompleto"))
        }
        val parsedPair = try {
            DataInputStream(ByteArrayInputStream(bytes)).use { dis ->
                readProductsFromStream(dis)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Formato de archivo de catálogo inválido", e))
        } ?: return@withContext Result.failure(
            IllegalArgumentException("El archivo seleccionado no corresponde a un respaldo de Catálogo (.bin)")
        )

        val importedList = parsedPair.first
        productsMutex.withLock {
            val finalProducts = if (replaceAll) {
                val sorted = ArrayList<Product>(importedList)
                sorted.sortWith(PRODUCT_SORT_COMPARATOR)
                sorted
            } else {
                val current = _productsFlow.value
                val byId = LinkedHashMap<String, Product>(current.size + importedList.size)
                val codeToId = HashMap<String, String>(current.size + importedList.size)

                for (p in current) {
                    byId[p.id] = p
                    val cleanCode = p.codigo?.trim()?.lowercase()
                    if (!cleanCode.isNullOrEmpty()) {
                        codeToId[cleanCode] = p.id
                    }
                }

                for (imp in importedList) {
                    val cleanCode = imp.codigo?.trim()?.lowercase()
                    val existingIdByCode = if (!cleanCode.isNullOrEmpty()) codeToId[cleanCode] else null
                    val targetId = existingIdByCode ?: if (byId.containsKey(imp.id)) imp.id else null

                    if (targetId != null) {
                        val updatedProd = imp.copy(id = targetId)
                        byId[targetId] = updatedProd
                        if (!cleanCode.isNullOrEmpty()) {
                            codeToId[cleanCode] = targetId
                        }
                    } else {
                        byId[imp.id] = imp
                        if (!cleanCode.isNullOrEmpty()) {
                            codeToId[cleanCode] = imp.id
                        }
                    }
                }

                val merged = ArrayList<Product>(byId.values)
                merged.sortWith(PRODUCT_SORT_COMPARATOR)
                merged
            }

            val newIndex = HashMap<String, Product>(finalProducts.size)
            for (p in finalProducts) {
                newIndex[p.id] = p
            }
            productIndex = newIndex
            _productsFlow.value = finalProducts
            writeProductsToDisk(finalProducts)
        }
        Result.success(importedList.size)
    }

    suspend fun exportRequestsBinary(): ByteArray = withContext(Dispatchers.IO) {
        val current = requestsMutex.withLock { _requestsFlow.value }
        val baos = ByteArrayOutputStream(current.size * 144 + 16)
        DataOutputStream(BufferedOutputStream(baos, BUFFER_SIZE)).use { dos ->
            writeRequestsToStream(dos, current)
            dos.flush()
        }
        baos.toByteArray()
    }

    suspend fun importRequestsBinary(bytes: ByteArray, replaceAll: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        if (bytes.size < 8) {
            return@withContext Result.failure(IllegalArgumentException("El archivo binario está vacío o incompleto"))
        }
        val parsedPair = try {
            DataInputStream(ByteArrayInputStream(bytes)).use { dis ->
                readRequestsFromStream(dis)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Formato de archivo de historial inválido", e))
        } ?: return@withContext Result.failure(
            IllegalArgumentException("El archivo seleccionado no corresponde a un respaldo de Historial (.bin)")
        )

        val importedList = parsedPair.first
        requestsMutex.withLock {
            val finalRequests = if (replaceAll) {
                val sorted = ArrayList<PriceRequest>(importedList)
                sorted.sortByDescending { it.timestamp }
                sorted
            } else {
                val current = _requestsFlow.value
                val byId = LinkedHashMap<String, PriceRequest>(current.size + importedList.size)
                for (r in current) {
                    byId[r.id] = r
                }
                for (imp in importedList) {
                    byId[imp.id] = imp
                }
                val merged = ArrayList<PriceRequest>(byId.values)
                merged.sortByDescending { it.timestamp }
                merged
            }

            _requestsFlow.value = finalRequests
            writeRequestsToDisk(finalRequests)
        }
        Result.success(importedList.size)
    }

    suspend fun exportDistributorsBinary(): ByteArray = withContext(Dispatchers.IO) {
        val snapshot = distributorsMutex.withLock {
            _distributorsFlow.value to _activeDistributorIdFlow.value
        }
        val baos = ByteArrayOutputStream(snapshot.first.size * 224 + 32)
        DataOutputStream(BufferedOutputStream(baos, BUFFER_SIZE)).use { dos ->
            writeDistributorsToStream(dos, snapshot.first, snapshot.second)
            dos.flush()
        }
        baos.toByteArray()
    }

    suspend fun importDistributorsBinary(bytes: ByteArray, replaceAll: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        if (bytes.size < 8) {
            return@withContext Result.failure(IllegalArgumentException("El archivo binario está vacío o incompleto"))
        }
        val parsed = try {
            DataInputStream(ByteArrayInputStream(bytes)).use { dis ->
                readDistributorsFromStream(dis)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Formato de archivo de distribuidores inválido", e))
        } ?: return@withContext Result.failure(
            IllegalArgumentException("El archivo seleccionado no corresponde a un respaldo de Distribuidores (.bin)")
        )

        val importedList = parsed.first
        var totalItemsCount = 0
        for (d in importedList) {
            totalItemsCount += d.items.size
        }

        distributorsMutex.withLock {
            val finalDistributors = if (replaceAll) {
                ArrayList<DistributorInvoice>(importedList)
            } else {
                val current = _distributorsFlow.value
                val byNameOrId = LinkedHashMap<String, DistributorInvoice>(current.size + importedList.size)
                for (d in current) {
                    byNameOrId[d.sortKey] = d
                }
                for (imp in importedList) {
                    val key = imp.sortKey
                    val existing = byNameOrId[key]
                    if (existing != null) {
                        val mergedItems = LinkedHashMap<String, DistributorItem>()
                        for (item in existing.items) {
                            mergedItems[item.sortKey] = item
                        }
                        for (impItem in imp.items) {
                            mergedItems[impItem.sortKey] = impItem
                        }
                        val sortedMerged = mergedItems.values.sortedWith(DistributorItem.ROW_COMPARATOR)
                        byNameOrId[key] = existing.copy(
                            items = sortedMerged,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        byNameOrId[key] = imp
                    }
                }
                ArrayList<DistributorInvoice>(byNameOrId.values)
            }

            _distributorsFlow.value = finalDistributors
            val currentActive = _activeDistributorIdFlow.value
            if (currentActive == null || finalDistributors.none { it.id == currentActive }) {
                _activeDistributorIdFlow.value = parsed.second
                    ?.takeIf { id -> finalDistributors.any { it.id == id } }
                    ?: finalDistributors.firstOrNull()?.id
            }
            writeDistributorsToDisk(finalDistributors, _activeDistributorIdFlow.value)
        }
        Result.success(totalItemsCount.coerceAtLeast(importedList.size))
    }

    private fun readWorkplaceNameFromDisk(): String {
        if (!configFile.exists()) return DEFAULT_WORKPLACE_NAME
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(configFile), BUFFER_SIZE)).use { dis ->
                when (dis.readInt()) {
                    MAGIC_CONFIG_V2 -> dis.readFastString().trim().ifBlank { DEFAULT_WORKPLACE_NAME }
                    MAGIC_CONFIG_V1 -> {
                        val legacy = dis.readUTF().trim().ifBlank { DEFAULT_WORKPLACE_NAME }
                        configWriteSignal.trySend(Unit)
                        legacy
                    }
                    else -> DEFAULT_WORKPLACE_NAME
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DEFAULT_WORKPLACE_NAME
        }
    }

    private fun writeWorkplaceNameToDisk(workplaceName: String) {
        atomicWrite(configFile) { dos ->
            dos.writeInt(MAGIC_CONFIG_V2)
            dos.writeFastString(workplaceName)
        }
    }

    internal fun readProductsFromStream(dis: DataInputStream): Pair<List<Product>, Boolean>? {
        val magic = dis.readInt()
        val version = when (magic) {
            MAGIC_PRODUCTS_V3 -> 3
            MAGIC_PRODUCTS_V2 -> 2
            MAGIC_PRODUCTS_V1 -> 1
            else -> return null
        }
        val count = dis.readInt().coerceIn(0, MAX_SAFE_ITEMS)
        val list = ArrayList<Product>(count)

        if (version == 3) {
            for (i in 0 until count) {
                val flags = dis.readUnsignedByte()
                val id = dis.readFastString()
                val codigo = if ((flags and PROD_FLAG_CODIGO) != 0) dis.readFastString() else null
                val codigoAlt = if ((flags and PROD_FLAG_CODIGO_ALT) != 0) dis.readFastString() else null
                val descripcion = dis.readFastString()
                val precioCosto = if ((flags and PROD_FLAG_COSTO) != 0) dis.readLong() else null
                val precioVenta = if ((flags and PROD_FLAG_VENTA) != 0) dis.readLong() else null
                val pctPersonalizado = if ((flags and PROD_FLAG_PCT_PERS) != 0) dis.readInt() else null
                val pctReal = if ((flags and PROD_FLAG_PCT_REAL) != 0) dis.readInt() else null
                val estado = dis.readFastString()
                val nota = if ((flags and PROD_FLAG_NOTA) != 0) dis.readFastString() else null
                val updatedAt = dis.readLong()
                list.add(
                    Product(
                        id = id,
                        codigo = codigo,
                        codigoAlternativo = codigoAlt,
                        descripcion = descripcion,
                        precioCosto = precioCosto,
                        precioVenta = precioVenta,
                        porcentajePersonalizado = pctPersonalizado,
                        porcentajeRealRedondeado = pctReal,
                        estadoRegistro = estado,
                        notaRedondeo = nota,
                        updatedAt = updatedAt
                    )
                )
            }
        } else {
            val isV2 = version == 2
            for (i in 0 until count) {
                val id = if (isV2) dis.readFastString() else dis.readUTF()
                val codigo = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
                val codigoAlt = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
                val descripcion = if (isV2) dis.readFastString() else dis.readUTF()
                val precioCosto = dis.readNullableLong()
                val precioVenta = dis.readNullableLong()
                val pctPersonalizado = dis.readNullableInt()
                val pctReal = dis.readNullableInt()
                val estado = if (isV2) dis.readFastString() else dis.readUTF()
                val nota = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
                val updatedAt = dis.readLong()
                list.add(
                    Product(
                        id = id,
                        codigo = codigo,
                        codigoAlternativo = codigoAlt,
                        descripcion = descripcion,
                        precioCosto = precioCosto,
                        precioVenta = precioVenta,
                        porcentajePersonalizado = pctPersonalizado,
                        porcentajeRealRedondeado = pctReal,
                        estadoRegistro = estado,
                        notaRedondeo = nota,
                        updatedAt = updatedAt
                    )
                )
            }
        }

        list.sortWith(PRODUCT_SORT_COMPARATOR)
        return list to (version < 3)
    }

    internal fun writeProductsToStream(dos: DataOutputStream, products: List<Product>) {
        dos.writeInt(MAGIC_PRODUCTS_V3)
        dos.writeInt(products.size)
        for (p in products) {
            var flags = 0
            if (p.codigo != null) flags = flags or PROD_FLAG_CODIGO
            if (p.codigoAlternativo != null) flags = flags or PROD_FLAG_CODIGO_ALT
            if (p.precioCosto != null) flags = flags or PROD_FLAG_COSTO
            if (p.precioVenta != null) flags = flags or PROD_FLAG_VENTA
            if (p.porcentajePersonalizado != null) flags = flags or PROD_FLAG_PCT_PERS
            if (p.porcentajeRealRedondeado != null) flags = flags or PROD_FLAG_PCT_REAL
            if (p.notaRedondeo != null) flags = flags or PROD_FLAG_NOTA

            dos.writeByte(flags)
            dos.writeFastString(p.id)
            if (p.codigo != null) dos.writeFastString(p.codigo)
            if (p.codigoAlternativo != null) dos.writeFastString(p.codigoAlternativo)
            dos.writeFastString(p.descripcion)
            if (p.precioCosto != null) dos.writeLong(p.precioCosto)
            if (p.precioVenta != null) dos.writeLong(p.precioVenta)
            if (p.porcentajePersonalizado != null) dos.writeInt(p.porcentajePersonalizado)
            if (p.porcentajeRealRedondeado != null) dos.writeInt(p.porcentajeRealRedondeado)
            dos.writeFastString(p.estadoRegistro)
            if (p.notaRedondeo != null) dos.writeFastString(p.notaRedondeo)
            dos.writeLong(p.updatedAt)
        }
    }

    private fun readProductsFromDisk(): List<Product> {
        if (!productsFile.exists()) return emptyList()
        return try {
            val pair = DataInputStream(BufferedInputStream(FileInputStream(productsFile), BUFFER_SIZE)).use { dis ->
                readProductsFromStream(dis)
            } ?: return emptyList()
            if (pair.second) {
                productsWriteSignal.trySend(Unit)
            }
            pair.first
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeProductsToDisk(products: List<Product>) {
        atomicWrite(productsFile) { dos ->
            writeProductsToStream(dos, products)
        }
    }

    internal fun readRequestsFromStream(dis: DataInputStream): Pair<List<PriceRequest>, Boolean>? {
        val magic = dis.readInt()
        val version = when (magic) {
            MAGIC_REQUESTS_V4 -> 4
            MAGIC_REQUESTS_V3 -> 3
            MAGIC_REQUESTS_V2 -> 2
            MAGIC_REQUESTS_V1 -> 1
            else -> return null
        }
        val count = dis.readInt().coerceIn(0, MAX_SAFE_ITEMS)
        val list = ArrayList<PriceRequest>(count)

        if (version == 4) {
            for (i in 0 until count) {
                val flags = dis.readUnsignedShort()
                val id = dis.readFastString()
                val timestamp = dis.readLong()
                val tipoId = dis.readInt()
                val titulo = dis.readFastString()
                val distId = if ((flags and REQ_FLAG_DIST_ID) != 0) dis.readFastString() else null
                val rowOrd = if ((flags and REQ_FLAG_ROW_ORD) != 0) dis.readInt() else null
                val yaReg = (flags and REQ_FLAG_YA_REG) != 0
                val effectiveCode = if ((flags and REQ_FLAG_CODIGO) != 0) dis.readFastString() else null
                val descripcion = dis.readFastString()
                val pctDeseado = if ((flags and REQ_FLAG_PCT_DESEADO) != 0) dis.readInt() else null
                val costoAnt = if ((flags and REQ_FLAG_COSTO_ANT) != 0) dis.readLong() else null
                val costoNuevo = dis.readLong()
                val ventaAnt = if ((flags and REQ_FLAG_VENTA_ANT) != 0) dis.readLong() else null
                val pctAnt = if ((flags and REQ_FLAG_PCT_ANT) != 0) dis.readInt() else null
                val ventaNueva = dis.readLong()
                val pctNuevo = dis.readInt()
                val stockAnt = if ((flags and REQ_FLAG_STOCK_ANT) != 0) dis.readFastString() else null
                val recargaStock = if ((flags and REQ_FLAG_RECARGA) != 0) dis.readFastString() else null
                val variedades = if ((flags and REQ_FLAG_VARIEDADES) != 0) dis.readFastString() else null

                list.add(
                    PriceRequest(
                        id = id,
                        timestamp = timestamp,
                        tipoId = tipoId,
                        titulo = titulo,
                        productoYaRegistrado = yaReg,
                        codigo = effectiveCode,
                        codigoProducto = effectiveCode,
                        descripcion = descripcion,
                        porcentajeDeseado = pctDeseado,
                        precioCostoAnterior = costoAnt,
                        precioCostoNuevo = costoNuevo,
                        precioVentaAnterior = ventaAnt,
                        porcentajeAnteriorRedondeado = pctAnt,
                        precioVentaNuevo = ventaNueva,
                        porcentajeNuevoRedondeado = pctNuevo,
                        stockAnterior = stockAnt,
                        recargaStock = recargaStock,
                        variedadesMismoCosto = variedades,
                        distributorId = distId,
                        ordenFilaBoleta = rowOrd
                    )
                )
            }
        } else {
            val isV2OrV3 = version >= 2
            val isV3 = version == 3
            for (i in 0 until count) {
                val id = if (isV2OrV3) dis.readFastString() else dis.readUTF()
                val timestamp = dis.readLong()
                val tipoId = dis.readInt()
                val titulo = if (isV2OrV3) dis.readFastString() else dis.readUTF()
                val distId = if (isV3) {
                    dis.readNullableFastString()
                } else {
                    if (isV2OrV3) dis.readNullableFastString() else dis.readNullableUTF()
                    null
                }
                val rowOrd = if (isV3) {
                    dis.readNullableInt()
                } else {
                    if (isV2OrV3) dis.readNullableFastString() else dis.readNullableUTF()
                    null
                }
                val yaReg = dis.readBoolean()
                val codigo = if (isV2OrV3) dis.readNullableFastString() else dis.readNullableUTF()
                val codigoProd = if (isV2OrV3) dis.readNullableFastString() else dis.readNullableUTF()
                val descripcion = if (isV2OrV3) dis.readFastString() else dis.readUTF()
                val pctDeseado = dis.readNullableInt()
                val costoAnt = dis.readNullableLong()
                val costoNuevo = dis.readLong()
                val ventaAnt = dis.readNullableLong()
                val pctAnt = dis.readNullableInt()
                val ventaNueva = dis.readLong()
                val pctNuevo = dis.readInt()
                val stockAnt = if (isV2OrV3) dis.readNullableFastString() else dis.readNullableUTF()
                val recargaStock = if (isV2OrV3) dis.readNullableFastString() else dis.readNullableUTF()
                val variedades = if (isV2OrV3) dis.readNullableFastString() else dis.readNullableUTF()
                val effectiveCode = codigo ?: codigoProd
                list.add(
                    PriceRequest(
                        id = id,
                        timestamp = timestamp,
                        tipoId = tipoId,
                        titulo = titulo,
                        productoYaRegistrado = yaReg,
                        codigo = effectiveCode,
                        codigoProducto = effectiveCode,
                        descripcion = descripcion,
                        porcentajeDeseado = pctDeseado,
                        precioCostoAnterior = costoAnt,
                        precioCostoNuevo = costoNuevo,
                        precioVentaAnterior = ventaAnt,
                        porcentajeAnteriorRedondeado = pctAnt,
                        precioVentaNuevo = ventaNueva,
                        porcentajeNuevoRedondeado = pctNuevo,
                        stockAnterior = stockAnt,
                        recargaStock = recargaStock,
                        variedadesMismoCosto = variedades,
                        distributorId = distId,
                        ordenFilaBoleta = rowOrd
                    )
                )
            }
        }
        return list to (version < 4)
    }

    internal fun writeRequestsToStream(dos: DataOutputStream, requests: List<PriceRequest>) {
        dos.writeInt(MAGIC_REQUESTS_V4)
        dos.writeInt(requests.size)
        for (r in requests) {
            val effectiveCode = r.codigoEfectivo
            var flags = 0
            if (r.distributorId != null) flags = flags or REQ_FLAG_DIST_ID
            if (r.ordenFilaBoleta != null) flags = flags or REQ_FLAG_ROW_ORD
            if (r.productoYaRegistrado) flags = flags or REQ_FLAG_YA_REG
            if (effectiveCode != null) flags = flags or REQ_FLAG_CODIGO
            if (r.porcentajeDeseado != null) flags = flags or REQ_FLAG_PCT_DESEADO
            if (r.precioCostoAnterior != null) flags = flags or REQ_FLAG_COSTO_ANT
            if (r.precioVentaAnterior != null) flags = flags or REQ_FLAG_VENTA_ANT
            if (r.porcentajeAnteriorRedondeado != null) flags = flags or REQ_FLAG_PCT_ANT
            if (r.stockAnterior != null) flags = flags or REQ_FLAG_STOCK_ANT
            if (r.recargaStock != null) flags = flags or REQ_FLAG_RECARGA
            if (r.variedadesMismoCosto != null) flags = flags or REQ_FLAG_VARIEDADES

            dos.writeShort(flags)
            dos.writeFastString(r.id)
            dos.writeLong(r.timestamp)
            dos.writeInt(r.tipoId)
            dos.writeFastString(r.titulo)
            if (r.distributorId != null) dos.writeFastString(r.distributorId)
            if (r.ordenFilaBoleta != null) dos.writeInt(r.ordenFilaBoleta)
            if (effectiveCode != null) dos.writeFastString(effectiveCode)
            dos.writeFastString(r.descripcion)
            if (r.porcentajeDeseado != null) dos.writeInt(r.porcentajeDeseado)
            if (r.precioCostoAnterior != null) dos.writeLong(r.precioCostoAnterior)
            dos.writeLong(r.precioCostoNuevo)
            if (r.precioVentaAnterior != null) dos.writeLong(r.precioVentaAnterior)
            if (r.porcentajeAnteriorRedondeado != null) dos.writeInt(r.porcentajeAnteriorRedondeado)
            dos.writeLong(r.precioVentaNuevo)
            dos.writeInt(r.porcentajeNuevoRedondeado)
            if (r.stockAnterior != null) dos.writeFastString(r.stockAnterior)
            if (r.recargaStock != null) dos.writeFastString(r.recargaStock)
            if (r.variedadesMismoCosto != null) dos.writeFastString(r.variedadesMismoCosto)
        }
    }

    private fun readRequestsFromDisk(): List<PriceRequest> {
        if (!requestsFile.exists()) return emptyList()
        return try {
            val pair = DataInputStream(BufferedInputStream(FileInputStream(requestsFile), BUFFER_SIZE)).use { dis ->
                readRequestsFromStream(dis)
            } ?: return emptyList()
            if (pair.second) {
                requestsWriteSignal.trySend(Unit)
            }
            pair.first
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeRequestsToDisk(requests: List<PriceRequest>) {
        atomicWrite(requestsFile) { dos ->
            writeRequestsToStream(dos, requests)
        }
    }

    internal fun readDistributorsFromStream(dis: DataInputStream): Pair<List<DistributorInvoice>, String?>? {
        val magic = dis.readInt()
        val version = when (magic) {
            MAGIC_DISTRIBUTORS_V3 -> 3
            MAGIC_DISTRIBUTORS_V2 -> 2
            MAGIC_DISTRIBUTORS_V1 -> 1
            else -> return null
        }
        val activeId = dis.readNullableFastString()
        val distCount = dis.readInt().coerceIn(0, 10_000)
        val distributors = ArrayList<DistributorInvoice>(distCount)
        for (i in 0 until distCount) {
            val dId = dis.readFastString()
            val dNombre = dis.readFastString()
            val dUpdatedAt = dis.readLong()
            val itemCount = dis.readInt().coerceIn(0, MAX_SAFE_ITEMS)
            val items = ArrayList<DistributorItem>(itemCount)

            if (version == 3) {
                for (j in 0 until itemCount) {
                    val flags = dis.readUnsignedByte()
                    val itemId = dis.readFastString()
                    val ordenFila = dis.readInt()
                    val tipoId = dis.readInt()
                    val titulo = dis.readFastString()
                    val codigo = if ((flags and DIST_FLAG_CODIGO) != 0) dis.readFastString() else null
                    val descripcion = dis.readFastString()
                    val pctDeseado = if ((flags and DIST_FLAG_PCT_DESEADO) != 0) dis.readInt() else null
                    val costoAnt = if ((flags and DIST_FLAG_COSTO_ANT) != 0) dis.readLong() else null
                    val costoNuevo = dis.readLong()
                    val ventaAnt = if ((flags and DIST_FLAG_VENTA_ANT) != 0) dis.readLong() else null
                    val pctAnt = if ((flags and DIST_FLAG_PCT_ANT) != 0) dis.readInt() else null
                    val ventaRes = dis.readLong()
                    val pctMargen = dis.readInt()
                    val stockAnt = if ((flags and DIST_FLAG_STOCK_ANT) != 0) dis.readFastString() else null
                    val recargaStock = if ((flags and DIST_FLAG_RECARGA) != 0) dis.readFastString() else null
                    val variedades = if ((flags and DIST_FLAG_VARIEDADES) != 0) dis.readFastString() else null
                    val itemUpdated = dis.readLong()

                    items.add(
                        DistributorItem(
                            id = itemId,
                            ordenFila = ordenFila,
                            tipoId = tipoId,
                            porcentajeDeseado = pctDeseado,
                            titulo = titulo,
                            codigo = codigo,
                            descripcion = descripcion,
                            precioCostoAnterior = costoAnt,
                            precioCostoNuevo = costoNuevo,
                            precioVentaAnterior = ventaAnt,
                            porcentajeAnteriorRedondeado = pctAnt,
                            precioVentaResultante = ventaRes,
                            porcentajeMargen = pctMargen,
                            stockAnterior = stockAnt,
                            recargaStock = recargaStock,
                            variedadesMismoCosto = variedades,
                            ultimoTipoSolicitudId = tipoId,
                            updatedAt = itemUpdated
                        )
                    )
                }
            } else {
                val isV2 = version == 2
                for (j in 0 until itemCount) {
                    val itemId = dis.readFastString()
                    val ordenFila = dis.readInt()
                    val codigo = dis.readNullableFastString()
                    val descripcion = dis.readFastString()
                    val costoAnt = dis.readNullableLong()
                    val costoNuevo = dis.readLong()
                    val ventaRes = dis.readLong()
                    val pctMargen = dis.readInt()
                    val ultimoTipo = dis.readNullableInt()
                    val itemUpdated = dis.readLong()

                    val resolvedTipoId: Int
                    val titulo: String
                    val pctDeseado: Int?
                    val ventaAnt: Long?
                    val pctAnt: Int?
                    val stockAnt: String?
                    val recargaStock: String?
                    val variedades: String?

                    if (isV2) {
                        resolvedTipoId = dis.readInt()
                        titulo = dis.readFastString()
                        pctDeseado = dis.readNullableInt()
                        ventaAnt = dis.readNullableLong()
                        pctAnt = dis.readNullableInt()
                        stockAnt = dis.readNullableFastString()
                        recargaStock = dis.readNullableFastString()
                        variedades = dis.readNullableFastString()
                    } else {
                        resolvedTipoId = ultimoTipo ?: 1
                        pctDeseado = if (resolvedTipoId in 4..5) 25 else null
                        titulo = DistributorItem.resolveDistributorTipoTitle(resolvedTipoId, pctDeseado ?: 25)
                        ventaAnt = null
                        pctAnt = null
                        stockAnt = null
                        recargaStock = null
                        variedades = null
                    }

                    items.add(
                        DistributorItem(
                            id = itemId,
                            ordenFila = ordenFila,
                            tipoId = resolvedTipoId,
                            porcentajeDeseado = pctDeseado,
                            titulo = titulo,
                            codigo = codigo,
                            descripcion = descripcion,
                            precioCostoAnterior = costoAnt,
                            precioCostoNuevo = costoNuevo,
                            precioVentaAnterior = ventaAnt,
                            porcentajeAnteriorRedondeado = pctAnt,
                            precioVentaResultante = ventaRes,
                            porcentajeMargen = pctMargen,
                            stockAnterior = stockAnt,
                            recargaStock = recargaStock,
                            variedadesMismoCosto = variedades,
                            ultimoTipoSolicitudId = resolvedTipoId,
                            updatedAt = itemUpdated
                        )
                    )
                }
            }

            items.sortWith(DistributorItem.ROW_COMPARATOR)
            distributors.add(
                DistributorInvoice(
                    id = dId,
                    nombre = dNombre,
                    items = items,
                    updatedAt = dUpdatedAt
                )
            )
        }
        return distributors to activeId
    }

    internal fun writeDistributorsToStream(
        dos: DataOutputStream,
        distributors: List<DistributorInvoice>,
        activeId: String?
    ) {
        dos.writeInt(MAGIC_DISTRIBUTORS_V3)
        dos.writeNullableFastString(activeId)
        dos.writeInt(distributors.size)
        for (d in distributors) {
            dos.writeFastString(d.id)
            dos.writeFastString(d.nombre)
            dos.writeLong(d.updatedAt)
            val ordered = d.sortedItems
            dos.writeInt(ordered.size)
            for (item in ordered) {
                val effectiveCode = item.codigoEfectivo
                var flags = 0
                if (effectiveCode != null) flags = flags or DIST_FLAG_CODIGO
                if (item.porcentajeDeseado != null) flags = flags or DIST_FLAG_PCT_DESEADO
                if (item.precioCostoAnterior != null) flags = flags or DIST_FLAG_COSTO_ANT
                if (item.precioVentaAnterior != null) flags = flags or DIST_FLAG_VENTA_ANT
                if (item.porcentajeAnteriorRedondeado != null) flags = flags or DIST_FLAG_PCT_ANT
                if (item.stockAnterior != null) flags = flags or DIST_FLAG_STOCK_ANT
                if (item.recargaStock != null) flags = flags or DIST_FLAG_RECARGA
                if (item.variedadesMismoCosto != null) flags = flags or DIST_FLAG_VARIEDADES

                dos.writeByte(flags)
                dos.writeFastString(item.id)
                dos.writeInt(item.ordenFila)
                dos.writeInt(item.tipoId)
                dos.writeFastString(item.titulo)
                if (effectiveCode != null) dos.writeFastString(effectiveCode)
                dos.writeFastString(item.descripcion)
                if (item.porcentajeDeseado != null) dos.writeInt(item.porcentajeDeseado)
                if (item.precioCostoAnterior != null) dos.writeLong(item.precioCostoAnterior)
                dos.writeLong(item.precioCostoNuevo)
                if (item.precioVentaAnterior != null) dos.writeLong(item.precioVentaAnterior)
                if (item.porcentajeAnteriorRedondeado != null) dos.writeInt(item.porcentajeAnteriorRedondeado)
                dos.writeLong(item.precioVentaResultante)
                dos.writeInt(item.porcentajeMargen)
                if (item.stockAnterior != null) dos.writeFastString(item.stockAnterior)
                if (item.recargaStock != null) dos.writeFastString(item.recargaStock)
                if (item.variedadesMismoCosto != null) dos.writeFastString(item.variedadesMismoCosto)
                dos.writeLong(item.updatedAt)
            }
        }
    }

    private fun readDistributorsFromDisk(): Pair<List<DistributorInvoice>, String?> {
        if (!distributorsFile.exists()) return emptyList<DistributorInvoice>() to null
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(distributorsFile), BUFFER_SIZE)).use { dis ->
                readDistributorsFromStream(dis)
            } ?: (emptyList<DistributorInvoice>() to null)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<DistributorInvoice>() to null
        }
    }

    private fun writeDistributorsToDisk(distributors: List<DistributorInvoice>, activeId: String?) {
        atomicWrite(distributorsFile) { dos ->
            writeDistributorsToStream(dos, distributors, activeId)
        }
    }

    private inline fun atomicWrite(target: File, block: (DataOutputStream) -> Unit) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        try {
            FileOutputStream(tmp).use { fos ->
                DataOutputStream(BufferedOutputStream(fos, BUFFER_SIZE)).use { dos ->
                    block(dos)
                    dos.flush()
                }
                try {
                    fos.fd.sync()
                } catch (_: Exception) {
                    Unit
                }
            }
            moveAtomic(tmp, target)
        } catch (e: Exception) {
            e.printStackTrace()
            tmp.delete()
        }
    }

    private fun moveAtomic(src: File, dst: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.move(
                    src.toPath(),
                    dst.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
                return
            } catch (_: Exception) {
                Unit
            }
        }
        if (dst.exists()) {
            dst.delete()
        }
        if (!src.renameTo(dst)) {
            src.copyTo(dst, overwrite = true)
            src.delete()
        }
    }

    private fun DataOutputStream.writeFastString(s: String) {
        val charLen = s.length
        if (charLen == 0) {
            writeInt(0)
            return
        }
        val maxBytes = charLen * 3
        val buf = obtainScratchBytes(maxBytes)
        var outPos = 0
        var i = 0
        while (i < charLen) {
            val c = s[i].code
            if (c < 0x80) {
                buf[outPos++] = c.toByte()
            } else if (c < 0x800) {
                buf[outPos++] = (0xC0 or (c ushr 6)).toByte()
                buf[outPos++] = (0x80 or (c and 0x3F)).toByte()
            } else if (c in 0xD800..0xDBFF && i + 1 < charLen) {
                val low = s[i + 1].code
                if (low in 0xDC00..0xDFFF) {
                    val cp = 0x10000 + (((c - 0xD800) shl 10) or (low - 0xDC00))
                    buf[outPos++] = (0xF0 or (cp ushr 18)).toByte()
                    buf[outPos++] = (0x80 or ((cp ushr 12) and 0x3F)).toByte()
                    buf[outPos++] = (0x80 or ((cp ushr 6) and 0x3F)).toByte()
                    buf[outPos++] = (0x80 or (cp and 0x3F)).toByte()
                    i++
                } else {
                    buf[outPos++] = '?'.code.toByte()
                }
            } else {
                buf[outPos++] = (0xE0 or (c ushr 12)).toByte()
                buf[outPos++] = (0x80 or ((c ushr 6) and 0x3F)).toByte()
                buf[outPos++] = (0x80 or (c and 0x3F)).toByte()
            }
            i++
        }
        writeInt(outPos)
        write(buf, 0, outPos)
    }

    private fun DataInputStream.readFastString(): String {
        val len = readInt()
        if (len <= 0) return ""
        val safeLen = len.coerceAtMost(MAX_STRING_BYTES)
        val buf = obtainScratchBytes(safeLen)
        readFully(buf, 0, safeLen)
        return String(buf, 0, safeLen, Charsets.UTF_8)
    }

    private fun DataOutputStream.writeNullableFastString(value: String?) {
        if (value == null) {
            writeBoolean(false)
        } else {
            writeBoolean(true)
            writeFastString(value)
        }
    }

    private fun DataInputStream.readNullableFastString(): String? {
        return if (readBoolean()) readFastString() else null
    }

    private fun DataInputStream.readNullableUTF(): String? {
        return if (readBoolean()) readUTF() else null
    }

    private fun DataInputStream.readNullableLong(): Long? {
        return if (readBoolean()) readLong() else null
    }

    private fun DataInputStream.readNullableInt(): Int? {
        return if (readBoolean()) readInt() else null
    }

    private fun indexOfProductId(list: List<Product>, id: String): Int {
        for (i in list.indices) {
            if (list[i].id == id) return i
        }
        return -1
    }

    private fun indexOfRequestId(list: List<PriceRequest>, id: String): Int {
        for (i in list.indices) {
            if (list[i].id == id) return i
        }
        return -1
    }

    companion object {
        const val DEFAULT_WORKPLACE_NAME = "Mi Local Comercial"

        private val PRODUCT_SORT_COMPARATOR: Comparator<Product> = compareBy { it.sortKey }

        private val scratchByteBuffer = object : ThreadLocal<ByteArray>() {
            override fun initialValue(): ByteArray = ByteArray(4096)
        }

        private fun obtainScratchBytes(requiredSize: Int): ByteArray {
            var current = scratchByteBuffer.get() ?: ByteArray(4096)
            if (current.size < requiredSize) {
                var newCap = current.size
                while (newCap < requiredSize) {
                    newCap = newCap shl 1
                }
                current = ByteArray(newCap)
                scratchByteBuffer.set(current)
            }
            return current
        }

        private const val PROD_FLAG_CODIGO = 1 shl 0
        private const val PROD_FLAG_CODIGO_ALT = 1 shl 1
        private const val PROD_FLAG_COSTO = 1 shl 2
        private const val PROD_FLAG_VENTA = 1 shl 3
        private const val PROD_FLAG_PCT_PERS = 1 shl 4
        private const val PROD_FLAG_PCT_REAL = 1 shl 5
        private const val PROD_FLAG_NOTA = 1 shl 6

        private const val REQ_FLAG_DIST_ID = 1 shl 0
        private const val REQ_FLAG_ROW_ORD = 1 shl 1
        private const val REQ_FLAG_YA_REG = 1 shl 2
        private const val REQ_FLAG_CODIGO = 1 shl 3
        private const val REQ_FLAG_PCT_DESEADO = 1 shl 4
        private const val REQ_FLAG_COSTO_ANT = 1 shl 5
        private const val REQ_FLAG_VENTA_ANT = 1 shl 6
        private const val REQ_FLAG_PCT_ANT = 1 shl 7
        private const val REQ_FLAG_STOCK_ANT = 1 shl 8
        private const val REQ_FLAG_RECARGA = 1 shl 9
        private const val REQ_FLAG_VARIEDADES = 1 shl 10

        private const val DIST_FLAG_CODIGO = 1 shl 0
        private const val DIST_FLAG_PCT_DESEADO = 1 shl 1
        private const val DIST_FLAG_COSTO_ANT = 1 shl 2
        private const val DIST_FLAG_VENTA_ANT = 1 shl 3
        private const val DIST_FLAG_PCT_ANT = 1 shl 4
        private const val DIST_FLAG_STOCK_ANT = 1 shl 5
        private const val DIST_FLAG_RECARGA = 1 shl 6
        private const val DIST_FLAG_VARIEDADES = 1 shl 7

        private const val MAGIC_PRODUCTS_V1 = 0x53545031
        private const val MAGIC_REQUESTS_V1 = 0x53545231
        private const val MAGIC_CONFIG_V1 = 0x53544331

        private const val MAGIC_PRODUCTS_V2 = 0x53545032
        private const val MAGIC_PRODUCTS_V3 = 0x53545033
        private const val MAGIC_REQUESTS_V2 = 0x53545232
        private const val MAGIC_REQUESTS_V3 = 0x53545233
        private const val MAGIC_REQUESTS_V4 = 0x53545234
        private const val MAGIC_CONFIG_V2 = 0x53544332
        private const val MAGIC_DISTRIBUTORS_V1 = 0x46424431
        private const val MAGIC_DISTRIBUTORS_V2 = 0x46424432
        private const val MAGIC_DISTRIBUTORS_V3 = 0x46424433

        private const val BUFFER_SIZE = 32 * 1024
        private const val WRITE_DEBOUNCE_MS = 250L
        private const val MAX_SAFE_ITEMS = 250_000
        private const val MAX_STRING_BYTES = 1_048_576

        @Volatile
        private var INSTANCE: FastBinaryStore? = null

        fun getInstance(context: Context): FastBinaryStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FastBinaryStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
