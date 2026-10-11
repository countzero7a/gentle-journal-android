package com.gentlejournal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gentlejournal.domain.model.AppFont
import com.gentlejournal.domain.model.AppFontSize
import com.gentlejournal.domain.model.AppTheme
import com.gentlejournal.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")
    private val languageKey = stringPreferencesKey("app_language")
    private val fontKey = stringPreferencesKey("app_font")
    private val fontSizeKey = stringPreferencesKey("app_font_size")
    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")

    val selectedTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val name = prefs[themeKey] ?: AppTheme.MEADOW.name
        try { AppTheme.valueOf(name) } catch (e: IllegalArgumentException) { AppTheme.MEADOW }
    }

    val selectedLanguage: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val name = prefs[languageKey] ?: AppLanguage.SYSTEM.name
        try { AppLanguage.valueOf(name) } catch (e: IllegalArgumentException) { AppLanguage.SYSTEM }
    }

    val selectedFont: Flow<AppFont> = context.dataStore.data.map { prefs ->
        val name = prefs[fontKey] ?: AppFont.DEFAULT.name
        try { AppFont.valueOf(name) } catch (e: IllegalArgumentException) { AppFont.DEFAULT }
    }

    val selectedFontSize: Flow<AppFontSize> = context.dataStore.data.map { prefs ->
        val name = prefs[fontSizeKey] ?: AppFontSize.MEDIUM.name
        try { AppFontSize.valueOf(name) } catch (e: IllegalArgumentException) { AppFontSize.MEDIUM }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[themeKey] = theme.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[languageKey] = language.name }
    }

    suspend fun setFont(font: AppFont) {
        context.dataStore.edit { it[fontKey] = font.name }
    }

    suspend fun setFontSize(fontSize: AppFontSize) {
        context.dataStore.edit { it[fontSizeKey] = fontSize.name }
    }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[reminderEnabledKey] ?: false
    }

    val reminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[reminderHourKey] ?: 21
    }

    val reminderMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[reminderMinuteKey] ?: 0
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[reminderEnabledKey] = enabled
            it[reminderHourKey] = hour
            it[reminderMinuteKey] = minute
        }
    }
}
