# Meertalige ondersteuning — PolsNotitie

**Datum:** 2026-05-28
**Status:** Goedgekeurd

## Samenvatting

De app ondersteunt drie talen: Nederlands (standaard), Duits en Engels. De gebruiker kiest de taal in de telefoon-app; deze voorkeur wordt gesynchroniseerd naar het horloge. Alle lagen worden meertalig: UI-tekst, spraakherkenning, categorie-trefwoorden, tijdparser en plaatsparser.

---

## 1. Taalvoorkeur opslaan en synchroniseren

### AppLanguage enum

```kotlin
enum class AppLanguage(val code: String, val locale: String, val displayName: String) {
    DUTCH("nl",  "nl-NL", "Nederlands"),
    GERMAN("de", "de-DE", "Deutsch"),
    ENGLISH("en","en-GB", "English")
}
```

Geplaatst in `:mobile`, package `com.vincent.polsnotitie.language`.

### Opslag op de telefoon

`SharedPreferences` met key `"app_language"`, waarde `"nl"` / `"de"` / `"en"`. Standaard `"nl"`. Geen DataStore — het is één string-waarde.

Een `LanguagePreference`-object biedt `get(prefs)` en `set(prefs, lang)`.

### Sync naar het horloge

Bij elke taalwijziging op de telefoon wordt een DataItem gestuurd:

- **Pad:** `/settings/language`
- **DataMap:** `{ "language": "nl" }`

Het horloge leest dit DataItem on-demand in `MainActivity` vóór elke opname via `Tasks.await(dataClient.getDataItems(uri))`. Fallback als het DataItem ontbreekt: `"nl-NL"`. De ontvangen waarde wordt ook lokaal opgeslagen in de wear-app's eigen SharedPreferences (`"wear_language"`) zodat de fallback na herstart behouden blijft.

---

## 2. LanguageConfig data model

### Interfaces en data classes

```kotlin
// package com.vincent.polsnotitie.language

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
    val numberWords: Map<String, Int>,          // "een"->1, "zwei"->2
    val weekdays: Map<String, Int>,             // "maandag"->Calendar.MONDAY
    val tomorrowWords: List<String>,
    val dayAfterTomorrowWords: List<String>,
    val nextWeekWords: List<String>,
    val timeOfDay: Map<String, Pair<Int, Int>>, // "ochtend"->(9,0)
    val inXPattern: Regex,                      // "over X uur/min/dagen"
    val atTimePattern: Regex,                   // "om HH:mm"
    val halfHourIsBefore: Boolean,              // NL/DE: true (half 9=8:30), EN: false (half 9=9:30)
    val halfPattern: Regex,
    val quarterOverPattern: Regex,
    val quarterToPattern: Regex
)
```

### Implementaties

Drie objecten in `language/configs/`:

| Bestand | Inhoud |
|---|---|
| `NlLanguageConfig.kt` | Huidige hardcoded data uit de bestaande parsers — extraheren, niet herschrijven |
| `DeLanguageConfig.kt` | Duits: Einkaufen/Idee/Aufgabe/Erinnerung/Kalender, weekdagen (Montag…), Zahlwörter, halb/viertel-patronen |
| `EnLanguageConfig.kt` | Engels: groceries/idea/task/reminder/calendar, weekdays (Monday…), half nine = 9:30 |

### LanguageProvider

```kotlin
object LanguageProvider {
    fun get(prefs: SharedPreferences): LanguageConfig =
        when (prefs.getString("app_language", "nl")) {
            "de"  -> DeLanguageConfig
            "en"  -> EnLanguageConfig
            else  -> NlLanguageConfig
        }
}
```

---

## 3. Parsers en classifier refactoren

### Algemeen principe

`CategoryClassifier`, `ReminderTimeParser` en `PlaceParser` veranderen van `object` naar `class` met constructor-parameter `config: LanguageConfig`. Het algoritme (Levenshtein, tijdrekenwerk, regex-matching) blijft ongewijzigd. Alleen hardcoded data-literals worden vervangen door `config.*`-referenties.

### CategoryClassifier

```kotlin
class CategoryClassifier(private val config: LanguageConfig) {
    fun classify(text: String): Category { /* algoritme ongewijzigd */ }
    fun stripKeyword(text: String, category: Category): String { /* ongewijzigd */ }
}
```

Keywords worden opgehaald uit `config.categoryKeywords[category]`.

### ReminderTimeParser

```kotlin
class ReminderTimeParser(private val config: LanguageConfig) {
    fun parse(text: String, now: LocalDateTime): ParseResult? { /* algoritme ongewijzigd */ }
}
```

Alle `val numberWords = mapOf(...)`, `val weekdays = mapOf(...)`, etc. vervallen; vervangen door `config.timeParser.*`. De `halfHourIsBefore`-boolean stuurt de half-uur-berekening:
- `true` (NL/DE): "half negen" / "halb neun" → 8:30 (een half uur vóór negen)
- `false` (EN): "half nine" → 9:30 (een half uur na negen)

### PlaceParser

```kotlin
class PlaceParser(private val config: LanguageConfig) {
    fun parse(text: String): PlaceType? { /* algoritme ongewijzigd */ }
}
```

Patronen komen uit `config.placePatterns.homePatterns`, `.workPatterns`, `.shopPatterns`.

### MemoProcessor

Instantieert de drie klassen bij aanmaak:

```kotlin
class MemoProcessor(prefs: SharedPreferences, ...) {
    private val langConfig = LanguageProvider.get(prefs)
    private val classifier = CategoryClassifier(langConfig)
    private val timeParser = ReminderTimeParser(langConfig)
    private val placeParser = PlaceParser(langConfig)
}
```

Bij taalwijziging wordt `MemoProcessor` opnieuw aangemaakt (dit gebeurt toch per verwerking via `MemoListenerService`).

---

## 4. UI-strings

### Aanpak

Alle hardcoded strings in Kotlin-composables (`:mobile` en `:wear`) gaan naar `strings.xml`. Vervolgens worden `values-de/strings.xml` en `values-en/strings.xml` aangemaakt met vertalingen.

De app volgt **niet** de systeemlocale van het apparaat — de taalinstelling is een expliciete gebruikersvoorkeur. Implementatie via een `LocalizedContext`:

```kotlin
fun Context.withAppLanguage(prefs: SharedPreferences): Context {
    val lang = prefs.getString("app_language", "nl") ?: "nl"
    val locale = Locale.forLanguageTag(lang)
    val config = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(config)
}
```

In `MainActivity` (en de wear `MainActivity`) wordt de `Context` vóór `setContent` gewikkeld en als `CompositionLocal` meegegeven zodat `stringResource()` de juiste taal pakt:

```kotlin
val localizedCtx = context.withAppLanguage(prefs)
setContent {
    CompositionLocalProvider(LocalContext provides localizedCtx) {
        PolsnotitieTheme { AppRoot() }
    }
}
```

### Category-weergavenamen

De `Category`-enum behoudt zijn Kotlin-naam. Een extension function levert de vertaalde naam:

```kotlin
fun Category.displayName(context: Context): String = context.getString(when (this) {
    Category.BOODSCHAPPEN  -> R.string.category_groceries
    Category.TODO          -> R.string.category_todo
    Category.IDEEEN        -> R.string.category_ideas
    Category.HERINNERINGEN -> R.string.category_reminders
    Category.AGENDA        -> R.string.category_agenda
    Category.OVERIG        -> R.string.category_other
})
```

### PlaceType-weergavenamen

Analoog: extension function `PlaceType.displayName(context)` met resource-keys `place_home`, `place_work`, `place_supermarket`.

---

## 5. Instellingenscherm

De bestaande `SettingsScreen` in `MainActivity.kt` krijgt bovenaan een **Taal**-kaart met drie radiobuttons (Material3):

```
┌─ Taal ────────────────────────────┐
│  ● Nederlands                      │
│  ○ Deutsch                         │
│  ○ English                         │
└───────────────────────────────────┘
```

Bij selectie:
1. `LanguagePreference.set(prefs, lang)`
2. DataItem `/settings/language` sturen naar het horloge
3. `LocalizedContext` opnieuw instellen zodat de UI direct herlaadt

De radiobuttons gebruiken de `displayName`-velden van `AppLanguage`.

---

## 6. Wear-app

### Spraakherkenning

`MainActivity` leest vóór elke opname het DataItem `/settings/language`:

```kotlin
fun resolveLanguage(): String {
    val uri = Uri.parse("wear://*/settings/language")
    val items = Tasks.await(dataClient.getDataItems(uri))
    val code = items.firstOrNull()
        ?.let { DataMapItem.fromDataItem(it).dataMap.getString("language") }
        ?: wearPrefs.getString("wear_language", "nl")
    items.release()
    return AppLanguage.entries.first { it.code == code }.locale  // bijv. "de-DE"
}
```

De `RecognizerIntent` krijgt `EXTRA_LANGUAGE = resolveLanguage()`.

### Prompt-tekst

De prompt ("Spreek je notitie in" etc.) wordt meegenomen in `strings.xml` van het wear-module en opgehaald via `LocalizedContext` op basis van dezelfde taalinstelling.

### Wear UI-strings

`wear/src/main/res/values/strings.xml` bevat de Nederlandse standaard. `values-de/` en `values-en/` worden aangemaakt voor de Duitstalige en Engelstalige varianten. De statusberichten ("Verzonden ✓", "Bezig…", "Niet beschikbaar") worden vertaald.

---

## 7. Testbaar­heid

- `NlLanguageConfig`, `DeLanguageConfig`, `EnLanguageConfig` zijn pure Kotlin-objecten — geen Android-context nodig.
- `CategoryClassifier`, `ReminderTimeParser` en `PlaceParser` zijn gewone klassen — unit-testbaar zonder instrumentatie.
- Bestaande `ReminderTimeParserTest` en `PlaceParserTest` worden uitgebreid met DE- en EN-varianten door de config te wisselen.
- `CategoryClassifierTest` idem.

---

## 8. Niet in scope

- Automatische taaldetectie uit de spraakstroom
- Meer dan drie talen (Frans, Spaans, Portugees komen later)
- Vertaling van bestaande opgeslagen memo's bij taalwijziging
- Tile-tekst op het horloge (blijft voorlopig Nederlands, Tile-layout is beperkt)

---

## Bestandsstructuur (nieuw)

```
mobile/src/main/java/com/vincent/polsnotitie/
  language/
    AppLanguage.kt
    LanguageConfig.kt          (interface + PlacePatternConfig + TimeParserConfig)
    LanguageProvider.kt
    LanguagePreference.kt
    configs/
      NlLanguageConfig.kt
      DeLanguageConfig.kt
      EnLanguageConfig.kt

mobile/src/main/res/
  values/strings.xml           (uitgebreid met alle UI-strings)
  values-de/strings.xml
  values-en/strings.xml

wear/src/main/res/
  values/strings.xml           (uitgebreid)
  values-de/strings.xml
  values-en/strings.xml
```
