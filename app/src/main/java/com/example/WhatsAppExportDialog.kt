package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    initialMonochromeFormat: Boolean = false,
    onDismiss: () -> Unit,
    buildTextFunction: (products: List<Product>, codigo: Boolean, costo: Boolean, venta: Boolean, margen: Boolean) -> String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var exportFormat by remember(initialMonochromeFormat) {
        mutableStateOf(
            if (initialMonochromeFormat) CatalogExportFormat.MONOCHROME_IMAGE
            else CatalogExportFormat.PLAIN_TEXT
        )
    }
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
                    text = "Formato de envío",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isTxt = exportFormat == CatalogExportFormat.PLAIN_TEXT
                    FastChip(
                        selected = isTxt,
                        onClick = { exportFormat = CatalogExportFormat.PLAIN_TEXT },
                        label = "Texto Plano (TXT)",
                        selectedContainerColor = BrandBluePrimary,
                        unselectedContainerColor = BrandBlueContainer,
                        borderColor = BrandBluePrimary,
                        leadingIcon = { tint ->
                            Icon(
                                imageVector = if (isTxt) Icons.Default.Check else Icons.Default.Description,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_format_txt")
                    )

                    val isImg = exportFormat == CatalogExportFormat.MONOCHROME_IMAGE
                    FastChip(
                        selected = isImg,
                        onClick = { exportFormat = CatalogExportFormat.MONOCHROME_IMAGE },
                        label = "Imagen Monocroma",
                        selectedContainerColor = BrandBluePrimary,
                        unselectedContainerColor = BrandBlueContainer,
                        borderColor = BrandBluePrimary,
                        leadingIcon = { tint ->
                            Icon(
                                imageVector = if (isImg) Icons.Default.Check else Icons.Default.Image,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_format_image")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Campos incluidos (${selectedProducts.size} productos)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = dialogTextColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeCodigo = !includeCodigo }
                ) {
                    FastCheckBox(checked = includeCodigo, onCheckedChange = { includeCodigo = !includeCodigo }, uncheckedBorderColor = dialogTextColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Incluir Código", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeCosto = !includeCosto }
                ) {
                    FastCheckBox(checked = includeCosto, onCheckedChange = { includeCosto = !includeCosto }, uncheckedBorderColor = dialogTextColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Incluir Precio de Costo", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeVenta = !includeVenta }
                ) {
                    FastCheckBox(checked = includeVenta, onCheckedChange = { includeVenta = !includeVenta }, uncheckedBorderColor = dialogTextColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Incluir Precio de Venta", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeMargen = !includeMargen }
                ) {
                    FastCheckBox(checked = includeMargen, onCheckedChange = { includeMargen = !includeMargen }, uncheckedBorderColor = dialogTextColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Incluir % Margen", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (exportFormat == CatalogExportFormat.PLAIN_TEXT) {
                        "Vista previa del texto plano"
                    } else {
                        "Vista previa de imagen monocroma"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )

                if (exportFormat == CatalogExportFormat.PLAIN_TEXT) {
                    val previewBg = BrandBlueContainer
                    val previewTextColor = getContrastingTextColor(previewBg)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(previewBg, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = previewTextColor
                        )
                    }
                } else {
                    val monoBg = Color.White
                    val monoText = Color(0xFF111111)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(monoBg, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Catálogo de Precios - $workplaceName",
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
                        selectedProducts.take(4).forEach { prod ->
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                            }
                        }
                        if (selectedProducts.size > 4) {
                            Text(
                                text = "Y ${selectedProducts.size - 4} productos adicionales en la imagen completa",
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
            FastButton(
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
                                    context = context,
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
                containerColor = Tipo1Verde,
                modifier = Modifier.testTag("whatsapp_button_enviar")
            ) { sendBtnText ->
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = sendBtnText)
                Spacer(modifier = Modifier.width(8.dp))
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
            FastOutlinedButton(
                onClick = onDismiss,
                containerColor = BrandBlueContainer,
                borderColor = BrandBluePrimary
            ) { contentColor ->
                Text("Cancelar", color = contentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
