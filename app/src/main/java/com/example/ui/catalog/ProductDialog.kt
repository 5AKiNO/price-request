package com.example.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.FastButton
import com.example.ui.components.FastChip
import com.example.ui.components.FastOutlinedButton
import com.example.ui.components.FastTextField
import com.example.ui.components.SELECTABLE_PERCENTAGES
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.Tipo1Verde
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Código (Opcional)
                FastTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = "Código de Barras / Referencia (Opcional)",
                    placeholder = "Ej: 100648",
                    containerColor = dialogBg,
                    borderColor = BrandBluePrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_codigo")
                )

                // Descripción (Obligatorio, UTF-8 con ñ)
                FastTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        if (it.isNotBlank()) descError = false
                    },
                    label = "Descripción del Producto *",
                    placeholder = "Ej: Bandeja de Cartón Diseño Español N1",
                    containerColor = dialogBg,
                    borderColor = BrandBluePrimary,
                    isError = descError,
                    supportingText = if (descError) "La descripción es obligatoria" else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_descripcion")
                )

                // Precio de Costo (Gs.)
                val parsedCosto = costoText.toLongOrNull()
                FastTextField(
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
                    label = "Precio de Costo (Gs.) *",
                    placeholder = "Ej: 2800",
                    containerColor = dialogBg,
                    borderColor = BrandBluePrimary,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = costoError,
                    supportingText = parsedCosto?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_costo")
                )

                // Grupo de botones seleccionables de 20%, 25%, 30%, 35% y 50%
                Text(
                    text = "Porcentaje seleccionable:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SELECTABLE_PERCENTAGES.forEach { pct ->
                        val isSelected = (calculatedMargin ?: porcentajeSeleccionado) == pct
                        FastChip(
                            selected = isSelected,
                            onClick = { applyPercentage(pct) },
                            label = "$pct%",
                            selectedContainerColor = BrandBluePrimary,
                            unselectedContainerColor = BrandBlueContainer,
                            borderColor = BrandBluePrimary,
                            showCheckIcon = true,
                            modifier = Modifier.testTag("product_pct_chip_$pct")
                        )
                    }
                }

                // Precio de Venta (Gs.)
                val parsedVenta = ventaText.toLongOrNull()
                val ventaSupporting = parsedVenta?.let { v ->
                    val mSuffix = calculatedMargin?.let { " ($it%)" } ?: ""
                    "Gs. ${MathUtils.formatearGuaranies(v)}$mSuffix"
                }
                FastTextField(
                    value = ventaText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        ventaText = digits
                        ventaError = false
                    },
                    label = "Precio de Venta (Gs.) *",
                    placeholder = "Ej: 4000",
                    containerColor = dialogBg,
                    borderColor = BrandBluePrimary,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ventaError,
                    supportingText = ventaSupporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_input_venta")
                )

                // Resumen plano de margen calculado
                if (calculatedMargin != null) {
                    val summaryBg = BrandBlueContainer
                    val summaryText = getContrastingTextColor(summaryBg)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(summaryBg, RoundedCornerShape(8.dp))
                            .border(1.dp, BrandBluePrimary, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = summaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Margen Real Redondeado: $calculatedMargin%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = summaryText
                        )
                    }
                }

                // Botón directo para Guardar y Compartir el producto recién creado
                if (onSaveAndShare != null) {
                    FastButton(
                        onClick = {
                            validateAndRun { id, cod, desc, c, v, p ->
                                onSaveAndShare(id, cod, desc, c, v, p)
                            }
                        },
                        containerColor = Tipo1Verde,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_button_guardar_compartir")
                    ) { shareBtnText ->
                        Icon(Icons.Default.Share, contentDescription = null, tint = shareBtnText, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar y Compartir (TXT o Imagen)", color = shareBtnText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            FastButton(
                onClick = {
                    validateAndRun { id, cod, desc, c, v, p ->
                        onSave(id, cod, desc, c, v, p)
                    }
                },
                containerColor = BrandBluePrimary,
                modifier = Modifier.testTag("product_button_guardar")
            ) { saveBtnText ->
                Text("Guardar Producto", color = saveBtnText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FastOutlinedButton(
                onClick = onDismiss,
                containerColor = dialogBg,
                borderColor = BrandBluePrimary
            ) { contentColor ->
                Text("Cancelar", color = contentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
