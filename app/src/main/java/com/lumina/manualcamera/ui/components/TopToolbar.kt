package com.lumina.manualcamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.manualcamera.data.model.CaptureFormat
import com.lumina.manualcamera.data.model.GridType
import com.lumina.manualcamera.data.model.HistogramMode
import com.lumina.manualcamera.data.model.ShootingMode
import com.lumina.manualcamera.ui.CameraUiState
import com.lumina.manualcamera.ui.theme.LuminaAmber
import com.lumina.manualcamera.ui.theme.LuminaDarkSurface

@Composable
fun TopToolbar(
    uiState: CameraUiState,
    onToggleFlash: () -> Unit,
    onSelectFormat: (CaptureFormat) -> Unit,
    onSelectShootingMode: (ShootingMode) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleHistogram: () -> Unit,
    onTogglePeaking: () -> Unit,
    onToggleClipping: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOverlayMenu by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Flash Toggle
        IconButton(onClick = onToggleFlash) {
            Icon(
                imageVector = if (uiState.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flash",
                tint = if (uiState.isFlashOn) LuminaAmber else Color.White
            )
        }

        // 2. Format Quick Switch (JPEG <-> RAW)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (uiState.captureFormat == CaptureFormat.RAW_DNG) LuminaAmber else Color.DarkGray)
                .clickable {
                    val nextFormat = if (uiState.captureFormat == CaptureFormat.RAW_DNG) CaptureFormat.JPEG else CaptureFormat.RAW_DNG
                    onSelectFormat(nextFormat)
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (uiState.captureFormat == CaptureFormat.RAW_DNG) "RAW" else "JPG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (uiState.captureFormat == CaptureFormat.RAW_DNG) Color.Black else Color.White,
                fontFamily = FontFamily.Monospace
            )
        }

        // 3. Shooting Mode Selector (Photo / Bracketing / Enhanced RAW / Smart Selfie)
        Box {
            Text(
                text = uiState.shootingMode.displayName.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LuminaAmber,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { showModeMenu = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            DropdownMenu(
                expanded = showModeMenu,
                onDismissRequest = { showModeMenu = false },
                modifier = Modifier.background(LuminaDarkSurface)
            ) {
                ShootingMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.displayName, color = Color.White) },
                        onClick = {
                            onSelectShootingMode(mode)
                            showModeMenu = false
                        }
                    )
                }
            }
        }

        // 4. Overlays Menu Toggle
        Box {
            IconButton(onClick = { showOverlayMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Overlays",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = showOverlayMenu,
                onDismissRequest = { showOverlayMenu = false },
                modifier = Modifier.background(LuminaDarkSurface)
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Grid: ${uiState.overlayState.gridType.displayName}", color = Color.White)
                    },
                    onClick = onToggleGrid
                )
                DropdownMenuItem(
                    text = {
                        Text("Histogram: ${uiState.overlayState.histogramMode.displayName}", color = Color.White)
                    },
                    onClick = onToggleHistogram
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Focus Peaking: ${if (uiState.overlayState.focusPeakingEnabled) "ON" else "OFF"}",
                            color = if (uiState.overlayState.focusPeakingEnabled) LuminaAmber else Color.White
                        )
                    },
                    onClick = onTogglePeaking
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Clipping Zebra: ${if (uiState.overlayState.exposureClippingEnabled) "ON" else "OFF"}",
                            color = if (uiState.overlayState.exposureClippingEnabled) LuminaAmber else Color.White
                        )
                    },
                    onClick = onToggleClipping
                )
            }
        }

        // 5. Settings Button
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }
    }
}
