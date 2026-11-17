package com.gentlejournal.ui.screens.timeline

import android.icu.text.DateFormat
import android.icu.text.DateFormatSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.calendar.CalendarMonth
import com.gentlejournal.ui.components.JournalCard
import com.gentlejournal.ui.theme.LocalJournalColors
import com.gentlejournal.ui.util.resolveEmotionLabel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Date

/** A month of the journal: each day shows its main feeling; tapping a day selects it. */
@Composable
fun CalendarView(
    entries: List<EntryWithTags>,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate) -> Unit
) {
    val colors = LocalJournalColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales.get(0)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val cells = remember(month, firstDayOfWeek) { CalendarMonth.cells(month, firstDayOfWeek) }
    val feelings = remember(entries) { CalendarMonth.mainFeelingByDay(entries, zone) }
    val daysWithEntries = remember(entries) {
        entries.map { Instant.ofEpochMilli(it.entry.timestamp).atZone(zone).toLocalDate() }.toSet()
    }
    // ICU gives proper standalone month names and short weekday names in every language
    val title = remember(month, locale) {
        DateFormat.getInstanceForSkeleton("yMMMM", locale).format(Date.from(month.atDay(1).atStartOfDay(zone).toInstant()))
    }
    val weekdays = remember(locale, firstDayOfWeek) {
        val names = DateFormatSymbols.getInstance(locale).getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.SHORT)
        // ICU counts Sunday as 1; java.time counts Monday as 1
        (0L until 7L).map { names[firstDayOfWeek.plus(it).value % 7 + 1] }
    }

    JournalCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month), tint = colors.Accent)
            }
            Text(
                text = title.replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month), tint = colors.Accent)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.TextMuted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (day != null) {
                            val feeling = feelings[day]
                            val selected = day == selectedDay
                            val label = feeling?.let { resolveEmotionLabel(context, it.id, it.label) }
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.AccentLight else Color.Transparent)
                                    .border(
                                        width = if (day == today) 1.5.dp else 0.dp,
                                        color = if (day == today) colors.Accent else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectDay(day) }
                                    .semantics { contentDescription = listOfNotNull(day.dayOfMonth.toString(), label).joinToString(", ") }
                                    .padding(2.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (selected) colors.AccentDark else colors.TextMuted,
                                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                when {
                                    feeling != null -> Text(text = feeling.emoji, fontSize = 16.sp)
                                    day in daysWithEntries -> Box(modifier = Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(colors.Accent))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
