package com.example.ui.request

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FastBinaryStore
import com.example.data.model.PriceRequest
import com.example.data.model.Product
import com.example.data.repository.ProductRepository
import com.example.util.ImageUtils
import com.example.util.MathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Immutable
data class RequestUiState(
    val workplaceName: String = FastBinaryStore.DEFAULT_WORKPLACE_NAME,
    val selectedTipoId: Int = 1,
    val selectedProduct: Product? = null,
    val codigo: String = "",
    val codigoProducto: String = "",
    val descripcion: String = "",
    val porcentajeDeseado: Int = 25,
    val costoAnteriorInput: String = "",
    val costoAnterior: Long? = null,
    val costoNuevo: String = "",
    val ventaAnteriorInput: String = "",
    val ventaAnterior: Long? = null,
    val porcentajeAnterior: Int? = null,
    val ventaNueva: String = "",
    val porcentajeNuevo: Int? = null,
    val croppedBitmap169: Bitmap? = null,
    val validationError: String? = null,
    val isSuccess: Boolean = false,
    val generatedCompositeUri: Uri? = null,
    val allProducts: List<Product> = emptyList(),
    val allRequests: List<PriceRequest> = emptyList(),
    val stockAnterior: String = "",
    val recargaStock: String = "",
    val variedadesList: List<String> = emptyList()
)

class RequestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository = ProductRepository(FastBinaryStore.getInstance(application))

    private val _uiState = MutableStateFlow(RequestUiState())
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.workplaceName.collect { workplace ->
                _uiState.update { it.copy(workplaceName = workplace) }
            }
        }
        viewModelScope.launch {
            repository.allProducts.collect { prods ->
                _uiState.update { it.copy(allProducts = prods) }
            }
        }
        viewModelScope.launch {
            repository.allRequests.collect { reqs ->
                _uiState.update { it.copy(allRequests = reqs) }
            }
        }
    }

    fun setTipoId(tipoId: Int) {
        _uiState.update { current ->
            // In types 4 and 5, there is only one cost price; keep costoAnterior synced with costoNuevo if needed
            val singleCost = current.costoNuevo.toLongOrNull() ?: current.costoAnterior
            val vAnt = current.ventaAnteriorInput.toLongOrNull() ?: current.ventaAnterior
            val baseCostForAnt = if (tipoId in 4..5) singleCost else (current.costoAnteriorInput.toLongOrNull() ?: current.costoAnterior)
            val newPctAnt = if (baseCostForAnt != null && baseCostForAnt > 0L && vAnt != null && vAnt > 0L) {
                MathUtils.calcularPorcentajeRedondeado(baseCostForAnt, vAnt)
            } else current.porcentajeAnterior

            current.copy(
                selectedTipoId = tipoId,
                porcentajeAnterior = newPctAnt,
                validationError = null
            )
        }
    }

    fun selectProduct(product: Product?) {
        if (product != null) {
            val antMargin = product.porcentajeRealRedondeado ?: (
                    if (product.precioCosto != null && product.precioVenta != null) {
                        MathUtils.calcularPorcentajeRedondeado(product.precioCosto, product.precioVenta)
                    } else null
                    )

            _uiState.update { current ->
                val costoStr = product.precioCosto?.toString() ?: ""
                val ventaStr = product.precioVenta?.toString() ?: ""

                current.copy(
                    selectedProduct = product,
                    codigo = product.codigo ?: "",
                    codigoProducto = product.codigo ?: "",
                    descripcion = product.descripcion,
                    costoAnteriorInput = costoStr,
                    costoAnterior = product.precioCosto,
                    ventaAnteriorInput = ventaStr,
                    ventaAnterior = product.precioVenta,
                    porcentajeAnterior = antMargin,
                    porcentajeDeseado = antMargin ?: current.porcentajeDeseado,
                    costoNuevo = costoStr,
                    ventaNueva = ventaStr,
                    porcentajeNuevo = antMargin,
                    validationError = null
                )
            }
        } else {
            _uiState.update { it.copy(selectedProduct = null) }
        }
    }

    fun onCodigoChanged(value: String) {
        _uiState.update { it.copy(codigo = value, codigoProducto = value) }
    }

    fun onDescripcionChanged(value: String) {
        _uiState.update { it.copy(descripcion = value) }
    }

    fun onPorcentajeDeseadoChanged(value: Int) {
        _uiState.update { it.copy(porcentajeDeseado = value) }
    }

    fun onStockAnteriorChanged(value: String) {
        _uiState.update { it.copy(stockAnterior = value) }
    }

    fun onRecargaStockChanged(value: String) {
        _uiState.update { it.copy(recargaStock = value) }
    }

    fun addVariety(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update { current ->
                if (!current.variedadesList.contains(trimmed)) {
                    current.copy(variedadesList = current.variedadesList + trimmed)
                } else current
            }
        }
    }

    fun removeVariety(index: Int) {
        _uiState.update { current ->
            if (index in current.variedadesList.indices) {
                val updated = current.variedadesList.toMutableList().apply { removeAt(index) }
                current.copy(variedadesList = updated)
            } else current
        }
    }

    // --- COSTO ANTERIOR (Usado en Tipos 2 y 3) ---
    fun onCostoAnteriorChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { current ->
            val cAnt = digits.toLongOrNull()
            val vAnt = current.ventaAnteriorInput.toLongOrNull()
            val marginAnt = if (cAnt != null && vAnt != null && cAnt > 0L) {
                MathUtils.calcularPorcentajeRedondeado(cAnt, vAnt)
            } else null
            current.copy(
                costoAnteriorInput = digits,
                costoAnterior = cAnt,
                porcentajeAnterior = marginAnt
            )
        }
    }

    // --- COSTO NUEVO / COSTO ÚNICO (Usado en Tipos 1..5) ---
    fun onCostoNuevoChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { current ->
            val cNuevo = digits.toLongOrNull()
            val vNueva = current.ventaNueva.toLongOrNull()
            val marginNuevo = if (cNuevo != null && vNueva != null && cNuevo > 0L) {
                MathUtils.calcularPorcentajeRedondeado(cNuevo, vNueva)
            } else null

            // En Tipos 4 y 5 se asume un solo precio de costo para Venta Anterior y Venta Nueva
            if (current.selectedTipoId in 4..5) {
                val vAnt = current.ventaAnteriorInput.toLongOrNull()
                val marginAnt = if (cNuevo != null && vAnt != null && cNuevo > 0L) {
                    MathUtils.calcularPorcentajeRedondeado(cNuevo, vAnt)
                } else null
                current.copy(
                    costoNuevo = digits,
                    costoAnteriorInput = digits,
                    costoAnterior = cNuevo,
                    porcentajeAnterior = marginAnt,
                    porcentajeNuevo = marginNuevo
                )
            } else {
                current.copy(
                    costoNuevo = digits,
                    porcentajeNuevo = marginNuevo
                )
            }
        }
    }

    // --- VENTA ANTERIOR Y SU PORCENTAJE SELECCIONABLE (Usado en Tipos 2, 3, 4 y 5) ---
    fun onVentaAnteriorChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { current ->
            val baseCost = if (current.selectedTipoId in 4..5) {
                current.costoNuevo.toLongOrNull()
            } else {
                current.costoAnteriorInput.toLongOrNull() ?: current.costoAnterior
            }
            val vAnt = digits.toLongOrNull()
            val marginAnt = if (baseCost != null && vAnt != null && baseCost > 0L) {
                MathUtils.calcularPorcentajeRedondeado(baseCost, vAnt)
            } else null

            current.copy(
                ventaAnteriorInput = digits,
                ventaAnterior = vAnt,
                porcentajeAnterior = marginAnt,
                porcentajeDeseado = if (current.selectedTipoId in 4..5 && marginAnt != null) marginAnt else current.porcentajeDeseado
            )
        }
    }

    fun applyMarginAnterior(marginPct: Int) {
        val state = _uiState.value
        val baseCost = if (state.selectedTipoId in 4..5) {
            state.costoNuevo.toLongOrNull()
        } else {
            state.costoAnteriorInput.toLongOrNull() ?: state.costoAnterior
        }
        if (baseCost != null && baseCost > 0L) {
            val autoVentaAnt = MathUtils.calcularVentaDesdePorcentaje(baseCost, marginPct)
            _uiState.update {
                it.copy(
                    ventaAnteriorInput = autoVentaAnt.toString(),
                    ventaAnterior = autoVentaAnt,
                    porcentajeAnterior = marginPct,
                    porcentajeDeseado = if (it.selectedTipoId in 4..5) marginPct else it.porcentajeDeseado
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    porcentajeAnterior = marginPct,
                    porcentajeDeseado = if (it.selectedTipoId in 4..5) marginPct else it.porcentajeDeseado
                )
            }
        }
    }

    // --- VENTA NUEVA Y SU PORCENTAJE SELECCIONABLE (Usado en Tipos 1..5) ---
    fun onVentaNuevaChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { current ->
            val c = current.costoNuevo.toLongOrNull()
            val v = digits.toLongOrNull()
            val margin = if (c != null && v != null && c > 0L) {
                MathUtils.calcularPorcentajeRedondeado(c, v)
            } else null
            current.copy(ventaNueva = digits, porcentajeNuevo = margin)
        }
    }

    fun applyMarginNuevo(marginPct: Int) {
        val c = _uiState.value.costoNuevo.toLongOrNull()
        if (c != null && c > 0L) {
            val autoVenta = MathUtils.calcularVentaDesdePorcentaje(c, marginPct)
            _uiState.update {
                it.copy(
                    ventaNueva = autoVenta.toString(),
                    porcentajeNuevo = marginPct
                )
            }
        } else {
            _uiState.update { it.copy(porcentajeNuevo = marginPct) }
        }
    }

    fun setCroppedBitmap169(bitmap: Bitmap?) {
        _uiState.update { it.copy(croppedBitmap169 = bitmap) }
    }

    fun getTipoTitle(tipoId: Int, porcentajeDeseado: Int): String {
        val state = _uiState.value
        val pctRef = state.porcentajeAnterior ?: porcentajeDeseado
        return when (tipoId) {
            1 -> "Este Producto Falta Registrar"
            2 -> "El costo de este producto subio"
            3 -> "El costo de este producto bajo"
            4 -> "El precio de venta supera el $pctRef% sobre el costo"
            5 -> "El precio de venta es menor que el $pctRef% sobre el costo"
            else -> "Solicitud de Cambio de Precio"
        }
    }

    fun validateAndSubmit(onReadyToShareImageOnly: (Uri) -> Unit) {
        val state = uiState.value
        val tipoId = state.selectedTipoId

        if (state.descripcion.trim().isBlank()) {
            _uiState.update { it.copy(validationError = "La descripción del producto es obligatoria") }
            return
        }

        val cNuevo = state.costoNuevo.toLongOrNull()
        if (cNuevo == null || cNuevo <= 0L) {
            _uiState.update { it.copy(validationError = "Ingresa un Precio de Costo válido") }
            return
        }

        val vNueva = state.ventaNueva.toLongOrNull()
        if (vNueva == null || vNueva <= 0L) {
            _uiState.update { it.copy(validationError = "Ingresa un Precio de Venta Nuevo válido") }
            return
        }

        val cAnt: Long? = when (tipoId) {
            2, 3 -> state.costoAnteriorInput.toLongOrNull() ?: state.costoAnterior
            4, 5 -> cNuevo // Un solo precio de costo en tipos 4 y 5
            else -> null
        }

        val vAnt: Long? = when (tipoId) {
            2, 3, 4, 5 -> state.ventaAnteriorInput.toLongOrNull() ?: state.ventaAnterior
            else -> null
        }

        if (tipoId in 2..3 && (cAnt == null || cAnt <= 0L)) {
            _uiState.update { it.copy(validationError = "Ingresa el Precio de Costo Anterior") }
            return
        }

        if (tipoId in 2..5 && (vAnt == null || vAnt <= 0L)) {
            _uiState.update { it.copy(validationError = "Ingresa el Precio de Venta Anterior") }
            return
        }

        val pAnt: Int? = if (cAnt != null && cAnt > 0L && vAnt != null && vAnt > 0L) {
            state.porcentajeAnterior ?: MathUtils.calcularPorcentajeRedondeado(cAnt, vAnt)
        } else null

        val pNuevo = state.porcentajeNuevo ?: MathUtils.calcularPorcentajeRedondeado(cNuevo, vNueva)

        // Validaciones específicas de negocio por tipo:
        when (tipoId) {
            2 -> { // Subió costo
                if (cAnt != null && cNuevo <= cAnt) {
                    _uiState.update {
                        it.copy(validationError = "Para subida de costo, el Costo Nuevo (Gs. ${MathUtils.formatearGuaranies(cNuevo)}) debe ser mayor al Anterior (Gs. ${MathUtils.formatearGuaranies(cAnt)})")
                    }
                    return
                }
            }
            3 -> { // Bajó costo
                if (cAnt != null && cNuevo >= cAnt) {
                    _uiState.update {
                        it.copy(validationError = "Para bajada de costo, el Costo Nuevo (Gs. ${MathUtils.formatearGuaranies(cNuevo)}) debe ser menor al Anterior (Gs. ${MathUtils.formatearGuaranies(cAnt)})")
                    }
                    return
                }
            }
            4 -> { // Supera % (subida de venta sobre único costo)
                val targetPct = pAnt ?: state.porcentajeDeseado
                if (vAnt != null && vNueva <= vAnt && pNuevo <= targetPct) {
                    _uiState.update {
                        it.copy(validationError = "El Precio de Venta Nuevo ($pNuevo%) debe superar al anterior ($targetPct%)")
                    }
                    return
                }
            }
            5 -> { // Menor que % (bajada de venta sobre único costo)
                val targetPct = pAnt ?: state.porcentajeDeseado
                if (vAnt != null && vNueva >= vAnt && pNuevo >= targetPct) {
                    _uiState.update {
                        it.copy(validationError = "El Precio de Venta Nuevo ($pNuevo%) debe ser menor al anterior ($targetPct%)")
                    }
                    return
                }
            }
        }

        _uiState.update { it.copy(validationError = null) }

        val targetPctForTitle = pAnt ?: state.porcentajeDeseado
        val reqTitle = getTipoTitle(tipoId, targetPctForTitle)
        val variedadesText = state.variedadesList.joinToString("\n").takeIf { it.isNotBlank() }

        val priceRequest = PriceRequest(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            tipoId = tipoId,
            titulo = reqTitle,
            productoYaRegistrado = tipoId in 2..5 || state.selectedProduct != null,
            codigo = state.codigo.trim().takeIf { it.isNotBlank() },
            codigoProducto = state.codigoProducto.trim().takeIf { it.isNotBlank() },
            descripcion = state.descripcion.trim(),
            porcentajeDeseado = if (tipoId in 4..5) targetPctForTitle else null,
            precioCostoAnterior = cAnt,
            precioCostoNuevo = cNuevo,
            precioVentaAnterior = vAnt,
            porcentajeAnteriorRedondeado = pAnt,
            precioVentaNuevo = vNueva,
            porcentajeNuevoRedondeado = pNuevo,
            stockAnterior = state.stockAnterior.trim().takeIf { it.isNotBlank() },
            recargaStock = state.recargaStock.trim().takeIf { it.isNotBlank() },
            variedadesMismoCosto = variedadesText
        )

        viewModelScope.launch {
            val imageUri = withContext(Dispatchers.Default) {
                val compositeBitmap = ImageUtils.generateInfographicBitmap(
                    photo16by9 = state.croppedBitmap169,
                    request = priceRequest,
                    workplaceName = state.workplaceName
                )
                val fileName = "solicitud_${priceRequest.id}.jpg"
                ImageUtils.saveBitmapToCache(getApplication(), compositeBitmap, fileName)
            }

            if (imageUri != null) {
                val finalRequest = priceRequest.copy(imagenCompuestaUri = imageUri.toString())
                repository.savePriceRequest(finalRequest)

                _uiState.update {
                    it.copy(
                        generatedCompositeUri = imageUri,
                        isSuccess = true
                    )
                }

                // Mandate Block 5: Share ONLY the generated image (without text)
                onReadyToShareImageOnly(imageUri)
            } else {
                _uiState.update {
                    it.copy(validationError = "Error al guardar la imagen en el dispositivo")
                }
            }
        }
    }

    fun resetForm() {
        _uiState.update {
            RequestUiState(
                workplaceName = it.workplaceName,
                allProducts = it.allProducts,
                allRequests = it.allRequests
            )
        }
    }

    fun deleteRequest(request: PriceRequest) {
        viewModelScope.launch {
            repository.deletePriceRequest(request)
        }
    }
}
