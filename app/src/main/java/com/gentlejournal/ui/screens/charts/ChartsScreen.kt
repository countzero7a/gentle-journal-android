package com.gentlejournal.ui.screens.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.ui.components.JournalCard
import com.gentlejournal.ui.theme.LocalJournalColors
import com.gentlejournal.ui.util.resolveEmotionLabel
import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChartsScreen(
    entries: List<EntryWithTags>,
    modifier: Modifier = Modifier
) {
    val colors = LocalJournalColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales.get(0)
    val zone = ZoneId.systemDefault()
    val dayLabelFormat = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale) }
    fun dayOf(timestamp: Long): LocalDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.charts_empty),
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted)
            )
        }
        return
    }

    // Compute emotion frequencies using resolved labels
    val emotionCounts = mutableMapOf<String, Pair<String, Int>>() // resolved_label -> (emoji, count)
    entries.forEach { e ->
        e.emotions.forEach { emo ->
            val label = resolveEmotionLabel(context, emo.id, emo.label)
            val current = emotionCounts[label]
            emotionCounts[label] = Pair(emo.emoji, (current?.second ?: 0) + 1)
        }
    }
    val sortedEmotions = emotionCounts.entries.sortedByDescending { it.value.second }
    val maxCount = sortedEmotions.firstOrNull()?.value?.second ?: 1

    // Daily mood timeline (last 30 days)
    val last30 = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    val recentEntries = entries.filter { it.entry.timestamp >= last30 }
    // Keyed by date so the order stays right across the new year
    val dailyMoods = mutableMapOf<LocalDate, MutableList<String>>()
    recentEntries.forEach { e ->
        val day = dayOf(e.entry.timestamp)
        e.emotions.forEach { emo ->
            dailyMoods.getOrPut(day) { mutableListOf() }.add(emo.emoji)
        }
    }
    val sortedDays = dailyMoods.keys.sorted()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.charts_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ── Emotion frequency chart ─────────────────────────────────
        JournalCard {
            Text(
                text = stringResource(R.string.charts_frequency),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            sortedEmotions.take(8).forEach { (label, emoData) ->
                val (emoji, count) = emoData
                val fraction = count.toFloat() / maxCount

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(text = emoji, fontSize = 18.sp, modifier = Modifier.width(30.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.Accent.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                color = colors.Accent,
                                size = Size(size.width * fraction, size.height)
                            )
                        }
                    }
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .width(30.dp)
                            .padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Daily mood timeline ─────────────────────────────────────
        if (sortedDays.isNotEmpty()) {
            JournalCard {
                Text(
                    text = stringResource(R.string.charts_timeline),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                val displayDays = sortedDays.takeLast(14)
                displayDays.forEach { day ->
                    val moods = dailyMoods[day] ?: emptyList()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Text(
                            text = day.format(dayLabelFormat),
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted),
                            modifier = Modifier.width(60.dp)
                        )
                        moods.take(6).forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        if (moods.size > 6) {
                            Text(
                                text = "+${moods.size - 6}",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Stats summary ───────────────────────────────────────────
        JournalCard {
            Text(
                text = stringResource(R.string.charts_stats),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val totalEntries = entries.size
            val totalEmotions = emotionCounts.values.sumOf { it.second }
            val uniqueEmotions = emotionCounts.size
            val daysWithEntries = entries.map { dayOf(it.entry.timestamp) }.toSet().size

            StatRow(stringResource(R.string.charts_total_entries), "$totalEntries")
            StatRow(stringResource(R.string.charts_days_journaled), "$daysWithEntries")
            StatRow(stringResource(R.string.charts_unique_emotions), "$uniqueEmotions")
            StatRow(stringResource(R.string.charts_total_tags), "$totalEmotions")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val colors = LocalJournalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
