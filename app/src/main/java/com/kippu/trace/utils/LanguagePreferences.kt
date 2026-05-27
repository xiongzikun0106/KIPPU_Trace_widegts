package com.kippu.trace.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class LanguageMode { SYSTEM, CHINESE, ENGLISH, JAPANESE }

object LanguagePreferences {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE_MODE = "language_mode"

    fun getLanguageMode(context: Context): LanguageMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_LANGUAGE_MODE, LanguageMode.SYSTEM.name) ?: LanguageMode.SYSTEM.name
        return try {
            LanguageMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            LanguageMode.SYSTEM
        }
    }

    fun setLanguageMode(context: Context, mode: LanguageMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_MODE, mode.name)
            .commit()
        applyLanguage(mode)
    }

    fun applyLanguage(mode: LanguageMode) {
        val localeTag = when (mode) {
            LanguageMode.SYSTEM -> ""
            LanguageMode.CHINESE -> "zh"
            LanguageMode.ENGLISH -> "en"
            LanguageMode.JAPANESE -> "ja"
        }
        val appLocale: LocaleListCompat = if (localeTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun languageModeLabel(mode: LanguageMode, context: Context): String = when (mode) {
        LanguageMode.SYSTEM -> context.getString(com.kippu.trace.R.string.follow_system)
        LanguageMode.CHINESE -> "简体中文"
        LanguageMode.ENGLISH -> "English"
        LanguageMode.JAPANESE -> "日本語"
    }
}
