package com.example

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
    val exportInitialFormatMonochrome: Boolean = false,
    val exportTargetProducts: List<Product> = emptyList(),
    val isWorkplaceDialogOpen: Boolean = false
)

@Immutable
private data class CatalogDialogOverlayState(
    val isFormOpen: Boolean = false,
    val editingProduct: Product? = null,
    val isExportDialogOpen: Boolean = false,
    val exportInitialFormatMonochrome: Boolean = false,
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

    private val _overlayState = MutableStateFlow(CatalogDialogOverlayState())

    private val filteredDataFlow = combine(
        repository.allProducts,
        repository.workplaceName,
        _searchQuery,
        _activeFilter,
        _selectedProductIds
    ) { products, workplace, query, filter, selectedIds ->
        val tokens = MathUtils.prepareQueryTokens(query)
        val hasTokens = tokens.isNotEmpty()

        val filtered = if (!hasTokens && filter == "Todos") {
            products
        } else {
            val out = ArrayList<Product>(products.size)
            for (i in products.indices) {
                val prod = products[i]
                if (hasTokens && !prod.matchesTokens(tokens)) continue
                val matchesFilter = when (filter) {
                    "Todos" -> true
                    "Completos" -> prod.estadoRegistro == "COMPLETO"
                    "Sin Código" -> prod.codigo.isNullOrBlank() || prod.estadoRegistro == "SIN_CODIGO"
                    "Falta Registrar" -> prod.estadoRegistro == "FALTA_REGISTRAR"
                    "Seleccionados" -> selectedIds.contains(prod.id)
                    "20%" -> prod.porcentajeRealRedondeado == 20
                    "25%" -> prod.porcentajeRealRedondeado == 25
                    "30%" -> prod.porcentajeRealRedondeado == 30
                    "35%" -> prod.porcentajeRealRedondeado == 35
                    "50%" -> prod.porcentajeRealRedondeado == 50
                    "Personalizado" -> prod.porcentajePersonalizado != null
                    else -> true
                }
                if (matchesFilter) {
                    out.add(prod)
                }
            }
            out
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

    val uiState: StateFlow<CatalogUiState> = combine(
        filteredDataFlow,
        _overlayState
    ) { base, overlay ->
        base.copy(
            isFormOpen = overlay.isFormOpen,
            editingProduct = overlay.editingProduct,
            isExportDialogOpen = overlay.isExportDialogOpen,
            exportInitialFormatMonochrome = overlay.exportInitialFormatMonochrome,
            exportTargetProducts = overlay.exportTargetProducts,
            isWorkplaceDialogOpen = overlay.isWorkplaceDialogOpen
        )
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
        val allIds = uiState.value.filteredProducts.mapTo(HashSet()) { it.id }
        _selectedProductIds.value = allIds
    }

    fun clearSelection() {
        _selectedProductIds.value = emptySet()
    }

    fun openWorkplaceDialog() {
        _overlayState.value = _overlayState.value.copy(isWorkplaceDialogOpen = true)
    }

    fun closeWorkplaceDialog() {
        _overlayState.value = _overlayState.value.copy(isWorkplaceDialogOpen = false)
    }

    fun saveWorkplaceName(newName: String) {
        viewModelScope.launch {
            repository.saveWorkplaceName(newName)
            _overlayState.value = _overlayState.value.copy(isWorkplaceDialogOpen = false)
        }
    }

    fun openNewProductForm() {
        _overlayState.value = _overlayState.value.copy(
            editingProduct = null,
            isFormOpen = true
        )
    }

    fun openEditProductForm(product: Product) {
        _overlayState.value = _overlayState.value.copy(
            editingProduct = product,
            isFormOpen = true
        )
    }

    fun closeForm() {
        _overlayState.value = _overlayState.value.copy(
            isFormOpen = false,
            editingProduct = null
        )
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
                id = id ?: UUID.randomUUID().toString(),
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

    fun openExportDialog(preferMonochromeImage: Boolean = false) {
        val state = uiState.value
        val selected = state.allProducts.filter { state.selectedProductIds.contains(it.id) }
        _overlayState.value = _overlayState.value.copy(
            exportTargetProducts = if (selected.isNotEmpty()) selected else state.filteredProducts,
            exportInitialFormatMonochrome = preferMonochromeImage,
            isExportDialogOpen = true
        )
    }

    fun openExportDialogForAll(preferMonochromeImage: Boolean = false) {
        val state = uiState.value
        _overlayState.value = _overlayState.value.copy(
            exportTargetProducts = if (state.filteredProducts.isNotEmpty()) state.filteredProducts else state.allProducts,
            exportInitialFormatMonochrome = preferMonochromeImage,
            isExportDialogOpen = true
        )
    }

    fun openSingleProductExport(product: Product) {
        _overlayState.value = _overlayState.value.copy(
            exportTargetProducts = listOf(product),
            exportInitialFormatMonochrome = false,
            isExportDialogOpen = true
        )
    }

    fun closeExportDialog() {
        _overlayState.value = _overlayState.value.copy(isExportDialogOpen = false)
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

    fun buildWhatsAppPlainText(
        selectedProducts: List<Product>,
        includeCodigo: Boolean = true,
        includeCosto: Boolean = true,
        includeVenta: Boolean = true,
        includeMargen: Boolean = true
    ): String {
        val cleanWorkplace = uiState.value.workplaceName.trim().ifBlank { FastBinaryStore.DEFAULT_WORKPLACE_NAME }
        val sb = StringBuilder(selectedProducts.size * 128 + 64)
        sb.append("*Listado de Productos - ").append(cleanWorkplace).append("*\n\n")

        for (prod in selectedProducts) {
            sb.append("· *").append(prod.descripcion).append("*\n")

            if (includeCodigo) {
                sb.append("Código: ").append(prod.codigoMostrar).append('\n')
            }

            if (includeCosto) {
                val costoStr = prod.precioCosto?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                sb.append("Precio de Costo Gs.: ").append(costoStr).append('\n')
            }

            if (includeVenta) {
                val ventaStr = prod.precioVenta?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                sb.append("Precio de Venta Gs.: ").append(ventaStr)
                if (includeMargen && prod.porcentajeRealRedondeado != null) {
                    sb.append(" (").append(prod.porcentajeRealRedondeado).append("%)")
                }
                sb.append('\n')
            }
            sb.append('\n')
        }

        sb.append("Total de productos: ").append(selectedProducts.size)
        return sb.toString()
    }
}
