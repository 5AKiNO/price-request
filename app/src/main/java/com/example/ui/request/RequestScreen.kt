package com.example.ui.request

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.Product
import com.example.ui.theme.adaptiveTextFieldColors
import com.example.ui.theme.getContrastingTextColor
import com.example.ui.theme.getRequestTypeColor
import com.example.ui.theme.getRequestTypeContainerColor
import com.example.util.ImageUtils
import com.example.util.MathUtils
import java.io.File

/**
 * Universal camera contract compatible with all Android devices (including budget phones,
 * custom OEM camera apps, and emulators). Grants ClipData URI permissions and supports both
 * full-resolution FileProvider output and inline thumbnail fallback.
 */
private class UniversalTakePictureContract : ActivityResultContract<Uri, Bitmap?>() {
    private var outputUri: Uri? = null
    private var appContext: Context? = null

    override fun createIntent(context: Context, input: Uri): Intent {
        outputUri = input
        appContext = context.applicationContext
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input)
            clipData = ClipData.newRawUri("", input)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Bitmap? {
        if (resultCode != android.app.Activity.RESULT_OK) return null
        val ctx = appContext
        val uri = outputUri
        if (ctx != null && uri != null) {
            val fromFile = ImageUtils.decodeBitmapFromUri(ctx, uri)
            if (fromFile != null) return fromFile
        }
        // Fallback for budget/custom cameras that return inline bitmap in extras
        val inlineBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.extras?.getParcelable("data", Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.extras?.getParcelable("data") as? Bitmap
        }
        return inlineBmp
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    viewModel: RequestViewModel,
    modifier: Modifier = Modifier,
    initialProduct: Product? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var uncroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var isProductDropdownExpanded by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    var newVarietyInput by remember { mutableStateOf("") }

    remember(initialProduct) {
        if (initialProduct != null) {
            viewModel.selectProduct(initialProduct)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = UniversalTakePictureContract()
    ) { capturedBitmap ->
        if (capturedBitmap != null) {
            uncroppedBitmap = capturedBitmap
            showCropDialog = true
        } else {
            val fallbackUri = tempCameraUri
            if (fallbackUri != null) {
                val decoded = ImageUtils.decodeBitmapFromUri(context, fallbackUri)
                if (decoded != null) {
                    uncroppedBitmap = decoded
                    showCropDialog = true
                }
            }
        }
    }

    fun launchCameraSafely() {
        try {
            val cacheDir = File(context.cacheDir, "shared_images")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "capture_temp_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir la cámara en este dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraSafely()
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para tomar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val decoded = ImageUtils.decodeBitmapFromUri(context, uri)
            if (decoded != null) {
                uncroppedBitmap = decoded
                showCropDialog = true
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = ImageUtils.decodeBitmapFromUri(context, uri)
            if (decoded != null) {
                uncroppedBitmap = decoded
                showCropDialog = true
            }
        }
    }

    val requestTypes = listOf(
        1 to "1. Falta Registrar",
        2 to "2. Costo Subió",
        3 to "3. Costo Bajó",
        4 to "4. Supera % deseado",
        5 to "5. Menor a % deseado"
    )

    val activeColor = getRequestTypeColor(uiState.selectedTipoId)
    val activeContainerColor = getRequestTypeContainerColor(uiState.selectedTipoId)
    val textOnActiveColor = getContrastingTextColor(activeColor)
    val textOnActiveContainer = getContrastingTextColor(activeContainerColor)

    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)

    val cardBg = MaterialTheme.colorScheme.surface
    val cardTextColor = getContrastingTextColor(cardBg)
    val cardFieldColors = adaptiveTextFieldColors(
        containerColor = cardBg,
        borderColor = activeColor
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Gestor de Solicitudes de Precio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = screenTextColor
        )

        // Banner informativo: fondo claro del tipo de solicitud -> texto negro (getContrastingTextColor)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = activeContainerColor),
            border = BorderStroke(2.dp, activeColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (uiState.selectedTipoId) {
                        2 -> Icons.Default.TrendingUp
                        3 -> Icons.Default.TrendingDown
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = textOnActiveContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = viewModel.getTipoTitle(uiState.selectedTipoId, uiState.porcentajeDeseado),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textOnActiveContainer
                )
            }
        }

        // 1. Selector Horizontal de los 5 Tipos de Solicitud (fondo oscuro -> texto blanco, fondo claro -> texto negro)
        Text(
            text = "Tipo de Solicitud:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = screenTextColor
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(
                items = requestTypes,
                key = { it.first },
                contentType = { "request_type_chip" }
            ) { (id, label) ->
                val isSelected = uiState.selectedTipoId == id
                val chipColor = getRequestTypeColor(id)
                val chipContainer = getRequestTypeContainerColor(id)
                val chipBg = if (isSelected) chipColor else chipContainer
                val chipTextColor = getContrastingTextColor(chipBg)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setTipoId(id) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            color = chipTextColor
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = getContrastingTextColor(chipColor),
                        containerColor = chipContainer,
                        labelColor = getContrastingTextColor(chipContainer)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = chipColor,
                        selectedBorderColor = chipColor,
                        borderWidth = 1.5.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    modifier = Modifier.testTag("request_tipo_chip_$id")
                )
            }
        }

        // 2. Sección Multimedia: Foto y Recorte Obligatorio 16:9
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.5.dp, activeColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evidencia Fotográfica (16:9)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardTextColor
                    )
                    Surface(
                        color = activeContainerColor,
                        border = BorderStroke(1.5.dp, activeColor),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (uiState.croppedBitmap169 != null) "✓ 16:9 Listo" else "Formato 16:9",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textOnActiveContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (uiState.croppedBitmap169 != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        Image(
                            bitmap = uiState.croppedBitmap169!!.asImageBitmap(),
                            contentDescription = "Foto recortada 16:9",
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.setCroppedBitmap169(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Eliminar foto", tint = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (uncroppedBitmap != null) {
                                showCropDialog = true
                            }
                        },
                        border = BorderStroke(1.5.dp, activeColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = null, tint = cardTextColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reencuadrar Recorte 16:9", color = cardTextColor, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasCamPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCamPerm) {
                                    launchCameraSafely()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("request_button_camera"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor,
                                contentColor = textOnActiveColor
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = textOnActiveColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cámara", color = textOnActiveColor, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } catch (_: Exception) {
                                    galleryFallbackLauncher.launch("image/*")
                                }
                            },
                            border = BorderStroke(1.5.dp, activeColor),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("request_button_gallery")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = cardTextColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería", color = cardTextColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Datos del Producto y Variedades
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.5.dp, activeColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (uiState.selectedTipoId == 1) "Datos del Producto" else "Producto Registrado a Modificar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )

                if (uiState.selectedTipoId != 1) {
                    ExposedDropdownMenuBox(
                        expanded = isProductDropdownExpanded,
                        onExpandedChange = { isProductDropdownExpanded = !isProductDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = productSearchQuery.ifEmpty { uiState.selectedProduct?.descripcion ?: "" },
                            onValueChange = {
                                productSearchQuery = it
                                isProductDropdownExpanded = true
                            },
                            label = { Text("Buscar producto en catálogo (admite ñ)", color = cardTextColor) },
                            placeholder = { Text("Escribe código o nombre...", color = cardTextColor) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProductDropdownExpanded) },
                            colors = cardFieldColors,
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("request_product_search")
                        )

                        val filteredCandidates = remember(productSearchQuery, uiState.allProducts) {
                            if (productSearchQuery.isBlank()) uiState.allProducts.take(6)
                            else {
                                val q = MathUtils.normalizarConEnie(productSearchQuery.trim())
                                uiState.allProducts.filter {
                                    it.searchIndex.contains(q)
                                }.take(6)
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = isProductDropdownExpanded,
                            onDismissRequest = { isProductDropdownExpanded = false },
                            containerColor = cardBg
                        ) {
                            filteredCandidates.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(prod.descripcion, fontWeight = FontWeight.Bold, color = cardTextColor)
                                            val c = prod.precioCosto?.let { "Costo: Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "Sin costo"
                                            val v = prod.precioVenta?.let { "Venta: Gs. ${MathUtils.formatearGuaranies(it)}" } ?: "Sin venta"
                                            Text("Cód: ${prod.codigo ?: "Sin cód"} | $c | $v", style = MaterialTheme.typography.bodySmall, color = cardTextColor)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectProduct(prod)
                                        productSearchQuery = ""
                                        isProductDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Código Opcional en las 5 opciones
                OutlinedTextField(
                    value = uiState.codigoProducto,
                    onValueChange = { viewModel.onCodigoChanged(it) },
                    label = { Text("Código del Producto (Opcional)", color = cardTextColor) },
                    placeholder = { Text("Ej: 100648", color = cardTextColor) },
                    colors = cardFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Descripción Principal (Obligatorio)
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = { viewModel.onDescripcionChanged(it) },
                    label = { Text("Descripción / Nombre Principal *", color = cardTextColor) },
                    placeholder = { Text("Ej: Bandeja de Cartón N1", color = cardTextColor) },
                    colors = cardFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Múltiples Variedades / Nombres con el Mismo Costo
                Text(
                    text = "Variedades / Nombres con mismo costo (Opcional):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newVarietyInput,
                        onValueChange = { newVarietyInput = it },
                        placeholder = { Text("Ej: Sabor Vainilla, Medida 20cm...", color = cardTextColor) },
                        colors = cardFieldColors,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newVarietyInput.isNotBlank()) {
                                viewModel.addVariety(newVarietyInput)
                                newVarietyInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColor,
                            contentColor = textOnActiveColor
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = textOnActiveColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar", color = textOnActiveColor, fontWeight = FontWeight.Bold)
                    }
                }

                // Vista previa tipo lista con puntos (·) con texto según el fondo
                if (uiState.descripcion.isNotBlank() || uiState.variedadesList.isNotEmpty()) {
                    val previewBg = activeContainerColor
                    val previewTextColor = getContrastingTextColor(previewBg)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = previewBg,
                        border = BorderStroke(1.5.dp, activeColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Nombre Producto (Vista previa en imagen):",
                                style = MaterialTheme.typography.labelMedium,
                                color = previewTextColor,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.descripcion.isNotBlank()) {
                                Text(
                                    text = "· ${uiState.descripcion.trim()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = previewTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            uiState.variedadesList.forEachIndexed { index, variety ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "· $variety",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = previewTextColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeVariety(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Eliminar variedad",
                                            tint = previewTextColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Campos Opcionales de Stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.stockAnterior,
                        onValueChange = { viewModel.onStockAnteriorChanged(it) },
                        label = { Text("Stock Anterior (Opcional)", color = cardTextColor) },
                        placeholder = { Text("Ej: 5 unid.", color = cardTextColor) },
                        colors = cardFieldColors,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.recargaStock,
                        onValueChange = { viewModel.onRecargaStockChanged(it) },
                        label = { Text("Recarga Stock (Opcional)", color = cardTextColor) },
                        placeholder = { Text("Ej: +24 unid.", color = cardTextColor) },
                        colors = cardFieldColors,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. VALORES DE COSTO Y VENTA (ORDEN EXACTO SEGÚN EL TIPO DE SOLICITUD 1, 2-3, 4-5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.5.dp, activeColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Valores de Costo y Venta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )

                when (uiState.selectedTipoId) {
                    // =========================================================================
                    // TIPO 1: FALTA REGISTRAR (Costo + Venta con botones de 20% a 50%)
                    // =========================================================================
                    1 -> {
                        OutlinedTextField(
                            value = uiState.costoNuevo,
                            onValueChange = { viewModel.onCostoNuevoChanged(it) },
                            label = { Text("Precio de Costo (Gs.) *", color = cardTextColor) },
                            placeholder = { Text("Ej: 3500", color = cardTextColor) },
                            colors = cardFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = {
                                val c = uiState.costoNuevo.toLongOrNull()
                                if (c != null) Text("Gs. ${MathUtils.formatearGuaranies(c)}", color = cardTextColor, fontWeight = FontWeight.Bold)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_input_costo_nuevo")
                        )

                        SelectablePercentageAndPriceBlock(
                            priceLabel = "Precio de Venta (Gs.) *",
                            priceValue = uiState.ventaNueva,
                            onPriceChanged = { viewModel.onVentaNuevaChanged(it) },
                            selectedPercentage = uiState.porcentajeNuevo,
                            onSelectPercentage = { viewModel.applyMarginNuevo(it) },
                            cardBg = cardBg,
                            activeColor = activeColor,
                            activeContainerColor = activeContainerColor,
                            priceTestTag = "request_input_venta_nueva"
                        )
                    }

                    // =========================================================================
                    // TIPOS 2 y 3: SUBIDA Y BAJADA DE COSTO
                    // Orden estricto:
                    // 1. Precio de Costo Anterior
                    // 2. Precio de Costo Nuevo
                    // 3. Precio de Venta Anterior con su porcentaje seleccionable (20% a 50%)
                    // 4. Precio de Venta Nuevo con su porcentaje seleccionable (20% a 50%)
                    // =========================================================================
                    2, 3 -> {
                        // 1. Precio de Costo Anterior
                        OutlinedTextField(
                            value = uiState.costoAnteriorInput,
                            onValueChange = { viewModel.onCostoAnteriorChanged(it) },
                            label = { Text("1. Precio de Costo Anterior (Gs.) *", color = cardTextColor) },
                            placeholder = { Text("Ej: 3000", color = cardTextColor) },
                            colors = cardFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = {
                                val cAnt = uiState.costoAnteriorInput.toLongOrNull()
                                if (cAnt != null) Text("Gs. ${MathUtils.formatearGuaranies(cAnt)}", color = cardTextColor, fontWeight = FontWeight.Bold)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_input_costo_anterior")
                        )

                        // 2. Precio de Costo Nuevo
                        OutlinedTextField(
                            value = uiState.costoNuevo,
                            onValueChange = { viewModel.onCostoNuevoChanged(it) },
                            label = { Text("2. Precio de Costo Nuevo (Gs.) *", color = cardTextColor) },
                            placeholder = { Text("Ej: 3500", color = cardTextColor) },
                            colors = cardFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = {
                                val cNuevo = uiState.costoNuevo.toLongOrNull()
                                if (cNuevo != null) Text("Gs. ${MathUtils.formatearGuaranies(cNuevo)}", color = cardTextColor, fontWeight = FontWeight.Bold)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_input_costo_nuevo")
                        )

                        HorizontalDivider(color = activeColor.copy(alpha = 0.4f))

                        // 3. Precio de Venta Anterior con su porcentaje seleccionable
                        SelectablePercentageAndPriceBlock(
                            priceLabel = "3. Precio de Venta Anterior (Gs.) *",
                            priceValue = uiState.ventaAnteriorInput,
                            onPriceChanged = { viewModel.onVentaAnteriorChanged(it) },
                            selectedPercentage = uiState.porcentajeAnterior,
                            onSelectPercentage = { viewModel.applyMarginAnterior(it) },
                            cardBg = cardBg,
                            activeColor = activeColor,
                            activeContainerColor = activeContainerColor,
                            priceTestTag = "request_input_venta_anterior"
                        )

                        HorizontalDivider(color = activeColor.copy(alpha = 0.4f))

                        // 4. Precio de Venta Nuevo con su porcentaje seleccionable
                        SelectablePercentageAndPriceBlock(
                            priceLabel = "4. Precio de Venta Nuevo (Gs.) *",
                            priceValue = uiState.ventaNueva,
                            onPriceChanged = { viewModel.onVentaNuevaChanged(it) },
                            selectedPercentage = uiState.porcentajeNuevo,
                            onSelectPercentage = { viewModel.applyMarginNuevo(it) },
                            cardBg = cardBg,
                            activeColor = activeColor,
                            activeContainerColor = activeContainerColor,
                            priceTestTag = "request_input_venta_nueva"
                        )
                    }

                    // =========================================================================
                    // TIPOS 4 y 5: CAMBIO DE PRECIO DE VENTA AL SUBIR Y BAJAR (UN SOLO COSTO)
                    // Orden estricto:
                    // 1. Precio de Costo (una sola vez)
                    // 2. Precio de Venta Anterior con su porcentaje seleccionable (20% a 50%)
                    // 3. Precio de Venta Nuevo con su porcentaje seleccionable (20% a 50%)
                    // =========================================================================
                    4, 5 -> {
                        // 1. Precio de Costo (único)
                        OutlinedTextField(
                            value = uiState.costoNuevo,
                            onValueChange = { viewModel.onCostoNuevoChanged(it) },
                            label = { Text("1. Precio de Costo (Gs.) *", color = cardTextColor) },
                            placeholder = { Text("Ej: 3500", color = cardTextColor) },
                            colors = cardFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = {
                                val c = uiState.costoNuevo.toLongOrNull()
                                if (c != null) Text("Gs. ${MathUtils.formatearGuaranies(c)}", color = cardTextColor, fontWeight = FontWeight.Bold)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_input_costo_nuevo")
                        )

                        HorizontalDivider(color = activeColor.copy(alpha = 0.4f))

                        // 2. Precio de Venta Anterior con su porcentaje seleccionable
                        SelectablePercentageAndPriceBlock(
                            priceLabel = "2. Precio de Venta Anterior (Gs.) *",
                            priceValue = uiState.ventaAnteriorInput,
                            onPriceChanged = { viewModel.onVentaAnteriorChanged(it) },
                            selectedPercentage = uiState.porcentajeAnterior,
                            onSelectPercentage = { viewModel.applyMarginAnterior(it) },
                            cardBg = cardBg,
                            activeColor = activeColor,
                            activeContainerColor = activeContainerColor,
                            priceTestTag = "request_input_venta_anterior"
                        )

                        HorizontalDivider(color = activeColor.copy(alpha = 0.4f))

                        // 3. Precio de Venta Nuevo con su porcentaje seleccionable
                        SelectablePercentageAndPriceBlock(
                            priceLabel = "3. Precio de Venta Nuevo (Gs.) *",
                            priceValue = uiState.ventaNueva,
                            onPriceChanged = { viewModel.onVentaNuevaChanged(it) },
                            selectedPercentage = uiState.porcentajeNuevo,
                            onSelectPercentage = { viewModel.applyMarginNuevo(it) },
                            cardBg = cardBg,
                            activeColor = activeColor,
                            activeContainerColor = activeContainerColor,
                            priceTestTag = "request_input_venta_nueva"
                        )
                    }
                }
            }
        }

        if (uiState.validationError != null) {
            val errBg = Color(0xFFFFEBEE)
            val errText = getContrastingTextColor(errBg)
            Card(
                colors = CardDefaults.cardColors(containerColor = errBg),
                border = BorderStroke(1.5.dp, Color(0xFFB71C1C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = errText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.validationError!!,
                        color = errText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Botón Principal - Envío a WhatsApp de SOLO LA IMAGEN RENDERIZADA
        Button(
            onClick = {
                if (newVarietyInput.isNotBlank()) {
                    viewModel.addVariety(newVarietyInput)
                    newVarietyInput = ""
                }
                viewModel.validateAndSubmit { imageUri ->
                    ImageUtils.shareImageOnlyToWhatsApp(context, imageUri)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = activeColor,
                contentColor = textOnActiveColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("request_button_submit_whatsapp")
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = textOnActiveColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Generar Ficha y Enviar a WhatsApp",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textOnActiveColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showCropDialog && uncroppedBitmap != null) {
        Crop169Dialog(
            sourceBitmap = uncroppedBitmap!!,
            onDismiss = { showCropDialog = false },
            onCropped = { cropped ->
                viewModel.setCroppedBitmap169(cropped)
                showCropDialog = false
            }
        )
    }
}

/**
 * Reusable component for a Sale Price input paired ONLY with the selectable percentage buttons
 * from 20% to 50% (20%, 25%, 30%, 40%, 50%), where dark backgrounds use white text and light
 * backgrounds use black text.
 */
@Composable
private fun SelectablePercentageAndPriceBlock(
    priceLabel: String,
    priceValue: String,
    onPriceChanged: (String) -> Unit,
    selectedPercentage: Int?,
    onSelectPercentage: (Int) -> Unit,
    cardBg: Color,
    activeColor: Color,
    activeContainerColor: Color,
    priceTestTag: String
) {
    val cardTextColor = getContrastingTextColor(cardBg)
    val badgeTextColor = getContrastingTextColor(activeContainerColor)
    val textOnActive = getContrastingTextColor(activeColor)
    val fieldColors = adaptiveTextFieldColors(
        containerColor = cardBg,
        borderColor = activeColor
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Campo de Precio de Venta
        OutlinedTextField(
            value = priceValue,
            onValueChange = onPriceChanged,
            label = { Text(priceLabel, color = cardTextColor) },
            placeholder = { Text("Ej: 4500", color = cardTextColor) },
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            supportingText = {
                val v = priceValue.toLongOrNull()
                if (v != null) {
                    val pctSuffix = selectedPercentage?.let { " ($it%)" } ?: ""
                    Text(
                        text = "Gs. ${MathUtils.formatearGuaranies(v)}$pctSuffix",
                        color = cardTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(priceTestTag)
        )

        // Encabezado de porcentaje con insignia de borde coloreado y texto según su fondo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Porcentaje seleccionable:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = cardTextColor
            )
            if (selectedPercentage != null) {
                Surface(
                    color = activeContainerColor,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.5.dp, activeColor)
                ) {
                    Text(
                        text = "$selectedPercentage%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Grupo de botones seleccionables únicamente de 20% hasta 50%
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(20, 25, 30, 40, 50).forEach { pct ->
                val isSelected = selectedPercentage == pct
                val chipBg = if (isSelected) activeColor else activeContainerColor
                val chipText = getContrastingTextColor(chipBg)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPercentage(pct) },
                    label = {
                        Text(
                            text = "$pct%",
                            fontWeight = FontWeight.Bold,
                            color = chipText
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = activeColor,
                        selectedLabelColor = textOnActive,
                        containerColor = activeContainerColor,
                        labelColor = badgeTextColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = activeColor,
                        selectedBorderColor = activeColor,
                        borderWidth = 1.5.dp,
                        selectedBorderWidth = 2.dp
                    )
                )
            }
        }
    }
}
