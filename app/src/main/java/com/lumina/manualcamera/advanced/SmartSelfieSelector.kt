package com.lumina.manualcamera.advanced

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class SelfieFaceScore(
    val faceCount: Int,
    val averageSmileProb: Float,
    val averageEyeOpenProb: Float,
    val compositeScore: Float
)

object SmartSelfieSelector {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val faceDetector = FaceDetection.getClient(detectorOptions)

    /**
     * Tính điểm độ đẹp của ảnh dựa trên nụ cười và mắt mở
     */
    fun calculateScore(faces: List<Face>): SelfieFaceScore {
        if (faces.isEmpty()) {
            return SelfieFaceScore(0, 0f, 0f, 0f)
        }

        var totalSmile = 0f
        var totalEyes = 0f

        for (face in faces) {
            val smile = face.smilingProbability ?: 0.5f
            val leftEye = face.leftEyeOpenProbability ?: 0.5f
            val rightEye = face.rightEyeOpenProbability ?: 0.5f
            val eyeAvg = (leftEye + rightEye) / 2f

            totalSmile += smile
            totalEyes += eyeAvg
        }

        val avgSmile = totalSmile / faces.size
        val avgEyes = totalEyes / faces.size

        // Trọng số: Mắt mở là điều kiện tiên quyết (nhắm mắt = hỏng ảnh), nụ cười tăng điểm
        val composite = if (avgEyes < 0.4f) {
            avgEyes * 0.2f // Phạt nặng nếu nhắm mắt
        } else {
            avgEyes * 0.4f + avgSmile * 0.6f
        }

        return SelfieFaceScore(
            faceCount = faces.size,
            averageSmileProb = avgSmile,
            averageEyeOpenProb = avgEyes,
            compositeScore = composite
        )
    }

    /**
     * Phân tích InputImage từ ML Kit
     */
    suspend fun analyzeImage(image: InputImage): SelfieFaceScore = suspendCancellableCoroutine { cont ->
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val score = calculateScore(faces)
                cont.resume(score)
            }
            .addOnFailureListener {
                cont.resume(SelfieFaceScore(0, 0f, 0f, 0f))
            }
    }
}
