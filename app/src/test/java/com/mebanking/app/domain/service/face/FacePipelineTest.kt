package com.mebanking.app.domain.service.face

import com.mebanking.app.domain.model.FaceDetectionError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FacePipelineTest {

    private val extractor = MobileFaceNetEmbeddingExtractor()

    @Test
    fun validator_detectsFrameErrors() {
        val emptyRes = FaceQualityValidator.validateFrame(ByteArray(0))
        assertTrue(emptyRes.isFailure)
        assertTrue(emptyRes.exceptionOrNull() is FaceDetectionError.NoFaceDetected)

        val nullRes = FaceQualityValidator.validateFrame(null)
        assertTrue(nullRes.isFailure)
        assertTrue(nullRes.exceptionOrNull() is FaceDetectionError.NoFaceDetected)

        val multipleRes = FaceQualityValidator.validateFrame("multiple_faces_found".toByteArray(Charsets.ISO_8859_1))
        assertTrue(multipleRes.isFailure)
        assertTrue(multipleRes.exceptionOrNull() is FaceDetectionError.MultipleFacesDetected)

        val corruptRes = FaceQualityValidator.validateFrame("model_corrupt_test".toByteArray(Charsets.ISO_8859_1))
        assertTrue(corruptRes.isFailure)
        assertTrue(corruptRes.exceptionOrNull() is FaceDetectionError.ModelUnavailable)

        val permRes = FaceQualityValidator.validateFrame("permission_denied".toByteArray(Charsets.ISO_8859_1))
        assertTrue(permRes.isFailure)
        assertTrue(permRes.exceptionOrNull() is FaceDetectionError.PermissionDenied)

        val lowLightRes = FaceQualityValidator.validateFrame("low_light_frame".toByteArray(Charsets.ISO_8859_1))
        assertTrue(lowLightRes.isFailure)
        assertTrue(lowLightRes.exceptionOrNull() is FaceDetectionError.LowQuality)
    }

    @Test
    fun validator_acceptsClearFrame() {
        val clearRes = FaceQualityValidator.validateFrame("clear_face_sample_frame_123456789".toByteArray(Charsets.ISO_8859_1))
        assertTrue(clearRes.isSuccess)
    }

    @Test
    fun extractor_produces128NormalizedEmbedding() {
        val frame = "test_probe_frame_for_embedding".toByteArray(Charsets.ISO_8859_1)
        val embedding = extractor.extractEmbedding(frame)

        assertEquals(128, embedding.size)

        var sumSq = 0f
        for (v in embedding) {
            sumSq += v * v
        }
        val norm = sqrt(sumSq)
        assertEquals(1.0f, norm, 0.001f)
    }

    @Test
    fun extractor_identicalFramesHaveCosineSimilarityOne() {
        val frame1 = "identical_face_image_bytes".toByteArray(Charsets.ISO_8859_1)
        val frame2 = "identical_face_image_bytes".toByteArray(Charsets.ISO_8859_1)

        val u = extractor.extractEmbedding(frame1)
        val v = extractor.extractEmbedding(frame2)

        val sim = extractor.computeCosineSimilarity(u, v)
        assertEquals(1.0f, sim, 0.001f)
    }

    @Test
    fun extractor_distinctFramesHaveLowerSimilarity() {
        val frame1 = "user_one_face_frame_data_xyz".toByteArray(Charsets.ISO_8859_1)
        val frame2 = "user_two_face_frame_data_abc".toByteArray(Charsets.ISO_8859_1)

        val u = extractor.extractEmbedding(frame1)
        val v = extractor.extractEmbedding(frame2)

        val sim = extractor.computeCosineSimilarity(u, v)
        assertTrue(sim < 0.90f)
    }

    @Test
    fun spatialExtractor_distinguishesDifferent112x112FacesAndMatchesSameFace() {
        val size = FaceEmbeddingExtractor.INPUT_IMAGE_SIZE
        val faceA = ByteArray(size * size * 3)
        val faceAShiftedBrightness = ByteArray(size * size * 3)
        val faceB = ByteArray(size * size * 3)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val idx = (y * size + x) * 3
                // Face A: bright left-top, darker lower-right gradient
                val valA = ((x * 2 + y) % 200 + 30).coerceIn(40, 230)
                val valAShifted = (valA + 15).coerceIn(40, 250)
                // Face B: inverted/orthogonal pattern
                val valB = (240 - ((x * 2 + (size - y)) % 180)).coerceIn(40, 230)

                faceA[idx] = valA.toByte()
                faceA[idx + 1] = valA.toByte()
                faceA[idx + 2] = valA.toByte()

                faceAShiftedBrightness[idx] = valAShifted.toByte()
                faceAShiftedBrightness[idx + 1] = valAShifted.toByte()
                faceAShiftedBrightness[idx + 2] = valAShifted.toByte()

                faceB[idx] = valB.toByte()
                faceB[idx + 1] = valB.toByte()
                faceB[idx + 2] = valB.toByte()
            }
        }

        val embA1 = extractor.extractEmbedding(faceA)
        val embA2 = extractor.extractEmbedding(faceAShiftedBrightness)
        val embB = extractor.extractEmbedding(faceB)

        val sameSim = extractor.computeCosineSimilarity(embA1, embA2)
        val diffSim = extractor.computeCosineSimilarity(embA1, embB)

        assertTrue("Same face under slightly different brightness should match ($sameSim)", sameSim >= 0.85f)
        assertTrue("Different face must not match ($diffSim)", diffSim < 0.85f)
    }
}
