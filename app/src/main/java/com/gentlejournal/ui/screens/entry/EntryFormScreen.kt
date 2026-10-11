package com.gentlejournal.ui.screens.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gentlejournal.R
import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.ui.components.*
import com.gentlejournal.ui.theme.LocalJournalColors
import com.gentlejournal.ui.util.resolveActionLabel
import com.gentlejournal.ui.util.resolveEmotionLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryFormScreen(
    isEdit: Boolean, text: String, onTextChange: (String) -> Unit,
    selectedEmotionIds: List<String>, selectedActionIds: List<String>,
    emotions: List<EmotionEntity>, actions: List<ActionEntity>,
    onToggleEmotion: (String) -> Unit, onToggleAction: (String) -> Unit,
    onAddCustomEmotion: (String, String) -> Boolean, onAddCustomAction: (String, String) -> Boolean,
    onSave: () -> Unit, onDelete: (() -> Unit)? = null, isSaving: Boolean = false, modifier: Modifier = Modifier
) {
    val colors = LocalJournalColors.current
    val context = LocalContext.current
    var showNewEmotion by remember { mutableStateOf(false) }
    var showNewAction by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var customLabel by remember { mutableStateOf("") }
    var customEmoji by remember { mutableStateOf("") }
    var customLabelTaken by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().imePadding().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 12.dp)) {
        SectionLabel(text = stringResource(R.string.entry_on_your_mind), hint = stringResource(R.string.entry_optional))
        Surface(shape = RoundedCornerShape(12.dp), color = colors.SurfaceAlt, border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(colors.Border))) {
            Box(modifier = Modifier.padding(14.dp)) {
                if (text.isEmpty()) {
                    Text(text = stringResource(R.string.entry_placeholder), style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted))
                }
                BasicTextField(value = text, onValueChange = onTextChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.Text), cursorBrush = SolidColor(colors.Accent),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).onFocusChanged { if (it.isFocused) coroutineScope.launch { scrollState.animateScrollTo(0) } })
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(text = stringResource(R.string.entry_how_feeling), hint = stringResource(R.string.entry_up_to_two))

        // Top-level emotions
        val topEmotions = emotions.filter { it.parentId == null }
        val subEmotions = emotions.filter { it.parentId != null }
        // Show sub-emotions for any selected parent, and keep them visible after one of them
        // has replaced its parent
        val selectedParentIds = selectedEmotionIds.mapNotNull { selId ->
            emotions.find { it.id == selId }?.let { it.parentId ?: it.id }
        }.toSet()
        val visibleSubEmotions = subEmotions.filter { it.parentId in selectedParentIds }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            topEmotions.forEach { emo ->
                TagChip(emoji = emo.emoji, label = resolveEmotionLabel(context, emo.id, emo.label), selected = selectedEmotionIds.contains(emo.id), onClick = { onToggleEmotion(emo.id) })
            }
            AddCustomChip(onClick = { showNewEmotion = true; customLabel = ""; customEmoji = ""; customLabelTaken = false })
        }

        // Sub-emotions (expand when parent is selected)
        if (visibleSubEmotions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.entry_more_specific),
                style = MaterialTheme.typography.labelSmall.copy(color = colors.TextMuted),
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                visibleSubEmotions.forEach { emo ->
                    TagChip(emoji = emo.emoji, label = resolveEmotionLabel(context, emo.id, emo.label), selected = selectedEmotionIds.contains(emo.id), onClick = { onToggleEmotion(emo.id) }, small = true)
                }
            }
        }
        if (showNewEmotion) {
            Spacer(modifier = Modifier.height(12.dp))
            CustomTagCard(title = stringResource(R.string.entry_new_emotion_tag), emoji = customEmoji, label = customLabel, onEmojiChange = { customEmoji = it }, labelTaken = customLabelTaken, onLabelChange = { customLabel = it; customLabelTaken = false },
                onAdd = { if (onAddCustomEmotion(customLabel, customEmoji.ifBlank { "\uD83D\uDCAD" })) showNewEmotion = false else customLabelTaken = true }, onCancel = { showNewEmotion = false })
        }
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel(text = stringResource(R.string.entry_what_did))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { action ->
                TagChip(emoji = action.emoji, label = resolveActionLabel(context, action.id, action.label), selected = selectedActionIds.contains(action.id), onClick = { onToggleAction(action.id) })
            }
            AddCustomChip(onClick = { showNewAction = true; customLabel = ""; customEmoji = ""; customLabelTaken = false })
        }
        if (showNewAction) {
            Spacer(modifier = Modifier.height(12.dp))
            CustomTagCard(title = stringResource(R.string.entry_new_action_tag), emoji = customEmoji, label = customLabel, onEmojiChange = { customEmoji = it }, labelTaken = customLabelTaken, onLabelChange = { customLabel = it; customLabelTaken = false },
                onAdd = { if (onAddCustomAction(customLabel, customEmoji.ifBlank { "\u2728" })) showNewAction = false else customLabelTaken = true }, onCancel = { showNewAction = false })
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.Accent), contentPadding = PaddingValues(vertical = 14.dp)) {
            Text(text = if (isEdit) stringResource(R.string.entry_update) else stringResource(R.string.entry_save), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.White))
        }
        if (isEdit && onDelete != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { showDeleteConfirm = true }, enabled = !isSaving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.Danger), contentPadding = PaddingValues(vertical = 14.dp)) {
                Text(text = stringResource(R.string.entry_delete), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.White))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.entry_delete_confirm_title)) },
            text = { Text(stringResource(R.string.entry_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.entry_delete), color = colors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.entry_cancel)) }
            },
            containerColor = colors.Surface
        )
    }
}

@Composable
private fun CustomTagCard(title: String, emoji: String, label: String, labelTaken: Boolean, onEmojiChange: (String) -> Unit, onLabelChange: (String) -> Unit, onAdd: () -> Unit, onCancel: () -> Unit) {
    val colors = LocalJournalColors.current
    Surface(shape = RoundedCornerShape(16.dp), color = colors.WarmLight, border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(colors.Warm))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = emoji, onValueChange = { if (it.length <= 2) onEmojiChange(it) }, modifier = Modifier.width(64.dp), placeholder = { Text("\uD83D\uDE0A") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = label, onValueChange = onLabelChange, modifier = Modifier.weight(1f), placeholder = { Text(stringResource(R.string.entry_label_hint)) }, singleLine = true, shape = RoundedCornerShape(12.dp))
            }
            if (labelTaken) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = stringResource(R.string.tag_label_taken), style = MaterialTheme.typography.bodySmall.copy(color = colors.Danger))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd, enabled = label.isNotBlank(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.Accent), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text(stringResource(R.string.entry_add)) }
                OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text(stringResource(R.string.entry_cancel)) }
            }
        }
    }
}
