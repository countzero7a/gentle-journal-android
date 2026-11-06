package com.gentlejournal.domain.format

import com.gentlejournal.domain.format.FormatKind.BOLD
import com.gentlejournal.domain.format.FormatKind.BULLET
import com.gentlejournal.domain.format.FormatKind.ITALIC
import com.gentlejournal.domain.format.FormatKind.MARKER
import com.gentlejournal.domain.format.FormatKind.NUMBER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLiteTest {

    private fun rendered(text: String) = MarkdownLite.render(text)

    private fun spans(text: String, kind: FormatKind) =
        rendered(text).spans.filter { it.kind == kind }.map { it.start until it.end }

    @Test
    fun boldAndItalicHideTheirMarkers() {
        val r = rendered("a **b** *c*")
        assertEquals("a b c", r.text)
        assertEquals(listOf(2 until 3), spans("a **b** *c*", BOLD))
        assertEquals(listOf(4 until 5), spans("a **b** *c*", ITALIC))
    }

    @Test
    fun editorSeesTheMarkers() {
        val parsed = MarkdownLite.parse("a **b**")
        assertTrue(FormatSpan(2, 4, MARKER) in parsed)
        assertTrue(FormatSpan(5, 7, MARKER) in parsed)
        assertTrue(FormatSpan(4, 5, BOLD) in parsed)
    }

    @Test
    fun threeAsterisksAreBoldAndItalic() {
        val r = rendered("***x***")
        assertEquals("x", r.text)
        assertEquals(listOf(0 until 1), spans("***x***", BOLD))
        assertEquals(listOf(0 until 1), spans("***x***", ITALIC))
    }

    @Test
    fun bulletsBecomeDots() {
        val r = rendered("- one\n- two")
        assertEquals("• one\n• two", r.text)
        assertEquals(listOf(0 until 1, 6 until 7), spans("- one\n- two", BULLET))
    }

    @Test
    fun asteriskBulletWithItalicText() {
        val r = rendered("* *a*")
        assertEquals("• a", r.text)
        assertEquals(listOf(2 until 3), spans("* *a*", ITALIC))
    }

    @Test
    fun numberedItemsKeepTheirNumbers() {
        val text = "1. one\n12) two"
        assertEquals(text, rendered(text).text)
        assertEquals(listOf(0 until 2, 7 until 10), spans(text, NUMBER))
    }

    @Test
    fun ordinaryAsterisksAreLeftAlone() {
        for (text in listOf("2 * 3 * 4", "a ** b", "**", "**open", "close**", "**a\nb**", "-no space", "1.no space")) {
            val r = rendered(text)
            assertEquals(text, r.text)
            assertTrue("$text should have no formatting", r.spans.isEmpty())
        }
    }
}
