package com.gentlejournal.ui.screens.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.entity.InsightEntity
import com.gentlejournal.domain.model.Insight
import com.gentlejournal.domain.model.InsightType
import com.gentlejournal.domain.model.TrendDirection
import com.gentlejournal.ui.components.EmptyState
import com.gentlejournal.ui.components.JournalCard
import com.gentlejournal.ui.theme.LocalJournalColors
import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InsightsScreen(
    insights: List<Insight>,
    savedInsights: List<InsightEntity>,
    currentPeriod: Int,
    onPeriodChange: (Int) -> Unit,
    onInsightTap: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalJournalColors.current
    val locale = LocalConfiguration.current.locales.get(0)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val headerFormat = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale) }

    // Past insights for the selected period, each text once (newest first), excluding the current ones
    val currentTexts = insights.map { it.text }.toSet()
    val pastInsights = savedInsights
        .filter { it.periodDays == currentPeriod && it.text !in currentTexts }
        .distinctBy { it.text }
        .groupBy { Instant.ofEpochMilli(it.generatedAt).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending { it })

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Period toggle
        item(key = "period_toggle") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 30).forEach { days ->
                    val isSelected = days == currentPeriod
                    val label = when (days) {
                        7 -> stringResource(R.string.insights_7days)
                        14 -> stringResource(R.string.insights_14days)
                        30 -> stringResource(R.string.insights_30days)
                        else -> "$days"
                    }
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onPeriodChange(days) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.AccentLight else colors.SurfaceAlt,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.Accent else colors.Border)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.AccentDark else colors.TextMuted
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }

        // Header
        item(key = "header") {
            Text(
                text = stringResource(R.string.insights_header),
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted, lineHeight = 22.sp),
                modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
            )
        }

        // Current insights
        if (insights.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    emoji = "\uD83D\uDD0D",
                    title = stringResource(R.string.insights_empty_title),
                    message = stringResource(R.string.insights_empty_message)
                )
            }
        } else {
            items(insights, key = { "${it.type}_${it.text.hashCode()}" }) { insight ->
                InsightCard(
                    insight = insight,
                    onClick = { onInsightTap(insight.type.name) }
                )
            }
        }

        // Privacy note
        item(key = "privacy") {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.SurfaceAlt,
                border = BorderStroke(1.dp, colors.Border)
            ) {
                Text(
                    text = "\uD83D\uDD12 " + stringResource(R.string.insights_privacy),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Past insights history
        if (pastInsights.isNotEmpty()) {
            item(key = "past_header") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.insights_past),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            pastInsights.forEach { (date, dayInsights) ->
                item(key = "past_date_$date") {
                    val displayDate = when (date) {
                        today -> stringResource(R.string.timeline_today)
                        today.minusDays(1) -> stringResource(R.string.timeline_yesterday)
                        else -> date.format(headerFormat)
                    }
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.TextMuted
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(dayInsights.take(5), key = { "past_${it.id}" }) { entity ->
                    PastInsightCard(entity = entity)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun InsightCard(insight: Insight, onClick: () -> Unit = {}) {
    val colors = LocalJournalColors.current
    val borderColor = when (insight.type) {
        InsightType.SUMMARY -> colors.Accent
        InsightType.CORRELATION -> colors.Accent
        InsightType.SEQUENCE -> colors.Accent
        InsightType.KEYWORD -> colors.Accent
        InsightType.TREND -> when (insight.trend) {
            TrendDirection.RISING -> colors.Warm
            TrendDirection.FALLING -> colors.Accent
            else -> colors.Border
        }
        InsightType.PATTERN -> colors.Warm
        InsightType.TIME_BASED -> colors.Warm
        InsightType.SEASONAL -> colors.Warm
        InsightType.INFO -> colors.Border
    }
    val typeLabel = when (insight.type) {
        InsightType.SUMMARY -> stringResource(R.string.insights_summary)
        InsightType.CORRELATION -> stringResource(R.string.insights_correlation)
        InsightType.TREND -> stringResource(R.string.insights_trend)
        InsightType.PATTERN -> stringResource(R.string.insights_pattern)
        InsightType.TIME_BASED -> stringResource(R.string.insights_time)
        InsightType.SEQUENCE -> stringResource(R.string.insights_sequence)
        InsightType.SEASONAL -> stringResource(R.string.insights_seasonal)
        InsightType.KEYWORD -> stringResource(R.string.insights_keyword)
        InsightType.INFO -> stringResource(R.string.insights_observation)
    }
    JournalCard(borderStart = borderColor, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = insight.icon, fontSize = 24.sp)
            Column {
                Text(text = insight.text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = typeLabel, style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic))
            }
        }
    }
}

@Composable
private fun PastInsightCard(entity: InsightEntity) {
    val colors = LocalJournalColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.SurfaceAlt,
        border = BorderStroke(1.dp, colors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = entity.icon, fontSize = 18.sp)
            Column {
                Text(
                    text = entity.text,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, color = colors.TextMuted)
                )
                Text(
                    text = entity.type.lowercase().replace("_", " "),
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, color = colors.Border)
                )
            }
        }
    }
}
