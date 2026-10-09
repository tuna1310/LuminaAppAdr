package com.lumina.manualcamera.ui.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.lumina.manualcamera.data.model.CapturedPhoto
import com.lumina.manualcamera.data.model.ExifInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository(private val context: Context) {

    suspend fun loadPhotos(): List<CapturedPhoto> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<CapturedPhoto>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "IMG_$id.jpg"
                    val dateAdded = c.getLong(dateCol)
                    val size = c.getLong(sizeCol)
                    val width = c.getInt(wCol)
                    val height = c.getInt(hCol)

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    // Đọc EXIF
                    val exifInfo = readExif(uri, name, size, width, height)

                    photos.add(
                        CapturedPhoto(
                            uri = uri,
                            displayName = name,
                            dateAdded = dateAdded,
                            size = size,
                            exifInfo = exifInfo
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        photos
    }

    private fun readExif(uri: Uri, name: String, size: Long, defaultW: Int, defaultH: Int): ExifInfo {
        var iso: String? = null
        var shutter: String? = null
        var aperture: String? = null
        var focalLength: String? = null
        var wb: String? = null
        var date: String? = null
        var model: String? = null
        var w = defaultW
        var h = defaultH

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                wb = if (exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE) == ExifInterface.WHITE_BALANCE_MANUAL.toString()) "Manual" else "Auto"
                date = exif.getAttribute(ExifInterface.TAG_DATETIME)
                model = exif.getAttribute(ExifInterface.TAG_MODEL)

                val ew = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val eh = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                if (ew > 0) w = ew
                if (eh > 0) h = eh
            }
        } catch (e: Exception) {
            // Không đọc được exif thì dùng giá trị mặc định
        }

        val format = when {
            name.endsWith(".dng", ignoreCase = true) -> "RAW (DNG)"
            name.endsWith(".heic", ignoreCase = true) -> "HEIC"
            else -> "JPEG"
        }

        return ExifInfo(
            iso = iso?.let { "ISO $it" } ?: "ISO Auto",
            shutterSpeed = shutter?.let { "$it s" } ?: "N/A",
            aperture = aperture?.let { "f/$it" } ?: "f/1.8",
            focalLength = focalLength?.let { "${it}mm" } ?: "26mm",
            whiteBalance = wb,
            dateTaken = date,
            width = w,
            height = h,
            cameraModel = model,
            format = format,
            fileSizeBytes = size
        )
    }

    suspend fun deletePhoto(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            false
        }
    }
}
