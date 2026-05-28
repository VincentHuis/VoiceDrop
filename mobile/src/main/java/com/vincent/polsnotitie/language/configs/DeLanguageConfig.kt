package com.vincent.polsnotitie.language.configs

import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.language.AppLanguage
import com.vincent.polsnotitie.language.LanguageConfig
import com.vincent.polsnotitie.language.PlacePatternConfig
import com.vincent.polsnotitie.language.TimeParserConfig
import java.util.Calendar

object DeLanguageConfig : LanguageConfig {

    override val language = AppLanguage.GERMAN

    private val numberWords = mapOf(
        "ein" to 1, "eins" to 1, "eine" to 1, "einer" to 1, "zwei" to 2, "drei" to 3, "vier" to 4,
        "fünf" to 5, "sechs" to 6, "sieben" to 7, "acht" to 8, "neun" to 9,
        "zehn" to 10, "elf" to 11, "zwölf" to 12
    )
    private val numAlt = numberWords.keys.joinToString("|")

    override val categoryKeywords = mapOf(
        Category.BOODSCHAPPEN  to listOf("einkaufen", "einkauf", "lebensmittel", "supermarkt"),
        Category.IDEEEN        to listOf("idee", "ideen"),
        Category.TODO          to listOf("aufgabe", "aufgaben", "todo", "to-do"),
        Category.HERINNERINGEN to listOf("erinnerung", "erinnerungen", "erinnere"),
        Category.AGENDA        to listOf("termin", "kalender", "agenda"),
        Category.OVERIG        to emptyList()
    )

    override val placePatterns = PlacePatternConfig(
        homePatterns = listOf(
            Regex("(?<![a-z])wenn ich zuhause (?:bin|ankomme)(?![a-z])"),
            Regex("(?<![a-z])bei ankunft (?:zu hause|zuhause)(?![a-z])"),
            Regex("(?<![a-z])zu hause(?![a-z])"),
            Regex("(?<![a-z])zuhause(?![a-z])")
        ),
        workPatterns = listOf(
            Regex("(?<![a-z])wenn ich (?:auf der arbeit|im büro) (?:bin|ankomme)(?![a-z])"),
            Regex("(?<![a-z])auf der arbeit(?![a-z])"),
            Regex("(?<![a-z])im büro(?![a-z])"),
            Regex("(?<![a-z])arbeit(?![a-z])")
        ),
        shopPatterns = listOf(
            Regex("(?<![a-z])wenn ich im supermarkt bin(?![a-z])"),
            Regex("(?<![a-z])im supermarkt(?![a-z])"),
            Regex("(?<![a-z])beim supermarkt(?![a-z])"),
            Regex("(?<![a-z])supermarkt(?![a-z])")
        )
    )

    override val timeParser = TimeParserConfig(
        numberWords = numberWords,
        weekdays = mapOf(
            "montag"     to Calendar.MONDAY,    "dienstag"   to Calendar.TUESDAY,
            "mittwoch"   to Calendar.WEDNESDAY, "donnerstag" to Calendar.THURSDAY,
            "freitag"    to Calendar.FRIDAY,    "samstag"    to Calendar.SATURDAY,
            "sonntag"    to Calendar.SUNDAY
        ),
        tomorrowWords         = listOf("morgen"),
        dayAfterTomorrowWords = listOf("übermorgen"),
        todayWords            = listOf("heute"),
        periodPatterns = listOf(
            "morning"   to Regex("(?<![a-z])(?:heute morgen|morgens|am morgen|am vormittag|vormittags)(?![a-z])"),
            "afternoon" to Regex("(?<![a-z])(?:heute nachmittag|nachmittags|am nachmittag)(?![a-z])"),
            "evening"   to Regex("(?<![a-z])(?:heute abend|abends|am abend)(?![a-z])"),
            "night"     to Regex("(?<![a-z])(?:heute nacht|nachts|in der nacht)(?![a-z])")
        ),
        todayPrefixes         = listOf("heute"),
        relativeHalfHourPattern = Regex("\\bin\\s+einer\\s+halben\\s+stunde\\b"),
        relativeQuarterPattern  = Regex("\\bin\\s+(\\d+|$numAlt)\\s+viertelstunden?\\b"),
        relativeUnitPattern     = Regex("\\bin\\s+(\\d+|$numAlt)\\s+(minuten?|stunden?|tagen?|wochen?)\\b"),
        unitMultipliers = mapOf(
            "minute"  to 60_000L,             "minuten"  to 60_000L,
            "stunde"  to 3_600_000L,          "stunden"  to 3_600_000L,
            "tag"     to 86_400_000L,          "tagen"    to 86_400_000L,
            "woche"   to 7 * 86_400_000L,      "wochen"   to 7 * 86_400_000L
        ),
        clockPattern       = Regex("\\b(?:um\\s+)?(\\d{1,2})[:.](\\d{2})\\b"),
        halfHourPattern    = Regex("\\b(?:um\\s+)?halb\\s+($numAlt|\\d{1,2})\\b"),
        halfHourIsBefore   = true,
        quarterOverPattern = Regex("\\bviertel\\s+nach\\s+($numAlt|\\d{1,2})\\b"),
        quarterToPattern   = Regex("\\bviertel\\s+vor\\s+($numAlt|\\d{1,2})\\b"),
        hourWordPattern    = Regex("\\b(?:um\\s+)?(\\d{1,2}|$numAlt)\\s*uhr\\b"),
        atHourPattern      = Regex("\\bum\\s+(\\d{1,2}|$numAlt)\\b")
    )
}
