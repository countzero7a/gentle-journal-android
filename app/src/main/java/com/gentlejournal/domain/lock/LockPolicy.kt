package com.gentlejournal.domain.lock

/** When the journal has to be unlocked before it's shown. */
object LockPolicy {

    /** Time away (share sheet, file picker, a quick app switch) that doesn't lock again. */
    const val GRACE_MILLIS = 60_000L

    /**
     * [enabled] should already account for the phone having a screen lock: without one
     * there's nothing to unlock with, and locking would shut the user out.
     */
    fun mustUnlock(enabled: Boolean, unlockedThisSession: Boolean, backgroundedAt: Long?, now: Long): Boolean {
        if (!enabled) return false
        if (!unlockedThisSession) return true
        if (backgroundedAt == null) return false
        return now - backgroundedAt > GRACE_MILLIS
    }
}
