package com.lumina.manualcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lumina.manualcamera.data.model.GridType
import com.lumina.manualcamera.data.model.HistogramMode
import com.lumina.manualcamera.data.model.LensType
import com.lumina.manualcamera.ui.components.CameraControls
import com.lumina.manualcamera.ui.components.CameraPreviewView
import com.lumina.manualcamera.ui.components.ManualControlBar
import com.lumina.manualcamera.ui.components.OverlayCanvas
import com.lumina.manualcamera.ui.components.ParameterDial
import com.lumina.manualcamera.ui.components.TopToolbar

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera Preview
        CameraPreviewView(
            cameraController = viewModel.cameraController,
            onTapFocus = { x, y, w, h ->
                viewModel.cameraController.tapToFocus(x, y, w, h)
            }
        )

        // 2. Overlays Layer (Grid, Realtime Histogram, Focus Peaking, Clipping)
        OverlayCanvas(
            overlayState = uiState.overlayState,
            analysisResult = analysisResult
        )

        // 3. Top Bar (Flash, RAW/JPG, Mode, Overlays menu, Settings)
        TopToolbar(
            uiState = uiState,
            onToggleFlash = { viewModel.toggleFlash() },
            onSelectFormat = { viewModel.setCaptureFormat(it) },
            onSelectShootingMode = { viewModel.setShootingMode(it) },
            onToggleGrid = {
                val next = when (uiState.overlayState.gridType) {
                    GridType.OFF -> GridType.RULE_OF_THIRDS
                    GridType.RULE_OF_THIRDS -> GridType.FOUR_BY_FOUR
                    GridType.FOUR_BY_FOUR -> GridType.GOLDEN_RATIO
                    GridType.GOLDEN_RATIO -> GridType.OFF
                }
                viewModel.setGridType(next)
            },
            onToggleHistogram = {
                val next = when (uiState.overlayState.histogramMode) {
                    HistogramMode.OFF -> HistogramMode.LUMA
                    HistogramMode.LUMA -> HistogramMode.RGB
                    HistogramMode.RGB -> HistogramMode.OFF
                }
                viewModel.setHistogramMode(next)
            },
            onTogglePeaking = { viewModel.toggleFocusPeaking() },
            onToggleClipping = { viewModel.toggleExposureClipping() },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 4. Bottom Controls Area (Parameter Dial + Manual Bar + Shutter)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Dial Bánh xe điều khiển tham số
            ParameterDial(
                uiState = uiState,
                onSetShutter = { viewModel.setShutterSpeedNs(it) },
                onToggleShutterAuto = { viewModel.toggleShutterAuto() },
                onSetIso = { viewModel.setIsoValue(it) },
                onToggleIsoAuto = { viewModel.toggleIsoAuto() },
                onSetFocus = { viewModel.setFocusDiopters(it) },
                onToggleFocusAuto = { viewModel.toggleFocusAuto() },
                onSetWbKelvin = { viewModel.setWbKelvin(it) },
                onSetWbPreset = { viewModel.setWbPreset(it) },
                onToggleWbAuto = { viewModel.toggleWbAuto() },
                onSetEvIndex = { viewModel.setExposureBiasIndex(it) }
            )

            // Dải 5 thông số thủ công EV, SS, ISO, MF, WB
            ManualControlBar(
                uiState = uiState,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Cụm nút chụp và chuyển đổi
            CameraControls(
                isCapturing = uiState.isCapturing,
                lastPhotoUri = uiState.lastCapturedPhotoUri,
                currentLens = uiState.currentLens,
                onShutterClick = { viewModel.takePhoto() },
                onGalleryClick = onOpenGallery,
                onSwitchLens = {
                    val next = when (uiState.currentLens) {
                        LensType.WIDE -> LensType.TELE
                        LensType.TELE -> LensType.ULTRA_WIDE
                        LensType.ULTRA_WIDE -> LensType.FRONT
                        LensType.FRONT -> LensType.WIDE
                    }
                    viewModel.switchLens(next)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        )
    }
}
