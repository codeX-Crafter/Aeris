package com.runanywhere.kotlin_starter_example.services

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * A simple encryption manager to provide a layer of privacy for messages on Firestore.
 * Uses AES with a key derived from the user's UID.
 */
object EncryptionManager {
    private const val ALGORITHM = "AES"

    private fun getSecretKey(uid: String): SecretKeySpec {
        // Ensure the key is exactly 16 bytes for AES-128
        val keyBytes = uid.padEnd(16, '0').substring(0, 16).toByteArray()
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encrypt(text: String, uid: String): String {
        if (text.isBlank()) return text
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(uid))
            val encryptedBytes = cipher.doFinal(text.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            text // Fallback to plain text on error
        }
    }

    fun decrypt(encryptedText: String, uid: String): String {
        if (encryptedText.isBlank()) return encryptedText
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(uid))
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            String(cipher.doFinal(decodedBytes))
        } catch (e: Exception) {
            encryptedText // Return as is if decryption fails (might be unencrypted old data)
        }
    }
}
