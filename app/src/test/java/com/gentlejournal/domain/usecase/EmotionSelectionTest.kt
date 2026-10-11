package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.EmotionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionSelectionTest {

    private val happy = EmotionEntity(id = "e_happy", label = "Happy", emoji = "")
    private val calm = EmotionEntity(id = "e_calm", label = "Calm", emoji = "")
    private val sad = EmotionEntity(id = "e_sad", label = "Sad", emoji = "")
    private val excited = EmotionEntity(id = "e_excited", label = "Excited", emoji = "", parentId = "e_happy")
    private val peaceful = EmotionEntity(id = "e_peaceful", label = "Peaceful", emoji = "", parentId = "e_calm")
    private val all = listOf(happy, calm, sad, excited, peaceful)

    private fun toggle(selected: List<String>, id: String) = EmotionSelection.toggle(selected, id, all)

    @Test
    fun addsUpToTwo() {
        assertEquals(listOf("e_happy", "e_calm"), toggle(listOf("e_happy"), "e_calm"))
    }

    @Test
    fun ignoresAThirdEmotion() {
        assertEquals(listOf("e_happy", "e_calm"), toggle(listOf("e_happy", "e_calm"), "e_sad"))
    }

    @Test
    fun subEmotionReplacesItsParentEvenAtTheLimit() {
        assertEquals(listOf("e_excited", "e_calm"), toggle(listOf("e_happy", "e_calm"), "e_excited"))
    }

    @Test
    fun deselectingAParentRemovesItsSubEmotions() {
        assertEquals(listOf("e_happy"), toggle(listOf("e_happy", "e_calm", "e_peaceful"), "e_calm"))
    }

    @Test
    fun deselectingASubEmotionKeepsTheRest() {
        assertEquals(listOf("e_calm"), toggle(listOf("e_excited", "e_calm"), "e_excited"))
    }
}
