package com.lumina.manualcamera.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.manualcamera.data.model.LensType
import com.lumina.manualcamera.ui.theme.LuminaAmber
import com.lumina.manualcamera.ui.theme.LuminaDarkSurfaceVariant

@Composable
fun CameraControls(
    isCapturing: Boolean,
    lastPhotoUri: Uri?,
    currentLens: LensType,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSwitchLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Gallery Button
        GalleryButton(
            lastPhotoUri = lastPhotoUri,
            onClick = onGalleryClick
        )

        // 2. Main Shutter Button
        ShutterButton(
            isCapturing = isCapturing,
            onClick = onShutterClick
        )

        // 3. Switch Lens Button
        LensSwitchButton(
            currentLens = currentLens,
            onClick = onSwitchLens
        )
    }
}

@Composable
private fun ShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1.0f, label = "shutterScale")

    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isCapturing
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White)
        )

        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(68.dp),
                color = LuminaAmber,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun GalleryButton(
    lastPhotoUri: Uri?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LuminaDarkSurfaceVariant)
            .border(1.dp, Color.DarkGray, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = "Gallery",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun LensSwitchButton(
    currentLens: LensType,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LuminaDarkSurfaceVariant)
            .border(1.dp, Color.DarkGray, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentLens.displayName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
