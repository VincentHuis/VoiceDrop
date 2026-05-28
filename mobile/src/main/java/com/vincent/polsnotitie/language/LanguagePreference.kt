package com.vincent.polsnotitie.language

import android.content.SharedPreferences

private const val KEY = "app_language"

object LanguagePreference {
    fun get(prefs: SharedPreferences): AppLanguage =
        AppLanguage.fromCode(prefs.getString(KEY, "nl") ?: "nl")

    fun set(prefs: SharedPreferences, lang: AppLanguage) =
        prefs.edit().putString(KEY, lang.code).apply()
}
