package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.model.Insight
import com.gentlejournal.domain.model.InsightType
import com.gentlejournal.domain.model.TrendDirection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

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
        strings: InsightStrings,
        periodDays: Int = 7,
        languageCode: String = "en",
        weights: Map<String, Float> = emptyMap(),
        labelResolver: ((String, String) -> String)? = null,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Insight> {
        val nowMs = now.toEpochMilli()
        val periodMs = periodDays.toLong() * 24 * 60 * 60 * 1000
        val current = allEntries.filter { it.entry.timestamp >= nowMs - periodMs }
        val prior = allEntries.filter {
            it.entry.timestamp >= nowMs - 2 * periodMs && it.entry.timestamp < nowMs - periodMs
        }
        val r: (String, String) -> String = labelResolver ?: { _, label -> label }
        val ctx = Ctx(strings, periodDays, zone, r)

        if (current.size < 3) return listOf(
            Insight(InsightType.INFO, strings.keepJournaling(), "🌱", periodDays = periodDays)
        )

        val insights = mutableListOf<Insight>()

        // v1.2: Summary
        generateSummary(current, ctx)?.let { insights.add(it) }

        // v1.0: Emotion frequency
        emotionFrequency(current, ctx)?.let { insights.add(it) }

        // v1.1: Trends
        insights.addAll(emotionTrends(current, prior, ctx))

        // v2.0: Sequence detection
        insights.addAll(sequenceDetection(current, ctx))

        // v1.0: Correlations
        insights.addAll(actionEmotionCorrelations(current, ctx))

        // v1.0: Time-of-day
        insights.addAll(timeOfDayPatterns(current, ctx))

        // v2.1: Seasonal (day-of-week)
        insights.addAll(seasonalPatterns(allEntries, ctx))

        // v2.0: Keywords (now multilingual)
        insights.addAll(keywordInsights(current, languageCode, ctx))

        // v1.0: Frequency
        insights.add(journalingFrequency(current, ctx))

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

    private class Ctx(
        val strings: InsightStrings,
        val periodDays: Int,
        val zone: ZoneId,
        val r: (String, String) -> String
    ) {
        val period: String get() = strings.periodPhrase(periodDays)
        fun dateTime(e: EntryWithTags): ZonedDateTime = Instant.ofEpochMilli(e.entry.timestamp).atZone(zone)
        fun date(e: EntryWithTags): LocalDate = dateTime(e).toLocalDate()
    }

    // ── v1.2: Summary ─────────────────────────────────────────────────

    private fun generateSummary(entries: List<EntryWithTags>, c: Ctx): Insight? {
        if (entries.size < 3) return null
        val parts = mutableListOf<String>()

        val uniqueDays = entries.map { c.date(it) }.toSet()
        parts.add(c.strings.summaryDays(uniqueDays.size, c.period))

        val emotionCounts = mutableMapOf<String, Int>()
        entries.forEach { e -> e.emotions.forEach { emo ->
            val label = c.r(emo.id, emo.label)
            emotionCounts[label] = (emotionCounts[label] ?: 0) + 1
        } }
        val topEmotion = emotionCounts.maxByOrNull { it.value }
        if (topEmotion != null && topEmotion.value >= 2) {
            parts.add(c.strings.summaryTopEmotion(topEmotion.key.lowercase()))
        }

        val bestCorr = findStrongestCorrelation(entries, c)
        if (bestCorr != null) parts.add(c.strings.summaryCorrelation(bestCorr.second.lowercase(), bestCorr.first.lowercase()))

        return if (parts.size >= 2) Insight(InsightType.SUMMARY, parts.joinToString(" "), "📝", periodDays = c.periodDays) else null
    }

    private fun findStrongestCorrelation(entries: List<EntryWithTags>, c: Ctx): Pair<String, String>? {
        val map = mutableMapOf<String, MutableMap<String, Int>>()
        entries.forEach { e ->
            e.actions.forEach { act ->
                val al = c.r(act.id, act.label)
                val m = map.getOrPut(al) { mutableMapOf() }
                e.emotions.forEach { emo -> val el = c.r(emo.id, emo.label); m[el] = (m[el] ?: 0) + 1 }
            }
        }
        var ba: String? = null; var be: String? = null; var bc = 1
        map.forEach { (a, emos) -> val t = emos.maxByOrNull { it.value }; if (t != null && t.value > bc) { bc = t.value; ba = a; be = t.key } }
        return if (ba != null && be != null) Pair(ba!!, be!!) else null
    }

    // ── v1.0: Emotion frequency ───────────────────────────────────────

    private fun emotionFrequency(entries: List<EntryWithTags>, c: Ctx): Insight? {
        val counts = mutableMapOf<String, Int>()
        entries.forEach { e -> e.emotions.forEach { emo ->
            val label = c.r(emo.id, emo.label)
            counts[label] = (counts[label] ?: 0) + 1
        } }
        val top = counts.maxByOrNull { it.value } ?: return null
        if (top.value < 2) return null
        return Insight(InsightType.PATTERN, c.strings.pattern(top.key, top.value, c.period), "🔁", periodDays = c.periodDays)
    }

    // ── v1.1: Trend direction ─────────────────────────────────────────

    private fun emotionTrends(current: List<EntryWithTags>, prior: List<EntryWithTags>, c: Ctx): List<Insight> {
        if (prior.size < 3) return emptyList()
        val cc = mutableMapOf<String, Int>(); current.forEach { e -> e.emotions.forEach { emo -> val l = c.r(emo.id, emo.label); cc[l] = (cc[l] ?: 0) + 1 } }
        val pc = mutableMapOf<String, Int>(); prior.forEach { e -> e.emotions.forEach { emo -> val l = c.r(emo.id, emo.label); pc[l] = (pc[l] ?: 0) + 1 } }
        return (cc.keys + pc.keys).mapNotNull { emotion ->
            val cur = cc[emotion] ?: 0; val p = pc[emotion] ?: 0
            if (cur < 2 && p < 2) return@mapNotNull null
            val diff = cur - p; if (kotlin.math.abs(diff) < 2) return@mapNotNull null
            if (diff >= 2) Insight(InsightType.TREND, c.strings.trendUp(emotion.lowercase(), cur, p), "⬆️", trend = TrendDirection.RISING, periodDays = c.periodDays)
            else Insight(InsightType.TREND, c.strings.trendDown(emotion.lowercase(), cur, p), "⬇️", trend = TrendDirection.FALLING, periodDays = c.periodDays)
        }.take(2)
    }

    // ── v2.0: Sequence detection ──────────────────────────────────────

    private fun sequenceDetection(entries: List<EntryWithTags>, c: Ctx): List<Insight> {
        if (entries.size < 4) return emptyList()

        val dayMap = entries.groupBy { c.date(it) }
        val sortedDays = dayMap.keys.sorted()
        if (sortedDays.size < 2) return emptyList()

        val transitions = mutableMapOf<String, MutableMap<String, Int>>()

        for (i in 0 until sortedDays.size - 1) {
            val day1 = sortedDays[i]
            val day2 = sortedDays[i + 1]
            // Only days that really follow each other count as "the next day"
            if (day2 != day1.plusDays(1)) continue
            val day1Entries = dayMap[day1] ?: continue
            val day2Entries = dayMap[day2] ?: continue

            val day1States = mutableSetOf<String>()
            day1Entries.forEach { e ->
                e.emotions.forEach { day1States.add(c.r(it.id, it.label).lowercase()) }
                e.actions.forEach { day1States.add(c.r(it.id, it.label).lowercase()) }
            }

            val day2Emotions = mutableSetOf<String>()
            day2Entries.forEach { e -> e.emotions.forEach { day2Emotions.add(c.r(it.id, it.label).lowercase()) } }

            day1States.forEach { state ->
                val map = transitions.getOrPut(state) { mutableMapOf() }
                day2Emotions.forEach { outcome -> map[outcome] = (map[outcome] ?: 0) + 1 }
            }
        }

        return transitions.mapNotNull { (state, outcomes) ->
            val top = outcomes.maxByOrNull { it.value } ?: return@mapNotNull null
            if (top.value < 3 || state == top.key) return@mapNotNull null
            Insight(InsightType.SEQUENCE, c.strings.sequence(state, top.key, top.value), "➡️", periodDays = c.periodDays)
        }.take(2)
    }

    // ── v1.0: Action-emotion correlation ──────────────────────────────

    private fun actionEmotionCorrelations(entries: List<EntryWithTags>, c: Ctx): List<Insight> {
        val map = mutableMapOf<String, MutableMap<String, Int>>()
        entries.forEach { e ->
            e.actions.forEach { act ->
                val al = c.r(act.id, act.label)
                val m = map.getOrPut(al) { mutableMapOf() }
                e.emotions.forEach { emo -> val el = c.r(emo.id, emo.label); m[el] = (m[el] ?: 0) + 1 }
            }
        }
        return map.mapNotNull { (action, emos) ->
            val top = emos.maxByOrNull { it.value } ?: return@mapNotNull null
            if (top.value < 2) return@mapNotNull null
            Insight(InsightType.CORRELATION, c.strings.correlation(action.lowercase(), top.key.lowercase()), "🔗", periodDays = c.periodDays)
        }
    }

    // ── v1.0: Time-of-day ─────────────────────────────────────────────

    private fun timeOfDayPatterns(entries: List<EntryWithTags>, c: Ctx): List<Insight> {
        val buckets = mutableMapOf<DayPart, MutableList<String>>()
        entries.forEach { e ->
            val h = c.dateTime(e).hour
            val p = when { h < 6 -> DayPart.NIGHT; h < 12 -> DayPart.MORNING; h < 18 -> DayPart.AFTERNOON; else -> DayPart.EVENING }
            e.emotions.forEach { emo -> buckets.getOrPut(p) { mutableListOf() }.add(c.r(emo.id, emo.label)) }
        }
        return buckets.mapNotNull { (part, emos) ->
            if (emos.size < 2) return@mapNotNull null
            val freq = emos.groupingBy { it }.eachCount()
            val top = freq.maxByOrNull { it.value } ?: return@mapNotNull null
            if (top.value < 2) return@mapNotNull null
            Insight(InsightType.TIME_BASED, c.strings.timeOfDay(part, top.key.lowercase()), "🕐", periodDays = c.periodDays)
        }
    }

    // ── v2.1: Seasonal patterns ───────────────────────────────────────

    private fun seasonalPatterns(allEntries: List<EntryWithTags>, c: Ctx): List<Insight> {
        if (allEntries.size < 10) return emptyList()

        val dowEmotions = allEntries.groupBy { c.dateTime(it).dayOfWeek }
            .mapValues { (_, dayEntries) -> dayEntries.flatMap { e -> e.emotions.map { c.r(it.id, it.label) } } }

        return dowEmotions.entries.sortedBy { it.key }.mapNotNull { (dow, emos) ->
            if (emos.size < 5) return@mapNotNull null
            val freq = emos.groupingBy { it }.eachCount()
            val top = freq.maxByOrNull { it.value } ?: return@mapNotNull null
            val ratio = top.value.toFloat() / emos.size
            if (ratio > 0.5f && top.value >= 3) {
                Insight(InsightType.SEASONAL, c.strings.dayOfWeek(dow, top.key.lowercase(), top.value), "📅", periodDays = c.periodDays)
            } else null
        }.take(2)
    }

    // ── v2.0/v3.0: Keyword extraction (TF-IDF, multilingual) ──────────

    private fun keywordInsights(entries: List<EntryWithTags>, languageCode: String, c: Ctx): List<Insight> {
        val textEntries = entries.filter { it.entry.text.isNotBlank() && it.emotions.isNotEmpty() }
        if (textEntries.size < 5) return emptyList()

        val sw = StopWords.forLanguage(languageCode)

        val emotionTexts = mutableMapOf<String, MutableList<String>>()
        textEntries.forEach { e ->
            e.emotions.forEach { emo ->
                emotionTexts.getOrPut(c.r(emo.id, emo.label)) { mutableListOf() }.add(e.entry.text)
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
                insights.add(Insight(InsightType.KEYWORD, c.strings.keyword(topKeyword.first, emotion.lowercase(), wordCount), "🔍", periodDays = c.periodDays))
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

    private fun journalingFrequency(entries: List<EntryWithTags>, c: Ctx): Insight {
        val uniqueDays = entries.map { c.date(it) }.toSet()
        return Insight(InsightType.INFO, c.strings.frequency(uniqueDays.size, c.periodDays), "📝", periodDays = c.periodDays)
    }
}
