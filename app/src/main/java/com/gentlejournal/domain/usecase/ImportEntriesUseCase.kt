package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryEntity
import com.gentlejournal.domain.model.JournalBackupFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** A tag as found in an import file. Old exports only carry the label. */
data class ImportedTag(
    val id: String?,
    val label: String,
    val emoji: String? = null,
    val parentId: String? = null,
    val sortOrder: Int? = null
)

data class ImportedEntry(
    val id: String?,
    val timestamp: Long,
    val text: String,
    val emotions: List<ImportedTag>,
    val actions: List<ImportedTag>
)

data class ParsedImport(
    val entries: List<ImportedEntry>,
    val errorCount: Int
)

data class PlannedEntry(
    val entry: EntryEntity,
    val emotionIds: List<String>,
    val actionIds: List<String>
)

/** What an import will write: new custom tags first, then the entries. */
data class ImportPlan(
    val newEmotions: List<EmotionEntity>,
    val newActions: List<ActionEntity>,
    val entries: List<PlannedEntry>,
    val duplicateCount: Int,
    val errorCount: Int
)

/**
 * Reads a Gentle Journal JSON export, either the current backup format
 * (see ExportEntriesUseCase) or the older array of entries with label lists.
 *
 * [parse] only reads the file; [plan] decides what to write given the current
 * database contents, so importing the same file twice adds nothing the second time.
 */
class ImportEntriesUseCase {

    private val legacyTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)

    /** Throws if the text isn't JSON or isn't a Gentle Journal export. */
    fun parse(jsonText: String, zone: ZoneId = ZoneId.systemDefault()): ParsedImport {
        return when (val root = Json.parseToJsonElement(jsonText)) {
            is JsonArray -> parseLegacy(root, zone)
            is JsonObject -> parseBackup(root)
            else -> throw IllegalArgumentException("Not a Gentle Journal export")
        }
    }

    private fun parseBackup(root: JsonObject): ParsedImport {
        require(root.string("format") == JournalBackupFormat.NAME) { "Not a Gentle Journal export" }
        val emotions = root.array("emotions").mapNotNull { parseTag(it) }.associateBy { it.id }
        val actions = root.array("actions").mapNotNull { parseTag(it) }.associateBy { it.id }

        var errors = 0
        val entries = root.array("entries").mapNotNull { element ->
            try {
                val obj = element.jsonObject
                ImportedEntry(
                    id = obj.string("id"),
                    timestamp = obj["timestampMillis"]?.jsonPrimitive?.longOrNull
                        ?: throw IllegalArgumentException("Entry without a timestamp"),
                    text = obj.string("text") ?: "",
                    emotions = obj.array("emotionIds").mapNotNull { emotions[it.jsonPrimitive.contentOrNull] },
                    actions = obj.array("actionIds").mapNotNull { actions[it.jsonPrimitive.contentOrNull] }
                )
            } catch (e: Exception) {
                errors++
                null
            }
        }
        return ParsedImport(entries, errors)
    }

    private fun parseTag(element: JsonElement): ImportedTag? = try {
        val obj = element.jsonObject
        val id = obj.string("id")
        val label = obj.string("label")
        if (id == null || label.isNullOrBlank()) null
        else ImportedTag(
            id = id,
            label = label,
            emoji = obj.string("emoji"),
            parentId = obj.string("parentId"),
            sortOrder = obj["sortOrder"]?.jsonPrimitive?.intOrNull
        )
    } catch (e: Exception) {
        null
    }

    private fun parseLegacy(root: JsonArray, zone: ZoneId): ParsedImport {
        var errors = 0
        val entries = root.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                val timestamp = LocalDateTime.parse(obj.string("timestamp"), legacyTimestamp)
                    .atZone(zone).toInstant().toEpochMilli()
                ImportedEntry(
                    id = null,
                    timestamp = timestamp,
                    text = obj.string("text") ?: "",
                    emotions = obj.labels("emotions").map { ImportedTag(id = null, label = it) },
                    actions = obj.labels("actions").map { ImportedTag(id = null, label = it) }
                )
            } catch (e: Exception) {
                errors++
                null
            }
        }
        return ParsedImport(entries, errors)
    }

    /**
     * Matches imported tags to existing ones (by id, then by stored or localized
     * label), creates the rest as custom tags, and skips entries already present.
     */
    fun plan(
        parsed: ParsedImport,
        existingEmotions: List<EmotionEntity>,
        existingActions: List<ActionEntity>,
        existingEntries: List<EntryEntity>,
        emotionLabel: (EmotionEntity) -> String = { it.label },
        actionLabel: (ActionEntity) -> String = { it.label },
        newId: () -> String = { UUID.randomUUID().toString() }
    ): ImportPlan {
        val existingEmotionIds = existingEmotions.map { it.id }.toSet()
        val emotionTags = TagMatcher(
            existing = existingEmotions,
            idOf = { it.id },
            labelsOf = { listOf(it.label, emotionLabel(it)) },
            newId = newId
        ) { tag, id, index ->
            EmotionEntity(
                id = id,
                label = tag.label.trim(),
                emoji = tag.emoji?.takeIf { it.isNotBlank() } ?: DEFAULT_EMOTION_EMOJI,
                isDefault = false,
                sortOrder = tag.sortOrder ?: (100 + existingEmotions.size + index),
                parentId = tag.parentId?.takeIf { it in existingEmotionIds }
            )
        }
        val actionTags = TagMatcher(
            existing = existingActions,
            idOf = { it.id },
            labelsOf = { listOf(it.label, actionLabel(it)) },
            newId = newId
        ) { tag, id, index ->
            ActionEntity(
                id = id,
                label = tag.label.trim(),
                emoji = tag.emoji?.takeIf { it.isNotBlank() } ?: DEFAULT_ACTION_EMOJI,
                isDefault = false,
                sortOrder = tag.sortOrder ?: (100 + existingActions.size + index)
            )
        }

        val knownIds = existingEntries.map { it.id }.toMutableSet()
        val knownContent = existingEntries.map { contentKey(it.timestamp, it.text) }.toMutableSet()
        var duplicates = 0
        val planned = mutableListOf<PlannedEntry>()

        parsed.entries.forEach { imported ->
            val key = contentKey(imported.timestamp, imported.text)
            if ((imported.id != null && imported.id in knownIds) || key in knownContent) {
                duplicates++
                return@forEach
            }
            val id = imported.id ?: newId()
            knownIds += id
            knownContent += key
            planned += PlannedEntry(
                entry = EntryEntity(id = id, timestamp = imported.timestamp, text = imported.text),
                emotionIds = imported.emotions.mapNotNull { emotionTags.resolve(it) }.distinct(),
                actionIds = imported.actions.mapNotNull { actionTags.resolve(it) }.distinct()
            )
        }

        return ImportPlan(
            newEmotions = emotionTags.created,
            newActions = actionTags.created,
            entries = planned,
            duplicateCount = duplicates,
            errorCount = parsed.errorCount
        )
    }

    /** Two entries written in the same minute with the same text are treated as the same entry. */
    private fun contentKey(timestamp: Long, text: String) = "${timestamp / 60_000}|${text.trim()}"

    private class TagMatcher<T>(
        existing: List<T>,
        private val idOf: (T) -> String,
        labelsOf: (T) -> List<String>,
        private val newId: () -> String,
        private val create: (tag: ImportedTag, id: String, index: Int) -> T
    ) {
        private val ids = existing.map(idOf).toMutableSet()
        private val idByLabel = mutableMapOf<String, String>()
        val created = mutableListOf<T>()

        init {
            existing.forEach { t -> labelsOf(t).forEach { idByLabel.putIfAbsent(normalize(it), idOf(t)) } }
        }

        fun resolve(tag: ImportedTag): String? {
            if (tag.id != null && tag.id in ids) return tag.id
            val label = normalize(tag.label)
            if (label.isEmpty()) return null
            idByLabel[label]?.let { return it }

            val id = tag.id?.takeIf { it !in ids } ?: newId()
            val entity = create(tag, id, created.size)
            created += entity
            ids += idOf(entity)
            idByLabel[label] = id
            return id
        }

        private fun normalize(label: String) = label.trim().lowercase()
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.labels(key: String): List<String> =
        array(key).mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf { label -> label.isNotBlank() } }

    companion object {
        const val DEFAULT_EMOTION_EMOJI = "💭"
        const val DEFAULT_ACTION_EMOJI = "✨"
    }
}
