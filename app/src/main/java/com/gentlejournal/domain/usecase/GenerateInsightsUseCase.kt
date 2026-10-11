package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.model.Insight
import com.gentlejournal.domain.model.InsightType
import com.gentlejournal.domain.model.TrendDirection
import java.util.Calendar

/**
 * On-device insight engine v3.0.
 *
 * v1.0 - Emotion frequency, action-emotion correlation, time-of-day, journaling pace
 * v1.1 - Configurable period (7/14/30), trend direction vs prior period
 * v1.2 - Weekly summary, sub-emotion awareness
 * v2.0 - Sequence detection (day-to-day chains), keyword extraction (TF-IDF)
 * v2.1 - Seasonal awareness (day-of-week, month patterns)
 * v3.0 - ML insight ranking (engagement-weighted), multilingual NLP, label resolver
 */
class GenerateInsightsUseCase {

    operator fun invoke(
        allEntries: List<EntryWithTags>,
        periodDays: Int = 7,
        languageCode: String = "en",
        weights: Map<String, Float> = emptyMap(),
        labelResolver: ((String, String) -> String)? = null
    ): List<Insight> {
        val now = System.currentTimeMillis()
        val periodMs = periodDays.toLong() * 24 * 60 * 60 * 1000
        val current = allEntries.filter { it.entry.timestamp >= now - periodMs }
        val prior = allEntries.filter {
            it.entry.timestamp >= now - 2 * periodMs && it.entry.timestamp < now - periodMs
        }
        val r: (String, String) -> String = labelResolver ?: { _, label -> label }

        if (current.size < 3) return listOf(
            Insight(InsightType.INFO, "Keep journaling — patterns emerge with a few more entries.", "🌱", periodDays = periodDays)
        )

        val insights = mutableListOf<Insight>()

        // v1.2: Summary
        generateSummary(current, periodDays, r)?.let { insights.add(it) }

        // v1.0: Emotion frequency
        emotionFrequency(current, periodDays, r)?.let { insights.add(it) }

        // v1.1: Trends
        insights.addAll(emotionTrends(current, prior, periodDays, r))

        // v2.0: Sequence detection
        insights.addAll(sequenceDetection(current, periodDays, r))

        // v1.0: Correlations
        insights.addAll(actionEmotionCorrelations(current, periodDays, r))

        // v1.0: Time-of-day
        insights.addAll(timeOfDayPatterns(current, periodDays, r))

        // v2.1: Seasonal (day-of-week, month)
        insights.addAll(seasonalPatterns(allEntries, periodDays, r))

        // v2.0: Keywords (now multilingual)
        insights.addAll(keywordInsights(current, periodDays, languageCode, r))

        // v1.0: Frequency
        insights.add(journalingFrequency(current, periodDays))

        // v3.0: ML ranking - sort middle insights by engagement weight
        if (weights.isNotEmpty()) {
            val summaries = insights.filter { it.type == InsightType.SUMMARY }
            val infos = insights.filter { it.type == InsightType.INFO }
            val middle = insights.filter { it.type != InsightType.SUMMARY && it.type != InsightType.INFO }
                .sortedByDescending { weights[it.type.name] ?: 1.0f }
            return (summaries + middle + infos).take(10)
        }

        return insights.take(10)
    }

    // ── v1.2: Summary ─────────────────────────────────────────────────

    private fun generateSummary(entries: List<EntryWithTags>, periodDays: Int, r: (String, String) -> String): Insight? {
        if (entries.size < 3) return null
        val periodLabel = when (periodDays) { 7 -> "this week"; 14 -> "these two weeks"; 30 -> "this month"; else -> "this period" }
        val parts = mutableListOf<String>()

        val uniqueDays = entries.map { e ->
            val cal = Calendar.getInstance().apply { timeInMillis = e.entry.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()
        parts.add("You journaled on ${uniqueDays.size} days $periodLabel.")

        val emotionCounts = mutableMapOf<String, Int>()
        entries.forEach { e -> e.emotions.forEach { emo ->
            val label = r(emo.id, emo.label)
            emotionCounts[label] = (emotionCounts[label] ?: 0) + 1
        } }
        val topEmotion = emotionCounts.maxByOrNull { it.value }
        if (topEmotion != null && topEmotion.value >= 2) {
            parts.add("You felt '${topEmotion.key.lowercase()}' most often.")
        }

        val bestCorr = findStrongestCorrelation(entries, r)
        if (bestCorr != null) parts.add("You often felt ${bestCorr.second.lowercase()} after ${bestCorr.first.lowercase()}.")

        return if (parts.size >= 2) Insight(InsightType.SUMMARY, parts.joinToString(" "), "📝", periodDays = periodDays) else null
    }

    private fun findStrongestCorrelation(entries: List<EntryWithTags>, r: (String, String) -> String): Pair<String, String>? {
        val map = mutableMapOf<String, MutableMap<String, Int>>()
        entries.forEach { e ->
            e.actions.forEach { act ->
                val al = r(act.id, act.label)
                val m = map.getOrPut(al) { mutableMapOf() }
                e.emotions.forEach { emo -> val el = r(emo.id, emo.label); m[el] = (m[el] ?: 0) + 1 }
            }
        }
        var ba: String? = null; var be: String? = null; var bc = 1
        map.forEach { (a, emos) -> val t = emos.maxByOrNull { it.value }; if (t != null && t.value > bc) { bc = t.value; ba = a; be = t.key } }
        return if (ba != null && be != null) Pair(ba!!, be!!) else null
    }

    // ── v1.0: Emotion frequency ───────────────────────────────────────

    private fun emotionFrequency(entries: List<EntryWithTags>, periodDays: Int, r: (String, String) -> String): Insight? {
        val counts = mutableMapOf<String, Int>()
        entries.forEach { e -> e.emotions.forEach { emo ->
            val label = r(emo.id, emo.label)
            counts[label] = (counts[label] ?: 0) + 1
        } }
        val top = counts.maxByOrNull { it.value } ?: return null
        if (top.value < 2) return null
        val label = when (periodDays) { 7 -> "this week"; 14 -> "these two weeks"; 30 -> "this month"; else -> "this period" }
        return Insight(InsightType.PATTERN, "You've felt '${top.key}' ${top.value} times $label.", "🔁", periodDays = periodDays)
    }

    // ── v1.1: Trend direction ─────────────────────────────────────────

    private fun emotionTrends(current: List<EntryWithTags>, prior: List<EntryWithTags>, periodDays: Int, r: (String, String) -> String): List<Insight> {
        if (prior.size < 3) return emptyList()
        val cc = mutableMapOf<String, Int>(); current.forEach { e -> e.emotions.forEach { emo -> val l = r(emo.id, emo.label); cc[l] = (cc[l] ?: 0) + 1 } }
        val pc = mutableMapOf<String, Int>(); prior.forEach { e -> e.emotions.forEach { emo -> val l = r(emo.id, emo.label); pc[l] = (pc[l] ?: 0) + 1 } }
        return (cc.keys + pc.keys).mapNotNull { emotion ->
            val c = cc[emotion] ?: 0; val p = pc[emotion] ?: 0
            if (c < 2 && p < 2) return@mapNotNull null
            val diff = c - p; if (kotlin.math.abs(diff) < 2) return@mapNotNull null
            if (diff >= 2) Insight(InsightType.TREND, "You've felt '${emotion.lowercase()}' more often this period ($c vs $p).", "⬆️", trend = TrendDirection.RISING, periodDays = periodDays)
            else Insight(InsightType.TREND, "You've felt '${emotion.lowercase()}' less often this period ($c vs $p).", "⬇️", trend = TrendDirection.FALLING, periodDays = periodDays)
        }.take(2)
    }

    // ── v2.0: Sequence detection ──────────────────────────────────────

    private fun sequenceDetection(entries: List<EntryWithTags>, periodDays: Int, r: (String, String) -> String): List<Insight> {
        if (entries.size < 4) return emptyList()

        val dayMap = mutableMapOf<String, MutableList<EntryWithTags>>()
        entries.forEach { e ->
            val cal = Calendar.getInstance().apply { timeInMillis = e.entry.timestamp }
            val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            dayMap.getOrPut(key) { mutableListOf() }.add(e)
        }

        val sortedDays = dayMap.keys.sorted()
        if (sortedDays.size < 2) return emptyList()

        val transitions = mutableMapOf<String, MutableMap<String, Int>>()

        for (i in 0 until sortedDays.size - 1) {
            val day1Entries = dayMap[sortedDays[i]] ?: continue
            val day2Entries = dayMap[sortedDays[i + 1]] ?: continue

            val day1States = mutableSetOf<String>()
            day1Entries.forEach { e ->
                e.emotions.forEach { day1States.add(r(it.id, it.label).lowercase()) }
                e.actions.forEach { day1States.add(r(it.id, it.label).lowercase()) }
            }

            val day2Emotions = mutableSetOf<String>()
            day2Entries.forEach { e -> e.emotions.forEach { day2Emotions.add(r(it.id, it.label).lowercase()) } }

            day1States.forEach { state ->
                val map = transitions.getOrPut(state) { mutableMapOf() }
                day2Emotions.forEach { outcome -> map[outcome] = (map[outcome] ?: 0) + 1 }
            }
        }

        return transitions.mapNotNull { (state, outcomes) ->
            val top = outcomes.maxByOrNull { it.value } ?: return@mapNotNull null
            if (top.value < 3 || state == top.key) return@mapNotNull null
            Insight(InsightType.SEQUENCE, "After days with '${state}', you often feel '${top.key}' the next day (${top.value} times).", "➡️", periodDays = periodDays)
        }.take(2)
    }

    // ── v1.0: Action-emotion correlation ──────────────────────────────

    private fun actionEmotionCorrelations(entries: List<EntryWithTags>, periodDays: Int, r: (String, String) -> String): List<Insight> {
        val map = mutableMapOf<String, MutableMap<String, Int>>()
        entries.forEach { e ->
            e.actions.forEach { act ->
                val al = r(act.id, act.label)
                val m = map.getOrPut(al) { mutableMapOf() }
                e.emotions.forEach { emo -> val el = r(emo.id, emo.label); m[el] = (m[el] ?: 0) + 1 }
            }
        }
        return map.mapNotNull { (action, emos) ->
            val top = emos.maxByOrNull { it.value } ?: return@mapNotNull null
            if (top.value < 2) return@mapNotNull null
            Insight(InsightType.CORRELATION, "When you '${action.lowercase()}', you often feel '${top.key.lowercase()}'.", "🔗", periodDays = periodDays)
        }
    }

    // ── v1.0: Time-of-day ─────────────────────────────────────────────

    private fun timeOfDayPatterns(entries: List<EntryWithTags>, periodDays: Int, r: (String, String) -> String): List<Insight> {
        val buckets = mutableMapOf<String, MutableList<String>>()
        entries.forEach { e ->
            val h = Calendar.getInstance().apply { timeInMillis = e.entry.timestamp }.get(Calendar.HOUR_OF_DAY)
            val p = when { h < 6 -> "night"; h < 12 -> "morning"; h < 18 -> "afternoon"; else -> "evening" }
            e.emotions.forEach { emo -> buckets.getOrPut(p) { mutableListOf() }.add(r(emo.id, emo.label)) }
        }
        return buckets.mapNotNull { (period, emos) ->
            if (emos.size < 2) return@mapNotNull null
            val freq = emos.groupingBy { it }.eachCount()
            val top = freq.maxByOrNull { it.value } ?: return@mapNotNull null
            if (top.value < 2) return@mapNotNull null
            Insight(InsightType.TIME_BASED, "In the $period, you tend to feel '${top.key.lowercase()}'.", "🕐", periodDays = periodDays)
        }
    }

    // ── v2.1: Seasonal patterns ───────────────────────────────────────

    private fun seasonalPatterns(allEntries: List<EntryWithTags>, periodDays: Int, r: (String, String) -> String): List<Insight> {
        if (allEntries.size < 10) return emptyList()

        val insights = mutableListOf<Insight>()

        val dowEmotions = mutableMapOf<Int, MutableList<String>>()
        allEntries.forEach { e ->
            val dow = Calendar.getInstance().apply { timeInMillis = e.entry.timestamp }.get(Calendar.DAY_OF_WEEK)
            e.emotions.forEach { emo -> dowEmotions.getOrPut(dow) { mutableListOf() }.add(r(emo.id, emo.label)) }
        }

        val dayNames = mapOf(
            Calendar.MONDAY to "Mondays", Calendar.TUESDAY to "Tuesdays",
            Calendar.WEDNESDAY to "Wednesdays", Calendar.THURSDAY to "Thursdays",
            Calendar.FRIDAY to "Fridays", Calendar.SATURDAY to "Saturdays",
            Calendar.SUNDAY to "Sundays"
        )

        dowEmotions.forEach { (dow, emos) ->
            if (emos.size < 5) return@forEach
            val freq = emos.groupingBy { it }.eachCount()
            val top = freq.maxByOrNull { it.value } ?: return@forEach
            val ratio = top.value.toFloat() / emos.size
            if (ratio > 0.5f && top.value >= 3) {
                val dayName = dayNames[dow] ?: return@forEach
                insights.add(Insight(InsightType.SEASONAL, "On $dayName, you often feel '${top.key.lowercase()}' (${top.value} times).", "📅", periodDays = periodDays))
            }
        }

        return insights.take(2)
    }

    // ── v2.0/v3.0: Keyword extraction (TF-IDF, multilingual) ──────────

    private fun keywordInsights(entries: List<EntryWithTags>, periodDays: Int, languageCode: String, r: (String, String) -> String): List<Insight> {
        val textEntries = entries.filter { it.entry.text.isNotBlank() && it.emotions.isNotEmpty() }
        if (textEntries.size < 5) return emptyList()

        val sw = StopWords.forLanguage(languageCode)

        val emotionTexts = mutableMapOf<String, MutableList<String>>()
        textEntries.forEach { e ->
            e.emotions.forEach { emo ->
                emotionTexts.getOrPut(r(emo.id, emo.label)) { mutableListOf() }.add(e.entry.text)
            }
        }

        val allWords = mutableMapOf<String, Int>()
        textEntries.forEach { e ->
            tokenize(e.entry.text, sw).toSet().forEach { word ->
                allWords[word] = (allWords[word] ?: 0) + 1
            }
        }
        val totalDocs = textEntries.size.toFloat()
        val insights = mutableListOf<Insight>()

        emotionTexts.forEach { (emotion, texts) ->
            if (texts.size < 3) return@forEach

            val emotionWords = mutableMapOf<String, Int>()
            texts.forEach { text ->
                tokenize(text, sw).toSet().forEach { word ->
                    emotionWords[word] = (emotionWords[word] ?: 0) + 1
                }
            }

            val scored = emotionWords.mapNotNull { (word, count) ->
                val globalCount = allWords[word] ?: 1
                if (count < 2) return@mapNotNull null
                val tf = count.toFloat() / texts.size
                val idf = kotlin.math.ln(totalDocs / globalCount)
                if (idf < 0.3f) return@mapNotNull null
                Pair(word, tf * idf)
            }.sortedByDescending { it.second }

            val topKeyword = scored.firstOrNull()
            if (topKeyword != null && topKeyword.second > 0.2f) {
                val wordCount = emotionWords[topKeyword.first] ?: 0
                insights.add(Insight(InsightType.KEYWORD, "When you write about '${topKeyword.first}', you often feel '${emotion.lowercase()}' ($wordCount entries).", "🔍", periodDays = periodDays))
            }
        }

        return insights.take(2)
    }

    private fun tokenize(text: String, stopWords: Set<String>): List<String> {
        return text.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in stopWords }
    }

    // ── v1.0: Journaling frequency ────────────────────────────────────

    private fun journalingFrequency(entries: List<EntryWithTags>, periodDays: Int): Insight {
        val uniqueDays = entries.map { e ->
            val cal = Calendar.getInstance().apply { timeInMillis = e.entry.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()
        return Insight(InsightType.INFO, "You journaled on ${uniqueDays.size} of the last $periodDays days. No pressure — any pace is fine.", "📝", periodDays = periodDays)
    }
}
