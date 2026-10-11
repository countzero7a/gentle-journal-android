package com.gentlejournal.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

/**
 * Manages the SQLCipher encryption passphrase.
 *
 * - On first launch: generates a random 256-bit key
 * - Stores it in EncryptedSharedPreferences (backed by Android Keystore)
 * - Provides a SupportOpenHelperFactory for Room
 *
 * The user never sees or manages a password.
 * If someone extracts the .db file, it is unreadable.
 */
object DatabaseKeyManager {

    private const val PREFS_NAME = "gentle_journal_crypto"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_LENGTH = 32 // 256 bits

    fun getFactory(context: Context): SupportOpenHelperFactory {
        val passphrase = getOrCreatePassphrase(context)
        return SupportOpenHelperFactory(passphrase)
    }

    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return hexToBytes(existing)
        }

        // Generate new random passphrase
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphrase)

        // Store as hex string
        prefs.edit().putString(KEY_DB_PASSPHRASE, bytesToHex(passphrase)).apply()

        return passphrase
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
