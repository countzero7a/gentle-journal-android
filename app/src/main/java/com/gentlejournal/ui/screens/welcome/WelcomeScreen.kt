package com.gentlejournal.ui.screens.welcome

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.data.ReminderManager
import com.gentlejournal.ui.theme.LocalJournalColors
import kotlinx.coroutines.launch

private data class WelcomePage(val emoji: String, val title: Int, val text: Int)

private val pages = listOf(
    WelcomePage("🌿", R.string.welcome_1_title, R.string.welcome_1_text),
    WelcomePage("🔒", R.string.welcome_2_title, R.string.welcome_2_text),
    WelcomePage("🕊️", R.string.welcome_3_title, R.string.welcome_3_text)
)

/** Three short pages for a new journal, ending in its first entry. */
@Composable
fun WelcomeScreen(onFinish: (startFirstEntry: Boolean, remind: Boolean) -> Unit) {
    val colors = LocalJournalColors.current
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == pages.lastIndex

    var remind by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> remind = granted }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.Background, contentColor = colors.Text) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (!lastPage) {
                    TextButton(onClick = { onFinish(false, false) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Text(stringResource(R.string.welcome_skip), color = colors.TextMuted)
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = page.emoji, fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(page.title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(page.text),
                        style = MaterialTheme.typography.bodyLarge.copy(color = colors.TextMuted),
                        textAlign = TextAlign.Center
                    )
                    if (index == pages.lastIndex) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.welcome_reminder),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                            )
                            Switch(
                                checked = remind,
                                onCheckedChange = { on ->
                                    if (on && ReminderManager.needsNotificationPermission(context)) {
                                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        remind = on
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = colors.Accent)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { i ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (i == pagerState.currentPage) colors.Accent else colors.Border)
                    )
                }
            }

            Button(
                onClick = {
                    if (lastPage) onFinish(true, remind)
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.Accent),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(if (lastPage) R.string.welcome_start else R.string.welcome_next),
                    color = colors.White
                )
            }
        }
    }
}
