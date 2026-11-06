package com.gentlejournal.domain.format

enum class FormatKind { BOLD, ITALIC, MARKER, BULLET, NUMBER }

/** A formatted range [start, end) of a text. */
data class FormatSpan(val start: Int, val end: Int, val kind: FormatKind)

/** Text with the markers removed and bullets drawn as "•", plus where the formatting goes. */
data class RenderedText(val text: String, val spans: List<FormatSpan>)

/**
 * The small Markdown subset entries can use: **bold**, *italic*, "- " bullet items
 * and "1. " numbered items. Entries are stored with the markers, so exports and
 * backups keep the formatting; the editor shows the markers faded and everything
 * else hides them.
 */
object MarkdownLite {

    internal val BULLET_ITEM = Regex("""^([ \t]*)([-*+]) """)
    internal val NUMBERED_ITEM = Regex("""^([ \t]*)(\d{1,3})([.)]) """)

    // Bold may contain single asterisks, so ***both*** is bold around *italic*
    private val BOLD = Regex("""\*\*(?=\S)(.+?)(?<=\S)\*\*(?!\*)""")
    private val ITALIC = Regex("""(?<!\*)\*(?=[^\s*])(.+?)(?<=[^\s*])\*(?!\*)""")

    // Stands in for characters already used as markers, so later patterns skip them
    private const val MASK = '\u0001'

    /** Formatting in the stored text; MARKER spans cover the asterisks. */
    fun parse(text: String): List<FormatSpan> {
        val spans = mutableListOf<FormatSpan>()
        var lineStart = 0
        for (line in text.split('\n')) {
            parseLine(line, lineStart, spans)
            lineStart += line.length + 1
        }
        return spans
    }

    /** The text as it should be shown outside the editor. */
    fun render(text: String): RenderedText {
        val spans = parse(text)
        val hidden = BooleanArray(text.length)
        val bulletAt = BooleanArray(text.length)
        for (span in spans) {
            when (span.kind) {
                FormatKind.MARKER -> for (i in span.start until span.end) hidden[i] = true
                FormatKind.BULLET -> bulletAt[span.start] = true
                else -> Unit
            }
        }

        // outIndex[i] = where raw position i lands in the output
        val outIndex = IntArray(text.length + 1)
        val out = StringBuilder(text.length)
        for (i in text.indices) {
            outIndex[i] = out.length
            if (hidden[i]) continue
            out.append(if (bulletAt[i]) '•' else text[i])
        }
        outIndex[text.length] = out.length

        val outSpans = spans
            .filter { it.kind != FormatKind.MARKER }
            .map { FormatSpan(outIndex[it.start], outIndex[it.end], it.kind) }
            .filter { it.end > it.start }
        return RenderedText(out.toString(), outSpans)
    }

    private fun parseLine(line: String, offset: Int, spans: MutableList<FormatSpan>) {
        val masked = line.toCharArray()

        val bullet = BULLET_ITEM.find(line)
        val numbered = if (bullet == null) NUMBERED_ITEM.find(line) else null
        when {
            bullet != null -> {
                val marker = bullet.groups[2]!!.range
                spans += FormatSpan(offset + marker.first, offset + marker.last + 1, FormatKind.BULLET)
                for (i in 0..bullet.range.last) masked[i] = MASK
            }
            numbered != null -> {
                val number = numbered.groups[2]!!.range.first
                val dot = numbered.groups[3]!!.range.last
                spans += FormatSpan(offset + number, offset + dot + 1, FormatKind.NUMBER)
                for (i in 0..numbered.range.last) masked[i] = MASK
            }
        }

        for (match in BOLD.findAll(String(masked))) {
            val s = match.range.first
            val e = match.range.last + 1
            spans += FormatSpan(offset + s, offset + s + 2, FormatKind.MARKER)
            spans += FormatSpan(offset + e - 2, offset + e, FormatKind.MARKER)
            spans += FormatSpan(offset + s + 2, offset + e - 2, FormatKind.BOLD)
            masked[s] = MASK; masked[s + 1] = MASK; masked[e - 2] = MASK; masked[e - 1] = MASK
        }

        for (match in ITALIC.findAll(String(masked))) {
            val s = match.range.first
            val e = match.range.last + 1
            spans += FormatSpan(offset + s, offset + s + 1, FormatKind.MARKER)
            spans += FormatSpan(offset + e - 1, offset + e, FormatKind.MARKER)
            spans += FormatSpan(offset + s + 1, offset + e - 1, FormatKind.ITALIC)
        }
    }
}
