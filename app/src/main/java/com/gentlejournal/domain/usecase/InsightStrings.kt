package com.gentlejournal.domain.usecase

import java.time.DayOfWeek

enum class DayPart { NIGHT, MORNING, AFTERNOON, EVENING }

/**
 * Sentences used by GenerateInsightsUseCase. The app implements this with string
 * resources in the chosen language; tests use a plain English version.
 * Labels passed in are already resolved for the current language.
 */
interface InsightStrings {
    fun keepJournaling(): String

    /** "this week", "these two weeks", "this month" or "this period" */
    fun periodPhrase(periodDays: Int): String

    fun summaryDays(days: Int, period: String): String
    fun summaryTopEmotion(emotion: String): String
    fun summaryCorrelation(emotion: String, action: String): String

    fun pattern(emotion: String, count: Int, period: String): String
    fun trendUp(emotion: String, current: Int, prior: Int): String
    fun trendDown(emotion: String, current: Int, prior: Int): String
    fun sequence(state: String, outcome: String, count: Int): String
    fun correlation(action: String, emotion: String): String
    fun timeOfDay(part: DayPart, emotion: String): String
    fun dayOfWeek(day: DayOfWeek, emotion: String, count: Int): String
    fun keyword(word: String, emotion: String, count: Int): String
    fun frequency(days: Int, periodDays: Int): String
}
