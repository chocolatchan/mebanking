package com.mebanking.app.ui.home

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * CameraX ImageAnalysis Analyzer for real-time QR / VietQR decoding using ZXing.
 */
class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()
    @Volatile
    var isEnabled: Boolean = true

    override fun analyze(image: ImageProxy) {
        if (!isEnabled) {
            image.close()
            return
        }

        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val width = image.width
            val height = image.height

            val source = PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false,
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            if (result != null && result.text.isNotBlank()) {
                isEnabled = false
                onQrCodeScanned(result.text)
            }
        } catch (_: Throwable) {
            // No QR detected in current frame, ignore and proceed
        } finally {
            try {
                reader.reset()
            } catch (_: Throwable) {}
            image.close()
        }
    }
}
