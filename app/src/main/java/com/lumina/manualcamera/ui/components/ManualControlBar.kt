package com.lumina.manualcamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.lumina.manualcamera.ui.CameraUiState
import com.lumina.manualcamera.ui.ControlTab
import com.lumina.manualcamera.ui.theme.LuminaAmber
import com.lumina.manualcamera.ui.theme.LuminaDarkSurfaceVariant
import com.lumina.manualcamera.ui.theme.LuminaTextPrimary
import com.lumina.manualcamera.ui.theme.LuminaTextSecondary

@Composable
fun ManualControlBar(
    uiState: CameraUiState,
    onTabSelected: (ControlTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // EV Item
        ParamItem(
            label = "EV",
            value = uiState.exposureBias.displayString(),
            isSelected = uiState.selectedTab == ControlTab.EV,
            isManual = uiState.exposureBias.evIndex != 0,
            onClick = { onTabSelected(ControlTab.EV) }
        )

        // Shutter Speed Item
        ParamItem(
            label = "SEC",
            value = uiState.shutterSpeed.displayString(),
            isSelected = uiState.selectedTab == ControlTab.SHUTTER,
            isManual = !uiState.shutterSpeed.isAuto,
            onClick = { onTabSelected(ControlTab.SHUTTER) }
        )

        // ISO Item
        ParamItem(
            label = "ISO",
            value = uiState.iso.displayString(),
            isSelected = uiState.selectedTab == ControlTab.ISO,
            isManual = !uiState.iso.isAuto,
            onClick = { onTabSelected(ControlTab.ISO) }
        )

        // Focus Item
        ParamItem(
            label = "FOCUS",
            value = uiState.focus.displayString(),
            isSelected = uiState.selectedTab == ControlTab.FOCUS,
            isManual = !uiState.focus.isAuto,
            onClick = { onTabSelected(ControlTab.FOCUS) }
        )

        // White Balance Item
        ParamItem(
            label = "WB",
            value = uiState.whiteBalance.displayString(),
            isSelected = uiState.selectedTab == ControlTab.WB,
            isManual = !uiState.whiteBalance.isAuto,
            onClick = { onTabSelected(ControlTab.WB) }
        )
    }
}

@Composable
private fun ParamItem(
    label: String,
    value: String,
    isSelected: Boolean,
    isManual: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) LuminaDarkSurfaceVariant else Color.Transparent
    val borderColor = if (isSelected) LuminaAmber else Color.Transparent
    val valueColor = if (isManual) LuminaAmber else LuminaTextPrimary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = LuminaTextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
    }
}
