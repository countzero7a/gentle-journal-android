package com.gentlejournal

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gentlejournal.data.DatabaseKeyManager
import com.gentlejournal.data.ThemePreferences
import com.gentlejournal.domain.lock.LockPolicy
import com.gentlejournal.domain.model.AppLanguage
import com.gentlejournal.ui.JournalApp
import com.gentlejournal.ui.JournalViewModel
import com.gentlejournal.ui.lock.AppLock
import com.gentlejournal.ui.lock.LockAction
import com.gentlejournal.ui.lock.LockSession
import com.gentlejournal.ui.screens.locked.JournalLockedScreen
import com.gentlejournal.ui.screens.locked.LockScreen
import com.gentlejournal.ui.theme.GentleJournalTheme
import com.gentlejournal.ui.util.LocaleHelper
import com.gentlejournal.ui.widget.MoodWidgetProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// FragmentActivity (a ComponentActivity) because BiometricPrompt needs one
class MainActivity : FragmentActivity() {

    private val viewModel: JournalViewModel by viewModels()
    private val lockSession: LockSession by viewModels()
    private lateinit var appliedLanguage: AppLanguage
    private lateinit var appLock: AppLock
    private lateinit var preferences: ThemePreferences
    private var lockEnabled = false

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

        // Unlock the journal's key before anything touches the database; if it can't be
        // read, explain and offer a way out instead of crashing on every start
        val keyUnavailable = try {
            DatabaseKeyManager.getPassphrase(applicationContext)
            false
        } catch (e: Exception) {
            true
        }
        if (keyUnavailable) {
            setContent {
                GentleJournalTheme {
                    JournalLockedScreen(
                        onRetry = { recreate() },
                        onStartNew = {
                            DatabaseKeyManager.resetJournal(applicationContext)
                            recreate()
                        }
                    )
                }
            }
            return
        }

        preferences = ThemePreferences(applicationContext)
        appLock = AppLock(this, ::onAuthenticationResult)
        // Read synchronously, so the journal never shows for a moment before the lock
        lockEnabled = runBlocking { preferences.appLockEnabled.first() }
        hideFromRecents(lockEnabled)
        updateLock()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Recreate only when the stored language differs from the one this activity was built with
                launch {
                    preferences.selectedLanguage
                        .distinctUntilChanged()
                        .collect { language -> if (language != appliedLanguage) recreate() }
                }
                launch {
                    preferences.appLockEnabled.collect { enabled ->
                        lockEnabled = enabled
                        hideFromRecents(enabled)
                    }
                }
            }
        }

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val appFont by viewModel.appFont.collectAsStateWithLifecycle()
            val appFontSize by viewModel.appFontSize.collectAsStateWithLifecycle()

            GentleJournalTheme(appTheme = appTheme, appFont = appFont, appFontSize = appFontSize) {
                if (lockSession.locked) {
                    LockScreen(onUnlock = { requestAuthentication(LockAction.UNLOCK) })
                } else {
                    JournalApp(
                        viewModel = viewModel,
                        appLockAvailable = appLock.canLock(),
                        onAppLockChange = ::changeAppLock
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::appLock.isInitialized) updateLock()
    }

    override fun onResume() {
        super.onResume()
        if (!::appLock.isInitialized) return
        if (lockSession.locked && lockSession.autoPrompt && lockSession.pendingAction == null) {
            lockSession.autoPrompt = false
            requestAuthentication(LockAction.UNLOCK)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) lockSession.backgroundedAt = System.currentTimeMillis()
    }

    /** Locks when the policy says so; never while a prompt is open (its own screen stops us). */
    private fun updateLock() {
        if (lockSession.pendingAction != null) return
        val mustUnlock = LockPolicy.mustUnlock(
            enabled = lockEnabled && appLock.canLock(),
            unlockedThisSession = lockSession.unlockedThisSession,
            backgroundedAt = lockSession.backgroundedAt,
            now = System.currentTimeMillis()
        )
        if (mustUnlock && !lockSession.locked) {
            lockSession.locked = true
            lockSession.autoPrompt = true
        }
    }

    private fun changeAppLock(enabled: Boolean) {
        // Without a screen lock there's nothing to confirm with; switching off must still work
        if (!appLock.canLock()) {
            if (!enabled) saveAppLock(false)
            return
        }
        requestAuthentication(if (enabled) LockAction.ENABLE else LockAction.DISABLE)
    }

    private fun requestAuthentication(action: LockAction) {
        if (lockSession.pendingAction != null) return
        lockSession.pendingAction = action
        appLock.authenticate(getString(R.string.lock_prompt_title))
    }

    private fun onAuthenticationResult(success: Boolean) {
        val action = lockSession.pendingAction ?: return
        lockSession.pendingAction = null
        if (!success) return

        lockSession.unlockedThisSession = true
        lockSession.backgroundedAt = null
        when (action) {
            LockAction.UNLOCK -> lockSession.locked = false
            LockAction.ENABLE -> saveAppLock(true)
            LockAction.DISABLE -> saveAppLock(false)
        }
    }

    private fun saveAppLock(enabled: Boolean) {
        lockEnabled = enabled
        hideFromRecents(enabled)
        lifecycleScope.launch {
            preferences.setAppLockEnabled(enabled)
            MoodWidgetProvider.updateAll(applicationContext)
        }
    }

    /** Keeps the journal out of the recent-apps thumbnail while the lock is on (Android 13+). */
    private fun hideFromRecents(hide: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!hide)
    }
}
