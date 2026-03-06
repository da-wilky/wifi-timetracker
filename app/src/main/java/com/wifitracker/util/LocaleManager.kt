package com.wifitracker.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("wifi_tracker_prefs", Context.MODE_PRIVATE)

    fun setLanguage(languageCode: String) {
        prefs.edit {
            putString(PREF_LANGUAGE, languageCode)
        }
    }

    fun getCurrentLanguage(): String {
        return prefs.getString(PREF_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun attachBaseContext(base: Context): Context {
        val languageCode = base.getSharedPreferences("wifi_tracker_prefs", Context.MODE_PRIVATE)
            .getString(PREF_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)

        return base.createConfigurationContext(config)
    }

    companion object {
        private const val PREF_LANGUAGE = "app_language"
        private const val DEFAULT_LANGUAGE = "en"
    }
}
