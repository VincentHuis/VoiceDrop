package com.vincent.voicedrop.language.configs

import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.language.AppLanguage
import com.vincent.voicedrop.language.LanguageConfig
import com.vincent.voicedrop.language.PlacePatternConfig
import com.vincent.voicedrop.language.TimeParserConfig
import java.util.Calendar

object EnLanguageConfig : LanguageConfig {

    override val language = AppLanguage.ENGLISH

    private val numberWords = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12
    )
    private val numAlt = numberWords.keys.joinToString("|")

    override val categoryKeywords = mapOf(
        Category.BOODSCHAPPEN  to listOf("groceries", "grocery", "shopping", "supermarket"),
        Category.IDEEEN        to listOf("idea", "ideas"),
        Category.TODO          to listOf("todo", "to do", "to-do", "task", "tasks"),
        Category.HERINNERINGEN to listOf("reminder", "reminders", "remind"),
        Category.AGENDA        to listOf("appointment", "calendar", "agenda"),
        Category.OVERIG        to emptyList()
    )

    override val placePatterns = PlacePatternConfig(
        homePatterns = listOf(
            Regex("(?<![a-z])when i(?:'m| am| get) home(?![a-z])"),
            Regex("(?<![a-z])upon arrival (?:at )?home(?![a-z])"),
            Regex("(?<![a-z])at home(?![a-z])")
        ),
        workPatterns = listOf(
            Regex("(?<![a-z])when i(?:'m| am| get| arrive) (?:at |to )?(?:work|the office)(?![a-z])"),
            Regex("(?<![a-z])at the office(?![a-z])"),
            Regex("(?<![a-z])at work(?![a-z])")
        ),
        shopPatterns = listOf(
            Regex("(?<![a-z])when i(?:'m| am) at the (?:supermarket|grocery store)(?![a-z])"),
            Regex("(?<![a-z])at the (?:supermarket|grocery store)(?![a-z])"),
            Regex("(?<![a-z])(?:supermarket|grocery store)(?![a-z])")
        )
    )

    override val timeParser = TimeParserConfig(
        numberWords = numberWords,
        weekdays = mapOf(
            "monday"    to Calendar.MONDAY,    "tuesday"   to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "thursday"  to Calendar.THURSDAY,
            "friday"    to Calendar.FRIDAY,    "saturday"  to Calendar.SATURDAY,
            "sunday"    to Calendar.SUNDAY
        ),
        tomorrowWords         = listOf("tomorrow"),
        dayAfterTomorrowWords = listOf("day after tomorrow"),
        todayWords            = listOf("today"),
        periodPatterns = listOf(
            "morning"   to Regex("(?<![a-z])(?:this morning|in the morning|mornings?)(?![a-z])"),
            "afternoon" to Regex("(?<![a-z])(?:this afternoon|in the afternoon|afternoons?)(?![a-z])"),
            "evening"   to Regex("(?<![a-z])(?:this evening|in the evening|evenings?)(?![a-z])"),
            "night"     to Regex("(?<![a-z])(?:tonight|at night|nights?)(?![a-z])")
        ),
        todayPrefixes         = listOf("this", "tonight"),
        relativeHalfHourPattern = Regex("\\bin\\s+half\\s+an?\\s+hour\\b"),
        relativeQuarterPattern  = Regex("\\bin\\s+(\\d+|$numAlt)\\s+quarters?(?:\\s+of\\s+an\\s+hour)?\\b"),
        relativeUnitPattern     = Regex("\\bin\\s+(\\d+|$numAlt)\\s+(minutes?|hours?|days?|weeks?)\\b"),
        unitMultipliers = mapOf(
            "minute" to 60_000L,           "minutes" to 60_000L,
            "hour"   to 3_600_000L,        "hours"   to 3_600_000L,
            "day"    to 86_400_000L,       "days"    to 86_400_000L,
            "week"   to 7 * 86_400_000L,   "weeks"   to 7 * 86_400_000L
        ),
        clockPattern       = Regex("\\b(?:at\\s+)?(\\d{1,2})[:.](\\d{2})\\b"),
        halfHourPattern    = Regex("\\b(?:at\\s+)?half\\s+(?:past\\s+)?($numAlt|\\d{1,2})\\b"),
        halfHourIsBefore   = false,
        quarterOverPattern = Regex("\\bquarter\\s+(?:past|after)\\s+($numAlt|\\d{1,2})\\b"),
        quarterToPattern   = Regex("\\bquarter\\s+(?:to|before)\\s+($numAlt|\\d{1,2})\\b"),
        hourWordPattern    = Regex("\\b(?:at\\s+)?(\\d{1,2}|$numAlt)\\s*o'?clock\\b"),
        atHourPattern      = Regex("\\bat\\s+(\\d{1,2}|$numAlt)\\b")
    )
}
