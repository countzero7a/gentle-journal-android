package com.gentlejournal

import android.app.Application
import com.gentlejournal.data.ExportFiles

class GentleJournalApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Plain-text exports are only needed while the share sheet hands them over
        ExportFiles.clear(this)
    }
}
