# Multi-language Support (NL/DE/EN) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add full NL/DE/EN language support to Polsnotitie — UI, speech recognition, category classifier, time parser, and place parser — driven by a user preference stored on the phone and synced to the watch.

**Architecture:** A `LanguageConfig` interface holds all language-specific data (keywords, regex patterns, weekdays, number words). Three config objects (`NlLanguageConfig`, `DeLanguageConfig`, `EnLanguageConfig`) implement it. Parsers and classifier become classes that accept the config at construction time. `MemoProcessor` reads the language preference from SharedPreferences and wires everything together. UI strings move to `strings.xml`; a `LocalizedContext` applies the selected language to Compose without relying on system locale.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Wearable Data Layer (DataClient), SharedPreferences, Android `strings.xml` resource system, `createConfigurationContext`

---

## File Map

**New files:**
- `mobile/.../language/AppLanguage.kt` — enum with code/locale/displayName per language
- `mobile/.../language/LanguagePreference.kt` — SharedPreferences read/write
- `mobile/.../language/LanguageConfig.kt` — interface + `PlacePatternConfig` + `TimeParserConfig` data classes
- `mobile/.../language/LanguageProvider.kt` — factory: prefs → LanguageConfig
- `mobile/.../language/configs/NlLanguageConfig.kt` — Dutch config (extracted from existing code)
- `mobile/.../language/configs/DeLanguageConfig.kt` — German config
- `mobile/.../language/configs/EnLanguageConfig.kt` — English config
- `mobile/.../language/ContextExt.kt` — `Context.withAppLanguage(prefs)` extension
- `mobile/src/main/res/values-de/strings.xml` — German strings
- `mobile/src/main/res/values-en/strings.xml` — English strings
- `wear/src/main/res/values-de/strings.xml`
- `wear/src/main/res/values-en/strings.xml`

**Modified files:**
- `mobile/.../data/Category.kt` — remove `displayName`/`keywords` constructor params
- `mobile/.../data/Place.kt` — remove `PlaceType.displayName` constructor param
- `mobile/.../data/CategoryClassifier.kt` — object → class(config)
- `mobile/.../reminder/ReminderTimeParser.kt` — object → class(config)
- `mobile/.../reminder/PlaceParser.kt` — object → class(config)
- `mobile/.../MemoProcessor.kt` — object → class(context), reads prefs, instantiates parsers
- `mobile/.../MemoListenerService.kt` — instantiate MemoProcessor(this)
- `mobile/.../MainActivity.kt` — LocalizedContext, settings screen language card, update all callers
- `mobile/src/main/res/values/strings.xml` — extract all hardcoded UI strings
- `wear/.../presentation/MainActivity.kt` — read DataItem for language, dynamic RecognizerIntent locale
- `wear/src/main/res/values/strings.xml` — extract hardcoded wear strings
- `mobile/src/test/.../CategoryClassifierTest.kt` — update to use NlLanguageConfig, add DE/EN tests
- `mobile/src/test/.../ReminderTimeParserTest.kt` — update to use NlLanguageConfig, add DE/EN tests
- `mobile/src/test/.../PlaceParserTest.kt` — update to use NlLanguageConfig, add DE/EN tests

---

## Task 1: AppLanguage enum + LanguagePreference

**Files:**
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/AppLanguage.kt`
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/LanguagePreference.kt`

- [ ] **Step 1: Create AppLanguage.kt**

```kotlin
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
```

- [ ] **Step 2: Create LanguagePreference.kt**

```kotlin
package com.vincent.polsnotitie.language

import android.content.SharedPreferences

private const val KEY = "app_language"

object LanguagePreference {
    fun get(prefs: SharedPreferences): AppLanguage =
        AppLanguage.fromCode(prefs.getString(KEY, "nl") ?: "nl")

    fun set(prefs: SharedPreferences, lang: AppLanguage) =
        prefs.edit().putString(KEY, lang.code).apply()
}
```

- [ ] **Step 3: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/language/
git commit -m "feat: add AppLanguage enum and LanguagePreference"
```

---

## Task 2: LanguageConfig interfaces

**Files:**
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/LanguageConfig.kt`

- [ ] **Step 1: Create LanguageConfig.kt**

```kotlin
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
```

- [ ] **Step 2: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/language/LanguageConfig.kt
git commit -m "feat: add LanguageConfig interface and data classes"
```

---

## Task 3: NlLanguageConfig

**Files:**
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/configs/NlLanguageConfig.kt`

This extracts all hardcoded data from the existing `CategoryClassifier`, `ReminderTimeParser`, and `PlaceParser` objects.

- [ ] **Step 1: Create NlLanguageConfig.kt**

```kotlin
package com.vincent.polsnotitie.language.configs

import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.language.AppLanguage
import com.vincent.polsnotitie.language.LanguageConfig
import com.vincent.polsnotitie.language.PlacePatternConfig
import com.vincent.polsnotitie.language.TimeParserConfig
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
        Category.TODO          to listOf("todo", "to do", "to-do", "taak", "taken"),
        Category.HERINNERINGEN to listOf("herinnering", "herinneringen", "herinner"),
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
        atHourPattern      = Regex("\\bom\\s+(\\d{1,2}|$numAlt)\\b")
    )
}
```

- [ ] **Step 2: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/language/configs/NlLanguageConfig.kt
git commit -m "feat: add NlLanguageConfig (extracted from existing parsers)"
```

---

## Task 4: DeLanguageConfig + EnLanguageConfig

**Files:**
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/configs/DeLanguageConfig.kt`
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/configs/EnLanguageConfig.kt`

- [ ] **Step 1: Create DeLanguageConfig.kt**

```kotlin
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
        "ein" to 1, "eins" to 1, "eine" to 1, "zwei" to 2, "drei" to 3, "vier" to 4,
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
```

- [ ] **Step 2: Create EnLanguageConfig.kt**

```kotlin
package com.vincent.polsnotitie.language.configs

import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.language.AppLanguage
import com.vincent.polsnotitie.language.LanguageConfig
import com.vincent.polsnotitie.language.PlacePatternConfig
import com.vincent.polsnotitie.language.TimeParserConfig
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
```

- [ ] **Step 3: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/language/configs/
git commit -m "feat: add DeLanguageConfig and EnLanguageConfig"
```

---

## Task 5: LanguageProvider

**Files:**
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/LanguageProvider.kt`

- [ ] **Step 1: Create LanguageProvider.kt**

```kotlin
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
```

- [ ] **Step 2: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/language/LanguageProvider.kt
git commit -m "feat: add LanguageProvider factory"
```

---

## Task 6: Refactor CategoryClassifier — object → class(config) + update tests

**Files:**
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/data/CategoryClassifier.kt`
- Modify: `mobile/src/test/java/com/vincent/polsnotitie/CategoryClassifierTest.kt`

The algorithm is unchanged. The `category.keywords` references are replaced by `config.categoryKeywords[category]`. The class takes a `LanguageConfig` constructor parameter.

- [ ] **Step 1: Write failing test for DE keyword classification**

Add to `CategoryClassifierTest.kt` at the top (import + test):

```kotlin
import com.vincent.polsnotitie.language.configs.DeLanguageConfig
import com.vincent.polsnotitie.language.configs.EnLanguageConfig
import com.vincent.polsnotitie.language.configs.NlLanguageConfig

// Change existing helper to use NlLanguageConfig:
private fun assertCategory(input: String, expected: Category, expectedText: String) {
    val result = CategoryClassifier(NlLanguageConfig).classify(input)
    assertEquals("category for '$input'", expected, result.category)
    assertEquals("text for '$input'", expectedText, result.text)
}

@Test
fun deKeywords() {
    val clf = CategoryClassifier(DeLanguageConfig)
    assertEquals(Category.BOODSCHAPPEN, clf.classify("Einkaufen Milch").category)
    assertEquals(Category.IDEEEN,       clf.classify("Idee neue App").category)
    assertEquals(Category.TODO,         clf.classify("Aufgabe Rechnung schicken").category)
    assertEquals(Category.HERINNERINGEN,clf.classify("Erinnerung Zahnarzt").category)
}

@Test
fun enKeywords() {
    val clf = CategoryClassifier(EnLanguageConfig)
    assertEquals(Category.BOODSCHAPPEN, clf.classify("Groceries milk and bread").category)
    assertEquals(Category.IDEEEN,       clf.classify("Idea new feature").category)
    assertEquals(Category.TODO,         clf.classify("Task send invoice").category)
    assertEquals(Category.HERINNERINGEN,clf.classify("Reminder dentist").category)
}
```

- [ ] **Step 2: Run test — expect compile error (CategoryClassifier still object)**

```
.\gradlew.bat :mobile:testDebugUnitTest --tests "com.vincent.polsnotitie.CategoryClassifierTest" 2>&1 | Select-String -Pattern "error:|FAILED|PASSED" | Select-Object -First 20
```

Expected: compile error — `CategoryClassifier` takes no arguments.

- [ ] **Step 3: Refactor CategoryClassifier.kt**

Replace the entire file content:

```kotlin
package com.vincent.polsnotitie.data

import com.vincent.polsnotitie.language.LanguageConfig

class CategoryClassifier(private val config: LanguageConfig) {

    data class Result(val category: Category, val text: String)

    private val THRESHOLD = 0.7

    fun classify(raw: String): Result {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result(Category.OVERIG, trimmed)

        val words = trimmed.split(Regex("\\s+"))
        val first = words[0]
        val firstTwo = if (words.size >= 2) "${words[0]} ${words[1]}" else null

        var bestCategory: Category? = null
        var bestConsumed = 0
        var bestScore = 0.0

        for (category in Category.entries) {
            val keywords = config.categoryKeywords[category] ?: continue
            if (keywords.isEmpty()) continue
            for (keyword in keywords) {
                score(first, keyword)?.let { s ->
                    if (s > bestScore) { bestScore = s; bestCategory = category; bestConsumed = 1 }
                }
                if (firstTwo != null) {
                    score(firstTwo, keyword)?.let { s ->
                        if (s > bestScore) { bestScore = s; bestCategory = category; bestConsumed = 2 }
                    }
                }
            }
        }

        val category = bestCategory ?: return Result(Category.OVERIG, trimmed)
        val remaining = words.drop(bestConsumed).joinToString(" ").trim()
        return Result(category, remaining.ifEmpty { trimmed })
    }

    private fun score(candidate: String, keyword: String): Double? {
        val a = normalize(candidate)
        val b = normalize(keyword)
        if (a.isEmpty() || b.isEmpty()) return null
        val maxLen = maxOf(a.length, b.length)
        val similarity = 1.0 - levenshtein(a, b).toDouble() / maxLen
        return if (similarity >= THRESHOLD) similarity else null
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace("ë", "e").replace("é", "e").replace("è", "e")
            .replace("ï", "i").replace("ö", "o").replace("ü", "u")
            .replace(Regex("[^a-z0-9]"), "")

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[b.length]
    }
}
```

- [ ] **Step 4: Run tests — all should pass**

```
.\gradlew.bat :mobile:testDebugUnitTest --tests "com.vincent.polsnotitie.CategoryClassifierTest"
```

Expected: BUILD SUCCESSFUL, all tests PASSED.

- [ ] **Step 5: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/data/CategoryClassifier.kt
git add mobile/src/test/java/com/vincent/polsnotitie/CategoryClassifierTest.kt
git commit -m "refactor: CategoryClassifier object -> class(config), add DE/EN tests"
```

---

## Task 7: Refactor ReminderTimeParser — object → class(config) + update tests

**Files:**
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/reminder/ReminderTimeParser.kt`
- Modify: `mobile/src/test/java/com/vincent/polsnotitie/ReminderTimeParserTest.kt`

The algorithm is identical to the existing one. All hardcoded maps/patterns are replaced by `config.timeParser.*` references.

- [ ] **Step 1: Add DE/EN tests to ReminderTimeParserTest.kt**

Add at the top of the class (before existing tests):

```kotlin
import com.vincent.polsnotitie.language.configs.DeLanguageConfig
import com.vincent.polsnotitie.language.configs.EnLanguageConfig
import com.vincent.polsnotitie.language.configs.NlLanguageConfig

// Update the existing private helper to use NlLanguageConfig:
// (the existing tests use ReminderTimeParser.parse(...) — update all to ReminderTimeParser(NlLanguageConfig).parse(...))
```

Then add these new test methods:

```kotlin
@Test
fun deInEinerStunde() {
    val n = now()
    val r = ReminderTimeParser(DeLanguageConfig).parse("in einer stunde anrufen", n)
    assertEquals(n + 3_600_000L, r.remindAt)
    assertEquals("anrufen", r.text)
}

@Test
fun deMorgenUmNeun() {
    val r = ReminderTimeParser(DeLanguageConfig).parse("morgen um 9 uhr zahnarzt", now())
    assertNotNull(r.remindAt)
    val c = cal(r.remindAt!!)
    assertEquals(Calendar.THURSDAY, c.get(Calendar.DAY_OF_WEEK))
    assertEquals(21, c.get(Calendar.HOUR_OF_DAY)) // 9 ambiguous → +12
    assertEquals("zahnarzt", r.text)
}

@Test
fun deHalbNeun() {
    // "halb neun" → 8:30 (halfHourIsBefore=true, same as Dutch)
    val r = ReminderTimeParser(DeLanguageConfig).parse("morgen halb neun frühstück", now())
    assertNotNull(r.remindAt)
    val c = cal(r.remindAt!!)
    assertEquals(8, c.get(Calendar.HOUR_OF_DAY))
    assertEquals(30, c.get(Calendar.MINUTE))
}

@Test
fun enInOneHour() {
    val n = now()
    val r = ReminderTimeParser(EnLanguageConfig).parse("in one hour call back", n)
    assertEquals(n + 3_600_000L, r.remindAt)
    assertEquals("call back", r.text)
}

@Test
fun enTomorrowAtNine() {
    val r = ReminderTimeParser(EnLanguageConfig).parse("tomorrow at 9 o'clock dentist", now())
    assertNotNull(r.remindAt)
    val c = cal(r.remindAt!!)
    assertEquals(Calendar.THURSDAY, c.get(Calendar.DAY_OF_WEEK))
    assertEquals(21, c.get(Calendar.HOUR_OF_DAY)) // 9 ambiguous → +12
    assertEquals("dentist", r.text)
}

@Test
fun enHalfNineIs930() {
    // English: "half nine" = 9:30, not 8:30
    val r = ReminderTimeParser(EnLanguageConfig).parse("tomorrow half nine breakfast", now())
    assertNotNull(r.remindAt)
    val c = cal(r.remindAt!!)
    assertEquals(9, c.get(Calendar.HOUR_OF_DAY))
    assertEquals(30, c.get(Calendar.MINUTE))
}
```

Also update all existing test calls from `ReminderTimeParser.parse(...)` to `ReminderTimeParser(NlLanguageConfig).parse(...)`.

- [ ] **Step 2: Run test — expect compile errors**

```
.\gradlew.bat :mobile:testDebugUnitTest --tests "com.vincent.polsnotitie.ReminderTimeParserTest" 2>&1 | Select-String "error:" | Select-Object -First 10
```

- [ ] **Step 3: Rewrite ReminderTimeParser.kt**

Replace the entire file:

```kotlin
package com.vincent.polsnotitie.reminder

import com.vincent.polsnotitie.language.LanguageConfig
import java.util.Calendar

class ReminderTimeParser(private val config: LanguageConfig) {

    data class Result(val remindAt: Long?, val text: String)

    private enum class Period { MORNING, AFTERNOON, EVENING, NIGHT }
    private data class TimeMatch(val hour: Int, val minute: Int, val ambiguous: Boolean)

    private val tc = config.timeParser

    fun parse(raw: String, now: Long = System.currentTimeMillis()): Result {
        val lower = raw.lowercase()
        val ranges = mutableListOf<IntRange>()

        // 1) Relative: "over een half uur", "in einer stunde", "in one hour"
        tc.relativeHalfHourPattern.find(lower)?.let { m ->
            return Result(now + 30 * 60_000L, clean(raw, listOf(m.range)))
        }
        tc.relativeQuarterPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            return Result(now + n * 15 * 60_000L, clean(raw, listOf(m.range)))
        }
        tc.relativeUnitPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            val unitMs = tc.unitMultipliers[m.groupValues[2]] ?: return@let
            return Result(now + n * unitMs, clean(raw, listOf(m.range)))
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        // 2) Period (morning/afternoon/evening/night)
        val (period, vanToday) = detectPeriod(lower, ranges)

        var dayFound = false
        var pushIfPast = false
        var weekdayMatched = false

        // 3) Day
        if (vanToday) {
            dayFound = true; pushIfPast = true
        } else {
            when {
                containsAny(lower, tc.dayAfterTomorrowWords, ranges) -> {
                    cal.add(Calendar.DAY_OF_YEAR, 2); dayFound = true
                }
                containsAny(lower, tc.tomorrowWords, ranges) -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1); dayFound = true
                }
                containsAny(lower, tc.todayWords, ranges) -> {
                    dayFound = true; pushIfPast = true
                }
                else -> {
                    for ((name, dow) in tc.weekdays) {
                        if (contains(lower, name, ranges)) {
                            val diff = ((dow - cal.get(Calendar.DAY_OF_WEEK)) + 7) % 7
                            cal.add(Calendar.DAY_OF_YEAR, diff)
                            dayFound = true; pushIfPast = true; weekdayMatched = true
                            break
                        }
                    }
                }
            }
        }

        // 4) Time
        val time = parseTime(lower, ranges)
        var timeFound = false
        if (time != null) {
            cal.set(Calendar.HOUR_OF_DAY, applyPeriod(time.hour, time.ambiguous, period))
            cal.set(Calendar.MINUTE, time.minute)
            timeFound = true
            if (!dayFound) pushIfPast = true
        } else if (dayFound || period != null) {
            cal.set(Calendar.HOUR_OF_DAY, defaultHourFor(period))
            cal.set(Calendar.MINUTE, 0)
            if (!dayFound) { dayFound = true; pushIfPast = true }
        }

        if (!dayFound && !timeFound) return Result(null, raw)

        if (cal.timeInMillis <= now && pushIfPast) {
            val step = if (timeFound && weekdayMatched) 7 else 1
            cal.add(Calendar.DAY_OF_YEAR, step)
        }

        return Result(cal.timeInMillis, clean(raw, ranges))
    }

    private fun detectPeriod(lower: String, ranges: MutableList<IntRange>): Pair<Period?, Boolean> {
        for ((key, pattern) in tc.periodPatterns) {
            val m = pattern.find(lower) ?: continue
            ranges += m.range
            val isToday = tc.todayPrefixes.any { m.value.startsWith(it) }
            val period = when (key) {
                "morning"   -> Period.MORNING
                "afternoon" -> Period.AFTERNOON
                "evening"   -> Period.EVENING
                "night"     -> Period.NIGHT
                else        -> null
            }
            return period to isToday
        }
        return null to false
    }

    private fun parseTime(lower: String, ranges: MutableList<IntRange>): TimeMatch? {
        // HH:mm / HH.mm explicit
        tc.clockPattern.find(lower)?.let { m ->
            val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) { ranges += m.range; return TimeMatch(h, min, false) }
        }
        // "half nine" / "halb neun" — semantics differ per language
        tc.halfHourPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            val h = if (tc.halfHourIsBefore) hourBefore(n) else n
            ranges += m.range; return TimeMatch(h, 30, true)
        }
        // quarter past / kwart over / viertel nach
        tc.quarterOverPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            ranges += m.range; return TimeMatch(n, 15, true)
        }
        // quarter to / kwart voor / viertel vor
        tc.quarterToPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            ranges += m.range; return TimeMatch(hourBefore(n), 45, true)
        }
        // "9 uur" / "9 Uhr" / "9 o'clock"
        tc.hourWordPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            if (n in 0..23) { ranges += m.range; return TimeMatch(n, 0, true) }
        }
        // "om 9" / "um 9" / "at 9"
        tc.atHourPattern.find(lower)?.let { m ->
            val n = toInt(m.groupValues[1]) ?: return@let
            if (n in 0..23) { ranges += m.range; return TimeMatch(n, 0, true) }
        }
        return null
    }

    private fun applyPeriod(hour: Int, ambiguous: Boolean, period: Period?): Int {
        if (!ambiguous) return hour
        return when (period) {
            Period.MORNING -> hour
            Period.AFTERNOON, Period.EVENING -> if (hour in 1..11) hour + 12 else hour
            Period.NIGHT -> hour
            null -> if (hour in 1..10) hour + 12 else hour
        }
    }

    private fun defaultHourFor(period: Period?): Int = when (period) {
        Period.MORNING -> 9; Period.AFTERNOON -> 14; Period.EVENING -> 19; Period.NIGHT -> 23; null -> 9
    }

    private fun hourBefore(n: Int): Int = ((n - 2 + 12) % 12) + 1

    private fun toInt(token: String): Int? = token.toIntOrNull() ?: tc.numberWords[token]

    private fun contains(lower: String, word: String, ranges: MutableList<IntRange>): Boolean {
        val m = Regex("\\b${Regex.escape(word)}\\b").find(lower) ?: return false
        ranges += m.range; return true
    }

    private fun containsAny(lower: String, words: List<String>, ranges: MutableList<IntRange>): Boolean {
        for (word in words) { if (contains(lower, word, ranges)) return true }
        return false
    }

    private fun clean(raw: String, ranges: List<IntRange>): String {
        val sb = StringBuilder(raw)
        for (r in ranges.sortedByDescending { it.first }) {
            if (r.first in sb.indices && r.last in sb.indices) sb.delete(r.first, r.last + 1)
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
```

- [ ] **Step 4: Run tests — all should pass**

```
.\gradlew.bat :mobile:testDebugUnitTest --tests "com.vincent.polsnotitie.ReminderTimeParserTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/reminder/ReminderTimeParser.kt
git add mobile/src/test/java/com/vincent/polsnotitie/ReminderTimeParserTest.kt
git commit -m "refactor: ReminderTimeParser object -> class(config), add DE/EN tests"
```

---

## Task 8: Refactor PlaceParser — object → class(config) + update tests

**Files:**
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/reminder/PlaceParser.kt`
- Modify: `mobile/src/test/java/com/vincent/polsnotitie/PlaceParserTest.kt`

- [ ] **Step 1: Add DE/EN tests and update NL calls in PlaceParserTest.kt**

Update existing calls from `PlaceParser.parse(...)` to `PlaceParser(NlLanguageConfig).parse(...)`.

Add new tests:

```kotlin
import com.vincent.polsnotitie.language.configs.DeLanguageConfig
import com.vincent.polsnotitie.language.configs.EnLanguageConfig
import com.vincent.polsnotitie.language.configs.NlLanguageConfig

@Test
fun deThuis() {
    val r = PlaceParser(DeLanguageConfig).parse("zuhause Milch kaufen")
    assertEquals(PlaceType.THUIS, r.place)
    assertEquals("Milch kaufen", r.text)
}

@Test
fun deWerk() {
    val r = PlaceParser(DeLanguageConfig).parse("auf der arbeit dokument drucken")
    assertEquals(PlaceType.WERK, r.place)
}

@Test
fun deSupermarkt() {
    val r = PlaceParser(DeLanguageConfig).parse("im supermarkt Äpfel")
    assertEquals(PlaceType.SUPERMARKT, r.place)
    assertEquals("Äpfel", r.text)
}

@Test
fun enHome() {
    val r = PlaceParser(EnLanguageConfig).parse("when i get home buy milk")
    assertEquals(PlaceType.THUIS, r.place)
    assertEquals("buy milk", r.text)
}

@Test
fun enWork() {
    val r = PlaceParser(EnLanguageConfig).parse("at work print document")
    assertEquals(PlaceType.WERK, r.place)
}

@Test
fun enSupermarket() {
    val r = PlaceParser(EnLanguageConfig).parse("at the supermarket get apples")
    assertEquals(PlaceType.SUPERMARKT, r.place)
    assertEquals("get apples", r.text)
}
```

- [ ] **Step 2: Run test — expect compile errors**

```
.\gradlew.bat :mobile:testDebugUnitTest --tests "com.vincent.polsnotitie.PlaceParserTest" 2>&1 | Select-String "error:" | Select-Object -First 10
```

- [ ] **Step 3: Rewrite PlaceParser.kt**

```kotlin
package com.vincent.polsnotitie.reminder

import com.vincent.polsnotitie.data.PlaceType
import com.vincent.polsnotitie.language.LanguageConfig

class PlaceParser(private val config: LanguageConfig) {

    data class Result(val place: PlaceType?, val text: String)

    fun parse(raw: String): Result {
        val lower = raw.lowercase()
        val pp = config.placePatterns
        val allPatterns = listOf(
            PlaceType.THUIS     to pp.homePatterns,
            PlaceType.WERK      to pp.workPatterns,
            PlaceType.SUPERMARKT to pp.shopPatterns
        )
        for ((place, patterns) in allPatterns) {
            for (pattern in patterns) {
                val match = pattern.find(lower) ?: continue
                val cleaned = StringBuilder(raw)
                    .delete(match.range.first, match.range.last + 1)
                    .toString()
                    .replace(Regex("\\s+"), " ")
                    .trim()
                return Result(place, cleaned.ifEmpty { raw })
            }
        }
        return Result(null, raw)
    }
}
```

- [ ] **Step 4: Run tests**

```
.\gradlew.bat :mobile:testDebugUnitTest --tests "com.vincent.polsnotitie.PlaceParserTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/reminder/PlaceParser.kt
git add mobile/src/test/java/com/vincent/polsnotitie/PlaceParserTest.kt
git commit -m "refactor: PlaceParser object -> class(config), add DE/EN tests"
```

---

## Task 9: Refactor MemoProcessor + update callers

**Files:**
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/MemoProcessor.kt`
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/MemoListenerService.kt`
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt` (the `startMic` call site)

`MemoProcessor` changes from `object` to `class(context: Context)`. It reads language prefs and instantiates the parsers internally.

- [ ] **Step 1: Rewrite MemoProcessor.kt**

```kotlin
package com.vincent.polsnotitie

import android.content.Context
import com.vincent.polsnotitie.calendar.CalendarHelper
import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.data.CategoryClassifier
import com.vincent.polsnotitie.data.Memo
import com.vincent.polsnotitie.data.MemoDatabase
import com.vincent.polsnotitie.language.LanguageProvider
import com.vincent.polsnotitie.location.GeofenceManager
import com.vincent.polsnotitie.reminder.PlaceParser
import com.vincent.polsnotitie.reminder.ReminderNotifications
import com.vincent.polsnotitie.reminder.ReminderScheduler
import com.vincent.polsnotitie.reminder.ReminderTimeParser
import com.vincent.polsnotitie.widget.ShoppingWidget

class MemoProcessor(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val config = LanguageProvider.get(prefs)
    private val classifier = CategoryClassifier(config)
    private val timeParser = ReminderTimeParser(config)
    private val placeParser = PlaceParser(config)

    suspend fun process(id: String, rawText: String, timestamp: Long) {
        val dao = MemoDatabase.get(context).memoDao()
        val classified = classifier.classify(rawText)

        if (classified.category == Category.AGENDA) {
            handleAgenda(classified.text)
            return
        }

        var text = classified.text
        var remindAt: Long? = null
        var placeId: String? = null
        if (classified.category == Category.HERINNERINGEN) {
            val placeResult = placeParser.parse(text)
            if (placeResult.place != null) {
                placeId = placeResult.place.name
                text = placeResult.text
            } else {
                val parsed = timeParser.parse(text)
                text = parsed.text.ifEmpty { text }
                remindAt = parsed.remindAt
            }
        }

        val memo = Memo(
            id = id, text = text, timestamp = timestamp,
            category = classified.category.name, remindAt = remindAt, placeId = placeId
        )
        dao.insert(memo)

        if (classified.category == Category.HERINNERINGEN) {
            when {
                placeId != null  -> GeofenceManager.registerAll(context)
                remindAt != null -> ReminderScheduler.schedule(context, memo)
                else             -> ReminderNotifications.notifyAddTime(context, memo.id, memo.text)
            }
        }
        ShoppingWidget.refresh(context)
    }

    private fun handleAgenda(rawText: String) {
        val parsed = timeParser.parse(rawText)
        val title = parsed.text.ifEmpty { rawText }
        val start = parsed.remindAt
        if (start != null && CalendarHelper.insertEvent(context, title, start)) {
            CalendarHelper.notifyPlanned(context, title, start)
        } else {
            CalendarHelper.notifyAddToCalendar(context, title, start)
        }
    }
}
```

- [ ] **Step 2: Update MemoListenerService.kt — change the call site**

In `MemoListenerService.kt`, line 28, change:
```kotlin
MemoProcessor.process(
    context = this@MemoListenerService,
    id = id,
    rawText = map.getString("text").orEmpty(),
    timestamp = map.getLong("timestamp")
)
```
to:
```kotlin
MemoProcessor(this@MemoListenerService).process(
    id = id,
    rawText = map.getString("text").orEmpty(),
    timestamp = map.getLong("timestamp")
)
```

- [ ] **Step 3: Update the mic call site in MainActivity.kt**

In `MemoListScreen` composable (around line 212), change:
```kotlin
MemoProcessor.process(
    context, UUID.randomUUID().toString(), spoken, System.currentTimeMillis()
)
```
to:
```kotlin
MemoProcessor(context).process(
    UUID.randomUUID().toString(), spoken, System.currentTimeMillis()
)
```

- [ ] **Step 4: Build to verify no compile errors**

```
.\gradlew.bat :mobile:assembleDebug 2>&1 | Select-String "error:" | Select-Object -First 20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/MemoProcessor.kt
git add mobile/src/main/java/com/vincent/polsnotitie/MemoListenerService.kt
git add mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt
git commit -m "refactor: MemoProcessor object -> class(context), reads language prefs"
```

---

## Task 10: Refactor Category + PlaceType enums, add displayName extensions

**Files:**
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/data/Category.kt`
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/data/Place.kt`
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt`

The `keywords` and `displayName` constructor fields are removed from both enums. A `displayName(context)` extension function is added instead. The UI callers are updated in the same step.

- [ ] **Step 1: Replace Category.kt**

```kotlin
package com.vincent.polsnotitie.data

enum class Category {
    BOODSCHAPPEN, IDEEEN, TODO, HERINNERINGEN, AGENDA, OVERIG;

    companion object {
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name == name } ?: OVERIG
    }
}
```

- [ ] **Step 2: Replace PlaceType in Place.kt**

In `Place.kt`, replace the `PlaceType` enum:

```kotlin
enum class PlaceType {
    THUIS, WERK, SUPERMARKT;

    companion object {
        fun fromName(name: String?): PlaceType? = entries.firstOrNull { it.name == name }
    }
}
```

- [ ] **Step 3: Add displayName extensions in MainActivity.kt**

Add these two private functions near the top of the file (after imports):

```kotlin
import com.vincent.polsnotitie.R

private fun Category.displayName(context: Context): String = context.getString(
    when (this) {
        Category.BOODSCHAPPEN  -> R.string.category_groceries
        Category.TODO          -> R.string.category_todo
        Category.IDEEEN        -> R.string.category_ideas
        Category.HERINNERINGEN -> R.string.category_reminders
        Category.AGENDA        -> R.string.category_agenda
        Category.OVERIG        -> R.string.category_other
    }
)

private fun PlaceType.displayName(context: Context): String = context.getString(
    when (this) {
        PlaceType.THUIS      -> R.string.place_home
        PlaceType.WERK       -> R.string.place_work
        PlaceType.SUPERMARKT -> R.string.place_supermarket
    }
)
```

- [ ] **Step 4: Update callers of displayName in MainActivity.kt**

There are three call sites. Make these changes:

**Line 530 — CategoryChip composable:**
```kotlin
// Before:
text = category.displayName,
// After:
text = category.displayName(LocalContext.current),
```

**Line 381 — MemoCard, place label:**
```kotlin
// Before:
text = "Bij ${place.displayName}",
// After:
text = stringResource(R.string.at_place, place.displayName(context)),
```

**Line 763 — PlacesScreen, type name:**
```kotlin
// Before:
Text(type.displayName, style = ...)
// After:
Text(type.displayName(context), style = ...)
```

**Line 855 — MapPickerScreen, title:**
```kotlin
// Before:
title = { Text("Kies ${type.displayName}") },
// After:
title = { Text(stringResource(R.string.pick_place, type.displayName(context))) },
```

- [ ] **Step 5: Build — will fail with unresolved R references (strings added in next task)**

Confirm the *only* errors are about missing string resources, not logic errors:

```
.\gradlew.bat :mobile:assembleDebug 2>&1 | Select-String "error:" | Select-Object -First 20
```

Expected: errors mentioning `R.string.category_groceries` etc. — that is correct, strings are added in Task 11.

- [ ] **Step 6: Commit (will not compile yet — strings pending)**

```
git add mobile/src/main/java/com/vincent/polsnotitie/data/Category.kt
git add mobile/src/main/java/com/vincent/polsnotitie/data/Place.kt
git add mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt
git commit -m "refactor: remove keywords/displayName from enums, add displayName(context) extensions"
```

---

## Task 11: Mobile strings.xml — extract all UI strings

**Files:**
- Modify: `mobile/src/main/res/values/strings.xml`
- Create: `mobile/src/main/res/values-de/strings.xml`
- Create: `mobile/src/main/res/values-en/strings.xml`

- [ ] **Step 1: Replace mobile/src/main/res/values/strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">VoiceDrop</string>

    <!-- Main screen -->
    <string name="shopping_label">Boodschappen</string>
    <string name="settings_label">Instellingen</string>
    <string name="no_notes">Nog geen notities</string>
    <string name="no_results">Geen resultaten</string>
    <string name="search_hint">Zoeken…</string>
    <string name="record_label">Inspreken</string>
    <string name="speech_prompt">Spreek je notitie in</string>
    <string name="delete_label">Verwijderen</string>
    <string name="copy_label">Kopiëren</string>
    <string name="share_label">Delen</string>
    <string name="reminder_prefix">Herinnering: %s</string>
    <string name="add_time">Tijd toevoegen</string>
    <string name="at_place">Bij %s</string>

    <!-- Settings screen -->
    <string name="settings_title">Instellingen</string>
    <string name="language_title">Taal</string>
    <string name="locations_title">Locaties</string>
    <string name="locations_subtitle">Thuis, werk en supermarkt instellen</string>

    <!-- Places screen -->
    <string name="places_title">Plekken</string>
    <string name="back_label">Terug</string>
    <string name="place_set">Ingesteld</string>
    <string name="place_not_set">Niet ingesteld</string>
    <string name="allow_location">Locatie altijd toestaan</string>

    <!-- Map picker screen -->
    <string name="pick_place">Kies %s</string>
    <string name="current_location">Huidige locatie</string>
    <string name="address_hint">Adres zoeken…</string>
    <string name="address_not_found">Adres niet gevonden</string>
    <string name="save_location">Opslaan</string>

    <!-- Category names -->
    <string name="category_groceries">Boodschappen</string>
    <string name="category_todo">To-do</string>
    <string name="category_ideas">Ideeën</string>
    <string name="category_reminders">Herinneringen</string>
    <string name="category_agenda">Agenda</string>
    <string name="category_other">Overig</string>

    <!-- Place type names -->
    <string name="place_home">Thuis</string>
    <string name="place_work">Werk</string>
    <string name="place_supermarket">Supermarkt</string>
</resources>
```

- [ ] **Step 2: Create mobile/src/main/res/values-de/strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">VoiceDrop</string>
    <string name="shopping_label">Einkaufen</string>
    <string name="settings_label">Einstellungen</string>
    <string name="no_notes">Noch keine Notizen</string>
    <string name="no_results">Keine Ergebnisse</string>
    <string name="search_hint">Suchen…</string>
    <string name="record_label">Aufnehmen</string>
    <string name="speech_prompt">Notiz einsprechen</string>
    <string name="delete_label">Löschen</string>
    <string name="copy_label">Kopieren</string>
    <string name="share_label">Teilen</string>
    <string name="reminder_prefix">Erinnerung: %s</string>
    <string name="add_time">Zeit hinzufügen</string>
    <string name="at_place">Bei %s</string>
    <string name="settings_title">Einstellungen</string>
    <string name="language_title">Sprache</string>
    <string name="locations_title">Standorte</string>
    <string name="locations_subtitle">Zuhause, Arbeit und Supermarkt einrichten</string>
    <string name="places_title">Orte</string>
    <string name="back_label">Zurück</string>
    <string name="place_set">Eingestellt</string>
    <string name="place_not_set">Nicht eingestellt</string>
    <string name="allow_location">Standort immer erlauben</string>
    <string name="pick_place">%s auswählen</string>
    <string name="current_location">Aktueller Standort</string>
    <string name="address_hint">Adresse suchen…</string>
    <string name="address_not_found">Adresse nicht gefunden</string>
    <string name="save_location">Speichern</string>
    <string name="category_groceries">Lebensmittel</string>
    <string name="category_todo">Aufgabe</string>
    <string name="category_ideas">Ideen</string>
    <string name="category_reminders">Erinnerungen</string>
    <string name="category_agenda">Termin</string>
    <string name="category_other">Sonstiges</string>
    <string name="place_home">Zuhause</string>
    <string name="place_work">Arbeit</string>
    <string name="place_supermarket">Supermarkt</string>
</resources>
```

- [ ] **Step 3: Create mobile/src/main/res/values-en/strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">VoiceDrop</string>
    <string name="shopping_label">Groceries</string>
    <string name="settings_label">Settings</string>
    <string name="no_notes">No notes yet</string>
    <string name="no_results">No results</string>
    <string name="search_hint">Search…</string>
    <string name="record_label">Record</string>
    <string name="speech_prompt">Speak your note</string>
    <string name="delete_label">Delete</string>
    <string name="copy_label">Copy</string>
    <string name="share_label">Share</string>
    <string name="reminder_prefix">Reminder: %s</string>
    <string name="add_time">Add time</string>
    <string name="at_place">At %s</string>
    <string name="settings_title">Settings</string>
    <string name="language_title">Language</string>
    <string name="locations_title">Locations</string>
    <string name="locations_subtitle">Set up home, work and supermarket</string>
    <string name="places_title">Places</string>
    <string name="back_label">Back</string>
    <string name="place_set">Set</string>
    <string name="place_not_set">Not set</string>
    <string name="allow_location">Always allow location</string>
    <string name="pick_place">Choose %s</string>
    <string name="current_location">Current location</string>
    <string name="address_hint">Search address…</string>
    <string name="address_not_found">Address not found</string>
    <string name="save_location">Save</string>
    <string name="category_groceries">Groceries</string>
    <string name="category_todo">To-do</string>
    <string name="category_ideas">Ideas</string>
    <string name="category_reminders">Reminders</string>
    <string name="category_agenda">Agenda</string>
    <string name="category_other">Other</string>
    <string name="place_home">Home</string>
    <string name="place_work">Work</string>
    <string name="place_supermarket">Supermarket</string>
</resources>
```

- [ ] **Step 4: Replace all hardcoded strings in MainActivity.kt with stringResource() calls**

Go through `MainActivity.kt` and replace each hardcoded string with the matching `stringResource(R.string.*)` call. Key replacements:

| Location | Old | New |
|---|---|---|
| TopAppBar title (MemoListScreen) | `Text("VoiceDrop")` | `Text(stringResource(R.string.app_name))` |
| Shopping button | `Text("Boodschappen")` | `Text(stringResource(R.string.shopping_label))` |
| Settings icon contentDescription | `"Instellingen"` | `stringResource(R.string.settings_label)` |
| Empty state | `"Nog geen notities"` / `"Geen resultaten"` | `stringResource(R.string.no_notes)` / `stringResource(R.string.no_results)` |
| Search placeholder | `Text("Zoeken…")` | `Text(stringResource(R.string.search_hint))` |
| Mic contentDescription | `"Inspreken"` | `stringResource(R.string.record_label)` |
| startMic prompt | `"Spreek je notitie in"` | `getString(R.string.speech_prompt)` (inside Activity context) |
| Delete contentDescription | `"Verwijderen"` | `stringResource(R.string.delete_label)` |
| Reminder text | `"Herinnering: ${formatTimestamp(it)}"` | `stringResource(R.string.reminder_prefix, formatTimestamp(it, locale))` |
| Add time | `"Tijd toevoegen"` | `stringResource(R.string.add_time)` |
| Copy contentDescription | `"Kopiëren"` | `stringResource(R.string.copy_label)` |
| Share contentDescription | `"Delen"` | `stringResource(R.string.share_label)` |
| SettingsScreen title | `Text("Instellingen")` | `Text(stringResource(R.string.settings_title))` |
| Back contentDescription | `"Terug"` | `stringResource(R.string.back_label)` |
| Locations card title | `Text("Locaties", ...)` | `Text(stringResource(R.string.locations_title), ...)` |
| Locations card subtitle | `Text("Thuis, werk en supermarkt instellen", ...)` | `Text(stringResource(R.string.locations_subtitle), ...)` |
| Places screen title | `Text("Plekken")` | `Text(stringResource(R.string.places_title))` |
| Place set/not set | `"Ingesteld"` / `"Niet ingesteld"` | `stringResource(R.string.place_set)` / `stringResource(R.string.place_not_set)` |
| Allow location button | `Text("Locatie altijd toestaan")` | `Text(stringResource(R.string.allow_location))` |
| Current location icon | `"Huidige locatie"` | `stringResource(R.string.current_location)` |

Also update `formatTimestamp` to accept a `Locale` parameter:
```kotlin
private fun formatTimestamp(timestamp: Long, locale: Locale): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", locale).format(Date(timestamp))
```

- [ ] **Step 5: Build — verify clean**

```
.\gradlew.bat :mobile:assembleDebug 2>&1 | Select-String "error:" | Select-Object -First 20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```
git add mobile/src/main/res/
git add mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt
git commit -m "feat: extract all UI strings to strings.xml, add DE/EN translations"
```

---

## Task 12: LocalizedContext + apply in mobile MainActivity

**Files:**
- Create: `mobile/src/main/java/com/vincent/polsnotitie/language/ContextExt.kt`
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt`

This makes the app use the selected language for all `stringResource()` calls, regardless of the system locale.

- [ ] **Step 1: Create ContextExt.kt**

```kotlin
package com.vincent.polsnotitie.language

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

fun Context.withAppLanguage(prefs: SharedPreferences): Context {
    val lang = LanguagePreference.get(prefs)
    val locale = Locale.forLanguageTag(lang.code)
    val config = Configuration(resources.configuration).also { it.setLocale(locale) }
    return createConfigurationContext(config)
}
```

- [ ] **Step 2: Apply LocalizedContext in mobile MainActivity.onCreate**

In `MainActivity.kt`, change `onCreate` to:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val localizedCtx = this.withAppLanguage(prefs)
        val startInShopping = intent?.getBooleanExtra("openShopping", false) == true
        val timeForMemo = intent?.getStringExtra("setTimeForMemo")
        setContent {
            CompositionLocalProvider(LocalContext provides localizedCtx) {
                PolsnotitieTheme {
                    AppRoot(startInShopping = startInShopping, startTimeForMemoId = timeForMemo)
                }
            }
        }
    }
}
```

Add the import:
```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.vincent.polsnotitie.language.withAppLanguage
```

- [ ] **Step 3: Fix formatTimestamp locale**

`formatTimestamp` is called from composables that now have the right `LocalContext`. Update composable callers to pass the locale:

```kotlin
// Where formatTimestamp is called in MemoCard:
val locale = Locale.forLanguageTag(
    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        .getString("app_language", "nl") ?: "nl"
)
// ...
text = stringResource(R.string.reminder_prefix, formatTimestamp(it, locale)),
```

- [ ] **Step 4: Build**

```
.\gradlew.bat :mobile:assembleDebug 2>&1 | Select-String "error:" | Select-Object -First 20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/language/ContextExt.kt
git add mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt
git commit -m "feat: apply LocalizedContext in MainActivity for app-language-driven UI"
```

---

## Task 13: Settings screen — language selector

**Files:**
- Modify: `mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt`

The existing `SettingsScreen` composable gets a language card added above the existing locations card.

- [ ] **Step 1: Update SettingsScreen signature to accept language state**

Change the `SettingsScreen` signature and add a language card:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPlaces: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var selectedLang by remember {
        mutableStateOf(LanguagePreference.get(prefs))
    }
    val dataClient = remember { Wearable.getDataClient(context) }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_label))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLang = lang
                                    LanguagePreference.set(prefs, lang)
                                    syncLanguageToWatch(dataClient, lang)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLang == lang,
                                onClick = {
                                    selectedLang = lang
                                    LanguagePreference.set(prefs, lang)
                                    syncLanguageToWatch(dataClient, lang)
                                    // Recreate so LocalizedContext in onCreate picks up the new locale
                                    (context as? android.app.Activity)?.recreate()
                                }
                            )
                            Text(lang.displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            // Locations card (unchanged)
            Card(modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPlaces() }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.locations_title),
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.locations_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun syncLanguageToWatch(dataClient: DataClient, lang: AppLanguage) {
    val request = PutDataMapRequest.create("/settings/language").apply {
        dataMap.putString("language", lang.code)
    }.asPutDataRequest().setUrgent()
    dataClient.putDataItem(request)
}
```

Add the necessary imports at the top of MainActivity.kt:

```kotlin
import androidx.compose.material3.RadioButton
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.vincent.polsnotitie.language.AppLanguage
import com.vincent.polsnotitie.language.LanguagePreference
```

- [ ] **Step 2: Build**

```
.\gradlew.bat :mobile:assembleDebug 2>&1 | Select-String "error:" | Select-Object -First 20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```
git add mobile/src/main/java/com/vincent/polsnotitie/MainActivity.kt
git commit -m "feat: add language selector to settings screen, sync to watch via DataLayer"
```

---

## Task 14: Wear UI strings

**Files:**
- Modify: `wear/src/main/res/values/strings.xml`
- Create: `wear/src/main/res/values-de/strings.xml`
- Create: `wear/src/main/res/values-en/strings.xml`

- [ ] **Step 1: Replace wear/src/main/res/values/strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">VoiceDrop</string>
    <string name="tile_label">Inspreken</string>
    <string name="tap_to_record">Tik om in te spreken</string>
    <string name="sending">Verzenden…</string>
    <string name="sent">Verzonden ✓</string>
    <string name="nothing_heard">Niets verstaan, opnieuw?</string>
    <string name="send_failed">Versturen mislukt, opnieuw?</string>
    <string name="not_available">Spraakherkenning niet beschikbaar</string>
    <string name="record_button">Inspreken</string>
    <string name="speech_prompt">Spreek je notitie in</string>
</resources>
```

- [ ] **Step 2: Create wear/src/main/res/values-de/strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">VoiceDrop</string>
    <string name="tile_label">Aufnehmen</string>
    <string name="tap_to_record">Tippen zum Aufnehmen</string>
    <string name="sending">Senden…</string>
    <string name="sent">Gesendet ✓</string>
    <string name="nothing_heard">Nichts verstanden, erneut?</string>
    <string name="send_failed">Senden fehlgeschlagen, erneut?</string>
    <string name="not_available">Spracherkennung nicht verfügbar</string>
    <string name="record_button">Aufnehmen</string>
    <string name="speech_prompt">Notiz einsprechen</string>
</resources>
```

- [ ] **Step 3: Create wear/src/main/res/values-en/strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">VoiceDrop</string>
    <string name="tile_label">Record</string>
    <string name="tap_to_record">Tap to record</string>
    <string name="sending">Sending…</string>
    <string name="sent">Sent ✓</string>
    <string name="nothing_heard">Nothing heard, retry?</string>
    <string name="send_failed">Send failed, retry?</string>
    <string name="not_available">Speech recognition not available</string>
    <string name="record_button">Record</string>
    <string name="speech_prompt">Speak your note</string>
</resources>
```

- [ ] **Step 4: Commit**

```
git add wear/src/main/res/
git commit -m "feat: add wear UI strings for NL/DE/EN"
```

---

## Task 15: Wear app — read language from Data Layer

**Files:**
- Modify: `wear/src/main/java/com/vincent/polsnotitie/presentation/MainActivity.kt`

The wear app reads `/settings/language` from the Data Layer before each recording and uses it for both the `RecognizerIntent` locale and the UI status strings.

- [ ] **Step 1: Rewrite wear MainActivity.kt**

Replace the entire file:

```kotlin
package com.vincent.polsnotitie.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.vincent.polsnotitie.R
import com.vincent.polsnotitie.presentation.theme.PolsnotitieTheme
import java.util.Locale

private const val PREFS_NAME = "wear_settings"
private const val KEY_LANGUAGE = "wear_language"

class MainActivity : ComponentActivity() {
    private val autoStartTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("autostart", false) == true) autoStartTrigger.intValue++
        val localizedCtx = applyLanguage(this)
        setContent {
            CompositionLocalProvider(LocalContext provides localizedCtx) {
                MemoScreen(autoStartTrigger = autoStartTrigger.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("autostart", false) == true) autoStartTrigger.intValue++
    }
}

private fun applyLanguage(context: Context): Context {
    val code = resolveLanguageCode(context)
    val locale = Locale.forLanguageTag(code)
    val config = android.content.res.Configuration(context.resources.configuration)
        .also { it.setLocale(locale) }
    return context.createConfigurationContext(config)
}

private fun resolveLanguageCode(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return try {
        val uri = android.net.Uri.parse("wear://*/settings/language")
        val items = Tasks.await(Wearable.getDataClient(context).getDataItems(uri))
        val code = items.firstOrNull()
            ?.let { DataMapItem.fromDataItem(it).dataMap.getString("language") }
            ?: prefs.getString(KEY_LANGUAGE, "nl")
            ?: "nl"
        items.release()
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
        code
    } catch (e: Exception) {
        prefs.getString(KEY_LANGUAGE, "nl") ?: "nl"
    }
}

private enum class Status { Idle, Sending, Sent, NothingHeard, Error, NotAvailable }

@Composable
fun MemoScreen(autoStartTrigger: Int = 0) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(Status.Idle) }

    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!text.isNullOrEmpty()) {
                status = Status.Sending
                MemoSender.send(context, text) { ok ->
                    status = if (ok) Status.Sent else Status.Error
                }
            } else {
                status = Status.NothingHeard
            }
        } else {
            status = Status.Idle
        }
    }

    fun startRecognition() {
        val code = resolveLanguageCode(context)
        val locale = when (code) {
            "de" -> "de-DE"
            "en" -> "en-GB"
            else -> "nl-NL"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            status = Status.NotAvailable
        }
    }

    LaunchedEffect(autoStartTrigger) {
        if (autoStartTrigger > 0) startRecognition()
    }

    val statusText = when (status) {
        Status.Idle          -> stringResource(R.string.tap_to_record)
        Status.Sending       -> stringResource(R.string.sending)
        Status.Sent          -> stringResource(R.string.sent)
        Status.NothingHeard  -> stringResource(R.string.nothing_heard)
        Status.Error         -> stringResource(R.string.send_failed)
        Status.NotAvailable  -> stringResource(R.string.not_available)
    }

    PolsnotitieTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Button(onClick = { startRecognition() }) {
                Text("🎤  ${stringResource(R.string.record_button)}")
            }
            Text(
                text = statusText,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
```

Note: `resolveLanguageCode` calls `Tasks.await` on the main thread inside `startRecognition`. This is a known limitation — for the initial call in `onCreate` it's also on the main thread. If this causes ANR issues in practice, move the resolution to a background coroutine with a cached value. For now this matches the design.

- [ ] **Step 2: Build wear module**

```
.\gradlew.bat :wear:assembleDebug 2>&1 | Select-String "error:" | Select-Object -First 20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all unit tests**

```
.\gradlew.bat :mobile:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```
git add wear/src/main/java/com/vincent/polsnotitie/presentation/MainActivity.kt
git commit -m "feat: wear app reads language from DataLayer, uses for RecognizerIntent and UI strings"
```

---

## Final verification

- [ ] **Build both modules**

```
.\gradlew.bat :mobile:assembleDebug :wear:assembleDebug
```

Expected: BUILD SUCCESSFUL for both.

- [ ] **Run all tests**

```
.\gradlew.bat :mobile:testDebugUnitTest
```

Expected: all tests pass including the new DE/EN classifier, time parser, and place parser tests.

- [ ] **Commit final state**

```
git add -A
git commit -m "feat: multi-language support NL/DE/EN complete"
```
