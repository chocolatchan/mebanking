package com.mebanking.app.domain.model

/**
 * Result of 1:1 biometric face identity verification.
 * Note: Represents feature vector similarity comparison only; no claims of anti-spoofing or liveness detection.
 */
sealed interface FaceRecognitionResult {
    data class Match(val confidence: Float, val similarity: Float) : FaceRecognitionResult
    data class Mismatch(val similarity: Float, val threshold: Float) : FaceRecognitionResult
    data class Error(val error: FaceRecognitionError) : FaceRecognitionResult
}

/**
 * Standard failure conditions encountered during biometric pipeline processing.
 */
sealed interface FaceRecognitionError {
    data object NoFaceDetected : FaceRecognitionError
    data object MultipleFacesDetected : FaceRecognitionError
    data class LowLightCondition(val brightness: Float) : FaceRecognitionError
    data object PoorQuality : FaceRecognitionError
    data class ModelLoadFailed(val cause: String) : FaceRecognitionError
    data object PermissionDenied : FaceRecognitionError
    data object TemplateNotFound : FaceRecognitionError
}

/**
 * Face detection exceptions thrown or returned as [Result.failure] during frame validation.
 */
sealed class FaceDetectionError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
    class NoFaceDetected(message: String = "Không tìm thấy khuôn mặt trong khung hình") :
        FaceDetectionError(message)

    class MultipleFacesDetected(message: String = "Phát hiện nhiều khuôn mặt trong khung hình") :
        FaceDetectionError(message)

    class LowQuality(message: String = "Chất lượng hình ảnh quá thấp / điều kiện ánh sáng yếu") :
        FaceDetectionError(message)

    class ModelUnavailable(message: String = "Mô hình nhận diện khuôn mặt ngoại tuyến không khả dụng") :
        FaceDetectionError(message)

    class PermissionDenied(message: String = "Quyền truy cập máy ảnh bị từ chối") :
        FaceDetectionError(message)
}
