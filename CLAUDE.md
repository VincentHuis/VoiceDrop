# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Wat is dit

PolsNotitie: tikloos spraaknotities maken op een Wear OS-horloge (Pixel Watch 4). Je
spreekt een notitie in op het horloge → wordt lokaal naar tekst omgezet (Nederlands) →
gaat via de Wearable Data Layer naar de telefoon → wordt daar opgeslagen en getoond.
Er worden géén audiobestanden bewaard, alleen tekst.

Communicatie met de gebruiker (Vincent) verloopt in het **Nederlands**.

## Modules

- `:wear` — Wear OS-app (Jetpack Compose voor Wear). Neemt spraak op en verstuurt.
- `:mobile` — telefoon-app (Jetpack Compose + Room). Ontvangt, classificeert, bewaart, toont.

Beide modules delen `applicationId = com.vincent.polsnotitie`. Dat is **vereist** voor de
Data Layer: alleen apps met dezelfde applicationId én dezelfde signing-key kunnen via
`DataClient` communiceren. Debug-builds delen de debug-keystore, dus dat werkt out of the box.

## Architectuur / data flow

```
Horloge (:wear)                         Data Layer              Telefoon (:mobile)
MainActivity (mic-knop)                                         MemoListenerService
 → RecognizerIntent (nl-NL)    ──/memo/<uuid>──────▶            (WearableListenerService)
 → MemoSender.putDataItem        {id, text, timestamp}           → CategoryClassifier
 → status "Verzonden ✓"                                          → Room insert
                                                                 → verwijdert DataItem
MemoTileService (Tile)                                          MainActivity (lijst/zoek/
 → opent MainActivity met                                        kleur-chip/delete/delen)
   extra "autostart"=true
```

Belangrijke ontwerpkeuzes:
- **Transport = `DataClient` met uniek pad `/memo/<uuid>`** (niet `MessageClient`). Reden:
  een DataItem blijft op het horloge staan en synct vanzelf zodra de telefoon weer in
  bereik is — offline-bestendig. Identieke teksten moeten een uniek pad krijgen, anders
  ziet de ontvanger ze niet als wijziging. De listener **verwijdert** het DataItem na
  verwerking zodat ze niet opstapelen.
- **Telefoon is de enige bron van waarheid** (Room). Het horloge bewaart niets.
- **Geen `RECORD_AUDIO`-permissie** in de wear-app: `RecognizerIntent` start de
  systeem-spraak-UI die zijn eigen mic-permissie regelt.
- **Tile-autostart**: een tik op de Tile opent `MainActivity` met extra `autostart=true`.
  De activity is `launchMode="singleTop"` en leest de extra in zowel `onCreate` als
  `onNewIntent` via een trigger-teller, zodat de opname óók direct start als de app al
  draaide (anders moest je nog een keer tikken).
- **Categorieën**: `CategoryClassifier` (pure Kotlin, in `:mobile` `data/`) bepaalt op de
  telefoon de categorie uit het eerste woord (of eerste twee, voor "to do") met
  genormaliseerde Levenshtein-gelijkenis (drempel 0.7) tegen trefwoordlijsten per
  `Category`. Het trefwoord wordt uit de opgeslagen tekst gehaald. Geen match → `OVERIG`.
  Categorie wordt als enum-naam (`String`) in Room bewaard; kleur-mapping zit in de UI
  (`categoryColor` in `MainActivity.kt`) en gebruikt het merk-palet (zie Huisstijl):
  Boodschappen coral, To-do baltic-blue, Ideeën amber-gold, Herinneringen steel-azure,
  Overig platinum. Agenda wordt niet opgeslagen, dus die chip wordt nooit getoond. De
  chip-tekstkleur kiest automatisch donker/wit op basis van de luminantie van de kleur.
- **Boodschappenlijst**: knop rechtsboven in de balk opent `ShoppingScreen` (navigatie via
  een simpele `rememberSaveable` boolean in `AppRoot`, géén Navigation-library). Toont alle
  memo's met categorie Boodschappen, elk met een vinkje. Afvinken zet `Memo.checkedAt`;
  afgevinkte items blijven zichtbaar (doorgestreept, onderaan) en worden **15 min** later
  verwijderd (`CHECKED_TTL_MS`). Opruimen is lui: een `LaunchedEffect`-lus draait
  `deleteCheckedBefore(now - 15min)` bij openen en elke 30s zolang het scherm open is. Geen
  WorkManager. De hoofdlijst (`getAll`/`search`) sluit categorie Boodschappen uit, dus die
  zie je alleen in het Boodschappen-scherm.
- **Herinneringen met tijd** (`reminder/`): voor categorie Herinneringen parst
  `ReminderTimeParser` (pure Kotlin, conservatief: `null` bij twijfel) een Nederlands
  tijdstip uit de tekst ("morgen om 9 uur", "over een uur", "vrijdag om 15:30") en haalt de
  tijd-woorden uit de tekst. In `MemoListenerService`: lukt parsen → `remindAt` opslaan +
  `ReminderScheduler` plant een exact alarm (AlarmManager, `USE_EXACT_ALARM`); lukt het niet
  → `ReminderNotifications.notifyAddTime` toont een notificatie die `MainActivity` opent met
  extra `setTimeForMemo` → `ReminderTimeScreen` (datum/tijd-picker). Op het moment zelf vuurt
  `ReminderReceiver` → notificatie (Wear OS spiegelt die naar de pols). `BootReceiver`
  herplant toekomstige alarmen na herstart. Herinnering-kaarten in de lijst tonen de tijd en
  zijn aantikbaar om die te zetten/wijzigen. Eén herinneringsmoment per memo (geen herhaling).
  Parser-gedrag is gedekt door `ReminderTimeParserTest`.
- **Gedeelde verwerking** (`MemoProcessor`): classificeren → agenda/herinnering(tijd of plek)/
  gewone memo opslaan. Wordt aangeroepen door zowel `MemoListenerService` (memo's van het
  horloge) als de **microfoonknop in de telefoon-app** (naast de zoekbalk), zodat je ook
  zonder horloge kunt inspreken — zelfde pijplijn (`RecognizerIntent` nl-NL → `MemoProcessor`).
- **Locatie-herinneringen** (`location/`): drie vaste plekken (`PlaceType`: THUIS/WERK/
  SUPERMARKT) zet je in de app op een kaart (osmdroid, geen API-key) met 100 m straal,
  opgeslagen in de `places`-tabel. `PlaceParser` herkent "als ik thuis ben / op werk / bij de
  supermarkt" in een herinnering en zet `Memo.placeId`. `GeofenceManager` registreert
  geofences (enter-trigger) voor alle ingestelde plekken; `GeofenceBroadcastReceiver` toont
  bij aankomst de herinnering(en) voor die plek (eenmalig, daarna verwijderd) en bij de
  supermarkt de boodschappen (1–3 in de melding, anders verwijzing naar de app). Vereist
  `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` ("altijd toestaan") voor werking met
  app dicht; `BootReceiver` herregistreert geofences na herstart. Parser-gedrag getest in
  `PlaceParserTest`.
- **Agenda → Google Calendar** (`calendar/`): categorie Agenda (trefwoorden agenda/afspraak/
  kalender) wordt niet in Room opgeslagen. In `MemoListenerService.handleAgenda` wordt de tijd
  via `ReminderTimeParser` bepaald en de rest is de titel. `CalendarHelper.insertEvent` zet de
  afspraak (duur 1u) direct in de primaire Google-agenda via de Calendar-provider
  (`CalendarContract`, vereist `READ_CALENDAR`/`WRITE_CALENDAR`), die naar Google Calendar
  synct. Lukt dat niet (geen rechten/tijd) → `notifyAddToCalendar` toont een notificatie met
  een `ACTION_INSERT`-intent (voorgevuld) zodat de gebruiker 'm zelf opslaat. Permissies
  worden bij appstart gevraagd in `RequestPermissions` (samen met POST_NOTIFICATIONS).
- **Home-screen widget** (`widget/`): klassieke RemoteViews-collectiewidget die de nog te
  kopen boodschappen (`byCategoryUncheckedNow`) toont. `ShoppingWidgetProvider` (receiver)
  + `ShoppingWidgetService`/factory + layouts in `res/layout/widget_*.xml` en config in
  `res/xml/shopping_widget_info.xml`. Tikken opent `MainActivity` met extra
  `openShopping=true` → start direct in het Boodschappen-scherm. `ShoppingWidget.refresh()`
  wordt aangeroepen na een nieuwe boodschap (in `MemoListenerService`) en bij afvinken/
  opruimen (in `ShoppingScreen`). Afvinken vanaf de widget zelf zit er (nog) niet in.

## Bouwen, testen, deployen

Gradle wrapper (`gradlew.bat`) op Windows/PowerShell.

```powershell
# Bouwen
.\gradlew.bat :wear:assembleDebug
.\gradlew.bat :mobile:assembleDebug

# Unit tests (mobile) — bv. alleen de classifier:
.\gradlew.bat :mobile:testDebugUnitTest --tests "com.vincent.polsnotitie.CategoryClassifierTest"
```

`adb` staat op de gebruikers-PATH (`%LOCALAPPDATA%\Android\Sdk\platform-tools`). In een
verse PowerShell ververs je de PATH eventueel met:
```powershell
$env:Path = [Environment]::GetEnvironmentVariable("Path","User") + ";" + [Environment]::GetEnvironmentVariable("Path","Machine")
```

Installeren op specifieke apparaten (gebruik `adb devices -l` voor de serials):
```powershell
adb -s <watch-serial> install -r wear\build\outputs\apk\debug\wear-debug.apk
adb -s <phone-serial> install -r mobile\build\outputs\apk\debug\mobile-debug.apk
```

De Pixel Watch 4 verbindt via **Wireless debugging** (de oplaadpuck is alleen stroom, geen
data). Pairen + verbinden:
```powershell
adb pair <ip>:<pair-poort> <koppelcode>   # code als 2e argument; pipen werkt niet
adb connect <ip>:<verbind-poort>           # verbind-poort ≠ pair-poort
```

## Belangrijke build-eigenaardigheden (AGP 9)

- AGP 9 heeft **ingebouwde Kotlin**: er is géén `kotlin-android`-plugin gedeclareerd.
  Alleen de Compose-plugin (`kotlin-compose`) en, in `:mobile`, de `ksp`-plugin.
- **KSP botst met de ingebouwde Kotlin**. Opgelost met
  `android.disallowKotlinSourceSets=false` in `gradle.properties`. Niet weghalen.
- `:mobile` houdt `com.google.android.material` (Views) als dependency aan, puur omdat
  `res/values/themes.xml` een `Theme.Material3.*` venster-thema gebruikt; de UI zelf is
  volledig Compose.
- Versies staan centraal in `gradle/libs.versions.toml` (version catalog). Let op: de
  catalog-entry `androidx-compose-material3` verwijst naar **Wear** Compose Material3; de
  telefoon gebruikt `androidx-material3` (gewone Material3, BOM-versie).
- Bij schema-wijzigingen in Room: `version` in `MemoDatabase` ophogen én een migratie
  toevoegen. `exportSchema = true` (schema's komen in `mobile/schemas/` — bewaren!) en de
  `fallbackToDestructiveMigration` is verwijderd, dus zonder migratie weigert Room te openen
  i.p.v. stilletjes data te wissen. Additieve wijzigingen (kolom/tabel erbij) kunnen via een
  `AutoMigration(from, to)` in de `@Database`-annotatie of een handgeschreven `Migration`
  (zie `MIGRATION_5_6` in `MemoDatabase.kt`).

## Huisstijl / kleuren

Merk-palet (VoiceDrop). Mobile: `ui/theme/Color.kt` (+ `colors.xml` voor View-widgets);
wear: `presentation/theme/Color.kt`. Beide thema's (`PolsnotitieTheme`) bouwen hierop een
vast Material3-`ColorScheme` — de mobile-app gebruikt dus **geen** dynamic color meer, zodat
het merk overal consistent is.

| Naam | HEX | Rol |
|------|-----|-----|
| amber-gold | `#FFBF00` | tertiary / accent, categorie Ideeën |
| vibrant-coral | `#FE5F55` | error / accent, categorie Boodschappen, kaartpin |
| steel-azure | `#004E89` | primary (light), categorie Herinneringen |
| baltic-blue | `#1A659E` | secondary / primary (dark), categorie To-do |
| platinum | `#EFEFEF` | surface/achtergrond-neutraal, categorie Overig |
| ink-dark | `#1A1A1A` | tekst/neutraal donker (afgeleid, niet uit basispalet) |

## Tile toevoegen aan de wijzerplaat

De Tile (`MemoTileService`) staat niet automatisch in de carousel. Toevoegen via:
wijzerplaat ingedrukt houden → Tegels toevoegen → "Inspreken" kiezen.

## Spec

Het oorspronkelijke ontwerp staat in
`docs/superpowers/specs/2026-05-28-polsnotitie-design.md` (categorieën zijn er later bij
gekomen en hier in CLAUDE.md beschreven).
