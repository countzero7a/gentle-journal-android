package com.gentlejournal.domain.calendar

import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryWithTags
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The month grid of the timeline's calendar view. */
object CalendarMonth {

    /** Whole weeks starting on [firstDayOfWeek]; days outside the month are null. */
    fun cells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val days: List<LocalDate?> = List<LocalDate?>(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val trailing = (7 - days.size % 7) % 7
        return days + List(trailing) { null }
    }

    /** Each day's most frequent feeling; a tie goes to the one noted first that day. */
    fun mainFeelingByDay(entries: List<EntryWithTags>, zone: ZoneId): Map<LocalDate, EmotionEntity> {
        val result = mutableMapOf<LocalDate, EmotionEntity>()
        entries.groupBy { Instant.ofEpochMilli(it.entry.timestamp).atZone(zone).toLocalDate() }
            .forEach { (day, dayEntries) ->
                val feelings = dayEntries.sortedBy { it.entry.timestamp }.flatMap { it.emotions }
                if (feelings.isEmpty()) return@forEach
                val counts = feelings.groupingBy { it.id }.eachCount()
                val most = counts.values.max()
                result[day] = feelings.first { counts[it.id] == most }
            }
        return result
    }
}
