package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDarkPrimary,
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color.White,
    secondary = PriceDropOrangeAccent,
    onSecondary = Color(0xFF111111),
    background = SurfaceDark,
    onBackground = Color.White,
    surface = SurfaceDarkCard,
    onSurface = Color.White,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = Color.White,
    surfaceContainer = SurfaceDarkCard,
    surfaceContainerHigh = SurfaceDarkCard,
    surfaceContainerHighest = SurfaceDarkCard,
    surfaceContainerLow = SurfaceDark,
    surfaceContainerLowest = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBluePrimary,
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = Color(0xFF111111),
    secondary = PriceDropOrange,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = Color(0xFF111111),
    surface = SurfaceCard,
    onSurface = Color(0xFF111111),
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = Color(0xFF111111),
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceCard,
    surfaceContainerLow = SurfaceLight,
    surfaceContainerLowest = SurfaceCard
)

/**
 * Returns OutlinedTextField colors where the text, label, placeholder, icons, and supporting text
 * are automatically White on dark backgrounds and Black (#111111) on light backgrounds.
 */
@Composable
fun adaptiveTextFieldColors(
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = BrandBluePrimary
): TextFieldColors {
    val textColor = getContrastingTextColor(containerColor)
    val placeholderColor = textColor.copy(alpha = 0.7f)
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = placeholderColor,
        errorTextColor = textColor,
        focusedLabelColor = textColor,
        unfocusedLabelColor = textColor,
        disabledLabelColor = placeholderColor,
        errorLabelColor = textColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        focusedLeadingIconColor = textColor,
        unfocusedLeadingIconColor = textColor,
        focusedTrailingIconColor = textColor,
        unfocusedTrailingIconColor = textColor,
        focusedSupportingTextColor = textColor,
        unfocusedSupportingTextColor = textColor,
        errorSupportingTextColor = textColor,
        cursorColor = textColor,
        focusedBorderColor = borderColor,
        unfocusedBorderColor = borderColor
    )
}

@Composable
fun AppPricingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BrandBluePrimary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
