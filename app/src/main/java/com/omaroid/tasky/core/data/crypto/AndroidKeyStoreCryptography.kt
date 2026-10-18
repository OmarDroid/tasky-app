package com.omaroid.tasky.core.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.omaroid.tasky.core.domain.crypto.Cryptography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class AndroidKeyStoreCryptography : Cryptography {
    private companion object {
        const val KEY_ALIAS = "tasky-session"
        const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        const val IV_SIZE_BYTES = 16
    }

    private val keyStore = KeyStore
        .getInstance("AndroidKeyStore")
        .apply { load(null) }

    override suspend fun encrypt(plainText: String): String {
        return withContext(Dispatchers.IO) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = cipher.iv + encryptedBytes
            Base64.encodeToString(combined, Base64.NO_WRAP)
        }
    }

    override suspend fun decrypt(cipherText: String): String {
        return withContext(Dispatchers.IO) {
            val combined = Base64.decode(cipherText, Base64.DEFAULT)
            val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
            val encryptedBytes = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            decryptedBytes.toString(Charsets.UTF_8)
        }
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return KeyGenerator
            .getInstance(ALGORITHM)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(BLOCK_MODE)
                        .setEncryptionPaddings(PADDING)
                        .setRandomizedEncryptionRequired(true)
                        .setUserAuthenticationRequired(false)
                        .build(),
                )
            }
            .generateKey()
    }
}