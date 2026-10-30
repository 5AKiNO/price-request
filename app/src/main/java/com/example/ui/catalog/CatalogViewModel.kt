package com.example.ui.catalog

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FastBinaryStore
import com.example.data.model.Product
import com.example.data.repository.ProductRepository
import com.example.util.MathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class CatalogUiState(
    val workplaceName: String = FastBinaryStore.DEFAULT_WORKPLACE_NAME,
    val allProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: String = "Todos",
    val selectedProductIds: Set<String> = emptySet(),
    val isFormOpen: Boolean = false,
    val editingProduct: Product? = null,
    val isExportDialogOpen: Boolean = false,
    val exportTargetProducts: List<Product> = emptyList(),
    val isWorkplaceDialogOpen: Boolean = false
)

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository = ProductRepository(FastBinaryStore.getInstance(application))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeFilter = MutableStateFlow("Todos")
    val activeFilter: StateFlow<String> = _activeFilter

    private val _selectedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedProductIds: StateFlow<Set<String>> = _selectedProductIds

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct

    private val _isFormOpen = MutableStateFlow(false)
    val isFormOpen: StateFlow<Boolean> = _isFormOpen

    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen

    private val _exportTargetProducts = MutableStateFlow<List<Product>>(emptyList())

    private val _isWorkplaceDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.allProducts,
        repository.workplaceName,
        _searchQuery,
        _activeFilter,
        _selectedProductIds
    ) { products, workplace, query, filter, selectedIds ->

        val trimmedQuery = query.trim()

        val searched = if (trimmedQuery.isEmpty()) {
            products
        } else {
            products.filter { prod ->
                prod.matchesQuery(trimmedQuery)
            }
        }

        val filtered = when (filter) {
            "Todos" -> searched
            "Completos" -> searched.filter { it.estadoRegistro == "COMPLETO" }
            "Sin Código" -> searched.filter { it.codigo.isNullOrBlank() || it.estadoRegistro == "SIN_CODIGO" }
            "Falta Registrar" -> searched.filter { it.estadoRegistro == "FALTA_REGISTRAR" }
            "Seleccionados" -> searched.filter { selectedIds.contains(it.id) }
            "20%" -> searched.filter { it.porcentajeRealRedondeado == 20 }
            "25%" -> searched.filter { it.porcentajeRealRedondeado == 25 }
            "30%" -> searched.filter { it.porcentajeRealRedondeado == 30 }
            "35%" -> searched.filter { it.porcentajeRealRedondeado == 35 }
            "Personalizado" -> searched.filter { it.porcentajePersonalizado != null }
            else -> searched
        }

        CatalogUiState(
            workplaceName = workplace,
            allProducts = products,
            filteredProducts = filtered,
            searchQuery = query,
            activeFilter = filter,
            selectedProductIds = selectedIds
        )
    }.flowOn(Dispatchers.Default)
    .combine(_isFormOpen) { state, formOpen ->
        state.copy(isFormOpen = formOpen)
    }.combine(_editingProduct) { state, editingProd ->
        state.copy(editingProduct = editingProd)
    }.combine(_isExportDialogOpen) { state, exportOpen ->
        state.copy(isExportDialogOpen = exportOpen)
    }.combine(_exportTargetProducts) { state, exportList ->
        state.copy(exportTargetProducts = exportList)
    }.combine(_isWorkplaceDialogOpen) { state, workplaceOpen ->
        state.copy(isWorkplaceDialogOpen = workplaceOpen)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CatalogUiState()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: String) {
        _activeFilter.value = filter
    }

    fun toggleProductSelection(productId: String) {
        val current = _selectedProductIds.value
        _selectedProductIds.value = if (current.contains(productId)) {
            current - productId
        } else {
            current + productId
        }
    }

    fun selectAll() {
        val allIds = uiState.value.filteredProducts.map { it.id }.toSet()
        _selectedProductIds.value = allIds
    }

    fun clearSelection() {
        _selectedProductIds.value = emptySet()
    }

    fun openWorkplaceDialog() {
        _isWorkplaceDialogOpen.value = true
    }

    fun closeWorkplaceDialog() {
        _isWorkplaceDialogOpen.value = false
    }

    fun saveWorkplaceName(newName: String) {
        viewModelScope.launch {
            repository.saveWorkplaceName(newName)
            _isWorkplaceDialogOpen.value = false
        }
    }

    fun openNewProductForm() {
        _editingProduct.value = null
        _isFormOpen.value = true
    }

    fun openEditProductForm(product: Product) {
        _editingProduct.value = product
        _isFormOpen.value = true
    }

    fun closeForm() {
        _isFormOpen.value = false
        _editingProduct.value = null
    }

    fun saveProduct(
        id: String?,
        codigo: String?,
        descripcion: String,
        costo: Long?,
        venta: Long?,
        porcentajePersonalizado: Int?
    ) {
        viewModelScope.launch {
            val roundedMargin = if (costo != null && venta != null && costo > 0L) {
                MathUtils.calcularPorcentajeRedondeado(costo, venta)
            } else {
                null
            }

            val status = when {
                codigo.isNullOrBlank() && costo != null && venta != null -> "SIN_CODIGO"
                costo == null || venta == null -> "FALTA_REGISTRAR"
                else -> "COMPLETO"
            }

            val productToSave = Product(
                id = id ?: java.util.UUID.randomUUID().toString(),
                codigo = codigo?.takeIf { it.isNotBlank() },
                descripcion = descripcion.trim(),
                precioCosto = costo,
                precioVenta = venta,
                porcentajePersonalizado = porcentajePersonalizado,
                porcentajeRealRedondeado = roundedMargin,
                estadoRegistro = status,
                updatedAt = System.currentTimeMillis()
            )

            repository.saveProduct(productToSave)
            closeForm()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            val current = _selectedProductIds.value
            if (current.contains(product.id)) {
                _selectedProductIds.value = current - product.id
            }
        }
    }

    fun openExportDialog() {
        val state = uiState.value
        val selected = state.allProducts.filter { state.selectedProductIds.contains(it.id) }
        _exportTargetProducts.value = if (selected.isNotEmpty()) selected else state.filteredProducts
        _isExportDialogOpen.value = true
    }

    fun openExportDialogForAll() {
        val state = uiState.value
        _exportTargetProducts.value = if (state.filteredProducts.isNotEmpty()) state.filteredProducts else state.allProducts
        _isExportDialogOpen.value = true
    }

    fun openSingleProductExport(product: Product) {
        _exportTargetProducts.value = listOf(product)
        _isExportDialogOpen.value = true
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
    }

    fun exportCatalogBinaryToUri(uri: Uri, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val bytes = repository.exportCatalogBinary()
                    val count = uiState.value.allProducts.size
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

    fun importCatalogBinaryFromUri(uri: Uri, replaceAll: Boolean, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val resolver = getApplication<Application>().contentResolver
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext Result.failure(IllegalStateException("No se pudo leer el archivo seleccionado"))
                    repository.importCatalogBinary(bytes, replaceAll)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            if (result.isSuccess && replaceAll) {
                _selectedProductIds.value = emptySet()
            }
            onComplete(result)
        }
    }

    /**
     * Builds an emoji-free plain-text export using the custom workplace name and the consultation structure.
     */
    fun buildWhatsAppPlainText(
        selectedProducts: List<Product>,
        includeCodigo: Boolean = true,
        includeCosto: Boolean = true,
        includeVenta: Boolean = true,
        includeMargen: Boolean = true
    ): String {
        val cleanWorkplace = uiState.value.workplaceName.trim().ifBlank { FastBinaryStore.DEFAULT_WORKPLACE_NAME }
        val sb = StringBuilder()
        sb.append("*LISTADO DE PRODUCTOS - ${cleanWorkplace.uppercase()}*\n")
        sb.append("----------------------------------------\n")

        for (prod in selectedProducts) {
            sb.append("Nombre Producto:\n")
            sb.append("· *${prod.descripcion}*\n")

            if (includeCodigo) {
                val c = prod.codigo?.takeIf { it.isNotBlank() } ?: "Sin código"
                sb.append("Código: $c\n")
            }

            if (includeCosto) {
                val costoStr = prod.precioCosto?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                sb.append("Precio de Costo Gs.: $costoStr\n")
            }

            if (includeVenta) {
                val ventaStr = prod.precioVenta?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                val marginStr = if (includeMargen && prod.porcentajeRealRedondeado != null) {
                    " (${prod.porcentajeRealRedondeado}%)"
                } else ""
                sb.append("Precio de Venta Gs.: $ventaStr$marginStr\n")
            }
            sb.append("----------------------------------------\n")
        }

        sb.append("Total de productos: ${selectedProducts.size}")
        return sb.toString()
    }
}
