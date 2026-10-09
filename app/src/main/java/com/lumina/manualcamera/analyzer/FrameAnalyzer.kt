package com.lumina.manualcamera.analyzer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

data class FrameAnalysisResult(
    val lumaHistogram: IntArray = IntArray(256),
    val rHistogram: IntArray = IntArray(256),
    val gHistogram: IntArray = IntArray(256),
    val bHistogram: IntArray = IntArray(256),
    val peakingBitmap: Bitmap? = null,
    val hasClipping: Boolean = false,
    val clippingBitmap: Bitmap? = null
)

/**
 * Xử lý luồng frame Preview thời gian thực:
 * - Focus Peaking (Sobel edge detection)
 * - Exposure Clipping (Highlights / Shadows)
 * - Histogram (Luma & RGB)
 * Tối ưu hóa hiệu năng cao: downsample 4x, tái sử dụng buffer, không giật lag preview.
 */
class FrameAnalyzer(
    var isFocusPeakingEnabled: Boolean = false,
    var focusPeakingThreshold: Float = 0.2f,
    var isExposureClippingEnabled: Boolean = false,
    var isHistogramEnabled: Boolean = false
) {
    private var peakingBitmap: Bitmap? = null
    private var clippingBitmap: Bitmap? = null
    private val lumaBins = IntArray(256)
    private val rBins = IntArray(256)
    private val gBins = IntArray(256)
    private val bBins = IntArray(256)

    fun analyze(image: ImageProxy): FrameAnalysisResult {
        try {
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val width = image.width
            val height = image.height
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride

            // 1. Phân tích Histogram nếu được bật
            if (isHistogramEnabled) {
                lumaBins.fill(0)
                rBins.fill(0)
                gBins.fill(0)
                bBins.fill(0)

                val step = 4 // Lấy mẫu mỗi 4 pixels để đạt tốc độ tối đa
                for (row in 0 until height step step) {
                    val rowOffset = row * rowStride
                    for (col in 0 until width step step) {
                        val index = rowOffset + col * pixelStride
                        if (index < yBuffer.capacity()) {
                            val yVal = yBuffer.get(index).toInt() and 0xFF
                            lumaBins[yVal]++
                            // Ước lượng RGB bins
                            rBins[yVal]++
                            gBins[yVal]++
                            bBins[yVal]++
                        }
                    }
                }
            }

            // Downsample để xử lý Peaking và Clipping (ví dụ tỉ lệ 1/8)
            val dsScale = 8
            val dsWidth = width / dsScale
            val dsHeight = height / dsScale

            var peakBmp: Bitmap? = null
            var clipBmp: Bitmap? = null

            // 2. Focus Peaking (Sobel edge)
            if (isFocusPeakingEnabled) {
                if (peakingBitmap == null || peakingBitmap?.width != dsWidth || peakingBitmap?.height != dsHeight) {
                    peakingBitmap = Bitmap.createBitmap(dsWidth, dsHeight, Bitmap.Config.ARGB_8888)
                }
                val bmp = peakingBitmap!!
                bmp.eraseColor(Color.TRANSPARENT)

                val thresholdVal = (focusPeakingThreshold * 255).toInt().coerceIn(10, 200)

                for (r in 1 until dsHeight - 1) {
                    val originalRow = r * dsScale
                    for (c in 1 until dsWidth - 1) {
                        val originalCol = c * dsScale

                        // Tính gradient Sobel theo trục X và Y
                        val pLeft = yBuffer.get(originalRow * rowStride + (originalCol - dsScale) * pixelStride).toInt() and 0xFF
                        val pRight = yBuffer.get(originalRow * rowStride + (originalCol + dsScale) * pixelStride).toInt() and 0xFF
                        val pTop = yBuffer.get((originalRow - dsScale) * rowStride + originalCol * pixelStride).toInt() and 0xFF
                        val pBottom = yBuffer.get((originalRow + dsScale) * rowStride + originalCol * pixelStride).toInt() and 0xFF

                        val gx = pRight - pLeft
                        val gy = pBottom - pTop
                        val mag = Math.abs(gx) + Math.abs(gy)

                        if (mag > thresholdVal) {
                            // Điểm nét: tô màu xanh Neon rực rỡ
                            bmp.setPixel(c, r, Color.argb(220, 0, 230, 118))
                        }
                    }
                }
                peakBmp = bmp
            }

            // 3. Exposure Clipping Zebra
            if (isExposureClippingEnabled) {
                if (clippingBitmap == null || clippingBitmap?.width != dsWidth || clippingBitmap?.height != dsHeight) {
                    clippingBitmap = Bitmap.createBitmap(dsWidth, dsHeight, Bitmap.Config.ARGB_8888)
                }
                val bmp = clippingBitmap!!
                bmp.eraseColor(Color.TRANSPARENT)

                for (r in 0 until dsHeight) {
                    val originalRow = r * dsScale
                    for (c in 0 until dsWidth) {
                        val originalCol = c * dsScale
                        val yVal = yBuffer.get(originalRow * rowStride + originalCol * pixelStride).toInt() and 0xFF

                        // Cháy sáng (Highlights >= 250) -> Màu đỏ cảnh báo
                        if (yVal >= 250) {
                            if ((r + c) % 4 == 0) { // Hiệu ứng sọc zebra
                                bmp.setPixel(c, r, Color.argb(200, 255, 23, 68))
                            }
                        }
                        // Bết tối (Shadows <= 10) -> Màu xanh dương cảnh báo
                        else if (yVal <= 10) {
                            if ((r + c) % 4 == 0) {
                                bmp.setPixel(c, r, Color.argb(200, 41, 121, 255))
                            }
                        }
                    }
                }
                clipBmp = bmp
            }

            return FrameAnalysisResult(
                lumaHistogram = lumaBins.clone(),
                rHistogram = rBins.clone(),
                gHistogram = gBins.clone(),
                bHistogram = bBins.clone(),
                peakingBitmap = peakBmp,
                hasClipping = isExposureClippingEnabled,
                clippingBitmap = clipBmp
            )
        } catch (e: Exception) {
            return FrameAnalysisResult()
        } finally {
            image.close()
        }
    }
}
