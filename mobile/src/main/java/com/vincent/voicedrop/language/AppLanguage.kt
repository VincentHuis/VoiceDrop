package com.vincent.voicedrop.language

enum class AppLanguage(val code: String, val locale: String, val displayName: String) {
    DUTCH("nl",   "nl-NL", "Nederlands"),
    GERMAN("de",  "de-DE", "Deutsch"),
    ENGLISH("en", "en-GB", "English"),
    FRENCH("fr",   "fr-FR", "Français"),
    SPANISH("es",  "es-ES", "Español"),
    KOREAN("ko",   "ko-KR", "한국어"),
    JAPANESE("ja", "ja-JP", "日本語");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: DUTCH
    }
}
