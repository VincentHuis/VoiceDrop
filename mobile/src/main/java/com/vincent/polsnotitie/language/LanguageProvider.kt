package com.vincent.polsnotitie.language

import android.content.SharedPreferences
import com.vincent.polsnotitie.language.configs.DeLanguageConfig
import com.vincent.polsnotitie.language.configs.EnLanguageConfig
import com.vincent.polsnotitie.language.configs.NlLanguageConfig

object LanguageProvider {
    fun get(prefs: SharedPreferences): LanguageConfig =
        when (LanguagePreference.get(prefs)) {
            AppLanguage.GERMAN  -> DeLanguageConfig
            AppLanguage.ENGLISH -> EnLanguageConfig
            AppLanguage.DUTCH   -> NlLanguageConfig
        }
}
