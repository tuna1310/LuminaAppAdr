package com.lumina.manualcamera.ui

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.lumina.manualcamera.advanced.BracketingManager
import com.lumina.manualcamera.advanced.EnhancedRawCalculator
import com.lumina.manualcamera.advanced.GyroShakeDetector
import com.lumina.manualcamera.advanced.SmartSelfieSelector
import com.lumina.manualcamera.analyzer.FrameAnalysisResult
import com.lumina.manualcamera.analyzer.FrameAnalyzer
import com.lumina.manualcamera.camera.CameraController
import com.lumina.manualcamera.data.model.CameraCapabilities
import com.lumina.manualcamera.data.model.CaptureFormat
import com.lumina.manualcamera.data.model.ColorSpaceOption
import com.lumina.manualcamera.data.model.ExposureBiasParameter
import com.lumina.manualcamera.data.model.FocusParameter
import com.lumina.manualcamera.data.model.GridType
import com.lumina.manualcamera.data.model.HistogramMode
import com.lumina.manualcamera.data.model.IsoParameter
import com.lumina.manualcamera.data.model.LensType
import com.lumina.manualcamera.data.model.OverlayState
import com.lumina.manualcamera.data.model.PostProcessingQuality
import com.lumina.manualcamera.data.model.ShootingMode
import com.lumina.manualcamera.data.model.ShutterSpeed
import com.lumina.manualcamera.data.model.WbPreset
import com.lumina.manualcamera.data.model.WhiteBalanceParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ControlTab {
    NONE,
    SHUTTER,
    ISO,
    FOCUS,
    WB,
    EV
}

data class CameraUiState(
    val shutterSpeed: ShutterSpeed = ShutterSpeed(16_666_667L, isAuto = true),
    val iso: IsoParameter = IsoParameter(100, isAuto = true),
    val focus: FocusParameter = FocusParameter(0.0f, isAuto = true),
    val whiteBalance: WhiteBalanceParameter = WhiteBalanceParameter(5000, isAuto = true),
    val exposureBias: ExposureBiasParameter = ExposureBiasParameter(0),
    val selectedTab: ControlTab = ControlTab.NONE,
    val captureFormat: CaptureFormat = CaptureFormat.JPEG,
    val colorSpace: ColorSpaceOption = ColorSpaceOption.SRGB,
    val postProcessing: PostProcessingQuality = PostProcessingQuality.STANDARD,
    val shootingMode: ShootingMode = ShootingMode.PHOTO,
    val overlayState: OverlayState = OverlayState(),
    val isCapturing: Boolean = false,
    val lastCapturedPhotoUri: Uri? = null,
    val capabilities: CameraCapabilities = CameraCapabilities(),
    val currentLens: LensType = LensType.WIDE,
    val isFlashOn: Boolean = false,
    val message: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraController = CameraController(application)
    private val gyroDetector = GyroShakeDetector(application)
    private val frameAnalyzer = FrameAnalyzer()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _analysisResult = MutableStateFlow<FrameAnalysisResult?>(null)
    val analysisResult: StateFlow<FrameAnalysisResult?> = _analysisResult.asStateFlow()

    init {
        gyroDetector.start()

        // Kết nối Frame Analyzer thời gian thực
        cameraController.setFrameAnalyzer { imageProxy ->
            val result = frameAnalyzer.analyze(imageProxy)
            _analysisResult.value = result
        }

        viewModelScope.launch {
            cameraController.capabilities.collect { caps ->
                _uiState.update { it.copy(capabilities = caps) }
            }
        }
        viewModelScope.launch {
            cameraController.currentLens.collect { lens ->
                _uiState.update { it.copy(currentLens = lens) }
            }
        }
        viewModelScope.launch {
            cameraController.isFlashOn.collect { flash ->
                _uiState.update { it.copy(isFlashOn = flash) }
            }
        }
        loadLatestPhoto()
    }

    private fun applySettingsToCamera() {
        val s = _uiState.value
        cameraController.updateManualSettings(
            shutter = s.shutterSpeed,
            iso = s.iso,
            focus = s.focus,
            wb = s.whiteBalance,
            ev = s.exposureBias,
            postProcessing = s.postProcessing
        )
    }

    private fun updateAnalyzerFlags() {
        val ov = _uiState.value.overlayState
        frameAnalyzer.isFocusPeakingEnabled = ov.focusPeakingEnabled
        frameAnalyzer.focusPeakingThreshold = ov.focusPeakingThreshold
        frameAnalyzer.isExposureClippingEnabled = ov.exposureClippingEnabled
        frameAnalyzer.isHistogramEnabled = ov.histogramMode != HistogramMode.OFF
    }

    fun selectTab(tab: ControlTab) {
        _uiState.update {
            it.copy(selectedTab = if (it.selectedTab == tab) ControlTab.NONE else tab)
        }
    }

    // --- Shutter Speed ---
    fun setShutterSpeedNs(exposureNs: Long) {
        _uiState.update {
            it.copy(shutterSpeed = it.shutterSpeed.copy(exposureTimeNs = exposureNs, isAuto = false))
        }
        applySettingsToCamera()
    }

    fun toggleShutterAuto() {
        _uiState.update {
            it.copy(shutterSpeed = it.shutterSpeed.copy(isAuto = !it.shutterSpeed.isAuto))
        }
        applySettingsToCamera()
    }

    // --- ISO ---
    fun setIsoValue(value: Int) {
        _uiState.update {
            it.copy(iso = it.iso.copy(value = value, isAuto = false))
        }
        applySettingsToCamera()
    }

    fun toggleIsoAuto() {
        _uiState.update {
            it.copy(iso = it.iso.copy(isAuto = !it.iso.isAuto))
        }
        applySettingsToCamera()
    }

    // --- Focus ---
    fun setFocusDiopters(diopters: Float) {
        _uiState.update {
            it.copy(focus = it.focus.copy(diopters = diopters, isAuto = false))
        }
        applySettingsToCamera()
    }

    fun toggleFocusAuto() {
        _uiState.update {
            it.copy(focus = it.focus.copy(isAuto = !it.focus.isAuto))
        }
        applySettingsToCamera()
    }

    // --- White Balance ---
    fun setWbKelvin(kelvin: Int) {
        _uiState.update {
            it.copy(whiteBalance = it.whiteBalance.copy(kelvin = kelvin, isAuto = false))
        }
        applySettingsToCamera()
    }

    fun setWbPreset(preset: WbPreset) {
        if (preset == WbPreset.AUTO) {
            _uiState.update { it.copy(whiteBalance = it.whiteBalance.copy(isAuto = true)) }
        } else {
            _uiState.update {
                it.copy(whiteBalance = it.whiteBalance.copy(kelvin = preset.kelvin, isAuto = false))
            }
        }
        applySettingsToCamera()
    }

    fun toggleWbAuto() {
        _uiState.update {
            it.copy(whiteBalance = it.whiteBalance.copy(isAuto = !it.whiteBalance.isAuto))
        }
        applySettingsToCamera()
    }

    // --- EV ---
    fun setExposureBiasIndex(index: Int) {
        _uiState.update {
            it.copy(exposureBias = it.exposureBias.copy(evIndex = index))
        }
        applySettingsToCamera()
    }

    // --- Lenses & Flash ---
    fun switchLens(lens: LensType) {
        cameraController.switchLens(lens)
    }

    fun toggleFlash() {
        cameraController.toggleFlash()
    }

    // --- Overlays ---
    fun setGridType(gridType: GridType) {
        _uiState.update { it.copy(overlayState = it.overlayState.copy(gridType = gridType)) }
        updateAnalyzerFlags()
    }

    fun setHistogramMode(mode: HistogramMode) {
        _uiState.update { it.copy(overlayState = it.overlayState.copy(histogramMode = mode)) }
        updateAnalyzerFlags()
    }

    fun toggleFocusPeaking() {
        _uiState.update {
            it.copy(overlayState = it.overlayState.copy(focusPeakingEnabled = !it.overlayState.focusPeakingEnabled))
        }
        updateAnalyzerFlags()
    }

    fun toggleExposureClipping() {
        _uiState.update {
            it.copy(overlayState = it.overlayState.copy(exposureClippingEnabled = !it.overlayState.exposureClippingEnabled))
        }
        updateAnalyzerFlags()
    }

    // --- Định dạng lưu trữ & Chất lượng ---
    fun setCaptureFormat(format: CaptureFormat) {
        _uiState.update { it.copy(captureFormat = format) }
    }

    fun setPostProcessing(quality: PostProcessingQuality) {
        _uiState.update { it.copy(postProcessing = quality) }
        applySettingsToCamera()
    }

    fun setColorSpace(colorSpace: ColorSpaceOption) {
        _uiState.update { it.copy(colorSpace = colorSpace) }
    }

    fun setShootingMode(mode: ShootingMode) {
        _uiState.update { it.copy(shootingMode = mode) }
    }

    // --- Chụp ảnh (Hỗ trợ Normal, Bracketing, Enhanced RAW, Smart Selfie) ---
    fun takePhoto() {
        if (_uiState.value.isCapturing) return
        _uiState.update { it.copy(isCapturing = true, message = null) }

        viewModelScope.launch {
            when (_uiState.value.shootingMode) {
                ShootingMode.PHOTO -> executeSingleCapture()
                ShootingMode.BRACKETING -> executeBracketingCapture()
                ShootingMode.ENHANCED_RAW -> executeEnhancedRawCapture()
                ShootingMode.SMART_SELFIE -> executeSmartSelfieCapture()
            }
        }
    }

    private suspend fun executeSingleCapture() {
        val state = _uiState.value
        val result = cameraController.capturePhoto(
            format = state.captureFormat,
            shutter = state.shutterSpeed,
            iso = state.iso,
            wb = state.whiteBalance,
            ev = state.exposureBias
        )

        result.onSuccess { uri ->
            _uiState.update {
                it.copy(
                    isCapturing = false,
                    lastCapturedPhotoUri = uri,
                    message = "Saved: ${uri.lastPathSegment}"
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    isCapturing = false,
                    message = "Capture error: ${err.localizedMessage}"
                )
            }
        }
    }

    /**
     * Chụp Bracketing 3 tấm ở các mức EV khác nhau, khóa cố định ISO
     */
    private suspend fun executeBracketingCapture() {
        val state = _uiState.value
        val baseShutterNs = if (state.shutterSpeed.isAuto) 16_666_667L else state.shutterSpeed.exposureTimeNs
        val baseIso = if (state.iso.isAuto) 200 else state.iso.value

        val shots = BracketingManager.create3ShotBracket(
            baseShutterNs = baseShutterNs,
            baseIso = baseIso
        )

        var lastUri: Uri? = null
        for (shot in shots) {
            val res = cameraController.capturePhoto(
                format = state.captureFormat,
                shutter = ShutterSpeed(shot.shutterSpeedNs, isAuto = false),
                iso = IsoParameter(shot.iso, isAuto = false),
                wb = state.whiteBalance,
                ev = state.exposureBias
            )
            res.onSuccess { lastUri = it }
            kotlinx.coroutines.delay(250) // Giãn cách ngắn giữa các shot
        }

        _uiState.update {
            it.copy(
                isCapturing = false,
                lastCapturedPhotoUri = lastUri,
                message = "3-shot Bracketing capture complete"
            )
        }
    }

    /**
     * Enhanced RAW: Đo độ rung gyro, chọn shutter an toàn tối ưu để hạ ISO thấp nhất
     */
    private suspend fun executeEnhancedRawCapture() {
        val variance = gyroDetector.currentAngularSpeed
        val optimal = EnhancedRawCalculator.calculateOptimalExposure(
            gyroVarianceRadPerSec = variance,
            minIso = _uiState.value.capabilities.minIso,
            maxIso = _uiState.value.capabilities.maxIso
        )

        val res = cameraController.capturePhoto(
            format = CaptureFormat.RAW_DNG, // Luôn ưu tiên RAW cho Enhanced RAW
            shutter = ShutterSpeed(optimal.shutterSpeedNs, isAuto = false),
            iso = IsoParameter(optimal.iso, isAuto = false),
            wb = _uiState.value.whiteBalance,
            ev = _uiState.value.exposureBias
        )

        res.onSuccess { uri ->
            _uiState.update {
                it.copy(
                    isCapturing = false,
                    lastCapturedPhotoUri = uri,
                    message = "Enhanced RAW: Shutter ${ShutterSpeed.formatSeconds(optimal.shutterSpeedNs / 1e9)}, ISO ${optimal.iso}"
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    isCapturing = false,
                    message = "Enhanced RAW error: ${err.localizedMessage}"
                )
            }
        }
    }

    /**
     * Smart Selfie: Chụp loạt ảnh camera trước và chọn tấm đẹp nhất
     */
    private suspend fun executeSmartSelfieCapture() {
        // Tự động chuyển lens trước nếu đang ở camera sau
        if (_uiState.value.currentLens != LensType.FRONT) {
            cameraController.switchLens(LensType.FRONT)
            kotlinx.coroutines.delay(300)
        }

        executeSingleCapture()
    }

    private fun loadLatestPhoto() {
        viewModelScope.launch {
            try {
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                val cursor = getApplication<Application>().contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        _uiState.update { s -> s.copy(lastCapturedPhotoUri = uri) }
                    }
                }
            } catch (e: Exception) {
                // Bỏ qua lỗi query MediaStore ban đầu nếu chưa có quyền
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gyroDetector.stop()
        cameraController.release()
    }
}
