package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.model.ExportFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports journal data in user-chosen format.
 * Data ownership: user controls their data export (TDD §3.1).
 */
class ExportEntriesUseCase {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val readableDateFormat = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())

    operator fun invoke(entries: List<EntryWithTags>, format: ExportFormat): String {
        return when (format) {
            ExportFormat.JSON -> toJson(entries)
            ExportFormat.CSV -> toCsv(entries)
            ExportFormat.MARKDOWN -> toMarkdown(entries)
        }
    }

    private fun toJson(entries: List<EntryWithTags>): String {
        val sb = StringBuilder()
        sb.appendLine("[")
        entries.forEachIndexed { index, e ->
            sb.appendLine("  {")
            sb.appendLine("    \"timestamp\": \"${dateFormat.format(Date(e.entry.timestamp))}\",")
            sb.appendLine("    \"text\": ${escapeJsonString(e.entry.text)},")
            sb.appendLine("    \"emotions\": [${e.emotions.joinToString(", ") { "\"${it.label}\"" }}],")
            sb.appendLine("    \"actions\": [${e.actions.joinToString(", ") { "\"${it.label}\"" }}]")
            sb.append("  }")
            if (index < entries.size - 1) sb.append(",")
            sb.appendLine()
        }
        sb.appendLine("]")
        return sb.toString()
    }

    private fun toCsv(entries: List<EntryWithTags>): String {
        val sb = StringBuilder()
        sb.appendLine("Timestamp,Text,Emotions,Actions")
        entries.forEach { e ->
            val ts = dateFormat.format(Date(e.entry.timestamp))
            val text = e.entry.text.replace("\"", "\"\"")
            val emotions = e.emotions.joinToString("; ") { it.label }
            val actions = e.actions.joinToString("; ") { it.label }
            sb.appendLine("\"$ts\",\"$text\",\"$emotions\",\"$actions\"")
        }
        return sb.toString()
    }

    private fun toMarkdown(entries: List<EntryWithTags>): String {
        val sb = StringBuilder()
        sb.appendLine("# Journal Export")
        sb.appendLine()
        entries.forEach { e ->
            val ts = readableDateFormat.format(Date(e.entry.timestamp))
            sb.appendLine("### $ts")
            if (e.emotions.isNotEmpty()) {
                sb.appendLine("**Emotions:** ${e.emotions.joinToString(", ") { "${it.emoji} ${it.label}" }}")
            }
            if (e.actions.isNotEmpty()) {
                sb.appendLine("**Actions:** ${e.actions.joinToString(", ") { "${it.emoji} ${it.label}" }}")
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

    private fun escapeJsonString(s: String): String {
        return "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
    }
}
