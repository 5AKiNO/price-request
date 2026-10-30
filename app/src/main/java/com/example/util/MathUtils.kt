package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MathUtils {

    private val threadLocalFormatter = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat {
            val symbols = DecimalFormatSymbols(Locale.GERMAN).apply {
                groupingSeparator = '.'
            }
            return DecimalFormat("#,###", symbols)
        }
    }

    /**
     * Regla matemática explícita: ENTERO_SIN_DECIMALES_HALF_UP
     * Aritmética entera en Long (sin asignaciones ni conversiones Double) para máximo rendimiento.
     * Si el decimal del porcentaje es igual o mayor a 0.5, se redondea al entero superior inmediato.
     */
    fun calcularPorcentajeRedondeado(costo: Long, venta: Long): Int {
        if (costo <= 0L) return 0
        val diff = venta - costo
        return if (diff >= 0L) {
            (((diff * 100L) + (costo ushr 1)) / costo).toInt()
        } else {
            (((diff * 100L) - (costo ushr 1)) / costo).toInt()
        }
    }

    /**
     * Calcula el precio de venta a partir del costo y el margen deseado usando aritmética Long pura.
     */
    fun calcularVentaDesdePorcentaje(costo: Long, porcentaje: Int): Long {
        if (costo <= 0L) return 0L
        return ((costo * (100L + porcentaje.toLong())) + 50L) / 100L
    }

    /**
     * Redondeo comercial en Guaraníes al múltiplo de 500 más cercano hacia arriba.
     */
    fun redondearGuaranies(valor: Long, multiplo: Long = 500L): Long {
        if (multiplo <= 0L) return valor
        val resto = valor % multiplo
        return if (resto == 0L) valor else valor + (multiplo - resto)
    }

    /**
     * Normalización de búsqueda multicriterio en una sola pasada (0 objetos intermedios):
     * Convierte a minúsculas y elimina tildes (á->a, é->e, í->i, ó->o, ú->u, ü->u)
     * pero preserva de forma estricta la 'ñ' y 'Ñ' (-> 'ñ').
     */
    fun normalizarConEnie(texto: String): String {
        val len = texto.length
        if (len == 0) return ""
        val out = CharArray(len)
        for (i in 0 until len) {
            val c = texto[i].lowercaseChar()
            out[i] = when (c) {
                'á', 'à', 'ä', 'â' -> 'a'
                'é', 'è', 'ë', 'ê' -> 'e'
                'í', 'ì', 'ï', 'î' -> 'i'
                'ó', 'ò', 'ö', 'ô' -> 'o'
                'ú', 'ù', 'ü', 'û' -> 'u'
                else -> c
            }
        }
        return String(out)
    }

    /**
     * Formatea importes en Guaraníes sin decimales usando puntos de miles (Ej: 13.600)
     * reutilizando una instancia cacheada por hilo (ThreadLocal) para evitar asignaciones en listas.
     */
    fun formatearGuaranies(monto: Long?): String {
        if (monto == null) return "0"
        return threadLocalFormatter.get()?.format(monto) ?: monto.toString()
    }
}
