package com.vincent.voicedrop.language.configs

import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.language.AppLanguage
import com.vincent.voicedrop.language.LanguageConfig
import com.vincent.voicedrop.language.PlacePatternConfig
import com.vincent.voicedrop.language.TimeParserConfig
import java.util.Calendar

object FrLanguageConfig : LanguageConfig {

    override val language = AppLanguage.FRENCH

    private val numberWords = mapOf(
        "un" to 1, "une" to 1, "deux" to 2, "trois" to 3, "quatre" to 4,
        "cinq" to 5, "six" to 6, "sept" to 7, "huit" to 8, "neuf" to 9,
        "dix" to 10, "onze" to 11, "douze" to 12
    )
    private val numAlt = numberWords.keys.joinToString("|")

    override val categoryKeywords = mapOf(
        Category.BOODSCHAPPEN  to listOf("courses", "course", "epicerie", "supermarche", "commissions"),
        Category.IDEEEN        to listOf("idee", "idees"),
        Category.TODO          to listOf("tache", "taches", "todo", "to-do", "faire"),
        Category.HERINNERINGEN to listOf("rappel", "rappels", "rappelle"),
        Category.AGENDA        to listOf("agenda", "rendez-vous", "rdv", "calendrier"),
        Category.OVERIG        to emptyList()
    )

    override val placePatterns = PlacePatternConfig(
        homePatterns = listOf(
            Regex("(?<![a-z])quand je suis (?:a|à) la maison(?![a-z])"),
            Regex("(?<![a-z])(?:en arrivant )?(?:a|à) la maison(?![a-z])"),
            Regex("(?<![a-z])chez moi(?![a-z])")
        ),
        workPatterns = listOf(
            Regex("(?<![a-z])quand je suis (?:au travail|au bureau)(?![a-z])"),
            Regex("(?<![a-z])(?:en arrivant )?au travail(?![a-z])"),
            Regex("(?<![a-z])au bureau(?![a-z])")
        ),
        shopPatterns = listOf(
            Regex("(?<![a-z])quand je suis (?:au supermarche|au supermarché)(?![a-z])"),
            Regex("(?<![a-z])au supermarche(?![a-z])"),
            Regex("(?<![a-z])au supermarché(?![a-z])"),
            Regex("(?<![a-z])supermarche(?![a-z])")
        )
    )

    override val timeParser = TimeParserConfig(
        numberWords = numberWords,
        weekdays = mapOf(
            "lundi"    to Calendar.MONDAY,    "mardi"   to Calendar.TUESDAY,
            "mercredi" to Calendar.WEDNESDAY, "jeudi"   to Calendar.THURSDAY,
            "vendredi" to Calendar.FRIDAY,    "samedi"  to Calendar.SATURDAY,
            "dimanche" to Calendar.SUNDAY
        ),
        tomorrowWords         = listOf("demain"),
        dayAfterTomorrowWords = listOf("après-demain", "apres-demain"),
        todayWords            = listOf("aujourd'hui", "aujourd hui"),
        periodPatterns = listOf(
            "morning"   to Regex("(?<![a-z])(?:ce matin|du matin|matin)(?![a-z])"),
            "afternoon" to Regex("(?<![a-z])(?:cet? après-midi|cet? apres-midi|après-midi|apres-midi)(?![a-z])"),
            "evening"   to Regex("(?<![a-z])(?:ce soir|du soir|soir)(?![a-z])"),
            "night"     to Regex("(?<![a-z])(?:cette nuit|de nuit|nuit)(?![a-z])")
        ),
        todayPrefixes         = listOf("ce ", "cet ", "cette "),
        relativeHalfHourPattern = Regex("\\bdans\\s+une\\s+demi[- ]heure\\b"),
        relativeQuarterPattern  = Regex("\\bdans\\s+(\\d+|$numAlt)\\s+quarts?\\s+d.heure\\b"),
        relativeUnitPattern     = Regex("\\bdans\\s+(\\d+|$numAlt)\\s+(minutes?|heures?|jours?|semaines?)\\b"),
        unitMultipliers = mapOf(
            "minute"  to 60_000L,            "minutes"  to 60_000L,
            "heure"   to 3_600_000L,         "heures"   to 3_600_000L,
            "jour"    to 86_400_000L,         "jours"    to 86_400_000L,
            "semaine" to 7 * 86_400_000L,    "semaines" to 7 * 86_400_000L
        ),
        // "9h30" or "9:30"
        clockPattern       = Regex("\\b(\\d{1,2})[h:](\\d{2})\\b"),
        // "neuf heures et demie" → 9:30, halfHourIsBefore=false (captured number IS current hour)
        halfHourPattern    = Regex("\\b(?:à\\s+)?($numAlt|\\d{1,2})\\s+heures?\\s+et\\s+demis?e?\\b"),
        halfHourIsBefore   = false,
        // "neuf heures et quart" → 9:15
        quarterOverPattern = Regex("\\b(?:à\\s+)?($numAlt|\\d{1,2})\\s+heures?\\s+et\\s+quart\\b"),
        // "dix heures moins le quart" → 9:45 (captures next hour: 10)
        quarterToPattern   = Regex("\\b(?:à\\s+)?($numAlt|\\d{1,2})\\s+heures?\\s+moins\\s+(?:le\\s+)?quart\\b"),
        // "neuf heures" → 9:00
        hourWordPattern    = Regex("\\b(?:à\\s+)?($numAlt|\\d{1,2})\\s+heures?\\b"),
        // "à neuf" / "à 9"
        atHourPattern      = Regex("\\bà\\s+(\\d{1,2}|$numAlt)\\b")
    )
}
