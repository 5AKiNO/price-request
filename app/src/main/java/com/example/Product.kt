package com.example

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val codigo: String? = null,
    val codigoAlternativo: String? = null,
    val descripcion: String,
    val precioCosto: Long? = null,
    val precioVenta: Long? = null,
    val porcentajePersonalizado: Int? = null,
    val porcentajeRealRedondeado: Int? = null,
    val estadoRegistro: String = "COMPLETO",
    val notaRedondeo: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val searchIndex: String = buildString(128) {
        if (!codigo.isNullOrBlank()) {
            append(MathUtils.normalizarConEnie(codigo)).append(' ')
        } else {
            append("sin codigo sincodigo ")
        }
        if (!codigoAlternativo.isNullOrBlank()) {
            append(MathUtils.normalizarConEnie(codigoAlternativo)).append(' ')
        }
        append(MathUtils.normalizarConEnie(descripcion)).append(' ')
        if (precioCosto != null) {
            append(precioCosto).append(' ')
            append(MathUtils.formatearGuaranies(precioCosto)).append(' ')
        }
        if (precioVenta != null) {
            append(precioVenta).append(' ')
            append(MathUtils.formatearGuaranies(precioVenta)).append(' ')
        }
        if (porcentajeRealRedondeado != null) {
            append(porcentajeRealRedondeado).append("% ")
            append(porcentajeRealRedondeado).append(' ')
        }
        if (porcentajePersonalizado != null) {
            append(porcentajePersonalizado).append("% ")
            append("personalizado ")
        }
        when (estadoRegistro) {
            "COMPLETO" -> append("completo completos registrado ")
            "SIN_CODIGO" -> append("sin codigo sincodigo ")
            "FALTA_REGISTRAR" -> append("falta registrar faltaregistrar pendiente ")
            else -> append(MathUtils.normalizarConEnie(estadoRegistro)).append(' ')
        }
        if (!notaRedondeo.isNullOrBlank()) {
            append(MathUtils.normalizarConEnie(notaRedondeo))
        }
    }
) {
    val sortKey: String = MathUtils.normalizarConEnie(descripcion)

    val codigoMostrar: String
        get() = codigo?.takeIf { it.isNotBlank() } ?: "Sin código"

    val codigoCortoMostrar: String
        get() = codigo?.takeIf { it.isNotBlank() } ?: "Sin cód."

    val costoFormateado: String by lazy(LazyThreadSafetyMode.NONE) {
        precioCosto?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "Sin dato"
    }

    val ventaFormateada: String by lazy(LazyThreadSafetyMode.NONE) {
        precioVenta?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "Sin dato"
    }

    val ventaCortaFormateada: String by lazy(LazyThreadSafetyMode.NONE) {
        precioVenta?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "Sin precio"
    }

    val margenFormateado: String?
        get() = porcentajeRealRedondeado?.let { "$it%" }

    fun matchesTokens(tokens: Array<String>): Boolean {
        return MathUtils.matchesTokens(searchIndex, tokens)
    }

    fun matchesQuery(rawQuery: String): Boolean {
        val tokens = MathUtils.prepareQueryTokens(rawQuery)
        if (tokens.isEmpty()) return true
        return MathUtils.matchesTokens(searchIndex, tokens)
    }
}
