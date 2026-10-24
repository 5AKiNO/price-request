package com.example.ui.request

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.PriceRequest
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.Tipo1Verde
import com.example.ui.theme.getContrastingTextColor
import com.example.ui.theme.getRequestTypeColor
import com.example.ui.theme.getRequestTypeContainerColor
import com.example.util.ImageUtils
import com.example.util.MathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryExportFormat {
    PLAIN_TEXT,
    MONOCHROME_IMAGE
}

@Composable
fun RequestHistoryScreen(
    viewModel: RequestViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)

    var exportRequestsTarget by remember { mutableStateOf<List<PriceRequest>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = screenTextColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Historial de Solicitudes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = screenTextColor
                )
            }

            if (uiState.allRequests.isNotEmpty()) {
                val exportBtnBg = BrandBluePrimary
                val exportBtnText = getContrastingTextColor(exportBtnBg)
                Button(
                    onClick = { exportRequestsTarget = uiState.allRequests },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = exportBtnBg,
                        contentColor = exportBtnText
                    ),
                    modifier = Modifier.testTag("history_button_export_all")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = exportBtnText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar Historial", color = exportBtnText, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.allRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sin solicitudes registradas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = screenTextColor
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = uiState.allRequests,
                    key = { it.id },
                    contentType = { "history_card" }
                ) { request ->
                    RequestHistoryCard(
                        request = request,
                        onShareAgain = {
                            val uri = request.imagenCompuestaUri?.let { Uri.parse(it) }
                            if (uri != null) {
                                ImageUtils.shareImageOnlyToWhatsApp(context, uri)
                            } else {
                                exportRequestsTarget = listOf(request)
                            }
                        },
                        onExportOptions = {
                            exportRequestsTarget = listOf(request)
                        },
                        onDelete = { viewModel.deleteRequest(request) }
                    )
                }
            }
        }
    }

    if (exportRequestsTarget != null) {
        HistoryExportDialog(
            workplaceName = uiState.workplaceName,
            requests = exportRequestsTarget!!,
            onDismiss = { exportRequestsTarget = null }
        )
    }
}

@Composable
private fun HistoryExportDialog(
    workplaceName: String,
    requests: List<PriceRequest>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportFormat by remember { mutableStateOf(HistoryExportFormat.PLAIN_TEXT) }
    var isGenerating by remember { mutableStateOf(false) }

    val previewText = remember(workplaceName, requests) {
        ImageUtils.buildHistoryPlainText(workplaceName, requests)
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
                    text = "Exportar Historial (${requests.size})",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isTxt = exportFormat == HistoryExportFormat.PLAIN_TEXT
                    val txtBg = if (isTxt) BrandBluePrimary else BrandBlueContainer
                    val txtColor = getContrastingTextColor(txtBg)

                    FilterChip(
                        selected = isTxt,
                        onClick = { exportFormat = HistoryExportFormat.PLAIN_TEXT },
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
                            .testTag("history_export_format_txt")
                    )

                    val isImg = exportFormat == HistoryExportFormat.MONOCHROME_IMAGE
                    val imgBg = if (isImg) BrandBluePrimary else BrandBlueContainer
                    val imgColor = getContrastingTextColor(imgBg)

                    FilterChip(
                        selected = isImg,
                        onClick = { exportFormat = HistoryExportFormat.MONOCHROME_IMAGE },
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
                            .testTag("history_export_format_image")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (exportFormat == HistoryExportFormat.PLAIN_TEXT) {
                        "Vista previa del texto plano:"
                    } else {
                        "Vista previa de Imagen Render Monocroma (Fondo blanco, texto negro):"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )

                if (exportFormat == HistoryExportFormat.PLAIN_TEXT) {
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
                            text = "HISTORIAL DE SOLICITUDES - ${workplaceName.uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = monoText
                        )
                        Text(
                            text = "Total de registros: ${requests.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = monoText
                        )
                        HorizontalDivider(thickness = 2.dp, color = monoText)
                        requests.take(3).forEach { req ->
                            Text(
                                text = req.titulo,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = monoText
                            )
                            Text(
                                text = "Nombre Producto:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = monoText
                            )
                            Text(
                                text = "· ${req.descripcion}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = monoText
                            )
                            Text(
                                text = "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(req.precioCostoNuevo)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = monoText
                            )
                            Text(
                                text = "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(req.precioVentaNuevo)} (${req.porcentajeNuevoRedondeado}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = monoText
                            )
                            HorizontalDivider(thickness = 1.dp, color = monoText)
                        }
                        if (requests.size > 3) {
                            Text(
                                text = "... y ${requests.size - 3} registros más en la imagen completa",
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
                enabled = !isGenerating && requests.isNotEmpty(),
                onClick = {
                    if (exportFormat == HistoryExportFormat.PLAIN_TEXT) {
                        ImageUtils.shareTextToWhatsApp(context, previewText)
                        onDismiss()
                    } else {
                        isGenerating = true
                        coroutineScope.launch {
                            val uri = withContext(Dispatchers.Default) {
                                val bmp = ImageUtils.generateMonochromeHistoryBitmap(
                                    workplaceName = workplaceName,
                                    requests = requests
                                )
                                val fileName = "historial_monocromo_${System.currentTimeMillis()}.jpg"
                                ImageUtils.saveBitmapToCache(context, bmp, fileName)
                            }
                            isGenerating = false
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
                modifier = Modifier.testTag("history_export_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = sendBtnText)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (exportFormat == HistoryExportFormat.PLAIN_TEXT) {
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

@Composable
fun RequestHistoryCard(
    request: PriceRequest,
    onShareAgain: () -> Unit,
    onExportOptions: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = getRequestTypeColor(request.tipoId)
    val cardBg = getRequestTypeContainerColor(request.tipoId)
    val textOnCardColor = getContrastingTextColor(cardColor)
    val textOnCardBg = getContrastingTextColor(cardBg)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${request.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, cardColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = cardBg,
                    border = BorderStroke(1.5.dp, cardColor),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (request.tipoId == 2) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = textOnCardBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (request.tipoId == 3) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = textOnCardBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = request.titulo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                    }
                }

                val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(request.timestamp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = textOnCardBg,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nombre Producto:",
                style = MaterialTheme.typography.labelSmall,
                color = textOnCardBg,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "· ${request.descripcion}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textOnCardBg
            )

            // Variedades adicionales con puntos igual que en la imagen
            if (!request.variedadesMismoCosto.isNullOrBlank()) {
                val lines = request.variedadesMismoCosto.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                lines.forEach { item ->
                    Text(
                        text = "· $item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textOnCardBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val code = request.codigoProducto ?: request.codigo
            if (!code.isNullOrBlank()) {
                Text(
                    text = "Código: $code",
                    style = MaterialTheme.typography.bodySmall,
                    color = textOnCardBg,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!request.stockAnterior.isNullOrBlank() || !request.recargaStock.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val ant = request.stockAnterior ?: "---"
                val rec = request.recargaStock ?: "---"
                Text(
                    text = "Stock Anterior: $ant  |  Recarga: $rec",
                    style = MaterialTheme.typography.bodySmall,
                    color = textOnCardBg,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar precios en el orden exacto según el tipo de solicitud con contraste según el fondo
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (request.tipoId) {
                    2, 3 -> {
                        val cAnt = request.precioCostoAnterior ?: request.precioCostoNuevo
                        val vAnt = request.precioVentaAnterior ?: request.precioVentaNuevo
                        val pAnt = request.porcentajeAnteriorRedondeado
                            ?: MathUtils.calcularPorcentajeRedondeado(cAnt, vAnt)

                        Text(
                            text = "Precio de Costo Anterior Gs.: ${MathUtils.formatearGuaranies(cAnt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Costo Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnCardBg
                        )
                    }
                    4, 5 -> {
                        val vAnt = request.precioVentaAnterior ?: request.precioVentaNuevo
                        val pAnt = request.porcentajeAnteriorRedondeado
                            ?: MathUtils.calcularPorcentajeRedondeado(request.precioCostoNuevo, vAnt)

                        Text(
                            text = "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnCardBg
                        )
                    }
                    else -> {
                        Text(
                            text = "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnCardBg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = textOnCardBg)
                }
                OutlinedButton(
                    onClick = onExportOptions,
                    border = BorderStroke(1.5.dp, cardColor),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = textOnCardBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TXT / Monocromo", color = textOnCardBg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onShareAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardColor,
                        contentColor = textOnCardColor
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = textOnCardColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reenviar Ficha", color = textOnCardColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
