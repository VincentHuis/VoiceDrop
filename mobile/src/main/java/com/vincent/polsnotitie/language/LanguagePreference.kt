package com.vincent.polsnotitie.language

import android.content.SharedPreferences
import java.util.Locale

private const val KEY = "app_language"

object LanguagePreference {
    fun get(prefs: SharedPreferences): AppLanguage {
        val stored = prefs.getString(KEY, null)
        if (stored != null) return AppLanguage.fromCode(stored)
        val systemCode = Locale.getDefault().language
        return AppLanguage.entries.firstOrNull { it.code == systemCode } ?: AppLanguage.ENGLISH
    }

    fun set(prefs: SharedPreferences, lang: AppLanguage) =
        prefs.edit().putString(KEY, lang.code).apply()

    fun speechLocale(prefs: SharedPreferences): String {
        val code = prefs.getString(KEY, null) ?: get(prefs).code
        return when (code) {
            "de" -> "de-DE"
            "en" -> "en-GB"
            "fr" -> "fr-FR"
            "es" -> "es-ES"
            "ko" -> "ko-KR"
            "ja" -> "ja-JP"
            else -> "nl-NL"
        }
    }
}
