package com.gentlejournal.ui

import android.content.res.Resources
import com.gentlejournal.R

/** Outcome of an import, turned into text by the UI so it follows the app language. */
sealed interface ImportStatus {
    data object ReadError : ImportStatus
    data object NoEntries : ImportStatus
    data object Failed : ImportStatus
    data class Done(val imported: Int, val duplicates: Int, val errors: Int) : ImportStatus
}

fun ImportStatus.message(res: Resources): String = when (this) {
    ImportStatus.ReadError -> res.getString(R.string.import_read_error)
    ImportStatus.NoEntries -> res.getString(R.string.import_no_entries)
    ImportStatus.Failed -> res.getString(R.string.import_failed)
    is ImportStatus.Done -> buildList {
        add(res.getQuantityString(R.plurals.import_imported, imported, imported))
        if (duplicates > 0) add(res.getQuantityString(R.plurals.import_duplicates, duplicates, duplicates))
        if (errors > 0) add(res.getQuantityString(R.plurals.import_errors, errors, errors))
    }.joinToString(" · ")
}
