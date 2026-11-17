package com.gentlejournal.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A small secret (the database passphrase, the backup key) kept in a file under
 * noBackupFilesDir, encrypted with a non-exportable AES key held by the Android Keystore.
 */
internal class KeystoreSecretStore(context: Context, fileName: String, private val keyAlias: String) : PassphraseStore {

    private val file = AtomicFile(File(context.noBackupFilesDir, fileName))

    override fun exists(): Boolean = file.baseFile.exists()

    override fun read(): ByteArray {
        val bytes = file.readFully()
        require(bytes.size > 2 && bytes[0] == FORMAT_VERSION) { "Unknown key file format" }
        val ivLength = bytes[1].toInt()
        val iv = bytes.copyOfRange(2, 2 + ivLength)
        val encrypted = bytes.copyOfRange(2 + ivLength, bytes.size)
        // Never create a wrapping key while reading: a new one can't decrypt the old file
        val key = keyStore().getKey(keyAlias, null) as? SecretKey
            ?: throw IllegalStateException("The Keystore key is missing")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    override fun write(passphrase: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val iv = cipher.iv
        val bytes = byteArrayOf(FORMAT_VERSION, iv.size.toByte()) + iv + cipher.doFinal(passphrase)

        val out = file.startWrite()
        try {
            out.write(bytes)
            file.finishWrite(out)
        } catch (e: IOException) {
            file.failWrite(out)
            throw e
        }
    }

    fun delete() {
        file.delete()
        keyStore().deleteEntry(keyAlias)
    }

    private fun wrappingKey(): SecretKey {
        (keyStore().getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val FORMAT_VERSION: Byte = 1
    }
}
