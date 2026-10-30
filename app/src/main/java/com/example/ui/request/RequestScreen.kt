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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.model.VarietyStockItem
import com.example.ui.components.FastButton
import com.example.ui.components.FastChip
import com.example.ui.components.FastOutlinedButton
import com.example.ui.components.FastTextField
import com.example.ui.components.SELECTABLE_PERCENTAGES
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
        val inlineBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.extras?.getParcelable("data", Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.extras?.getParcelable("data") as? Bitmap
        }
        return inlineBmp
    }
}

@Composable
fun RequestScreen(
    viewModel: RequestViewModel,
    modifier: Modifier = Modifier,
    initialProduct: Product? = null,
    onEditSavedToHistory: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var uncroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var newVarietyInput by remember { mutableStateOf("") }
    var newVarietyStockInput by remember { mutableStateOf("") }
    var newVarietyRecargaInput by remember { mutableStateOf("") }

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

    val requestTypes = remember {
        listOf(
            1 to "1. Falta Registrar",
            2 to "2. Costo Subió",
            3 to "3. Costo Bajó",
            4 to "4. Excede % deseado",
            5 to "5. Inferior al % deseado"
        )
    }

    val activeColor = getRequestTypeColor(uiState.selectedTipoId)
    val activeContainerColor = getRequestTypeContainerColor(uiState.selectedTipoId)
    val textOnActiveContainer = getContrastingTextColor(activeContainerColor)

    val cardBg = MaterialTheme.colorScheme.surface
    val cardTextColor = getContrastingTextColor(cardBg)

    @Composable
    fun PhotoAndProductBlock() {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Sección Multimedia: Foto y Recorte 16:9 (contenedor plano ligero)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, activeColor, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evidencia Fotográfica (Opcional 16:9)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cardTextColor
                    )
                    Text(
                        text = if (uiState.croppedBitmap169 != null) "Listo (16:9)" else "Opcional 16:9",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textOnActiveContainer,
                        modifier = Modifier
                            .background(activeContainerColor, RoundedCornerShape(4.dp))
                            .border(1.dp, activeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
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

                    FastOutlinedButton(
                        onClick = {
                            if (uncroppedBitmap != null) {
                                showCropDialog = true
                            }
                        },
                        containerColor = cardBg,
                        borderColor = activeColor,
                        modifier = Modifier.fillMaxWidth()
                    ) { contentColor ->
                        Icon(Icons.Default.Crop, contentDescription = null, tint = contentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reencuadrar Recorte 16:9", color = contentColor, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FastButton(
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
                            containerColor = activeColor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("request_button_camera")
                        ) { textOnActive ->
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = textOnActive)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cámara", color = textOnActive, fontWeight = FontWeight.Bold)
                        }

                        FastOutlinedButton(
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
                            containerColor = cardBg,
                            borderColor = activeColor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("request_button_gallery")
                        ) { contentColor ->
                            Icon(Icons.Default.Image, contentDescription = null, tint = contentColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería", color = contentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Datos del Producto y Variedades (con FastTextField y FastButton planos)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, activeColor, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (uiState.selectedTipoId == 1) "Datos del Producto" else "Producto a Modificar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )

                // Código Opcional en las 5 opciones
                FastTextField(
                    value = uiState.codigoProducto,
                    onValueChange = { viewModel.onCodigoChanged(it) },
                    label = "Código del Producto (Opcional)",
                    placeholder = "Ej: 100648",
                    containerColor = cardBg,
                    borderColor = activeColor,
                    modifier = Modifier.fillMaxWidth()
                )

                // Descripción Principal (Obligatorio)
                FastTextField(
                    value = uiState.descripcion,
                    onValueChange = { viewModel.onDescripcionChanged(it) },
                    label = "Descripción / Nombre Principal *",
                    placeholder = "Ej: Bandeja de Cartón N1",
                    containerColor = cardBg,
                    borderColor = activeColor,
                    modifier = Modifier.fillMaxWidth()
                )

                // Stock y Recarga del Nombre Principal (En Tipo 1 se oculta Stock Anterior por ser producto nuevo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.selectedTipoId != 1) {
                        FastTextField(
                            value = uiState.stockAnterior,
                            onValueChange = { viewModel.onStockAnteriorChanged(it) },
                            label = "Stock Principal (Opcional)",
                            placeholder = "Ej: 5 unid.",
                            containerColor = cardBg,
                            borderColor = activeColor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("request_main_stock_input")
                        )
                    }
                    FastTextField(
                        value = uiState.recargaStock,
                        onValueChange = { viewModel.onRecargaStockChanged(it) },
                        label = if (uiState.selectedTipoId == 1) "Recarga / Cantidad Inicial (Opcional)" else "Recarga Principal (Opcional)",
                        placeholder = "Ej: +24 unid.",
                        containerColor = cardBg,
                        borderColor = activeColor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("request_main_recarga_input")
                    )
                }

                HorizontalDivider(color = activeColor.copy(alpha = 0.3f))

                // Múltiples Variedades / Nombres con el Mismo Costo y su propio Stock / Recarga
                Text(
                    text = "Variedades / Nombres con mismo costo (Opcional):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )

                FastTextField(
                    value = newVarietyInput,
                    onValueChange = { newVarietyInput = it },
                    label = "Nombre de la variedad",
                    placeholder = "Ej: Sabor Vainilla, Medida 20cm...",
                    containerColor = cardBg,
                    borderColor = activeColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_new_variety_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (uiState.selectedTipoId != 1) {
                        FastTextField(
                            value = newVarietyStockInput,
                            onValueChange = { newVarietyStockInput = it },
                            label = "Stock Variedad",
                            placeholder = "Ej: 3 unid.",
                            containerColor = cardBg,
                            borderColor = activeColor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("request_new_variety_stock")
                        )
                    }
                    FastTextField(
                        value = newVarietyRecargaInput,
                        onValueChange = { newVarietyRecargaInput = it },
                        label = "Recarga Variedad",
                        placeholder = "Ej: +12 unid.",
                        containerColor = cardBg,
                        borderColor = activeColor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("request_new_variety_recarga")
                    )
                    FastButton(
                        onClick = {
                            if (newVarietyInput.isNotBlank()) {
                                viewModel.addVariety(
                                    name = newVarietyInput,
                                    stock = newVarietyStockInput,
                                    recarga = newVarietyRecargaInput
                                )
                                newVarietyInput = ""
                                newVarietyStockInput = ""
                                newVarietyRecargaInput = ""
                            }
                        },
                        containerColor = activeColor,
                        modifier = Modifier.testTag("request_add_variety_button")
                    ) { textOnActive ->
                        Icon(Icons.Default.Add, contentDescription = null, tint = textOnActive, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar", color = textOnActive, fontWeight = FontWeight.Bold)
                    }
                }

                // Edición individual de Stock y Recarga para cada variedad ya agregada
                if (uiState.variedadesList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.variedadesList.forEachIndexed { index, varietyItem ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardBg, RoundedCornerShape(6.dp))
                                    .border(1.dp, activeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Variedad #${index + 1}: ${varietyItem.nombre}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = cardTextColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeVariety(index) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Eliminar variedad",
                                            tint = cardTextColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (uiState.selectedTipoId != 1) {
                                        FastTextField(
                                            value = varietyItem.stock,
                                            onValueChange = { viewModel.updateVarietyStock(index, it) },
                                            label = "Stock (Opcional)",
                                            placeholder = "Sin declarar",
                                            containerColor = cardBg,
                                            borderColor = activeColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    FastTextField(
                                        value = varietyItem.recarga,
                                        onValueChange = { viewModel.updateVarietyRecarga(index, it) },
                                        label = "Recarga (Opcional)",
                                        placeholder = "Sin declarar",
                                        containerColor = cardBg,
                                        borderColor = activeColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Vista previa tipo lista con viñetas (·): omite Stock o Recarga si no se declararon
                if (uiState.descripcion.isNotBlank() || uiState.variedadesList.isNotEmpty()) {
                    val previewBg = activeContainerColor
                    val previewTextColor = getContrastingTextColor(previewBg)
                    val includeStockInPreview = uiState.selectedTipoId != 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(previewBg, RoundedCornerShape(6.dp))
                            .border(1.dp, activeColor, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Nombre Producto (Vista previa en imagen/TXT):",
                            style = MaterialTheme.typography.labelSmall,
                            color = previewTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.descripcion.isNotBlank()) {
                            val mainFormatted = VarietyStockItem(
                                nombre = uiState.descripcion,
                                stock = uiState.stockAnterior,
                                recarga = uiState.recargaStock
                            ).formatBulletLine(includeStock = includeStockInPreview)
                            Text(
                                text = "· $mainFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = previewTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        uiState.variedadesList.forEach { varietyItem ->
                            val formattedVariety = varietyItem.formatBulletLine(includeStock = includeStockInPreview)
                            Text(
                                text = "· $formattedVariety",
                                style = MaterialTheme.typography.bodySmall,
                                color = previewTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PricesAndSubmitBlock() {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // VALORES DE COSTO Y VENTA (contenedor plano ligero con FastTextField y FastChip)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, activeColor, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Valores de Costo y Venta",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )

                when (uiState.selectedTipoId) {
                    1 -> {
                        val parsedCosto = uiState.costoNuevo.toLongOrNull()
                        FastTextField(
                            value = uiState.costoNuevo,
                            onValueChange = { viewModel.onCostoNuevoChanged(it) },
                            label = "Precio de Costo (Gs.) *",
                            placeholder = "Ej: 3500",
                            containerColor = cardBg,
                            borderColor = activeColor,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = parsedCosto?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" },
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

                    2, 3 -> {
                        val parsedCostoAnt = uiState.costoAnteriorInput.toLongOrNull()
                        FastTextField(
                            value = uiState.costoAnteriorInput,
                            onValueChange = { viewModel.onCostoAnteriorChanged(it) },
                            label = "1. Precio de Costo Anterior (Gs.) *",
                            placeholder = "Ej: 3000",
                            containerColor = cardBg,
                            borderColor = activeColor,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = parsedCostoAnt?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_input_costo_anterior")
                        )

                        val parsedCostoNuevo = uiState.costoNuevo.toLongOrNull()
                        FastTextField(
                            value = uiState.costoNuevo,
                            onValueChange = { viewModel.onCostoNuevoChanged(it) },
                            label = "2. Precio de Costo Nuevo (Gs.) *",
                            placeholder = "Ej: 3500",
                            containerColor = cardBg,
                            borderColor = activeColor,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = parsedCostoNuevo?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_input_costo_nuevo")
                        )

                        HorizontalDivider(color = activeColor.copy(alpha = 0.35f))

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

                        HorizontalDivider(color = activeColor.copy(alpha = 0.35f))

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

                    4, 5 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Porcentaje deseado por el usuario:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = cardTextColor
                                )
                                Text(
                                    text = "${uiState.porcentajeDeseado}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textOnActiveContainer,
                                    modifier = Modifier
                                        .background(activeContainerColor, RoundedCornerShape(4.dp))
                                        .border(1.dp, activeColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SELECTABLE_PERCENTAGES.forEach { pct ->
                                    val isSelected = uiState.porcentajeDeseado == pct
                                    FastChip(
                                        selected = isSelected,
                                        onClick = { viewModel.onPorcentajeDeseadoChanged(pct) },
                                        label = "$pct%",
                                        selectedContainerColor = activeColor,
                                        unselectedContainerColor = activeContainerColor,
                                        borderColor = activeColor,
                                        modifier = Modifier.testTag("request_desired_pct_$pct")
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = activeColor.copy(alpha = 0.35f))

                        val parsedCosto = uiState.costoNuevo.toLongOrNull()
                        FastTextField(
                            value = uiState.costoNuevo,
                            onValueChange = { viewModel.onCostoNuevoChanged(it) },
                            label = "1. Precio de Costo (Gs.) *",
                            placeholder = "Ej: 3500",
                            containerColor = cardBg,
                            borderColor = activeColor,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = parsedCosto?.let { "Gs. ${MathUtils.formatearGuaranies(it)}" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_input_costo_nuevo")
                        )

                        HorizontalDivider(color = activeColor.copy(alpha = 0.35f))

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

                        HorizontalDivider(color = activeColor.copy(alpha = 0.35f))

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

            if (uiState.validationError != null) {
                val errBg = Color(0xFFFFEBEE)
                val errText = getContrastingTextColor(errBg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(errBg, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFB71C1C), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = errText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.validationError!!,
                        color = errText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val isEditingHistory = uiState.editingRequestId != null

            if (isEditingHistory) {
                FastOutlinedButton(
                    onClick = {
                        if (newVarietyInput.isNotBlank()) {
                            viewModel.addVariety(
                                name = newVarietyInput,
                                stock = newVarietyStockInput,
                                recarga = newVarietyRecargaInput
                            )
                            newVarietyInput = ""
                            newVarietyStockInput = ""
                            newVarietyRecargaInput = ""
                        }
                        viewModel.validateAndSubmit(
                            shareToWhatsApp = false,
                            onSavedOnly = {
                                Toast.makeText(
                                    context,
                                    "Solicitud actualizada en el historial",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onEditSavedToHistory?.invoke()
                            }
                        )
                    },
                    containerColor = cardBg,
                    borderColor = activeColor,
                    minHeight = 46.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_button_save_edit_only")
                ) { contentColor ->
                    Icon(Icons.Default.Check, contentDescription = null, tint = contentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Cambios en Historial",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }

            // Botón Principal plano - Envío a WhatsApp de SOLO LA IMAGEN RENDERIZADA
            FastButton(
                onClick = {
                    if (newVarietyInput.isNotBlank()) {
                        viewModel.addVariety(
                            name = newVarietyInput,
                            stock = newVarietyStockInput,
                            recarga = newVarietyRecargaInput
                        )
                        newVarietyInput = ""
                        newVarietyStockInput = ""
                        newVarietyRecargaInput = ""
                    }
                    viewModel.validateAndSubmit(
                        shareToWhatsApp = true,
                        onReadyToShareImageOnly = { imageUri ->
                            ImageUtils.shareImageOnlyToWhatsApp(context, imageUri)
                        }
                    )
                },
                containerColor = activeColor,
                minHeight = 48.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("request_button_submit_whatsapp")
            ) { textOnActive ->
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = textOnActive)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditingHistory) {
                        "Actualizar y Enviar a WhatsApp"
                    } else {
                        "Generar Ficha y Enviar a WhatsApp"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textOnActive
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoColumn = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.editingRequestId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeContainerColor, RoundedCornerShape(8.dp))
                        .border(1.5.dp, activeColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = textOnActiveContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editando solicitud del historial",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnActiveContainer
                        )
                    }
                    FastOutlinedButton(
                        onClick = { viewModel.cancelEditing() },
                        containerColor = cardBg,
                        borderColor = activeColor,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        minHeight = 32.dp,
                        modifier = Modifier.testTag("request_cancel_edit_button")
                    ) { contentColor ->
                        Text(
                            text = "Cancelar edición",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }

            // Encabezado plano del tipo de solicitud activo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeContainerColor, RoundedCornerShape(8.dp))
                    .border(1.5.dp, activeColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.getTipoTitle(uiState.selectedTipoId, uiState.porcentajeDeseado),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = textOnActiveContainer
                )
            }

            // Selector Horizontal plano de los 5 Tipos de Solicitud
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    FastChip(
                        selected = isSelected,
                        onClick = { viewModel.setTipoId(id) },
                        label = label,
                        selectedContainerColor = chipColor,
                        unselectedContainerColor = chipContainer,
                        borderColor = chipColor,
                        modifier = Modifier.testTag("request_tipo_chip_$id")
                    )
                }
            }

            if (isTwoColumn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PhotoAndProductBlock()
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PricesAndSubmitBlock()
                    }
                }
            } else {
                PhotoAndProductBlock()
                PricesAndSubmitBlock()
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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
 * from 20% to 50% (20%, 25%, 30%, 35%, 50%), using flat FastTextField and FastChip.
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
    val parsedVenta = priceValue.toLongOrNull()
    val supportingStr = parsedVenta?.let { v ->
        val pctSuffix = selectedPercentage?.let { " ($it%)" } ?: ""
        "Gs. ${MathUtils.formatearGuaranies(v)}$pctSuffix"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FastTextField(
            value = priceValue,
            onValueChange = onPriceChanged,
            label = priceLabel,
            placeholder = "Ej: 4500",
            containerColor = cardBg,
            borderColor = activeColor,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = supportingStr,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(priceTestTag)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Porcentaje seleccionable:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = cardTextColor
            )
            if (selectedPercentage != null) {
                Text(
                    text = "$selectedPercentage%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeTextColor,
                    modifier = Modifier
                        .background(activeContainerColor, RoundedCornerShape(4.dp))
                        .border(1.dp, activeColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SELECTABLE_PERCENTAGES.forEach { pct ->
                val isSelected = selectedPercentage == pct
                FastChip(
                    selected = isSelected,
                    onClick = { onSelectPercentage(pct) },
                    label = "$pct%",
                    selectedContainerColor = activeColor,
                    unselectedContainerColor = activeContainerColor,
                    borderColor = activeColor
                )
            }
        }
    }
}
