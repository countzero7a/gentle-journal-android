package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryEntity
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.model.InsightType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class GenerateInsightsUseCaseTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val useCase = GenerateInsightsUseCase()

    private val calm = EmotionEntity(id = "e_calm", label = "Calm", emoji = "😌")
    private val sad = EmotionEntity(id = "e_sad", label = "Sad", emoji = "😢")
    private val exercise = ActionEntity(id = "a_exercise", label = "Exercise", emoji = "🏃")

    private object English : InsightStrings {
        override fun keepJournaling() = "Keep journaling."
        override fun periodPhrase(periodDays: Int) = "in $periodDays days"
        override fun summaryDays(days: Int, period: String) = "Journaled $days days $period."
        override fun summaryTopEmotion(emotion: String) = "Mostly $emotion."
        override fun summaryCorrelation(emotion: String, action: String) = "$emotion after $action."
        override fun pattern(emotion: String, count: Int, period: String) = "Felt $emotion $count times $period."
        override fun trendUp(emotion: String, current: Int, prior: Int) = "More $emotion ($current vs $prior)."
        override fun trendDown(emotion: String, current: Int, prior: Int) = "Less $emotion ($current vs $prior)."
        override fun sequence(state: String, outcome: String, count: Int) = "After $state, $outcome next day ($count)."
        override fun correlation(action: String, emotion: String) = "When $action, $emotion."
        override fun timeOfDay(part: DayPart, emotion: String) = "In the ${part.name.lowercase()}, $emotion."
        override fun dayOfWeek(day: DayOfWeek, emotion: String, count: Int) = "On $day, $emotion ($count)."
        override fun keyword(word: String, emotion: String, count: Int) = "Writing $word, $emotion ($count)."
        override fun frequency(days: Int, periodDays: Int) = "$days of $periodDays days."
    }

    private var nextId = 0

    private fun entry(date: LocalDate, hour: Int, emotions: List<EmotionEntity>, actions: List<ActionEntity> = emptyList()) =
        EntryWithTags(
            EntryEntity(id = "e${nextId++}", timestamp = date.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), text = ""),
            emotions, actions
        )

    /** One "exercise + calm" entry per date, then insights as of noon the day after the last one. */
    private fun sequenceInsights(dates: List<LocalDate>) = useCase(
        allEntries = dates.map { entry(it, 9, listOf(calm), listOf(exercise)) },
        strings = English,
        periodDays = 30,
        now = dates.last().plusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC),
        zone = zone
    ).filter { it.type == InsightType.SEQUENCE }.map { it.text }

    @Test
    fun fewerThanThreeEntriesOnlyEncourages() {
        val today = LocalDate.of(2026, 10, 11)
        val result = useCase(
            listOf(entry(today, 9, listOf(calm)), entry(today, 10, listOf(calm))), English,
            now = today.atTime(12, 0).toInstant(ZoneOffset.UTC), zone = zone
        )
        assertEquals(listOf("Keep journaling."), result.map { it.text })
    }

    @Test
    fun sequenceAcrossDay99To100IsFound() {
        // 2026-04-09 is day 99 of the year; a string sort used to put day 100 before day 99
        val start = LocalDate.of(2026, 4, 8)
        val result = sequenceInsights((0L..3L).map { start.plusDays(it) })
        assertEquals(listOf("After exercise, calm next day (3)."), result)
    }

    @Test
    fun sequenceAcrossNewYearIsFound() {
        val start = LocalDate.of(2025, 12, 30)
        val result = sequenceInsights((0L..3L).map { start.plusDays(it) })
        assertEquals(listOf("After exercise, calm next day (3)."), result)
    }

    @Test
    fun daysWithGapsAreNotTreatedAsConsecutive() {
        val start = LocalDate.of(2026, 9, 1)
        val result = sequenceInsights((0L..3L).map { start.plusDays(it * 2) })
        assertTrue(result.isEmpty())
    }

    @Test
    fun risingEmotionIsReportedAgainstThePriorPeriod() {
        val today = LocalDate.of(2026, 10, 11)
        val current = (0L..3L).map { entry(today.minusDays(it), 20, listOf(sad)) }
        val prior = (8L..10L).map { entry(today.minusDays(it), 20, listOf(calm)) } +
            entry(today.minusDays(11), 20, listOf(sad))
        val result = useCase(
            current + prior, English, periodDays = 7,
            now = today.atTime(23, 0).toInstant(ZoneOffset.UTC), zone = zone
        )
        assertTrue(result.any { it.type == InsightType.TREND && it.text == "More sad (4 vs 1)." })
        assertEquals("4 of 7 days.", result.last().text)
    }

    @Test
    fun timeOfDayUsesTheGivenZone() {
        val day = LocalDate.of(2026, 10, 10)
        // 23:00 UTC is already 02:00 the next night in Athens (UTC+3 in October)
        val entries = (0L..2L).map { entry(day.minusDays(it), 23, listOf(calm)) }
        val result = useCase(
            entries, English, periodDays = 7,
            now = day.plusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC), zone = ZoneId.of("Europe/Athens")
        )
        assertTrue(result.any { it.text == "In the night, calm." })
    }
}
