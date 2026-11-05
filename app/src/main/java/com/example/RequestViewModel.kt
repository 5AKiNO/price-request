package com.example

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val editingDistributorItemId: String? = null,
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
    val variedadesList: List<VarietyStockItem> = emptyList(),
    val distributors: List<DistributorInvoice> = emptyList(),
    val selectedDistributorId: String? = null,
    val ordenFilaBoletaInput: String = ""
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
        viewModelScope.launch {
            repository.allDistributors.collect { distList ->
                _uiState.update { current ->
                    val validSelectedId = current.selectedDistributorId?.takeIf { id ->
                        distList.any { it.id == id }
                    }
                    val suggestedRow = if (validSelectedId != null && current.ordenFilaBoletaInput.isBlank()) {
                        computeSuggestedRowOrder(distList, validSelectedId, current.codigo, current.descripcion)
                    } else {
                        current.ordenFilaBoletaInput
                    }
                    current.copy(
                        distributors = distList,
                        selectedDistributorId = validSelectedId,
                        ordenFilaBoletaInput = suggestedRow
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.activeDistributorId.collect { activeId ->
                _uiState.update { current ->
                    if (current.editingRequestId == null && current.editingDistributorItemId == null && current.selectedDistributorId == null && activeId != null) {
                        val suggestedRow = computeSuggestedRowOrder(
                            current.distributors,
                            activeId,
                            current.codigo,
                            current.descripcion
                        )
                        current.copy(
                            selectedDistributorId = activeId,
                            ordenFilaBoletaInput = suggestedRow
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun computeSuggestedRowOrder(
        distributors: List<DistributorInvoice>,
        distributorId: String?,
        codigo: String,
        descripcion: String
    ): String {
        if (distributorId == null) return ""
        val invoice = distributors.firstOrNull { it.id == distributorId } ?: return "1"
        val cleanCode = codigo.trim()
        val cleanDesc = descripcion.trim()
        val existingMatch = invoice.items.firstOrNull { item ->
            (cleanCode.isNotBlank() && item.codigo?.equals(cleanCode, ignoreCase = true) == true) ||
                (cleanDesc.isNotBlank() && item.descripcion.equals(cleanDesc, ignoreCase = true))
        }
        return (existingMatch?.ordenFila ?: invoice.nextRowOrder).toString()
    }

    fun selectDistributorForRequest(distributorId: String?) {
        _uiState.update { current ->
            val suggestedRow = if (distributorId != null) {
                computeSuggestedRowOrder(current.distributors, distributorId, current.codigo, current.descripcion)
            } else {
                ""
            }
            current.copy(
                selectedDistributorId = distributorId,
                ordenFilaBoletaInput = suggestedRow,
                validationError = null
            )
        }
        if (distributorId != null) {
            viewModelScope.launch {
                repository.setActiveDistributorId(distributorId)
            }
        }
    }

    fun prepareNewRequestForDistributor(distributorId: String) {
        _uiState.update { current ->
            val suggestedRow = computeSuggestedRowOrder(current.distributors, distributorId, "", "")
            RequestUiState(
                workplaceName = current.workplaceName,
                allRequests = current.allRequests,
                distributors = current.distributors,
                selectedDistributorId = distributorId,
                ordenFilaBoletaInput = suggestedRow
            )
        }
        viewModelScope.launch {
            repository.setActiveDistributorId(distributorId)
        }
    }

    fun onOrdenFilaBoletaChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { it.copy(ordenFilaBoletaInput = digits) }
    }

    fun createQuickDistributorAndSelect(nombre: String) {
        val clean = nombre.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val created = DistributorInvoice(nombre = clean)
            repository.saveDistributor(created)
            selectDistributorForRequest(created.id)
        }
    }

    fun loadFromDistributorItem(distributorId: String, item: DistributorItem) {
        val parsedVarieties = item.variedadesLista.map { line ->
            VarietyStockItem.parseFromFormattedLine(line)
        }
        val hasCostoNuevo = item.precioCostoNuevo > 0L
        val hasVenta = item.precioVentaResultante > 0L
        val cNuevoStr = if (hasCostoNuevo) item.precioCostoNuevo.toString() else ""
        val vNuevaStr = if (hasVenta) item.precioVentaResultante.toString() else ""
        val cAntVal = item.precioCostoAnterior ?: item.precioCostoNuevo.takeIf { it > 0L }
        val vAntVal = item.precioVentaAnterior ?: item.precioVentaResultante.takeIf { it > 0L }
        val marginNuevo = if (hasCostoNuevo && hasVenta) {
            MathUtils.calcularPorcentajeRedondeado(item.precioCostoNuevo, item.precioVentaResultante)
        } else {
            item.porcentajeMargen
        }
        val marginAnt = item.porcentajeAnteriorRedondeado ?: (
            if (cAntVal != null && cAntVal > 0L && vAntVal != null && vAntVal > 0L) {
                MathUtils.calcularPorcentajeRedondeado(cAntVal, vAntVal)
            } else marginNuevo
            )
        val presetPct = item.porcentajeDeseado
            ?: marginNuevo.takeIf { it in listOf(20, 25, 30, 35, 50) }
            ?: 25
        val codeVal = item.codigo ?: ""
        val resolvedTipo = item.tipoId.coerceIn(1, 5)

        _uiState.update { current ->
            current.copy(
                editingRequestId = null,
                editingDistributorItemId = item.id,
                selectedTipoId = resolvedTipo,
                selectedProduct = null,
                codigo = codeVal,
                codigoProducto = codeVal,
                descripcion = item.descripcion,
                costoAnteriorInput = cAntVal?.toString() ?: "",
                costoAnterior = cAntVal,
                ventaAnteriorInput = vAntVal?.toString() ?: "",
                ventaAnterior = vAntVal,
                porcentajeAnterior = marginAnt,
                porcentajeDeseado = presetPct,
                costoNuevo = cNuevoStr,
                ventaNueva = vNuevaStr,
                porcentajeNuevo = marginNuevo,
                stockAnterior = if (resolvedTipo == 1) "" else (item.stockAnterior ?: ""),
                recargaStock = item.recargaStock ?: "",
                variedadesList = parsedVarieties,
                selectedDistributorId = distributorId,
                ordenFilaBoletaInput = item.ordenFila.toString(),
                validationError = null,
                isSuccess = false
            )
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
                val codeVal = product.codigo ?: ""
                val suggestedRow = computeSuggestedRowOrder(
                    current.distributors,
                    current.selectedDistributorId,
                    codeVal,
                    product.descripcion
                )

                current.copy(
                    editingRequestId = null,
                    editingDistributorItemId = null,
                    selectedProduct = product,
                    codigo = codeVal,
                    codigoProducto = codeVal,
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
                    ordenFilaBoletaInput = suggestedRow,
                    validationError = null
                )
            }
        } else {
            _uiState.update { it.copy(selectedProduct = null) }
        }
    }

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
            val suggestedRow = request.ordenFilaBoleta?.toString() ?: computeSuggestedRowOrder(
                current.distributors,
                request.distributorId ?: current.selectedDistributorId,
                codeStr,
                request.descripcion
            )
            current.copy(
                editingRequestId = request.id,
                editingDistributorItemId = null,
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
                selectedDistributorId = request.distributorId ?: current.selectedDistributorId,
                ordenFilaBoletaInput = suggestedRow,
                validationError = null,
                isSuccess = false
            )
        }
    }

    fun cancelEditing() {
        resetForm()
    }

    fun onCodigoChanged(value: String) {
        _uiState.update { current ->
            val suggestedRow = if (current.selectedDistributorId != null && current.ordenFilaBoletaInput.isBlank()) {
                computeSuggestedRowOrder(current.distributors, current.selectedDistributorId, value, current.descripcion)
            } else {
                current.ordenFilaBoletaInput
            }
            current.copy(codigo = value, codigoProducto = value, ordenFilaBoletaInput = suggestedRow)
        }
    }

    fun onDescripcionChanged(value: String) {
        _uiState.update { current ->
            val suggestedRow = if (current.selectedDistributorId != null && current.ordenFilaBoletaInput.isBlank()) {
                computeSuggestedRowOrder(current.distributors, current.selectedDistributorId, current.codigo, value)
            } else {
                current.ordenFilaBoletaInput
            }
            current.copy(descripcion = value, ordenFilaBoletaInput = suggestedRow)
        }
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

    fun onCostoNuevoChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { current ->
            val cNuevo = digits.toLongOrNull()
            val vNueva = current.ventaNueva.toLongOrNull()
            val marginNuevo = if (cNuevo != null && vNueva != null && cNuevo > 0L) {
                MathUtils.calcularPorcentajeRedondeado(cNuevo, vNueva)
            } else null

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
        return DistributorItem.resolveDistributorTipoTitle(tipoId, porcentajeDeseado)
    }

    fun saveOnlyToDistributorBoleta(
        onSaved: (distributorName: String, rowOrder: Int, wasEditingDistributorRow: Boolean) -> Unit
    ) {
        val state = _uiState.value
        val distId = state.selectedDistributorId
        if (distId == null) {
            _uiState.update {
                it.copy(validationError = "Selecciona o crea un Distribuidor en el bloque superior para guardar esta solicitud en su boleta")
            }
            return
        }
        val distName = state.distributors.firstOrNull { it.id == distId }?.nombre ?: "Distribuidor"
        val rowOrd = state.ordenFilaBoletaInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val wasEditingRow = state.editingDistributorItemId != null

        validateAndSubmit(
            shareToWhatsApp = false,
            onSavedOnly = {
                onSaved(distName, rowOrd, wasEditingRow)
            }
        )
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
            4, 5 -> cNuevo
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

        when (tipoId) {
            2 -> {
                if (cAnt != null && cNuevo <= cAnt) {
                    _uiState.update {
                        it.copy(validationError = "Para subida de costo, el Costo Nuevo (Gs. ${MathUtils.formatearGuaranies(cNuevo)}) debe ser mayor al Anterior (Gs. ${MathUtils.formatearGuaranies(cAnt)})")
                    }
                    return
                }
            }
            3 -> {
                if (cAnt != null && cNuevo >= cAnt) {
                    _uiState.update {
                        it.copy(validationError = "Para bajada de costo, el Costo Nuevo (Gs. ${MathUtils.formatearGuaranies(cNuevo)}) debe ser menor al Anterior (Gs. ${MathUtils.formatearGuaranies(cAnt)})")
                    }
                    return
                }
            }
            4 -> {
                val targetPct = state.porcentajeDeseado
                if (vAnt != null && vNueva >= vAnt) {
                    _uiState.update {
                        it.copy(validationError = "En Tipo 4 (excede el $targetPct%), el Precio de Venta Nuevo (Gs. ${MathUtils.formatearGuaranies(vNueva)}) debe ser menor al Precio de Venta Anterior (Gs. ${MathUtils.formatearGuaranies(vAnt)})")
                    }
                    return
                }
            }
            5 -> {
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

        val variedadesText = state.variedadesList
            .map { it.formatBulletLine(includeStock = includeStock) }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .takeIf { it.isNotBlank() }

        val distId = state.selectedDistributorId
        val rowOrderVal = if (distId != null) {
            state.ordenFilaBoletaInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
        } else null

        val requestId = state.editingRequestId ?: UUID.randomUUID().toString()
        val cleanCode = state.codigo.trim().takeIf { it.isNotBlank() }
        val cleanDesc = state.descripcion.trim()
        val cleanStockAnt = if (includeStock) state.stockAnterior.trim().takeIf { it.isNotBlank() } else null
        val cleanRecarga = state.recargaStock.trim().takeIf { it.isNotBlank() }

        val priceRequest = PriceRequest(
            id = requestId,
            timestamp = System.currentTimeMillis(),
            tipoId = tipoId,
            titulo = reqTitle,
            productoYaRegistrado = tipoId in 2..5 || state.selectedProduct != null,
            codigo = cleanCode,
            codigoProducto = cleanCode,
            descripcion = cleanDesc,
            porcentajeDeseado = if (tipoId in 4..5) targetPctForTitle else null,
            precioCostoAnterior = cAnt,
            precioCostoNuevo = cNuevo,
            precioVentaAnterior = vAnt,
            porcentajeAnteriorRedondeado = pAnt,
            precioVentaNuevo = vNueva,
            porcentajeNuevoRedondeado = pNuevo,
            stockAnterior = cleanStockAnt,
            recargaStock = cleanRecarga,
            variedadesMismoCosto = variedadesText,
            distributorId = distId,
            ordenFilaBoleta = rowOrderVal
        )

        viewModelScope.launch {
            repository.savePriceRequest(priceRequest)

            if (distId != null && rowOrderVal != null) {
                repository.upsertDistributorItem(
                    distributorId = distId,
                    item = DistributorItem(
                        id = state.editingDistributorItemId ?: UUID.randomUUID().toString(),
                        ordenFila = rowOrderVal,
                        tipoId = tipoId,
                        titulo = reqTitle,
                        codigo = cleanCode,
                        descripcion = cleanDesc,
                        porcentajeDeseado = if (tipoId in 4..5) targetPctForTitle else null,
                        precioCostoAnterior = cAnt,
                        precioCostoNuevo = cNuevo,
                        precioVentaAnterior = vAnt,
                        porcentajeAnteriorRedondeado = pAnt,
                        precioVentaResultante = vNueva,
                        porcentajeMargen = pNuevo,
                        stockAnterior = cleanStockAnt,
                        recargaStock = cleanRecarga,
                        variedadesMismoCosto = variedadesText,
                        ultimoTipoSolicitudId = tipoId
                    )
                )
            }

            if (!shareToWhatsApp) {
                val nextRowAfterSave = (rowOrderVal ?: 1) + 1
                _uiState.update { current ->
                    RequestUiState(
                        workplaceName = current.workplaceName,
                        allRequests = current.allRequests,
                        distributors = current.distributors,
                        selectedDistributorId = current.selectedDistributorId,
                        selectedTipoId = current.selectedTipoId,
                        ordenFilaBoletaInput = if (current.selectedDistributorId != null) nextRowAfterSave.toString() else ""
                    )
                }
                onSavedOnly?.invoke()
                return@launch
            }

            val appContext = getApplication<Application>()
            val imageUri = withContext(Dispatchers.Default) {
                val compositeBitmap = ImageUtils.generateInfographicBitmap(
                    context = appContext,
                    photo16by9 = state.croppedBitmap169,
                    request = priceRequest,
                    workplaceName = state.workplaceName
                )
                val fileName = "solicitud_${priceRequest.id}.jpg"
                ImageUtils.saveBitmapToCache(appContext, compositeBitmap, fileName)
            }

            if (imageUri != null) {
                _uiState.update {
                    it.copy(
                        editingRequestId = null,
                        editingDistributorItemId = null,
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
        _uiState.update { current ->
            val suggestedRow = computeSuggestedRowOrder(
                current.distributors,
                current.selectedDistributorId,
                "",
                ""
            )
            RequestUiState(
                workplaceName = current.workplaceName,
                allRequests = current.allRequests,
                distributors = current.distributors,
                selectedDistributorId = current.selectedDistributorId,
                ordenFilaBoletaInput = suggestedRow
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
