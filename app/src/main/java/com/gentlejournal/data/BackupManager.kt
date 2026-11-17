package com.gentlejournal.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.gentlejournal.data.repository.JournalRepository
import com.gentlejournal.domain.backup.BackupCrypto
import com.gentlejournal.domain.backup.BackupFrequency
import com.gentlejournal.domain.backup.BackupSchedule
import com.gentlejournal.domain.model.ExportFormat
import com.gentlejournal.domain.usecase.ExportEntriesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

sealed interface BackupResult {
    data class Saved(val at: Long) : BackupResult
    data object FolderUnavailable : BackupResult
    data object Failed : BackupResult
}

/**
 * Writes password-encrypted backups (the JSON export, see ExportEntriesUseCase) into a
 * folder the user picked through Android's folder picker, which may be a cloud folder;
 * the app itself never goes online. The key derived from the backup password is kept
 * Keystore-wrapped, so backups run without asking for the password.
 */
class BackupManager(
    private val context: Context,
    private val repository: JournalRepository,
    private val preferences: ThemePreferences
) {
    private val exporter = ExportEntriesUseCase()
    private val keyStore = KeystoreSecretStore(context, "backup_key.bin", "gentle_journal_backup_key")
    private val mutex = Mutex()

    /** Backs up if automatic backup is on and the last one is older than its frequency. */
    suspend fun backUpIfDue(): BackupResult? {
        val frequency = preferences.backupFrequency.first() ?: return null
        val due = BackupSchedule.isDue(preferences.lastBackupAt.first(), frequency, System.currentTimeMillis(), ZoneId.systemDefault())
        return if (due) backUpNow() else null
    }

    suspend fun backUpNow(): BackupResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val folder = folder() ?: return@withContext BackupResult.FolderUnavailable
            try {
                val json = exporter(
                    repository.getAllEntriesOnce(), repository.getEmotionsOnce(), repository.getActionsOnce(), ExportFormat.JSON
                )
                val data = BackupCrypto.encrypt(json.toByteArray(Charsets.UTF_8), BackupCrypto.BackupKey.decode(keyStore.read()))

                // A second backup on the same day replaces the first
                val name = BackupSchedule.fileName(LocalDate.now())
                folder.findFile(name)?.delete()
                val file = folder.createFile(MIME_TYPE, name) ?: return@withContext BackupResult.FolderUnavailable
                val written = context.contentResolver.openOutputStream(file.uri)?.use { it.write(data) } != null
                if (!written) return@withContext BackupResult.FolderUnavailable

                // Keep the newest few; files the app didn't name are never touched
                val files = folder.listFiles()
                val old = BackupSchedule.filesToDelete(files.mapNotNull { it.name }).toSet()
                files.filter { it.name in old }.forEach { it.delete() }

                val now = System.currentTimeMillis()
                preferences.setLastBackupAt(now)
                BackupResult.Saved(now)
            } catch (e: Exception) {
                BackupResult.Failed
            }
        }
    }

    /** Derives the key from the password (slow by design, so off the main thread) and remembers the folder. */
    suspend fun setUp(password: CharArray, folderUri: Uri, frequency: BackupFrequency) {
        withContext(Dispatchers.Default) {
            val key = BackupCrypto.newKey(password)
            takePermission(folderUri)
            keyStore.write(key.encoded())
        }
        preferences.setBackup(folderUri.toString(), frequency)
    }

    suspend fun changeFolder(folderUri: Uri) {
        val old = preferences.backupFolder.first()
        takePermission(folderUri)
        preferences.setBackupFolder(folderUri.toString())
        if (old != null && old != folderUri.toString()) releasePermission(Uri.parse(old))
    }

    /** Stops automatic backups; files already in the folder stay there. */
    suspend fun turnOff() {
        preferences.backupFolder.first()?.let { releasePermission(Uri.parse(it)) }
        preferences.turnOffBackup()
        withContext(Dispatchers.IO) { keyStore.delete() }
    }

    /** The folder's name for display, or null if it can't be reached any more. */
    suspend fun folderName(): String? = withContext(Dispatchers.IO) { folder()?.name }

    private suspend fun folder(): DocumentFile? {
        val uri = preferences.backupFolder.first()?.let(Uri::parse) ?: return null
        val folder = DocumentFile.fromTreeUri(context, uri) ?: return null
        return if (folder.canWrite()) folder else null
    }

    private fun takePermission(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    private fun releasePermission(uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private companion object {
        // Generic binary, so document providers keep the file name as given
        const val MIME_TYPE = "application/octet-stream"
    }
}
