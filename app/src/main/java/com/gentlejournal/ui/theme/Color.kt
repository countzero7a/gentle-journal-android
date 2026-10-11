package com.gentlejournal.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Custom color palette for the journal.
 * Provided via CompositionLocal so all screens resolve to the active theme.
 */
data class JournalColorPalette(
    val Background: Color,
    val Surface: Color,
    val SurfaceAlt: Color,
    val Border: Color,
    val Text: Color,
    val TextMuted: Color,
    val Accent: Color,
    val AccentLight: Color,
    val AccentDark: Color,
    val Warm: Color,
    val WarmLight: Color,
    val Danger: Color,
    val DangerLight: Color,
    val White: Color = Color(0xFFFFFFFF),
    val Transparent: Color = Color.Transparent,
    val isDark: Boolean = false
)

// Meadow (warm beige and sage green -- the original)
val MeadowPalette = JournalColorPalette(
    Background  = Color(0xFFFAF8F3),
    Surface     = Color(0xFFFFFFFF),
    SurfaceAlt  = Color(0xFFF5F1EA),
    Border      = Color(0xFFE5DED2),
    Text        = Color(0xFF2C2921),
    TextMuted   = Color(0xFF8A8279),
    Accent      = Color(0xFF5B6A4E),
    AccentLight = Color(0xFFE8E2D6),
    AccentDark  = Color(0xFF3A4530),
    Warm        = Color(0xFFC4956A),
    WarmLight   = Color(0xFFF5EDE4),
    Danger      = Color(0xFFB55B4C),
    DangerLight = Color(0xFFFAE8E5),
    isDark      = false
)

// Moonlight (soft dark -- deep charcoal with muted gold)
val MoonlightPalette = JournalColorPalette(
    Background  = Color(0xFF1A1B20),
    Surface     = Color(0xFF242530),
    SurfaceAlt  = Color(0xFF2C2D3A),
    Border      = Color(0xFF3D3E4E),
    Text        = Color(0xFFE8E6E1),
    TextMuted   = Color(0xFF9896A3),
    Accent      = Color(0xFFCDB87D),
    AccentLight = Color(0xFF3A3729),
    AccentDark  = Color(0xFFE8D9A6),
    Warm        = Color(0xFFD4A574),
    WarmLight   = Color(0xFF352E25),
    Danger      = Color(0xFFD47B6E),
    DangerLight = Color(0xFF3D2A27),
    isDark      = true
)

// Ocean (cool light -- soft blues and seafoam)
val OceanPalette = JournalColorPalette(
    Background  = Color(0xFFF3F7FA),
    Surface     = Color(0xFFFFFFFF),
    SurfaceAlt  = Color(0xFFEBF1F6),
    Border      = Color(0xFFD2DDE6),
    Text        = Color(0xFF1E2D3A),
    TextMuted   = Color(0xFF6E8494),
    Accent      = Color(0xFF3D7A8A),
    AccentLight = Color(0xFFDCECF0),
    AccentDark  = Color(0xFF285663),
    Warm        = Color(0xFF6AABAA),
    WarmLight   = Color(0xFFE4F2F1),
    Danger      = Color(0xFFC45E5E),
    DangerLight = Color(0xFFFAE5E5),
    isDark      = false
)

// Ember (warm dark -- deep browns with amber glow)
val EmberPalette = JournalColorPalette(
    Background  = Color(0xFF1C1614),
    Surface     = Color(0xFF2A211E),
    SurfaceAlt  = Color(0xFF332925),
    Border      = Color(0xFF4A3B34),
    Text        = Color(0xFFEDE5DF),
    TextMuted   = Color(0xFFA89889),
    Accent      = Color(0xFFD48A4C),
    AccentLight = Color(0xFF3D2E1F),
    AccentDark  = Color(0xFFE8A96A),
    Warm        = Color(0xFFCC7A4A),
    WarmLight   = Color(0xFF3A281C),
    Danger      = Color(0xFFD46B5B),
    DangerLight = Color(0xFF3D2420),
    isDark      = true
)

val LocalJournalColors = staticCompositionLocalOf { MeadowPalette }
