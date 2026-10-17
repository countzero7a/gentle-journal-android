package com.gentlejournal.data

/** The database key couldn't be read; the journal can't be opened without it. */
class DatabaseKeyException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Where the database passphrase is kept now (Keystore-wrapped file). */
interface PassphraseStore {
    fun exists(): Boolean
    fun read(): ByteArray
    fun write(passphrase: ByteArray)
}

/** The old security-crypto storage, only ever read (to migrate from). */
interface LegacyPassphraseStore {
    fun exists(): Boolean
    fun read(): ByteArray
}

/**
 * Decides where the SQLCipher passphrase comes from.
 *
 * The passphrase itself never changes, so the database file is never re-encrypted;
 * only where the passphrase is kept moves from the legacy store to [PassphraseStore].
 * The legacy store is left in place after migrating, so it can repair a damaged new
 * store. A new passphrase is only ever created when there is no database yet:
 * creating one over an existing journal would make it unreadable for good.
 */
object PassphraseResolver {

    fun resolve(
        store: PassphraseStore,
        legacy: LegacyPassphraseStore,
        databaseExists: Boolean,
        newPassphrase: () -> ByteArray
    ): ByteArray {
        if (store.exists()) {
            val failure = try {
                return store.read()
            } catch (e: Exception) {
                e
            }
            if (legacy.exists()) {
                val passphrase = try {
                    legacy.read()
                } catch (e: Exception) {
                    throw DatabaseKeyException("Neither key store can be read", failure)
                }
                runCatching { saveVerified(store, passphrase) }
                return passphrase
            }
            throw DatabaseKeyException("The stored database key can't be read", failure)
        }

        if (legacy.exists()) {
            val passphrase = try {
                legacy.read()
            } catch (e: Exception) {
                // Without a database there's nothing to lose, so a broken old store isn't fatal
                if (databaseExists) throw DatabaseKeyException("The old key store can't be read", e)
                null
            }
            if (passphrase != null) {
                // If saving fails, the old store still works and migration is retried next start
                runCatching { saveVerified(store, passphrase) }
                return passphrase
            }
        }

        if (databaseExists) throw DatabaseKeyException("The journal exists but its key is missing")

        val fresh = newPassphrase()
        try {
            saveVerified(store, fresh)
        } catch (e: Exception) {
            throw DatabaseKeyException("Couldn't save a new database key", e)
        }
        return fresh
    }

    /** Writes, then reads back, so a store that can't return the key is never relied on. */
    private fun saveVerified(store: PassphraseStore, passphrase: ByteArray) {
        store.write(passphrase)
        check(store.read().contentEquals(passphrase)) { "The saved database key didn't read back" }
    }
}
