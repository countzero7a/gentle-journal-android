package com.gentlejournal.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.gentlejournal.domain.format.FormatKind
import com.gentlejournal.domain.format.FormatSpan
import com.gentlejournal.domain.format.MarkdownLite

/** An entry's text for display: formatted, with the markers hidden. */
fun formattedEntryText(raw: String, accent: Color): AnnotatedString {
    val rendered = MarkdownLite.render(raw)
    return styled(rendered.text, rendered.spans, muted = accent, accent = accent)
}

/**
 * Formats the entry while it's being written. The text itself is unchanged, so the
 * cursor maps one to one; the markers stay visible but faded.
 */
data class FormattingVisualTransformation(val muted: Color, val accent: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        return TransformedText(styled(raw, MarkdownLite.parse(raw), muted, accent), OffsetMapping.Identity)
    }
}

private fun styled(text: String, spans: List<FormatSpan>, muted: Color, accent: Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        for (span in spans) {
            val style = when (span.kind) {
                FormatKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                FormatKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                FormatKind.MARKER -> SpanStyle(color = muted)
                FormatKind.BULLET, FormatKind.NUMBER -> SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)
            }
            addStyle(style, span.start, span.end)
        }
    }
