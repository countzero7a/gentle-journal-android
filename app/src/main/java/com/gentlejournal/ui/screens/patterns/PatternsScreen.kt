package com.gentlejournal.ui.screens.patterns

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gentlejournal.R
import com.gentlejournal.ui.theme.LocalJournalColors

private val tabs = listOf(R.string.nav_insights, R.string.nav_charts)

/** Insights and charts as two tabs of one screen; the chosen tab survives leaving and coming back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsScreen(
    insights: @Composable () -> Unit,
    charts: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalJournalColors.current
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selected, containerColor = colors.Background) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selected == index,
                    onClick = { selected = index },
                    text = { Text(stringResource(title)) },
                    selectedContentColor = colors.Accent,
                    unselectedContentColor = colors.TextMuted
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (selected == 0) insights() else charts()
        }
    }
}
