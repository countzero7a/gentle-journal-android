package com.gentlejournal.domain.format

import kotlin.math.max
import kotlin.math.min

/** Text and selection after an edit. */
data class EditResult(val text: String, val selectionStart: Int, val selectionEnd: Int)

/**
 * What the formatting toolbar and the Enter key do to the entry text, as plain
 * functions of the text and selection.
 */
object FormattingActions {

    fun toggleBold(text: String, selectionStart: Int, selectionEnd: Int) =
        toggleInline(text, selectionStart, selectionEnd, width = 2)

    fun toggleItalic(text: String, selectionStart: Int, selectionEnd: Int) =
        toggleInline(text, selectionStart, selectionEnd, width = 1)

    fun toggleBullets(text: String, selectionStart: Int, selectionEnd: Int) =
        toggleList(text, selectionStart, selectionEnd, numbered = false)

    fun toggleNumbers(text: String, selectionStart: Int, selectionEnd: Int) =
        toggleList(text, selectionStart, selectionEnd, numbered = true)

    /**
     * Called for every change in the editor. If the change was Enter pressed inside a
     * list item, the new line gets the next marker ("- ", or "3. " after "2. "); Enter
     * on an empty item removes it and ends the list. Returns null for any other change.
     */
    fun continueList(
        oldText: String, oldSelectionStart: Int, oldSelectionEnd: Int,
        newText: String, newSelectionStart: Int, newSelectionEnd: Int
    ): EditResult? {
        val p = oldSelectionStart
        if (oldSelectionStart != oldSelectionEnd || newSelectionStart != newSelectionEnd) return null
        if (newSelectionStart != p + 1 || newText.length != oldText.length + 1 || p > oldText.length) return null
        if (newText[p] != '\n' || !newText.startsWith(oldText.substring(0, p)) || !newText.endsWith(oldText.substring(p))) return null

        val lineStart = oldText.lastIndexOf('\n', p - 1) + 1
        val lineEnd = oldText.indexOf('\n', p).let { if (it == -1) oldText.length else it }
        val beforeCursor = oldText.substring(lineStart, p)
        val afterCursor = oldText.substring(p, lineEnd)

        val bullet = MarkdownLite.BULLET_ITEM.find(beforeCursor)
        val numbered = if (bullet == null) MarkdownLite.NUMBERED_ITEM.find(beforeCursor) else null
        val item = bullet ?: numbered ?: return null

        // Enter on an empty item: drop its marker and don't start another one
        if (item.range.last + 1 == beforeCursor.length && afterCursor.isBlank()) {
            val text = oldText.substring(0, lineStart) + oldText.substring(p)
            return EditResult(text, lineStart, lineStart)
        }

        val nextMarker = if (bullet != null) {
            bullet.value
        } else {
            val match = numbered!!
            val number = match.groups[2]!!.value.toInt() + 1
            "${match.groups[1]!!.value}$number${match.groups[3]!!.value} "
        }
        val text = newText.substring(0, p + 1) + nextMarker + newText.substring(p + 1)
        val cursor = p + 1 + nextMarker.length
        return EditResult(text, cursor, cursor)
    }

    private fun toggleInline(text: String, selectionStart: Int, selectionEnd: Int, width: Int): EditResult {
        val start = min(selectionStart, selectionEnd)
        val end = max(selectionStart, selectionEnd)

        if (start == end) {
            // No selection: format the word under the cursor, or start an empty pair
            var a = start
            var b = start
            while (a > 0 && text[a - 1].isLetterOrDigit()) a--
            while (b < text.length && text[b].isLetterOrDigit()) b++
            if (a == b) {
                val marker = "*".repeat(width)
                val newText = text.substring(0, start) + marker + marker + text.substring(start)
                return EditResult(newText, start + width, start + width)
            }
            val edits = inlineEdits(text, a, b, width)
            val cursor = mapPosition(start, edits, insertionAtPositionGoesBefore = true)
            return EditResult(applyEdits(text, edits), cursor, cursor)
        }

        return apply(text, start, end, inlineEdits(text, start, end, width))
    }

    private fun inlineEdits(text: String, start: Int, end: Int, width: Int): List<Edit> {
        val segments = lineSegments(text, start, end)
        val marker = "*".repeat(width)
        val removing = segments.isNotEmpty() &&
            segments.all { isWrapped(text, it.first, it.last + 1, width) || containsWrapped(text, it.first, it.last + 1, width) }

        val edits = mutableListOf<Edit>()
        for (segment in segments) {
            val a = segment.first
            val b = segment.last + 1
            when {
                // Remove the innermost markers just outside the selection
                removing && isWrapped(text, a, b, width) -> {
                    edits += Edit(a - width, width, "")
                    edits += Edit(b, width, "")
                }
                // The selection includes the markers themselves
                removing -> {
                    edits += Edit(a, width, "")
                    edits += Edit(b - width, width, "")
                }
                !isWrapped(text, a, b, width) -> {
                    edits += Edit(a, 0, marker)
                    edits += Edit(b, 0, marker)
                }
            }
        }
        return edits
    }

    private fun toggleList(text: String, selectionStart: Int, selectionEnd: Int, numbered: Boolean): EditResult {
        val start = min(selectionStart, selectionEnd)
        val end = max(selectionStart, selectionEnd)
        val lines = text.split('\n')
        val lineStarts = IntArray(lines.size)
        for (i in 1 until lines.size) lineStarts[i] = lineStarts[i - 1] + lines[i - 1].length + 1

        val first = lineStarts.indexOfLast { it <= start }
        val last = lineStarts.indexOfLast { it <= end }
        val pattern = if (numbered) MarkdownLite.NUMBERED_ITEM else MarkdownLite.BULLET_ITEM
        val removing = (first..last).all { pattern.containsMatchIn(lines[it]) }

        // A numbered list continues from a numbered line just above the selection
        var number = 1
        if (numbered && first > 0) {
            MarkdownLite.NUMBERED_ITEM.find(lines[first - 1])?.let { number = it.groups[2]!!.value.toInt() + 1 }
        }

        val edits = mutableListOf<Edit>()
        for (i in first..last) {
            val existing = MarkdownLite.BULLET_ITEM.find(lines[i]) ?: MarkdownLite.NUMBERED_ITEM.find(lines[i])
            val newMarker = when {
                removing -> ""
                numbered -> "${number++}. "
                else -> "- "
            }
            edits += Edit(lineStarts[i], existing?.let { it.range.last + 1 } ?: 0, newMarker)
        }
        return apply(text, start, end, edits)
    }

    /** Non-blank parts of each line inside [start, end), without list markers. */
    private fun lineSegments(text: String, start: Int, end: Int): List<IntRange> {
        val segments = mutableListOf<IntRange>()
        var lineStart = text.lastIndexOf('\n', start - 1) + 1
        while (lineStart <= end) {
            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            val line = text.substring(lineStart, lineEnd)
            val markerLength = (MarkdownLite.BULLET_ITEM.find(line) ?: MarkdownLite.NUMBERED_ITEM.find(line))
                ?.let { it.range.last + 1 } ?: 0
            var a = max(start, lineStart + markerLength)
            var b = min(end, lineEnd)
            while (a < b && text[a].isWhitespace()) a++
            while (b > a && text[b - 1].isWhitespace()) b--
            if (a < b) segments += a until b
            if (lineEnd == text.length) break
            lineStart = lineEnd + 1
        }
        return segments
    }

    /** Whether [a, b) sits between markers of [width] (bold: 2+ asterisks, italic: an odd run). */
    private fun isWrapped(text: String, a: Int, b: Int, width: Int): Boolean {
        var before = 0
        while (a - before - 1 >= 0 && text[a - before - 1] == '*') before++
        var after = 0
        while (b + after < text.length && text[b + after] == '*') after++
        return hasMarker(before, width) && hasMarker(after, width)
    }

    /** Whether [a, b) itself starts and ends with markers of [width]. */
    private fun containsWrapped(text: String, a: Int, b: Int, width: Int): Boolean {
        if (b - a <= 2 * width) return false
        var leading = 0
        while (a + leading < b && text[a + leading] == '*') leading++
        var trailing = 0
        while (b - trailing - 1 >= a && text[b - trailing - 1] == '*') trailing++
        return hasMarker(leading, width) && hasMarker(trailing, width)
    }

    private fun hasMarker(run: Int, width: Int) = if (width == 2) run >= 2 else run % 2 == 1

    /** Replace [deleteLength] characters at [position] (in the original text) with [insert]. */
    private data class Edit(val position: Int, val deleteLength: Int, val insert: String)

    private fun applyEdits(text: String, edits: List<Edit>): String {
        val sb = StringBuilder(text)
        for (edit in edits.sortedByDescending { it.position }) {
            sb.replace(edit.position, edit.position + edit.deleteLength, edit.insert)
        }
        return sb.toString()
    }

    /** Applies the edits and keeps the selection on the text it covered. */
    private fun apply(text: String, start: Int, end: Int, edits: List<Edit>): EditResult {
        val newStart = mapPosition(start, edits, insertionAtPositionGoesBefore = true)
        val newEnd = max(newStart, mapPosition(end, edits, insertionAtPositionGoesBefore = false))
        return EditResult(applyEdits(text, edits), newStart, newEnd)
    }

    /**
     * Where position [x] of the original text ends up after the edits. An insertion
     * exactly at [x] counts as before it for a selection start and after it for an end.
     */
    private fun mapPosition(x: Int, edits: List<Edit>, insertionAtPositionGoesBefore: Boolean): Int {
        var shift = 0
        for (edit in edits.sortedBy { it.position }) {
            val delta = edit.insert.length - edit.deleteLength
            val deleteEnd = edit.position + edit.deleteLength
            when {
                edit.deleteLength == 0 && edit.position == x -> if (insertionAtPositionGoesBefore) shift += delta
                deleteEnd <= x -> shift += delta
                edit.position < x -> return edit.position + shift + edit.insert.length
                else -> return x + shift
            }
        }
        return x + shift
    }
}
