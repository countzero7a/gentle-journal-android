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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.ui.theme.LocalJournalColors

/** Covers the journal until it's unlocked with the phone's lock. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val colors = LocalJournalColors.current
    Surface(modifier = Modifier.fillMaxSize(), color = colors.Background, contentColor = colors.Text) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🔒", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.Accent),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(stringResource(R.string.lock_unlock), color = colors.White)
            }
        }
    }
}
