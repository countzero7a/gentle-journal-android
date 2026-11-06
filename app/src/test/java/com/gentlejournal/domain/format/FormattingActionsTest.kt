package com.gentlejournal.domain.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattingActionsTest {

    private fun assertResult(expected: String, start: Int, end: Int, actual: EditResult) {
        assertEquals(expected, actual.text)
        assertEquals("selection start", start, actual.selectionStart)
        assertEquals("selection end", end, actual.selectionEnd)
    }

    // ── Bold and italic ──────────────────────────────────────────────

    @Test
    fun boldWrapsTheSelectionAndKeepsItSelected() {
        assertResult("hello **world**", 8, 13, FormattingActions.toggleBold("hello world", 6, 11))
    }

    @Test
    fun boldAgainRemovesIt() {
        assertResult("hello world", 6, 11, FormattingActions.toggleBold("hello **world**", 8, 13))
    }

    @Test
    fun boldRemovesMarkersThatAreInsideTheSelection() {
        assertResult("hello world", 6, 11, FormattingActions.toggleBold("hello **world**", 6, 15))
    }

    @Test
    fun surroundingSpacesStayOutsideTheMarkers() {
        assertEquals("**word** next", FormattingActions.toggleBold("word next", 0, 5).text)
    }

    @Test
    fun withoutSelectionTheWordUnderTheCursorIsFormatted() {
        assertResult("hello **world**", 10, 10, FormattingActions.toggleBold("hello world", 8, 8))
    }

    @Test
    fun withoutSelectionOrWordAnEmptyPairIsStarted() {
        assertResult("hello ****", 8, 8, FormattingActions.toggleBold("hello ", 6, 6))
    }

    @Test
    fun italicInsideBoldAndBack() {
        val both = FormattingActions.toggleItalic("**x**", 2, 3)
        assertEquals("***x***", both.text)
        assertEquals("**x**", FormattingActions.toggleItalic(both.text, both.selectionStart, both.selectionEnd).text)
    }

    @Test
    fun formattingAcrossLinesWrapsEachLine() {
        assertEquals("**one**\n**two**", FormattingActions.toggleBold("one\ntwo", 0, 7).text)
    }

    @Test
    fun listMarkersStayOutsideTheFormatting() {
        assertEquals("- **item**", FormattingActions.toggleBold("- item", 0, 6).text)
    }

    // ── Typing with the toolbar, as in a word processor ──────────────

    /** Presses toolbar buttons and types character by character, like the entry screen. */
    private class Editor(var text: String = "", var cursor: Int = text.length) {
        fun press(action: (String, Int, Int) -> EditResult) = apply {
            val result = action(text, cursor, cursor)
            text = result.text
            cursor = result.selectionStart
        }

        fun type(chars: String) = apply {
            for (c in chars) {
                val typed = text.substring(0, cursor) + c + text.substring(cursor)
                val adjusted = FormattingActions.afterTyping(text, cursor, cursor, typed, cursor + 1, cursor + 1)
                text = adjusted?.text ?: typed
                cursor = adjusted?.selectionStart ?: (cursor + 1)
            }
        }
    }

    private val bold = FormattingActions::toggleBold
    private val italic = FormattingActions::toggleItalic

    @Test
    fun italicSwitchedOffAndOnAgainInsideBold() {
        // The reported sequence: B, I, a word, I off, another word, I on, a third word
        val editor = Editor().press(bold).press(italic).type("word").press(italic).type(" more").press(italic).type(" again")
        assertEquals("***word* more *again***", editor.text)

        val rendered = MarkdownLite.render(editor.text)
        assertEquals("word more again", rendered.text)
        val italic = rendered.spans.filter { it.kind == FormatKind.ITALIC }.map { it.start until it.end }
        val bold = rendered.spans.filter { it.kind == FormatKind.BOLD }.map { it.start until it.end }
        assertEquals(listOf(0 until 4, 10 until 15), italic)
        assertEquals(listOf(0 until 15), bold)
    }

    @Test
    fun boldOnTypeBoldOffType() {
        assertEquals("**bold** plain", Editor().press(bold).type("bold").press(bold).type(" plain").text)
    }

    @Test
    fun switchingOnAfterAWordKeepsTheSpaceOutside() {
        assertEquals("hello *world*", Editor("hello").press(italic).type(" world").text)
    }

    @Test
    fun pressingTwiceWithoutTypingUndoesIt() {
        val editor = Editor("hi ").press(bold).press(bold)
        assertEquals("hi ", editor.text)
        assertEquals(3, editor.cursor)
    }

    @Test
    fun switchingOffAndOnAgainContinuesTheSameFormatting() {
        assertEquals("**ab**", Editor().press(bold).type("a").press(bold).press(bold).type("b").text)
    }

    @Test
    fun adjacentFormattedWordsAreNotMerged() {
        // Between two italic words the asterisks close one word and open the next
        val editor = Editor("*a**b*", cursor = 3).type(" ")
        assertEquals("*a* *b*", editor.text)
    }

    // ── Lists ────────────────────────────────────────────────────────

    @Test
    fun bulletsAreAddedToEverySelectedLineAndRemovedAgain() {
        val added = FormattingActions.toggleBullets("one\ntwo", 0, 7)
        assertEquals("- one\n- two", added.text)
        assertEquals("one\ntwo", FormattingActions.toggleBullets(added.text, 0, added.text.length).text)
    }

    @Test
    fun bulletOnTheCursorLineMovesTheCursorAlong() {
        assertResult("first\n- second", 11, 11, FormattingActions.toggleBullets("first\nsecond", 9, 9))
    }

    @Test
    fun numberingContinuesFromTheLineAbove() {
        assertEquals("1. a\n2. b", FormattingActions.toggleNumbers("1. a\nb", 6, 6).text)
    }

    @Test
    fun numbersReplaceBullets() {
        assertEquals("1. a\n2. b", FormattingActions.toggleNumbers("- a\n- b", 0, 7).text)
    }

    @Test
    fun aListCanStartInAnEmptyEntry() {
        assertResult("- ", 2, 2, FormattingActions.toggleBullets("", 0, 0))
    }

    // ── Enter inside lists ───────────────────────────────────────────

    @Test
    fun enterContinuesABulletList() {
        assertResult("- a\n- ", 6, 6, FormattingActions.continueList("- a", 3, 3, "- a\n", 4, 4)!!)
    }

    @Test
    fun enterContinuesANumberedList() {
        assertResult("1. a\n2. ", 8, 8, FormattingActions.continueList("1. a", 4, 4, "1. a\n", 5, 5)!!)
    }

    @Test
    fun enterOnAnEmptyItemEndsTheList() {
        assertResult("- a\n", 4, 4, FormattingActions.continueList("- a\n- ", 6, 6, "- a\n- \n", 7, 7)!!)
    }

    @Test
    fun enterInOrdinaryTextIsLeftAlone() {
        assertNull(FormattingActions.continueList("abc", 3, 3, "abc\n", 4, 4))
    }

    @Test
    fun otherChangesAreLeftAlone() {
        assertNull(FormattingActions.continueList("- a", 3, 3, "- ab", 4, 4))
        assertNull(FormattingActions.continueList("- a", 3, 3, "- a\nxy", 6, 6))
    }
}
