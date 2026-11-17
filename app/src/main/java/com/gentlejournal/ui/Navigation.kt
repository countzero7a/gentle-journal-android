package com.gentlejournal.ui

sealed class Screen(val route: String) {
    data object Timeline : Screen("timeline")
    data object NewEntry : Screen("new_entry")
    data object EditEntry : Screen("edit_entry/{entryId}") {
        fun createRoute(entryId: String) = "edit_entry/$entryId"
    }
    data object Patterns : Screen("patterns")
    data object Settings : Screen("settings")
    data object Export : Screen("export")
}
