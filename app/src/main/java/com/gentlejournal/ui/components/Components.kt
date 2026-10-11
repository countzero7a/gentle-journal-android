package com.gentlejournal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gentlejournal.R
import com.gentlejournal.ui.theme.JournalColors

// ── Tag Chip (emotion / action selector) ────────────────────────────

@Composable
fun TagChip(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    val bgColor by animateColorAsState(
        if (selected) JournalColors.AccentLight else JournalColors.Background,
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        if (selected) JournalColors.Accent else JournalColors.Border,
        label = "chipBorder"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (small) 10.dp else 14.dp,
                vertical = if (small) 4.dp else 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (small) 4.dp else 6.dp)
        ) {
            Text(
                text = emoji,
                fontSize = if (small) 14.sp else 18.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (small) 12.sp else 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) JournalColors.AccentDark else JournalColors.TextMuted
                )
            )
        }
    }
}

// ── Add Custom chip button ──────────────────────────────────────────

@Composable
fun AddCustomChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, JournalColors.Accent)
    ) {
        Text(
            text = stringResource(R.string.entry_custom),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                color = JournalColors.Accent,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

// ── Section label ───────────────────────────────────────────────────

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    Row(
        modifier = modifier.padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp
                )
            )
        }
    }
}

// ── Card wrapper ────────────────────────────────────────────────────

@Composable
fun JournalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderStart: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = JournalColors.Surface,
        border = BorderStroke(1.dp, JournalColors.Border)
    ) {
        Row {
            if (borderStart != null) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(borderStart)
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

// ── Empty state ─────────────────────────────────────────────────────

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = JournalColors.TextMuted),
            textAlign = TextAlign.Center
        )
    }
}

// ── Insight type label ──────────────────────────────────────────────

@Composable
fun InsightTypeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontStyle = FontStyle.Italic
        ),
        modifier = Modifier.padding(top = 4.dp)
    )
}
