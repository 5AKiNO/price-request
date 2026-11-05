package com.example

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class DistributorRowDisplay(
    val item: DistributorItem,
    val catalogProduct: Product?,
    val hasCatalogDifference: Boolean,
    val isMissingPrices: Boolean
)

@Immutable
data class DistributorUiState(
    val workplaceName: String = FastBinaryStore.DEFAULT_WORKPLACE_NAME,
    val distributors: List<DistributorInvoice> = emptyList(),
    val activeDistributor: DistributorInvoice? = null,
    val displayedRows: List<DistributorRowDisplay> = emptyList(),
    val catalogProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedTipoFilter: Int? = null,
    val onlyWithChanges: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),
    val editingRowId: String? = null
)

@Immutable
private data class DistributorFilterControls(
    val searchQuery: String = "",
    val selectedTipoFilter: Int? = null,
    val onlyWithChanges: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),
    val editingRowId: String? = null
)

class DistributorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository = ProductRepository(FastBinaryStore.getInstance(application))

    private val _controls = MutableStateFlow(DistributorFilterControls())

    private val _uiState = MutableStateFlow(DistributorUiState())
    val uiState: StateFlow<DistributorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.workplaceName,
                repository.allDistributors,
                repository.activeDistributorId,
                repository.allProducts,
                _controls
            ) { workplace, distributors, activeId, catalogProducts, controls ->
                val activeDist = distributors.firstOrNull { it.id == activeId } ?: distributors.firstOrNull()
                val validSelectedIds = if (activeDist != null && controls.selectedItemIds.isNotEmpty()) {
                    val itemIds = activeDist.items.mapTo(HashSet(activeDist.items.size)) { it.id }
                    controls.selectedItemIds.intersect(itemIds)
                } else {
                    emptySet()
                }

                val rows = if (activeDist != null) {
                    buildDisplayedRows(
                        invoice = activeDist,
                        catalogProducts = catalogProducts,
                        query = controls.searchQuery,
                        tipoFilter = controls.selectedTipoFilter,
                        onlyWithChanges = controls.onlyWithChanges
                    )
                } else {
                    emptyList()
                }

                DistributorUiState(
                    workplaceName = workplace,
                    distributors = distributors,
                    activeDistributor = activeDist,
                    displayedRows = rows,
                    catalogProducts = catalogProducts,
                    searchQuery = controls.searchQuery,
                    selectedTipoFilter = controls.selectedTipoFilter,
                    onlyWithChanges = controls.onlyWithChanges,
                    selectedItemIds = validSelectedIds,
                    editingRowId = controls.editingRowId
                )
            }.flowOn(Dispatchers.Default).collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun buildDisplayedRows(
        invoice: DistributorInvoice,
        catalogProducts: List<Product>,
        query: String,
        tipoFilter: Int?,
        onlyWithChanges: Boolean
    ): List<DistributorRowDisplay> {
        val codeMap = HashMap<String, Product>(catalogProducts.size)
        val descMap = HashMap<String, Product>(catalogProducts.size)
        for (p in catalogProducts) {
            val c = p.codigo?.trim()?.lowercase()
            if (!c.isNullOrEmpty()) codeMap[c] = p
            descMap[p.sortKey] = p
        }

        val tokens = MathUtils.prepareQueryTokens(query)
        val hasTokens = tokens.isNotEmpty()
        val ordered: List<DistributorItem> = invoice.sortedItems
        val result = ArrayList<DistributorRowDisplay>(ordered.size)

        for (item in ordered) {
            if (tipoFilter != null && item.tipoId != tipoFilter) continue
            if (hasTokens && !item.matchesTokens(tokens)) continue

            val catProd = item.codigoEfectivo?.lowercase()?.let { codeMap[it] }
                ?: descMap[item.sortKey]

            val isMissing = item.precioCostoNuevo <= 0L || item.precioVentaResultante <= 0L
            val hasDiff = item.hasDifferenceWithCatalog(catProd)

            if (onlyWithChanges && !hasDiff && !isMissing) continue

            result.add(
                DistributorRowDisplay(
                    item = item,
                    catalogProduct = catProd,
                    hasCatalogDifference = hasDiff,
                    isMissingPrices = isMissing
                )
            )
        }
        return result
    }

    fun selectDistributor(distributorId: String) {
        _controls.update { it.copy(selectedItemIds = emptySet(), editingRowId = null) }
        viewModelScope.launch {
            repository.setActiveDistributorId(distributorId)
        }
    }

    fun createDistributor(nombre: String, onCreated: ((DistributorInvoice) -> Unit)? = null) {
        val cleanName = nombre.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val created = DistributorInvoice(nombre = cleanName)
            repository.saveDistributor(created)
            repository.setActiveDistributorId(created.id)
            _controls.update { it.copy(selectedItemIds = emptySet(), editingRowId = null) }
            onCreated?.invoke(created)
        }
    }

    fun renameActiveDistributor(newName: String) {
        val active = _uiState.value.activeDistributor ?: return
        val clean = newName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.saveDistributor(active.copy(nombre = clean))
        }
    }

    fun deleteActiveDistributor() {
        val active = _uiState.value.activeDistributor ?: return
        _controls.update { it.copy(selectedItemIds = emptySet(), editingRowId = null) }
        viewModelScope.launch {
            repository.deleteDistributor(active.id)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _controls.update { it.copy(searchQuery = query) }
    }

    fun setTipoFilter(tipoId: Int?) {
        _controls.update { current ->
            current.copy(selectedTipoFilter = if (current.selectedTipoFilter == tipoId) null else tipoId)
        }
    }

    fun toggleOnlyWithChanges() {
        _controls.update { it.copy(onlyWithChanges = !it.onlyWithChanges) }
    }

    fun setEditingRow(itemId: String?) {
        _controls.update { current ->
            current.copy(editingRowId = if (current.editingRowId == itemId) null else itemId)
        }
    }

    fun toggleItemSelection(itemId: String) {
        _controls.update { current ->
            val next = if (current.selectedItemIds.contains(itemId)) {
                current.selectedItemIds - itemId
            } else {
                current.selectedItemIds + itemId
            }
            current.copy(selectedItemIds = next)
        }
    }

    fun selectItemsWithCatalogChanges() {
        val ids = _uiState.value.displayedRows
            .filter { it.hasCatalogDifference || it.isMissingPrices }
            .mapTo(HashSet()) { it.item.id }
        _controls.update { it.copy(selectedItemIds = ids) }
    }

    fun selectAllDisplayedItems() {
        val ids = _uiState.value.displayedRows.mapTo(HashSet()) { it.item.id }
        _controls.update { it.copy(selectedItemIds = ids) }
    }

    fun clearSelection() {
        _controls.update { it.copy(selectedItemIds = emptySet()) }
    }

    fun getSelectedItemsOrdered(): List<DistributorItem> {
        val state = _uiState.value
        val active = state.activeDistributor ?: return emptyList()
        val selected = state.selectedItemIds
        return active.sortedItems.filter { item -> selected.contains(item.id) }
    }

    fun upsertRowItem(
        existingItem: DistributorItem? = null,
        ordenFila: Int,
        tipoId: Int = existingItem?.tipoId ?: 1,
        porcentajeDeseado: Int? = existingItem?.porcentajeDeseado,
        codigo: String?,
        descripcion: String,
        precioCostoAnterior: Long? = existingItem?.precioCostoAnterior,
        precioCosto: Long?,
        precioVentaAnterior: Long? = existingItem?.precioVentaAnterior,
        precioVenta: Long?,
        porcentajeRedondeado: Int?
    ) {
        val active = _uiState.value.activeDistributor ?: return
        val cleanDesc = descripcion.trim()
        if (cleanDesc.isBlank()) return
        val cleanCode = codigo?.trim()?.takeIf { it.isNotBlank() }
        val resolvedTipo = tipoId.coerceIn(1, 5)
        val costoVal = precioCosto ?: 0L
        val ventaVal = precioVenta ?: 0L
        val pct = porcentajeRedondeado ?: if (costoVal > 0L && ventaVal > 0L) {
            MathUtils.calcularPorcentajeRedondeado(costoVal, ventaVal)
        } else 25
        val targetDesiredPct = if (resolvedTipo in 4..5) (porcentajeDeseado ?: 25) else null
        val resolvedTitle = DistributorItem.resolveDistributorTipoTitle(resolvedTipo, targetDesiredPct ?: 25)

        val item = if (existingItem != null) {
            val prevCost = precioCostoAnterior ?: if (existingItem.precioCostoNuevo > 0L && existingItem.precioCostoNuevo != costoVal) {
                existingItem.precioCostoNuevo
            } else {
                existingItem.precioCostoAnterior
            }
            val prevSale = precioVentaAnterior ?: existingItem.precioVentaAnterior
            val prevPct = if (prevCost != null && prevCost > 0L && prevSale != null && prevSale > 0L) {
                MathUtils.calcularPorcentajeRedondeado(prevCost, prevSale)
            } else existingItem.porcentajeAnteriorRedondeado

            existingItem.copy(
                ordenFila = ordenFila.coerceAtLeast(1),
                tipoId = resolvedTipo,
                titulo = resolvedTitle,
                codigo = cleanCode,
                descripcion = cleanDesc,
                porcentajeDeseado = targetDesiredPct,
                precioCostoAnterior = prevCost,
                precioCostoNuevo = costoVal,
                precioVentaAnterior = prevSale,
                porcentajeAnteriorRedondeado = prevPct,
                precioVentaResultante = ventaVal,
                porcentajeMargen = pct,
                ultimoTipoSolicitudId = resolvedTipo,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            val prevPct = if (precioCostoAnterior != null && precioCostoAnterior > 0L && precioVentaAnterior != null && precioVentaAnterior > 0L) {
                MathUtils.calcularPorcentajeRedondeado(precioCostoAnterior, precioVentaAnterior)
            } else null

            DistributorItem(
                ordenFila = ordenFila.coerceAtLeast(1),
                tipoId = resolvedTipo,
                porcentajeDeseado = targetDesiredPct,
                titulo = resolvedTitle,
                codigo = cleanCode,
                descripcion = cleanDesc,
                precioCostoAnterior = precioCostoAnterior,
                precioCostoNuevo = costoVal,
                precioVentaAnterior = precioVentaAnterior,
                porcentajeAnteriorRedondeado = prevPct,
                precioVentaResultante = ventaVal,
                porcentajeMargen = pct,
                ultimoTipoSolicitudId = resolvedTipo,
                updatedAt = System.currentTimeMillis()
            )
        }

        viewModelScope.launch {
            repository.upsertDistributorItem(active.id, item)
        }
    }

    fun removeRowItem(itemId: String) {
        val active = _uiState.value.activeDistributor ?: return
        _controls.update { current ->
            current.copy(
                selectedItemIds = current.selectedItemIds - itemId,
                editingRowId = if (current.editingRowId == itemId) null else current.editingRowId
            )
        }
        viewModelScope.launch {
            repository.deleteDistributorItem(active.id, itemId)
        }
    }

    fun shiftRowOrder(itemId: String, delta: Int) {
        val active = _uiState.value.activeDistributor ?: return
        viewModelScope.launch {
            repository.shiftDistributorItemOrder(active.id, itemId, delta)
        }
    }

    fun importProductsFromCatalogToTemplate(productIds: Set<String>, onComplete: (Int) -> Unit = {}) {
        val active = _uiState.value.activeDistributor ?: return
        if (productIds.isEmpty()) return
        viewModelScope.launch {
            val catalogById: Map<String, Product> = _uiState.value.catalogProducts.associateBy { prod -> prod.id }
            var nextRow = active.nextRowOrder
            var addedCount = 0
            for (pid in productIds) {
                val prod = catalogById[pid] ?: continue
                val costoVal = prod.precioCosto ?: 0L
                val ventaVal = prod.precioVenta ?: 0L
                val pct = prod.porcentajeRealRedondeado ?: if (costoVal > 0L && ventaVal > 0L) {
                    MathUtils.calcularPorcentajeRedondeado(costoVal, ventaVal)
                } else (prod.porcentajePersonalizado ?: 25)
                val initialTipo = if (costoVal > 0L) 2 else 1

                repository.upsertDistributorItem(
                    distributorId = active.id,
                    item = DistributorItem(
                        ordenFila = nextRow,
                        tipoId = initialTipo,
                        titulo = DistributorItem.resolveDistributorTipoTitle(initialTipo, 25),
                        codigo = prod.codigo,
                        descripcion = prod.descripcion,
                        precioCostoAnterior = prod.precioCosto,
                        precioCostoNuevo = costoVal,
                        precioVentaAnterior = prod.precioVenta,
                        porcentajeAnteriorRedondeado = pct,
                        precioVentaResultante = ventaVal,
                        porcentajeMargen = pct,
                        ultimoTipoSolicitudId = initialTipo
                    )
                )
                nextRow += 1
                addedCount += 1
            }
            onComplete(addedCount)
        }
    }

    fun syncToCatalog(onlySelected: Boolean, onComplete: (Int) -> Unit) {
        val active = _uiState.value.activeDistributor ?: return
        val selectedIds = _uiState.value.selectedItemIds
        val itemsToSync = if (onlySelected && selectedIds.isNotEmpty()) {
            active.sortedItems.filter { selectedIds.contains(it.id) }
        } else {
            active.sortedItems
        }
        viewModelScope.launch {
            val syncedCount = repository.syncDistributorItemsToCatalog(itemsToSync)
            onComplete(syncedCount)
        }
    }

    fun exportDistributorsBinaryToUri(uri: Uri, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val bytes = repository.exportDistributorsBinary()
                    val count = _uiState.value.distributors.size
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                        out.flush()
                    } ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el destino para guardar"))
                    Result.success(count)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            onComplete(result)
        }
    }

    fun importDistributorsBinaryFromUri(uri: Uri, replaceAll: Boolean, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val resolver = getApplication<Application>().contentResolver
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext Result.failure(IllegalStateException("No se pudo leer el archivo seleccionado"))
                    repository.importDistributorsBinary(bytes, replaceAll)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            onComplete(result)
        }
    }
}
