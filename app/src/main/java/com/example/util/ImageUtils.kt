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
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ImageUtils {

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
                inPreferredConfig = Bitmap.Config.RGB_565 // Low-memory footprint on budget hardware
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Read EXIF orientation safely
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
     * (0f..1f relative to source bitmap width and height) so the cropped output matches
     * 100% what the user selected inside the 16:9 frame.
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

            // Enforce exact 16:9 inside source bounds
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
            // Default centered 16:9 crop
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
     * Generates a borderless, full-width composite infographic image:
     * - Zero outer borders around the image
     * - Full-width 16:9 photo at the top (0..1080px) without any "HD" badge or border
     * - Monochrome text (#111111) across all labels, names, and prices for 100% legibility
     * - Exact price ordering per Request Type:
     *   * Type 1: Precio de Costo, Precio de Venta (%)
     *   * Types 2 & 3: Precio de Costo Anterior, Precio de Costo Nuevo, Precio de Venta Anterior (%), Precio de Venta Nuevo (%)
     *   * Types 4 & 5: Precio de Costo (único), Precio de Venta Anterior (%), Precio de Venta Nuevo (%)
     */
    fun generateInfographicBitmap(
        photo16by9: Bitmap?,
        request: PriceRequest
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

        // Full-bleed 16:9 photo dimensions (no outer border)
        val photoWidth = targetWidth
        val photoHeight = if (photo16by9 != null) (targetWidth * 9) / 16 else 0

        // Monochrome typography paints for maximum legibility
        val titlePaint = TextPaint().apply {
            color = monoTextColor
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val labelBoldPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bulletItemPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val secondaryInfoPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val priceBoldPaint = TextPaint().apply {
            color = monoTextColor
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Build exact list of product names / varieties as typed in the form
        val rawNames = mutableListOf<String>()
        if (request.descripcion.isNotBlank()) {
            rawNames.add(request.descripcion.trim())
        }
        if (!request.variedadesMismoCosto.isNullOrBlank()) {
            val extraVarieties = request.variedadesMismoCosto
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !rawNames.contains(it) }
            rawNames.addAll(extraVarieties)
        }

        // Pre-build StaticLayouts to calculate exact dynamic height so no text is ever truncated
        val titleLayout = buildStaticLayout(request.titulo, titlePaint, contentWidth)
        val nombreHeaderLayout = buildStaticLayout("Nombre Producto:", labelBoldPaint, contentWidth)

        val bulletLayouts = rawNames.map { itemText ->
            buildStaticLayout("· $itemText", bulletItemPaint, contentWidth)
        }

        val extraInfoLines = mutableListOf<StaticLayout>()
        val codeVal = request.codigoProducto?.takeIf { it.isNotBlank() } ?: request.codigo?.takeIf { it.isNotBlank() }
        if (codeVal != null) {
            extraInfoLines.add(buildStaticLayout("Código: $codeVal", secondaryInfoPaint, contentWidth))
        }
        if (!request.stockAnterior.isNullOrBlank() || !request.recargaStock.isNullOrBlank()) {
            val antStr = request.stockAnterior?.takeIf { it.isNotBlank() } ?: "---"
            val recStr = request.recargaStock?.takeIf { it.isNotBlank() } ?: "---"
            extraInfoLines.add(buildStaticLayout("Stock Anterior: $antStr   |   Recarga: $recStr", secondaryInfoPaint, contentWidth))
        }

        // Build Price lines in the exact order required by each Request Type (1 vs 2-3 vs 4-5)
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

        // Calculate total height dynamically without any outer border
        var totalHeight = photoHeight + 36
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
        totalHeight += 32 // Space before prices
        for (pLayout in priceLayouts) {
            totalHeight += pLayout.height + 14
        }
        totalHeight += 40 // Bottom padding

        // Use RGB_565 for 50% less RAM usage on low-end phones while keeping crisp text
        val composite = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(composite)

        // 1. Draw clean borderless background
        val bgPaint = Paint().apply {
            color = softBgColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), totalHeight.toFloat(), bgPaint)

        var currentY = 0f

        // 2. Draw full-width 16:9 Photo at the top with NO borders and NO HD badge
        if (photo16by9 != null) {
            val srcRect = Rect(0, 0, photo16by9.width, photo16by9.height)
            val dstRect = Rect(0, 0, photoWidth, photoHeight)
            val photoPaint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(photo16by9, srcRect, dstRect, photoPaint)
            currentY = photoHeight.toFloat()
        }

        currentY += 36f

        // 3. Draw Request Title in monochrome text
        drawStaticLayout(canvas, titleLayout, horizontalPad, currentY)
        currentY += titleLayout.height + 16f

        // 4. Draw "Nombre Producto:" Header
        drawStaticLayout(canvas, nombreHeaderLayout, horizontalPad, currentY)
        currentY += nombreHeaderLayout.height + 10f

        // 5. Draw Bulleted List ("· ") of Product Names / Varieties exactly as written in the form
        for (bLayout in bulletLayouts) {
            drawStaticLayout(canvas, bLayout, horizontalPad, currentY)
            currentY += bLayout.height + 8f
        }

        // 6. Draw Optional Code & Stock if present
        if (extraInfoLines.isNotEmpty()) {
            currentY += 18f
            for (infoLayout in extraInfoLines) {
                drawStaticLayout(canvas, infoLayout, horizontalPad, currentY)
                currentY += infoLayout.height + 8f
            }
        }

        // 7. Space before Prices
        currentY += 32f

        // 8. Draw Prices in exact order with monochrome bold text
        for (pLayout in priceLayouts) {
            drawStaticLayout(canvas, pLayout, horizontalPad, currentY)
            currentY += pLayout.height + 14f
        }

        return composite
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(100))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
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

            // Free native memory immediately on low-end devices
            bitmap.recycle()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Mandate Block 5: Sends ONLY the generated infographic image to WhatsApp
     * without any Intent.EXTRA_TEXT accompanying it.
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
            Intent.createChooser(intent, "Enviar ficha a WhatsApp")
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    /**
     * Share method for text catalog exports or single product sharing.
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
            Intent.createChooser(intent, "Compartir producto por WhatsApp")
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
