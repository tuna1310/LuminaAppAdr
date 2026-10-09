package com.lumina.manualcamera.camera

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Rational
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import com.lumina.manualcamera.data.model.CameraCapabilities
import com.lumina.manualcamera.data.model.ExposureBiasParameter
import com.lumina.manualcamera.data.model.FocusParameter
import com.lumina.manualcamera.data.model.IsoParameter
import com.lumina.manualcamera.data.model.PostProcessingQuality
import com.lumina.manualcamera.data.model.ShutterSpeed
import com.lumina.manualcamera.data.model.WhiteBalanceParameter

/**
 * Helper xây dựng và áp dụng các tham số điều khiển thủ công lên Camera2 qua Camera2Interop
 */
@OptIn(ExperimentalCamera2Interop::class)
object ManualParametersHelper {

    fun readCapabilities(cameraInfo: CameraInfo): CameraCapabilities {
        val camera2Info = Camera2CameraInfo.from(cameraInfo)

        // Kiểm tra capabilities
        val capabilities: IntArray? = camera2Info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true
        val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true

        // Dải phơi sáng
        val exposureRange: Range<Long>? = camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val minExp = exposureRange?.lower ?: 100_000L
        val maxExp = exposureRange?.upper ?: 1_000_000_000L

        // Dải ISO
        val isoRange: Range<Int>? = camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val minIso = isoRange?.lower ?: 100
        val maxIso = isoRange?.upper ?: 3200

        // Lấy nét tối thiểu (diopters: 0 = infinity, số càng lớn càng gần)
        val minFocus: Float? = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val focusDistance = minFocus ?: 10.0f

        // Bù trừ sáng EV
        val evRange: Range<Int>? = camera2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val evStepRational: Rational? = camera2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val minEv = evRange?.lower ?: -6
        val maxEv = evRange?.upper ?: 6
        val evStep = if (evStepRational != null && evStepRational.denominator != 0) {
            evStepRational.numerator.toFloat() / evStepRational.denominator.toFloat()
        } else {
            1f / 3f
        }

        return CameraCapabilities(
            supportsManualSensor = supportsManualSensor,
            supportsRaw = supportsRaw,
            minExposureNs = minExp,
            maxExposureNs = maxExp,
            minIso = minIso,
            maxIso = maxIso,
            minFocusDistance = focusDistance,
            minEv = minEv,
            maxEv = maxEv,
            evStep = evStep
        )
    }

    fun applyManualSettings(
        camera2Control: Camera2CameraControl,
        shutter: ShutterSpeed,
        iso: IsoParameter,
        focus: FocusParameter,
        wb: WhiteBalanceParameter,
        ev: ExposureBiasParameter,
        postProcessing: PostProcessingQuality
    ) {
        val builder = CaptureRequestOptions.Builder()

        // 1. Phơi sáng (AE, Shutter, ISO)
        val isManualExposure = !shutter.isAuto || !iso.isAuto
        if (isManualExposure) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )
            if (!shutter.isAuto) {
                builder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    shutter.exposureTimeNs
                )
            }
            if (!iso.isAuto) {
                builder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY,
                    iso.value
                )
            }
        } else {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )
            // Áp dụng EV Compensation khi ở chế độ AE
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                ev.evIndex
            )
        }

        // 2. Lấy nét (AF / Manual Focus)
        if (!focus.isAuto) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            builder.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                focus.diopters
            )
        } else {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        }

        // 3. Cân bằng trắng (AWB / Kelvin)
        if (!wb.isAuto) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF
            )
            builder.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
            )
            builder.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_GAINS,
                wb.toRggbVector()
            )
        } else {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
        }

        // 4. Khử nhiễu & Làm nét (Post-Processing)
        when (postProcessing) {
            PostProcessingQuality.OFF -> {
                builder.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CaptureRequest.EDGE_MODE_OFF
                )
            }
            PostProcessingQuality.LOW -> {
                builder.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_FAST
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CaptureRequest.EDGE_MODE_FAST
                )
            }
            PostProcessingQuality.STANDARD -> {
                builder.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CaptureRequest.EDGE_MODE_HIGH_QUALITY
                )
            }
        }

        camera2Control.setCaptureRequestOptions(builder.build())
    }
}
