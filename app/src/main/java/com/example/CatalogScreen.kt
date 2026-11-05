package com.example

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
    onNavigateToNewRequestWithProduct: ((Product) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var isRenderingSelection by remember { mutableStateOf(false) }

    val exportBinaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCatalogBinaryToUri(uri) { result ->
                result.onSuccess { count ->
                    Toast.makeText(
                        context,
                        "Catálogo exportado ($count productos)",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "Error al exportar catálogo",
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

    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)

    val surfaceBg = MaterialTheme.colorScheme.surface
    val surfaceTextColor = getContrastingTextColor(surfaceBg)

    val filterOptions = remember {
        listOf(
            "Todos",
            "Completos",
            "Sin Código",
            "Falta Registrar",
            "Seleccionados",
            "20%",
            "25%",
            "30%",
            "35%",
            "50%",
            "Personalizado"
        )
    }

    val selectedCount by remember {
        derivedStateOf { uiState.selectedProductIds.size }
    }

    fun sendSelectedAsMonochromeImageDirectly() {
        val selectedList = uiState.allProducts.filter { uiState.selectedProductIds.contains(it.id) }
        if (selectedList.isEmpty() || isRenderingSelection) return
        isRenderingSelection = true
        coroutineScope.launch {
            val uri = withContext(Dispatchers.Default) {
                val bmp = ImageUtils.generateMonochromeCatalogBitmap(
                    context = context,
                    workplaceName = uiState.workplaceName,
                    products = selectedList,
                    includeCodigo = true,
                    includeCosto = true,
                    includeVenta = true,
                    includeMargen = true
                )
                val fileName = "seleccion_catalogo_${System.currentTimeMillis()}.jpg"
                ImageUtils.saveBitmapToCache(context, bmp, fileName)
            }
            isRenderingSelection = false
            if (uri != null) {
                ImageUtils.shareImageOnlyToWhatsApp(context, uri)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoColumnGrid = maxWidth >= 680.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FastTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = "Buscar código, descripción, precio o margen",
                    containerColor = surfaceBg,
                    borderColor = BrandBluePrimary,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = surfaceTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChanged("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = surfaceTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("catalog_search_bar")
                )

                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("catalog_options_menu_button")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = screenTextColor)
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false },
                    containerColor = surfaceBg
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = surfaceTextColor)
                        },
                        text = {
                            Column {
                                Text(
                                    "Configurar nombre del local",
                                    color = surfaceTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Actual: ${uiState.workplaceName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = surfaceTextColor
                                )
                            }
                        },
                        onClick = {
                            showOptionsMenu = false
                            viewModel.openWorkplaceDialog()
                        },
                        modifier = Modifier.testTag("menu_configure_workplace")
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = surfaceTextColor)
                        },
                        text = {
                            Text(
                                "Enviar catálogo",
                                color = surfaceTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            showOptionsMenu = false
                            viewModel.openExportDialogForAll()
                        },
                        modifier = Modifier.testTag("menu_export_all_catalog")
                    )
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
                            showOptionsMenu = false
                            val safeWorkplace = uiState.workplaceName
                                .trim()
                                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                .ifBlank { "catalogo" }
                            exportBinaryLauncher.launch("catalogo_${safeWorkplace}.bin")
                        },
                        modifier = Modifier.testTag("menu_export_binary_catalog")
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
                            showOptionsMenu = false
                            importBinaryLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.testTag("menu_import_binary_catalog")
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Seleccionar todos los visibles",
                                color = surfaceTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            viewModel.selectAll()
                            showOptionsMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Deseleccionar todos",
                                color = surfaceTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            viewModel.clearSelection()
                            showOptionsMenu = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(
                    items = filterOptions,
                    key = { it },
                    contentType = { "filter_chip" }
                ) { filter ->
                    val isSelected = uiState.activeFilter == filter
                    val labelText = if (filter == "Seleccionados" && selectedCount > 0) {
                        "$filter ($selectedCount)"
                    } else {
                        filter
                    }
                    FastChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = labelText,
                        selectedContainerColor = BrandBluePrimary,
                        unselectedContainerColor = BrandBlueContainer,
                        borderColor = BrandBluePrimary,
                        showCheckIcon = true,
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }

            if (selectedCount > 0) {
                val selectionBarBg = BrandBlueContainer
                val selectionBarText = getContrastingTextColor(selectionBarBg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(selectionBarBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Marcados ($selectedCount) · Doble toque para marcar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = selectionBarText,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FastButton(
                            onClick = { sendSelectedAsMonochromeImageDirectly() },
                            enabled = !isRenderingSelection,
                            containerColor = Tipo1Verde,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            minHeight = 32.dp,
                            modifier = Modifier.testTag("catalog_render_selected_whatsapp")
                        ) { btnColor ->
                            Icon(Icons.Default.Image, contentDescription = null, tint = btnColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Render WhatsApp",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = btnColor
                            )
                        }
                        FastOutlinedButton(
                            onClick = { viewModel.clearSelection() },
                            containerColor = surfaceBg,
                            borderColor = BrandBluePrimary,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            minHeight = 32.dp,
                            modifier = Modifier.testTag("catalog_clear_selection_button")
                        ) { btnColor ->
                            Text(
                                text = "Limpiar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = btnColor
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerSummary = if (selectedCount > 0) {
                    "Productos (${uiState.filteredProducts.size}) · Seleccionados: $selectedCount"
                } else {
                    "Productos (${uiState.filteredProducts.size}) · Doble toque para seleccionar"
                }
                Text(
                    text = headerSummary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = screenTextColor,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.filteredProducts.isNotEmpty()) {
                    FastOutlinedButton(
                        onClick = {
                            if (selectedCount > 0) {
                                viewModel.openExportDialog(preferMonochromeImage = true)
                            } else {
                                viewModel.openExportDialogForAll()
                            }
                        },
                        containerColor = BrandBlueContainer,
                        borderColor = BrandBluePrimary,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        minHeight = 32.dp,
                        modifier = Modifier.testTag("catalog_button_export_header")
                    ) { contentColor ->
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedCount > 0) "Opciones ($selectedCount)" else "Enviar Catálogo",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (uiState.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (uiState.allProducts.isEmpty())
                                "Sin productos registrados"
                            else
                                "No se encontraron coincidencias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = screenTextColor
                        )
                        if (uiState.allProducts.isEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FastButton(
                                    onClick = { viewModel.openNewProductForm() },
                                    containerColor = BrandBluePrimary
                                ) { btnText ->
                                    Icon(Icons.Default.Add, contentDescription = null, tint = btnText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Registrar Producto", color = btnText, fontWeight = FontWeight.Bold)
                                }

                                FastOutlinedButton(
                                    onClick = { importBinaryLauncher.launch(arrayOf("*/*")) },
                                    containerColor = BrandBlueContainer,
                                    borderColor = BrandBluePrimary
                                ) { btnText ->
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = btnText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Importar (.bin)", color = btnText, fontWeight = FontWeight.Bold)
                                }
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
                    contentPadding = PaddingValues(bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.filteredProducts,
                        key = { it.id },
                        contentType = { "product_item" }
                    ) { product ->
                        val isSelected = uiState.selectedProductIds.contains(product.id)
                        ProductListItem(
                            product = product,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleProductSelection(product.id) },
                            onEdit = { viewModel.openEditProductForm(product) },
                            onDelete = { viewModel.deleteProduct(product) },
                            onShareProduct = { viewModel.openSingleProductExport(product) },
                            onRequestPriceChange = onNavigateToNewRequestWithProduct?.let {
                                { it(product) }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.filteredProducts,
                        key = { it.id },
                        contentType = { "product_item" }
                    ) { product ->
                        val isSelected = uiState.selectedProductIds.contains(product.id)
                        ProductListItem(
                            product = product,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleProductSelection(product.id) },
                            onEdit = { viewModel.openEditProductForm(product) },
                            onDelete = { viewModel.deleteProduct(product) },
                            onShareProduct = { viewModel.openSingleProductExport(product) },
                            onRequestPriceChange = onNavigateToNewRequestWithProduct?.let {
                                { it(product) }
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.selectedProductIds.isNotEmpty()) {
                val waFabBg = Tipo1Verde
                val waFabText = getContrastingTextColor(waFabBg)
                ExtendedFloatingActionButton(
                    onClick = { sendSelectedAsMonochromeImageDirectly() },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = waFabText) },
                    text = { Text("Render (${uiState.selectedProductIds.size})", color = waFabText, fontWeight = FontWeight.Bold) },
                    containerColor = waFabBg,
                    contentColor = waFabText,
                    modifier = Modifier.testTag("catalog_fab_whatsapp")
                )
            }

            val addFabBg = BrandBluePrimary
            val addFabText = getContrastingTextColor(addFabBg)
            FloatingActionButton(
                onClick = { viewModel.openNewProductForm() },
                containerColor = addFabBg,
                contentColor = addFabText,
                modifier = Modifier.testTag("catalog_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Producto", tint = addFabText)
            }
        }
    }

    if (pendingImportUri != null) {
        val targetUri = pendingImportUri!!
        BinaryImportModeDialog(
            title = "Importar Catálogo (.bin)",
            description = "Seleccione el modo de importación para el archivo elegido",
            onDismiss = { pendingImportUri = null },
            onSelectMode = { replaceAll ->
                pendingImportUri = null
                viewModel.importCatalogBinaryFromUri(targetUri, replaceAll = replaceAll) { result ->
                    result.onSuccess { count ->
                        val modeLabel = if (replaceAll) "reemplazado" else "fusionado"
                        Toast.makeText(
                            context,
                            "Catálogo $modeLabel ($count registros procesados)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { err ->
                        Toast.makeText(
                            context,
                            err.message ?: "Error al importar el archivo binario",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    if (uiState.isFormOpen) {
        ProductDialog(
            initialProduct = uiState.editingProduct,
            onDismiss = { viewModel.closeForm() },
            onSave = { id, cod, desc, c, v, p ->
                viewModel.saveProduct(id, cod, desc, c, v, p)
            },
            onSaveAndShare = { id, cod, desc, c, v, p ->
                val roundedMargin = if (c != null && v != null && c > 0L) {
                    MathUtils.calcularPorcentajeRedondeado(c, v)
                } else null
                val tempProd = Product(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    codigo = cod,
                    descripcion = desc,
                    precioCosto = c,
                    precioVenta = v,
                    porcentajePersonalizado = p,
                    porcentajeRealRedondeado = roundedMargin
                )
                viewModel.saveProduct(tempProd.id, cod, desc, c, v, p)
                viewModel.openSingleProductExport(tempProd)
            }
        )
    }

    if (uiState.isExportDialogOpen) {
        val exportList = uiState.exportTargetProducts.ifEmpty {
            uiState.allProducts.filter { uiState.selectedProductIds.contains(it.id) }
        }
        WhatsAppExportDialog(
            workplaceName = uiState.workplaceName,
            selectedProducts = exportList,
            initialMonochromeFormat = uiState.exportInitialFormatMonochrome,
            onDismiss = { viewModel.closeExportDialog() },
            buildTextFunction = { list, cod, cst, vnt, mrg ->
                viewModel.buildWhatsAppPlainText(list, cod, cst, vnt, mrg)
            }
        )
    }

    if (uiState.isWorkplaceDialogOpen) {
        WorkplaceNameDialog(
            currentName = uiState.workplaceName,
            onDismiss = { viewModel.closeWorkplaceDialog() },
            onSave = { newName -> viewModel.saveWorkplaceName(newName) }
        )
    }
}

@Composable
fun BinaryImportModeDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onSelectMode: (replaceAll: Boolean) -> Unit
) {
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
                    text = title,
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = dialogTextColor
                )
                FastButton(
                    onClick = { onSelectMode(false) },
                    containerColor = BrandBluePrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("binary_import_merge_button")
                ) { mergeBtnText ->
                    Text(
                        "Fusionar registros",
                        color = mergeBtnText,
                        fontWeight = FontWeight.Bold
                    )
                }
                FastButton(
                    onClick = { onSelectMode(true) },
                    containerColor = Tipo5Rojo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("binary_import_replace_button")
                ) { replaceBtnText ->
                    Text(
                        "Reemplazar todos los registros",
                        color = replaceBtnText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {},
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
fun WorkplaceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
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
                    text = "Configurar Nombre del Local",
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ingrese el nombre comercial que se mostrará en el encabezado y en los reportes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = dialogTextColor
                )
                FastTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = "Nombre del local *",
                    placeholder = "Ej: Mi Local Comercial",
                    containerColor = BrandBlueContainer,
                    borderColor = BrandBluePrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workplace_name_input")
                )
            }
        },
        confirmButton = {
            FastButton(
                onClick = { onSave(nameInput) },
                containerColor = BrandBluePrimary,
                modifier = Modifier.testTag("workplace_name_save_button")
            ) { saveText ->
                Text("Guardar Nombre", color = saveText, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductListItem(
    product: Product,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareProduct: () -> Unit,
    onRequestPriceChange: (() -> Unit)?
) {
    val rowBg = if (isSelected) BrandBlueContainer else MaterialTheme.colorScheme.surface
    val textColor = getContrastingTextColor(rowBg)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onToggleSelect() },
                onDoubleClick = { onToggleSelect() }
            )
            .padding(16.dp)
            .testTag("product_item_${product.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FastCheckBox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                checkedColor = BrandBluePrimary,
                uncheckedBorderColor = textColor,
                modifier = Modifier.testTag("product_check_${product.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = product.descripcion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val statusSuffix = if (product.estadoRegistro == "FALTA_REGISTRAR") {
                    " · Falta Registrar"
                } else {
                    ""
                }
                Text(
                    text = "Código: ${product.codigoMostrar}$statusSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShareProduct,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("product_share_${product.id}")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir producto",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (onRequestPriceChange != null) {
                    IconButton(
                        onClick = onRequestPriceChange,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("product_to_request_${product.id}")
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Crear Solicitud",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Costo: ${product.costoFormateado}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = "Venta: ${product.ventaFormateada}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )

            Text(
                text = product.margenFormateado?.let { "Margen: $it" } ?: "Sin margen",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}
