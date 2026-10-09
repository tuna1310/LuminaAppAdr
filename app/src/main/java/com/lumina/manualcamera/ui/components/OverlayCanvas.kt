package com.lumina.manualcamera.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lumina.manualcamera.analyzer.FrameAnalysisResult
import com.lumina.manualcamera.data.model.GridType
import com.lumina.manualcamera.data.model.HistogramMode
import com.lumina.manualcamera.data.model.OverlayState

@Composable
fun OverlayCanvas(
    overlayState: OverlayState,
    analysisResult: FrameAnalysisResult?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Grid Lines
        if (overlayState.gridType != GridType.OFF) {
            GridLines(gridType = overlayState.gridType)
        }

        // 2. Focus Peaking & Zebra Bitmap Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            analysisResult?.peakingBitmap?.let { bmp ->
                drawImage(
                    image = bmp.asImageBitmap(),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    dstOffset = IntOffset.Zero
                )
            }
            analysisResult?.clippingBitmap?.let { bmp ->
                drawImage(
                    image = bmp.asImageBitmap(),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    dstOffset = IntOffset.Zero
                )
            }
        }

        // 3. Mini Real-time Histogram View (góc trên bên phải)
        if (overlayState.histogramMode != HistogramMode.OFF && analysisResult != null) {
            HistogramView(
                mode = overlayState.histogramMode,
                result = analysisResult,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun GridLines(gridType: GridType) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.35f)
        val strokeWidth = 1.dp.toPx()

        when (gridType) {
            GridType.RULE_OF_THIRDS -> {
                val colStep = size.width / 3f
                val rowStep = size.height / 3f

                drawLine(lineColor, Offset(colStep, 0f), Offset(colStep, size.height), strokeWidth)
                drawLine(lineColor, Offset(colStep * 2, 0f), Offset(colStep * 2, size.height), strokeWidth)

                drawLine(lineColor, Offset(0f, rowStep), Offset(size.width, rowStep), strokeWidth)
                drawLine(lineColor, Offset(0f, rowStep * 2), Offset(size.width, rowStep * 2), strokeWidth)
            }
            GridType.FOUR_BY_FOUR -> {
                val colStep = size.width / 4f
                val rowStep = size.height / 4f

                for (i in 1..3) {
                    drawLine(lineColor, Offset(colStep * i, 0f), Offset(colStep * i, size.height), strokeWidth)
                    drawLine(lineColor, Offset(0f, rowStep * i), Offset(size.width, rowStep * i), strokeWidth)
                }
            }
            GridType.GOLDEN_RATIO -> {
                val phi = 0.618f
                val x1 = size.width * (1f - phi)
                val x2 = size.width * phi
                val y1 = size.height * (1f - phi)
                val y2 = size.height * phi

                drawLine(lineColor, Offset(x1, 0f), Offset(x1, size.height), strokeWidth)
                drawLine(lineColor, Offset(x2, 0f), Offset(x2, size.height), strokeWidth)
                drawLine(lineColor, Offset(0f, y1), Offset(size.width, y1), strokeWidth)
                drawLine(lineColor, Offset(0f, y2), Offset(size.width, y2), strokeWidth)
            }
            GridType.OFF -> {}
        }
    }
}

@Composable
private fun HistogramView(
    mode: HistogramMode,
    result: FrameAnalysisResult,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(120.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bins = when (mode) {
                HistogramMode.LUMA -> result.lumaHistogram
                HistogramMode.RGB -> result.lumaHistogram
                HistogramMode.OFF -> return@Canvas
            }

            val maxVal = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
            val path = Path()
            val stepX = size.width / 256f

            path.moveTo(0f, size.height)
            for (i in 0 until 256) {
                val x = i * stepX
                val normalizedY = size.height - (bins[i].toFloat() / maxVal * size.height)
                path.lineTo(x, normalizedY)
            }
            path.lineTo(size.width, size.height)
            path.close()

            val fillColor = if (mode == HistogramMode.LUMA) {
                Color.White.copy(alpha = 0.7f)
            } else {
                Color(0xFFFFB300).copy(alpha = 0.7f)
            }

            drawPath(path, fillColor)
            drawPath(path, Color.White, style = Stroke(width = 1.dp.toPx()))
        }
    }
}
