package com.vincent.polsnotitie.language

import com.vincent.polsnotitie.data.Category
import java.util.Calendar

interface LanguageConfig {
    val language: AppLanguage
    val categoryKeywords: Map<Category, List<String>>
    val placePatterns: PlacePatternConfig
    val timeParser: TimeParserConfig
}

data class PlacePatternConfig(
    val homePatterns: List<Regex>,
    val workPatterns: List<Regex>,
    val shopPatterns: List<Regex>
)

data class TimeParserConfig(
    val numberWords: Map<String, Int>,
    val weekdays: Map<String, Int>,              // word -> Calendar.MONDAY etc.
    val tomorrowWords: List<String>,
    val dayAfterTomorrowWords: List<String>,
    val todayWords: List<String>,
    // List of (periodKey, pattern): periodKey is "morning"/"afternoon"/"evening"/"night"
    val periodPatterns: List<Pair<String, Regex>>,
    // Words whose presence at the start of a period match means "today" (e.g. "van" in "vanavond")
    val todayPrefixes: List<String>,
    val relativeHalfHourPattern: Regex,          // matches whole "over een half uur"-style phrase
    val relativeQuarterPattern: Regex,           // group(1) = number word/digit
    val relativeUnitPattern: Regex,              // group(1) = number, group(2) = unit word
    val unitMultipliers: Map<String, Long>,      // unit word -> milliseconds
    val clockPattern: Regex,                     // group(1) = hour digits, group(2) = minute digits
    val halfHourPattern: Regex,                  // group(1) = hour number word
    val halfHourIsBefore: Boolean,               // true = "half nine" means 8:30 (NL/DE), false = 9:30 (EN)
    val quarterOverPattern: Regex,               // group(1) = hour
    val quarterToPattern: Regex,                 // group(1) = hour
    val hourWordPattern: Regex,                  // group(1) = hour (e.g. "9 uur")
    val atHourPattern: Regex                     // group(1) = hour (e.g. "om 9")
)
