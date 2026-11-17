package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.format.MarkdownLite
import java.text.Normalizer

/** Finding entries by words in their text or the names of their feelings and activities. */
object EntrySearch {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    fun matches(
        entry: EntryWithTags,
        query: String,
        emotionLabel: (EmotionEntity) -> String,
        actionLabel: (ActionEntity) -> String
    ): Boolean {
        val q = normalize(query.trim())
        if (q.isEmpty()) return true
        // Formatting markers aren't part of what was written
        if (normalize(MarkdownLite.render(entry.entry.text).text).contains(q)) return true
        return entry.emotions.any { normalize(emotionLabel(it)).contains(q) || normalize(it.label).contains(q) } ||
            entry.actions.any { normalize(actionLabel(it)).contains(q) || normalize(it.label).contains(q) }
    }

    /** Case, accents and Greek final sigma don't matter: "χαρουμενος" finds "Χαρούμενος". */
    internal fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase().replace('ς', 'σ')
}
