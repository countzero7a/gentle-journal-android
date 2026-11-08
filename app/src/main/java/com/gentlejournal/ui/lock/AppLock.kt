package com.gentlejournal.ui.lock

import android.app.KeyguardManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel

/**
 * Unlocking with the phone's own lock: fingerprint or face if set up, otherwise the
 * phone's PIN, pattern or password. There's no separate app PIN to forget.
 *
 * Must be created in the activity's onCreate; results arrive through [onResult],
 * also after a rotation while the prompt was open.
 */
class AppLock(private val activity: FragmentActivity, onResult: (success: Boolean) -> Unit) {

    private val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)

            // Cancelled, too many attempts, or no lock available. A single unrecognised
            // fingerprint isn't an error; the prompt stays open for another try.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
        }
    )

    /** Whether the phone has a screen lock to unlock with. */
    fun canLock(): Boolean {
        if (BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) return true
        return activity.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
    }

    fun authenticate(title: String) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}

enum class LockAction { UNLOCK, ENABLE, DISABLE }

/** Lock state for this app session: survives rotation and language changes, not process death. */
class LockSession : ViewModel() {
    var locked by mutableStateOf(false)
    var unlockedThisSession = false
    var backgroundedAt: Long? = null

    /** What the open prompt is for, so its result is handled even after a rotation. */
    var pendingAction: LockAction? = null

    /** Show the prompt on its own when the lock appears, but not again after a cancel. */
    var autoPrompt = true
}
