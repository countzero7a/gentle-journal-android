package com.gentlejournal.domain.onboarding

/** The welcome pages are for new journals only; anyone who already has entries skips them. */
object Welcome {
    fun shouldShow(welcomeDone: Boolean, entryCount: Int): Boolean = !welcomeDone && entryCount == 0
}
