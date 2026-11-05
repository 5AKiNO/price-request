package com.example

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class DistributorItem(
    val id: String = UUID.randomUUID().toString(),
    val ordenFila: Int = 1,
    val tipoId: Int = 1,
    val porcentajeDeseado: Int? = null,
    val titulo: String = resolveDistributorTipoTitle(tipoId, porcentajeDeseado ?: 25),
    val codigo: String? = null,
    val descripcion: String,
    val precioCostoAnterior: Long? = null,
    val precioCostoNuevo: Long = 0L,
    val precioVentaAnterior: Long? = null,
    val porcentajeAnteriorRedondeado: Int? = null,
    val precioVentaResultante: Long = 0L,
    val porcentajeMargen: Int = 25,
    val stockAnterior: String? = null,
    val recargaStock: String? = null,
    val variedadesMismoCosto: String? = null,
    val ultimoTipoSolicitudId: Int? = tipoId,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val sortKey: String = MathUtils.normalizarConEnie(descripcion)

    val codigoEfectivo: String? = codigo?.trim()?.takeIf { it.isNotEmpty() }

    val codigoMostrar: String
        get() = codigoEfectivo ?: "Sin código"

    val includeStockInDisplay: Boolean
        get() = tipoId != 1

    val variedadesLista: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        variedadesMismoCosto
            ?.lines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    val lineasProductosFormateadas: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        buildList {
            val mainLine = VarietyStockItem(
                nombre = descripcion.trim(),
                stock = stockAnterior?.trim() ?: "",
                recarga = recargaStock?.trim() ?: ""
            ).formatBulletLine(includeStock = includeStockInDisplay)
            if (mainLine.isNotEmpty()) {
                add(mainLine)
            }
            for (rawLine in variedadesLista) {
                val parsed = VarietyStockItem.parseFromFormattedLine(rawLine)
                val formatted = parsed.formatBulletLine(includeStock = includeStockInDisplay)
                if (formatted.isNotEmpty()) {
                    add(formatted)
                }
            }
        }
    }

    val costoAnteriorFormateado: String
        get() = precioCostoAnterior?.let {
            "Gs. ${MathUtils.formatearGuaranies(it)}"
        } ?: "Sin dato"

    val costoNuevoFormateado: String
        get() = if (precioCostoNuevo > 0L) {
            "Gs. ${MathUtils.formatearGuaranies(precioCostoNuevo)}"
        } else {
            "Pendiente"
        }

    val ventaResultanteFormateada: String
        get() = if (precioVentaResultante > 0L) {
            "Gs. ${MathUtils.formatearGuaranies(precioVentaResultante)}"
        } else {
            "Pendiente"
        }

    val lineaCostoAnterior: String
        get() = "Precio de Costo Anterior Gs.: ${MathUtils.formatearGuaranies(precioCostoAnterior ?: precioCostoNuevo)}"

    val lineaCostoNuevo: String
        get() = "Precio de Costo Nuevo Gs.: ${MathUtils.formatearGuaranies(precioCostoNuevo)}"

    val lineaCostoUnico: String
        get() = "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(precioCostoNuevo)}"

    val lineaVentaAnterior: String
        get() {
            val vAnt = precioVentaAnterior ?: precioVentaResultante
            val cBase = if (tipoId in 2..3) (precioCostoAnterior ?: precioCostoNuevo) else precioCostoNuevo
            val pAnt = porcentajeAnteriorRedondeado
                ?: if (cBase > 0L && vAnt > 0L) MathUtils.calcularPorcentajeRedondeado(cBase, vAnt) else porcentajeMargen
            return "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)"
        }

    val lineaVentaNueva: String
        get() = "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(precioVentaResultante)} ($porcentajeMargen%)"

    val lineaVentaUnica: String
        get() = "Precio de Venta Gs.: ${MathUtils.formatearGuaranies(precioVentaResultante)} ($porcentajeMargen%)"

    val searchIndex: String by lazy(LazyThreadSafetyMode.NONE) {
        buildString(128) {
            append("fila ").append(ordenFila).append(' ')
            append("tipo ").append(tipoId).append(' ')
            append(MathUtils.normalizarConEnie(titulo)).append(' ')
            if (codigoEfectivo != null) {
                append(MathUtils.normalizarConEnie(codigoEfectivo)).append(' ')
            }
            append(sortKey).append(' ')
            if (!variedadesMismoCosto.isNullOrBlank()) {
                append(MathUtils.normalizarConEnie(variedadesMismoCosto)).append(' ')
            }
            if (precioCostoNuevo > 0L) {
                append(precioCostoNuevo).append(' ')
                append(MathUtils.formatearGuaranies(precioCostoNuevo)).append(' ')
            }
            if (precioVentaResultante > 0L) {
                append(precioVentaResultante).append(' ')
                append(MathUtils.formatearGuaranies(precioVentaResultante)).append(' ')
            }
            append(porcentajeMargen).append("% ")
        }
    }

    fun matchesTokens(tokens: Array<String>): Boolean {
        if (tokens.isEmpty()) return true
        return MathUtils.matchesTokens(searchIndex, tokens)
    }

    fun matchesQuery(rawQuery: String): Boolean {
        val tokens = MathUtils.prepareQueryTokens(rawQuery)
        if (tokens.isEmpty()) return true
        return MathUtils.matchesTokens(searchIndex, tokens)
    }

    fun hasDifferenceWithCatalog(catalogProduct: Product?): Boolean {
        if (catalogProduct == null) return true
        val catCosto = catalogProduct.precioCosto ?: 0L
        val catVenta = catalogProduct.precioVenta ?: 0L
        return (precioCostoNuevo > 0L && precioCostoNuevo != catCosto) ||
            (precioVentaResultante > 0L && precioVentaResultante != catVenta)
    }

    companion object {
        val ROW_COMPARATOR: Comparator<DistributorItem> =
            compareBy<DistributorItem> { it.ordenFila }.thenBy { it.sortKey }

        fun resolveDistributorTipoTitle(tipoId: Int, porcentajeDeseado: Int = 25): String {
            return when (tipoId) {
                1 -> "Este Producto Falta Registrar"
                2 -> "El costo de este producto subió"
                3 -> "El costo de este producto bajó"
                4 -> "El precio de venta excede el $porcentajeDeseado%"
                5 -> "El precio de venta es inferior al $porcentajeDeseado%"
                else -> "Solicitud de Cambio de Precio"
            }
        }
    }
}

@Immutable
data class DistributorInvoice(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val items: List<DistributorItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val sortKey: String = MathUtils.normalizarConEnie(nombre)

    val sortedItems: List<DistributorItem> by lazy(LazyThreadSafetyMode.NONE) {
        if (items.size <= 1) items else items.sortedWith(DistributorItem.ROW_COMPARATOR)
    }

    val nextRowOrder: Int
        get() = (items.maxOfOrNull { it.ordenFila } ?: 0) + 1
}
