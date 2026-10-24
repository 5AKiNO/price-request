package com.example.ui.catalog

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FastBinaryStore
import com.example.data.model.Product
import com.example.data.repository.ProductRepository
import com.example.util.MathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CatalogUiState(
    val allProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val recentOrQuickProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: String = "Todos",
    val selectedProductIds: Set<String> = emptySet(),
    val isFormOpen: Boolean = false,
    val editingProduct: Product? = null,
    val isExportDialogOpen: Boolean = false
)

@OptIn(FlowPreview::class)
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

    // Debounced search query to prevent jank on low-end phones
    private val debouncedQuery = _searchQuery
        .debounce(90L)

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.allProducts,
        debouncedQuery,
        _activeFilter,
        _selectedProductIds
    ) { products, query, filter, selectedIds ->

        val normalizedQuery = MathUtils.normalizarConEnie(query.trim())

        val searched = if (normalizedQuery.isEmpty()) {
            products
        } else {
            products.filter { prod ->
                prod.searchIndex.contains(normalizedQuery)
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
            "Personalizado" -> searched.filter { it.porcentajePersonalizado != null }
            else -> searched
        }

        val quickCards = products.take(8)

        CatalogUiState(
            allProducts = products,
            filteredProducts = filtered,
            recentOrQuickProducts = quickCards,
            searchQuery = _searchQuery.value,
            activeFilter = filter,
            selectedProductIds = selectedIds
        )
    }.flowOn(Dispatchers.Default) // Execute filtering off the main thread for smooth 60fps rendering
    .combine(_isFormOpen) { state, formOpen ->
        state.copy(isFormOpen = formOpen)
    }.combine(_editingProduct) { state, editingProd ->
        state.copy(editingProduct = editingProd)
    }.combine(_isExportDialogOpen) { state, exportOpen ->
        state.copy(isExportDialogOpen = exportOpen)
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
        _isExportDialogOpen.value = true
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
    }

    fun buildWhatsAppPlainText(
        selectedProducts: List<Product>,
        includeCodigo: Boolean = true,
        includeCosto: Boolean = true,
        includeVenta: Boolean = true,
        includeMargen: Boolean = true
    ): String {
        val sb = StringBuilder()
        sb.append("📋 *LISTADO DE PRODUCTOS - AUTOSERVICIO CLAUDIA*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")

        for (prod in selectedProducts) {
            val codText = if (includeCodigo) {
                val c = prod.codigo ?: "Sin código"
                "🔹 *Cód:* $c | "
            } else {
                "🔹 "
            }
            sb.append("$codText*${prod.descripcion}*\n")

            if (includeCosto) {
                val costoStr = prod.precioCosto?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                sb.append("   • Costo: Gs. $costoStr\n")
            }

            if (includeVenta) {
                val ventaStr = prod.precioVenta?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                val marginStr = if (includeMargen && prod.porcentajeRealRedondeado != null) {
                    " (${prod.porcentajeRealRedondeado}%)"
                } else ""
                sb.append("   • Venta: Gs. $ventaStr$marginStr\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Total seleccionados: ${selectedProducts.size}")
        return sb.toString()
    }
}
