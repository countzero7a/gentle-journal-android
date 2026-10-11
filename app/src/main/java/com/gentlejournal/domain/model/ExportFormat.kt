package com.gentlejournal.domain.model

enum class ExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
    MARKDOWN("md", "text/markdown")
}

/** Identifies the JSON backup format written by export and read back by import. */
object JournalBackupFormat {
    const val NAME = "gentle-journal"
    const val VERSION = 2
}
