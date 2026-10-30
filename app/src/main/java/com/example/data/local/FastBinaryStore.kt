package com.example.data.local

import android.content.Context
import android.os.Build
import com.example.data.model.PriceRequest
import com.example.data.model.Product
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

/**
 * Motor de almacenamiento binario V2 ultraligero en memoria y archivo binario directo (.bin).
 * Optimizado para dispositivos de gama baja (1 GB - 3 GB RAM):
 * - Escritura única por canal conflated + debounce (250ms) que elimina condiciones de carrera y colapsa ráfagas de I/O.
 * - Durabilidad física real mediante FileDescriptor.sync() (fsync) + movimiento atómico (ATOMIC_MOVE / renameTo).
 * - Índice Hash O(1) en paralelo para consultas instantáneas por ID (getProductById).
 * - Inserción ordenada por búsqueda binaria (binarySearchBy) sobre sortKey precomputada.
 * - Carga inicial paralela de los 3 archivos independientes mediante corrutinas async.
 * - Exportación e importación directa de respaldos binarios nativos (.bin) con modo Fusionar o Reemplazar.
 */
class FastBinaryStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val productsMutex = Mutex()
    private val requestsMutex = Mutex()
    private val configMutex = Mutex()

    private val productsFile = File(appContext.filesDir, "store_products_v1.bin")
    private val requestsFile = File(appContext.filesDir, "store_requests_v1.bin")
    private val configFile = File(appContext.filesDir, "store_config_v1.bin")

    // Índice O(1) para búsquedas instantáneas por ID sin recorrer la lista
    @Volatile
    private var productIndex: Map<String, Product> = emptyMap()

    private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: StateFlow<List<Product>> = _productsFlow.asStateFlow()

    private val _requestsFlow = MutableStateFlow<List<PriceRequest>>(emptyList())
    val requestsFlow: StateFlow<List<PriceRequest>> = _requestsFlow.asStateFlow()

    private val _workplaceNameFlow = MutableStateFlow(DEFAULT_WORKPLACE_NAME)
    val workplaceNameFlow: StateFlow<String> = _workplaceNameFlow.asStateFlow()

    // Señales CONFLATED: colapsan múltiples guardados consecutivos en una única operación de disco
    private val productsWriteSignal = Channel<Unit>(Channel.CONFLATED)
    private val requestsWriteSignal = Channel<Unit>(Channel.CONFLATED)
    private val configWriteSignal = Channel<Unit>(Channel.CONFLATED)

    init {
        ioScope.launch {
            coroutineScope {
                val nameDeferred = async(Dispatchers.IO) { readWorkplaceNameFromDisk() }
                val productsDeferred = async(Dispatchers.IO) { readProductsFromDisk() }
                val requestsDeferred = async(Dispatchers.IO) { readRequestsFromDisk() }

                _workplaceNameFlow.value = nameDeferred.await()

                val loadedProducts = productsDeferred.await()
                val initialIndex = HashMap<String, Product>(loadedProducts.size)
                for (p in loadedProducts) {
                    initialIndex[p.id] = p
                }
                productIndex = initialIndex
                _productsFlow.value = loadedProducts

                _requestsFlow.value = requestsDeferred.await()
            }
        }
        startWriters()
    }

    // ─────────────────────────────────────────────────────────────
    //  Escritores dedicados con conflation + debounce
    // ─────────────────────────────────────────────────────────────

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
    }

    private fun drain(ch: Channel<Unit>) {
        while (ch.tryReceive().isSuccess) {
            // Vaciar señales acumuladas durante la ventana de debounce
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  API pública
    // ─────────────────────────────────────────────────────────────

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
                    list.sortWith(compareBy { it.sortKey })
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

    /**
     * Búsqueda O(1) instantánea mediante HashMap indexado en memoria.
     */
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

    // ─────────────────────────────────────────────────────────────
    //  Exportación e Importación Binaria Nativa (.bin)
    // ─────────────────────────────────────────────────────────────

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
                sorted.sortWith(compareBy { it.sortKey })
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
                merged.sortWith(compareBy { it.sortKey })
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
        val baos = ByteArrayOutputStream(current.size * 160 + 16)
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

    // ─────────────────────────────────────────────────────────────
    //  Lectura / Escritura Binaria V2 (con soporte de migración V1)
    // ─────────────────────────────────────────────────────────────

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

    private fun readProductsFromStream(dis: DataInputStream): Pair<List<Product>, Boolean>? {
        val magic = dis.readInt()
        var migratedFromV1 = false
        val isV2 = when (magic) {
            MAGIC_PRODUCTS_V2 -> true
            MAGIC_PRODUCTS_V1 -> {
                migratedFromV1 = true
                false
            }
            else -> return null
        }
        val count = dis.readInt().coerceIn(0, MAX_SAFE_ITEMS)
        val list = ArrayList<Product>(count)
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
        list.sortWith(compareBy { it.sortKey })
        return list to migratedFromV1
    }

    private fun writeProductsToStream(dos: DataOutputStream, products: List<Product>) {
        dos.writeInt(MAGIC_PRODUCTS_V2)
        dos.writeInt(products.size)
        for (p in products) {
            dos.writeFastString(p.id)
            dos.writeNullableFastString(p.codigo)
            dos.writeNullableFastString(p.codigoAlternativo)
            dos.writeFastString(p.descripcion)
            dos.writeNullableLong(p.precioCosto)
            dos.writeNullableLong(p.precioVenta)
            dos.writeNullableInt(p.porcentajePersonalizado)
            dos.writeNullableInt(p.porcentajeRealRedondeado)
            dos.writeFastString(p.estadoRegistro)
            dos.writeNullableFastString(p.notaRedondeo)
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

    private fun readRequestsFromStream(dis: DataInputStream): Pair<List<PriceRequest>, Boolean>? {
        val magic = dis.readInt()
        var migratedFromV1 = false
        val isV2 = when (magic) {
            MAGIC_REQUESTS_V2 -> true
            MAGIC_REQUESTS_V1 -> {
                migratedFromV1 = true
                false
            }
            else -> return null
        }
        val count = dis.readInt().coerceIn(0, MAX_SAFE_ITEMS)
        val list = ArrayList<PriceRequest>(count)
        for (i in 0 until count) {
            val id = if (isV2) dis.readFastString() else dis.readUTF()
            val timestamp = dis.readLong()
            val tipoId = dis.readInt()
            val titulo = if (isV2) dis.readFastString() else dis.readUTF()
            // Se consumen los campos legados de URI del flujo binario sin guardar imágenes en el historial
            if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
            if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
            val yaReg = dis.readBoolean()
            val codigo = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
            val codigoProd = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
            val descripcion = if (isV2) dis.readFastString() else dis.readUTF()
            val pctDeseado = dis.readNullableInt()
            val costoAnt = dis.readNullableLong()
            val costoNuevo = dis.readLong()
            val ventaAnt = dis.readNullableLong()
            val pctAnt = dis.readNullableInt()
            val ventaNueva = dis.readLong()
            val pctNuevo = dis.readInt()
            val stockAnt = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
            val recargaStock = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
            val variedades = if (isV2) dis.readNullableFastString() else dis.readNullableUTF()
            list.add(
                PriceRequest(
                    id = id,
                    timestamp = timestamp,
                    tipoId = tipoId,
                    titulo = titulo,
                    productoYaRegistrado = yaReg,
                    codigo = codigo,
                    codigoProducto = codigoProd,
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
                    variedadesMismoCosto = variedades
                )
            )
        }
        return list to migratedFromV1
    }

    private fun writeRequestsToStream(dos: DataOutputStream, requests: List<PriceRequest>) {
        dos.writeInt(MAGIC_REQUESTS_V2)
        dos.writeInt(requests.size)
        for (r in requests) {
            dos.writeFastString(r.id)
            dos.writeLong(r.timestamp)
            dos.writeInt(r.tipoId)
            dos.writeFastString(r.titulo)
            // No se guardan imágenes en el historial (se escriben flags nulos para compatibilidad binaria V2)
            dos.writeNullableFastString(null)
            dos.writeNullableFastString(null)
            dos.writeBoolean(r.productoYaRegistrado)
            dos.writeNullableFastString(r.codigo)
            dos.writeNullableFastString(r.codigoProducto)
            dos.writeFastString(r.descripcion)
            dos.writeNullableInt(r.porcentajeDeseado)
            dos.writeNullableLong(r.precioCostoAnterior)
            dos.writeLong(r.precioCostoNuevo)
            dos.writeNullableLong(r.precioVentaAnterior)
            dos.writeNullableInt(r.porcentajeAnteriorRedondeado)
            dos.writeLong(r.precioVentaNuevo)
            dos.writeInt(r.porcentajeNuevoRedondeado)
            dos.writeNullableFastString(r.stockAnterior)
            dos.writeNullableFastString(r.recargaStock)
            dos.writeNullableFastString(r.variedadesMismoCosto)
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

    // ─────────────────────────────────────────────────────────────
    //  Helpers de I/O atómico con fsync (durabilidad física)
    // ─────────────────────────────────────────────────────────────

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
                    // Algunos sistemas de archivos virtuales en tests pueden no soportar sync()
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
                // Fallback a renameTo si el filesystem no soporta ATOMIC_MOVE
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

    // ─────────────────────────────────────────────────────────────
    //  Serialización UTF-8 rápida (sin límite de 64 KB)
    // ─────────────────────────────────────────────────────────────

    private fun DataOutputStream.writeFastString(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readFastString(): String {
        val len = readInt()
        if (len <= 0) return ""
        val bytes = ByteArray(len)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
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

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        if (value == null) {
            writeBoolean(false)
        } else {
            writeBoolean(true)
            writeLong(value)
        }
    }

    private fun DataInputStream.readNullableLong(): Long? {
        return if (readBoolean()) readLong() else null
    }

    private fun DataOutputStream.writeNullableInt(value: Int?) {
        if (value == null) {
            writeBoolean(false)
        } else {
            writeBoolean(true)
            writeInt(value)
        }
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

        // Cabeceras V1 (compatibilidad y migración automática)
        private const val MAGIC_PRODUCTS_V1 = 0x53545031 // "STP1"
        private const val MAGIC_REQUESTS_V1 = 0x53545231 // "STR1"
        private const val MAGIC_CONFIG_V1 = 0x53544331   // "STC1"

        // Cabeceras V2 (serialización rápida writeFastString)
        private const val MAGIC_PRODUCTS_V2 = 0x53545032 // "STP2"
        private const val MAGIC_REQUESTS_V2 = 0x53545232 // "STR2"
        private const val MAGIC_CONFIG_V2 = 0x53544332   // "STC2"

        private const val BUFFER_SIZE = 16 * 1024
        private const val WRITE_DEBOUNCE_MS = 250L
        private const val MAX_SAFE_ITEMS = 250_000

        @Volatile
        private var INSTANCE: FastBinaryStore? = null

        fun getInstance(context: Context): FastBinaryStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FastBinaryStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
