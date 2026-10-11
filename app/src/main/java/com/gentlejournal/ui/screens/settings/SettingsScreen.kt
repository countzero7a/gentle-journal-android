package com.gentlejournal.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.domain.model.AppFont
import com.gentlejournal.domain.model.AppFontSize
import com.gentlejournal.domain.model.AppTheme
import com.gentlejournal.domain.model.AppLanguage
import com.gentlejournal.ui.ImportStatus
import com.gentlejournal.ui.message
import com.gentlejournal.ui.components.JournalCard
import com.gentlejournal.ui.components.TagChip
import com.gentlejournal.ui.theme.*
import com.gentlejournal.ui.util.resolveActionLabel
import com.gentlejournal.ui.util.resolveEmotionLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    entryCount: Int,
    emotions: List<EmotionEntity>,
    actions: List<ActionEntity>,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    currentFont: AppFont,
    onFontChange: (AppFont) -> Unit,
    currentFontSize: AppFontSize,
    onFontSizeChange: (AppFontSize) -> Unit,
    widgetEnabled: Boolean,
    onWidgetToggle: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onImportData: () -> Unit,
    importStatus: ImportStatus?,
    onClearImportStatus: () -> Unit,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onReminderChange: (Boolean, Int, Int) -> Unit,
    onDonate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalJournalColors.current
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Show import status snackbar
    LaunchedEffect(importStatus) {
        if (importStatus != null) {
            kotlinx.coroutines.delay(5000)
            onClearImportStatus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Theme picker
        JournalCard {
            Text(text = stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    val isSelected = theme == currentTheme
                    val previewPalette = when (theme) {
                        AppTheme.SYSTEM -> MeadowPalette
                        AppTheme.MEADOW -> MeadowPalette
                        AppTheme.MOONLIGHT -> MoonlightPalette
                        AppTheme.OCEAN -> OceanPalette
                        AppTheme.EMBER -> EmberPalette
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onThemeChange(theme) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.AccentLight else colors.SurfaceAlt,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.Accent else colors.Border)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(previewPalette.Background, previewPalette.Accent, previewPalette.Warm, previewPalette.Surface).forEach { color ->
                                    Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(color))
                                }
                            }
                            Text(
                                text = "${theme.emoji}  ${theme.displayName}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = colors.Text
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.settings_selected), tint = colors.Accent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Language dropdown
        JournalCard {
            Text(text = stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            var languageExpanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { languageExpanded = true },
                    shape = RoundedCornerShape(12.dp), color = colors.SurfaceAlt, border = BorderStroke(1.dp, colors.Border)
                ) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentLanguage.emoji, fontSize = 20.sp)
                            Text(text = currentLanguage.nativeName, style = MaterialTheme.typography.bodyMedium.copy(color = colors.Text))
                        }
                        Text(text = "\u25BE", style = MaterialTheme.typography.bodyLarge.copy(color = colors.TextMuted))
                    }
                }
                DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                    AppLanguage.entries.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = language.emoji, fontSize = 18.sp)
                                    Column {
                                        Text(text = language.nativeName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (language == currentLanguage) FontWeight.SemiBold else FontWeight.Normal))
                                        if (language != AppLanguage.SYSTEM && language.nativeName != language.displayName) {
                                            Text(text = language.displayName, style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted))
                                        }
                                    }
                                }
                            },
                            onClick = { onLanguageChange(language); languageExpanded = false },
                            trailingIcon = { if (language == currentLanguage) Icon(Icons.Default.Check, contentDescription = null, tint = colors.Accent, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Typography
        JournalCard {
            Text(text = stringResource(R.string.settings_typography), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            // Font picker dropdown
            Text(text = stringResource(R.string.settings_font), style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted))
            Spacer(modifier = Modifier.height(6.dp))
            var fontExpanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { fontExpanded = true },
                    shape = RoundedCornerShape(12.dp), color = colors.SurfaceAlt, border = BorderStroke(1.dp, colors.Border)
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = currentFont.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "\u25BE", style = MaterialTheme.typography.bodyLarge.copy(color = colors.TextMuted))
                    }
                }
                DropdownMenu(expanded = fontExpanded, onDismissRequest = { fontExpanded = false }) {
                    AppFont.entries.forEach { font ->
                        DropdownMenuItem(
                            text = { Text(text = font.displayName, fontWeight = if (font == currentFont) FontWeight.SemiBold else FontWeight.Normal) },
                            onClick = { onFontChange(font); fontExpanded = false },
                            trailingIcon = { if (font == currentFont) Icon(Icons.Default.Check, contentDescription = null, tint = colors.Accent, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Font size picker
            Text(text = stringResource(R.string.settings_font_size), style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFontSize.entries.forEach { size ->
                    val isSelected = size == currentFontSize
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onFontSizeChange(size) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.AccentLight else colors.SurfaceAlt,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.Accent else colors.Border)
                    ) {
                        Text(
                            text = size.displayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.AccentDark else colors.TextMuted,
                                fontSize = (11 * size.scaleFactor).sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Widget toggle
        JournalCard {
            Text(text = stringResource(R.string.settings_widget), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.settings_widget_desc), style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_widget_enabled), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = widgetEnabled,
                    onCheckedChange = onWidgetToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.Accent)
                )
            }
            if (widgetEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_widget_hint),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted, lineHeight = 18.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reminder
        JournalCard {
            Text(text = stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.settings_reminder_desc), style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted))
            Spacer(modifier = Modifier.height(12.dp))

            var localEnabled by remember { mutableStateOf(reminderEnabled) }
            var localHour by remember { mutableIntStateOf(reminderHour) }
            var localMinute by remember { mutableIntStateOf(reminderMinute) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_reminder_enabled), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = localEnabled,
                    onCheckedChange = {
                        localEnabled = it
                        onReminderChange(it, localHour, localMinute)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.Accent)
                )
            }

            if (localEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(R.string.settings_reminder_time), style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Hour picker
                    var hourExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { hourExpanded = true },
                            shape = RoundedCornerShape(10.dp), color = colors.SurfaceAlt, border = BorderStroke(1.dp, colors.Border)
                        ) {
                            Text(text = "%02d".format(localHour), modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                        DropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                            (0..23).forEach { h ->
                                DropdownMenuItem(text = { Text("%02d".format(h)) }, onClick = { localHour = h; hourExpanded = false; onReminderChange(localEnabled, h, localMinute) })
                            }
                        }
                    }
                    Text(text = ":", style = MaterialTheme.typography.bodyLarge)
                    // Minute picker
                    var minExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { minExpanded = true },
                            shape = RoundedCornerShape(10.dp), color = colors.SurfaceAlt, border = BorderStroke(1.dp, colors.Border)
                        ) {
                            Text(text = "%02d".format(localMinute), modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                        DropdownMenu(expanded = minExpanded, onDismissRequest = { minExpanded = false }) {
                            listOf(0, 15, 30, 45).forEach { m ->
                                DropdownMenuItem(text = { Text("%02d".format(m)) }, onClick = { localMinute = m; minExpanded = false; onReminderChange(localEnabled, localHour, m) })
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // About
        JournalCard {
            Text(text = stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.settings_about_text), style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted, lineHeight = 22.sp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Emotion tags
        JournalCard {
            Text(text = stringResource(R.string.settings_emotion_tags, emotions.size), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emotions.forEach { emo ->
                    TagChip(emoji = emo.emoji, label = resolveEmotionLabel(context, emo.id, emo.label), selected = false, onClick = {}, small = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action tags
        JournalCard {
            Text(text = stringResource(R.string.settings_action_tags, actions.size), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { action ->
                    TagChip(emoji = action.emoji, label = resolveActionLabel(context, action.id, action.label), selected = false, onClick = {}, small = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Data management
        JournalCard {
            Text(text = stringResource(R.string.settings_your_data), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.settings_entries_stored, entryCount), style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted))
            Spacer(modifier = Modifier.height(12.dp))

            // Import button
            OutlinedButton(
                onClick = onImportData,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.settings_import), fontSize = 13.sp)
            }
            Text(text = stringResource(R.string.settings_import_desc), style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted), modifier = Modifier.padding(top = 4.dp))

            // Import status
            if (importStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = importStatus.message(context.resources), style = MaterialTheme.typography.bodySmall.copy(color = colors.Accent, fontWeight = FontWeight.SemiBold))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete
            if (showDeleteConfirm) {
                Text(text = stringResource(R.string.settings_delete_confirm), style = MaterialTheme.typography.bodyMedium.copy(color = colors.Danger, fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDeleteAllData(); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = colors.Danger), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(stringResource(R.string.settings_delete_yes))
                    }
                    OutlinedButton(onClick = { showDeleteConfirm = false }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(stringResource(R.string.entry_cancel))
                    }
                }
            } else {
                Button(onClick = { showDeleteConfirm = true }, colors = ButtonDefaults.buttonColors(containerColor = colors.Danger), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.settings_delete_all), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Donation
        Surface(shape = RoundedCornerShape(16.dp), color = colors.AccentLight, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "\u2615", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(R.string.settings_support), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.settings_support_text), style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onDonate, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.Accent)) {
                    Text(stringResource(R.string.settings_donate) + " \u2615")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = stringResource(R.string.settings_footer), style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
    }
}
