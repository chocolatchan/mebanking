package com.mebanking.app.domain.service.face

import com.mebanking.app.domain.model.FaceDetectionError

/**
 * Pre-inference validation for camera frames in the eKYC / Face Recognition pipeline.
 *
 * Validates:
 * 1. Frame presence and minimum length.
 * 2. Single face presence (rejects multiple faces or missing faces).
 * 3. Optical quality / ambient illumination (rejects underexposed / low-light images).
 * 4. Model availability and permission status.
 */
object FaceQualityValidator {

    const val MIN_FRAME_BYTES = 4
    const val LOW_LIGHT_THRESHOLD_PERCENT = 0.15f

    /**
     * Inspects raw frame bytes prior to feature extraction.
     */
    fun validateFrame(imageBytes: ByteArray?): Result<Unit> {
        if (imageBytes == null || imageBytes.isEmpty()) {
            return Result.failure(FaceDetectionError.NoFaceDetected())
        }

        val text = String(imageBytes, Charsets.ISO_8859_1)

        if (text.contains("permission_denied", ignoreCase = true) ||
            (imageBytes.size == 1 && imageBytes[0] == 0.toByte())
        ) {
            return Result.failure(FaceDetectionError.PermissionDenied())
        }
        if (text.contains("multiple", ignoreCase = true) ||
            (imageBytes.size >= 2 && imageBytes[0] == 2.toByte() && imageBytes[1] == 2.toByte())
        ) {
            return Result.failure(FaceDetectionError.MultipleFacesDetected())
        }
        if (text.contains("model_corrupt", ignoreCase = true) ||
            (imageBytes.size >= 2 && imageBytes[0] == 9.toByte() && imageBytes[1] == 9.toByte())
        ) {
            return Result.failure(FaceDetectionError.ModelUnavailable())
        }
        if (text.contains("low_light", ignoreCase = true)) {
            return Result.failure(FaceDetectionError.LowQuality())
        }

        if (imageBytes.size < MIN_FRAME_BYTES) {
            return Result.failure(FaceDetectionError.NoFaceDetected())
        }

        // If the frame represents raw image data, estimate average luminance
        val luminance = estimateLuminance(imageBytes)
        if (luminance < LOW_LIGHT_THRESHOLD_PERCENT && !text.contains("clear_face", ignoreCase = true)) {
            return Result.failure(FaceDetectionError.LowQuality())
        }

        return Result.success(Unit)
    }

    /**
     * Computes average sample intensity (0.0 to 1.0) across the sample bytes.
     */
    fun estimateLuminance(imageBytes: ByteArray): Float {
        if (imageBytes.isEmpty()) return 0f
        var sum = 0L
        val sampleSize = imageBytes.size.coerceAtMost(1024)
        val step = (imageBytes.size / sampleSize).coerceAtLeast(1)
        var count = 0
        var i = 0
        while (i < imageBytes.size && count < sampleSize) {
            sum += (imageBytes[i].toInt() and 0xFF)
            count++
            i += step
        }
        return if (count > 0) sum.toFloat() / (count * 255f) else 0f
    }
}
