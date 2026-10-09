package com.lumina.manualcamera.data.model

import android.net.Uri

data class ExifInfo(
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val whiteBalance: String? = null,
    val dateTaken: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val cameraModel: String? = null,
    val flashUsed: Boolean = false,
    val format: String = "JPEG",
    val fileSizeBytes: Long = 0L
) {
    val resolutionMegaPixels: String
        get() {
            if (width <= 0 || height <= 0) return "N/A"
            val mp = (width.toLong() * height.toLong()) / 1_000_000.0
            return String.format("%.1f MP", mp)
        }

    val fileSizeFormatted: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format("%.2f MB", mb)
            } else {
                val kb = fileSizeBytes / 1024.0
                String.format("%.0f KB", kb)
            }
        }
}

data class CapturedPhoto(
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val size: Long,
    val exifInfo: ExifInfo
)
