package com.example.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.Tipo1Verde
import com.example.ui.theme.adaptiveTextFieldColors
import com.example.ui.theme.getContrastingTextColor
import com.example.util.MathUtils

@Composable
fun ProductDialog(
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSave: (id: String?, codigo: String?, descripcion: String, costo: Long?, venta: Long?, porcentaje: Int?) -> Unit,
    onSaveAndShare: ((id: String?, codigo: String?, descripcion: String, costo: Long?, venta: Long?, porcentaje: Int?) -> Unit)? = null
) {
    var codigo by remember { mutableStateOf(initialProduct?.codigo ?: "") }
    var descripcion by remember { mutableStateOf(initialProduct?.descripcion ?: "") }
    var costoText by remember { mutableStateOf(initialProduct?.precioCosto?.toString() ?: "") }
    var ventaText by remember { mutableStateOf(initialProduct?.precioVenta?.toString() ?: "") }
    var porcentajeSeleccionado by remember { mutableStateOf(initialProduct?.porcentajePersonalizado) }

    val dialogBg = MaterialTheme.colorScheme.surface
    val dialogTextColor = getContrastingTextColor(dialogBg)
    val textFieldColors = adaptiveTextFieldColors(
        containerColor = dialogBg,
        borderColor = BrandBluePrimary
    )

    val calculatedMargin = remember(costoText, ventaText) {
        val c = costoText.toLongOrNull()
        val v = ventaText.toLongOrNull()
        if (c != null && v != null && c > 0L) {
            MathUtils.calcularPorcentajeRedondeado(c, v)
        } else {
            null
        }
    }

    var descError by remember { mutableStateOf(false) }
    var costoError by remember { mutableStateOf(false) }
    var ventaError by remember { mutableStateOf(false) }

    fun applyPercentage(pct: Int) {
        porcentajeSeleccionado = pct
        val c = costoText.toLongOrNull()
        if (c != null && c > 0L) {
            val autoVenta = MathUtils.calcularVentaDesdePorcentaje(c, pct)
            ventaText = autoVenta.toString()
        }
    }

    fun validateAndRun(action: (String?, String?, String, Long, Long, Int?) -> Unit) {
        val descTrimmed = descripcion.trim()
        if (descTrimmed.isBlank()) descError = true
        val c = costoText.toLongOrNull()
        if (c == null || c <= 0L) costoError = true
        val v = ventaText.toLongOrNull()
        if (v == null || v <= 0L) ventaError = true

        if (descTrimmed.isNotBlank() && c != null && c > 0L && v != null && v > 0L) {
            action(
                initialProduct?.id,
                codigo.trim().takeIf { it.isNotBlank() },
                descTrimmed,
                c,
                v,
                porcentajeSeleccionado
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = dialogTextColor,
        textContentColor = dialogTextColor,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialProduct == null) "Nuevo Producto" else "Editar Producto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = dialogTextColor)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Código (Opcional)
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Código de Barras / Referencia (Opcional)", color = dialogTextColor) },
                    placeholder = { Text("Ej: 100648", color = dialogTextColor) },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_codigo")
                )

                // Descripción (Obligatorio, UTF-8 con ñ)
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        if (it.isNotBlank()) descError = false
                    },
                    label = { Text("Descripción del Producto *", color = dialogTextColor) },
                    placeholder = { Text("Ej: Bandeja de Cartón Diseño Español N1", color = dialogTextColor) },
                    colors = textFieldColors,
                    isError = descError,
                    supportingText = {
                        if (descError) Text("La descripción es obligatoria", color = dialogTextColor, fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_descripcion")
                )

                // Precio de Costo (Gs.)
                OutlinedTextField(
                    value = costoText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        costoText = digits
                        costoError = false
                        val c = digits.toLongOrNull()
                        val p = porcentajeSeleccionado
                        if (c != null && p != null && c > 0L) {
                            val autoVenta = MathUtils.calcularVentaDesdePorcentaje(c, p)
                            ventaText = autoVenta.toString()
                        }
                    },
                    label = { Text("Precio de Costo (Gs.) *", color = dialogTextColor) },
                    placeholder = { Text("Ej: 2800", color = dialogTextColor) },
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = costoError,
                    singleLine = true,
                    supportingText = {
                        val c = costoText.toLongOrNull()
                        if (c != null) {
                            Text("Gs. ${MathUtils.formatearGuaranies(c)}", color = dialogTextColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_costo")
                )

                // Grupo de botones seleccionables únicamente de 20% hasta 50%
                Text(
                    text = "Porcentaje seleccionable:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 25, 30, 40, 50).forEach { pct ->
                        val isSelected = (calculatedMargin ?: porcentajeSeleccionado) == pct
                        val chipBg = if (isSelected) BrandBluePrimary else BrandBlueContainer
                        val chipText = getContrastingTextColor(chipBg)
                        FilterChip(
                            selected = isSelected,
                            onClick = { applyPercentage(pct) },
                            label = {
                                Text(
                                    text = "$pct%",
                                    fontWeight = FontWeight.Bold,
                                    color = chipText
                                )
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = chipText) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBluePrimary,
                                selectedLabelColor = getContrastingTextColor(BrandBluePrimary),
                                containerColor = BrandBlueContainer,
                                labelColor = getContrastingTextColor(BrandBlueContainer)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BrandBluePrimary,
                                selectedBorderColor = BrandBluePrimary,
                                borderWidth = 1.5.dp,
                                selectedBorderWidth = 2.dp
                            )
                        )
                    }
                }

                // Precio de Venta (Gs.)
                OutlinedTextField(
                    value = ventaText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        ventaText = digits
                        ventaError = false
                    },
                    label = { Text("Precio de Venta (Gs.) *", color = dialogTextColor) },
                    placeholder = { Text("Ej: 4000", color = dialogTextColor) },
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ventaError,
                    singleLine = true,
                    supportingText = {
                        val v = ventaText.toLongOrNull()
                        if (v != null) {
                            val mSuffix = calculatedMargin?.let { " ($it%)" } ?: ""
                            Text("Gs. ${MathUtils.formatearGuaranies(v)}$mSuffix", color = dialogTextColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_venta")
                )

                // Card resumen de margen calculado con contraste según su fondo
                if (calculatedMargin != null) {
                    val summaryBg = BrandBlueContainer
                    val summaryText = getContrastingTextColor(summaryBg)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = summaryBg),
                        border = BorderStroke(1.5.dp, BrandBluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = summaryText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Margen Real Redondeado: $calculatedMargin%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = summaryText
                            )
                        }
                    }
                }

                // Botón directo para Guardar y Compartir el producto recién creado
                if (onSaveAndShare != null) {
                    val shareBtnBg = Tipo1Verde
                    val shareBtnText = getContrastingTextColor(shareBtnBg)
                    Button(
                        onClick = {
                            validateAndRun { id, cod, desc, c, v, p ->
                                onSaveAndShare(id, cod, desc, c, v, p)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shareBtnBg,
                            contentColor = shareBtnText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_button_guardar_compartir")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = shareBtnText, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar y Compartir (TXT o Imagen)", color = shareBtnText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            val saveBtnBg = BrandBluePrimary
            val saveBtnText = getContrastingTextColor(saveBtnBg)
            Button(
                onClick = {
                    validateAndRun { id, cod, desc, c, v, p ->
                        onSave(id, cod, desc, c, v, p)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = saveBtnBg,
                    contentColor = saveBtnText
                ),
                modifier = Modifier.testTag("product_button_guardar")
            ) {
                Text("Guardar Producto", color = saveBtnText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.5.dp, BrandBluePrimary)
            ) {
                Text("Cancelar", color = dialogTextColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
