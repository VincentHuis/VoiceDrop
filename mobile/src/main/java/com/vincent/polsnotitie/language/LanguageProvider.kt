package com.vincent.polsnotitie.language

import android.content.SharedPreferences
import com.vincent.polsnotitie.language.configs.DeLanguageConfig
import com.vincent.polsnotitie.language.configs.EnLanguageConfig
import com.vincent.polsnotitie.language.configs.EsLanguageConfig
import com.vincent.polsnotitie.language.configs.FrLanguageConfig
import com.vincent.polsnotitie.language.configs.JaLanguageConfig
import com.vincent.polsnotitie.language.configs.KoLanguageConfig
import com.vincent.polsnotitie.language.configs.NlLanguageConfig

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
