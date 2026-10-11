package com.gentlejournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentlejournal.R
import com.gentlejournal.ui.theme.LocalJournalColors

/**
 * Rename, re-emoji or delete a tag the user created. [onSave] returns false when
 * the name is already taken, which keeps the dialog open with a message.
 */
@Composable
fun TagEditDialog(
    initialLabel: String,
    initialEmoji: String,
    onSave: (label: String, emoji: String) -> Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalJournalColors.current
    var label by remember { mutableStateOf(initialLabel) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    var labelTaken by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.tag_delete_title, initialLabel)) },
            text = { Text(stringResource(R.string.tag_delete_message)) },
            confirmButton = {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.tag_delete), color = colors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.entry_cancel)) }
            },
            containerColor = colors.Surface
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_edit_title)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji, onValueChange = { if (it.length <= 2) emoji = it },
                        modifier = Modifier.width(64.dp), singleLine = true, shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = label, onValueChange = { label = it; labelTaken = false },
                        modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(stringResource(R.string.entry_label_hint)) }
                    )
                }
                if (labelTaken) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.tag_label_taken),
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.Danger)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.tag_delete), color = colors.Danger)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank(),
                onClick = { if (onSave(label, emoji)) onDismiss() else labelTaken = true }
            ) { Text(stringResource(R.string.tag_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.entry_cancel)) }
        },
        containerColor = colors.Surface
    )
}
