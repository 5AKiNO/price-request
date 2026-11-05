package com.example

import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Immutable
data class VarietyStockItem(
    val nombre: String,
    val stock: String = "",
    val recarga: String = ""
) {
    fun formatBulletLine(includeStock: Boolean): String {
        val cleanName = nombre.trim()
        val cleanStock = if (includeStock) stock.trim() else ""
        val cleanRecarga = recarga.trim()

        return when {
            cleanStock.isNotEmpty() && cleanRecarga.isNotEmpty() ->
                "$cleanName (Stock: $cleanStock | Recarga: $cleanRecarga)"
            cleanStock.isNotEmpty() ->
                "$cleanName (Stock: $cleanStock)"
            cleanRecarga.isNotEmpty() ->
                "$cleanName (Recarga: $cleanRecarga)"
            else -> cleanName
        }
    }

    companion object {
        fun parseFromFormattedLine(line: String): VarietyStockItem {
            val trimmed = line.trim().removePrefix("·").trim()
            if (!trimmed.endsWith(")")) {
                return VarietyStockItem(nombre = trimmed)
            }
            val openIdx = trimmed.lastIndexOf(" (")
            if (openIdx <= 0) {
                return VarietyStockItem(nombre = trimmed)
            }
            val namePart = trimmed.substring(0, openIdx).trim()
            val metaPart = trimmed.substring(openIdx + 2, trimmed.length - 1).trim()

            var parsedStock = ""
            var parsedRecarga = ""
            val segments = metaPart.split('|')
            for (seg in segments) {
                val s = seg.trim()
                if (s.startsWith("Stock:", ignoreCase = true)) {
                    parsedStock = s.substringAfter(':').trim()
                } else if (s.startsWith("Recarga:", ignoreCase = true)) {
                    parsedRecarga = s.substringAfter(':').trim()
                }
            }

            return if (parsedStock.isNotEmpty() || parsedRecarga.isNotEmpty()) {
                VarietyStockItem(
                    nombre = namePart,
                    stock = parsedStock,
                    recarga = parsedRecarga
                )
            } else {
                VarietyStockItem(nombre = trimmed)
            }
        }
    }
}

@Immutable
data class PriceRequest(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val tipoId: Int,
    val titulo: String,
    val productoYaRegistrado: Boolean = false,
    val codigo: String? = null,
    val codigoProducto: String? = codigo,
    val descripcion: String,
    val porcentajeDeseado: Int? = null,
    val precioCostoAnterior: Long? = null,
    val precioCostoNuevo: Long = 0L,
    val precioVentaAnterior: Long? = null,
    val porcentajeAnteriorRedondeado: Int? = null,
    val precioVentaNuevo: Long = 0L,
    val porcentajeNuevoRedondeado: Int = 0,
    val stockAnterior: String? = null,
    val recargaStock: String? = null,
    val variedadesMismoCosto: String? = null,
    val distributorId: String? = null,
    val ordenFilaBoleta: Int? = null
) {
    val fechaCortaFormateada: String by lazy(LazyThreadSafetyMode.NONE) {
        formatShortTimestamp(timestamp)
    }

    val lineaPrincipalFormateada: String by lazy(LazyThreadSafetyMode.NONE) {
        VarietyStockItem(
            nombre = descripcion,
            stock = if (tipoId == 1) "" else (stockAnterior ?: ""),
            recarga = recargaStock ?: ""
        ).formatBulletLine(includeStock = tipoId != 1)
    }

    val variedadesLista: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        if (variedadesMismoCosto.isNullOrBlank()) {
            emptyList()
        } else {
            variedadesMismoCosto.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    val lineasProductosFormateadas: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        buildList {
            if (lineaPrincipalFormateada.isNotBlank()) {
                add(lineaPrincipalFormateada)
            }
            for (v in variedadesLista) {
                if (v != lineaPrincipalFormateada && v != descripcion.trim()) {
                    add(v)
                }
            }
        }
    }

    val codigoEfectivo: String? = (codigo ?: codigoProducto)?.takeIf { it.isNotBlank() }

    val costoAnteriorEfectivo: Long
        get() = precioCostoAnterior ?: precioCostoNuevo

    val ventaAnteriorEfectiva: Long
        get() = precioVentaAnterior ?: precioVentaNuevo

    val porcentajeAnteriorEfectivo: Int
        get() = porcentajeAnteriorRedondeado
            ?: MathUtils.calcularPorcentajeRedondeado(costoAnteriorEfectivo, ventaAnteriorEfectiva)

    val lineaCostoUnico: String
        get() = "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(precioCostoNuevo)}"

    val lineaCostoAnterior: String
        get() = "Precio de Costo Anterior Gs.: ${MathUtils.formatearGuaranies(costoAnteriorEfectivo)}"

    val lineaCostoNuevo: String
        get() = "Precio de Costo Nuevo Gs.: ${MathUtils.formatearGuaranies(precioCostoNuevo)}"

    val lineaVentaAnterior: String
        get() = "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(ventaAnteriorEfectiva)} ($porcentajeAnteriorEfectivo%)"

    val lineaVentaNueva: String
        get() = "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(precioVentaNuevo)} ($porcentajeNuevoRedondeado%)"

    val lineaVentaUnica: String
        get() = "Precio de Venta Gs.: ${MathUtils.formatearGuaranies(precioVentaNuevo)} ($porcentajeNuevoRedondeado%)"

    companion object {
        private val shortDateFormat = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        }

        private fun formatShortTimestamp(ts: Long): String {
            return shortDateFormat.get()?.format(Date(ts)) ?: ""
        }
    }
}
