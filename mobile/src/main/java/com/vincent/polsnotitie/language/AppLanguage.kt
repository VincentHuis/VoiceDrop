package com.vincent.polsnotitie.language

enum class AppLanguage(val code: String, val locale: String, val displayName: String) {
    DUTCH("nl",  "nl-NL", "Nederlands"),
    GERMAN("de", "de-DE", "Deutsch"),
    ENGLISH("en","en-GB", "English");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: DUTCH
    }
}
