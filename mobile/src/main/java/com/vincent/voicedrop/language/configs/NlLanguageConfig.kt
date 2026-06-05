package com.vincent.voicedrop.language.configs

import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.language.AppLanguage
import com.vincent.voicedrop.language.LanguageConfig
import com.vincent.voicedrop.language.PlacePatternConfig
import com.vincent.voicedrop.language.TimeParserConfig
import java.util.Calendar

object NlLanguageConfig : LanguageConfig {

    override val language = AppLanguage.DUTCH

    private val numberWords = mapOf(
        "een" to 1, "één" to 1, "twee" to 2, "drie" to 3, "vier" to 4, "vijf" to 5,
        "zes" to 6, "zeven" to 7, "acht" to 8, "negen" to 9, "tien" to 10,
        "elf" to 11, "twaalf" to 12
    )
    private val numAlt = numberWords.keys.joinToString("|")

    override val categoryKeywords = mapOf(
        Category.BOODSCHAPPEN  to listOf("boodschappen", "boodschap", "winkel", "supermarkt"),
        Category.IDEEEN        to listOf("idee", "ideeen", "ideetje", "ideetjes"),
        Category.TAKEN to listOf(
            "todo",
            "to do",
            "to-do",
            "taak",
            "taken",
            "herinnering",
            "herinneringen",
            "herinner"
        ),
        Category.AGENDA        to listOf("agenda", "afspraak", "kalender"),
        Category.OVERIG        to emptyList()
    )

    override val placePatterns = PlacePatternConfig(
        homePatterns = listOf(
            Regex("(?<![a-z])als ik thuis (?:ben|kom|aankom)(?![a-z])"),
            Regex("(?<![a-z])wanneer ik thuis (?:ben|kom)(?![a-z])"),
            Regex("(?<![a-z])zodra ik thuis (?:ben|kom)(?![a-z])"),
            Regex("(?<![a-z])bij thuiskomst(?![a-z])"),
            Regex("(?<![a-z])thuis(?![a-z])")
        ),
        workPatterns = listOf(
            Regex("(?<![a-z])als ik op (?:het |mijn )?(?:werk|kantoor) (?:ben|kom|aankom)(?![a-z])"),
            Regex("(?<![a-z])wanneer ik op (?:het |mijn )?(?:werk|kantoor) (?:ben|kom)(?![a-z])"),
            Regex("(?<![a-z])zodra ik op (?:het |mijn )?(?:werk|kantoor) (?:ben|kom)(?![a-z])"),
            Regex("(?<![a-z])op (?:het |mijn )?werk(?![a-z])"),
            Regex("(?<![a-z])op kantoor(?![a-z])")
        ),
        shopPatterns = listOf(
            Regex("(?<![a-z])als ik (?:bij|in) de supermarkt ben(?![a-z])"),
            Regex("(?<![a-z])bij de supermarkt(?![a-z])"),
            Regex("(?<![a-z])in de supermarkt(?![a-z])"),
            Regex("(?<![a-z])bij de winkel(?![a-z])"),
            Regex("(?<![a-z])supermarkt(?![a-z])")
        )
    )

    override val timeParser = TimeParserConfig(
        numberWords = numberWords,
        weekdays = mapOf(
            "maandag"  to Calendar.MONDAY,    "dinsdag"   to Calendar.TUESDAY,
            "woensdag" to Calendar.WEDNESDAY, "donderdag" to Calendar.THURSDAY,
            "vrijdag"  to Calendar.FRIDAY,    "zaterdag"  to Calendar.SATURDAY,
            "zondag"   to Calendar.SUNDAY
        ),
        tomorrowWords          = listOf("morgen"),
        dayAfterTomorrowWords  = listOf("overmorgen"),
        todayWords             = listOf("vandaag"),
        periodPatterns = listOf(
            "evening"   to Regex("(?<![a-z])(?:'?s\\s+avonds|savonds|vanavond|in\\s+de\\s+avond|avonds|avond)(?![a-z])"),
            "afternoon" to Regex("(?<![a-z])(?:'?s\\s+middags|smiddags|vanmiddag|in\\s+de\\s+middag|middags|middag)(?![a-z])"),
            "morning"   to Regex("(?<![a-z])(?:'?s\\s+ochtends|sochtends|'?s\\s+morgens|smorgens|vanochtend|vanmorgen|in\\s+de\\s+ochtend|ochtends|ochtend|morgens)(?![a-z])"),
            "night"     to Regex("(?<![a-z])(?:'?s\\s+nachts|snachts|vannacht|in\\s+de\\s+nacht|nachts|nacht)(?![a-z])")
        ),
        todayPrefixes          = listOf("van"),
        relativeHalfHourPattern = Regex("\\bover\\s+een\\s+half\\s*uur\\b"),
        relativeQuarterPattern  = Regex("\\bover\\s+(\\d+|$numAlt)\\s+kwartier(?:tje)?s?\\b"),
        relativeUnitPattern     = Regex("\\bover\\s+(\\d+|$numAlt)\\s+(minuten|minuut|min|uur|uren|dagen|dag|weken|week)\\b"),
        unitMultipliers = mapOf(
            "minuut" to 60_000L, "minuten" to 60_000L, "min" to 60_000L,
            "uur" to 3_600_000L, "uren" to 3_600_000L,
            "dag" to 86_400_000L, "dagen" to 86_400_000L,
            "week" to 7 * 86_400_000L, "weken" to 7 * 86_400_000L
        ),
        clockPattern      = Regex("\\b(?:om\\s+)?(\\d{1,2})[:.](\\d{2})\\b"),
        halfHourPattern   = Regex("\\b(?:om\\s+)?half\\s+($numAlt)\\b"),
        halfHourIsBefore  = true,
        quarterOverPattern = Regex("\\bkwart\\s+over\\s+($numAlt|\\d{1,2})\\b"),
        quarterToPattern   = Regex("\\bkwart\\s+voor\\s+($numAlt|\\d{1,2})\\b"),
        hourWordPattern    = Regex("\\b(?:om\\s+)?(\\d{1,2}|$numAlt)\\s*uur\\b"),
        atHourPattern = Regex("\\bom\\s+(\\d{1,2}|$numAlt)\\b"),
        recurringUnits = mapOf(
            "dag" to "DAY", "dagen" to "DAY",
            "week" to "WEEK", "weken" to "WEEK",
            "maand" to "MONTH", "maanden" to "MONTH",
            "uur" to "HOUR", "uren" to "HOUR",
            "minuut" to "MIN", "minuten" to "MIN", "min" to "MIN"
        ),
        recurringUnitPattern = Regex("\\b(?:elke|iedere|ieder|elk)\\s+(?:(\\d+|$numAlt)\\s+)?(dagen|dag|weken|week|maanden|maand|uren|uur|minuten|minuut|min)\\b"),
        recurringWeekdayPattern = Regex("\\b(elke|iedere|ieder)\\s+(maandag|dinsdag|woensdag|donderdag|vrijdag|zaterdag|zondag)\\b"),
        recurringAdverbs = mapOf(
            "dagelijks" to "DAY:1",
            "wekelijks" to "WEEK:1",
            "maandelijks" to "MONTH:1"
        )
    )
}
