package com.dopamind.app.core.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dopamind.app.feature.profile.data.LanguagePreference

/**
 * Applies the user's language choice via AndroidX's per-app language API.
 * This persists automatically across app restarts (backed by the platform
 * LocaleManager on API 33+, or AppCompat's own storage below that) — so it
 * only needs to be called when the preference actually changes, not on
 * every app start.
 *
 * On Android 13+ the change applies immediately, system-wide for this app.
 * On Android 12 and below, MainActivity extends the plain ComponentActivity
 * (not AppCompatActivity, to keep the manifest theme as a platform
 * Material theme rather than forcing a Theme.AppCompat parent), so the new
 * locale takes effect the next time the app is opened rather than
 * instantly — a normal, common pattern for locale-switch settings.
 */
object LocaleController {
    fun apply(preference: LanguagePreference) {
        val locales = when (preference) {
            LanguagePreference.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            LanguagePreference.IT -> LocaleListCompat.forLanguageTags("it")
            LanguagePreference.EN -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
