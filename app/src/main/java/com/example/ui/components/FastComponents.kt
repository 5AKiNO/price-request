package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.getContrastingTextColor

/**
 * Lista global de porcentajes seleccionables en toda la aplicación (20%, 25%, 30%, 35%, 50%).
 */
val SELECTABLE_PERCENTAGES = listOf(20, 25, 30, 35, 50)

/**
 * Campo de texto plano ultraligero basado en BasicTextField.
 * Elimina SubcomposeLayout, animaciones de etiqueta flotante y múltiples capas Surface
 * de OutlinedTextField para ofrecer respuesta inmediata al abrir el teclado y escribir.
 */
@Composable
fun FastTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = BrandBluePrimary,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val textColor = getContrastingTextColor(containerColor)
    val effectiveBorder = if (isError) Color(0xFFB71C1C) else borderColor
    val shape = RoundedCornerShape(8.dp)

    Column(modifier = modifier) {
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isError) Color(0xFFB71C1C) else textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
            ),
            cursorBrush = SolidColor(effectiveBorder),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 42.dp)
                        .background(containerColor, shape)
                        .border(if (isError) 1.5.dp else 1.dp, effectiveBorder, shape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        CompositionLocalProvider(LocalContentColor provides textColor) {
                            leadingIcon()
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        CompositionLocalProvider(LocalContentColor provides textColor) {
                            trailingIcon()
                        }
                    }
                }
            }
        )

        if (!supportingText.isNullOrEmpty()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isError) Color(0xFFB71C1C) else textColor,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            )
        }
    }
}

/**
 * Chip plano de una sola capa que reemplaza a FilterChip de Material 3.
 * Sin animaciones de elevación ni capas internas duplicadas; aplica contraste automático
 * (texto blanco sobre fondo oscuro y texto negro sobre fondo claro).
 */
@Composable
fun FastChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = BrandBluePrimary,
    unselectedContainerColor: Color = BrandBlueContainer,
    borderColor: Color = selectedContainerColor,
    showCheckIcon: Boolean = false,
    leadingIcon: (@Composable (tint: Color) -> Unit)? = null
) {
    val bg = if (selected) selectedContainerColor else unselectedContainerColor
    val textColor = getContrastingTextColor(bg)
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(shape)
            .background(bg, shape)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            leadingIcon(textColor)
            Spacer(modifier = Modifier.width(4.dp))
        } else if (selected && showCheckIcon) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Botón de acción plano de una sola capa sin sombras de elevación ni Surface anidados.
 */
@Composable
fun FastButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = BrandBluePrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    minHeight: Dp = 42.dp,
    content: @Composable RowScope.(contentColor: Color) -> Unit
) {
    val effectiveBg = if (enabled) containerColor else containerColor.copy(alpha = 0.45f)
    val contentColor = getContrastingTextColor(containerColor)
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(shape)
            .background(effectiveBg, shape)
            .then(
                if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content(contentColor)
        }
    }
}

/**
 * Botón con borde plano de una sola capa que reemplaza a OutlinedButton de Material 3.
 */
@Composable
fun FastOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = BrandBluePrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    minHeight: Dp = 38.dp,
    content: @Composable RowScope.(contentColor: Color) -> Unit
) {
    val contentColor = getContrastingTextColor(containerColor)
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(shape)
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .then(
                if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content(contentColor)
        }
    }
}

/**
 * Casilla de selección plana de una sola capa sin animaciones de transición costosas en listas.
 */
@Composable
fun FastCheckBox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    checkedColor: Color = BrandBluePrimary,
    uncheckedBorderColor: Color = BrandBluePrimary
) {
    val shape = RoundedCornerShape(4.dp)
    val bg = if (checked) checkedColor else Color.Transparent
    val checkTint = getContrastingTextColor(checkedColor)

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(shape)
            .clickable(role = Role.Checkbox, onClick = onCheckedChange)
            .padding(3.dp)
            .background(bg, shape)
            .border(1.5.dp, if (checked) checkedColor else uncheckedBorderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Botón plano de navegación de una sola capa (solo icono, sin texto) para reemplazar
 * NavigationBarItem y NavigationRailItem de Material 3 sin sobrecarga de animaciones ni etiquetas.
 */
@Composable
fun FastNavIconItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = BrandBlueContainer,
    unselectedContainerColor: Color = Color.Transparent,
    borderColor: Color = BrandBluePrimary
) {
    val bg = if (selected) selectedContainerColor else unselectedContainerColor
    val tint = getContrastingTextColor(bg)
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
            .clip(shape)
            .background(bg, shape)
            .then(
                if (selected) Modifier.border(1.5.dp, borderColor, shape)
                else Modifier
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

