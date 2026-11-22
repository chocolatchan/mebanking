package com.mebanking.app.domain.service

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * At-rest protection for local face templates.
 *
 * When the Android Keystore is available, templates are encrypted with
 * AES-256/GCM using a non-exportable key. If the Keystore is unavailable
 * (for example on an unsupported test runtime), callers keep the template in
 * memory only rather than writing plaintext or obfuscated bytes to disk.
 */
internal class FaceTemplateCipher(
    private val context: Context?,
) {

    companion object {
        private const val ALIAS = "mebanking_face_template_v1"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
        private val MAGIC = byteArrayOf('M'.code.toByte(), 'B'.code.toByte(), 'F'.code.toByte(), '1'.code.toByte())
        private const val FILE_PREFIX = "template_"
    }

    private val key: SecretKey? by lazy { loadOrCreateKey() }

    fun write(userId: Long, plaintext: ByteArray): Boolean {
        val ctx = context ?: return false
        val secretKey = key ?: return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext)
            val payload = ByteBuffer.allocate(MAGIC.size + 4 + iv.size + ciphertext.size)
                .put(MAGIC)
                .putInt(iv.size)
                .put(iv)
                .put(ciphertext)
                .array()

            val dir = File(ctx.filesDir, "biometrics").apply { mkdirs() }
            File(dir, "$FILE_PREFIX$userId.bin").writeBytes(payload)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun read(userId: Long): ByteArray? {
        val ctx = context ?: return null
        val secretKey = key ?: return null
        return try {
            val file = File(File(ctx.filesDir, "biometrics"), "$FILE_PREFIX$userId.bin")
            if (!file.exists()) return null
            val payload = file.readBytes()
            if (payload.size < MAGIC.size + 4) return null
            val buffer = ByteBuffer.wrap(payload)
            val magic = ByteArray(MAGIC.size)
            buffer.get(magic)
            if (!magic.contentEquals(MAGIC)) return null
            val ivSize = buffer.int
            if (ivSize !in 1..32 || buffer.remaining() < ivSize) return null
            val iv = ByteArray(ivSize).also { buffer.get(it) }
            val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (_: Throwable) {
            null
        }
    }

    fun exists(userId: Long): Boolean {
        val ctx = context ?: return false
        return File(File(ctx.filesDir, "biometrics"), "$FILE_PREFIX$userId.bin").exists()
    }

    fun delete(userId: Long) {
        val ctx = context ?: return
        try {
            val file = File(File(ctx.filesDir, "biometrics"), "$FILE_PREFIX$userId.bin")
            if (file.exists()) {
                file.writeBytes(ByteArray(file.length().toInt()))
                file.delete()
            }
        } catch (_: Throwable) {
            // Best-effort zeroization; the row/file is still removed when possible.
        }
    }

    private fun loadOrCreateKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            val existing = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                existing.secretKey
            } else {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
                generator.init(
                    KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build(),
                )
                generator.generateKey()
            }
        } catch (_: Throwable) {
            null
        }
    }
}
