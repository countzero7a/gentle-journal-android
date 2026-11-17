package com.gentlejournal.ui.screens.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.domain.usecase.EntrySearch
import com.gentlejournal.ui.components.EmptyState
import com.gentlejournal.ui.components.JournalCard
import com.gentlejournal.ui.theme.LocalJournalColors
import com.gentlejournal.ui.screens.settings.BackupReminderCard
import com.gentlejournal.ui.util.formattedEntryText
import com.gentlejournal.ui.util.resolveActionLabel
import com.gentlejournal.ui.util.resolveEmotionLabel
import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
fun TimelineScreen(
    entries: List<EntryWithTags>,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    showBackupReminder: Boolean = false,
    onSetUpBackup: () -> Unit = {},
    onDismissBackupReminder: () -> Unit = {}
) {
    if (entries.isEmpty()) {
        EmptyState(emoji = "\uD83D\uDCD3", title = stringResource(R.string.timeline_empty_title), message = stringResource(R.string.timeline_empty_message))
        return
    }

    val context = LocalContext.current
    val colors = LocalJournalColors.current
    val locale = LocalConfiguration.current.locales.get(0)
    val zone = ZoneId.systemDefault()
    // Locale-appropriate patterns; the time follows the phone's 12/24-hour setting
    val headerFormat = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale) }
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val today = LocalDate.now(zone)
    fun dayOf(entry: EntryWithTags): LocalDate = Instant.ofEpochMilli(entry.entry.timestamp).atZone(zone).toLocalDate()

    var query by rememberSaveable { mutableStateOf("") }
    var calendarMode by rememberSaveable { mutableStateOf(false) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now(zone)) }
    var selectedDay by rememberSaveable { mutableStateOf<LocalDate?>(today) }

    // Search covers the text and the feeling and activity names, in both views
    val shown = remember(entries, query, context) {
        if (query.isBlank()) entries
        else entries.filter {
            EntrySearch.matches(
                it, query,
                emotionLabel = { e -> resolveEmotionLabel(context, e.id, e.label) },
                actionLabel = { a -> resolveActionLabel(context, a.id, a.label) }
            )
        }
    }
    val grouped = remember(shown) { shown.groupBy { dayOf(it) } }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showBackupReminder) {
            item(key = "backup_reminder") { BackupReminderCard(onSetUp = onSetUpBackup, onLater = onDismissBackupReminder) }
        }
        item(key = "search") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.timeline_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.TextMuted) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.timeline_clear_search), tint = colors.TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = { calendarMode = !calendarMode }) {
                    Icon(
                        imageVector = if (calendarMode) Icons.AutoMirrored.Filled.List else Icons.Default.DateRange,
                        contentDescription = stringResource(if (calendarMode) R.string.timeline_show_list else R.string.timeline_show_calendar),
                        tint = colors.Accent
                    )
                }
            }
        }

        if (calendarMode) {
            item(key = "calendar") {
                CalendarView(
                    entries = shown,
                    month = month,
                    onMonthChange = { month = it },
                    selectedDay = selectedDay,
                    onSelectDay = { selectedDay = it }
                )
            }
            val dayEntries = selectedDay?.let { day -> shown.filter { dayOf(it) == day } }.orEmpty()
            if (selectedDay != null && dayEntries.isEmpty()) {
                item(key = "day_empty") {
                    Text(
                        text = stringResource(R.string.calendar_no_entries_day),
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            items(dayEntries, key = { it.entry.id }) { entry -> EntryCard(entry = entry, timeFormat = timeFormat, onClick = { onEntryClick(entry.entry.id) }) }
        } else if (shown.isEmpty()) {
            item(key = "no_results") {
                Text(
                    text = stringResource(R.string.timeline_no_results),
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                )
            }
        } else {
            grouped.forEach { (date, dayEntries) ->
                item(key = "header_$date") {
                    val displayDate = when (date) {
                        today -> stringResource(R.string.timeline_today)
                        today.minusDays(1) -> stringResource(R.string.timeline_yesterday)
                        else -> date.format(headerFormat)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(text = displayDate, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(text = "${dayEntries.size}", style = MaterialTheme.typography.labelMedium.copy(color = colors.TextMuted))
                    }
                }
                items(dayEntries, key = { it.entry.id }) { entry -> EntryCard(entry = entry, timeFormat = timeFormat, onClick = { onEntryClick(entry.entry.id) }) }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun EntryCard(entry: EntryWithTags, timeFormat: java.text.DateFormat, onClick: () -> Unit) {
    val colors = LocalJournalColors.current
    val context = LocalContext.current
    val timeStr = timeFormat.format(Date(entry.entry.timestamp))
    JournalCard(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { entry.emotions.forEach { emo -> Text(text = emo.emoji, fontSize = 24.sp) } }
            Text(text = timeStr, style = MaterialTheme.typography.bodySmall)
        }
        if (entry.entry.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            val formatted = remember(entry.entry.text, colors.Accent) { formattedEntryText(entry.entry.text, colors.Accent) }
            Text(text = formatted, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (entry.actions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                entry.actions.forEach { action ->
                    Surface(shape = RoundedCornerShape(10.dp), color = colors.SurfaceAlt) {
                        Text(text = "${action.emoji} ${resolveActionLabel(context, action.id, action.label)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
        }
    }
}
