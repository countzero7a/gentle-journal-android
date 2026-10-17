package com.gentlejournal.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassphraseResolverTest {

    private class FakeStore(var stored: ByteArray? = null, var readFails: Boolean = false, var writeFails: Boolean = false) : PassphraseStore {
        var reads = 0
        override fun exists() = stored != null
        override fun read(): ByteArray {
            reads++
            if (readFails) throw IllegalStateException("damaged")
            return stored!!.copyOf()
        }
        override fun write(passphrase: ByteArray) {
            if (writeFails) throw IllegalStateException("disk full")
            stored = passphrase.copyOf()
            readFails = false
        }
    }

    private class FakeLegacy(val stored: ByteArray? = null, val readFails: Boolean = false) : LegacyPassphraseStore {
        var reads = 0
        override fun exists() = stored != null
        override fun read(): ByteArray {
            reads++
            if (readFails) throw IllegalStateException("keyset corrupted")
            return stored!!.copyOf()
        }
    }

    private val oldKey = ByteArray(32) { it.toByte() }
    private val otherKey = ByteArray(32) { (100 + it).toByte() }
    private val freshKey = ByteArray(32) { 7 }

    private fun resolve(store: FakeStore, legacy: FakeLegacy, databaseExists: Boolean) =
        PassphraseResolver.resolve(store, legacy, databaseExists) { freshKey.copyOf() }

    private fun assertKeyError(block: () -> Unit) {
        try {
            block()
        } catch (e: DatabaseKeyException) {
            return
        }
        throw AssertionError("Expected DatabaseKeyException")
    }

    @Test
    fun freshInstallCreatesAndSavesANewKey() {
        val store = FakeStore()
        assertArrayEquals(freshKey, resolve(store, FakeLegacy(), databaseExists = false))
        assertArrayEquals(freshKey, store.stored)
    }

    @Test
    fun migratesTheOldKeyUnchangedAndKeepsTheOldStore() {
        val store = FakeStore()
        val legacy = FakeLegacy(oldKey)
        assertArrayEquals(oldKey, resolve(store, legacy, databaseExists = true))
        assertArrayEquals(oldKey, store.stored)
        assertTrue(legacy.exists())
    }

    @Test
    fun afterMigrationTheOldStoreIsNotRead() {
        val legacy = FakeLegacy(otherKey)
        assertArrayEquals(oldKey, resolve(FakeStore(oldKey), legacy, databaseExists = true))
        assertEquals(0, legacy.reads)
    }

    @Test
    fun aDamagedNewStoreIsRepairedFromTheOldOne() {
        val store = FakeStore(oldKey, readFails = true)
        assertArrayEquals(oldKey, resolve(store, FakeLegacy(oldKey), databaseExists = true))
        assertArrayEquals(oldKey, store.stored)
    }

    @Test
    fun aDamagedNewStoreWithoutTheOldOneIsAnError() {
        assertKeyError { resolve(FakeStore(oldKey, readFails = true), FakeLegacy(), databaseExists = true) }
    }

    @Test
    fun neverCreatesAKeyOverAnExistingJournal() {
        val store = FakeStore()
        assertKeyError { resolve(store, FakeLegacy(), databaseExists = true) }
        assertNull(store.stored)
    }

    @Test
    fun anUnreadableOldStoreWithAJournalIsAnError() {
        val store = FakeStore()
        assertKeyError { resolve(store, FakeLegacy(oldKey, readFails = true), databaseExists = true) }
        assertNull(store.stored)
    }

    @Test
    fun anUnreadableOldStoreWithoutAJournalStartsFresh() {
        assertArrayEquals(freshKey, resolve(FakeStore(), FakeLegacy(oldKey, readFails = true), databaseExists = false))
    }

    @Test
    fun migrationStillOpensTheJournalWhenSavingFails() {
        val store = FakeStore(writeFails = true)
        assertArrayEquals(oldKey, resolve(store, FakeLegacy(oldKey), databaseExists = true))
        assertNull(store.stored)
    }

    @Test
    fun aNewKeyThatCanNotBeSavedIsAnError() {
        assertKeyError { resolve(FakeStore(writeFails = true), FakeLegacy(), databaseExists = false) }
    }
}
