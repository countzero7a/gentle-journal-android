package com.gentlejournal.domain.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {

    private val now = 1_000_000L

    @Test
    fun aDisabledLockNeverAsks() {
        assertFalse(LockPolicy.mustUnlock(enabled = false, unlockedThisSession = false, backgroundedAt = null, now = now))
    }

    @Test
    fun asksOnceWhenTheAppStarts() {
        assertTrue(LockPolicy.mustUnlock(enabled = true, unlockedThisSession = false, backgroundedAt = null, now = now))
    }

    @Test
    fun staysOpenWhileInUse() {
        assertFalse(LockPolicy.mustUnlock(enabled = true, unlockedThisSession = true, backgroundedAt = null, now = now))
    }

    @Test
    fun aShortTripAwayDoesNotLock() {
        assertFalse(LockPolicy.mustUnlock(true, true, backgroundedAt = now - 59_000, now = now))
    }

    @Test
    fun comingBackAfterAMinuteLocks() {
        assertTrue(LockPolicy.mustUnlock(true, true, backgroundedAt = now - 61_000, now = now))
    }
}
