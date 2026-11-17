package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryEntity
import com.gentlejournal.data.entity.EntryWithTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntrySearchTest {

    private val greekHappy = EmotionEntity(id = "e_happy", label = "Χαρούμενος", emoji = "😊")
    private val walk = ActionEntity(id = "custom", label = "Long walk", emoji = "🚶")
    private val entry = EntryWithTags(
        EntryEntity("1", 0, "Met **Anna** at the café\n- talked for hours"),
        listOf(greekHappy), listOf(walk)
    )

    private fun search(query: String) = EntrySearch.matches(
        entry, query,
        emotionLabel = { if (it.id == "e_happy") "Happy" else it.label },
        actionLabel = { it.label }
    )

    @Test
    fun findsWordsInTheTextWithoutFormattingMarks() {
        assertTrue(search("anna"))
        assertTrue(search("anna at"))
        assertFalse(search("**anna**"))
    }

    @Test
    fun accentsAndCaseDoNotMatter() {
        assertTrue(search("CAFE"))
        assertTrue(search("χαρουμενοσ"))
        assertTrue(search("χαρουμενος"))
    }

    @Test
    fun findsFeelingsByTheirTranslatedAndStoredNames() {
        assertTrue(search("happy"))
        assertTrue(search("Χαρούμενος"))
        assertTrue(search("walk"))
    }

    @Test
    fun aBlankQueryShowsEverything() {
        assertTrue(search("  "))
    }

    @Test
    fun unrelatedWordsDoNotMatch() {
        assertFalse(search("sad"))
    }
}
