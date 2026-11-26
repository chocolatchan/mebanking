package com.mebanking.app.ui.ekyc

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.mebanking.app.domain.service.face.FaceEmbeddingExtractor
import java.util.concurrent.atomic.AtomicBoolean

enum class FaceDetectionLiveState {
    INITIALIZING,
    NO_FACE,
    MULTIPLE_FACES,
    LOW_LIGHT,
    READY,
}

/**
 * Real-time CameraX ImageAnalysis analyzer powered by ML Kit Face Detection.
 *
 * Detects whether a single, well-lit face is present in the frame and crops it
 * to a standardized 112x112 RGB byte array for biometric feature extraction.
 */
class FaceFrameAnalyzer(
    private val onFaceStatus: (FaceDetectionLiveState, ByteArray?) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.18f)
            .build(),
    )

    private val isBusy = AtomicBoolean(false)
    private var lastAnalyzedMs = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalyzedMs < 250L || !isBusy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        lastAnalyzedMs = now

        try {
            val rotatedBitmap = imageProxyToRotatedGrayscaleBitmap(imageProxy)
            val inputImage = InputImage.fromBitmap(rotatedBitmap, 0)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    try {
                        when {
                            faces.isEmpty() -> {
                                onFaceStatus(FaceDetectionLiveState.NO_FACE, null)
                            }
                            faces.size > 1 -> {
                                onFaceStatus(FaceDetectionLiveState.MULTIPLE_FACES, null)
                            }
                            else -> {
                                val box = faces[0].boundingBox
                                val left = box.left.coerceIn(0, rotatedBitmap.width - 1)
                                val top = box.top.coerceIn(0, rotatedBitmap.height - 1)
                                val right = box.right.coerceIn(left + 1, rotatedBitmap.width)
                                val bottom = box.bottom.coerceIn(top + 1, rotatedBitmap.height)
                                val cropW = (right - left).coerceAtLeast(1)
                                val cropH = (bottom - top).coerceAtLeast(1)

                                val cropped = Bitmap.createBitmap(rotatedBitmap, left, top, cropW, cropH)
                                val targetSize = FaceEmbeddingExtractor.INPUT_IMAGE_SIZE
                                val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)

                                val pixels = IntArray(targetSize * targetSize)
                                scaled.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

                                val rgbBytes = ByteArray(targetSize * targetSize * 3)
                                var sumLuma = 0L
                                for (i in pixels.indices) {
                                    val c = pixels[i]
                                    val r = (c shr 16) and 0xFF
                                    val g = (c shr 8) and 0xFF
                                    val b = c and 0xFF
                                    rgbBytes[i * 3] = r.toByte()
                                    rgbBytes[i * 3 + 1] = g.toByte()
                                    rgbBytes[i * 3 + 2] = b.toByte()
                                    sumLuma += (r + g + b) / 3
                                }
                                val avgLuma = sumLuma.toFloat() / (pixels.size * 255f)
                                if (cropped !== rotatedBitmap) cropped.recycle()
                                if (scaled !== cropped) scaled.recycle()

                                if (avgLuma < 0.14f) {
                                    onFaceStatus(FaceDetectionLiveState.LOW_LIGHT, null)
                                } else {
                                    onFaceStatus(FaceDetectionLiveState.READY, rgbBytes)
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        onFaceStatus(FaceDetectionLiveState.NO_FACE, null)
                    }
                }
                .addOnFailureListener {
                    onFaceStatus(FaceDetectionLiveState.NO_FACE, null)
                }
                .addOnCompleteListener {
                    if (!rotatedBitmap.isRecycled) rotatedBitmap.recycle()
                    isBusy.set(false)
                    imageProxy.close()
                }
        } catch (_: Throwable) {
            isBusy.set(false)
            imageProxy.close()
        }
    }

    fun close() {
        try {
            detector.close()
        } catch (_: Throwable) {}
    }

    private fun imageProxyToRotatedGrayscaleBitmap(imageProxy: ImageProxy): Bitmap {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val srcW = imageProxy.width
        val srcH = imageProxy.height

        // Subsample for fast on-device face detection
        val step = (srcW / 240).coerceAtLeast(1)
        val dstW = (srcW / step).coerceAtLeast(1)
        val dstH = (srcH / step).coerceAtLeast(1)

        val rawBytes = ByteArray(buffer.remaining())
        buffer.rewind()
        buffer.get(rawBytes)

        val pixels = IntArray(dstW * dstH)
        for (y in 0 until dstH) {
            val srcY = (y * step).coerceAtMost(srcH - 1)
            val rowOffset = srcY * rowStride
            for (x in 0 until dstW) {
                val srcX = (x * step).coerceAtMost(srcW - 1)
                val idx = rowOffset + srcX * pixelStride
                val yVal = if (idx < rawBytes.size) (rawBytes[idx].toInt() and 0xFF) else 128
                pixels[y * dstW + x] = (0xFF shl 24) or (yVal shl 16) or (yVal shl 8) or yVal
            }
        }

        val baseBitmap = Bitmap.createBitmap(pixels, dstW, dstH, Bitmap.Config.ARGB_8888)
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0) return baseBitmap

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(baseBitmap, 0, 0, dstW, dstH, matrix, true)
        if (rotated !== baseBitmap) baseBitmap.recycle()
        return rotated
    }
}
