package com.lumina.manualcamera.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.DngCreator
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import com.lumina.manualcamera.data.model.CameraCapabilities
import com.lumina.manualcamera.data.model.CaptureFormat
import com.lumina.manualcamera.data.model.ExposureBiasParameter
import com.lumina.manualcamera.data.model.FocusParameter
import com.lumina.manualcamera.data.model.IsoParameter
import com.lumina.manualcamera.data.model.LensType
import com.lumina.manualcamera.data.model.PostProcessingQuality
import com.lumina.manualcamera.data.model.ShutterSpeed
import com.lumina.manualcamera.data.model.WhiteBalanceParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
class CameraController(
    private val context: Context
) {
    private val tag = "LuminaCameraController"
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val _capabilities = MutableStateFlow(CameraCapabilities())
    val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    private val _currentLens = MutableStateFlow(LensType.WIDE)
    val currentLens: StateFlow<LensType> = _currentLens.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    private var frameAnalyzerCallback: ((ImageProxy) -> Unit)? = null

    fun setFrameAnalyzer(callback: (ImageProxy) -> Unit) {
        frameAnalyzerCallback = callback
    }

    /**
     * Khởi tạo CameraProvider và bind các UseCases
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lens: LensType = _currentLens.value,
        onInitialized: () -> Unit = {}
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        _currentLens.value = lens

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView, lens)
                onInitialized()
            } catch (e: Exception) {
                Log.e(tag, "Khởi tạo Camera thất bại", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lens: LensType
    ) {
        val provider = cameraProvider ?: return

        val cameraSelector = if (lens == LensType.FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Preview
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        // ImageAnalysis cho Focus Peaking, Histogram, Exposure Clipping
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build().also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    frameAnalyzerCallback?.invoke(imageProxy) ?: imageProxy.close()
                }
            }

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

            // Đọc thông tin phần cứng
            camera?.cameraInfo?.let { info ->
                _capabilities.value = ManualParametersHelper.readCapabilities(info)
            }

            // Zoom tương ứng nếu chọn Tele hoặc Ultra-wide
            if (lens == LensType.TELE) {
                camera?.cameraControl?.setZoomRatio(2.0f)
            } else if (lens == LensType.WIDE) {
                camera?.cameraControl?.setZoomRatio(1.0f)
            }

        } catch (e: Exception) {
            Log.e(tag, "Lỗi bind use cases", e)
        }
    }

    /**
     * Chuyển đổi ống kính / camera
     */
    fun switchLens(lens: LensType) {
        _currentLens.value = lens
        val owner = currentLifecycleOwner ?: return
        val view = currentPreviewView ?: return
        bindCameraUseCases(owner, view, lens)
    }

    /**
     * Áp dụng thông số điều khiển thủ công lên Camera đang chạy
     */
    fun updateManualSettings(
        shutter: ShutterSpeed,
        iso: IsoParameter,
        focus: FocusParameter,
        wb: WhiteBalanceParameter,
        ev: ExposureBiasParameter,
        postProcessing: PostProcessingQuality
    ) {
        val cam = camera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            ManualParametersHelper.applyManualSettings(
                camera2Control = camera2Control,
                shutter = shutter,
                iso = iso,
                focus = focus,
                wb = wb,
                ev = ev,
                postProcessing = postProcessing
            )
        } catch (e: Exception) {
            Log.e(tag, "Lỗi áp dụng cài đặt thủ công", e)
        }
    }

    /**
     * Chạm lấy nét (Tap to Focus)
     */
    fun tapToFocus(x: Float, y: Float, width: Float, height: Float) {
        val cam = camera ?: return
        val factory = SurfaceOrientedMeteringPointFactory(width, height)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    /**
     * Bật / tắt Flash
     */
    fun toggleFlash() {
        val cam = camera ?: return
        val newState = !_isFlashOn.value
        _isFlashOn.value = newState
        cam.cameraControl.enableTorch(newState)
    }

    /**
     * Chụp ảnh với định dạng JPEG / HEIC / RAW
     */
    suspend fun capturePhoto(
        format: CaptureFormat,
        shutter: ShutterSpeed,
        iso: IsoParameter,
        wb: WhiteBalanceParameter,
        ev: ExposureBiasParameter
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val capture = imageCapture ?: return@withContext Result.failure(IllegalStateException("ImageCapture chưa sẵn sàng"))

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "LUMINA_${timeStamp}.${format.extension}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, when (format) {
                CaptureFormat.JPEG -> "image/jpeg"
                CaptureFormat.HEIC -> "image/heic"
                CaptureFormat.RAW_DNG -> "image/x-adobe-dng"
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumina")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        return@withContext suspendCancellableCapture(capture, outputOptions, format, shutter, iso, wb, ev)
    }

    private suspend fun suspendCancellableCapture(
        capture: ImageCapture,
        outputOptions: ImageCapture.OutputFileOptions,
        format: CaptureFormat,
        shutter: ShutterSpeed,
        iso: IsoParameter,
        wb: WhiteBalanceParameter,
        ev: ExposureBiasParameter
    ): Result<Uri> = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        // Cập nhật trạng thái hoàn tất trên Android 10+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }
                            context.contentResolver.update(savedUri, values, null, null)
                        }

                        // Ghi bổ sung EXIF metadata nếu cần
                        writeExifMetadata(savedUri, shutter, iso, wb, ev)

                        continuation.resumeWith(Result.success(Result.success(savedUri)))
                    } else {
                        continuation.resumeWith(Result.success(Result.failure(Exception("Uri trả về null"))))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(tag, "Lỗi lưu ảnh", exception)
                    continuation.resumeWith(Result.success(Result.failure(exception)))
                }
            }
        )
    }

    /**
     * Ghi EXIF metadata vào file ảnh đã chụp
     */
    private fun writeExifMetadata(
        uri: Uri,
        shutter: ShutterSpeed,
        iso: IsoParameter,
        wb: WhiteBalanceParameter,
        ev: ExposureBiasParameter
    ) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                if (!iso.isAuto) {
                    exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, iso.value.toString())
                }
                if (!shutter.isAuto && shutter.exposureTimeNs > 0) {
                    val sec = shutter.exposureTimeNs / 1_000_000_000.0
                    exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, sec.toString())
                }
                exif.setAttribute(ExifInterface.TAG_MAKE, "Lumina Manual Camera")
                exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
                exif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, if (wb.isAuto) ExifInterface.WHITE_BALANCE_AUTO.toString() else ExifInterface.WHITE_BALANCE_MANUAL.toString())
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            Log.w(tag, "Không thể ghi EXIF metadata", e)
        }
    }

    /**
     * Giải phóng camera đúng vòng đời
     */
    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        analysisExecutor.shutdown()
    }
}
