package com.gentlejournal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlejournal.R
import com.gentlejournal.ui.theme.LocalJournalColors

/** Bold, italic, bulleted and numbered list buttons for the entry text. */
@Composable
fun FormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onBullets: () -> Unit,
    onNumbers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ToolbarButton("B", stringResource(R.string.format_bold), onBold, fontWeight = FontWeight.Bold)
        ToolbarButton("I", stringResource(R.string.format_italic), onItalic, fontStyle = FontStyle.Italic)
        ToolbarButton("•", stringResource(R.string.format_bullets), onBullets)
        ToolbarButton("1.", stringResource(R.string.format_numbers), onNumbers)
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontStyle: FontStyle = FontStyle.Normal
) {
    val colors = LocalJournalColors.current
    // Not focusable in touch mode, so the text field keeps its focus and selection
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        shape = RoundedCornerShape(8.dp),
        color = colors.Surface,
        contentColor = colors.Text,
        border = BorderStroke(1.dp, colors.Border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, fontSize = 15.sp, fontWeight = fontWeight, fontStyle = fontStyle)
        }
    }
}
