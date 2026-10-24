package com.example.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.theme.ClaudiaBlueContainer
import com.example.ui.theme.ClaudiaBluePrimary
import com.example.ui.theme.PriceDropBg
import com.example.ui.theme.PriceDropOrange
import com.example.ui.theme.PriceIncreaseBg
import com.example.ui.theme.PriceIncreaseBlue
import com.example.ui.theme.Tipo1Verde
import com.example.ui.theme.adaptiveTextFieldColors
import com.example.ui.theme.getContrastingTextColor
import com.example.util.ImageUtils
import com.example.util.MathUtils

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
    onNavigateToNewRequestWithProduct: ((Product) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showOptionsMenu by remember { mutableStateOf(false) }

    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)

    val surfaceBg = MaterialTheme.colorScheme.surface
    val surfaceTextColor = getContrastingTextColor(surfaceBg)

    val filterOptions = listOf(
        "Todos",
        "Completos",
        "Sin Código",
        "Falta Registrar",
        "Seleccionados",
        "20%",
        "25%",
        "Personalizado"
    )

    val selectedCount by remember {
        androidx.compose.runtime.derivedStateOf { uiState.selectedProductIds.size }
    }

    fun shareSingleProduct(product: Product) {
        val text = viewModel.buildWhatsAppPlainText(
            selectedProducts = listOf(product),
            includeCodigo = true,
            includeCosto = true,
            includeVenta = true,
            includeMargen = true
        )
        ImageUtils.shareTextToWhatsApp(context, text)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Buscador Rápido Reactivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            "Buscar código, descripción (ñ), costo o venta...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = surfaceTextColor
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = surfaceTextColor)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = surfaceTextColor)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = adaptiveTextFieldColors(
                        containerColor = surfaceBg,
                        borderColor = ClaudiaBluePrimary
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("catalog_search_bar")
                )

                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = screenTextColor)
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false },
                    containerColor = surfaceBg
                ) {
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

            Spacer(modifier = Modifier.height(10.dp))

            // LazyRow de FilterChips: fondo oscuro -> texto blanco, fondo claro -> texto negro
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(
                    items = filterOptions,
                    key = { it },
                    contentType = { "filter_chip" }
                ) { filter ->
                    val isSelected = uiState.activeFilter == filter
                    val chipBg = if (isSelected) ClaudiaBluePrimary else ClaudiaBlueContainer
                    val chipTextColor = getContrastingTextColor(chipBg)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = {
                            val labelText = if (filter == "Seleccionados" && selectedCount > 0) {
                                "$filter ($selectedCount)"
                            } else {
                                filter
                            }
                            Text(
                                text = labelText,
                                fontWeight = FontWeight.Bold,
                                color = chipTextColor
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = chipTextColor) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClaudiaBluePrimary,
                            selectedLabelColor = getContrastingTextColor(ClaudiaBluePrimary),
                            containerColor = ClaudiaBlueContainer,
                            labelColor = getContrastingTextColor(ClaudiaBlueContainer)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ClaudiaBluePrimary,
                            selectedBorderColor = ClaudiaBluePrimary,
                            borderWidth = 1.5.dp,
                            selectedBorderWidth = 2.dp
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LazyRow de Tarjetas Rápidas de Resultados
            if (uiState.recentOrQuickProducts.isNotEmpty()) {
                Text(
                    text = "Acceso Rápido / Recientes:",
                    style = MaterialTheme.typography.labelMedium,
                    color = screenTextColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(
                        items = uiState.recentOrQuickProducts,
                        key = { it.id },
                        contentType = { "quick_card" }
                    ) { product ->
                        val isSelected = uiState.selectedProductIds.contains(product.id)
                        QuickProductCard(
                            product = product,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleProductSelection(product.id) },
                            onEdit = { viewModel.openEditProductForm(product) },
                            onShare = { shareSingleProduct(product) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Encabezado de Lista Principal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Productos (${uiState.filteredProducts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = screenTextColor
                )
                if (selectedCount > 0) {
                    val badgeBg = ClaudiaBlueContainer
                    val badgeTextColor = getContrastingTextColor(badgeBg)
                    Surface(
                        color = badgeBg,
                        border = BorderStroke(1.5.dp, ClaudiaBluePrimary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$selectedCount seleccionados",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTextColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // LISTA PRINCIPAL
            if (uiState.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (uiState.allProducts.isEmpty())
                                "Catálogo limpio para producción"
                            else
                                "No se encontraron coincidencias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = screenTextColor
                        )
                        if (uiState.allProducts.isEmpty()) {
                            val btnBg = ClaudiaBluePrimary
                            val btnText = getContrastingTextColor(btnBg)
                            Button(
                                onClick = { viewModel.openNewProductForm() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = btnBg,
                                    contentColor = btnText
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = btnText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Registrar Primer Producto", color = btnText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
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
                            onShareProduct = { shareSingleProduct(product) },
                            onRequestPriceChange = onNavigateToNewRequestWithProduct?.let {
                                { it(product) }
                            }
                        )
                    }
                }
            }
        }

        // Bottom Action Bar / Floating buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.selectedProductIds.isNotEmpty()) {
                val waFabBg = Tipo1Verde
                val waFabText = getContrastingTextColor(waFabBg)
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openExportDialog() },
                    icon = { Icon(Icons.Default.Share, contentDescription = null, tint = waFabText) },
                    text = { Text("WhatsApp (${uiState.selectedProductIds.size})", color = waFabText, fontWeight = FontWeight.Bold) },
                    containerColor = waFabBg,
                    contentColor = waFabText,
                    modifier = Modifier.testTag("catalog_fab_whatsapp")
                )
            }

            val addFabBg = ClaudiaBluePrimary
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

    // Modal Formulario de Alta / Edición con opción de Guardar y Compartir
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
                shareSingleProduct(tempProd)
            }
        )
    }

    // Modal Exportación WhatsApp
    if (uiState.isExportDialogOpen) {
        val selectedItems = uiState.allProducts.filter { uiState.selectedProductIds.contains(it.id) }
        WhatsAppExportDialog(
            selectedProducts = selectedItems,
            onDismiss = { viewModel.closeExportDialog() },
            buildTextFunction = { list, cod, cst, vnt, mrg ->
                viewModel.buildWhatsAppPlainText(list, cod, cst, vnt, mrg)
            }
        )
    }
}

@Composable
fun QuickProductCard(
    product: Product,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    val cardBg = if (isSelected) ClaudiaBlueContainer else MaterialTheme.colorScheme.surface
    val cardTextColor = getContrastingTextColor(cardBg)

    ElevatedCard(
        modifier = Modifier
            .width(190.dp)
            .height(118.dp)
            .clickable { onToggleSelect() }
            .testTag("quick_card_${product.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = product.codigo ?: "Sin Cód.",
                    style = MaterialTheme.typography.labelSmall,
                    color = cardTextColor,
                    fontWeight = FontWeight.Bold
                )
                if (product.porcentajeRealRedondeado != null) {
                    val isHigh = product.porcentajeRealRedondeado >= 30
                    val badgeBg = if (isHigh) PriceIncreaseBg else PriceDropBg
                    val badgeTextColor = getContrastingTextColor(badgeBg)
                    Surface(
                        color = badgeBg,
                        border = BorderStroke(1.dp, if (isHigh) PriceIncreaseBlue else PriceDropOrange),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${product.porcentajeRealRedondeado}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = product.descripcion,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = cardTextColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val ventaStr = product.precioVenta?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "Sin precio"
                Text(
                    text = ventaStr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )
                Row {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir producto",
                            tint = cardTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = cardTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

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
    val cardBg = if (isSelected) ClaudiaBlueContainer else MaterialTheme.colorScheme.surface
    val textColor = getContrastingTextColor(cardBg)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.5.dp,
            color = ClaudiaBluePrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ClaudiaBluePrimary,
                        checkmarkColor = getContrastingTextColor(ClaudiaBluePrimary),
                        uncheckedColor = textColor
                    ),
                    modifier = Modifier.testTag("product_check_${product.id}")
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val cod = product.codigo ?: "Sin Código"
                        val codeBadgeBg = ClaudiaBlueContainer
                        val codeBadgeText = getContrastingTextColor(codeBadgeBg)
                        Surface(
                            color = codeBadgeBg,
                            border = BorderStroke(1.dp, ClaudiaBluePrimary),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Cód: $cod",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = codeBadgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (product.estadoRegistro == "FALTA_REGISTRAR") {
                            Spacer(modifier = Modifier.width(6.dp))
                            val faltaBg = Color(0xFFFFEBEE)
                            val faltaText = getContrastingTextColor(faltaBg)
                            Surface(
                                color = faltaBg,
                                border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Falta Registrar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = faltaText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.descripcion,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Acciones rápidas con contraste según el fondo de la tarjeta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShareProduct,
                        modifier = Modifier.testTag("product_share_${product.id}")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir producto por WhatsApp",
                            tint = textColor
                        )
                    }
                    if (onRequestPriceChange != null) {
                        IconButton(
                            onClick = onRequestPriceChange,
                            modifier = Modifier.testTag("product_to_request_${product.id}")
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Crear Ficha 16:9",
                                tint = textColor
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = textColor)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = textColor)
                    }
                }
            }

            // Detalle de Precios y Margen con contraste según el fondo de cada bloque
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Costo:",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.precioCosto?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "---",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Column {
                    Text(
                        text = "Venta:",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.precioVenta?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "---",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Margen Real:",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (product.porcentajeRealRedondeado != null) {
                        val isHigh = product.porcentajeRealRedondeado >= 30
                        val pctBadgeBg = if (isHigh) PriceIncreaseBg else PriceDropBg
                        val pctBadgeText = getContrastingTextColor(pctBadgeBg)
                        Surface(
                            color = pctBadgeBg,
                            border = BorderStroke(1.5.dp, if (isHigh) PriceIncreaseBlue else PriceDropOrange),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${product.porcentajeRealRedondeado}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = pctBadgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "---",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
