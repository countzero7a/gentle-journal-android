package com.gentlejournal.domain.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongBackupPasswordException : Exception("Wrong backup password, or the file is damaged")

/**
 * Password-protected backup files: "GJBK", a format version, the PBKDF2 salt and
 * iteration count, the AES-GCM IV, then the encrypted backup JSON. Because the salt
 * and iterations travel in the file, the password alone opens it on another phone.
 * The header is authenticated too, so no part of the file can be altered unnoticed.
 */
object BackupCrypto {

    /** PBKDF2-HMAC-SHA256 iterations (OWASP's 2023 recommendation); takes a moment on a phone. */
    const val ITERATIONS = 600_000

    private val MAGIC = "GJBK".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 1
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val HEADER_LENGTH = 4 + 1 + SALT_LENGTH + 4 + IV_LENGTH

    /** A key derived from the backup password, kept so automatic backups need no password. */
    class BackupKey(val key: ByteArray, val salt: ByteArray, val iterations: Int) {
        fun encoded(): ByteArray =
            ByteBuffer.allocate(SALT_LENGTH + 4 + key.size).put(salt).putInt(iterations).put(key).array()

        companion object {
            fun decode(bytes: ByteArray): BackupKey {
                val buffer = ByteBuffer.wrap(bytes)
                val salt = ByteArray(SALT_LENGTH).also { buffer.get(it) }
                val iterations = buffer.int
                val key = ByteArray(buffer.remaining()).also { buffer.get(it) }
                return BackupKey(key, salt, iterations)
            }
        }
    }

    fun newKey(password: CharArray, iterations: Int = ITERATIONS, random: SecureRandom = SecureRandom()): BackupKey {
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        return BackupKey(deriveKey(password, salt, iterations), salt, iterations)
    }

    fun isEncrypted(data: ByteArray): Boolean =
        data.size > HEADER_LENGTH && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(plain: ByteArray, key: BackupKey, random: SecureRandom = SecureRandom()): ByteArray {
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        val header = ByteBuffer.allocate(HEADER_LENGTH)
            .put(MAGIC).put(VERSION).put(key.salt).putInt(key.iterations).put(iv)
            .array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    /** Throws [WrongBackupPasswordException] for a wrong password or a damaged file. */
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        require(isEncrypted(data)) { "Not an encrypted backup" }
        val buffer = ByteBuffer.wrap(data, MAGIC.size, HEADER_LENGTH - MAGIC.size)
        val version = buffer.get()
        require(version == VERSION) { "Unknown backup format version $version" }
        val salt = ByteArray(SALT_LENGTH).also { buffer.get(it) }
        val iterations = buffer.int
        require(iterations in 1..10_000_000) { "Implausible iteration count" }
        val iv = ByteArray(IV_LENGTH).also { buffer.get(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(deriveKey(password, salt, iterations), "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(data, 0, HEADER_LENGTH)
        return try {
            cipher.doFinal(data, HEADER_LENGTH, data.size - HEADER_LENGTH)
        } catch (e: AEADBadTagException) {
            throw WrongBackupPasswordException()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
