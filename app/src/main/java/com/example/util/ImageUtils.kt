package com.example.util

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
import androidx.core.graphics.ColorUtils
import com.example.data.model.PriceRequest
import com.example.data.model.Product
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object ImageUtils {

    private val TYPEFACE_BOLD: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    /**
     * Decodes and downsamples a Bitmap from URI to fit strictly within 1280x720 (HD)
     * to prevent OutOfMemoryError on low-end devices (1GB - 3GB RAM).
     */
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
                w /= 2
                h /= 2
                sampleSize *= 2
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

    /**
     * Crops a bitmap to exact 16:9 aspect ratio using the normalized selection rectangle
     * (0f..1f relative to source bitmap width and height).
     */
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
                left = (srcW - cropW) / 2
                top = 0
            } else {
                cropW = srcW
                cropH = (srcW * 9) / 16
                left = 0
                top = (srcH - cropH) / 2
            }
            val safeW = cropW.coerceIn(1, srcW - left)
            val safeH = cropH.coerceIn(1, srcH - top)
            return Bitmap.createBitmap(source, left, top, safeW, safeH)
        }
    }

    /**
     * Returns monochrome primary text color (Black or White) based on background luminance.
     */
    private fun getMonochromeTextColorInt(bgColor: Int): Int {
        return if (ColorUtils.calculateLuminance(bgColor) < 0.45) {
            AndroidColor.WHITE
        } else {
            AndroidColor.parseColor("#111111")
        }
    }

    /**
     * Generates a borderless, full-width composite infographic image for a PriceRequest:
     * - Full-width 16:9 photo at the top if present (optional)
     * - Monochrome text (#111111) across all labels, names, and prices
     * - Exact price ordering per Request Type (1, 2-3, 4-5)
     */
    fun generateInfographicBitmap(
        photo16by9: Bitmap?,
        request: PriceRequest,
        workplaceName: String? = null
    ): Bitmap {
        val targetWidth = 1080
        val horizontalPad = 44f
        val contentWidth = (targetWidth - (horizontalPad * 2)).toInt()

        val softBgColor = when (request.tipoId) {
            1 -> AndroidColor.parseColor("#F4FBF5")
            2 -> AndroidColor.parseColor("#F4F8FE")
            3 -> AndroidColor.parseColor("#FFF8F2")
            4 -> AndroidColor.parseColor("#F2FBF9")
            5 -> AndroidColor.parseColor("#FFF5F5")
            else -> AndroidColor.parseColor("#FFFFFF")
        }

        val monoTextColor = getMonochromeTextColorInt(softBgColor)

        val photoWidth = targetWidth
        val photoHeight = if (photo16by9 != null) (targetWidth * 9) / 16 else 0

        val workplacePaint = TextPaint().apply {
            color = monoTextColor
            textSize = 32f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val titlePaint = TextPaint().apply {
            color = monoTextColor
            textSize = 42f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val labelBoldPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 40f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val bulletItemPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 40f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val secondaryInfoPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 36f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val priceBoldPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 42f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val workplaceLayout = workplaceName?.takeIf { it.isNotBlank() }?.let {
            buildStaticLayout("Local: ${it.trim()}", workplacePaint, contentWidth)
        }
        val titleLayout = buildStaticLayout(request.titulo, titlePaint, contentWidth)
        val nombreHeaderLayout = buildStaticLayout("Nombre Producto:", labelBoldPaint, contentWidth)

        val bulletLayouts = request.lineasProductosFormateadas.map { itemText ->
            buildStaticLayout("· $itemText", bulletItemPaint, contentWidth)
        }

        val extraInfoLines = mutableListOf<StaticLayout>()
        val codeVal = request.codigoEfectivo
        if (codeVal != null) {
            extraInfoLines.add(buildStaticLayout("Código: $codeVal", secondaryInfoPaint, contentWidth))
        }

        val priceLayouts = buildRequestPriceLayouts(request, priceBoldPaint, contentWidth)

        var totalHeight = photoHeight + 36
        if (workplaceLayout != null) {
            totalHeight += workplaceLayout.height + 10
        }
        totalHeight += titleLayout.height + 16
        totalHeight += nombreHeaderLayout.height + 10
        for (bLayout in bulletLayouts) {
            totalHeight += bLayout.height + 8
        }
        if (extraInfoLines.isNotEmpty()) {
            totalHeight += 18
            for (infoLayout in extraInfoLines) {
                totalHeight += infoLayout.height + 8
            }
        }
        totalHeight += 32
        for (pLayout in priceLayouts) {
            totalHeight += pLayout.height + 14
        }
        totalHeight += 40

        val composite = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(composite)

        val bgPaint = Paint().apply {
            color = softBgColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), totalHeight.toFloat(), bgPaint)

        var currentY = 0f

        if (photo16by9 != null) {
            val srcRect = Rect(0, 0, photo16by9.width, photo16by9.height)
            val dstRect = Rect(0, 0, photoWidth, photoHeight)
            val photoPaint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(photo16by9, srcRect, dstRect, photoPaint)
            currentY = photoHeight.toFloat()
        }

        currentY += 36f

        if (workplaceLayout != null) {
            drawStaticLayout(canvas, workplaceLayout, horizontalPad, currentY)
            currentY += workplaceLayout.height + 10f
        }

        drawStaticLayout(canvas, titleLayout, horizontalPad, currentY)
        currentY += titleLayout.height + 16f

        drawStaticLayout(canvas, nombreHeaderLayout, horizontalPad, currentY)
        currentY += nombreHeaderLayout.height + 10f

        for (bLayout in bulletLayouts) {
            drawStaticLayout(canvas, bLayout, horizontalPad, currentY)
            currentY += bLayout.height + 8f
        }

        if (extraInfoLines.isNotEmpty()) {
            currentY += 18f
            for (infoLayout in extraInfoLines) {
                drawStaticLayout(canvas, infoLayout, horizontalPad, currentY)
                currentY += infoLayout.height + 8f
            }
        }

        currentY += 32f

        for (pLayout in priceLayouts) {
            drawStaticLayout(canvas, pLayout, horizontalPad, currentY)
            currentY += pLayout.height + 14f
        }

        return composite
    }

    /**
     * Generates a strictly monochrome (white background #FFFFFF, black text #111111, black dividing lines #111111)
     * rendered image of the Catalog (selected products or entire catalog), without requiring any camera or gallery photos.
     * Uses the exact consultation/request layout structure.
     */
    fun generateMonochromeCatalogBitmap(
        workplaceName: String,
        products: List<Product>,
        includeCodigo: Boolean = true,
        includeCosto: Boolean = true,
        includeVenta: Boolean = true,
        includeMargen: Boolean = true
    ): Bitmap {
        val targetWidth = 1080
        val horizontalPad = 44f
        val contentWidth = (targetWidth - (horizontalPad * 2)).toInt()
        val whiteColor = AndroidColor.WHITE
        val blackColor = AndroidColor.parseColor("#111111")

        val headerTitlePaint = TextPaint().apply {
            color = blackColor
            textSize = 42f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val subHeaderPaint = TextPaint().apply {
            color = blackColor
            textSize = 34f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val itemLabelPaint = TextPaint().apply {
            color = blackColor
            textSize = 36f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val itemNamePaint = TextPaint().apply {
            color = blackColor
            textSize = 38f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val itemDetailPaint = TextPaint().apply {
            color = blackColor
            textSize = 36f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val headerLayout = buildStaticLayout("CATÁLOGO DE PRECIOS - ${cleanWorkplace.uppercase()}", headerTitlePaint, contentWidth)
        val countLayout = buildStaticLayout("Total de productos incluidos: ${products.size}", subHeaderPaint, contentWidth)

        // Pre-build layouts per product block
        val productBlocks = products.map { prod ->
            val lines = mutableListOf<StaticLayout>()
            lines.add(buildStaticLayout("Nombre Producto:", itemLabelPaint, contentWidth))
            lines.add(buildStaticLayout("· ${prod.descripcion}", itemNamePaint, contentWidth))
            if (includeCodigo) {
                val cod = prod.codigo?.takeIf { it.isNotBlank() } ?: "Sin código"
                lines.add(buildStaticLayout("Código: $cod", itemDetailPaint, contentWidth))
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
            lines
        }

        var totalHeight = 40
        totalHeight += headerLayout.height + 12
        totalHeight += countLayout.height + 24
        totalHeight += 16 // Top divider space
        for (block in productBlocks) {
            totalHeight += 18
            for (line in block) {
                totalHeight += line.height + 8
            }
            totalHeight += 22 // Divider + bottom spacing per product
        }
        totalHeight += 36

        val safeHeight = totalHeight.coerceAtLeast(300)
        val bitmap = Bitmap.createBitmap(targetWidth, safeHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = whiteColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), safeHeight.toFloat(), bgPaint)

        val dividerPaint = Paint().apply {
            color = blackColor
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val thickDividerPaint = Paint().apply {
            color = blackColor
            strokeWidth = 5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        var currentY = 40f
        drawStaticLayout(canvas, headerLayout, horizontalPad, currentY)
        currentY += headerLayout.height + 12f

        drawStaticLayout(canvas, countLayout, horizontalPad, currentY)
        currentY += countLayout.height + 20f

        canvas.drawLine(horizontalPad, currentY, targetWidth - horizontalPad, currentY, thickDividerPaint)
        currentY += 16f

        for (block in productBlocks) {
            currentY += 14f
            for (line in block) {
                drawStaticLayout(canvas, line, horizontalPad, currentY)
                currentY += line.height + 8f
            }
            currentY += 12f
            canvas.drawLine(horizontalPad, currentY, targetWidth - horizontalPad, currentY, dividerPaint)
            currentY += 10f
        }

        return bitmap
    }

    /**
     * Generates a strictly monochrome (white background #FFFFFF, black text #111111, black dividing lines #111111)
     * rendered image of the Request History (single or multiple PriceRequests), identical in text structure
     * to price change requests/consultations.
     */
    fun generateMonochromeHistoryBitmap(
        workplaceName: String,
        requests: List<PriceRequest>
    ): Bitmap {
        val targetWidth = 1080
        val horizontalPad = 44f
        val contentWidth = (targetWidth - (horizontalPad * 2)).toInt()
        val whiteColor = AndroidColor.WHITE
        val blackColor = AndroidColor.parseColor("#111111")

        val headerTitlePaint = TextPaint().apply {
            color = blackColor
            textSize = 42f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val subHeaderPaint = TextPaint().apply {
            color = blackColor
            textSize = 34f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val reqTitlePaint = TextPaint().apply {
            color = blackColor
            textSize = 38f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val itemLabelPaint = TextPaint().apply {
            color = blackColor
            textSize = 36f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val itemDetailPaint = TextPaint().apply {
            color = blackColor
            textSize = 36f
            typeface = TYPEFACE_BOLD
            isAntiAlias = true
        }

        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val headerTitle = if (requests.size == 1) {
            "SOLICITUD DE PRECIO - ${cleanWorkplace.uppercase()}"
        } else {
            "HISTORIAL DE SOLICITUDES - ${cleanWorkplace.uppercase()}"
        }
        val headerLayout = buildStaticLayout(headerTitle, headerTitlePaint, contentWidth)
        val countLayout = buildStaticLayout("Total de registros: ${requests.size}", subHeaderPaint, contentWidth)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val requestBlocks = requests.map { req ->
            val lines = mutableListOf<StaticLayout>()
            val dateStr = dateFormat.format(Date(req.timestamp))
            lines.add(buildStaticLayout("${req.titulo} ($dateStr)", reqTitlePaint, contentWidth))
            lines.add(buildStaticLayout("Nombre Producto:", itemLabelPaint, contentWidth))

            req.lineasProductosFormateadas.forEach { itemLine ->
                lines.add(buildStaticLayout("· $itemLine", itemDetailPaint, contentWidth))
            }

            val code = req.codigoEfectivo
            if (code != null) {
                lines.add(buildStaticLayout("Código: $code", itemDetailPaint, contentWidth))
            }

            lines.addAll(buildRequestPriceLayouts(req, itemDetailPaint, contentWidth))
            lines
        }

        var totalHeight = 40
        totalHeight += headerLayout.height + 12
        totalHeight += countLayout.height + 24
        totalHeight += 16
        for (block in requestBlocks) {
            totalHeight += 18
            for (line in block) {
                totalHeight += line.height + 8
            }
            totalHeight += 24
        }
        totalHeight += 36

        val safeHeight = totalHeight.coerceAtLeast(300)
        val bitmap = Bitmap.createBitmap(targetWidth, safeHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = whiteColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), safeHeight.toFloat(), bgPaint)

        val dividerPaint = Paint().apply {
            color = blackColor
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val thickDividerPaint = Paint().apply {
            color = blackColor
            strokeWidth = 5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        var currentY = 40f
        drawStaticLayout(canvas, headerLayout, horizontalPad, currentY)
        currentY += headerLayout.height + 12f

        drawStaticLayout(canvas, countLayout, horizontalPad, currentY)
        currentY += countLayout.height + 20f

        canvas.drawLine(horizontalPad, currentY, targetWidth - horizontalPad, currentY, thickDividerPaint)
        currentY += 16f

        for (block in requestBlocks) {
            currentY += 14f
            for (line in block) {
                drawStaticLayout(canvas, line, horizontalPad, currentY)
                currentY += line.height + 8f
            }
            currentY += 14f
            canvas.drawLine(horizontalPad, currentY, targetWidth - horizontalPad, currentY, dividerPaint)
            currentY += 10f
        }

        return bitmap
    }

    /**
     * Builds a clean, emoji-free plain-text representation of the Request History with the custom workplace name.
     */
    fun buildHistoryPlainText(
        workplaceName: String,
        requests: List<PriceRequest>
    ): String {
        val cleanWorkplace = workplaceName.trim().ifBlank { "Mi Local Comercial" }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("*HISTORIAL DE SOLICITUDES - ${cleanWorkplace.uppercase()}*\n")
        sb.append("----------------------------------------\n")

        for (req in requests) {
            val dateStr = dateFormat.format(Date(req.timestamp))
            sb.append("*${req.titulo}* ($dateStr)\n")
            sb.append("Nombre Producto:\n")

            req.lineasProductosFormateadas.forEach { itemLine ->
                sb.append("· $itemLine\n")
            }

            val code = req.codigoEfectivo
            if (code != null) {
                sb.append("Código: $code\n")
            }

            when (req.tipoId) {
                2, 3 -> {
                    val cAnt = req.precioCostoAnterior ?: req.precioCostoNuevo
                    val vAnt = req.precioVentaAnterior ?: req.precioVentaNuevo
                    val pAnt = req.porcentajeAnteriorRedondeado
                        ?: MathUtils.calcularPorcentajeRedondeado(cAnt, vAnt)

                    sb.append("Precio de Costo Anterior Gs.: ${MathUtils.formatearGuaranies(cAnt)}\n")
                    sb.append("Precio de Costo Nuevo Gs.: ${MathUtils.formatearGuaranies(req.precioCostoNuevo)}\n")
                    sb.append("Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)\n")
                    sb.append("Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(req.precioVentaNuevo)} (${req.porcentajeNuevoRedondeado}%)\n")
                }
                4, 5 -> {
                    val vAnt = req.precioVentaAnterior ?: req.precioVentaNuevo
                    val pAnt = req.porcentajeAnteriorRedondeado
                        ?: MathUtils.calcularPorcentajeRedondeado(req.precioCostoNuevo, vAnt)

                    sb.append("Precio de Costo Gs.: ${MathUtils.formatearGuaranies(req.precioCostoNuevo)}\n")
                    sb.append("Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)\n")
                    sb.append("Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(req.precioVentaNuevo)} (${req.porcentajeNuevoRedondeado}%)\n")
                }
                else -> {
                    sb.append("Precio de Costo Gs.: ${MathUtils.formatearGuaranies(req.precioCostoNuevo)}\n")
                    sb.append("Precio de Venta Gs.: ${MathUtils.formatearGuaranies(req.precioVentaNuevo)} (${req.porcentajeNuevoRedondeado}%)\n")
                }
            }
            sb.append("----------------------------------------\n")
        }
        sb.append("Total de solicitudes: ${requests.size}")
        return sb.toString()
    }

    private fun buildRequestPriceLayouts(
        request: PriceRequest,
        priceBoldPaint: TextPaint,
        contentWidth: Int
    ): List<StaticLayout> {
        val priceLayouts = mutableListOf<StaticLayout>()
        when (request.tipoId) {
            1 -> {
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                        priceBoldPaint,
                        contentWidth
                    )
                )
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Venta Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                        priceBoldPaint,
                        contentWidth
                    )
                )
            }
            2, 3 -> {
                val cAnt = request.precioCostoAnterior ?: request.precioCostoNuevo
                val vAnt = request.precioVentaAnterior ?: request.precioVentaNuevo
                val pAnt = request.porcentajeAnteriorRedondeado ?: MathUtils.calcularPorcentajeRedondeado(cAnt, vAnt)

                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Costo Anterior Gs.: ${MathUtils.formatearGuaranies(cAnt)}",
                        priceBoldPaint,
                        contentWidth
                    )
                )
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Costo Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                        priceBoldPaint,
                        contentWidth
                    )
                )
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)",
                        priceBoldPaint,
                        contentWidth
                    )
                )
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                        priceBoldPaint,
                        contentWidth
                    )
                )
            }
            4, 5 -> {
                val vAnt = request.precioVentaAnterior ?: request.precioVentaNuevo
                val pAnt = request.porcentajeAnteriorRedondeado
                    ?: MathUtils.calcularPorcentajeRedondeado(request.precioCostoNuevo, vAnt)

                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                        priceBoldPaint,
                        contentWidth
                    )
                )
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)",
                        priceBoldPaint,
                        contentWidth
                    )
                )
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                        priceBoldPaint,
                        contentWidth
                    )
                )
            }
            else -> {
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                        priceBoldPaint,
                        contentWidth
                    )
                )
                priceLayouts.add(
                    buildStaticLayout(
                        "Precio de Venta Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                        priceBoldPaint,
                        contentWidth
                    )
                )
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

    /**
     * Saves bitmap to app cache directory as JPEG with 85% quality to optimize memory
     * and storage on low-end hardware. Recycles bitmap immediately after writing.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "shared_images")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, fileName)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.flush()
            stream.close()

            bitmap.recycle()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Sends ONLY the generated image to WhatsApp without any Intent.EXTRA_TEXT accompanying it.
     */
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

    /**
     * Share method for text catalog or history exports.
     */
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
