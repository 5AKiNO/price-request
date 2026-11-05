package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

val BrandBluePrimary = Color(0xFF0D47A1)
val BrandBlueSecondary = Color(0xFF1565C0)
val BrandBlueContainer = Color(0xFFE3F2FD)
val BrandOnBlueContainer = Color(0xFF0D47A1)

val Tipo1Verde = Color(0xFF1B5E20)
val Tipo1VerdeContainer = Color(0xFFE8F5E9)

val Tipo2Azul = Color(0xFF0D47A1)
val Tipo2AzulContainer = Color(0xFFE3F2FD)

val Tipo3Naranja = Color(0xFFD84315)
val Tipo3NaranjaContainer = Color(0xFFFFF3E0)

val Tipo4Teal = Color(0xFF00695C)
val Tipo4TealContainer = Color(0xFFE0F2F1)

val Tipo5Rojo = Color(0xFFB71C1C)
val Tipo5RojoContainer = Color(0xFFFFEBEE)

val PriceIncreaseBlue = Tipo2Azul
val PriceIncreaseBg = Tipo2AzulContainer

val PriceDropOrange = Tipo3Naranja
val PriceDropOrangeAccent = Color(0xFFFB8C00)
val PriceDropBg = Tipo3NaranjaContainer

val SurfaceLight = Color(0xFFF1F3F5)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF111111)
val TextSecondary = Color(0xFF222222)
val DisabledSurface = Color(0xFFCFD8DC)
val PlaceholderGray = Color(0xFF546E7A)
val ScrimDark = Color(0xFF121316)
val GridGuideLight = Color(0xFFE0E0E0)

val BrandBlueDarkPrimary = Color(0xFF90CAF9)
val BrandBlueDarkSecondary = Color(0xFF64B5F6)
val SurfaceDark = Color(0xFF121316)
val SurfaceDarkCard = Color(0xFF1E2024)
val TextDarkPrimary = Color(0xFFE2E2E6)
val TextDarkSecondary = Color(0xFFC4C7C5)

private val Tipo1LightAccent = Color(0xFF81C784)
private val Tipo2LightAccent = Color(0xFF90CAF9)
private val Tipo3LightAccent = Color(0xFFFFB74D)
private val Tipo4LightAccent = Color(0xFF80CBC4)
private val Tipo5LightAccent = Color(0xFFEF9A9A)

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

fun computeFastLumaScaled(argb: Int): Int {
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    return r * 299 + g * 587 + b * 114
}

fun getContrastingTextColor(backgroundColor: Color): Color {
    val luma = computeFastLumaScaled(backgroundColor.toArgb())
    return if (luma < 140_000) Color.White else TextPrimary
}

fun getLegibleAccentColor(tipoId: Int, surfaceColor: Color): Color {
    val luma = computeFastLumaScaled(surfaceColor.toArgb())
    return if (luma < 110_000) {
        when (tipoId) {
            1 -> Tipo1LightAccent
            2 -> Tipo2LightAccent
            3 -> Tipo3LightAccent
            4 -> Tipo4LightAccent
            5 -> Tipo5LightAccent
            else -> Color.White
        }
    } else {
        getRequestTypeColor(tipoId)
    }
}
