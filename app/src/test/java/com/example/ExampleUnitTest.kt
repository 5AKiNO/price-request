package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMathematicalHalfUpRoundingValidationCases() {
        assertEquals(38, MathUtils.calcularPorcentajeRedondeado(4000L, 5500L))
        assertEquals(43, MathUtils.calcularPorcentajeRedondeado(2800L, 4000L))
        assertEquals(24, MathUtils.calcularPorcentajeRedondeado(6839L, 8500L))
    }

    @Test
    fun testVentaFromPercentageCalculation() {
        assertEquals(3000L, MathUtils.calcularVentaDesdePorcentaje(2400L, 25))
        assertEquals(7500L, MathUtils.calcularVentaDesdePorcentaje(5000L, 50))
    }

    @Test
    fun testFastIntegerGuaraniesFormatterWithoutDecimalFormat() {
        assertEquals("0", MathUtils.formatearGuaranies(null))
        assertEquals("0", MathUtils.formatearGuaranies(0L))
        assertEquals("500", MathUtils.formatearGuaranies(500L))
        assertEquals("3.500", MathUtils.formatearGuaranies(3500L))
        assertEquals("10.650", MathUtils.formatearGuaranies(10650L))
        assertEquals("1.250.000", MathUtils.formatearGuaranies(1250000L))
        assertEquals("-45.000", MathUtils.formatearGuaranies(-45000L))
    }

    @Test
    fun testNormalizationWithEnieAndZeroAllocationFastPath() {
        val input = "Bandeja de Cartón Diseño Español N1"
        val normalized = MathUtils.normalizarConEnie(input)
        assertEquals("bandeja de carton diseño español n1", normalized)

        val alreadyNormalized = "bandeja de carton diseño español n1"
        assertSame(alreadyNormalized, MathUtils.normalizarConEnie(alreadyNormalized))
    }

    @Test
    fun testPreTokenizedQueryMatching() {
        val product = Product(
            codigo = "7840012",
            descripcion = "Yerba Mate Campesino 500g",
            precioCosto = 7000L,
            precioVenta = 9500L,
            porcentajeRealRedondeado = 36
        )
        val tokensMatch = MathUtils.prepareQueryTokens("yerba Gs. 9.500 784")
        assertTrue(product.matchesTokens(tokensMatch))

        val tokensNoMatch = MathUtils.prepareQueryTokens("yerba 15000")
        assertFalse(product.matchesTokens(tokensNoMatch))
    }

    @Test
    fun testFiveColorSystemAndBitwiseLumaContrast() {
        assertEquals(Tipo1Verde, getRequestTypeColor(1))
        assertEquals(Tipo2Azul, getRequestTypeColor(2))
        assertEquals(Tipo3Naranja, getRequestTypeColor(3))
        assertEquals(Tipo4Teal, getRequestTypeColor(4))
        assertEquals(Tipo5Rojo, getRequestTypeColor(5))

        assertTrue(computeFastLumaScaled(0xFF0D47A1.toInt()) < 140_000)
        assertTrue(computeFastLumaScaled(0xFFFFFFFF.toInt()) > 140_000)
    }

    @Test
    fun testDistributorInvoiceRowOrderingAndFiveTypesStorage() {
        val varietyLine = VarietyStockItem(
            nombre = "2- Yerba Mate Compuesta 500g",
            stock = "2",
            recarga = "6"
        ).formatBulletLine(includeStock = true)

        val itemTipo2 = DistributorItem(
            ordenFila = 2,
            tipoId = 2,
            porcentajeDeseado = 25,
            codigo = "100200",
            descripcion = "1- Yerba Mate 500g",
            precioCostoAnterior = 7000L,
            precioCostoNuevo = 8000L,
            precioVentaAnterior = 9500L,
            porcentajeAnteriorRedondeado = 26,
            precioVentaResultante = 10650L,
            porcentajeMargen = 25,
            stockAnterior = "4",
            recargaStock = "12",
            variedadesMismoCosto = varietyLine,
            ultimoTipoSolicitudId = 2
        )
        val itemTipo4 = DistributorItem(
            ordenFila = 1,
            tipoId = 4,
            porcentajeDeseado = 30,
            descripcion = "Galletita Dulce 200g",
            precioCostoNuevo = 3500L,
            precioVentaAnterior = 6000L,
            porcentajeAnteriorRedondeado = 42,
            precioVentaResultante = 5000L,
            porcentajeMargen = 30,
            stockAnterior = "10",
            recargaStock = "24",
            ultimoTipoSolicitudId = 4
        )
        val invoice = DistributorInvoice(
            nombre = "Distribuidora Norte",
            items = listOf(itemTipo2, itemTipo4)
        )
        val ordered = invoice.sortedItems
        assertEquals(1, ordered[0].ordenFila)
        assertEquals(4, ordered[0].tipoId)
        assertEquals("El precio de venta excede el 30%", ordered[0].titulo)
        assertEquals("Galletita Dulce 200g", ordered[0].descripcion)
        assertEquals("galletita dulce 200g", ordered[0].sortKey)
        assertEquals("Precio de Costo Gs.: 3.500", ordered[0].lineaCostoUnico)
        assertEquals("Precio de Venta Anterior Gs.: 6.000 (42%)", ordered[0].lineaVentaAnterior)
        assertEquals("Precio de Venta Nuevo Gs.: 5.000 (30%)", ordered[0].lineaVentaNueva)

        assertEquals(2, ordered[1].ordenFila)
        assertEquals(2, ordered[1].tipoId)
        assertEquals("El costo de este producto subió", ordered[1].titulo)
        assertEquals(2, ordered[1].lineasProductosFormateadas.size)
        assertTrue(ordered[1].lineasProductosFormateadas[0].contains("1- Yerba Mate 500g"))
        assertTrue(ordered[1].lineasProductosFormateadas[1].contains("2- Yerba Mate Compuesta 500g"))
        assertEquals("Precio de Costo Anterior Gs.: 7.000", ordered[1].lineaCostoAnterior)
        assertEquals("Precio de Costo Nuevo Gs.: 8.000", ordered[1].lineaCostoNuevo)
        assertEquals(3, invoice.nextRowOrder)
    }
}
