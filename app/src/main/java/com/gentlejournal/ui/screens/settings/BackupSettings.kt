package com.gentlejournal.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.BackupResult
import com.gentlejournal.domain.backup.BackupFrequency
import com.gentlejournal.ui.ImportStatus
import com.gentlejournal.ui.theme.LocalJournalColors
import java.text.DateFormat
import java.util.Date

private const val MIN_PASSWORD_LENGTH = 8

data class BackupUiState(
    val frequency: BackupFrequency? = null,
    val folderName: String? = null,
    val lastBackupAt: Long? = null,
    val result: BackupResult? = null,
    val busy: Boolean = false
)

class BackupCallbacks(
    val onSetUp: (password: CharArray, folder: Uri) -> Unit,
    val onBackUpNow: () -> Unit,
    val onChangeFolder: (Uri) -> Unit,
    val onFrequencyChange: (BackupFrequency) -> Unit,
    val onTurnOff: () -> Unit,
    val onRefreshFolder: () -> Unit
)

/** Automatic, password-protected backups into a folder the user picks. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupSection(state: BackupUiState, callbacks: BackupCallbacks) {
    val colors = LocalJournalColors.current
    val locale = LocalConfiguration.current.locales.get(0)

    // The password and the folder are collected in two steps; set up once both are known
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingPassword by remember { mutableStateOf<CharArray?>(null) }
    val setUpFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val password = pendingPassword
        pendingPassword = null
        if (uri != null && password != null) callbacks.onSetUp(password, uri)
    }
    val changeFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) callbacks.onChangeFolder(uri)
    }
    LaunchedEffect(state.frequency) { if (state.frequency != null) callbacks.onRefreshFolder() }

    Text(text = stringResource(R.string.backup_title), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.backup_desc),
        style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted, lineHeight = 18.sp)
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (state.frequency == null) {
        OutlinedButton(
            onClick = { showPasswordDialog = true },
            enabled = !state.busy,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.backup_set_up), fontSize = 13.sp)
        }
    } else {
        Text(
            text = state.folderName?.let { stringResource(R.string.backup_folder, it) }
                ?: stringResource(R.string.backup_folder_unavailable),
            style = MaterialTheme.typography.bodySmall.copy(color = if (state.folderName == null) colors.Danger else colors.Text)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.lastBackupAt?.let {
                stringResource(R.string.backup_last, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(it)))
            } ?: stringResource(R.string.backup_never),
            style = MaterialTheme.typography.bodySmall.copy(color = colors.TextMuted)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackupFrequency.entries.forEach { frequency ->
                val selected = frequency == state.frequency
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { callbacks.onFrequencyChange(frequency) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) colors.AccentLight else colors.SurfaceAlt,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.Accent else colors.Border)
                ) {
                    Text(
                        text = stringResource(if (frequency == BackupFrequency.DAILY) R.string.backup_daily else R.string.backup_weekly),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) colors.AccentDark else colors.TextMuted
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = callbacks.onBackUpNow, enabled = !state.busy, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.backup_now), fontSize = 13.sp)
            }
            OutlinedButton(onClick = { changeFolderPicker.launch(null) }, enabled = !state.busy, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.backup_change_folder), fontSize = 13.sp)
            }
            TextButton(onClick = callbacks.onTurnOff, enabled = !state.busy) {
                Text(stringResource(R.string.backup_turn_off), fontSize = 13.sp, color = colors.Danger)
            }
        }
    }

    if (state.busy) {
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.Accent)
    } else {
        state.result?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            val (message, color) = when (result) {
                is BackupResult.Saved -> stringResource(R.string.backup_saved) to colors.Accent
                BackupResult.FolderUnavailable -> stringResource(R.string.backup_folder_unavailable) to colors.Danger
                BackupResult.Failed -> stringResource(R.string.backup_failed) to colors.Danger
            }
            Text(text = message, style = MaterialTheme.typography.bodySmall.copy(color = color, fontWeight = FontWeight.SemiBold))
        }
    }

    if (showPasswordDialog) {
        NewBackupPasswordDialog(
            onConfirm = { password ->
                showPasswordDialog = false
                pendingPassword = password
                setUpFolderPicker.launch(null)
            },
            onDismiss = { showPasswordDialog = false }
        )
    }
}

/** Choosing the backup password, typed twice; it can't be recovered, which the dialog says. */
@Composable
private fun NewBackupPasswordDialog(onConfirm: (CharArray) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalJournalColors.current
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column {
                Text(stringResource(R.string.backup_password_message), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                PasswordField(password, { password = it; error = null }, stringResource(R.string.backup_password_hint))
                Spacer(modifier = Modifier.height(8.dp))
                PasswordField(repeat, { repeat = it; error = null }, stringResource(R.string.backup_password_repeat))
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall.copy(color = colors.Danger))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    password.length < MIN_PASSWORD_LENGTH -> R.string.backup_password_too_short
                    password != repeat -> R.string.backup_password_mismatch
                    else -> null
                }
                if (error == null) onConfirm(password.toCharArray())
            }) { Text(stringResource(R.string.backup_continue)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.entry_cancel)) } },
        containerColor = colors.Surface
    )
}

/** Asked when an imported file turns out to be a password-protected backup. */
@Composable
fun ImportPasswordDialog(status: ImportStatus, onOpen: (CharArray) -> Unit, onCancel: () -> Unit) {
    val colors = LocalJournalColors.current
    var password by remember { mutableStateOf("") }
    val decrypting = status is ImportStatus.Decrypting

    AlertDialog(
        onDismissRequest = { if (!decrypting) onCancel() },
        title = { Text(stringResource(R.string.import_password_title)) },
        text = {
            Column {
                Text(stringResource(R.string.import_password_message), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                PasswordField(password, { password = it }, stringResource(R.string.backup_password_hint))
                if (status is ImportStatus.WrongPassword) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(stringResource(R.string.import_password_wrong), style = MaterialTheme.typography.bodySmall.copy(color = colors.Danger))
                }
                if (decrypting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.Accent)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOpen(password.toCharArray()) }, enabled = password.isNotEmpty() && !decrypting) {
                Text(stringResource(R.string.import_open))
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !decrypting) { Text(stringResource(R.string.entry_cancel)) } },
        containerColor = colors.Surface
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/** A gentle note on the timeline when the journal has no recent backup. */
@Composable
fun BackupReminderCard(onSetUp: () -> Unit, onLater: () -> Unit) {
    val colors = LocalJournalColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.WarmLight,
        contentColor = colors.Text,
        border = BorderStroke(1.dp, colors.Warm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.backup_reminder_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.backup_reminder_text),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onLater) { Text(stringResource(R.string.backup_reminder_later), color = colors.TextMuted) }
                TextButton(onClick = onSetUp) {
                    Text(stringResource(R.string.backup_reminder_action), color = colors.AccentDark, textAlign = TextAlign.End)
                }
            }
        }
    }
}
