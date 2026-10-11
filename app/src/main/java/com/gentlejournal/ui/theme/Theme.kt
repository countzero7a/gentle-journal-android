package com.gentlejournal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.gentlejournal.domain.model.AppFont
import com.gentlejournal.domain.model.AppFontSize
import com.gentlejournal.domain.model.AppTheme

private fun buildColorScheme(p: JournalColorPalette) = if (p.isDark) {
    darkColorScheme(
        primary = p.Accent, onPrimary = p.White,
        primaryContainer = p.AccentLight, onPrimaryContainer = p.AccentDark,
        secondary = p.Warm, onSecondary = p.White, secondaryContainer = p.WarmLight,
        background = p.Background, onBackground = p.Text,
        surface = p.Surface, onSurface = p.Text,
        surfaceVariant = p.SurfaceAlt, onSurfaceVariant = p.TextMuted,
        outline = p.Border, error = p.Danger, onError = p.White, errorContainer = p.DangerLight,
    )
} else {
    lightColorScheme(
        primary = p.Accent, onPrimary = p.White,
        primaryContainer = p.AccentLight, onPrimaryContainer = p.AccentDark,
        secondary = p.Warm, onSecondary = p.White, secondaryContainer = p.WarmLight,
        background = p.Background, onBackground = p.Text,
        surface = p.Surface, onSurface = p.Text,
        surfaceVariant = p.SurfaceAlt, onSurfaceVariant = p.TextMuted,
        outline = p.Border, error = p.Danger, onError = p.White, errorContainer = p.DangerLight,
    )
}

fun resolvePalette(theme: AppTheme, isSystemDark: Boolean): JournalColorPalette {
    return when (theme) {
        AppTheme.SYSTEM -> if (isSystemDark) MoonlightPalette else MeadowPalette
        AppTheme.MEADOW -> MeadowPalette
        AppTheme.MOONLIGHT -> MoonlightPalette
        AppTheme.OCEAN -> OceanPalette
        AppTheme.EMBER -> EmberPalette
    }
}

@Composable
fun GentleJournalTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    appFont: AppFont = AppFont.DEFAULT,
    appFontSize: AppFontSize = AppFontSize.MEDIUM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val palette = resolvePalette(appTheme, isSystemDark)
    val colorScheme = buildColorScheme(palette)
    val typography = buildTypography(appFont, appFontSize)

    CompositionLocalProvider(LocalJournalColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
