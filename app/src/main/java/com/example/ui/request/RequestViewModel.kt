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
import com.example.data.model.VarietyStockItem
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
    val editingRequestId: String? = null,
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
    val allRequests: List<PriceRequest> = emptyList(),
    val stockAnterior: String = "",
    val recargaStock: String = "",
    val variedadesList: List<VarietyStockItem> = emptyList()
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
            repository.allRequests.collect { reqs ->
                _uiState.update { it.copy(allRequests = reqs) }
            }
        }
    }

    fun setTipoId(tipoId: Int) {
        _uiState.update { current ->
            val singleCost = current.costoNuevo.toLongOrNull() ?: current.costoAnterior
            val vAnt = current.ventaAnteriorInput.toLongOrNull() ?: current.ventaAnterior
            val baseCostForAnt = if (tipoId in 4..5) singleCost else (current.costoAnteriorInput.toLongOrNull() ?: current.costoAnterior)
            val newPctAnt = if (baseCostForAnt != null && baseCostForAnt > 0L && vAnt != null && vAnt > 0L) {
                MathUtils.calcularPorcentajeRedondeado(baseCostForAnt, vAnt)
            } else current.porcentajeAnterior

            current.copy(
                selectedTipoId = tipoId,
                porcentajeAnterior = newPctAnt,
                // Al cambiar a Tipo 1 (producto nuevo), se limpia el stock anterior ya que no trae stock previo
                stockAnterior = if (tipoId == 1) "" else current.stockAnterior,
                variedadesList = if (tipoId == 1) {
                    current.variedadesList.map { it.copy(stock = "") }
                } else {
                    current.variedadesList
                },
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
                val presetPct = product.porcentajePersonalizado
                    ?: antMargin?.takeIf { it in listOf(20, 25, 30, 35, 50) }
                    ?: current.porcentajeDeseado

                current.copy(
                    editingRequestId = null,
                    selectedProduct = product,
                    codigo = product.codigo ?: "",
                    codigoProducto = product.codigo ?: "",
                    descripcion = product.descripcion,
                    costoAnteriorInput = costoStr,
                    costoAnterior = product.precioCosto,
                    ventaAnteriorInput = ventaStr,
                    ventaAnterior = product.precioVenta,
                    porcentajeAnterior = antMargin,
                    porcentajeDeseado = presetPct,
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

    /**
     * Carga una solicitud del historial en el estado del formulario para permitir
     * re-editar todos sus datos y actualizar el registro existente sin guardar imágenes en el historial.
     */
    fun loadRequestForEditing(request: PriceRequest) {
        val parsedVarieties = request.variedadesLista.map { line ->
            VarietyStockItem.parseFromFormattedLine(line)
        }
        val codeStr = request.codigoEfectivo ?: ""
        val cAntVal = request.precioCostoAnterior
        val cNuevoStr = request.precioCostoNuevo.takeIf { it > 0L }?.toString() ?: ""
        val vAntVal = request.precioVentaAnterior
        val vNuevaStr = request.precioVentaNuevo.takeIf { it > 0L }?.toString() ?: ""

        _uiState.update { current ->
            current.copy(
                editingRequestId = request.id,
                selectedTipoId = request.tipoId,
                selectedProduct = null,
                codigo = codeStr,
                codigoProducto = codeStr,
                descripcion = request.descripcion,
                porcentajeDeseado = request.porcentajeDeseado ?: 25,
                costoAnteriorInput = cAntVal?.toString() ?: cNuevoStr,
                costoAnterior = cAntVal,
                costoNuevo = cNuevoStr,
                ventaAnteriorInput = vAntVal?.toString() ?: "",
                ventaAnterior = vAntVal,
                porcentajeAnterior = request.porcentajeAnteriorRedondeado,
                ventaNueva = vNuevaStr,
                porcentajeNuevo = request.porcentajeNuevoRedondeado,
                croppedBitmap169 = null,
                stockAnterior = if (request.tipoId == 1) "" else (request.stockAnterior ?: ""),
                recargaStock = request.recargaStock ?: "",
                variedadesList = parsedVarieties,
                validationError = null,
                isSuccess = false
            )
        }
    }

    fun cancelEditing() {
        resetForm()
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
        _uiState.update { current ->
            if (current.selectedTipoId == 1) current
            else current.copy(stockAnterior = value)
        }
    }

    fun onRecargaStockChanged(value: String) {
        _uiState.update { it.copy(recargaStock = value) }
    }

    fun addVariety(name: String, stock: String = "", recarga: String = "") {
        val trimmedName = name.trim()
        if (trimmedName.isNotBlank()) {
            _uiState.update { current ->
                val effectiveStock = if (current.selectedTipoId == 1) "" else stock.trim()
                val effectiveRecarga = recarga.trim()
                val exists = current.variedadesList.any {
                    it.nombre.equals(trimmedName, ignoreCase = true)
                }
                if (!exists) {
                    current.copy(
                        variedadesList = current.variedadesList + VarietyStockItem(
                            nombre = trimmedName,
                            stock = effectiveStock,
                            recarga = effectiveRecarga
                        )
                    )
                } else current
            }
        }
    }

    fun updateVarietyStock(index: Int, stock: String) {
        _uiState.update { current ->
            if (current.selectedTipoId == 1 || index !in current.variedadesList.indices) {
                current
            } else {
                val updated = current.variedadesList.toMutableList().apply {
                    this[index] = this[index].copy(stock = stock)
                }
                current.copy(variedadesList = updated)
            }
        }
    }

    fun updateVarietyRecarga(index: Int, recarga: String) {
        _uiState.update { current ->
            if (index !in current.variedadesList.indices) {
                current
            } else {
                val updated = current.variedadesList.toMutableList().apply {
                    this[index] = this[index].copy(recarga = recarga)
                }
                current.copy(variedadesList = updated)
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
                porcentajeAnterior = marginAnt
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
                    porcentajeAnterior = marginPct
                )
            }
        } else {
            _uiState.update {
                it.copy(porcentajeAnterior = marginPct)
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
        return when (tipoId) {
            1 -> "Este Producto Falta Registrar"
            2 -> "El costo de este producto subió"
            3 -> "El costo de este producto bajó"
            4 -> "El precio de venta excede el $porcentajeDeseado%"
            5 -> "El precio de venta es inferior al $porcentajeDeseado%"
            else -> "Solicitud de Cambio de Precio"
        }
    }

    fun validateAndSubmit(
        shareToWhatsApp: Boolean = true,
        onSavedOnly: (() -> Unit)? = null,
        onReadyToShareImageOnly: (Uri) -> Unit = {}
    ) {
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
            4 -> { // Excede % deseado (se solicita bajar el precio de venta respecto al anterior)
                val targetPct = state.porcentajeDeseado
                if (vAnt != null && vNueva >= vAnt) {
                    _uiState.update {
                        it.copy(validationError = "En Tipo 4 (excede el $targetPct%), el Precio de Venta Nuevo (Gs. ${MathUtils.formatearGuaranies(vNueva)}) debe ser menor al Precio de Venta Anterior (Gs. ${MathUtils.formatearGuaranies(vAnt)})")
                    }
                    return
                }
            }
            5 -> { // Inferior al % deseado (se solicita subir el precio de venta respecto al anterior)
                val targetPct = state.porcentajeDeseado
                if (vAnt != null && vNueva <= vAnt) {
                    _uiState.update {
                        it.copy(validationError = "En Tipo 5 (inferior al $targetPct%), el Precio de Venta Nuevo (Gs. ${MathUtils.formatearGuaranies(vNueva)}) debe ser mayor al Precio de Venta Anterior (Gs. ${MathUtils.formatearGuaranies(vAnt)})")
                    }
                    return
                }
            }
        }

        _uiState.update { it.copy(validationError = null) }

        val targetPctForTitle = state.porcentajeDeseado
        val reqTitle = getTipoTitle(tipoId, targetPctForTitle)
        val includeStock = tipoId != 1

        // Cada variedad se formatea con su propio Stock (solo si tipoId != 1 y fue declarado) y su propia Recarga (solo si fue declarada)
        val variedadesText = state.variedadesList
            .map { it.formatBulletLine(includeStock = includeStock) }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .takeIf { it.isNotBlank() }

        // Si se está editando una solicitud del historial, se conserva su ID para actualizar el registro en su lugar.
        // No se guardan imágenes en el historial.
        val requestId = state.editingRequestId ?: UUID.randomUUID().toString()
        val priceRequest = PriceRequest(
            id = requestId,
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
            stockAnterior = if (includeStock) state.stockAnterior.trim().takeIf { it.isNotBlank() } else null,
            recargaStock = state.recargaStock.trim().takeIf { it.isNotBlank() },
            variedadesMismoCosto = variedadesText
        )

        viewModelScope.launch {
            repository.savePriceRequest(priceRequest)

            if (!shareToWhatsApp) {
                resetForm()
                onSavedOnly?.invoke()
                return@launch
            }

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
                _uiState.update {
                    it.copy(
                        editingRequestId = null,
                        generatedCompositeUri = imageUri,
                        isSuccess = true
                    )
                }
                onReadyToShareImageOnly(imageUri)
            } else {
                _uiState.update {
                    it.copy(validationError = "Error al generar la imagen para compartir")
                }
            }
        }
    }

    fun resetForm() {
        _uiState.update {
            RequestUiState(
                workplaceName = it.workplaceName,
                allRequests = it.allRequests
            )
        }
    }

    fun deleteRequest(request: PriceRequest) {
        viewModelScope.launch {
            repository.deletePriceRequest(request)
        }
    }

    fun exportRequestsBinaryToUri(uri: Uri, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val bytes = repository.exportRequestsBinary()
                    val count = uiState.value.allRequests.size
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

    fun importRequestsBinaryFromUri(uri: Uri, replaceAll: Boolean, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val resolver = getApplication<Application>().contentResolver
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext Result.failure(IllegalStateException("No se pudo leer el archivo seleccionado"))
                    repository.importRequestsBinary(bytes, replaceAll)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            onComplete(result)
        }
    }
}
