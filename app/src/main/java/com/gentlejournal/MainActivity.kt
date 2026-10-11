package com.gentlejournal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gentlejournal.data.ThemePreferences
import com.gentlejournal.domain.model.AppLanguage
import com.gentlejournal.ui.JournalApp
import com.gentlejournal.ui.JournalViewModel
import com.gentlejournal.ui.theme.GentleJournalTheme
import com.gentlejournal.ui.util.LocaleHelper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var appliedLanguage: AppLanguage

    override fun attachBaseContext(newBase: Context) {
        // Read the language synchronously: the activity's resources are fixed once attached
        val language = runBlocking { ThemePreferences(newBase).selectedLanguage.first() }
        appliedLanguage = language
        LocaleHelper.applyDefaultLocale(language)
        super.attachBaseContext(LocaleHelper.localizedContext(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Recreate only when the stored language differs from the one this activity was built with
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ThemePreferences(applicationContext).selectedLanguage
                    .distinctUntilChanged()
                    .collect { language -> if (language != appliedLanguage) recreate() }
            }
        }

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val appFont by viewModel.appFont.collectAsStateWithLifecycle()
            val appFontSize by viewModel.appFontSize.collectAsStateWithLifecycle()

            GentleJournalTheme(appTheme = appTheme, appFont = appFont, appFontSize = appFontSize) {
                JournalApp(viewModel = viewModel)
            }
        }
    }
}
