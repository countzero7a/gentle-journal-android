package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryEntity
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.model.ExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class ExportImportTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val exporter = ExportEntriesUseCase()
    private val importer = ImportEntriesUseCase()

    private val happy = EmotionEntity(id = "e_happy", label = "Happy", emoji = "\uD83D\uDE0A", isDefault = true)
    private val quoted = EmotionEntity(id = "custom-1", label = "Quote \"this\", ok", emoji = "\uD83E\uDD14")
    private val nature = ActionEntity(id = "a_nature", label = "Nature", emoji = "\uD83C\uDF3F", isDefault = true)
    private val gardening = ActionEntity(id = "custom-2", label = "Back\\slash gardening", emoji = "\uD83C\uDF31")

    private val trickyText = "He said \"hi\"\\path\nline two\r\nline three\ttab \uD83D\uDE0A"

    private val entries = listOf(
        EntryWithTags(EntryEntity("entry-1", 1_700_000_000_000, trickyText), listOf(happy, quoted), listOf(nature, gardening)),
        EntryWithTags(EntryEntity("entry-2", 1_700_000_090_000, ""), listOf(happy), emptyList())
    )

    private fun exportJson() = exporter(
        entries, listOf(happy, quoted), listOf(nature, gardening), ExportFormat.JSON,
        zone = zone, now = Instant.ofEpochMilli(1_700_000_100_000)
    )

    @Test
    fun jsonRoundTripRestoresTextAndCustomTagsIntoEmptyJournal() {
        val parsed = importer.parse(exportJson(), zone)
        assertEquals(0, parsed.errorCount)

        val plan = importer.plan(parsed, emptyList(), emptyList(), emptyList())

        assertEquals(listOf("entry-1", "entry-2"), plan.entries.map { it.entry.id })
        val first = plan.entries.first()
        assertEquals(trickyText, first.entry.text)
        assertEquals(1_700_000_000_000, first.entry.timestamp)
        assertEquals(listOf("e_happy", "custom-1"), first.emotionIds)
        assertEquals(listOf("a_nature", "custom-2"), first.actionIds)

        val restored = plan.newEmotions.single { it.id == "custom-1" }
        assertEquals("Quote \"this\", ok", restored.label)
        assertEquals("\uD83E\uDD14", restored.emoji)
        assertEquals("Back\\slash gardening", plan.newActions.single { it.id == "custom-2" }.label)
    }

    @Test
    fun importingTheSameBackupTwiceAddsNothing() {
        val parsed = importer.parse(exportJson(), zone)
        val plan = importer.plan(
            parsed,
            existingEmotions = listOf(happy, quoted),
            existingActions = listOf(nature, gardening),
            existingEntries = entries.map { it.entry }
        )
        assertTrue(plan.entries.isEmpty())
        assertTrue(plan.newEmotions.isEmpty())
        assertTrue(plan.newActions.isEmpty())
        assertEquals(2, plan.duplicateCount)
    }

    @Test
    fun legacyArrayExportStillImports() {
        val legacy = """
            [
              {
                "timestamp": "2024-03-05 21:15",
                "text": "Old entry",
                "emotions": ["happy", "Wistful"],
                "actions": ["Nature"]
              }
            ]
        """.trimIndent()

        val parsed = importer.parse(legacy, zone)
        val plan = importer.plan(parsed, listOf(happy), listOf(nature), emptyList(), newId = { "new-id" })

        val entry = plan.entries.single()
        assertEquals(java.time.LocalDateTime.of(2024, 3, 5, 21, 15).toInstant(ZoneOffset.UTC).toEpochMilli(), entry.entry.timestamp)
        assertEquals("Old entry", entry.entry.text)
        // "happy" matches the default tag case-insensitively; "Wistful" becomes a custom tag
        assertEquals(listOf("e_happy", "new-id"), entry.emotionIds)
        assertEquals("Wistful", plan.newEmotions.single().label)
        assertEquals(ImportEntriesUseCase.DEFAULT_EMOTION_EMOJI, plan.newEmotions.single().emoji)
        assertEquals(listOf("a_nature"), entry.actionIds)
    }

    @Test
    fun legacyReimportIsSkippedByTimeAndText() {
        val legacy = """[{"timestamp": "2024-03-05 21:15", "text": "Old entry", "emotions": [], "actions": []}]"""
        val existing = EntryEntity(
            id = "already-here",
            timestamp = java.time.LocalDateTime.of(2024, 3, 5, 21, 15, 42).toInstant(ZoneOffset.UTC).toEpochMilli(),
            text = "Old entry"
        )
        val plan = importer.plan(importer.parse(legacy, zone), emptyList(), emptyList(), listOf(existing))
        assertTrue(plan.entries.isEmpty())
        assertEquals(1, plan.duplicateCount)
    }

    @Test
    fun labelsMatchTheLocalizedNameOfDefaultTags() {
        // A journal first created in Greek stores Greek labels for the default tags
        val greekHappy = happy.copy(label = "Χαρούμενος")
        val legacy = """[{"timestamp": "2024-03-05 21:15", "text": "x", "emotions": ["Happy"], "actions": []}]"""
        val plan = importer.plan(
            importer.parse(legacy, zone), listOf(greekHappy), emptyList(), emptyList(),
            emotionLabel = { if (it.id == "e_happy") "Happy" else it.label }
        )
        assertEquals(listOf("e_happy"), plan.entries.single().emotionIds)
        assertTrue(plan.newEmotions.isEmpty())
    }

    @Test
    fun unreadableEntriesAreCountedNotFatal() {
        val backup = """
            {"format": "gentle-journal", "version": 2, "emotions": [], "actions": [],
             "entries": [
               {"id": "ok", "timestampMillis": 1700000000000, "text": "fine"},
               {"id": "broken", "text": "no timestamp"}
             ]}
        """.trimIndent()
        val parsed = importer.parse(backup, zone)
        assertEquals(1, parsed.errorCount)
        assertEquals(listOf("ok"), parsed.entries.map { it.id })
    }

    @Test(expected = Exception::class)
    fun otherJsonIsRejected() {
        importer.parse("""{"something": "else"}""", zone)
    }

    @Test
    fun csvQuotesEveryFieldIncludingLabels() {
        val csv = exporter(entries, emptyList(), emptyList(), ExportFormat.CSV, zone = zone, locale = Locale.ROOT)
        val lines = csv.lines()
        assertEquals("\"Timestamp\",\"ISO timestamp\",\"Text\",\"Emotions\",\"Actions\"", lines[0])
        assertTrue(csv.contains("\"Happy; Quote \"\"this\"\", ok\""))
        assertTrue(csv.contains("\"He said \"\"hi\"\"\\path\nline two"))
        assertTrue(csv.contains("\"2023-11-14T22:13:20Z\""))
    }
}
