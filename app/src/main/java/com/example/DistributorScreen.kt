package com.example

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DistributorExportFormat {
    MONOCHROME_IMAGE,
    PLAIN_TEXT
}

@Composable
fun DistributorScreen(
    viewModel: DistributorViewModel,
    modifier: Modifier = Modifier,
    onNavigateToNewRequestForDistributor: (String) -> Unit = {},
    onNavigateToRequestWithDistributorItem: (String, DistributorItem) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)
    val surfaceBg = MaterialTheme.colorScheme.surface
    val surfaceTextColor = getContrastingTextColor(surfaceBg)

    var showCreateDistributorDialog by remember { mutableStateOf(false) }
    var showRenameDistributorDialog by remember { mutableStateOf(false) }
    var distributorNameInput by remember { mutableStateOf("") }

    var showAddRowDialog by remember { mutableStateOf(false) }
    var showCatalogPickerDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBackupMenu by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var isRenderingSelection by remember { mutableStateOf(false) }

    val requestTypesFilter = remember {
        listOf(
            1 to "1: Falta Registrar",
            2 to "2: Costo Subió",
            3 to "3: Costo Bajó",
            4 to "4: Excede %",
            5 to "5: Inferior %"
        )
    }

    val exportBinaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDistributorsBinaryToUri(uri) { result ->
                result.onSuccess { count ->
                    Toast.makeText(
                        context,
                        "Boletas exportadas ($count distribuidores)",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "Error al exportar boletas",
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

    fun renderSelectedToWhatsApp() {
        val activeDist = uiState.activeDistributor ?: return
        val selectedItems = viewModel.getSelectedItemsOrdered()
        if (selectedItems.isEmpty() || isRenderingSelection) return
        isRenderingSelection = true
        coroutineScope.launch {
            val uri = withContext(Dispatchers.Default) {
                val bmp = ImageUtils.generateMonochromeDistributorBitmap(
                    workplaceName = uiState.workplaceName,
                    distributorName = activeDist.nombre,
                    items = selectedItems,
                    context = context
                )
                val fileName = "boleta_seleccion_${System.currentTimeMillis()}.jpg"
                ImageUtils.saveBitmapToCache(context, bmp, fileName)
            }
            isRenderingSelection = false
            if (uri != null) {
                ImageUtils.shareImageOnlyToWhatsApp(context, uri)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoColumnGrid = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = screenTextColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.activeDistributor?.let {
                            "Boleta: ${it.nombre} (${uiState.displayedRows.size})"
                        } ?: "Boletas de Distribuidor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = screenTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FastOutlinedButton(
                        onClick = {
                            distributorNameInput = ""
                            showCreateDistributorDialog = true
                        },
                        containerColor = BrandBlueContainer,
                        borderColor = BrandBluePrimary,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        minHeight = 32.dp,
                        modifier = Modifier.testTag("distributor_button_new")
                    ) { contentColor ->
                        Icon(Icons.Default.Add, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Distribuidor",
                            color = contentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.activeDistributor != null && uiState.activeDistributor!!.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FastButton(
                            onClick = { showExportDialog = true },
                            containerColor = Tipo1Verde,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            minHeight = 32.dp,
                            modifier = Modifier.testTag("distributor_button_export")
                        ) { exportBtnText ->
                            Icon(Icons.Default.Share, contentDescription = null, tint = exportBtnText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WhatsApp",
                                color = exportBtnText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showBackupMenu = true },
                            modifier = Modifier.testTag("distributor_backup_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones de distribuidor", tint = screenTextColor)
                        }

                        DropdownMenu(
                            expanded = showBackupMenu,
                            onDismissRequest = { showBackupMenu = false },
                            containerColor = surfaceBg
                        ) {
                            if (uiState.activeDistributor != null) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = surfaceTextColor)
                                    },
                                    text = {
                                        Text(
                                            "Renombrar distribuidor activo",
                                            color = surfaceTextColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        showBackupMenu = false
                                        distributorNameInput = uiState.activeDistributor?.nombre ?: ""
                                        showRenameDistributorDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = surfaceTextColor)
                                    },
                                    text = {
                                        Text(
                                            "Eliminar distribuidor activo",
                                            color = surfaceTextColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        showBackupMenu = false
                                        viewModel.deleteActiveDistributor()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = surfaceTextColor)
                                },
                                text = {
                                    Text(
                                        "Exportar boletas (.bin)",
                                        color = surfaceTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showBackupMenu = false
                                    val safeName = (uiState.activeDistributor?.nombre ?: uiState.workplaceName)
                                        .trim()
                                        .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                        .ifBlank { "distribuidores" }
                                    exportBinaryLauncher.launch("boletas_${safeName}.bin")
                                },
                                modifier = Modifier.testTag("menu_export_binary_distributors")
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = surfaceTextColor)
                                },
                                text = {
                                    Text(
                                        "Importar boletas (.bin)",
                                        color = surfaceTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showBackupMenu = false
                                    importBinaryLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.testTag("menu_import_binary_distributors")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.distributors.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.distributors,
                        key = { it.id },
                        contentType = { "distributor_selector_chip" }
                    ) { dist ->
                        val isSelected = uiState.activeDistributor?.id == dist.id
                        FastChip(
                            selected = isSelected,
                            onClick = { viewModel.selectDistributor(dist.id) },
                            label = "${dist.nombre} (${dist.items.size})",
                            selectedContainerColor = BrandBluePrimary,
                            unselectedContainerColor = BrandBlueContainer,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier.testTag("distributor_chip_${dist.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.activeDistributor == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Cree una boleta de distribuidor para almacenar sus 5 tipos de solicitud ordenados por fila",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = screenTextColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FastButton(
                                onClick = {
                                    distributorNameInput = ""
                                    showCreateDistributorDialog = true
                                },
                                containerColor = BrandBluePrimary,
                                modifier = Modifier.testTag("distributor_empty_create_button")
                            ) { btnText ->
                                Icon(Icons.Default.Add, contentDescription = null, tint = btnText)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear Distribuidor", color = btnText, fontWeight = FontWeight.Bold)
                            }
                            FastOutlinedButton(
                                onClick = { importBinaryLauncher.launch(arrayOf("*/*")) },
                                containerColor = BrandBlueContainer,
                                borderColor = BrandBluePrimary,
                                modifier = Modifier.testTag("distributor_empty_import_button")
                            ) { contentColor ->
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = contentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importar (.bin)", color = contentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FastButton(
                        onClick = {
                            onNavigateToNewRequestForDistributor(uiState.activeDistributor!!.id)
                        },
                        containerColor = BrandBluePrimary,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        minHeight = 32.dp,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("distributor_new_request_5types_button")
                    ) { btnText ->
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = btnText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cargar Solicitud (Fila #${uiState.activeDistributor!!.nextRowOrder})",
                            color = btnText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FastOutlinedButton(
                        onClick = { showCatalogPickerDialog = true },
                        containerColor = BrandBlueContainer,
                        borderColor = BrandBluePrimary,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        minHeight = 32.dp,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("distributor_import_catalog_template_button")
                    ) { contentColor ->
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Plantilla",
                            color = contentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FastButton(
                        onClick = {
                            val onlySelected = uiState.selectedItemIds.isNotEmpty()
                            viewModel.syncToCatalog(onlySelected = onlySelected) { syncedCount ->
                                Toast.makeText(
                                    context,
                                    "Sincronizados $syncedCount productos al Catálogo general",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        containerColor = Tipo1Verde,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        minHeight = 32.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("distributor_sync_catalog_button")
                    ) { btnText ->
                        Icon(Icons.Default.Sync, contentDescription = null, tint = btnText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.selectedItemIds.isNotEmpty()) {
                                "Sincronizar (${uiState.selectedItemIds.size})"
                            } else {
                                "Sincronizar Catálogo"
                            },
                            color = btnText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "filter_all_types") {
                        FastChip(
                            selected = uiState.selectedTipoFilter == null,
                            onClick = { viewModel.setTipoFilter(null) },
                            label = "Todos los tipos",
                            selectedContainerColor = BrandBluePrimary,
                            unselectedContainerColor = BrandBlueContainer,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier.testTag("distributor_filter_tipo_all")
                        )
                    }
                    items(
                        items = requestTypesFilter,
                        key = { it.first },
                        contentType = { "distributor_tipo_filter_chip" }
                    ) { (tipoId, label) ->
                        val isSelected = uiState.selectedTipoFilter == tipoId
                        val chipColor = getRequestTypeColor(tipoId)
                        val chipContainer = getRequestTypeContainerColor(tipoId)
                        FastChip(
                            selected = isSelected,
                            onClick = { viewModel.setTipoFilter(tipoId) },
                            label = label,
                            selectedContainerColor = chipColor,
                            unselectedContainerColor = chipContainer,
                            borderColor = chipColor,
                            modifier = Modifier.testTag("distributor_filter_tipo_$tipoId")
                        )
                    }
                    item(key = "filter_diff_catalog") {
                        FastChip(
                            selected = uiState.onlyWithChanges,
                            onClick = { viewModel.toggleOnlyWithChanges() },
                            label = "Dif. Catálogo",
                            selectedContainerColor = Tipo3Naranja,
                            unselectedContainerColor = BrandBlueContainer,
                            borderColor = BrandBluePrimary,
                            leadingIcon = { tint ->
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("distributor_filter_changes_chip")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FastTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    label = "Buscar por fila, tipo, código o descripción",
                    placeholder = "Doble toque en una tarjeta para seleccionarla",
                    containerColor = surfaceBg,
                    borderColor = BrandBluePrimary,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("distributor_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.selectedItemIds.isNotEmpty()) {
                    val selBg = BrandBlueContainer
                    val selText = getContrastingTextColor(selBg)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(selBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${uiState.selectedItemIds.size} filas marcadas (doble toque)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = selText
                            )
                            Text(
                                text = "Render monocromo con título de solicitud, stock, variedades y precios",
                                style = MaterialTheme.typography.labelSmall,
                                color = selText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FastOutlinedButton(
                                onClick = { viewModel.clearSelection() },
                                containerColor = surfaceBg,
                                borderColor = BrandBluePrimary,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                minHeight = 32.dp,
                                modifier = Modifier.testTag("distributor_clear_selection_button")
                            ) { contentColor ->
                                Text("Limpiar", color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            FastButton(
                                enabled = !isRenderingSelection,
                                onClick = { renderSelectedToWhatsApp() },
                                containerColor = Tipo1Verde,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                minHeight = 32.dp,
                                modifier = Modifier.testTag("distributor_render_selected_whatsapp")
                            ) { btnText ->
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = btnText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Render WhatsApp",
                                    color = btnText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (uiState.displayedRows.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Doble toque en cualquier tarjeta para marcarla y enviar el render a WhatsApp",
                            style = MaterialTheme.typography.labelSmall,
                            color = screenTextColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FastOutlinedButton(
                                onClick = { showAddRowDialog = true },
                                containerColor = BrandBlueContainer,
                                borderColor = BrandBluePrimary,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                minHeight = 32.dp,
                                modifier = Modifier.testTag("distributor_add_row_button")
                            ) { contentColor ->
                                Icon(Icons.Default.Add, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fila Rápida", color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            FastOutlinedButton(
                                onClick = { viewModel.selectAllDisplayedItems() },
                                containerColor = BrandBlueContainer,
                                borderColor = BrandBluePrimary,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                minHeight = 32.dp,
                                modifier = Modifier.testTag("distributor_select_all_button")
                            ) { contentColor ->
                                Icon(Icons.Default.SelectAll, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Todo", color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.displayedRows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No hay solicitudes cargadas en esta boleta para el filtro actual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = screenTextColor
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FastButton(
                                    onClick = {
                                        onNavigateToNewRequestForDistributor(uiState.activeDistributor!!.id)
                                    },
                                    containerColor = BrandBluePrimary
                                ) { btnText ->
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = btnText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cargar Solicitud (5 Tipos)", color = btnText, fontWeight = FontWeight.Bold)
                                }
                                FastOutlinedButton(
                                    onClick = { showCatalogPickerDialog = true },
                                    containerColor = BrandBlueContainer,
                                    borderColor = BrandBluePrimary
                                ) { contentColor ->
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = contentColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Plantilla Catálogo", color = contentColor, fontWeight = FontWeight.Bold)
                                }
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
                            items = uiState.displayedRows,
                            key = { it.item.id },
                            contentType = { "distributor_row_card" }
                        ) { rowDisplay ->
                            DistributorRowCard(
                                rowDisplay = rowDisplay,
                                isSelected = uiState.selectedItemIds.contains(rowDisplay.item.id),
                                isEditingInline = uiState.editingRowId == rowDisplay.item.id,
                                onToggleSelect = { viewModel.toggleItemSelection(rowDisplay.item.id) },
                                onToggleInlineEdit = { viewModel.setEditingRow(rowDisplay.item.id) },
                                onShiftOrder = { delta -> viewModel.shiftRowOrder(rowDisplay.item.id, delta) },
                                onSaveInline = { ordenFila, tipoId, codigo, desc, costoAnt, costo, ventaAnt, venta, pct ->
                                    viewModel.upsertRowItem(
                                        existingItem = rowDisplay.item,
                                        ordenFila = ordenFila,
                                        tipoId = tipoId,
                                        codigo = codigo,
                                        descripcion = desc,
                                        precioCostoAnterior = costoAnt,
                                        precioCosto = costo,
                                        precioVentaAnterior = ventaAnt,
                                        precioVenta = venta,
                                        porcentajeRedondeado = pct
                                    )
                                    viewModel.setEditingRow(null)
                                },
                                onOpenInRequest = {
                                    val distId = uiState.activeDistributor?.id ?: return@DistributorRowCard
                                    onNavigateToRequestWithDistributorItem(distId, rowDisplay.item)
                                },
                                onDelete = { viewModel.removeRowItem(rowDisplay.item.id) }
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
                            items = uiState.displayedRows,
                            key = { it.item.id },
                            contentType = { "distributor_row_card" }
                        ) { rowDisplay ->
                            DistributorRowCard(
                                rowDisplay = rowDisplay,
                                isSelected = uiState.selectedItemIds.contains(rowDisplay.item.id),
                                isEditingInline = uiState.editingRowId == rowDisplay.item.id,
                                onToggleSelect = { viewModel.toggleItemSelection(rowDisplay.item.id) },
                                onToggleInlineEdit = { viewModel.setEditingRow(rowDisplay.item.id) },
                                onShiftOrder = { delta -> viewModel.shiftRowOrder(rowDisplay.item.id, delta) },
                                onSaveInline = { ordenFila, tipoId, codigo, desc, costoAnt, costo, ventaAnt, venta, pct ->
                                    viewModel.upsertRowItem(
                                        existingItem = rowDisplay.item,
                                        ordenFila = ordenFila,
                                        tipoId = tipoId,
                                        codigo = codigo,
                                        descripcion = desc,
                                        precioCostoAnterior = costoAnt,
                                        precioCosto = costo,
                                        precioVentaAnterior = ventaAnt,
                                        precioVenta = venta,
                                        porcentajeRedondeado = pct
                                    )
                                    viewModel.setEditingRow(null)
                                },
                                onOpenInRequest = {
                                    val distId = uiState.activeDistributor?.id ?: return@DistributorRowCard
                                    onNavigateToRequestWithDistributorItem(distId, rowDisplay.item)
                                },
                                onDelete = { viewModel.removeRowItem(rowDisplay.item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDistributorDialog) {
        val dialogBg = MaterialTheme.colorScheme.surface
        val dialogText = getContrastingTextColor(dialogBg)
        AlertDialog(
            onDismissRequest = { showCreateDistributorDialog = false },
            containerColor = dialogBg,
            titleContentColor = dialogText,
            textContentColor = dialogText,
            title = {
                Text(
                    text = "Nuevo Distribuidor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ingrese el nombre del distribuidor para almacenar sus 5 tipos de solicitud ordenados por filas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = dialogText
                    )
                    FastTextField(
                        value = distributorNameInput,
                        onValueChange = { distributorNameInput = it },
                        label = "Nombre del Distribuidor *",
                        placeholder = "Ej: Distribuidora San José",
                        containerColor = dialogBg,
                        borderColor = BrandBluePrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("distributor_create_name_input")
                    )
                }
            },
            confirmButton = {
                FastButton(
                    onClick = {
                        if (distributorNameInput.trim().isNotBlank()) {
                            viewModel.createDistributor(distributorNameInput)
                            showCreateDistributorDialog = false
                        }
                    },
                    containerColor = BrandBluePrimary,
                    modifier = Modifier.testTag("distributor_create_confirm_button")
                ) { btnText ->
                    Text("Crear Boleta", color = btnText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                FastOutlinedButton(
                    onClick = { showCreateDistributorDialog = false },
                    containerColor = BrandBlueContainer,
                    borderColor = BrandBluePrimary
                ) { contentColor ->
                    Text("Cancelar", color = contentColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRenameDistributorDialog) {
        val dialogBg = MaterialTheme.colorScheme.surface
        val dialogText = getContrastingTextColor(dialogBg)
        AlertDialog(
            onDismissRequest = { showRenameDistributorDialog = false },
            containerColor = dialogBg,
            titleContentColor = dialogText,
            textContentColor = dialogText,
            title = {
                Text(
                    text = "Renombrar Distribuidor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogText
                )
            },
            text = {
                FastTextField(
                    value = distributorNameInput,
                    onValueChange = { distributorNameInput = it },
                    label = "Nombre del Distribuidor *",
                    containerColor = dialogBg,
                    borderColor = BrandBluePrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                FastButton(
                    onClick = {
                        if (distributorNameInput.trim().isNotBlank()) {
                            viewModel.renameActiveDistributor(distributorNameInput)
                            showRenameDistributorDialog = false
                        }
                    },
                    containerColor = BrandBluePrimary
                ) { btnText ->
                    Text("Guardar", color = btnText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                FastOutlinedButton(
                    onClick = { showRenameDistributorDialog = false },
                    containerColor = BrandBlueContainer,
                    borderColor = BrandBluePrimary
                ) { contentColor ->
                    Text("Cancelar", color = contentColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddRowDialog && uiState.activeDistributor != null) {
        AddDistributorRowDialog(
            suggestedOrder = uiState.activeDistributor!!.nextRowOrder,
            onDismiss = { showAddRowDialog = false },
            onSave = { ordenFila, tipoId, codigo, desc, costoAnt, costo, ventaAnt, venta, pct ->
                viewModel.upsertRowItem(
                    existingItem = null,
                    ordenFila = ordenFila,
                    tipoId = tipoId,
                    codigo = codigo,
                    descripcion = desc,
                    precioCostoAnterior = costoAnt,
                    precioCosto = costo,
                    precioVentaAnterior = ventaAnt,
                    precioVenta = venta,
                    porcentajeRedondeado = pct
                )
                showAddRowDialog = false
            }
        )
    }

    if (showCatalogPickerDialog && uiState.activeDistributor != null) {
        CatalogTemplatePickerDialog(
            catalogProducts = uiState.catalogProducts,
            existingItems = uiState.activeDistributor!!.items,
            onDismiss = { showCatalogPickerDialog = false },
            onImportSelected = { productIds ->
                viewModel.importProductsFromCatalogToTemplate(productIds) { count ->
                    Toast.makeText(
                        context,
                        "Agregados $count productos a la plantilla del distribuidor",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showCatalogPickerDialog = false
            }
        )
    }

    if (showExportDialog && uiState.activeDistributor != null) {
        DistributorExportDialog(
            workplaceName = uiState.workplaceName,
            distributor = uiState.activeDistributor!!,
            selectedItemIds = uiState.selectedItemIds,
            onDismiss = { showExportDialog = false }
        )
    }

    if (pendingImportUri != null) {
        val targetUri = pendingImportUri!!
        BinaryImportModeDialog(
            title = "Importar Boletas de Distribuidor (.bin)",
            description = "¿Cómo desea aplicar las boletas del archivo seleccionado sobre los distribuidores actuales?",
            onDismiss = { pendingImportUri = null },
            onSelectMode = { replaceAll ->
                pendingImportUri = null
                viewModel.importDistributorsBinaryFromUri(targetUri, replaceAll = replaceAll) { result ->
                    result.onSuccess { count ->
                        val modeLabel = if (replaceAll) "reemplazadas" else "fusionadas"
                        Toast.makeText(
                            context,
                            "Boletas $modeLabel ($count distribuidores procesados)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { err ->
                        Toast.makeText(
                            context,
                            err.message ?: "Error al importar boletas",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DistributorRowCard(
    rowDisplay: DistributorRowDisplay,
    isSelected: Boolean,
    isEditingInline: Boolean,
    onToggleSelect: () -> Unit,
    onToggleInlineEdit: () -> Unit,
    onShiftOrder: (Int) -> Unit,
    onSaveInline: (Int, Int, String?, String, Long?, Long?, Long?, Long?, Int?) -> Unit,
    onOpenInRequest: () -> Unit,
    onDelete: () -> Unit
) {
    val item = rowDisplay.item
    val cardColor = getRequestTypeColor(item.tipoId)
    val cardBg = getRequestTypeContainerColor(item.tipoId)
    val textOnCardBg = getContrastingTextColor(cardBg)

    var ordenInput by remember(item.id, item.ordenFila, isEditingInline) {
        mutableStateOf(item.ordenFila.toString())
    }
    var tipoInput by remember(item.id, item.tipoId, isEditingInline) {
        mutableIntStateOf(item.tipoId)
    }
    var codigoInput by remember(item.id, item.codigo, isEditingInline) {
        mutableStateOf(item.codigo ?: "")
    }
    var descInput by remember(item.id, item.descripcion, isEditingInline) {
        mutableStateOf(item.descripcion)
    }
    var costoAntInput by remember(item.id, item.precioCostoAnterior, isEditingInline) {
        mutableStateOf(item.precioCostoAnterior?.toString() ?: "")
    }
    var costoInput by remember(item.id, item.precioCostoNuevo, isEditingInline) {
        mutableStateOf(if (item.precioCostoNuevo > 0L) item.precioCostoNuevo.toString() else "")
    }
    var ventaAntInput by remember(item.id, item.precioVentaAnterior, isEditingInline) {
        mutableStateOf(item.precioVentaAnterior?.toString() ?: "")
    }
    var ventaInput by remember(item.id, item.precioVentaResultante, isEditingInline) {
        mutableStateOf(if (item.precioVentaResultante > 0L) item.precioVentaResultante.toString() else "")
    }
    var pctInput by remember(item.id, item.porcentajeMargen, isEditingInline) {
        mutableStateOf<Int?>(item.porcentajeMargen)
    }

    val requestTypesShort = remember {
        listOf(
            1 to "1: Falta Reg.",
            2 to "2: Costo Subió",
            3 to "3: Costo Bajó",
            4 to "4: Excede %",
            5 to "5: Inferior %"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onToggleInlineEdit() },
                onDoubleClick = { onToggleSelect() }
            )
            .padding(16.dp)
            .testTag("distributor_row_${item.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                val badgeBg = cardColor
                val badgeText = getContrastingTextColor(badgeBg)
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Fila #${item.ordenFila}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeText
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.tipoId == 2) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = textOnCardBg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (item.tipoId == 3) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = textOnCardBg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = item.titulo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnCardBg
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isSelected) {
                            Text(
                                text = "MARCADO PARA RENDER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = cardColor
                            )
                        } else if (rowDisplay.hasCatalogDifference) {
                            Text(
                                text = "Dif. Catálogo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textOnCardBg
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onShiftOrder(-1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Subir fila",
                        tint = textOnCardBg,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onShiftOrder(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Bajar fila",
                        tint = textOnCardBg,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onOpenInRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("distributor_row_to_request_${item.id}")
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Editar solicitud completa",
                        tint = cardColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onToggleInlineEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("distributor_row_edit_${item.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edición rápida en línea",
                        tint = textOnCardBg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item.lineasProductosFormateadas.forEachIndexed { idx, bulletLine ->
            Text(
                text = "· $bulletLine",
                style = if (idx == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                color = textOnCardBg,
                fontWeight = FontWeight.Bold
            )
        }

        val code = item.codigoEfectivo
        if (code != null) {
            Text(
                text = "Código: $code",
                style = MaterialTheme.typography.bodySmall,
                color = textOnCardBg,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (item.tipoId) {
                2, 3 -> {
                    Text(
                        text = item.lineaCostoAnterior,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = item.lineaCostoNuevo,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = item.lineaVentaAnterior,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = item.lineaVentaNueva,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textOnCardBg
                    )
                }
                4, 5 -> {
                    Text(
                        text = item.lineaCostoUnico,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = item.lineaVentaAnterior,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = item.lineaVentaNueva,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textOnCardBg
                    )
                }
                else -> {
                    Text(
                        text = item.lineaCostoUnico,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnCardBg
                    )
                    Text(
                        text = item.lineaVentaUnica,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textOnCardBg
                    )
                }
            }
        }

        if (isEditingInline) {
            val inlinePanelBg = MaterialTheme.colorScheme.surface
            val inlinePanelText = getContrastingTextColor(inlinePanelBg)
            val activeEditColor = getRequestTypeColor(tipoInput)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(inlinePanelBg, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Edición Rápida de Fila #${item.ordenFila}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = inlinePanelText
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    requestTypesShort.forEach { (tid, tLabel) ->
                        val cCol = getRequestTypeColor(tid)
                        val cBg = getRequestTypeContainerColor(tid)
                        FastChip(
                            selected = tipoInput == tid,
                            onClick = { tipoInput = tid },
                            label = tLabel,
                            selectedContainerColor = cCol,
                            unselectedContainerColor = cBg,
                            borderColor = cCol
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FastTextField(
                        value = ordenInput,
                        onValueChange = { ordenInput = it.filter { ch -> ch.isDigit() } },
                        label = "Orden Fila",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = inlinePanelBg,
                        borderColor = activeEditColor,
                        modifier = Modifier.weight(0.4f)
                    )
                    FastTextField(
                        value = codigoInput,
                        onValueChange = { codigoInput = it },
                        label = "Código",
                        containerColor = inlinePanelBg,
                        borderColor = activeEditColor,
                        modifier = Modifier.weight(0.6f)
                    )
                }

                FastTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = "Descripción *",
                    containerColor = inlinePanelBg,
                    borderColor = activeEditColor,
                    modifier = Modifier.fillMaxWidth()
                )

                if (tipoInput in 2..3) {
                    FastTextField(
                        value = costoAntInput,
                        onValueChange = { costoAntInput = it.filter { ch -> ch.isDigit() } },
                        label = "Costo Anterior (Gs.)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = inlinePanelBg,
                        borderColor = activeEditColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (tipoInput in 2..5) {
                    FastTextField(
                        value = ventaAntInput,
                        onValueChange = { ventaAntInput = it.filter { ch -> ch.isDigit() } },
                        label = "Venta Anterior (Gs.)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = inlinePanelBg,
                        borderColor = activeEditColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FastTextField(
                        value = costoInput,
                        onValueChange = { newVal ->
                            val digits = newVal.filter { it.isDigit() }
                            costoInput = digits
                            val c = digits.toLongOrNull()
                            val v = ventaInput.toLongOrNull()
                            if (c != null && c > 0L && pctInput != null && (v == null || v <= 0L)) {
                                val autoV = MathUtils.calcularVentaDesdePorcentaje(c, pctInput!!)
                                ventaInput = autoV.toString()
                            } else if (c != null && c > 0L && v != null && v > 0L) {
                                pctInput = MathUtils.calcularPorcentajeRedondeado(c, v)
                            }
                        },
                        label = if (tipoInput in 2..3) "Costo Nuevo (Gs.)" else "Costo (Gs.)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = inlinePanelBg,
                        borderColor = activeEditColor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("distributor_inline_costo_${item.id}")
                    )

                    FastTextField(
                        value = ventaInput,
                        onValueChange = { newVal ->
                            val digits = newVal.filter { it.isDigit() }
                            ventaInput = digits
                            val c = costoInput.toLongOrNull()
                            val v = digits.toLongOrNull()
                            if (c != null && c > 0L && v != null && v > 0L) {
                                pctInput = MathUtils.calcularPorcentajeRedondeado(c, v)
                            }
                        },
                        label = if (tipoInput in 2..5) "Venta Nueva (Gs.)" else "Venta (Gs.)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = inlinePanelBg,
                        borderColor = activeEditColor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("distributor_inline_venta_${item.id}")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SELECTABLE_PERCENTAGES.forEach { pct ->
                        val selected = pctInput == pct
                        FastChip(
                            selected = selected,
                            onClick = {
                                pctInput = pct
                                val c = costoInput.toLongOrNull()
                                if (c != null && c > 0L) {
                                    ventaInput = MathUtils.calcularVentaDesdePorcentaje(c, pct).toString()
                                }
                            },
                            label = "$pct%",
                            selectedContainerColor = activeEditColor,
                            unselectedContainerColor = getRequestTypeContainerColor(tipoInput),
                            borderColor = activeEditColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar fila", tint = inlinePanelText)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FastOutlinedButton(
                            onClick = onOpenInRequest,
                            containerColor = getRequestTypeContainerColor(tipoInput),
                            borderColor = activeEditColor,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            minHeight = 32.dp
                        ) { contentColor ->
                            Text("Editar en Solicitud", color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        FastButton(
                            onClick = {
                                val ord = ordenInput.toIntOrNull()?.coerceAtLeast(1) ?: item.ordenFila
                                val cAnt = costoAntInput.toLongOrNull()
                                val c = costoInput.toLongOrNull()
                                val vAnt = ventaAntInput.toLongOrNull()
                                val v = ventaInput.toLongOrNull()
                                onSaveInline(ord, tipoInput, codigoInput, descInput, cAnt, c, vAnt, v, pctInput)
                            },
                            containerColor = activeEditColor,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            minHeight = 32.dp,
                            modifier = Modifier.testTag("distributor_inline_save_${item.id}")
                        ) { btnText ->
                            Icon(Icons.Default.Check, contentDescription = null, tint = btnText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Fila", color = btnText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDistributorRowDialog(
    suggestedOrder: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String?, String, Long?, Long?, Long?, Long?, Int?) -> Unit
) {
    var ordenInput by remember { mutableStateOf(suggestedOrder.toString()) }
    var tipoInput by remember { mutableIntStateOf(1) }
    var codigoInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var costoAntInput by remember { mutableStateOf("") }
    var costoInput by remember { mutableStateOf("") }
    var ventaAntInput by remember { mutableStateOf("") }
    var ventaInput by remember { mutableStateOf("") }
    var pctInput by remember { mutableStateOf<Int?>(25) }

    val requestTypesShort = remember {
        listOf(
            1 to "1: Falta Reg.",
            2 to "2: Costo Subió",
            3 to "3: Costo Bajó",
            4 to "4: Excede %",
            5 to "5: Inferior %"
        )
    }

    val dialogBg = MaterialTheme.colorScheme.surface
    val dialogText = getContrastingTextColor(dialogBg)
    val activeColor = getRequestTypeColor(tipoInput)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = dialogText,
        textContentColor = dialogText,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agregar Fila a la Boleta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogText
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = dialogText)
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
                    text = "Tipo de Solicitud (1 a 5)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogText
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    requestTypesShort.forEach { (tid, tLabel) ->
                        val cCol = getRequestTypeColor(tid)
                        val cBg = getRequestTypeContainerColor(tid)
                        FastChip(
                            selected = tipoInput == tid,
                            onClick = { tipoInput = tid },
                            label = tLabel,
                            selectedContainerColor = cCol,
                            unselectedContainerColor = cBg,
                            borderColor = cCol
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FastTextField(
                        value = ordenInput,
                        onValueChange = { ordenInput = it.filter { ch -> ch.isDigit() } },
                        label = "Orden de Fila *",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = dialogBg,
                        borderColor = activeColor,
                        modifier = Modifier
                            .weight(0.4f)
                            .testTag("distributor_dialog_row_order")
                    )
                    FastTextField(
                        value = codigoInput,
                        onValueChange = { codigoInput = it },
                        label = "Código (Opcional)",
                        containerColor = dialogBg,
                        borderColor = activeColor,
                        modifier = Modifier
                            .weight(0.6f)
                            .testTag("distributor_dialog_row_code")
                    )
                }

                FastTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = "Descripción del Producto *",
                    placeholder = "Ej: Gaseosa 2L",
                    containerColor = dialogBg,
                    borderColor = activeColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("distributor_dialog_row_desc")
                )

                if (tipoInput in 2..3) {
                    FastTextField(
                        value = costoAntInput,
                        onValueChange = { costoAntInput = it.filter { ch -> ch.isDigit() } },
                        label = "Precio de Costo Anterior (Gs.)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = dialogBg,
                        borderColor = activeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (tipoInput in 2..5) {
                    FastTextField(
                        value = ventaAntInput,
                        onValueChange = { ventaAntInput = it.filter { ch -> ch.isDigit() } },
                        label = "Precio de Venta Anterior (Gs.)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        containerColor = dialogBg,
                        borderColor = activeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                FastTextField(
                    value = costoInput,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }
                        costoInput = digits
                        val c = digits.toLongOrNull()
                        if (c != null && c > 0L && pctInput != null) {
                            ventaInput = MathUtils.calcularVentaDesdePorcentaje(c, pctInput!!).toString()
                        }
                    },
                    label = if (tipoInput in 2..3) "Precio de Costo Nuevo (Gs.)" else "Precio de Costo (Gs.)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    containerColor = dialogBg,
                    borderColor = activeColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("distributor_dialog_row_costo")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SELECTABLE_PERCENTAGES.forEach { pct ->
                        FastChip(
                            selected = pctInput == pct,
                            onClick = {
                                pctInput = pct
                                val c = costoInput.toLongOrNull()
                                if (c != null && c > 0L) {
                                    ventaInput = MathUtils.calcularVentaDesdePorcentaje(c, pct).toString()
                                }
                            },
                            label = "$pct%",
                            selectedContainerColor = activeColor,
                            unselectedContainerColor = getRequestTypeContainerColor(tipoInput),
                            borderColor = activeColor
                        )
                    }
                }

                FastTextField(
                    value = ventaInput,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }
                        ventaInput = digits
                        val c = costoInput.toLongOrNull()
                        val v = digits.toLongOrNull()
                        if (c != null && c > 0L && v != null && v > 0L) {
                            pctInput = MathUtils.calcularPorcentajeRedondeado(c, v)
                        }
                    },
                    label = if (tipoInput in 2..5) "Precio de Venta Nuevo (Gs.)" else "Precio de Venta Resultante (Gs.)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    containerColor = dialogBg,
                    borderColor = activeColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("distributor_dialog_row_venta")
                )
            }
        },
        confirmButton = {
            FastButton(
                onClick = {
                    if (descInput.trim().isNotBlank()) {
                        val ord = ordenInput.toIntOrNull()?.coerceAtLeast(1) ?: suggestedOrder
                        onSave(
                            ord,
                            tipoInput,
                            codigoInput,
                            descInput,
                            costoAntInput.toLongOrNull(),
                            costoInput.toLongOrNull(),
                            ventaAntInput.toLongOrNull(),
                            ventaInput.toLongOrNull(),
                            pctInput
                        )
                    }
                },
                containerColor = activeColor,
                modifier = Modifier.testTag("distributor_dialog_row_save")
            ) { btnText ->
                Text("Agregar a Boleta", color = btnText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FastOutlinedButton(
                onClick = onDismiss,
                containerColor = getRequestTypeContainerColor(tipoInput),
                borderColor = activeColor
            ) { contentColor ->
                Text("Cancelar", color = contentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun CatalogTemplatePickerDialog(
    catalogProducts: List<Product>,
    existingItems: List<DistributorItem>,
    onDismiss: () -> Unit,
    onImportSelected: (Set<String>) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val existingCodes = remember(existingItems) {
        existingItems.mapNotNull { it.codigo?.trim()?.lowercase() }.toSet()
    }
    val existingDescs = remember(existingItems) {
        existingItems.map { it.descripcion.trim().lowercase() }.toSet()
    }

    val filteredProducts = remember(catalogProducts, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) catalogProducts
        else catalogProducts.filter {
            it.descripcion.lowercase().contains(q) || it.codigo?.lowercase()?.contains(q) == true
        }
    }

    val dialogBg = MaterialTheme.colorScheme.surface
    val dialogText = getContrastingTextColor(dialogBg)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = dialogText,
        textContentColor = dialogText,
        title = {
            Text(
                text = "Plantilla desde Catálogo (${selectedIds.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = dialogText
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FastTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = "Buscar en Catálogo",
                    containerColor = dialogBg,
                    borderColor = BrandBluePrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredProducts,
                        key = { it.id }
                    ) { prod ->
                        val isSelected = selectedIds.contains(prod.id)
                        val alreadyInBoleta = (prod.codigo?.trim()?.lowercase()?.let { existingCodes.contains(it) } == true) ||
                            existingDescs.contains(prod.descripcion.trim().lowercase())
                        val rowBg = if (isSelected) BrandBlueContainer else MaterialTheme.colorScheme.background
                        val rowText = getContrastingTextColor(rowBg)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedIds = if (isSelected) selectedIds - prod.id else selectedIds + prod.id
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.descripcion,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = rowText
                                )
                                Text(
                                    text = buildString {
                                        if (!prod.codigo.isNullOrBlank()) append("Cód: ${prod.codigo} · ")
                                        append("Venta: ${prod.precioVenta?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "---"}")
                                        if (alreadyInBoleta) append(" (Ya en boleta)")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = rowText
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BrandBluePrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FastButton(
                enabled = selectedIds.isNotEmpty(),
                onClick = { onImportSelected(selectedIds) },
                containerColor = BrandBluePrimary
            ) { btnText ->
                Text("Cargar (${selectedIds.size})", color = btnText, fontWeight = FontWeight.Bold)
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

@Composable
private fun DistributorExportDialog(
    workplaceName: String,
    distributor: DistributorInvoice,
    selectedItemIds: Set<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportFormat by remember { mutableStateOf(DistributorExportFormat.MONOCHROME_IMAGE) }
    var onlySelected by remember { mutableStateOf(selectedItemIds.isNotEmpty()) }
    var isGenerating by remember { mutableStateOf(false) }

    val itemsToExport: List<DistributorItem> = remember(distributor, selectedItemIds, onlySelected) {
        val ordered = distributor.sortedItems
        if (onlySelected && selectedItemIds.isNotEmpty()) {
            ordered.filter { item -> selectedItemIds.contains(item.id) }
        } else {
            ordered
        }
    }

    val previewText = remember(workplaceName, distributor.nombre, itemsToExport) {
        ImageUtils.buildDistributorPlainText(workplaceName, distributor.nombre, itemsToExport)
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
                    text = "Exportar Boleta (${itemsToExport.size})",
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
                if (selectedItemIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FastChip(
                            selected = onlySelected,
                            onClick = { onlySelected = true },
                            label = "Marcados (${selectedItemIds.size})",
                            selectedContainerColor = BrandBluePrimary,
                            unselectedContainerColor = BrandBlueContainer,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        FastChip(
                            selected = !onlySelected,
                            onClick = { onlySelected = false },
                            label = "Boleta completa (${distributor.items.size})",
                            selectedContainerColor = BrandBluePrimary,
                            unselectedContainerColor = BrandBlueContainer,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isImg = exportFormat == DistributorExportFormat.MONOCHROME_IMAGE
                    FastChip(
                        selected = isImg,
                        onClick = { exportFormat = DistributorExportFormat.MONOCHROME_IMAGE },
                        label = "Imagen Tabla",
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
                        modifier = Modifier.weight(1f)
                    )

                    val isTxt = exportFormat == DistributorExportFormat.PLAIN_TEXT
                    FastChip(
                        selected = isTxt,
                        onClick = { exportFormat = DistributorExportFormat.PLAIN_TEXT },
                        label = "Texto Plano",
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
                        modifier = Modifier.weight(1f)
                    )
                }

                val previewBg = Color.White
                val previewTextCol = Color(0xFF111111)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = previewTextCol
                    )
                }
            }
        },
        confirmButton = {
            FastButton(
                enabled = !isGenerating && itemsToExport.isNotEmpty(),
                onClick = {
                    if (exportFormat == DistributorExportFormat.PLAIN_TEXT) {
                        ImageUtils.shareTextToWhatsApp(context, previewText)
                        onDismiss()
                    } else {
                        isGenerating = true
                        coroutineScope.launch {
                            val uri = withContext(Dispatchers.Default) {
                                val bmp = ImageUtils.generateMonochromeDistributorBitmap(
                                    workplaceName = workplaceName,
                                    distributorName = distributor.nombre,
                                    items = itemsToExport,
                                    context = context
                                )
                                val fileName = "boleta_monocroma_${System.currentTimeMillis()}.jpg"
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
                modifier = Modifier.testTag("distributor_export_send_button")
            ) { sendBtnText ->
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = sendBtnText)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (exportFormat == DistributorExportFormat.PLAIN_TEXT) {
                        "Enviar Texto"
                    } else {
                        "Enviar Tabla Monocroma"
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
