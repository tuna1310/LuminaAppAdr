package com.lumina.manualcamera.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.manualcamera.R
import com.lumina.manualcamera.data.model.CaptureFormat
import com.lumina.manualcamera.data.model.ColorSpaceOption
import com.lumina.manualcamera.data.model.PostProcessingQuality
import com.lumina.manualcamera.ui.CameraViewModel
import com.lumina.manualcamera.ui.theme.LuminaAmber
import com.lumina.manualcamera.ui.theme.LuminaDarkSurface
import com.lumina.manualcamera.ui.theme.LuminaDarkSurfaceVariant
import com.lumina.manualcamera.ui.theme.LuminaGreen
import com.lumina.manualcamera.ui.theme.LuminaTextPrimary
import com.lumina.manualcamera.ui.theme.LuminaTextSecondary

@Composable
fun SettingsScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = stringResource(R.string.settings),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Capture Format (JPEG / HEIC / RAW)
            SectionCard(title = stringResource(R.string.format)) {
                CaptureFormat.values().forEach { format ->
                    val isSupported = if (format == CaptureFormat.RAW_DNG) uiState.capabilities.supportsRaw else true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isSupported) { viewModel.setCaptureFormat(format) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = format.displayName,
                                color = if (isSupported) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                            if (!isSupported) {
                                Text(
                                    text = "Not supported on this sensor",
                                    color = Color.Red.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        RadioButton(
                            selected = uiState.captureFormat == format,
                            onClick = { if (isSupported) viewModel.setCaptureFormat(format) },
                            enabled = isSupported,
                            colors = RadioButtonDefaults.colors(selectedColor = LuminaAmber)
                        )
                    }
                }
            }

            // 2. Post-Processing (Off / Low / Standard)
            SectionCard(title = stringResource(R.string.post_processing)) {
                PostProcessingQuality.values().forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setPostProcessing(quality) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = quality.displayName, color = Color.White, fontSize = 14.sp)
                        RadioButton(
                            selected = uiState.postProcessing == quality,
                            onClick = { viewModel.setPostProcessing(quality) },
                            colors = RadioButtonDefaults.colors(selectedColor = LuminaAmber)
                        )
                    }
                }
            }

            // 3. Color Space (sRGB / Display P3)
            SectionCard(title = stringResource(R.string.color_space)) {
                ColorSpaceOption.values().forEach { space ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setColorSpace(space) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = space.displayName, color = Color.White, fontSize = 14.sp)
                        RadioButton(
                            selected = uiState.colorSpace == space,
                            onClick = { viewModel.setColorSpace(space) },
                            colors = RadioButtonDefaults.colors(selectedColor = LuminaAmber)
                        )
                    }
                }
            }

            // 4. Sensor Hardware Diagnostics
            SectionCard(title = "Sensor Hardware Information") {
                HardwareInfoRow("Manual Sensor Controls", if (uiState.capabilities.supportsManualSensor) "Yes" else "No")
                HardwareInfoRow("RAW (DNG) Capture Support", if (uiState.capabilities.supportsRaw) "Yes" else "No")
                HardwareInfoRow("ISO Range", "${uiState.capabilities.minIso} - ${uiState.capabilities.maxIso}")
                HardwareInfoRow("Shutter Range", "${uiState.capabilities.minExposureNs / 1_000_000}ms - ${uiState.capabilities.maxExposureNs / 1_000_000_000}s")
                HardwareInfoRow("EV Compensation", "${uiState.capabilities.minEv / 3} EV đến +${uiState.capabilities.maxEv / 3} EV")
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LuminaDarkSurface)
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = LuminaAmber,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun HardwareInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = LuminaTextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = if (value == "Yes") LuminaGreen else LuminaTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
