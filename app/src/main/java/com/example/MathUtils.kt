package com.example

object MathUtils {

    private val LATIN1_LUT: CharArray = CharArray(256) { code ->
        val c = code.toChar().lowercaseChar()
        when (c) {
            'á', 'à', 'ä', 'â', 'ã', 'å' -> 'a'
            'é', 'è', 'ë', 'ê' -> 'e'
            'í', 'ì', 'ï', 'î' -> 'i'
            'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
            'ú', 'ù', 'ü', 'û' -> 'u'
            else -> c
        }
    }

    private val EMPTY_TOKENS: Array<String> = emptyArray()

    private val threadLocalCharBuf = object : ThreadLocal<CharArray>() {
        override fun initialValue(): CharArray = CharArray(28)
    }

    @JvmStatic
    fun calcularPorcentajeRedondeado(costo: Long, venta: Long): Int {
        if (costo <= 0L) return 0
        val diff = venta - costo
        return if (diff >= 0L) {
            (((diff * 100L) + (costo ushr 1)) / costo).toInt()
        } else {
            (((diff * 100L) - (costo ushr 1)) / costo).toInt()
        }
    }

    @JvmStatic
    fun calcularVentaDesdePorcentaje(costo: Long, porcentaje: Int): Long {
        if (costo <= 0L) return 0L
        return ((costo * (100L + porcentaje.toLong())) + 50L) / 100L
    }

    @JvmStatic
    fun redondearGuaranies(valor: Long, multiplo: Long = 500L): Long {
        if (multiplo <= 0L) return valor
        val resto = valor % multiplo
        return if (resto == 0L) valor else valor + (multiplo - resto)
    }

    @JvmStatic
    fun normalizarConEnie(texto: String): String {
        val len = texto.length
        if (len == 0) return ""
        val lut = LATIN1_LUT
        var firstChangedIdx = -1
        for (i in 0 until len) {
            val orig = texto[i]
            val code = orig.code
            val mapped = if (code < 256) lut[code] else orig.lowercaseChar()
            if (mapped != orig) {
                firstChangedIdx = i
                break
            }
        }
        if (firstChangedIdx < 0) return texto

        val out = CharArray(len)
        texto.toCharArray(out, 0, 0, firstChangedIdx)
        for (i in firstChangedIdx until len) {
            val orig = texto[i]
            val code = orig.code
            out[i] = if (code < 256) lut[code] else orig.lowercaseChar()
        }
        return String(out)
    }

    @JvmStatic
    fun formatearGuaranies(monto: Long?): String {
        if (monto == null || monto == 0L) return "0"
        if (monto == Long.MIN_VALUE) return "-9.223.372.036.854.775.808"

        val buf = threadLocalCharBuf.get() ?: CharArray(28)
        val negative = monto < 0L
        var v = if (negative) -monto else monto
        var pos = buf.size
        var groupCount = 0

        while (v > 0L) {
            if (groupCount == 3) {
                pos--
                buf[pos] = '.'
                groupCount = 0
            }
            val q = v / 10L
            val digit = (v - (q * 10L)).toInt()
            pos--
            buf[pos] = ('0'.code + digit).toChar()
            v = q
            groupCount++
        }

        if (negative) {
            pos--
            buf[pos] = '-'
        }

        return String(buf, pos, buf.size - pos)
    }

    @JvmStatic
    fun prepareQueryTokens(rawQuery: String): Array<String> {
        val normalized = normalizarConEnie(rawQuery.trim())
        if (normalized.isEmpty()) return EMPTY_TOKENS

        var tokenCount = 0
        var inToken = false
        val len = normalized.length
        for (i in 0 until len) {
            if (normalized[i] != ' ') {
                if (!inToken) {
                    tokenCount++
                    inToken = true
                }
            } else {
                inToken = false
            }
        }
        if (tokenCount == 0) return EMPTY_TOKENS

        val tokens = Array(tokenCount) { "" }
        var idx = 0
        var start = -1
        for (i in 0 until len) {
            if (normalized[i] != ' ') {
                if (start < 0) start = i
            } else if (start >= 0) {
                tokens[idx++] = cleanSearchToken(normalized.substring(start, i))
                start = -1
            }
        }
        if (start >= 0 && idx < tokenCount) {
            tokens[idx] = cleanSearchToken(normalized.substring(start, len))
        }
        return tokens
    }

    @JvmStatic
    private fun cleanSearchToken(token: String): String {
        if (!token.startsWith("gs") && token.indexOf('.') < 0 && token.indexOf(',') < 0) {
            return token
        }
        return token
            .removePrefix("gs.")
            .removePrefix("gs")
            .replace(".", "")
            .replace(",", "")
            .trim()
    }

    @JvmStatic
    fun matchesTokens(searchIndex: String, tokens: Array<String>): Boolean {
        for (i in tokens.indices) {
            val token = tokens[i]
            if (token.isNotEmpty() && !searchIndex.contains(token)) {
                return false
            }
        }
        return true
    }
}
