package com.gentlejournal.ui

/** Opening the app straight into a new entry with a feeling already chosen (from the widget). */
object QuickLog {
    const val EXTRA_EMOTION_ID = "com.gentlejournal.extra.EMOTION_ID"

    /** The feelings offered on the widget; all are default tags, which can't be deleted. */
    val WIDGET_EMOTIONS: List<String> = listOf("e_happy", "e_calm", "e_tired", "e_anxious", "e_sad")
}

/** A request to open the new-entry screen, optionally with a feeling selected. */
data class NewEntryRequest(val emotionId: String?)
