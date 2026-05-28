# PolsNotitie — Ontwerp

Datum: 2026-05-28
Package: `com.vincent.polsnotitie`

## Doel
Tikloos notuleren met de Pixel Watch 4: spreek een notitie in op het horloge, deze
wordt via de ingebouwde spraakherkenning (Nederlands) naar tekst omgezet en verschijnt
op de telefoon om terug te lezen. Geen audiobestanden — alleen tekst.

## Beslissingen (uit brainstorm)
- Horloge-flow: na herkenning **direct versturen**, met korte "Verzonden"-bevestiging.
- Opslag: telefoon bewaart memo's **lokaal in Room**. Horloge bewaart **niets**.
- Offline: telefoon onbereikbaar → notitie blijft op horloge en synct **automatisch later**
  (kernreden voor DataClient).
- Telefoon-functies: verwijderen, delen/kopiëren, zoeken. **Geen** notificaties.
- Start op horloge: app met **één grote mic-knop**. Geen Tile (later mogelijk).
- Transport: **DataClient met unieke paden** (`/memo/<uuid>`).
- Telefoon-UI wordt omgebouwd van XML naar **Jetpack Compose**.

## Architectuur
```
Horloge (wear)                     Data Layer            Telefoon (mobile)
MainActivity (Compose)                                   MemoListenerService
 └ grote mic-knop          ──/memo/<uuid>──────▶          (WearableListenerService)
RecognizerIntent (nl-NL)     {id, text, timestamp}         └ schrijf naar Room
 → DataClient.putDataItem                                     + verwijder DataItem
 → toont "Verzonden"                                       MainActivity (lijst/zoek/
                                                            delete/delen)
```
Horloge bewaart niets; telefoon (Room) is enige bron van waarheid.

## Componenten

### Horloge (`wear`)
- `MainActivity` (Wear Compose): één scherm — grote mic-knop + statustekst
  ("Tik om in te spreken" → "Verzonden ✓" / "Niets verstaan, opnieuw?").
- Mic-knop start `RecognizerIntent` (`EXTRA_LANGUAGE = "nl-NL"`) via `ActivityResultLauncher`.
- `MemoSender`: `PutDataMapRequest` op `/memo/<uuid>` met velden `id` (uuid),
  `text`, `timestamp`; `DataClient.putDataItem(...).setUrgent()`.
- Runtime-permissie `RECORD_AUDIO` vóór eerste opname.

### Telefoon (`mobile`)
- `MemoListenerService` (`WearableListenerService`): `onDataChanged` → voor items met
  prefix `/memo/`: lees velden, schrijf naar Room (`OnConflict.IGNORE`),
  `DataClient.deleteDataItems(uri)` om op te ruimen.
- Room: `Memo(id: String, text: String, timestamp: Long)`, `MemoDao`
  (insert, `getAllFlow()`, `searchFlow(query)`, `delete`), `MemoDatabase`.
- `MainActivity` (Compose): lijst nieuwste-boven met tijdstip, zoekbalk,
  per item delen/kopiëren + swipe-to-delete.

## Data flow (gelukkig pad)
1. Tik mic-knop → spraakherkenning → tekst.
2. `MemoSender` schrijft `DataItem` `/memo/<uuid>`.
3. Data Layer synct (direct of zodra verbonden).
4. Telefoon `onDataChanged` → Room insert → DataItem verwijderd.
5. Lijst-UI (observeert Room `Flow`) toont memo automatisch.

## Foutafhandeling
- Geen spraak / herkenning mislukt → "Niets verstaan, opnieuw?".
- Geen spraakherkenning beschikbaar → nette melding.
- Telefoon onbereikbaar → DataItem blijft staan, synct later (geen actie nodig).
- Dubbele aflevering → `id` is primary key + `OnConflict.IGNORE`.

## Testen
- Unit: `MemoDao` (insert/search/delete) met in-memory Room; mapping DataItem→Memo.
- Handmatig end-to-end op Pixel Watch 4 + telefoon: inspreken → verschijnt; vliegtuigstand
  voor latere sync.
- Spraakherkenning en Data Layer → end-to-end op toestel verifiëren.

## Buiten scope (YAGNI)
Audiobestanden, account/cloud, notificaties, bewerken op horloge, Tile.
