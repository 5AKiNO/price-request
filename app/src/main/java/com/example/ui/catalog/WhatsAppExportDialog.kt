package com.example.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.Tipo1Verde
import com.example.ui.theme.getContrastingTextColor
import com.example.util.ImageUtils
import com.example.util.MathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CatalogExportFormat {
    PLAIN_TEXT,
    MONOCHROME_IMAGE
}

@Composable
fun WhatsAppExportDialog(
    workplaceName: String,
    selectedProducts: List<Product>,
    onDismiss: () -> Unit,
    buildTextFunction: (products: List<Product>, codigo: Boolean, costo: Boolean, venta: Boolean, margen: Boolean) -> String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var exportFormat by remember { mutableStateOf(CatalogExportFormat.PLAIN_TEXT) }
    var includeCodigo by remember { mutableStateOf(true) }
    var includeCosto by remember { mutableStateOf(true) }
    var includeVenta by remember { mutableStateOf(true) }
    var includeMargen by remember { mutableStateOf(true) }
    var isGeneratingImage by remember { mutableStateOf(false) }

    val previewText = remember(selectedProducts, workplaceName, includeCodigo, includeCosto, includeVenta, includeMargen) {
        buildTextFunction(selectedProducts, includeCodigo, includeCosto, includeVenta, includeMargen)
    }

    val dialogBg = MaterialTheme.colorScheme.surface
    val dialogTextColor = getContrastingTextColor(dialogBg)
    val checkboxColors = CheckboxDefaults.colors(
        checkedColor = BrandBluePrimary,
        checkmarkColor = getContrastingTextColor(BrandBluePrimary),
        uncheckedColor = dialogTextColor
    )

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
                    text = "Enviar Catálogo (${selectedProducts.size})",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tipo de mensaje a enviar:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )

                // Botones para elegir Texto Plano (TXT) o Imagen Render Monocroma
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isTxt = exportFormat == CatalogExportFormat.PLAIN_TEXT
                    val txtBg = if (isTxt) BrandBluePrimary else BrandBlueContainer
                    val txtColor = getContrastingTextColor(txtBg)

                    FilterChip(
                        selected = isTxt,
                        onClick = { exportFormat = CatalogExportFormat.PLAIN_TEXT },
                        label = {
                            Text(
                                text = "Texto Plano (TXT)",
                                fontWeight = FontWeight.Bold,
                                color = txtColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isTxt) Icons.Default.Check else Icons.Default.Description,
                                contentDescription = null,
                                tint = txtColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBluePrimary,
                            selectedLabelColor = getContrastingTextColor(BrandBluePrimary),
                            containerColor = BrandBlueContainer,
                            labelColor = getContrastingTextColor(BrandBlueContainer)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isTxt,
                            borderColor = BrandBluePrimary,
                            selectedBorderColor = BrandBluePrimary,
                            borderWidth = 1.5.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_format_txt")
                    )

                    val isImg = exportFormat == CatalogExportFormat.MONOCHROME_IMAGE
                    val imgBg = if (isImg) BrandBluePrimary else BrandBlueContainer
                    val imgColor = getContrastingTextColor(imgBg)

                    FilterChip(
                        selected = isImg,
                        onClick = { exportFormat = CatalogExportFormat.MONOCHROME_IMAGE },
                        label = {
                            Text(
                                text = "Imagen Monocroma",
                                fontWeight = FontWeight.Bold,
                                color = imgColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isImg) Icons.Default.Check else Icons.Default.Image,
                                contentDescription = null,
                                tint = imgColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBluePrimary,
                            selectedLabelColor = getContrastingTextColor(BrandBluePrimary),
                            containerColor = BrandBlueContainer,
                            labelColor = getContrastingTextColor(BrandBlueContainer)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isImg,
                            borderColor = BrandBluePrimary,
                            selectedBorderColor = BrandBluePrimary,
                            borderWidth = 1.5.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_format_image")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Campos a incluir (${selectedProducts.size} productos):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = dialogTextColor
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeCodigo, onCheckedChange = { includeCodigo = it }, colors = checkboxColors)
                    Text("Incluir Código", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeCosto, onCheckedChange = { includeCosto = it }, colors = checkboxColors)
                    Text("Incluir Precio de Costo", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeVenta, onCheckedChange = { includeVenta = it }, colors = checkboxColors)
                    Text("Incluir Precio de Venta", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeMargen, onCheckedChange = { includeMargen = it }, colors = checkboxColors)
                    Text("Incluir % Margen", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (exportFormat == CatalogExportFormat.PLAIN_TEXT) {
                        "Vista previa del texto plano:"
                    } else {
                        "Vista previa de Imagen Render Monocroma (Fondo blanco, texto negro):"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )

                if (exportFormat == CatalogExportFormat.PLAIN_TEXT) {
                    val previewBg = Color(0xFFECE5DD)
                    val previewTextColor = getContrastingTextColor(previewBg)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(previewBg, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = previewText,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = previewTextColor
                        )
                    }
                } else {
                    // Monochrome Render Preview (White background, Black text, Black dividing lines)
                    val monoBg = Color.White
                    val monoText = Color(0xFF111111)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(monoBg, RoundedCornerShape(6.dp))
                            .border(2.dp, monoText, RoundedCornerShape(6.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "CATÁLOGO DE PRECIOS - ${workplaceName.uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = monoText
                        )
                        Text(
                            text = "Total de productos incluidos: ${selectedProducts.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = monoText
                        )
                        HorizontalDivider(thickness = 2.dp, color = monoText)
                        selectedProducts.take(4).forEach { prod ->
                            Text(
                                text = "Nombre Producto:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = monoText
                            )
                            Text(
                                text = "· ${prod.descripcion}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = monoText
                            )
                            if (includeCodigo) {
                                Text(
                                    text = "Código: ${prod.codigo ?: "Sin código"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = monoText
                                )
                            }
                            if (includeCosto) {
                                val cStr = prod.precioCosto?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                                Text(
                                    text = "Precio de Costo Gs.: $cStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = monoText
                                )
                            }
                            if (includeVenta) {
                                val vStr = prod.precioVenta?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                                val mStr = if (includeMargen && prod.porcentajeRealRedondeado != null) " (${prod.porcentajeRealRedondeado}%)" else ""
                                Text(
                                    text = "Precio de Venta Gs.: $vStr$mStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = monoText
                                )
                            }
                            HorizontalDivider(thickness = 1.dp, color = monoText)
                        }
                        if (selectedProducts.size > 4) {
                            Text(
                                text = "... y ${selectedProducts.size - 4} productos más en la imagen completa",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = monoText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val sendBtnBg = Tipo1Verde
            val sendBtnText = getContrastingTextColor(sendBtnBg)
            Button(
                enabled = !isGeneratingImage && selectedProducts.isNotEmpty(),
                onClick = {
                    if (exportFormat == CatalogExportFormat.PLAIN_TEXT) {
                        ImageUtils.shareTextToWhatsApp(context, previewText)
                        onDismiss()
                    } else {
                        isGeneratingImage = true
                        coroutineScope.launch {
                            val uri = withContext(Dispatchers.Default) {
                                val bmp = ImageUtils.generateMonochromeCatalogBitmap(
                                    workplaceName = workplaceName,
                                    products = selectedProducts,
                                    includeCodigo = includeCodigo,
                                    includeCosto = includeCosto,
                                    includeVenta = includeVenta,
                                    includeMargen = includeMargen
                                )
                                val fileName = "catalogo_monocromo_${System.currentTimeMillis()}.jpg"
                                ImageUtils.saveBitmapToCache(context, bmp, fileName)
                            }
                            isGeneratingImage = false
                            if (uri != null) {
                                ImageUtils.shareImageOnlyToWhatsApp(context, uri)
                                onDismiss()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = sendBtnBg,
                    contentColor = sendBtnText
                ),
                modifier = Modifier.testTag("whatsapp_button_enviar")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = sendBtnText)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (exportFormat == CatalogExportFormat.PLAIN_TEXT) {
                        "Enviar Texto (TXT)"
                    } else {
                        "Enviar Imagen Render"
                    },
                    color = sendBtnText,
                    fontWeight = FontWeight.Bold
                )
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
