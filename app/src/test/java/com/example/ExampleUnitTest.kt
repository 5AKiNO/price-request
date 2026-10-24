package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Tipo1Verde
import com.example.ui.theme.Tipo2Azul
import com.example.ui.theme.Tipo3Naranja
import com.example.ui.theme.Tipo4Purpura
import com.example.ui.theme.Tipo5Rojo
import com.example.ui.theme.getRequestTypeColor
import com.example.util.MathUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMathematicalHalfUpRoundingValidationCases() {
        // Validación 1: 4000 a 5500, exacto 37.5% -> >= 0.5 redondea a 38%
        assertEquals(38, MathUtils.calcularPorcentajeRedondeado(4000L, 5500L))

        // Validación 2: 2800 a 4000, exacto 42.857% -> >= 0.5 redondea a 43%
        assertEquals(43, MathUtils.calcularPorcentajeRedondeado(2800L, 4000L))

        // Validación 3: 6839 a 8500, exacto 24.287% -> < 0.5 se mantiene en 24%
        assertEquals(24, MathUtils.calcularPorcentajeRedondeado(6839L, 8500L))
    }

    @Test
    fun testVentaFromPercentageCalculation() {
        // Costo 2400 con 25% -> 3000
        assertEquals(3000L, MathUtils.calcularVentaDesdePorcentaje(2400L, 25))

        // Costo 5000 con 50% -> 7500
        assertEquals(7500L, MathUtils.calcularVentaDesdePorcentaje(5000L, 50))
    }

    @Test
    fun testNormalizationWithEnieStrictPreservation() {
        val input = "Bandeja de Cartón Diseño Español N1"
        val normalized = MathUtils.normalizarConEnie(input)

        // 'ó' becomes 'o', but 'ñ' is preserved strictly!
        assertEquals("bandeja de carton diseño español n1", normalized)
    }

    @Test
    fun testFiveColorSystemForRequestTypes() {
        assertEquals(Tipo1Verde, getRequestTypeColor(1))
        assertEquals(Tipo2Azul, getRequestTypeColor(2))
        assertEquals(Tipo3Naranja, getRequestTypeColor(3))
        assertEquals(Tipo4Purpura, getRequestTypeColor(4))
        assertEquals(Tipo5Rojo, getRequestTypeColor(5))
    }
}
