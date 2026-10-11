package com.gentlejournal.ui.util

import android.content.Context
import com.gentlejournal.R
import com.gentlejournal.domain.usecase.DayPart
import com.gentlejournal.domain.usecase.InsightStrings
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Insight sentences from string resources. [context] must already be in the app language. */
class ResourceInsightStrings(context: Context, private val locale: Locale) : InsightStrings {

    private val res = context.resources

    override fun keepJournaling(): String = res.getString(R.string.insight_keep_journaling)

    override fun periodPhrase(periodDays: Int): String = res.getString(
        when (periodDays) {
            7 -> R.string.insight_period_week
            14 -> R.string.insight_period_two_weeks
            30 -> R.string.insight_period_month
            else -> R.string.insight_period_other
        }
    )

    override fun summaryDays(days: Int, period: String): String =
        res.getQuantityString(R.plurals.insight_summary_days, days, days, period)

    override fun summaryTopEmotion(emotion: String): String =
        res.getString(R.string.insight_summary_top_emotion, emotion)

    override fun summaryCorrelation(emotion: String, action: String): String =
        res.getString(R.string.insight_summary_correlation, emotion, action)

    override fun pattern(emotion: String, count: Int, period: String): String =
        res.getQuantityString(R.plurals.insight_pattern, count, emotion, count, period)

    override fun trendUp(emotion: String, current: Int, prior: Int): String =
        res.getString(R.string.insight_trend_up, emotion, current, prior)

    override fun trendDown(emotion: String, current: Int, prior: Int): String =
        res.getString(R.string.insight_trend_down, emotion, current, prior)

    override fun sequence(state: String, outcome: String, count: Int): String =
        res.getQuantityString(R.plurals.insight_sequence, count, state, outcome, count)

    override fun correlation(action: String, emotion: String): String =
        res.getString(R.string.insight_correlation, action, emotion)

    override fun timeOfDay(part: DayPart, emotion: String): String {
        val partName = res.getString(
            when (part) {
                DayPart.NIGHT -> R.string.time_night
                DayPart.MORNING -> R.string.time_morning
                DayPart.AFTERNOON -> R.string.time_afternoon
                DayPart.EVENING -> R.string.time_evening
            }
        )
        return res.getString(R.string.insight_time_of_day, partName, emotion)
    }

    override fun dayOfWeek(day: DayOfWeek, emotion: String, count: Int): String {
        val dayName = day.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
        return res.getQuantityString(R.plurals.insight_day_of_week, count, dayName, emotion, count)
    }

    override fun keyword(word: String, emotion: String, count: Int): String =
        res.getQuantityString(R.plurals.insight_keyword, count, word, emotion, count)

    override fun frequency(days: Int, periodDays: Int): String =
        res.getQuantityString(R.plurals.insight_frequency, periodDays, days, periodDays)
}
