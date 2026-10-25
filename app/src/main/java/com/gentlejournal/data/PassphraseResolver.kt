package com.gentlejournal.data

/** The database key couldn't be read; the journal can't be opened without it. */
class DatabaseKeyException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Where the database passphrase is kept (Keystore-wrapped file). */
interface PassphraseStore {
    fun exists(): Boolean
    fun read(): ByteArray
    fun write(passphrase: ByteArray)
}

/**
 * Decides where the SQLCipher passphrase comes from.
 *
 * A new passphrase is only ever created when there is no database yet: creating
 * one over an existing journal would make it unreadable for good.
 */
object PassphraseResolver {

    fun resolve(
        store: PassphraseStore,
        databaseExists: Boolean,
        newPassphrase: () -> ByteArray
    ): ByteArray {
        if (store.exists()) {
            return try {
                store.read()
            } catch (e: Exception) {
                throw DatabaseKeyException("The stored database key can't be read", e)
            }
        }

        if (databaseExists) throw DatabaseKeyException("The journal exists but its key is missing")

        val fresh = newPassphrase()
        try {
            // Read back, so a store that can't return the key is never relied on
            store.write(fresh)
            check(store.read().contentEquals(fresh)) { "The saved database key didn't read back" }
        } catch (e: Exception) {
            throw DatabaseKeyException("Couldn't save a new database key", e)
        }
        return fresh
    }
}
