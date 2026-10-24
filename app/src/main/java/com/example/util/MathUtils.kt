package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.floor

object MathUtils {

    /**
     * Regla matemática explícita: ENTERO_SIN_DECIMALES_HALF_UP
     * Si el decimal del porcentaje es igual o mayor a 0.5, se redondea al entero superior inmediato.
     * Si es menor a 0.5, se mantiene el número entero base.
     */
    fun calcularPorcentajeRedondeado(costo: Long, venta: Long): Int {
        if (costo <= 0L) return 0
        val exacto = ((venta - costo).toDouble() / costo.toDouble()) * 100.0
        return floor(exacto + 0.5).toInt()
    }

    /**
     * Calcula el precio de venta a partir del costo y el margen deseado:
     * PrecioCosto * (1.0 + (PorcentajePersonalizado / 100.0))
     */
    fun calcularVentaDesdePorcentaje(costo: Long, porcentaje: Int): Long {
        if (costo <= 0L) return 0L
        val calculo = costo * (1.0 + (porcentaje.toDouble() / 100.0))
        return floor(calculo + 0.5).toLong()
    }

    /**
     * Normalización de búsqueda multicriterio:
     * Elimina tildes (á->a, é->e, etc.) pero preserva de forma estricta la 'ñ' y 'Ñ'.
     */
    fun normalizarConEnie(texto: String): String {
        return texto.lowercase(Locale.ROOT)
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ü', 'u')
    }

    /**
     * Formatea importes en Guaraníes sin decimales usando puntos de miles (Ej: 13.600 Gs.)
     */
    fun formatearGuaranies(monto: Long?): String {
        if (monto == null) return "0"
        val symbols = DecimalFormatSymbols(Locale.GERMAN).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(monto)
    }
}
