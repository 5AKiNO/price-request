package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Brand Colors for Pricing & Inventory System
val BrandBluePrimary = Color(0xFF0D47A1)
val BrandBlueSecondary = Color(0xFF1565C0)
val BrandBlueContainer = Color(0xFFE3F2FD)
val BrandOnBlueContainer = Color(0xFF0D47A1)

// Sistema de 5 Colores Distintivos de Alta Legibilidad (Sin Lila):
// 1. Falta Registrar: Verde Esmeralda Oscuro
val Tipo1Verde = Color(0xFF1B5E20)
val Tipo1VerdeContainer = Color(0xFFE8F5E9)

// 2. Costo Subió: Azul Intenso
val Tipo2Azul = Color(0xFF0D47A1)
val Tipo2AzulContainer = Color(0xFFE3F2FD)

// 3. Costo Bajó: Naranja Fuerte Oscuro (optimizado para alto contraste sobre fondo claro)
val Tipo3Naranja = Color(0xFFD84315)
val Tipo3NaranjaContainer = Color(0xFFFFF3E0)

// 4. Supera % deseado: Turquesa / Teal Oscuro (Reemplaza al lila para máxima legibilidad)
val Tipo4Teal = Color(0xFF00695C)
val Tipo4TealContainer = Color(0xFFE0F2F1)
val Tipo4Purpura = Tipo4Teal // Alias de compatibilidad
val Tipo4PurpuraContainer = Tipo4TealContainer

// 5. Menor a % deseado: Rojo Carmesí Oscuro
val Tipo5Rojo = Color(0xFFB71C1C)
val Tipo5RojoContainer = Color(0xFFFFEBEE)

// Aliases para compatibilidad
val PriceIncreaseBlue = Tipo2Azul
val PriceIncreaseBg = Tipo2AzulContainer
val PriceIncreaseBorder = Color(0xFF90CAF9)

val PriceDropOrange = Tipo3Naranja
val PriceDropOrangeAccent = Color(0xFFFB8C00)
val PriceDropBg = Tipo3NaranjaContainer
val PriceDropBorder = Color(0xFFFFCC80)

// Neutral & Status Colors
val SurfaceLight = Color(0xFFF8F9FA)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF111111)
val TextSecondary = Color(0xFF222222)
val OutlineColor = Color(0xFFC4C7C5)
val SuccessGreen = Color(0xFF1B5E20)
val SuccessBg = Color(0xFFE8F5E9)

// Dark Palette
val BrandBlueDarkPrimary = Color(0xFF90CAF9)
val BrandBlueDarkSecondary = Color(0xFF64B5F6)
val SurfaceDark = Color(0xFF121316)
val SurfaceDarkCard = Color(0xFF1E2024)
val TextDarkPrimary = Color(0xFFE2E2E6)
val TextDarkSecondary = Color(0xFFC4C7C5)

fun getRequestTypeColor(tipoId: Int): Color {
    return when (tipoId) {
        1 -> Tipo1Verde
        2 -> Tipo2Azul
        3 -> Tipo3Naranja
        4 -> Tipo4Teal
        5 -> Tipo5Rojo
        else -> BrandBluePrimary
    }
}

fun getRequestTypeContainerColor(tipoId: Int): Color {
    return when (tipoId) {
        1 -> Tipo1VerdeContainer
        2 -> Tipo2AzulContainer
        3 -> Tipo3NaranjaContainer
        4 -> Tipo4TealContainer
        5 -> Tipo5RojoContainer
        else -> BrandBlueContainer
    }
}

/**
 * Returns crisp White text when the background/border color is dark (luminance < 0.45)
 * and deep Black (#111111) when the background is light, guaranteeing optimal legibility.
 */
fun getContrastingTextColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() < 0.45f) Color.White else Color(0xFF111111)
}

/**
 * Ensures the request type comment/accent color is always legible against a given surface color.
 */
fun getLegibleAccentColor(tipoId: Int, surfaceColor: Color): Color {
    val baseAccent = getRequestTypeColor(tipoId)
    return if (surfaceColor.luminance() < 0.35f) {
        // On dark backgrounds, use bright high-visibility variants
        when (tipoId) {
            1 -> Color(0xFF81C784)
            2 -> Color(0xFF90CAF9)
            3 -> Color(0xFFFFB74D)
            4 -> Color(0xFF80CBC4)
            5 -> Color(0xFFEF9A9A)
            else -> Color.White
        }
    } else {
        // On light backgrounds, use deep saturated variants with high contrast ratio
        baseAccent
    }
}
