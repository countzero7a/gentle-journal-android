package com.gentlejournal.ui.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.gentlejournal.domain.model.AppLanguage
import java.util.Locale

/**
 * Applies the in-app language choice.
 *
 * Only MainActivity's own context picks up the language (in attachBaseContext).
 * Anything that resolves strings from the Application context, a receiver or a
 * widget must go through [localizedContext] to get the same language.
 */
object LocaleHelper {

    /** The device's own locale, unaffected by Locale.setDefault(). */
    fun systemLocale(): Locale = Resources.getSystem().configuration.locales.get(0)

    fun localeFor(language: AppLanguage): Locale =
        if (language == AppLanguage.SYSTEM) systemLocale() else Locale.forLanguageTag(language.code)

    fun localizedContext(context: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(localeFor(language))
        return context.createConfigurationContext(config)
    }

    /** Makes date formatting follow the chosen language, and restores the device locale for SYSTEM. */
    fun applyDefaultLocale(language: AppLanguage) {
        Locale.setDefault(localeFor(language))
    }
}
