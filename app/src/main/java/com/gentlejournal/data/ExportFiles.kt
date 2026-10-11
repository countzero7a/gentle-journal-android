package com.gentlejournal.data

import android.content.Context
import java.io.File

/**
 * Export files are written unencrypted to the cache so other apps can receive them
 * through the share sheet. They are cleared on app start, before each new export
 * and when all data is deleted, so plain-text copies of the journal don't linger.
 */
object ExportFiles {

    fun dir(context: Context): File = File(context.cacheDir, "exports")

    fun clear(context: Context) {
        dir(context).deleteRecursively()
    }
}
