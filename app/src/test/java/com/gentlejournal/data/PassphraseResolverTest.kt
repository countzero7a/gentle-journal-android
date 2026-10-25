package com.gentlejournal.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassphraseResolverTest {

    private class FakeStore(
        var stored: ByteArray? = null,
        val readFails: Boolean = false,
        val writeFails: Boolean = false,
        val readsBackWrong: Boolean = false
    ) : PassphraseStore {
        override fun exists() = stored != null
        override fun read(): ByteArray {
            if (readFails) throw IllegalStateException("damaged")
            return if (readsBackWrong) ByteArray(32) else stored!!.copyOf()
        }
        override fun write(passphrase: ByteArray) {
            if (writeFails) throw IllegalStateException("disk full")
            stored = passphrase.copyOf()
        }
    }

    private val savedKey = ByteArray(32) { it.toByte() }
    private val freshKey = ByteArray(32) { 7 }

    private fun resolve(store: FakeStore, databaseExists: Boolean) =
        PassphraseResolver.resolve(store, databaseExists) { freshKey.copyOf() }

    private fun assertKeyError(block: () -> Unit) {
        try {
            block()
        } catch (e: DatabaseKeyException) {
            return
        }
        throw AssertionError("Expected DatabaseKeyException")
    }

    @Test
    fun usesTheStoredKey() {
        assertArrayEquals(savedKey, resolve(FakeStore(savedKey), databaseExists = true))
    }

    @Test
    fun freshInstallCreatesAndSavesANewKey() {
        val store = FakeStore()
        assertArrayEquals(freshKey, resolve(store, databaseExists = false))
        assertArrayEquals(freshKey, store.stored)
    }

    @Test
    fun neverCreatesAKeyOverAnExistingJournal() {
        val store = FakeStore()
        assertKeyError { resolve(store, databaseExists = true) }
        assertNull(store.stored)
    }

    @Test
    fun aDamagedStoreIsAnError() {
        assertKeyError { resolve(FakeStore(savedKey, readFails = true), databaseExists = true) }
    }

    @Test
    fun aNewKeyThatCanNotBeSavedIsAnError() {
        assertKeyError { resolve(FakeStore(writeFails = true), databaseExists = false) }
    }

    @Test
    fun aNewKeyThatDoesNotReadBackIsAnError() {
        assertKeyError { resolve(FakeStore(readsBackWrong = true), databaseExists = false) }
    }
}
