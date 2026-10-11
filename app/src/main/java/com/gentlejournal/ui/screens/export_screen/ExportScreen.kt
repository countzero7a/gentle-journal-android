package com.gentlejournal.ui.screens.export_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.domain.model.ExportFormat
import com.gentlejournal.ui.components.JournalCard
import com.gentlejournal.ui.components.SectionLabel
import com.gentlejournal.ui.theme.LocalJournalColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportScreen(entryCount: Int, oldestTimestamp: Long?, newestTimestamp: Long?, onExport: (ExportFormat) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalJournalColors.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.JSON) }
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = stringResource(R.string.export_description), style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted, lineHeight = 22.sp), modifier = Modifier.padding(bottom = 20.dp))
        JournalCard {
            SectionLabel(text = stringResource(R.string.export_format))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { format ->
                    val isSelected = format == selectedFormat
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (isSelected) colors.AccentLight else colors.Surface,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.Accent else colors.Border), onClick = { selectedFormat = format }) {
                        Text(text = ".${format.extension}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) colors.AccentDark else colors.TextMuted), modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        JournalCard {
            Text(text = stringResource(R.string.export_entries_count, entryCount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            if (entryCount > 0 && oldestTimestamp != null && newestTimestamp != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.export_range, dateFormat.format(Date(oldestTimestamp)), dateFormat.format(Date(newestTimestamp))), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onExport(selectedFormat) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = entryCount > 0,
            colors = ButtonDefaults.buttonColors(containerColor = colors.Accent), contentPadding = PaddingValues(vertical = 14.dp)) {
            Text(text = stringResource(R.string.export_button, selectedFormat.extension.uppercase()), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.White))
        }
    }
}
