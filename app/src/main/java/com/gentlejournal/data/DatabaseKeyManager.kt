package com.gentlejournal.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher encryption passphrase.
 *
 * - On first launch: generates a random 256-bit passphrase
 * - Keeps it encrypted (AES-256-GCM) with a key that never leaves the Android Keystore,
 *   in a small file under noBackupFilesDir
 * - Provides a SupportOpenHelperFactory for Room
 *
 * Before build 17 the passphrase lived in androidx.security-crypto's (deprecated)
 * EncryptedSharedPreferences. Build 17 migrated it; this version only deletes
 * what's left of that old store.
 *
 * The user never sees or manages a password.
 * If someone extracts the .db file, it is unreadable.
 */
object DatabaseKeyManager {

    const val DATABASE_NAME = "gentle_journal.db"

    private const val PASSPHRASE_LENGTH = 32 // 256 bits

    @Volatile
    private var cached: ByteArray? = null

    /** Throws [DatabaseKeyException] if the key can't be read. */
    @Synchronized
    fun getPassphrase(context: Context): ByteArray {
        cached?.let { return it.copyOf() }
        val app = context.applicationContext
        val passphrase = try {
            PassphraseResolver.resolve(
                store = KeystorePassphraseStore(app),
                databaseExists = app.getDatabasePath(DATABASE_NAME).exists(),
                newPassphrase = { ByteArray(PASSPHRASE_LENGTH).also { SecureRandom().nextBytes(it) } }
            )
        } catch (e: DatabaseKeyException) {
            throw e
        } catch (e: Exception) {
            throw DatabaseKeyException("The database key can't be loaded", e)
        }
        // The key came from the new store, so the old one is no longer needed
        runCatching { LegacyKeyStore.delete(app) }
        cached = passphrase
        // SQLCipher may clear the array it's given, so callers always get a copy
        return passphrase.copyOf()
    }

    fun getFactory(context: Context): SupportOpenHelperFactory = SupportOpenHelperFactory(getPassphrase(context))

    /**
     * Deletes the journal that can't be unlocked and every stored key, so the next start
     * creates an empty journal with a new key. Only for when the key is unreadable.
     */
    @Synchronized
    fun resetJournal(context: Context) {
        val app = context.applicationContext
        app.deleteDatabase(DATABASE_NAME)
        KeystorePassphraseStore(app).delete()
        runCatching { LegacyKeyStore.delete(app) }
        cached = null
    }
}

/** The passphrase, encrypted with a non-exportable AES key held by the Android Keystore. */
private class KeystorePassphraseStore(context: Context) : PassphraseStore {

    private val file = AtomicFile(File(context.noBackupFilesDir, "db_key.bin"))

    override fun exists(): Boolean = file.baseFile.exists()

    override fun read(): ByteArray {
        val bytes = file.readFully()
        require(bytes.size > 2 && bytes[0] == FORMAT_VERSION) { "Unknown key file format" }
        val ivLength = bytes[1].toInt()
        val iv = bytes.copyOfRange(2, 2 + ivLength)
        val encrypted = bytes.copyOfRange(2 + ivLength, bytes.size)
        // Never create a wrapping key while reading: a new one can't decrypt the old file
        val key = keyStore().getKey(KEY_ALIAS, null) as? SecretKey
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
        keyStore().deleteEntry(KEY_ALIAS)
    }

    private fun wrappingKey(): SecretKey {
        (keyStore().getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
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
        const val KEY_ALIAS = "gentle_journal_db_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val FORMAT_VERSION: Byte = 1
    }
}

/** What's left of the security-crypto store used before build 17, deleted once the new key works. */
private object LegacyKeyStore {

    private const val PREFS_NAME = "gentle_journal_crypto"
    // The alias security-crypto's MasterKey used for its Keystore key
    private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    fun delete(context: Context) {
        val prefsFile = File(context.dataDir, "shared_prefs/$PREFS_NAME.xml")
        if (!prefsFile.exists()) return
        context.deleteSharedPreferences(PREFS_NAME)
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(MASTER_KEY_ALIAS)
    }
}
