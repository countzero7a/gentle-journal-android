package com.gentlejournal.domain.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    // Few iterations keep the tests fast; the iteration count travels in the file
    private val password = "correct horse".toCharArray()
    private val key = BackupCrypto.newKey(password, iterations = 1_000)
    private val json = """{"format":"gentle-journal","entries":[{"text":"Καλημέρα 🌿"}]}""".toByteArray()

    @Test
    fun theRightPasswordOpensTheBackup() {
        val encrypted = BackupCrypto.encrypt(json, key)
        assertTrue(BackupCrypto.isEncrypted(encrypted))
        assertArrayEquals(json, BackupCrypto.decrypt(encrypted, "correct horse".toCharArray()))
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun aWrongPasswordIsRejected() {
        BackupCrypto.decrypt(BackupCrypto.encrypt(json, key), "wrong horse".toCharArray())
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun aDamagedFileIsRejected() {
        val encrypted = BackupCrypto.encrypt(json, key)
        encrypted[encrypted.size - 5] = (encrypted[encrypted.size - 5] + 1).toByte()
        BackupCrypto.decrypt(encrypted, "correct horse".toCharArray())
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun theHeaderCanNotBeAlteredEither() {
        val encrypted = BackupCrypto.encrypt(json, key)
        encrypted[8] = (encrypted[8] + 1).toByte()  // inside the salt
        BackupCrypto.decrypt(encrypted, "correct horse".toCharArray())
    }

    @Test
    fun plainExportsAreNotMistakenForBackups() {
        assertFalse(BackupCrypto.isEncrypted(json))
        assertFalse(BackupCrypto.isEncrypted("[]".toByteArray()))
    }

    @Test
    fun theStoredKeySurvivesEncoding() {
        val decoded = BackupCrypto.BackupKey.decode(key.encoded())
        assertArrayEquals(key.key, decoded.key)
        assertArrayEquals(key.salt, decoded.salt)
        assertEquals(key.iterations, decoded.iterations)
        // A backup made with the stored key still opens with the password
        assertArrayEquals(json, BackupCrypto.decrypt(BackupCrypto.encrypt(json, decoded), password.copyOf()))
    }
}
