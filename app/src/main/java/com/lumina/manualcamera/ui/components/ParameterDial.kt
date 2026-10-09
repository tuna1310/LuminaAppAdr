package com.lumina.manualcamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.manualcamera.data.model.ShutterSpeed
import com.lumina.manualcamera.data.model.WbPreset
import com.lumina.manualcamera.ui.CameraUiState
import com.lumina.manualcamera.ui.ControlTab
import com.lumina.manualcamera.ui.theme.LuminaAmber
import com.lumina.manualcamera.ui.theme.LuminaDarkSurface
import com.lumina.manualcamera.ui.theme.LuminaGreen
import com.lumina.manualcamera.ui.theme.LuminaTextPrimary
import com.lumina.manualcamera.ui.theme.LuminaTextSecondary

@Composable
fun ParameterDial(
    uiState: CameraUiState,
    onSetShutter: (Long) -> Unit,
    onToggleShutterAuto: () -> Unit,
    onSetIso: (Int) -> Unit,
    onToggleIsoAuto: () -> Unit,
    onSetFocus: (Float) -> Unit,
    onToggleFocusAuto: () -> Unit,
    onSetWbKelvin: (Int) -> Unit,
    onSetWbPreset: (WbPreset) -> Unit,
    onToggleWbAuto: () -> Unit,
    onSetEvIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.selectedTab == ControlTab.NONE) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(LuminaDarkSurface.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        when (uiState.selectedTab) {
            ControlTab.EV -> {
                EvDial(
                    currentEvIndex = uiState.exposureBias.evIndex,
                    minEv = uiState.capabilities.minEv,
                    maxEv = uiState.capabilities.maxEv,
                    evStep = uiState.capabilities.evStep,
                    onSelect = onSetEvIndex
                )
            }
            ControlTab.SHUTTER -> {
                ShutterDial(
                    currentExposureNs = uiState.shutterSpeed.exposureTimeNs,
                    isAuto = uiState.shutterSpeed.isAuto,
                    onSelect = onSetShutter,
                    onToggleAuto = onToggleShutterAuto
                )
            }
            ControlTab.ISO -> {
                IsoDial(
                    currentIso = uiState.iso.value,
                    isAuto = uiState.iso.isAuto,
                    minIso = uiState.capabilities.minIso,
                    maxIso = uiState.capabilities.maxIso,
                    onSelect = onSetIso,
                    onToggleAuto = onToggleIsoAuto
                )
            }
            ControlTab.FOCUS -> {
                FocusDial(
                    currentDiopters = uiState.focus.diopters,
                    isAuto = uiState.focus.isAuto,
                    maxDiopters = uiState.capabilities.minFocusDistance,
                    onSelect = onSetFocus,
                    onToggleAuto = onToggleFocusAuto
                )
            }
            ControlTab.WB -> {
                WbDial(
                    currentKelvin = uiState.whiteBalance.kelvin,
                    isAuto = uiState.whiteBalance.isAuto,
                    onSetKelvin = onSetWbKelvin,
                    onSetPreset = onSetWbPreset,
                    onToggleAuto = onToggleWbAuto
                )
            }
            ControlTab.NONE -> {}
        }
    }
}

@Composable
private fun EvDial(
    currentEvIndex: Int,
    minEv: Int,
    maxEv: Int,
    evStep: Float,
    onSelect: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in minEv..maxEv) {
            val evVal = i * evStep
            val label = when {
                i > 0 -> "+%.1f".format(evVal)
                i < 0 -> "%.1f".format(evVal)
                else -> "0.0"
            }
            DialButton(
                text = label,
                isSelected = currentEvIndex == i,
                onClick = { onSelect(i) }
            )
        }
    }
}

@Composable
private fun ShutterDial(
    currentExposureNs: Long,
    isAuto: Boolean,
    onSelect: (Long) -> Unit,
    onToggleAuto: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DialButton(
            text = "AUTO",
            isSelected = isAuto,
            isHighlightGreen = true,
            onClick = onToggleAuto
        )

        for (stepNs in ShutterSpeed.STANDARD_STEPS_NS) {
            val sec = stepNs / 1_000_000_000.0
            val label = ShutterSpeed.formatSeconds(sec)
            DialButton(
                text = label,
                isSelected = !isAuto && currentExposureNs == stepNs,
                onClick = { onSelect(stepNs) }
            )
        }
    }
}

@Composable
private fun IsoDial(
    currentIso: Int,
    isAuto: Boolean,
    minIso: Int,
    maxIso: Int,
    onSelect: (Int) -> Unit,
    onToggleAuto: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isoSteps = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400).filter { it in minIso..maxIso }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DialButton(
            text = "AUTO",
            isSelected = isAuto,
            isHighlightGreen = true,
            onClick = onToggleAuto
        )

        for (iso in isoSteps) {
            DialButton(
                text = "$iso",
                isSelected = !isAuto && currentIso == iso,
                onClick = { onSelect(iso) }
            )
        }
    }
}

@Composable
private fun FocusDial(
    currentDiopters: Float,
    isAuto: Boolean,
    maxDiopters: Float,
    onSelect: (Float) -> Unit,
    onToggleAuto: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DialButton(
            text = "AF",
            isSelected = isAuto,
            isHighlightGreen = true,
            onClick = onToggleAuto
        )

        Text(
            text = "∞",
            fontSize = 18.sp,
            color = LuminaTextSecondary,
            fontWeight = FontWeight.Bold
        )

        Slider(
            value = if (isAuto) 0.0f else currentDiopters,
            onValueChange = { onSelect(it) },
            valueRange = 0.0f..maxDiopters.coerceAtLeast(1.0f),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = LuminaAmber,
                activeTrackColor = LuminaAmber,
                inactiveTrackColor = Color.DarkGray
            )
        )

        Text(
            text = "Macro",
            fontSize = 12.sp,
            color = LuminaTextSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WbDial(
    currentKelvin: Int,
    isAuto: Boolean,
    onSetKelvin: (Int) -> Unit,
    onSetPreset: (WbPreset) -> Unit,
    onToggleAuto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hàng Preset Buttons
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialButton(
                text = "AWB",
                isSelected = isAuto,
                isHighlightGreen = true,
                onClick = onToggleAuto
            )

            WbPreset.values().filter { it != WbPreset.AUTO }.forEach { preset ->
                DialButton(
                    text = preset.displayName,
                    isSelected = !isAuto && currentKelvin == preset.kelvin,
                    onClick = { onSetPreset(preset) }
                )
            }
        }

        // Thanh trượt Kelvin mịn
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "2000K",
                fontSize = 10.sp,
                color = LuminaTextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Slider(
                value = currentKelvin.toFloat(),
                onValueChange = { onSetKelvin(it.toInt()) },
                valueRange = 2000f..10000f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = LuminaAmber,
                    activeTrackColor = LuminaAmber,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            Text(
                text = "10000K",
                fontSize = 10.sp,
                color = LuminaTextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun DialButton(
    text: String,
    isSelected: Boolean,
    isHighlightGreen: Boolean = false,
    onClick: () -> Unit
) {
    val highlightColor = if (isHighlightGreen) LuminaGreen else LuminaAmber
    val bg = if (isSelected) highlightColor.copy(alpha = 0.2f) else Color.Transparent
    val border = if (isSelected) highlightColor else Color.DarkGray
    val textColor = if (isSelected) highlightColor else LuminaTextPrimary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            fontFamily = FontFamily.Monospace
        )
    }
}
