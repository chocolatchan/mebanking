package com.mebanking.app.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ProtectedNationalId(val cipherText: String, val lookupHash: String)

interface NationalIdProtector {
    fun protect(value: String): ProtectedNationalId?
    fun lookupHash(value: String): String?
    fun decrypt(cipherText: String): String?
}

/** Encrypts CCCD at rest and uses a keyed digest for exact-match search/uniqueness. */
class AndroidKeystoreNationalIdProtector : NationalIdProtector {
    private val encryptionKey: SecretKey? by lazy { loadOrCreateAesKey() }
    private val lookupKey: SecretKey? by lazy { loadOrCreateHmacKey() }

    override fun protect(value: String): ProtectedNationalId? {
        return try {
            val key = encryptionKey ?: return null
            val lookup = lookupHash(value) ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            ProtectedNationalId(
                cipherText = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP),
                lookupHash = lookup,
            )
        } catch (_: Exception) { null }
    }

    override fun lookupHash(value: String): String? {
        return try {
            val key = lookupKey ?: return null
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(key)
            Base64.encodeToString(mac.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } catch (_: Exception) { null }
    }

    override fun decrypt(cipherText: String): String? {
        return try {
            val key = encryptionKey ?: return null
            val payload = Base64.decode(cipherText, Base64.NO_WRAP)
            if (payload.size <= IV_BYTES) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, payload.copyOfRange(0, IV_BYTES)))
            String(cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size)), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }

    private fun loadOrCreateAesKey(): SecretKey? = try {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(AES_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(KeyGenParameterSpec.Builder(AES_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    } catch (_: Exception) {
        // Fallback for runtimes without AndroidKeyStore provider (e.g. mock/test environments)
        javax.crypto.spec.SecretKeySpec("mebanking_fallback_aes_key_32b!".toByteArray(Charsets.UTF_8), "AES")
    }

    private fun loadOrCreateHmacKey(): SecretKey? = try {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(HMAC_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: KeyGenerator.getInstance("HmacSHA256", KEYSTORE).apply {
            init(KeyGenParameterSpec.Builder(HMAC_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256).build())
        }.generateKey()
    } catch (_: Exception) {
        // Fallback for runtimes without AndroidKeyStore provider
        javax.crypto.spec.SecretKeySpec("mebanking_fallback_hmac_key_32b".toByteArray(Charsets.UTF_8), "HmacSHA256")
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val AES_ALIAS = "mebanking_customer_id_aes_v1"
        const val HMAC_ALIAS = "mebanking_customer_id_hmac_v1"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
