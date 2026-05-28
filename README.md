# VoiceDrop

Hands-free voice notes on your Wear OS watch. Speak a note — it gets transcribed on-device and sent to your phone instantly. No audio files are ever stored.

## What it does

Tap the button on your watch (or the tile), speak your note, and it appears on your phone categorised and ready. That's the whole flow.

**Categories** are detected automatically from the first word you say:

| Say | Category |
|-----|----------|
| Groceries / Boodschappen / Einkaufen | Shopping list |
| Reminder / Herinnering / Erinnerung | Reminder |
| Todo / Task / Taak / Aufgabe | To-do |
| Idea / Idee | Ideas |
| Appointment / Agenda / Termin | Calendar |

**Reminders** understand natural time expressions — "tomorrow at 9", "in one hour", "friday afternoon", "when I get home". Location-based reminders trigger when you arrive at a saved place (home, work, supermarket).

**Calendar events** (Agenda category) go straight into Google Calendar.

**Shopping list** has its own screen with checkboxes. Checked items disappear after 15 minutes. A home screen widget shows what's still on the list.

## Languages

The app supports Dutch, German and English — both for the UI and for speech recognition. Set your language in Settings. Category keywords work across all languages regardless of which one is active.

## Tech

- **Wear OS module** — Jetpack Compose for Wear, `RecognizerIntent` for speech (no `RECORD_AUDIO` permission needed), `DataClient` to send notes to the phone
- **Mobile module** — Jetpack Compose, Room database, `WearableListenerService` to receive notes
- **Data layer** — notes travel as `DataItem` objects over the Wearable Data Layer, so they sync even when the watch and phone are temporarily out of range
- **Location reminders** — geofences via Google Play Services, osmdroid for the map picker (no API key required)
- **Notifications** — exact alarms (`USE_EXACT_ALARM`) for time reminders, rescheduled on boot

## Modules

```
:wear   — Wear OS app (voice recording + tile)
:mobile — Phone app (receive, classify, store, display)
```

Both share `applicationId = com.vincent.polsnotitie`, which is required for the Wearable Data Layer to connect them.

## Build

```powershell
.\gradlew.bat :wear:assembleDebug
.\gradlew.bat :mobile:assembleDebug

# Unit tests
.\gradlew.bat :mobile:testDebugUnitTest
```

Requires Android Studio with the Wear OS emulator or a physical Wear OS device. The Pixel Watch connects over Wireless Debugging (the charging puck is power-only).

## Permissions

| Permission | Why |
|---|---|
| `READ_CALENDAR` / `WRITE_CALENDAR` | Insert calendar events |
| `POST_NOTIFICATIONS` | Reminder and calendar notifications |
| `ACCESS_FINE_LOCATION` / `ACCESS_BACKGROUND_LOCATION` | Location-based reminders |
| `USE_EXACT_ALARM` | Fire reminders at the exact time |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after reboot |

Speech recognition uses the system's built-in recogniser, so no microphone permission is needed in the app itself.

## License

MIT
