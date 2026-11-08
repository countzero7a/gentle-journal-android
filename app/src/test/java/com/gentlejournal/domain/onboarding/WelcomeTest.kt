package com.gentlejournal.domain.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeTest {

    @Test
    fun aNewJournalSeesTheWelcome() {
        assertTrue(Welcome.shouldShow(welcomeDone = false, entryCount = 0))
    }

    @Test
    fun existingJournalsSkipIt() {
        assertFalse(Welcome.shouldShow(welcomeDone = false, entryCount = 3))
    }

    @Test
    fun itIsShownOnlyOnce() {
        assertFalse(Welcome.shouldShow(welcomeDone = true, entryCount = 0))
    }
}
