package com.vincent.voicedrop.language

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

fun Context.withAppLanguage(prefs: SharedPreferences): Context {
    val lang = LanguagePreference.get(prefs)
    val locale = Locale.forLanguageTag(lang.code)
    val config = Configuration(resources.configuration).also { it.setLocale(locale) }
    return createConfigurationContext(config)
}
