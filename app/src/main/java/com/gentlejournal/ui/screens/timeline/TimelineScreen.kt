package com.gentlejournal.ui.screens.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.ui.components.EmptyState
import com.gentlejournal.ui.components.JournalCard
import com.gentlejournal.ui.theme.LocalJournalColors
import com.gentlejournal.ui.util.resolveActionLabel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(entries: List<EntryWithTags>, onEntryClick: (String) -> Unit, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) {
        EmptyState(emoji = "\uD83D\uDCD3", title = stringResource(R.string.timeline_empty_title), message = stringResource(R.string.timeline_empty_message))
        return
    }

    val grouped = entries.groupBy { e ->
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(e.entry.timestamp))
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        grouped.forEach { (dateKey, dayEntries) ->
            item(key = "header_$dateKey") {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey) ?: Date()
                val isToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) == dateKey
                val displayDate = if (isToday) stringResource(R.string.timeline_today) else SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(text = displayDate, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(text = "${dayEntries.size}", style = MaterialTheme.typography.labelMedium.copy(color = LocalJournalColors.current.TextMuted))
                }
            }
            items(dayEntries, key = { it.entry.id }) { entry -> EntryCard(entry = entry, onClick = { onEntryClick(entry.entry.id) }) }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun EntryCard(entry: EntryWithTags, onClick: () -> Unit) {
    val colors = LocalJournalColors.current
    val context = LocalContext.current
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.entry.timestamp))
    JournalCard(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { entry.emotions.forEach { emo -> Text(text = emo.emoji, fontSize = 24.sp) } }
            Text(text = timeStr, style = MaterialTheme.typography.bodySmall)
        }
        if (entry.entry.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = entry.entry.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
