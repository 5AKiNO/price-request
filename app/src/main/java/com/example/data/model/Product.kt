package com.example.data.model

import androidx.compose.runtime.Immutable
import com.example.util.MathUtils
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
    // Pre-computed search index for 0-allocation instant multi-criteria search on low-end phones
    val searchIndex: String = buildString {
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
    // Clave precomputada para ordenamiento e inserción binaria sin alojar strings por comparación
    val sortKey: String = MathUtils.normalizarConEnie(descripcion)

    // Textos de UI precomputados una sola vez al instanciar el modelo (0 asignaciones durante el scroll de LazyColumn)
    val codigoMostrar: String = codigo?.takeIf { it.isNotBlank() } ?: "Sin Código"

    val codigoCortoMostrar: String = codigo?.takeIf { it.isNotBlank() } ?: "Sin Cód."

    val costoFormateado: String = precioCosto?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "---"

    val ventaFormateada: String = precioVenta?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "---"

    val ventaCortaFormateada: String = precioVenta?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "Sin precio"

    val margenFormateado: String? = porcentajeRealRedondeado?.let { "$it%" }

    /**
     * Evalúa si el producto coincide con una consulta de búsqueda multicriterio.
     * Soporta múltiples palabras/tokens en cualquier orden: código, descripción (con ñ y sin tildes),
     * precio de costo o venta (con o sin puntos de miles), porcentaje de margen (%) y estado.
     */
    fun matchesQuery(rawQuery: String): Boolean {
        val normalized = MathUtils.normalizarConEnie(rawQuery.trim())
        if (normalized.isEmpty()) return true

        val tokens = normalized.split(' ')
        for (token in tokens) {
            if (token.isEmpty()) continue
            if (searchIndex.contains(token)) continue

            // Soporte adicional si el usuario escribe "gs.12.500" o números con separadores
            val strippedNumeric = token
                .removePrefix("gs.")
                .removePrefix("gs")
                .replace(".", "")
                .replace(",", "")
                .trim()
            if (strippedNumeric.isNotEmpty() && searchIndex.contains(strippedNumeric)) continue

            return false
        }
        return true
    }
}
