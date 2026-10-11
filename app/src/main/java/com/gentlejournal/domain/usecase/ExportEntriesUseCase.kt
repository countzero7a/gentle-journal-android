package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.model.ExportFormat
import com.gentlejournal.domain.model.JournalBackupFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Exports journal data in user-chosen format.
 * Data ownership: user controls their data export (TDD §3.1).
 *
 * JSON is the backup format: it keeps ids, stored labels and emoji so that
 * ImportEntriesUseCase can restore entries and custom tags exactly.
 * CSV and Markdown are for reading, so they use the localized tag labels.
 */
class ExportEntriesUseCase {

    private val prettyJson = Json { prettyPrint = true }
    private val localTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)

    operator fun invoke(
        entries: List<EntryWithTags>,
        emotions: List<EmotionEntity>,
        actions: List<ActionEntity>,
        format: ExportFormat,
        emotionLabel: (EmotionEntity) -> String = { it.label },
        actionLabel: (ActionEntity) -> String = { it.label },
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
        now: Instant = Instant.now()
    ): String {
        return when (format) {
            ExportFormat.JSON -> toJson(entries, emotions, actions, zone, now)
            ExportFormat.CSV -> toCsv(entries, emotionLabel, actionLabel, zone)
            ExportFormat.MARKDOWN -> toMarkdown(entries, emotionLabel, actionLabel, zone, locale)
        }
    }

    private fun toJson(
        entries: List<EntryWithTags>,
        emotions: List<EmotionEntity>,
        actions: List<ActionEntity>,
        zone: ZoneId,
        now: Instant
    ): String {
        val root = buildJsonObject {
            put("format", JsonPrimitive(JournalBackupFormat.NAME))
            put("version", JsonPrimitive(JournalBackupFormat.VERSION))
            put("exportedAt", JsonPrimitive(isoTimestamp(now.toEpochMilli(), zone)))
            put("emotions", buildJsonArray {
                emotions.forEach { e ->
                    add(buildJsonObject {
                        put("id", JsonPrimitive(e.id))
                        put("label", JsonPrimitive(e.label))
                        put("emoji", JsonPrimitive(e.emoji))
                        put("parentId", JsonPrimitive(e.parentId))
                        put("isDefault", JsonPrimitive(e.isDefault))
                        put("sortOrder", JsonPrimitive(e.sortOrder))
                    })
                }
            })
            put("actions", buildJsonArray {
                actions.forEach { a ->
                    add(buildJsonObject {
                        put("id", JsonPrimitive(a.id))
                        put("label", JsonPrimitive(a.label))
                        put("emoji", JsonPrimitive(a.emoji))
                        put("isDefault", JsonPrimitive(a.isDefault))
                        put("sortOrder", JsonPrimitive(a.sortOrder))
                    })
                }
            })
            put("entries", buildJsonArray {
                entries.forEach { e ->
                    add(buildJsonObject {
                        put("id", JsonPrimitive(e.entry.id))
                        put("timestampMillis", JsonPrimitive(e.entry.timestamp))
                        put("timestamp", JsonPrimitive(isoTimestamp(e.entry.timestamp, zone)))
                        put("text", JsonPrimitive(e.entry.text))
                        put("emotionIds", JsonArray(e.emotions.map { JsonPrimitive(it.id) }))
                        put("actionIds", JsonArray(e.actions.map { JsonPrimitive(it.id) }))
                    })
                }
            })
        }
        return prettyJson.encodeToString(JsonElement.serializer(), root)
    }

    private fun toCsv(
        entries: List<EntryWithTags>,
        emotionLabel: (EmotionEntity) -> String,
        actionLabel: (ActionEntity) -> String,
        zone: ZoneId
    ): String {
        val sb = StringBuilder()
        sb.appendLine(csvRow("Timestamp", "ISO timestamp", "Text", "Emotions", "Actions"))
        entries.forEach { e ->
            sb.appendLine(
                csvRow(
                    Instant.ofEpochMilli(e.entry.timestamp).atZone(zone).format(localTimestamp),
                    isoTimestamp(e.entry.timestamp, zone),
                    e.entry.text,
                    e.emotions.joinToString("; ") { emotionLabel(it) },
                    e.actions.joinToString("; ") { actionLabel(it) }
                )
            )
        }
        return sb.toString()
    }

    private fun toMarkdown(
        entries: List<EntryWithTags>,
        emotionLabel: (EmotionEntity) -> String,
        actionLabel: (ActionEntity) -> String,
        zone: ZoneId,
        locale: Locale
    ): String {
        val readable = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
        val sb = StringBuilder()
        sb.appendLine("# Journal Export")
        sb.appendLine()
        entries.forEach { e ->
            sb.appendLine("### ${Instant.ofEpochMilli(e.entry.timestamp).atZone(zone).format(readable)}")
            if (e.emotions.isNotEmpty()) {
                sb.appendLine("**Emotions:** ${e.emotions.joinToString(", ") { "${it.emoji} ${emotionLabel(it)}" }}")
            }
            if (e.actions.isNotEmpty()) {
                sb.appendLine("**Actions:** ${e.actions.joinToString(", ") { "${it.emoji} ${actionLabel(it)}" }}")
            }
            if (e.entry.text.isNotBlank()) {
                sb.appendLine()
                sb.appendLine(e.entry.text)
            }
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun isoTimestamp(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    /** Quotes every field and doubles embedded quotes (RFC 4180). */
    private fun csvRow(vararg fields: String): String =
        fields.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" }
}
