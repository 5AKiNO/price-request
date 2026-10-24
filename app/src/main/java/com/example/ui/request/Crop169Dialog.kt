package com.example.ui.request

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ClaudiaBluePrimary
import com.example.ui.theme.getContrastingTextColor
import com.example.util.ImageUtils
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive 16:9 crop dialog that displays the complete photo fitted on screen
 * and lets the user move and resize a 16:9 crop frame directly over the full image.
 * The exact area inside the 16:9 rectangle is cropped without any offset errors.
 */
@Composable
fun Crop169Dialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    val imageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }
    val srcW = sourceBitmap.width.toFloat().coerceAtLeast(1f)
    val srcH = sourceBitmap.height.toFloat().coerceAtLeast(1f)

    // Maximum possible 16:9 rectangle inside the source bitmap (in source pixel units)
    val maxCropWInSrc: Float
    val maxCropHInSrc: Float
    if (srcW * 9f > srcH * 16f) {
        maxCropHInSrc = srcH
        maxCropWInSrc = (srcH * 16f) / 9f
    } else {
        maxCropWInSrc = srcW
        maxCropHInSrc = (srcW * 9f) / 16f
    }

    // boxScale: fraction of the maximum 16:9 box size (0.30f .. 1.0f)
    var boxScale by remember(sourceBitmap) { mutableFloatStateOf(0.85f) }
    // Normalized center of the 16:9 crop box on the source bitmap (0f..1f)
    var centerXNorm by remember(sourceBitmap) { mutableFloatStateOf(0.5f) }
    var centerYNorm by remember(sourceBitmap) { mutableFloatStateOf(0.5f) }

    // Keep track of the rendered image size on screen to convert drag gestures to normalized coordinates
    var renderedImgW by remember { mutableFloatStateOf(1f) }
    var renderedImgH by remember { mutableFloatStateOf(1f) }

    fun clampCenter(scaleVal: Float, cx: Float, cy: Float): Pair<Float, Float> {
        val halfWNorm = ((maxCropWInSrc * scaleVal) / (2f * srcW)).coerceIn(0f, 0.5f)
        val halfHNorm = ((maxCropHInSrc * scaleVal) / (2f * srcH)).coerceIn(0f, 0.5f)
        val clampedX = cx.coerceIn(halfWNorm, 1f - halfWNorm)
        val clampedY = cy.coerceIn(halfHNorm, 1f - halfHNorm)
        return clampedX to clampedY
    }

    val dialogBg = Color.Black
    val dialogTextColor = getContrastingTextColor(dialogBg)
    val panelBg = Color(0xFF1E2024)
    val panelTextColor = getContrastingTextColor(panelBg)
    val confirmBtnBg = ClaudiaBluePrimary
    val confirmBtnText = getContrastingTextColor(confirmBtnBg)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = dialogBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recortar Imagen en 16:9",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = dialogTextColor
                        )
                        Text(
                            text = "Mueve el recuadro 16:9 sobre la foto completa para elegir tu recorte",
                            style = MaterialTheme.typography.bodySmall,
                            color = dialogTextColor
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = dialogTextColor)
                    }
                }

                // Interactive Full Photo + Movable 16:9 Frame Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sourceBitmap) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = if (zoom != 1f) {
                                        (boxScale * zoom).coerceIn(0.30f, 1.0f)
                                    } else {
                                        boxScale
                                    }
                                    val dxNorm = pan.x / renderedImgW.coerceAtLeast(1f)
                                    val dyNorm = pan.y / renderedImgH.coerceAtLeast(1f)
                                    val (clampedX, clampedY) = clampCenter(
                                        newScale,
                                        centerXNorm + dxNorm,
                                        centerYNorm + dyNorm
                                    )
                                    boxScale = newScale
                                    centerXNorm = clampedX
                                    centerYNorm = clampedY
                                }
                            }
                    ) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // Fit full image inside canvas preserving aspect ratio
                        val fitScale = min(canvasW / srcW, canvasH / srcH)
                        val imgW = srcW * fitScale
                        val imgH = srcH * fitScale
                        val imgLeft = (canvasW - imgW) / 2f
                        val imgTop = (canvasH - imgH) / 2f

                        renderedImgW = imgW
                        renderedImgH = imgH

                        // 1. Draw the complete source photo
                        drawImage(
                            image = imageBitmap,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(sourceBitmap.width, sourceBitmap.height),
                            dstOffset = IntOffset(imgLeft.roundToInt(), imgTop.roundToInt()),
                            dstSize = IntSize(imgW.roundToInt(), imgH.roundToInt())
                        )

                        // 2. Compute the 16:9 crop box in screen coordinates
                        val (validCx, validCy) = clampCenter(boxScale, centerXNorm, centerYNorm)
                        val cropWNorm = (maxCropWInSrc * boxScale) / srcW
                        val cropHNorm = (maxCropHInSrc * boxScale) / srcH

                        val boxLeft = imgLeft + (validCx - cropWNorm / 2f) * imgW
                        val boxTop = imgTop + (validCy - cropHNorm / 2f) * imgH
                        val boxW = cropWNorm * imgW
                        val boxH = cropHNorm * imgH

                        // 3. Dim the area outside the 16:9 crop box
                        val scrimPath = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(
                                androidx.compose.ui.geometry.Rect(
                                    offset = Offset(imgLeft, imgTop),
                                    size = Size(imgW, imgH)
                                )
                            )
                            addRect(
                                androidx.compose.ui.geometry.Rect(
                                    offset = Offset(boxLeft, boxTop),
                                    size = Size(boxW, boxH)
                                )
                            )
                        }
                        drawPath(
                            path = scrimPath,
                            color = Color.Black.copy(alpha = 0.62f)
                        )

                        // 4. Draw crisp 16:9 frame border
                        drawRect(
                            color = Color(0xFF00E676),
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxW, boxH),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // 5. Rule-of-thirds grid lines inside the 16:9 crop box
                        val thirdW = boxW / 3f
                        val thirdH = boxH / 3f
                        val gridColor = Color.White.copy(alpha = 0.45f)
                        val gridStroke = 1.dp.toPx()

                        for (i in 1..2) {
                            // Vertical line
                            drawLine(
                                color = gridColor,
                                start = Offset(boxLeft + thirdW * i, boxTop),
                                end = Offset(boxLeft + thirdW * i, boxTop + boxH),
                                strokeWidth = gridStroke
                            )
                            // Horizontal line
                            drawLine(
                                color = gridColor,
                                start = Offset(boxLeft, boxTop + thirdH * i),
                                end = Offset(boxLeft + boxW, boxTop + thirdH * i),
                                strokeWidth = gridStroke
                            )
                        }

                        // 6. Corner handles for visual clarity
                        val handleLen = min(boxW, boxH) * 0.14f
                        val handleStroke = 5.dp.toPx()
                        val handleColor = Color.White

                        // Top-left
                        drawLine(handleColor, Offset(boxLeft, boxTop), Offset(boxLeft + handleLen, boxTop), handleStroke)
                        drawLine(handleColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + handleLen), handleStroke)
                        // Top-right
                        drawLine(handleColor, Offset(boxLeft + boxW, boxTop), Offset(boxLeft + boxW - handleLen, boxTop), handleStroke)
                        drawLine(handleColor, Offset(boxLeft + boxW, boxTop), Offset(boxLeft + boxW, boxTop + handleLen), handleStroke)
                        // Bottom-left
                        drawLine(handleColor, Offset(boxLeft, boxTop + boxH), Offset(boxLeft + handleLen, boxTop + boxH), handleStroke)
                        drawLine(handleColor, Offset(boxLeft, boxTop + boxH), Offset(boxLeft, boxTop + boxH - handleLen), handleStroke)
                        // Bottom-right
                        drawLine(handleColor, Offset(boxLeft + boxW, boxTop + boxH), Offset(boxLeft + boxW - handleLen, boxTop + boxH), handleStroke)
                        drawLine(handleColor, Offset(boxLeft + boxW, boxTop + boxH), Offset(boxLeft + boxW, boxTop + boxH - handleLen), handleStroke)
                    }
                }

                // Bottom Control Panel (dark background -> white text)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = panelBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tamaño 16:9:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = panelTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = boxScale,
                                onValueChange = { newScale ->
                                    val (cx, cy) = clampCenter(newScale, centerXNorm, centerYNorm)
                                    boxScale = newScale
                                    centerXNorm = cx
                                    centerYNorm = cy
                                },
                                valueRange = 0.30f..1.0f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )
                            Text(
                                text = "${(boxScale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = panelTextColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                border = BorderStroke(1.5.dp, panelTextColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar", color = panelTextColor, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val (validCx, validCy) = clampCenter(boxScale, centerXNorm, centerYNorm)
                                    val halfWNorm = (maxCropWInSrc * boxScale) / (2f * srcW)
                                    val halfHNorm = (maxCropHInSrc * boxScale) / (2f * srcH)

                                    val cropRectNorm = RectF(
                                        (validCx - halfWNorm).coerceIn(0f, 1f),
                                        (validCy - halfHNorm).coerceIn(0f, 1f),
                                        (validCx + halfWNorm).coerceIn(0f, 1f),
                                        (validCy + halfHNorm).coerceIn(0f, 1f)
                                    )
                                    val cropped = ImageUtils.cropTo16By9(sourceBitmap, cropRectNorm)
                                    onCropped(cropped)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = confirmBtnBg,
                                    contentColor = confirmBtnText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("crop_button_confirm")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = confirmBtnText)
                                Spacer(modifier = Modifier.padding(start = 4.dp))
                                Text("Confirmar 16:9", color = confirmBtnText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
