package com.gentlejournal.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.gentlejournal.R
import com.gentlejournal.domain.model.AppFont
import com.gentlejournal.domain.model.AppFontSize
import com.gentlejournal.domain.model.AppLanguage
import com.gentlejournal.domain.model.AppTheme
import java.util.Locale

// Display names for settings options, in the app language

@Composable
fun AppTheme.label(): String = stringResource(
    when (this) {
        AppTheme.SYSTEM -> R.string.theme_system
        AppTheme.MEADOW -> R.string.theme_meadow
        AppTheme.MOONLIGHT -> R.string.theme_moonlight
        AppTheme.OCEAN -> R.string.theme_ocean
        AppTheme.EMBER -> R.string.theme_ember
    }
)

@Composable
fun AppFont.label(): String = stringResource(
    when (this) {
        AppFont.DEFAULT -> R.string.font_default
        AppFont.SERIF -> R.string.font_serif
        AppFont.MONOSPACE -> R.string.font_monospace
    }
)

@Composable
fun AppFontSize.label(): String = stringResource(
    when (this) {
        AppFontSize.SMALL -> R.string.font_size_small
        AppFontSize.MEDIUM -> R.string.font_size_medium
        AppFontSize.LARGE -> R.string.font_size_large
        AppFontSize.EXTRA_LARGE -> R.string.font_size_extra_large
    }
)

/** The language's own name (e.g. "Deutsch"), or "System default" in the app language. */
@Composable
fun AppLanguage.nativeLabel(): String =
    if (this == AppLanguage.SYSTEM) stringResource(R.string.language_system) else nativeName

/** The language's name in the app language (e.g. "German" in English), or null for SYSTEM. */
@Composable
fun AppLanguage.localizedName(): String? {
    if (this == AppLanguage.SYSTEM) return null
    val uiLocale: Locale = LocalConfiguration.current.locales.get(0)
    return Locale.forLanguageTag(code).getDisplayLanguage(uiLocale).replaceFirstChar { it.titlecase(uiLocale) }
}
