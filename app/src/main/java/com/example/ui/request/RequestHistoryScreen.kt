package com.example.ui.request

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PriceRequest
import com.example.ui.catalog.BinaryImportModeDialog
import com.example.ui.components.FastButton
import com.example.ui.components.FastChip
import com.example.ui.components.FastOutlinedButton
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

private enum class HistoryExportFormat {
    PLAIN_TEXT,
    MONOCHROME_IMAGE
}

@Composable
fun RequestHistoryScreen(
    viewModel: RequestViewModel,
    modifier: Modifier = Modifier,
    onEditRequest: (PriceRequest) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)
    val surfaceBg = MaterialTheme.colorScheme.surface
    val surfaceTextColor = getContrastingTextColor(surfaceBg)

    var exportRequestsTarget by remember { mutableStateOf<List<PriceRequest>?>(null) }
    var showBackupMenu by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportBinaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportRequestsBinaryToUri(uri) { result ->
                result.onSuccess { count ->
                    Toast.makeText(
                        context,
                        "Historial exportado ($count solicitudes)",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "Error al exportar historial",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importBinaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoColumnGrid = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Historial (${uiState.allRequests.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = screenTextColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.allRequests.isNotEmpty()) {
                        FastButton(
                            onClick = { exportRequestsTarget = uiState.allRequests },
                            containerColor = BrandBluePrimary,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            minHeight = 34.dp,
                            modifier = Modifier.testTag("history_button_export_all")
                        ) { exportBtnText ->
                            Icon(Icons.Default.Share, contentDescription = null, tint = exportBtnText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Exportar",
                                color = exportBtnText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showBackupMenu = true },
                            modifier = Modifier.testTag("history_backup_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Respaldo de historial", tint = screenTextColor)
                        }

                        DropdownMenu(
                            expanded = showBackupMenu,
                            onDismissRequest = { showBackupMenu = false },
                            containerColor = surfaceBg
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = surfaceTextColor)
                                },
                                text = {
                                    Text(
                                        "Exportar respaldo binario (.bin)",
                                        color = surfaceTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showBackupMenu = false
                                    val safeWorkplace = uiState.workplaceName
                                        .trim()
                                        .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                        .ifBlank { "historial" }
                                    exportBinaryLauncher.launch("historial_${safeWorkplace}.bin")
                                },
                                modifier = Modifier.testTag("menu_export_binary_history")
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = surfaceTextColor)
                                },
                                text = {
                                    Text(
                                        "Importar respaldo binario (.bin)",
                                        color = surfaceTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showBackupMenu = false
                                    importBinaryLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.testTag("menu_import_binary_history")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.allRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sin solicitudes registradas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = screenTextColor
                        )
                        FastOutlinedButton(
                            onClick = { importBinaryLauncher.launch(arrayOf("*/*")) },
                            containerColor = surfaceBg,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier.testTag("history_empty_import_bin_button")
                        ) { contentColor ->
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = contentColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar Historial (.bin)", color = contentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isTwoColumnGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = uiState.allRequests,
                        key = { it.id },
                        contentType = { "history_card" }
                    ) { request ->
                        RequestHistoryCard(
                            request = request,
                            onEdit = { onEditRequest(request) },
                            onExportOptions = {
                                exportRequestsTarget = listOf(request)
                            },
                            onDelete = { viewModel.deleteRequest(request) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = uiState.allRequests,
                        key = { it.id },
                        contentType = { "history_card" }
                    ) { request ->
                        RequestHistoryCard(
                            request = request,
                            onEdit = { onEditRequest(request) },
                            onExportOptions = {
                                exportRequestsTarget = listOf(request)
                            },
                            onDelete = { viewModel.deleteRequest(request) }
                        )
                    }
                }
            }
        }
    }

    if (pendingImportUri != null) {
        val targetUri = pendingImportUri!!
        BinaryImportModeDialog(
            title = "Importar Historial (.bin)",
            description = "¿Cómo desea aplicar las solicitudes del archivo seleccionado sobre el historial actual?",
            onDismiss = { pendingImportUri = null },
            onSelectMode = { replaceAll ->
                pendingImportUri = null
                viewModel.importRequestsBinaryFromUri(targetUri, replaceAll = replaceAll) { result ->
                    result.onSuccess { count ->
                        val modeLabel = if (replaceAll) "reemplazado" else "fusionado"
                        Toast.makeText(
                            context,
                            "Historial $modeLabel ($count solicitudes procesadas)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { err ->
                        Toast.makeText(
                            context,
                            err.message ?: "Error al importar el archivo de historial",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
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
                    FastChip(
                        selected = isTxt,
                        onClick = { exportFormat = HistoryExportFormat.PLAIN_TEXT },
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
                            .testTag("history_export_format_txt")
                    )

                    val isImg = exportFormat == HistoryExportFormat.MONOCHROME_IMAGE
                    FastChip(
                        selected = isImg,
                        onClick = { exportFormat = HistoryExportFormat.MONOCHROME_IMAGE },
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
                            req.lineasProductosFormateadas.forEach { itemLine ->
                                Text(
                                    text = "· $itemLine",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = monoText
                                )
                            }
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
            FastButton(
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
                containerColor = Tipo1Verde,
                modifier = Modifier.testTag("history_export_send_button")
            ) { sendBtnText ->
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

/**
 * Contenedor plano de historial de una sola capa con FastButton y FastOutlinedButton
 * para menor uso de recursos y desplazamiento rápido.
 */
@Composable
fun RequestHistoryCard(
    request: PriceRequest,
    onEdit: () -> Unit,
    onExportOptions: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = getRequestTypeColor(request.tipoId)
    val cardBg = getRequestTypeContainerColor(request.tipoId)
    val textOnCardBg = getContrastingTextColor(cardBg)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(8.dp))
            .border(1.dp, cardColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("history_card_${request.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
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
                    fontWeight = FontWeight.ExtraBold,
                    color = textOnCardBg
                )
            }

            Text(
                text = request.fechaCortaFormateada,
                style = MaterialTheme.typography.labelSmall,
                color = textOnCardBg,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            thickness = 0.5.dp,
            color = cardColor.copy(alpha = 0.4f)
        )

        Text(
            text = "Nombre Producto:",
            style = MaterialTheme.typography.labelSmall,
            color = textOnCardBg,
            fontWeight = FontWeight.Bold
        )

        request.lineasProductosFormateadas.forEachIndexed { idx, item ->
            Text(
                text = "· $item",
                style = if (idx == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                color = textOnCardBg,
                fontWeight = FontWeight.Bold
            )
        }

        val code = request.codigoEfectivo
        if (code != null) {
            Text(
                text = "Código: $code",
                style = MaterialTheme.typography.bodySmall,
                color = textOnCardBg,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            when (request.tipoId) {
                2, 3 -> {
                    Text(
                        text = request.lineaCostoAnterior,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = request.lineaCostoNuevo,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = request.lineaVentaAnterior,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = request.lineaVentaNueva,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textOnCardBg
                    )
                }
                4, 5 -> {
                    Text(
                        text = request.lineaCostoUnico,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = request.lineaVentaAnterior,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = request.lineaVentaNueva,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textOnCardBg
                    )
                }
                else -> {
                    Text(
                        text = request.lineaCostoUnico,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = request.lineaVentaUnica,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textOnCardBg
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("history_delete_${request.id}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = textOnCardBg, modifier = Modifier.size(18.dp))
            }
            FastOutlinedButton(
                onClick = onEdit,
                containerColor = cardBg,
                borderColor = cardColor,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                minHeight = 32.dp,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .testTag("history_edit_${request.id}")
            ) { contentColor ->
                Icon(Icons.Default.Edit, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Editar", color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            FastButton(
                onClick = onExportOptions,
                containerColor = cardColor,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                minHeight = 32.dp,
                modifier = Modifier.testTag("history_share_${request.id}")
            ) { textOnCardColor ->
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = textOnCardColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("TXT / Monocromo", color = textOnCardColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
