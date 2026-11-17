package com.gentlejournal.data

import android.content.Context
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom

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
    private const val DB_KEY_FILE = "db_key.bin"
    private const val DB_KEY_ALIAS = "gentle_journal_db_key"

    @Volatile
    private var cached: ByteArray? = null

    /** Throws [DatabaseKeyException] if the key can't be read. */
    @Synchronized
    fun getPassphrase(context: Context): ByteArray {
        cached?.let { return it.copyOf() }
        val app = context.applicationContext
        val passphrase = try {
            PassphraseResolver.resolve(
                store = KeystoreSecretStore(app, DB_KEY_FILE, DB_KEY_ALIAS),
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
        KeystoreSecretStore(app, DB_KEY_FILE, DB_KEY_ALIAS).delete()
        runCatching { LegacyKeyStore.delete(app) }
        cached = null
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
