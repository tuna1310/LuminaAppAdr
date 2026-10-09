package com.lumina.manualcamera.data.model

import android.hardware.camera2.params.RggbChannelVector
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Định dạng lưu trữ ảnh
 */
enum class CaptureFormat(val extension: String, val displayName: String) {
    JPEG("jpg", "JPEG"),
    HEIC("heic", "HEIC"),
    RAW_DNG("dng", "RAW (DNG)")
}

/**
 * Mức độ xử lý khử nhiễu và làm nét
 */
enum class PostProcessingQuality(val displayName: String) {
    OFF("Off"),
    LOW("Low"),
    STANDARD("Standard")
}

/**
 * Không gian màu
 */
enum class ColorSpaceOption(val displayName: String) {
    SRGB("sRGB"),
    DISPLAY_P3("Display P3")
}

/**
 * Loại lưới ngắm (Grid)
 */
enum class GridType(val displayName: String) {
    OFF("Off"),
    RULE_OF_THIRDS("3x3"),
    FOUR_BY_FOUR("4x4"),
    GOLDEN_RATIO("Golden")
}

/**
 * Chế độ Histogram
 */
enum class HistogramMode(val displayName: String) {
    OFF("Off"),
    LUMA("Luma"),
    RGB("RGB")
}

/**
 * Chế độ chụp nâng cao
 */
enum class ShootingMode(val displayName: String) {
    PHOTO("Photo"),
    BRACKETING("Bracketing"),
    ENHANCED_RAW("Enhanced RAW"),
    SMART_SELFIE("Smart Selfie")
}

/**
 * Thông số tốc độ màn trập (Shutter Speed)
 */
data class ShutterSpeed(
    val exposureTimeNs: Long,
    val isAuto: Boolean = true
) {
    /**
     * Định dạng hiển thị Shutter Speed dạng phân số hoặc giây (1/50, 1/125, 0.5s, 2s...)
     */
    fun displayString(): String {
        if (isAuto) return "AUTO"
        if (exposureTimeNs <= 0L) return "1/60s"
        val seconds = exposureTimeNs / 1_000_000_000.0
        return formatSeconds(seconds)
    }

    companion object {
        fun formatSeconds(seconds: Double): String {
            return if (seconds >= 1.0) {
                if (seconds == seconds.toLong().toDouble()) {
                    "${seconds.toLong()}s"
                } else {
                    String.format("%.1fs", seconds)
                }
            } else {
                val fraction = (1.0 / seconds).roundToInt()
                "1/${fraction}s"
            }
        }

        // Các mốc tốc độ chuẩn DSLR (tính theo nano giây)
        val STANDARD_STEPS_NS = listOf(
            125_000L,       // 1/8000
            250_000L,       // 1/4000
            500_000L,       // 1/2000
            1_000_000L,     // 1/1000
            2_000_000L,     // 1/500
            4_000_000L,     // 1/250
            8_000_000L,     // 1/125
            16_666_667L,    // 1/60
            33_333_333L,    // 1/30
            66_666_667L,    // 1/15
            125_000_000L,   // 1/8
            250_000_000L,   // 1/4
            500_000_000L,   // 1/2
            1_000_000_000L, // 1s
            2_000_000_000L  // 2s
        )
    }
}

/**
 * Thông số ISO
 */
data class IsoParameter(
    val value: Int = 100,
    val isAuto: Boolean = true
) {
    fun displayString(): String = if (isAuto) "AUTO" else "ISO $value"
}

/**
 * Thông số lấy nét (Focus Distance)
 * 0.0f = Vô cực (Infinity)
 * minFocusDistance = Cận cảnh (Macro)
 */
data class FocusParameter(
    val diopters: Float = 0.0f,
    val isAuto: Boolean = true
) {
    fun displayString(): String {
        return if (isAuto) "AF" else String.format("%.2f m⁻¹", diopters)
    }
}

/**
 * Giá trị gains thuần của 3 kênh màu RGB
 */
data class RggbGains(val red: Float, val green: Float, val blue: Float)

/**
 * Thông số Cân bằng trắng (White Balance) tính theo Kelvin
 */
data class WhiteBalanceParameter(
    val kelvin: Int = 5000,
    val isAuto: Boolean = true
) {
    fun displayString(): String = if (isAuto) "AWB" else "${kelvin}K"

    /**
     * Quy đổi nhiệt độ màu Kelvin (2000K - 10000K) sang RGB Gains thuần
     */
    fun toRggbGains(): RggbGains {
        val temp = kelvin / 100.0
        val red: Double
        val green: Double
        val blue: Double

        // Ước lượng RGB từ Kelvin theo thuật toán Planckian Locus (Tanner Helland)
        if (temp <= 66.0) {
            red = 255.0
            green = (99.4708025861 * ln(temp) - 161.1195681661).coerceIn(0.0, 255.0)
            blue = if (temp <= 19.0) 0.0 else (138.5177312231 * ln(temp - 10.0) - 305.0447927307).coerceIn(0.0, 255.0)
        } else {
            red = (329.698727446 * (temp - 60.0).pow(-0.1332047592)).coerceIn(0.0, 255.0)
            green = (288.1221695283 * (temp - 60.0).pow(-0.0755148492)).coerceIn(0.0, 255.0)
            blue = 255.0
        }

        // Chuẩn hóa sang gains
        val gRef = if (green > 0) green else 1.0
        val rGain = ((red / gRef) * 1.8).toFloat().coerceIn(0.8f, 3.5f)
        val bGain = ((blue / gRef) * 1.8).toFloat().coerceIn(0.8f, 3.5f)
        return RggbGains(rGain, 1.0f, bGain)
    }

    fun toRggbVector(): RggbChannelVector {
        val gains = toRggbGains()
        return RggbChannelVector(gains.red, 1.0f, 1.0f, gains.blue)
    }
}

/**
 * Thông số Bù trừ sáng (Exposure Bias EV)
 */
data class ExposureBiasParameter(
    val evIndex: Int = 0,
    val stepEv: Float = 0.33333334f
) {
    val evValue: Float get() = evIndex * stepEv

    fun displayString(): String {
        val rounded = (evValue * 10).roundToInt() / 10.0
        return when {
            rounded > 0 -> "+%.1f EV".format(rounded)
            rounded < 0 -> "%.1f EV".format(rounded)
            else -> "0.0 EV"
        }
    }
}

/**
 * Preset cân bằng trắng
 */
enum class WbPreset(val displayName: String, val kelvin: Int) {
    AUTO("Auto", 5000),
    DAYLIGHT("Daylight", 5500),
    CLOUDY("Cloudy", 6500),
    TUNGSTEN("Tungsten", 3200),
    FLUORESCENT("Fluorescent", 4000),
    SHADE("Shade", 7500)
}

/**
 * Overlay cấu hình
 */
data class OverlayState(
    val gridType: GridType = GridType.OFF,
    val histogramMode: HistogramMode = HistogramMode.OFF,
    val focusPeakingEnabled: Boolean = false,
    val focusPeakingThreshold: Float = 0.15f,
    val exposureClippingEnabled: Boolean = false
)

/**
 * Thông tin khả năng phần cứng của Camera
 */
data class CameraCapabilities(
    val supportsManualSensor: Boolean = false,
    val supportsRaw: Boolean = false,
    val minExposureNs: Long = 100_000L,
    val maxExposureNs: Long = 1_000_000_000L,
    val minIso: Int = 100,
    val maxIso: Int = 3200,
    val minFocusDistance: Float = 10.0f, // 10 diopters = 10cm macro
    val minEv: Int = -6,
    val maxEv: Int = 6,
    val evStep: Float = 1f / 3f,
    val availableLenses: List<LensType> = listOf(LensType.WIDE),
    val maxPhotoResolution: Pair<Int, Int> = Pair(4000, 3000)
)

enum class LensType(val displayName: String, val zoomRatio: Float) {
    ULTRA_WIDE("0.5x", 0.5f),
    WIDE("1x", 1.0f),
    TELE("3x", 3.0f),
    FRONT("Front", 1.0f)
}
