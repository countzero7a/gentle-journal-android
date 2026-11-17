package com.gentlejournal.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class BackupScheduleTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private fun at(date: String, hour: Int) = LocalDateTime.of(LocalDate.parse(date), java.time.LocalTime.of(hour, 0)).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val day = 86_400_000L

    @Test
    fun theFirstBackupIsAlwaysDue() {
        assertTrue(BackupSchedule.isDue(null, BackupFrequency.WEEKLY, at("2026-11-17", 9), zone))
    }

    @Test
    fun dailyMeansOncePerCalendarDay() {
        val last = at("2026-11-16", 23)
        assertFalse(BackupSchedule.isDue(last, BackupFrequency.DAILY, at("2026-11-16", 23) + 60_000, zone))
        assertTrue(BackupSchedule.isDue(last, BackupFrequency.DAILY, at("2026-11-17", 0) + 60_000, zone))
    }

    @Test
    fun weeklyWaitsSevenDays() {
        val last = at("2026-11-10", 20)
        assertFalse(BackupSchedule.isDue(last, BackupFrequency.WEEKLY, at("2026-11-16", 23), zone))
        assertTrue(BackupSchedule.isDue(last, BackupFrequency.WEEKLY, at("2026-11-17", 8), zone))
    }

    @Test
    fun fileNamesCarryTheDate() {
        assertEquals("gentle-journal-backup-2026-11-17.gjbackup", BackupSchedule.fileName(LocalDate.of(2026, 11, 17)))
    }

    @Test
    fun onlyTheAppsOwnOldBackupsAreDeleted() {
        val names = (1..7).map { "gentle-journal-backup-2026-11-0$it.gjbackup" } + listOf("notes.txt", "gentle-journal-backup-old.gjbackup")
        assertEquals(
            listOf("gentle-journal-backup-2026-11-02.gjbackup", "gentle-journal-backup-2026-11-01.gjbackup"),
            BackupSchedule.filesToDelete(names)
        )
    }

    @Test
    fun aBackupIsSuggestedOnlyWhenItHelps() {
        val now = at("2026-11-17", 12)
        assertTrue(BackupSchedule.shouldSuggestBackup(false, 12, lastSavedAt = null, dismissedAt = null, now = now))
        assertFalse("automatic backup is on", BackupSchedule.shouldSuggestBackup(true, 12, null, null, now))
        assertFalse("too few entries", BackupSchedule.shouldSuggestBackup(false, 9, null, null, now))
        assertFalse("exported recently", BackupSchedule.shouldSuggestBackup(false, 12, now - 5 * day, null, now))
        assertFalse("put off recently", BackupSchedule.shouldSuggestBackup(false, 12, null, now - 5 * day, now))
        assertTrue("both a month ago", BackupSchedule.shouldSuggestBackup(false, 12, now - 31 * day, now - 31 * day, now))
    }
}
