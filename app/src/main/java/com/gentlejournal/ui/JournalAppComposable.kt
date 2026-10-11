package com.gentlejournal.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gentlejournal.R
import com.gentlejournal.domain.model.AppTheme
import com.gentlejournal.domain.model.AppLanguage
import com.gentlejournal.ui.screens.entry.EntryFormScreen
import com.gentlejournal.ui.screens.export_screen.ExportScreen
import com.gentlejournal.ui.screens.insights.InsightsScreen
import com.gentlejournal.ui.screens.charts.ChartsScreen
import com.gentlejournal.ui.screens.settings.SettingsScreen
import com.gentlejournal.ui.screens.timeline.TimelineScreen
import com.gentlejournal.ui.theme.LocalJournalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalApp(
    viewModel: JournalViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    val colors = LocalJournalColors.current

    // Collected state
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appFont by viewModel.appFont.collectAsStateWithLifecycle()
    val appFontSize by viewModel.appFontSize.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val emotions by viewModel.emotions.collectAsStateWithLifecycle()
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    val entryCount by viewModel.entryCount.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val insightPeriod by viewModel.insightPeriod.collectAsStateWithLifecycle()
    val savedInsights by viewModel.savedInsights.collectAsStateWithLifecycle()
    val formText by viewModel.formText.collectAsStateWithLifecycle()
    val formEmotionIds by viewModel.formEmotionIds.collectAsStateWithLifecycle()
    val formActionIds by viewModel.formActionIds.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()

    // Import file picker. Many file managers don't label .json files as
    // application/json, so plain text and generic binary are offered too.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it) }
    }

    val isMainScreen = currentRoute in listOf(
        Screen.Timeline.route, Screen.Insights.route, Screen.Charts.route, Screen.Settings.route
    )

    Scaffold(
        containerColor = colors.Background,
        topBar = {
            JournalTopBar(
                currentRoute = currentRoute,
                entryCount = entryCount,
                onBack = {
                    viewModel.resetForm()
                    navController.popBackStack()
                },
                onExport = { navController.navigate(Screen.Export.route) }
            )
        },
        bottomBar = {
            if (isMainScreen) {
                JournalBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == Screen.NewEntry.route) {
                            viewModel.resetForm()
                        }
                        navController.navigate(route) {
                            if (route != Screen.NewEntry.route) {
                                popUpTo(Screen.Timeline.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timeline.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    entries = entries,
                    onEntryClick = { entryId ->
                        viewModel.loadEntryForEditing(entryId)
                        navController.navigate(Screen.EditEntry.createRoute(entryId))
                    }
                )
            }

            composable(Screen.NewEntry.route) {
                EntryFormScreen(
                    isEdit = false,
                    text = formText,
                    onTextChange = viewModel::updateFormText,
                    selectedEmotionIds = formEmotionIds,
                    selectedActionIds = formActionIds,
                    emotions = emotions,
                    actions = actions,
                    onToggleEmotion = viewModel::toggleFormEmotion,
                    onToggleAction = viewModel::toggleFormAction,
                    onAddCustomEmotion = viewModel::addCustomEmotion,
                    onAddCustomAction = viewModel::addCustomAction,
                    onSave = {
                        viewModel.saveNewEntry { navController.popBackStack() }
                    },
                    isSaving = isSaving
                )
            }

            composable(
                route = Screen.EditEntry.route,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
                EntryFormScreen(
                    isEdit = true,
                    text = formText,
                    onTextChange = viewModel::updateFormText,
                    selectedEmotionIds = formEmotionIds,
                    selectedActionIds = formActionIds,
                    emotions = emotions,
                    actions = actions,
                    onToggleEmotion = viewModel::toggleFormEmotion,
                    onToggleAction = viewModel::toggleFormAction,
                    onAddCustomEmotion = viewModel::addCustomEmotion,
                    onAddCustomAction = viewModel::addCustomAction,
                    onSave = {
                        viewModel.updateExistingEntry { navController.popBackStack() }
                    },
                    onDelete = {
                        viewModel.deleteEntry(entryId) { navController.popBackStack() }
                    },
                    isSaving = isSaving
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(
                    insights = insights,
                    savedInsights = savedInsights,
                    currentPeriod = insightPeriod,
                    onPeriodChange = viewModel::setInsightPeriod,
                    onInsightTap = viewModel::recordInsightEngagement
                )
            }

            composable(Screen.Charts.route) {
                ChartsScreen(entries = entries)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    entryCount = entryCount,
                    emotions = emotions,
                    actions = actions,
                    currentTheme = appTheme,
                    onThemeChange = viewModel::setTheme,
                    currentLanguage = appLanguage,
                    onLanguageChange = viewModel::setLanguage,
                    currentFont = appFont,
                    onFontChange = viewModel::setFont,
                    currentFontSize = appFontSize,
                    onFontSizeChange = viewModel::setFontSize,
                    onDeleteAllData = viewModel::deleteAllEntries,
                    onImportData = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    importStatus = importStatus,
                    onClearImportStatus = viewModel::clearImportStatus,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    onReminderChange = viewModel::setReminder,
                    onEditEmotion = viewModel::updateCustomEmotion,
                    onDeleteEmotion = viewModel::deleteCustomEmotion,
                    onEditAction = viewModel::updateCustomAction,
                    onDeleteAction = viewModel::deleteCustomAction,
                    onDonate = {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ko-fi.com"))
                        context.startActivity(intent)
                    }
                )
            }

            composable(Screen.Export.route) {
                ExportScreen(
                    entryCount = entryCount,
                    oldestTimestamp = entries.lastOrNull()?.entry?.timestamp,
                    newestTimestamp = entries.firstOrNull()?.entry?.timestamp,
                    onExport = { format ->
                        viewModel.exportData(format) { intent ->
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_journal)))
                        }
                    }
                )
            }
        }
    }
}

// ── Top App Bar ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalTopBar(
    currentRoute: String?,
    entryCount: Int,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    val colors = LocalJournalColors.current
    val isDetailScreen = currentRoute in listOf(
        Screen.NewEntry.route, Screen.EditEntry.route, Screen.Export.route
    )

    val title = when {
        currentRoute == Screen.Timeline.route -> "\uD83C\uDF3F " + stringResource(R.string.nav_journal)
        currentRoute == Screen.NewEntry.route -> stringResource(R.string.nav_new_entry)
        currentRoute?.startsWith("edit_entry") == true -> stringResource(R.string.nav_edit_entry)
        currentRoute == Screen.Insights.route -> stringResource(R.string.nav_insights)
        currentRoute == Screen.Charts.route -> stringResource(R.string.nav_charts)
        currentRoute == Screen.Settings.route -> stringResource(R.string.nav_settings)
        currentRoute == Screen.Export.route -> stringResource(R.string.nav_export)
        else -> ""
    }

    val subtitle = if (currentRoute == Screen.Timeline.route) {
        if (entryCount == 1) stringResource(R.string.timeline_subtitle_one, entryCount)
        else stringResource(R.string.timeline_subtitle_many, entryCount)
    } else null

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        navigationIcon = {
            if (isDetailScreen) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = colors.Accent
                    )
                }
            }
        },
        actions = {
            if (currentRoute == Screen.Timeline.route) {
                TextButton(onClick = onExport) {
                    Text(
                        text = stringResource(R.string.timeline_export),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.TextMuted
                        )
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.Background
        )
    )
}

// ── Bottom Navigation Bar ───────────────────────────────────────────

@Composable
private fun JournalBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val colors = LocalJournalColors.current

    NavigationBar(
        containerColor = colors.Background,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Timeline.route,
            onClick = { onNavigate(Screen.Timeline.route) },
            icon = { Text("\uD83D\uDCD6", fontSize = 22.sp) },
            label = { Text(stringResource(R.string.nav_journal), fontSize = 10.sp) },
            colors = navItemColors()
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Insights.route,
            onClick = { onNavigate(Screen.Insights.route) },
            icon = { Text("\uD83D\uDCA1", fontSize = 22.sp) },
            label = { Text(stringResource(R.string.nav_insights), fontSize = 10.sp) },
            colors = navItemColors()
        )

        NavigationBarItem(
            selected = false,
            onClick = { onNavigate(Screen.NewEntry.route) },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = colors.Accent,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.settings_new_entry),
                            tint = colors.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            label = { },
            colors = navItemColors()
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Charts.route,
            onClick = { onNavigate(Screen.Charts.route) },
            icon = { Text("\uD83D\uDCCA", fontSize = 22.sp) },
            label = { Text(stringResource(R.string.nav_charts), fontSize = 10.sp) },
            colors = navItemColors()
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = { Text("\u2699\uFE0F", fontSize = 22.sp) },
            label = { Text(stringResource(R.string.nav_settings), fontSize = 10.sp) },
            colors = navItemColors()
        )
    }
}

@Composable
private fun navItemColors(): NavigationBarItemColors {
    val colors = LocalJournalColors.current
    return NavigationBarItemDefaults.colors(
        selectedIconColor = colors.Accent,
        selectedTextColor = colors.Accent,
        unselectedIconColor = colors.TextMuted,
        unselectedTextColor = colors.TextMuted,
        indicatorColor = colors.AccentLight
    )
}
