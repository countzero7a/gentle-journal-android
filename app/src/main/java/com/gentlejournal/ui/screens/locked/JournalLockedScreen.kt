package com.gentlejournal.ui.screens.locked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.ui.theme.LocalJournalColors

/**
 * Shown instead of the app when the key that encrypts the journal can't be read
 * (for example after the phone's Keystore was reset), so the app explains what
 * happened instead of crashing on every start.
 */
@Composable
fun JournalLockedScreen(onRetry: () -> Unit, onStartNew: () -> Unit) {
    val colors = LocalJournalColors.current
    var confirmStartNew by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.Background, contentColor = colors.Text) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🔒", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.locked_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.locked_message),
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.TextMuted),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.Accent),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(stringResource(R.string.locked_retry), color = colors.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmStartNew = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(stringResource(R.string.locked_start_new), color = colors.Danger)
            }
        }
    }

    if (confirmStartNew) {
        AlertDialog(
            onDismissRequest = { confirmStartNew = false },
            title = { Text(stringResource(R.string.locked_confirm_title)) },
            text = { Text(stringResource(R.string.locked_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { confirmStartNew = false; onStartNew() }) {
                    Text(stringResource(R.string.locked_confirm_yes), color = colors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStartNew = false }) { Text(stringResource(R.string.entry_cancel)) }
            },
            containerColor = colors.Surface
        )
    }
}
