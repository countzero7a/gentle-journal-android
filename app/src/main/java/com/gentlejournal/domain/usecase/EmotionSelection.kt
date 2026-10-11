package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.EmotionEntity

/** Emotion selection rules for the entry form. */
object EmotionSelection {

    const val MAX_SELECTED = 2

    /**
     * Toggles [tappedId]: at most [max] emotions, a sub-emotion takes its parent's
     * place, and deselecting a parent also removes its sub-emotions.
     */
    fun toggle(
        selected: List<String>,
        tappedId: String,
        all: List<EmotionEntity>,
        max: Int = MAX_SELECTED
    ): List<String> {
        val tapped = all.find { it.id == tappedId }

        if (tappedId in selected) {
            val childIds = if (tapped != null && tapped.parentId == null) {
                all.filter { it.parentId == tappedId }.map { it.id }.toSet()
            } else emptySet()
            return selected.filter { it != tappedId && it !in childIds }
        }

        val parentId = tapped?.parentId
        if (parentId != null && parentId in selected) {
            // A replacement, so it's allowed even when the limit is reached
            return selected.map { if (it == parentId) tappedId else it }
        }

        return if (selected.size < max) selected + tappedId else selected
    }
}
