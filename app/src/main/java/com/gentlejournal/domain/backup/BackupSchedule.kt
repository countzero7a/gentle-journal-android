package com.gentlejournal.domain.backup

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class BackupFrequency(val days: Long) { DAILY(1), WEEKLY(7) }

/** When automatic backups run, what they're called, and which old ones are removed. */
object BackupSchedule {

    /** How many backup files are kept in the folder. */
    const val KEEP = 5

    private const val DAY_MS = 86_400_000L
    private const val SUGGEST_AFTER_DAYS = 30
    private const val MIN_ENTRIES_TO_SUGGEST = 10
    private val FILE_NAME = Regex("""^gentle-journal-backup-(\d{4}-\d{2}-\d{2})\.gjbackup$""")

    /** One file per day; a second backup the same day replaces the first. */
    fun fileName(date: LocalDate): String = "gentle-journal-backup-$date.gjbackup"

    /** Daily means once per calendar day, weekly once every seven days. */
    fun isDue(lastBackupAt: Long?, frequency: BackupFrequency, now: Long, zone: ZoneId): Boolean {
        if (lastBackupAt == null) return true
        val last = Instant.ofEpochMilli(lastBackupAt).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return !today.isBefore(last.plusDays(frequency.days))
    }

    /** Backups beyond the newest [keep]; files the app didn't name are never touched. */
    fun filesToDelete(names: List<String>, keep: Int = KEEP): List<String> =
        names.mapNotNull { name -> FILE_NAME.matchEntire(name)?.let { name to it.groupValues[1] } }
            .sortedByDescending { it.second }
            .drop(keep)
            .map { it.first }

    /**
     * A gentle suggestion on the timeline: only without automatic backups, once the
     * journal has some entries, nothing was saved for a month and it wasn't put off lately.
     */
    fun shouldSuggestBackup(autoBackupOn: Boolean, entryCount: Int, lastSavedAt: Long?, dismissedAt: Long?, now: Long): Boolean {
        if (autoBackupOn || entryCount < MIN_ENTRIES_TO_SUGGEST) return false
        val month = SUGGEST_AFTER_DAYS * DAY_MS
        if (lastSavedAt != null && now - lastSavedAt < month) return false
        if (dismissedAt != null && now - dismissedAt < month) return false
        return true
    }
}
