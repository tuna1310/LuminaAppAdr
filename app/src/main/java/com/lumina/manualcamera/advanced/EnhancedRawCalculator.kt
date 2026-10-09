package com.lumina.manualcamera.advanced

import kotlin.math.roundToInt

/**
 * Thuật toán tính toán tốc độ màn trập tối ưu cho Enhanced RAW dựa trên độ rung của con quay hồi chuyển (Gyroscope).
 * Mục tiêu: hạ ISO xuống thấp nhất có thể mà ảnh vẫn sắc nét, không bị nhòe mờ do rung tay.
 */
object EnhancedRawCalculator {

    data class ExposureSetting(
        val shutterSpeedNs: Long,
        val iso: Int
    )

    /**
     * @param gyroVarianceRadPerSec Độ lệch chuẩn vận tốc góc đo từ Gyroscope (rad/s)
     * @param focalLengthMm Tiêu cự ống kính (thường 24-28mm quy đổi 35mm)
     * @param targetExposureVal Giá trị phơi sáng mục tiêu (EV hoặc Lux quy đổi)
     * @param minIso ISO thấp nhất của cảm biến
     * @param maxIso ISO cao nhất cho phép
     */
    fun calculateOptimalExposure(
        gyroVarianceRadPerSec: Float,
        focalLengthMm: Float = 26f,
        minIso: Int = 100,
        maxIso: Int = 3200
    ): ExposureSetting {
        // Nguyên tắc cơ bản: 1 / (focalLength * hệ số rung)
        val safeShutterSec: Double = when {
            // Rất tĩnh (Tripod hoặc kê chắc tay): Cho phép phơi sáng lâu tới 1/8s
            gyroVarianceRadPerSec < 0.04f -> 1.0 / 8.0

            // Ổn định tốt (Cầm chắc tay): 1/30s
            gyroVarianceRadPerSec < 0.10f -> 1.0 / 30.0

            // Cầm tay bình thường: 1/60s
            gyroVarianceRadPerSec < 0.25f -> 1.0 / 60.0

            // Rung lắc vừa: 1/125s
            gyroVarianceRadPerSec < 0.50f -> 1.0 / 125.0

            // Rung lắc mạnh: 1/250s - 1/500s
            else -> 1.0 / 500.0
        }

        val shutterNs = (safeShutterSec * 1_000_000_000.0).toLong()

        // Tính ISO tương ứng: shutter càng lâu thì ISO càng thấp được
        // Giả sử mức phơi sáng cơ sở chuẩn ở 1/60s là ISO 400
        val baseSec = 1.0 / 60.0
        val baseIso = 400.0
        val targetIso = (baseIso * (baseSec / safeShutterSec)).roundToInt().coerceIn(minIso, maxIso)

        return ExposureSetting(
            shutterSpeedNs = shutterNs,
            iso = targetIso
        )
    }
}
