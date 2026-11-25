package com.lumina.manualcamera.ui.gallery

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.manualcamera.R
import com.lumina.manualcamera.data.model.CapturedPhoto
import com.lumina.manualcamera.ui.theme.LuminaAmber
import com.lumina.manualcamera.ui.theme.LuminaDarkSurface
import com.lumina.manualcamera.ui.theme.LuminaDarkSurfaceVariant
import com.lumina.manualcamera.ui.theme.LuminaTextPrimary
import com.lumina.manualcamera.ui.theme.LuminaTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GalleryScreen(
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { GalleryRepository(context) }

    var photos by remember { mutableStateOf<List<CapturedPhoto>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var showExifDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        photos = repository.loadPhotos()
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (selectedPhoto == null) {
            // Chế độ Lưới (Grid)
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToCamera) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = stringResource(R.string.gallery),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LuminaAmber)
                    }
                } else if (photos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_photos),
                            color = LuminaTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(photos) { photo ->
                            ThumbnailItem(
                                photo = photo,
                                onClick = { selectedPhoto = photo }
                            )
                        }
                    }
                }
            }
        } else {
            // Chế độ Xem Full Ảnh
            val photo = selectedPhoto!!

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Full Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedPhoto = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Row {
                        // Nút Info EXIF
                        IconButton(onClick = { showExifDialog = !showExifDialog }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "EXIF",
                                tint = if (showExifDialog) LuminaAmber else Color.White
                            )
                        }

                        // Nút Share
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, photo.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Photo"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }

                        // Nút Delete
                        IconButton(onClick = {
                            scope.launch {
                                val success = repository.deletePhoto(photo.uri)
                                if (success) {
                                    photos = photos.filter { it.uri != photo.uri }
                                    selectedPhoto = null
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }

                // Full Image View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FullImagePreview(photo = photo)

                    // Card Thông số EXIF chuyên nghiệp
                    if (showExifDialog) {
                        ExifCard(
                            photo = photo,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    photo: CapturedPhoto,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(photo.uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(photo.uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 8 // Downsample nhanh cho thumbnail
                    }
                    bitmap = BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (e: Exception) {
                // Lỗi load ảnh
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(LuminaDarkSurfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Nhãn định dạng nếu là RAW
        if (photo.exifInfo.format.contains("RAW")) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LuminaAmber)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("RAW", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FullImagePreview(photo: CapturedPhoto) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(photo.uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(photo.uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Load full chất lượng cao
                    }
                    bitmap = BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        CircularProgressIndicator(color = LuminaAmber)
    }
}

@Composable
private fun ExifCard(
    photo: CapturedPhoto,
    modifier: Modifier = Modifier
) {
    val exif = photo.exifInfo
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LuminaDarkSurface.copy(alpha = 0.92f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = photo.displayName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ExifItem(label = "ISO", value = exif.iso ?: "Auto")
                ExifItem(label = "SHUTTER", value = exif.shutterSpeed ?: "N/A")
                ExifItem(label = "APERTURE", value = exif.aperture ?: "N/A")
                ExifItem(label = "FOCAL", value = exif.focalLength ?: "N/A")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ExifItem(label = "FORMAT", value = exif.format)
                ExifItem(label = "RESOLUTION", value = exif.resolutionMegaPixels)
                ExifItem(label = "SIZE", value = exif.fileSizeFormatted)
                ExifItem(label = "WB", value = exif.whiteBalance ?: "Auto")
            }
        }
    }
}

@Composable
private fun ExifItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = LuminaTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = LuminaAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}
