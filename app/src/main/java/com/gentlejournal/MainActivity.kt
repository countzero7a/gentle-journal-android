package com.gentlejournal

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlejournal.domain.model.AppLanguage
import com.gentlejournal.ui.JournalApp
import com.gentlejournal.ui.JournalViewModel
import com.gentlejournal.ui.theme.GentleJournalTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: JournalViewModel by viewModels()
    private var currentLanguageCode: String? = null

    override fun attachBaseContext(newBase: Context) {
        // Read language preference synchronously before the activity context is created
        val prefs = com.gentlejournal.data.ThemePreferences(newBase)
        val language = runBlocking { prefs.selectedLanguage.first() }

        val context = if (language != AppLanguage.SYSTEM) {
            val locale = Locale(language.code)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        currentLanguageCode = language.code
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val appFont by viewModel.appFont.collectAsStateWithLifecycle()
            val appFontSize by viewModel.appFontSize.collectAsStateWithLifecycle()

            // If language changed since activity was created, recreate
            if (currentLanguageCode != null && appLanguage.code != currentLanguageCode) {
                currentLanguageCode = appLanguage.code
                recreate()
                return@setContent
            }

            GentleJournalTheme(appTheme = appTheme, appFont = appFont, appFontSize = appFontSize) {
                JournalApp(viewModel = viewModel)
            }
        }
    }
}
