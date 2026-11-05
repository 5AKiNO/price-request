package com.example

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val SELECTABLE_PERCENTAGES = listOf(20, 25, 30, 35, 50)

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
    containerColor: Color = BrandBlueContainer,
    borderColor: Color = BrandBluePrimary,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val effectiveBg = if (isError) Tipo5RojoContainer else containerColor
    val textColor = getContrastingTextColor(effectiveBg)
    val cursorColor = if (isError) Tipo5Rojo else borderColor
    val shape = RoundedCornerShape(8.dp)

    Column(modifier = modifier) {
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isError) Tipo5Rojo else textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
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
            cursorBrush = SolidColor(cursorColor),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 40.dp)
                        .background(effectiveBg, shape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        CompositionLocalProvider(LocalContentColor provides textColor) {
                            leadingIcon()
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PlaceholderGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
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
                color = if (isError) Tipo5Rojo else textColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

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
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(bg, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            leadingIcon(textColor)
            Spacer(modifier = Modifier.width(8.dp))
        } else if (selected && showCheckIcon) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
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

@Composable
fun FastButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = BrandBluePrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    minHeight: Dp = 40.dp,
    content: @Composable RowScope.(contentColor: Color) -> Unit
) {
    val effectiveBg = if (enabled) containerColor else DisabledSurface
    val contentColor = getContrastingTextColor(effectiveBg)
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

@Composable
fun FastOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = BrandBlueContainer,
    borderColor: Color = BrandBluePrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    minHeight: Dp = 40.dp,
    content: @Composable RowScope.(contentColor: Color) -> Unit
) {
    val effectiveBg = if (enabled) containerColor else DisabledSurface
    val contentColor = getContrastingTextColor(effectiveBg)
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

@Composable
fun FastCheckBox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    checkedColor: Color = BrandBluePrimary,
    uncheckedBorderColor: Color = BrandBlueContainer
) {
    val shape = RoundedCornerShape(8.dp)
    val bg = if (checked) checkedColor else BrandBlueContainer
    val checkTint = getContrastingTextColor(bg)

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(shape)
            .background(bg, shape)
            .clickable(role = Role.Checkbox, onClick = onCheckedChange),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FastNavIconItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = BrandBluePrimary,
    unselectedContainerColor: Color = BrandBlueContainer,
    borderColor: Color = BrandBluePrimary
) {
    val bg = if (selected) selectedContainerColor else unselectedContainerColor
    val tint = getContrastingTextColor(bg)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .background(bg, RectangleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(8.dp),
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
