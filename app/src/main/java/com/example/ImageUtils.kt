package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object ImageUtils {

    private const val COLOR_TEXT_DARK = -0xEEEEEEF // #111111

    @Volatile
    private var cachedBoldTypeface: Typeface? = null

    private fun resolveTimesNewRomanBold(context: Context?): Typeface {
        cachedBoldTypeface?.let { return it }
        if (context != null) {
            try {
                val baseFont = ResourcesCompat.getFont(context.applicationContext, R.font.tinos)
                if (baseFont != null) {
                    val created = Typeface.create(baseFont, Typeface.BOLD)
                    cachedBoldTypeface = created
                    return created
                }
            } catch (_: Exception) {
                Unit
            }
        }
        return Typeface.create(Typeface.SERIF, Typeface.BOLD).also {
            cachedBoldTypeface = it
        }
    }

    fun decodeBitmapFromUri(context: Context, uri: Uri, targetMaxDim: Int = 1280): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            var sampleSize = 1
            var w = boundsOptions.outWidth
            var h = boundsOptions.outHeight
            while (w > targetMaxDim || h > targetMaxDim) {
                w = w ushr 1
                h = h ushr 1
                sampleSize = sampleSize shl 1
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
                inMutable = false
                inScaled = false
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            rotateBitmapIfRequired(rawBitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    fun cropTo16By9(
        source: Bitmap,
        cropRectNorm: RectF? = null
    ): Bitmap {
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)

        if (cropRectNorm != null) {
            val normL = cropRectNorm.left.coerceIn(0f, 1f)
            val normT = cropRectNorm.top.coerceIn(0f, 1f)
            val normR = cropRectNorm.right.coerceIn(normL, 1f)
            val normB = cropRectNorm.bottom.coerceIn(normT, 1f)

            var left = (normL * srcW).roundToInt().coerceIn(0, srcW - 1)
            var top = (normT * srcH).roundToInt().coerceIn(0, srcH - 1)
            var width = ((normR - normL) * srcW).roundToInt().coerceAtLeast(16)
            var height = ((normB - normT) * srcH).roundToInt().coerceAtLeast(9)

            if (width * 9 > height * 16) {
                width = (height * 16) / 9
            } else {
                height = (width * 9) / 16
            }

            width = width.coerceIn(1, srcW)
            height = height.coerceIn(1, srcH)

            if (left + width > srcW) left = (srcW - width).coerceAtLeast(0)
            if (top + height > srcH) top = (srcH - height).coerceAtLeast(0)

            return Bitmap.createBitmap(source, left, top, width, height)
        } else {
            val cropW: Int
            val cropH: Int
            val left: Int
            val top: Int
            if (srcW * 9 > srcH * 16) {
                cropH = srcH
                cropW = (srcH * 16) / 9
                left = (srcW - cropW) ushr 1
                top = 0
            } else {
                cropW = srcW
                cropH = (srcW * 9) / 16
                left = 0
                top = (srcH - cropH) ushr 1
            }
            val safeW = cropW.coerceIn(1, srcW - left)
            val safeH = cropH.coerceIn(1, srcH - top)
            return Bitmap.createBitmap(source, left, top, safeW, safeH)
        }
    }

    private fun getMonochromeTextColorInt(bgColor: Int): Int {
        return if (computeFastLumaScaled(bgColor) < 140_000) {
            AndroidColor.WHITE
        } else {
            COLOR_TEXT_DARK
        }
    }

    fun generateInfographicBitmap(
        context: Context? = null,
        photo16by9: Bitmap?,
        request: PriceRequest,
        workplaceName: String? = null
    ): Bitmap {
        val targetWidth = 1080
        val horizontalPad = 48f
        val contentWidth = (targetWidth - (horizontalPad * 2)).toInt()
        val boldTypeface = resolveTimesNewRomanBold(context)

        val softBgColor = when (request.tipoId) {
            1 -> -0xB040B // #F4FBF5
            2 -> -0xB0702 // #F4F8FE
            3 -> -0x00070E // #FFF8F2
            4 -> -0xD0407 // #F2FBF9
            5 -> -0x000A0B // #FFF5F5
            else -> AndroidColor.WHITE
        }

        val monoTextColor = getMonochromeTextColorInt(softBgColor)

        val photoWidth = targetWidth
        val photoHeight = if (photo16by9 != null) (targetWidth * 9) / 16 else 0

        val workplacePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = monoTextColor
            textSize = 32f
            typeface = boldTypeface
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = monoTextColor
            textSize = 42f
            typeface = boldTypeface
        }

        val bulletItemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = monoTextColor
            textSize = 40f
            typeface = boldTypeface
        }

        val secondaryInfoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = monoTextColor
            textSize = 36f
            typeface = boldTypeface
        }

        val priceBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = monoTextColor
            textSize = 42f
            typeface = boldTypeface
        }

        val workplaceLayout = workplaceName?.takeIf { it.isNotBlank() }?.let {
            buildStaticLayout("Local: ${it.trim()}", workplacePaint, contentWidth)
        }
        val titleLayout = buildStaticLayout(request.titulo, titlePaint, contentWidth)

        val bulletLines = request.lineasProductosFormateadas
        val bulletLayouts = ArrayList<StaticLayout>(bulletLines.size)
        for (itemText in bulletLines) {
            bulletLayouts.add(buildStaticLayout("· $itemText", bulletItemPaint, contentWidth))
        }

        val extraInfoLines = ArrayList<StaticLayout>(2)
        val codeVal = request.codigoEfectivo
        if (codeVal != null) {
            extraInfoLines.add(buildStaticLayout("Código: $codeVal", secondaryInfoPaint, contentWidth))
        }
        if (request.ordenFilaBoleta != null && request.ordenFilaBoleta > 0) {
            extraInfoLines.add(buildStaticLayout("Fila en boleta: ${request.ordenFilaBoleta}", secondaryInfoPaint, contentWidth))
        }

        val priceLayouts = buildRequestPriceLayouts(request, priceBoldPaint, contentWidth)

        var totalHeight = photoHeight + 32
        if (workplaceLayout != null) {
            totalHeight += workplaceLayout.height + 16
        }
        totalHeight += titleLayout.height + 16
        for (bLayout in bulletLayouts) {
            totalHeight += bLayout.height + 8
        }
        if (extraInfoLines.isNotEmpty()) {
            totalHeight += 16
            for (infoLayout in extraInfoLines) {
                totalHeight += infoLayout.height + 8
            }
        }
        totalHeight += 32
        for (pLayout in priceLayouts) {
            totalHeight += pLayout.height + 16
        }
        totalHeight += 40

        val composite = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(composite)
        canvas.drawColor(softBgColor)

        var currentY = 0f

        if (photo16by9 != null) {
            val srcRect = Rect(0, 0, photo16by9.width, photo16by9.height)
            val dstRect = Rect(0, 0, photoWidth, photoHeight)
            val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(photo16by9, srcRect, dstRect, photoPaint)
            currentY = photoHeight.toFloat()
        }

        currentY += 32f

        if (workplaceLayout != null) {
            drawStaticLayout(canvas, workplaceLayout, horizontalPad, currentY)
            currentY += workplaceLayout.height + 16f
        }

        drawStaticLayout(canvas, titleLayout, horizontalPad, currentY)
        currentY += titleLayout.height + 16f

        for (bLayout in bulletLayouts) {
            drawStaticLayout(canvas, bLayout, horizontalPad, currentY)
            currentY += bLayout.height + 8f
        }

        if (extraInfoLines.isNotEmpty()) {
            currentY += 16f
            for (infoLayout in extraInfoLines) {
                drawStaticLayout(canvas, infoLayout, horizontalPad, currentY)
                currentY += infoLayout.height + 8f
            }
        }

        currentY += 32f

        for (pLayout in priceLayouts) {
            drawStaticLayout(canvas, pLayout, horizontalPad, currentY)
            currentY += pLayout.height + 16f
        }

        return composite
    }

    fun generateMonochromeCatalogBitmap(
        context: Context? = null,
        workplaceName: String,
        products: List<Product>,
        includeCodigo: Boolean = true,
        includeCosto: Boolean = true,
        includeVenta: Boolean = true,
        includeMargen: Boolean = true
    ): Bitmap {
        val targetWidth = 1080
        val horizontalPad = 48f
        val contentWidth = (targetWidth - (horizontalPad * 2)).toInt()
        val boldTypeface = resolveTimesNewRomanBold(context)

        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 42f
            typeface = boldTypeface
        }

        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 34f
            typeface = boldTypeface
        }

        val itemNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 38f
            typeface = boldTypeface
        }

        val itemDetailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 36f
            typeface = boldTypeface
        }

        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val headerLayout = buildStaticLayout("Catálogo de Precios - $cleanWorkplace", headerTitlePaint, contentWidth)
        val countLayout = buildStaticLayout("Total de productos incluidos: ${products.size}", subHeaderPaint, contentWidth)

        val productBlocks = ArrayList<List<StaticLayout>>(products.size)
        for (prod in products) {
            val lines = ArrayList<StaticLayout>(4)
            lines.add(buildStaticLayout("· ${prod.descripcion}", itemNamePaint, contentWidth))
            if (includeCodigo) {
                lines.add(buildStaticLayout("Código: ${prod.codigoMostrar}", itemDetailPaint, contentWidth))
            }
            if (includeCosto) {
                val costoStr = prod.precioCosto?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                lines.add(buildStaticLayout("Precio de Costo Gs.: $costoStr", itemDetailPaint, contentWidth))
            }
            if (includeVenta) {
                val ventaStr = prod.precioVenta?.let { MathUtils.formatearGuaranies(it) } ?: "Pendiente"
                val marginSuffix = if (includeMargen && prod.porcentajeRealRedondeado != null) {
                    " (${prod.porcentajeRealRedondeado}%)"
                } else ""
                lines.add(buildStaticLayout("Precio de Venta Gs.: $ventaStr$marginSuffix", itemDetailPaint, contentWidth))
            }
            productBlocks.add(lines)
        }

        var totalHeight = 40
        totalHeight += headerLayout.height + 16
        totalHeight += countLayout.height + 32
        for (block in productBlocks) {
            totalHeight += 16
            for (line in block) {
                totalHeight += line.height + 8
            }
            totalHeight += 24
        }
        totalHeight += 40

        val safeHeight = totalHeight.coerceAtLeast(300)
        val bitmap = Bitmap.createBitmap(targetWidth, safeHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        var currentY = 40f
        drawStaticLayout(canvas, headerLayout, horizontalPad, currentY)
        currentY += headerLayout.height + 16f

        drawStaticLayout(canvas, countLayout, horizontalPad, currentY)
        currentY += countLayout.height + 32f

        for (block in productBlocks) {
            currentY += 16f
            for (line in block) {
                drawStaticLayout(canvas, line, horizontalPad, currentY)
                currentY += line.height + 8f
            }
            currentY += 24f
        }

        return bitmap
    }

    fun generateMonochromeDistributorBitmap(
        context: Context? = null,
        workplaceName: String,
        distributorName: String,
        items: List<DistributorItem>
    ): Bitmap {
        val targetWidth = 1080
        val horizontalPad = 48f
        val contentWidth = (targetWidth - (horizontalPad * 2)).toInt()
        val boldTypeface = resolveTimesNewRomanBold(context)

        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 42f
            typeface = boldTypeface
        }

        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 34f
            typeface = boldTypeface
        }

        val itemNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 38f
            typeface = boldTypeface
        }

        val itemDetailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 36f
            typeface = boldTypeface
        }

        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val cleanDistributor = distributorName.trim().ifBlank { "Distribuidor" }
        val orderedItems = if (items.size <= 1) items else items.sortedWith(DistributorItem.ROW_COMPARATOR)

        val headerLayout = buildStaticLayout("Boleta de Distribuidor: $cleanDistributor", headerTitlePaint, contentWidth)
        val subHeaderLayout = buildStaticLayout("Local: $cleanWorkplace · Filas incluidas: ${orderedItems.size}", subHeaderPaint, contentWidth)

        val itemBlocks = ArrayList<List<StaticLayout>>(orderedItems.size)
        for (item in orderedItems) {
            val lines = ArrayList<StaticLayout>(8)
            lines.add(
                buildStaticLayout(
                    "Fila ${item.ordenFila} · ${item.titulo}",
                    itemNamePaint,
                    contentWidth
                )
            )
            for (bulletLine in item.lineasProductosFormateadas) {
                lines.add(buildStaticLayout("· $bulletLine", itemDetailPaint, contentWidth))
            }
            val cod = item.codigoEfectivo
            if (cod != null) {
                lines.add(buildStaticLayout("Código: $cod", itemDetailPaint, contentWidth))
            }
            when (item.tipoId) {
                2, 3 -> {
                    lines.add(buildStaticLayout(item.lineaCostoAnterior, itemDetailPaint, contentWidth))
                    lines.add(buildStaticLayout(item.lineaCostoNuevo, itemDetailPaint, contentWidth))
                    lines.add(buildStaticLayout(item.lineaVentaAnterior, itemDetailPaint, contentWidth))
                    lines.add(buildStaticLayout(item.lineaVentaNueva, itemDetailPaint, contentWidth))
                }
                4, 5 -> {
                    lines.add(buildStaticLayout(item.lineaCostoUnico, itemDetailPaint, contentWidth))
                    lines.add(buildStaticLayout(item.lineaVentaAnterior, itemDetailPaint, contentWidth))
                    lines.add(buildStaticLayout(item.lineaVentaNueva, itemDetailPaint, contentWidth))
                }
                else -> {
                    lines.add(buildStaticLayout(item.lineaCostoUnico, itemDetailPaint, contentWidth))
                    lines.add(buildStaticLayout(item.lineaVentaUnica, itemDetailPaint, contentWidth))
                }
            }
            itemBlocks.add(lines)
        }

        var totalHeight = 40
        totalHeight += headerLayout.height + 16
        totalHeight += subHeaderLayout.height + 32
        for (block in itemBlocks) {
            totalHeight += 16
            for (line in block) {
                totalHeight += line.height + 8
            }
            totalHeight += 24
        }
        totalHeight += 40

        val safeHeight = totalHeight.coerceAtLeast(300)
        val bitmap = Bitmap.createBitmap(targetWidth, safeHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        var currentY = 40f
        drawStaticLayout(canvas, headerLayout, horizontalPad, currentY)
        currentY += headerLayout.height + 16f

        drawStaticLayout(canvas, subHeaderLayout, horizontalPad, currentY)
        currentY += subHeaderLayout.height + 32f

        for (block in itemBlocks) {
            currentY += 16f
            for (line in block) {
                drawStaticLayout(canvas, line, horizontalPad, currentY)
                currentY += line.height + 8f
            }
            currentY += 24f
        }

        return bitmap
    }

    fun buildDistributorPlainText(
        workplaceName: String,
        distributorName: String,
        items: List<DistributorItem>
    ): String {
        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val cleanDistributor = distributorName.trim().ifBlank { "Distribuidor" }
        val orderedItems = if (items.size <= 1) items else items.sortedWith(DistributorItem.ROW_COMPARATOR)
        val sb = StringBuilder(orderedItems.size * 180 + 96)
        sb.append("*Boleta de Distribuidor: ").append(cleanDistributor).append("*\n")
        sb.append("Local: ").append(cleanWorkplace).append("\n\n")

        for (item in orderedItems) {
            sb.append("*Fila ").append(item.ordenFila).append(" · ").append(item.titulo).append("*\n")
            for (bulletLine in item.lineasProductosFormateadas) {
                sb.append("· ").append(bulletLine).append('\n')
            }
            if (item.codigoEfectivo != null) {
                sb.append("Código: ").append(item.codigoEfectivo).append('\n')
            }
            when (item.tipoId) {
                2, 3 -> {
                    sb.append(item.lineaCostoAnterior).append('\n')
                    sb.append(item.lineaCostoNuevo).append('\n')
                    sb.append(item.lineaVentaAnterior).append('\n')
                    sb.append(item.lineaVentaNueva).append("\n\n")
                }
                4, 5 -> {
                    sb.append(item.lineaCostoUnico).append('\n')
                    sb.append(item.lineaVentaAnterior).append('\n')
                    sb.append(item.lineaVentaNueva).append("\n\n")
                }
                else -> {
                    sb.append(item.lineaCostoUnico).append('\n')
                    sb.append(item.lineaVentaUnica).append("\n\n")
                }
            }
        }
        sb.append("Total de filas: ").append(orderedItems.size)
        return sb.toString()
    }

    fun generateMonochromeHistoryBitmap(
        context: Context? = null,
        workplaceName: String,
        requests: List<PriceRequest>
    ): Bitmap {
        val targetWidth = 1080
        val horizontalPad = 48f
        val contentWidth = (targetWidth - (horizontalPad * 2)).toInt()
        val boldTypeface = resolveTimesNewRomanBold(context)

        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 42f
            typeface = boldTypeface
        }

        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 34f
            typeface = boldTypeface
        }

        val reqTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 38f
            typeface = boldTypeface
        }

        val itemDetailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 36f
            typeface = boldTypeface
        }

        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val headerTitle = if (requests.size == 1) {
            "Solicitud de Precio - $cleanWorkplace"
        } else {
            "Historial de Solicitudes - $cleanWorkplace"
        }
        val headerLayout = buildStaticLayout(headerTitle, headerTitlePaint, contentWidth)
        val countLayout = buildStaticLayout("Total de registros: ${requests.size}", subHeaderPaint, contentWidth)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val requestBlocks = ArrayList<List<StaticLayout>>(requests.size)
        for (req in requests) {
            val lines = ArrayList<StaticLayout>(8)
            val dateStr = dateFormat.format(Date(req.timestamp))
            lines.add(buildStaticLayout("${req.titulo} ($dateStr)", reqTitlePaint, contentWidth))

            for (itemLine in req.lineasProductosFormateadas) {
                lines.add(buildStaticLayout("· $itemLine", itemDetailPaint, contentWidth))
            }

            val code = req.codigoEfectivo
            if (code != null) {
                lines.add(buildStaticLayout("Código: $code", itemDetailPaint, contentWidth))
            }

            lines.addAll(buildRequestPriceLayouts(req, itemDetailPaint, contentWidth))
            requestBlocks.add(lines)
        }

        var totalHeight = 40
        totalHeight += headerLayout.height + 16
        totalHeight += countLayout.height + 32
        for (block in requestBlocks) {
            totalHeight += 16
            for (line in block) {
                totalHeight += line.height + 8
            }
            totalHeight += 24
        }
        totalHeight += 40

        val safeHeight = totalHeight.coerceAtLeast(300)
        val bitmap = Bitmap.createBitmap(targetWidth, safeHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        var currentY = 40f
        drawStaticLayout(canvas, headerLayout, horizontalPad, currentY)
        currentY += headerLayout.height + 16f

        drawStaticLayout(canvas, countLayout, horizontalPad, currentY)
        currentY += countLayout.height + 32f

        for (block in requestBlocks) {
            currentY += 16f
            for (line in block) {
                drawStaticLayout(canvas, line, horizontalPad, currentY)
                currentY += line.height + 8f
            }
            currentY += 24f
        }

        return bitmap
    }

    fun buildHistoryPlainText(
        workplaceName: String,
        requests: List<PriceRequest>
    ): String {
        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder(requests.size * 180 + 96)
        sb.append("*Historial de Solicitudes - ").append(cleanWorkplace).append("*\n\n")

        for (req in requests) {
            val dateStr = dateFormat.format(Date(req.timestamp))
            sb.append('*').append(req.titulo).append("* (").append(dateStr).append(")\n")

            for (itemLine in req.lineasProductosFormateadas) {
                sb.append("· ").append(itemLine).append('\n')
            }

            val code = req.codigoEfectivo
            if (code != null) {
                sb.append("Código: ").append(code).append('\n')
            }

            when (req.tipoId) {
                2, 3 -> {
                    sb.append(req.lineaCostoAnterior).append('\n')
                    sb.append(req.lineaCostoNuevo).append('\n')
                    sb.append(req.lineaVentaAnterior).append('\n')
                    sb.append(req.lineaVentaNueva).append('\n')
                }
                4, 5 -> {
                    sb.append(req.lineaCostoUnico).append('\n')
                    sb.append(req.lineaVentaAnterior).append('\n')
                    sb.append(req.lineaVentaNueva).append('\n')
                }
                else -> {
                    sb.append(req.lineaCostoUnico).append('\n')
                    sb.append(req.lineaVentaUnica).append('\n')
                }
            }
            sb.append('\n')
        }
        sb.append("Total de solicitudes: ").append(requests.size)
        return sb.toString()
    }

    private fun buildRequestPriceLayouts(
        request: PriceRequest,
        priceBoldPaint: TextPaint,
        contentWidth: Int
    ): List<StaticLayout> {
        val priceLayouts = ArrayList<StaticLayout>(4)
        when (request.tipoId) {
            2, 3 -> {
                priceLayouts.add(buildStaticLayout(request.lineaCostoAnterior, priceBoldPaint, contentWidth))
                priceLayouts.add(buildStaticLayout(request.lineaCostoNuevo, priceBoldPaint, contentWidth))
                priceLayouts.add(buildStaticLayout(request.lineaVentaAnterior, priceBoldPaint, contentWidth))
                priceLayouts.add(buildStaticLayout(request.lineaVentaNueva, priceBoldPaint, contentWidth))
            }
            4, 5 -> {
                priceLayouts.add(buildStaticLayout(request.lineaCostoUnico, priceBoldPaint, contentWidth))
                priceLayouts.add(buildStaticLayout(request.lineaVentaAnterior, priceBoldPaint, contentWidth))
                priceLayouts.add(buildStaticLayout(request.lineaVentaNueva, priceBoldPaint, contentWidth))
            }
            else -> {
                priceLayouts.add(buildStaticLayout(request.lineaCostoUnico, priceBoldPaint, contentWidth))
                priceLayouts.add(buildStaticLayout(request.lineaVentaUnica, priceBoldPaint, contentWidth))
            }
        }
        return priceLayouts
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(100))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setLineSpacing(2f, 1.05f)
            .setIncludePad(false)
            .build()
    }

    private fun drawStaticLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "shared_images")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                stream.flush()
            }

            bitmap.recycle()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareImageOnlyToWhatsApp(context: Context, imageUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val whatsappIntent = Intent(intent).apply {
            `package` = "com.whatsapp"
        }

        val launchIntent = if (whatsappIntent.resolveActivity(context.packageManager) != null) {
            whatsappIntent
        } else {
            Intent.createChooser(intent, "Enviar imagen por WhatsApp")
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    fun shareTextToWhatsApp(context: Context, textContent: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textContent)
        }

        val whatsappIntent = Intent(intent).apply {
            `package` = "com.whatsapp"
        }

        val launchIntent = if (whatsappIntent.resolveActivity(context.packageManager) != null) {
            whatsappIntent
        } else {
            Intent.createChooser(intent, "Compartir texto por WhatsApp")
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
