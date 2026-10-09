package com.lumina.manualcamera.advanced

import com.lumina.manualcamera.data.model.ExposureBiasParameter
import com.lumina.manualcamera.data.model.IsoParameter
import com.lumina.manualcamera.data.model.ShutterSpeed

object BracketingManager {

    data class BracketShot(
        val index: Int,
        val evOffset: Int,
        val shutterSpeedNs: Long,
        val iso: Int,
        val label: String
    )

    /**
     * Tạo 3 shot chụp Bracketing: -2 EV, 0 EV, +2 EV với ISO cố định, thay đổi Shutter
     */
    fun create3ShotBracket(
        baseShutterNs: Long,
        baseIso: Int,
        minShutterNs: Long = 100_000L,
        maxShutterNs: Long = 1_000_000_000L
    ): List<BracketShot> {
        // -2 EV tương ứng 1/4 thời gian phơi sáng
        val underExposureNs = (baseShutterNs / 4L).coerceIn(minShutterNs, maxShutterNs)
        // 0 EV
        val normalExposureNs = baseShutterNs.coerceIn(minShutterNs, maxShutterNs)
        // +2 EV tương ứng x4 thời gian phơi sáng
        val overExposureNs = (baseShutterNs * 4L).coerceIn(minShutterNs, maxShutterNs)

        return listOf(
            BracketShot(1, -2, underExposureNs, baseIso, "EV -2"),
            BracketShot(2, 0, normalExposureNs, baseIso, "EV 0"),
            BracketShot(3, 2, overExposureNs, baseIso, "EV +2")
        )
    }
}
