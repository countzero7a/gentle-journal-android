package com.gentlejournal.domain.calendar

import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryEntity
import com.gentlejournal.data.entity.EntryWithTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class CalendarMonthTest {

    // 1 November 2026 is a Sunday
    private val november = YearMonth.of(2026, 11)

    @Test
    fun mondayFirstWeeksStartWithBlanks() {
        val cells = CalendarMonth.cells(november, DayOfWeek.MONDAY)
        assertEquals(42, cells.size)
        assertTrue(cells.take(6).all { it == null })
        assertEquals(LocalDate.of(2026, 11, 1), cells[6])
        assertEquals(LocalDate.of(2026, 11, 30), cells[35])
        assertTrue(cells.drop(36).all { it == null })
    }

    @Test
    fun sundayFirstWeeksStartOnTheFirst() {
        val cells = CalendarMonth.cells(november, DayOfWeek.SUNDAY)
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 11, 1), cells[0])
    }

    @Test
    fun leapYearFebruaryHas29Days() {
        val cells = CalendarMonth.cells(YearMonth.of(2028, 2), DayOfWeek.MONDAY)
        assertEquals(0, cells.size % 7)
        assertEquals(29, cells.count { it != null })
    }

    private val happy = EmotionEntity(id = "e_happy", label = "Happy", emoji = "😊")
    private val sad = EmotionEntity(id = "e_sad", label = "Sad", emoji = "😢")
    private val calm = EmotionEntity(id = "e_calm", label = "Calm", emoji = "😌")
    private var id = 0

    private fun entry(day: Int, hour: Int, vararg feelings: EmotionEntity) = EntryWithTags(
        EntryEntity("e${id++}", LocalDate.of(2026, 11, day).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), ""),
        feelings.toList(), emptyList()
    )

    @Test
    fun theMostFrequentFeelingRepresentsTheDay() {
        val result = CalendarMonth.mainFeelingByDay(listOf(entry(3, 9, happy), entry(3, 20, sad, happy)), ZoneOffset.UTC)
        assertEquals("e_happy", result[LocalDate.of(2026, 11, 3)]?.id)
    }

    @Test
    fun aTieGoesToTheFeelingNotedFirst() {
        val result = CalendarMonth.mainFeelingByDay(listOf(entry(4, 21, sad), entry(4, 8, calm)), ZoneOffset.UTC)
        assertEquals("e_calm", result[LocalDate.of(2026, 11, 4)]?.id)
    }

    @Test
    fun daysWithoutFeelingsHaveNoEmoji() {
        val result = CalendarMonth.mainFeelingByDay(listOf(entry(5, 9)), ZoneOffset.UTC)
        assertNull(result[LocalDate.of(2026, 11, 5)])
        assertFalse(result.containsKey(LocalDate.of(2026, 11, 5)))
    }
}
