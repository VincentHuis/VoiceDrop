package com.vincent.voicedrop.language

import android.content.SharedPreferences
import com.vincent.voicedrop.language.configs.DeLanguageConfig
import com.vincent.voicedrop.language.configs.EnLanguageConfig
import com.vincent.voicedrop.language.configs.EsLanguageConfig
import com.vincent.voicedrop.language.configs.FrLanguageConfig
import com.vincent.voicedrop.language.configs.JaLanguageConfig
import com.vincent.voicedrop.language.configs.KoLanguageConfig
import com.vincent.voicedrop.language.configs.NlLanguageConfig

object LanguageProvider {
    fun get(prefs: SharedPreferences): LanguageConfig =
        when (LanguagePreference.get(prefs)) {
            AppLanguage.GERMAN   -> DeLanguageConfig
            AppLanguage.ENGLISH  -> EnLanguageConfig
            AppLanguage.FRENCH   -> FrLanguageConfig
            AppLanguage.SPANISH  -> EsLanguageConfig
            AppLanguage.KOREAN   -> KoLanguageConfig
            AppLanguage.JAPANESE -> JaLanguageConfig
            AppLanguage.DUTCH    -> NlLanguageConfig
        }
}
