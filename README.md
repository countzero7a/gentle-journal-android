# 🌿 Gentle Journal

A private, offline-first journaling Android app that helps you notice patterns between emotions, thoughts, actions, and time — without diagnosis, judgment, or surveillance.

**Features:** feeling-first entries with light formatting, a timeline with search and a month calendar, on-device patterns (insights and charts), a home-screen widget that logs a feeling in one tap, an optional fingerprint or screen-lock app lock, and password-protected automatic backups to a folder you choose. 12 languages, no accounts, no network.

## Architecture

```
com.gentlejournal/
├── data/                          # Data layer (Room + Repository)
│   ├── entity/                    # Room entities & relations (entries, tags, insights)
│   ├── dao/                       # EntryDao, TagDao, InsightDao, EngagementDao
│   ├── repository/
│   │   └── JournalRepository.kt  # Transactions for saving, importing, deleting
│   ├── JournalDatabase.kt        # Room DB (SQLCipher) with default tag seeding
│   ├── DatabaseKeyManager.kt     # Random DB key, encrypted with an Android Keystore key
│   ├── PassphraseResolver.kt     # Where that key comes from (migration, never overwrite)
│   ├── KeystoreSecretStore.kt    # Keystore-wrapped secrets (the DB key, the backup key)
│   ├── BackupManager.kt          # Automatic encrypted backups into a user-chosen folder
│   ├── ThemePreferences.kt       # DataStore: appearance, reminder, lock, welcome, backups
│   ├── ReminderManager.kt        # Daily reminder + reschedule after reboot
│   └── ExportFiles.kt            # Short-lived export files in the cache
│
├── domain/                        # Business logic (plain Kotlin, unit-tested)
│   ├── model/                     # Insight, ExportFormat + backup format, settings enums
│   ├── format/                    # Markdown-lite parsing and toolbar/Enter editing actions
│   ├── backup/                    # Backup schedule and file encryption (BackupCrypto)
│   ├── calendar/                  # Month grid and each day's main feeling
│   ├── lock/                      # When the app lock asks again (LockPolicy)
│   ├── onboarding/                # When the welcome is shown
│   └── usecase/
│       ├── GenerateInsightsUseCase.kt  # On-device pattern detection
│       ├── InsightStrings.kt           # Insight sentences, supplied by the app
│       ├── ExportEntriesUseCase.kt     # JSON backup / CSV / Markdown
│       ├── ImportEntriesUseCase.kt     # Reads backups and old exports, plans the import
│       ├── EmotionSelection.kt         # Entry-form emotion rules
│       ├── EntrySearch.kt              # Accent- and case-insensitive timeline search
│       └── StopWords.kt                # Per-language keyword filtering
│
├── ui/                            # Presentation layer (Compose)
│   ├── theme/                     # Palettes (Meadow, Moonlight, Ocean, Ember), type, theme
│   ├── components/                # TagChip, JournalCard, EmptyState, TagEditDialog
│   ├── screens/                   # timeline (+ calendar), entry, patterns (insights, charts),
│   │                              # settings (+ backups), export, welcome, locked
│   ├── lock/                      # AppLock (BiometricPrompt) and the lock session
│   ├── util/                      # LocaleHelper, tag label resolver, resource-backed strings
│   ├── widget/                    # Home-screen mood widget with one-tap feelings
│   ├── QuickLog.kt                # Widget → new entry with a feeling already chosen
│   ├── JournalViewModel.kt        # State orchestration
│   ├── JournalAppComposable.kt    # Navigation + scaffold
│   └── Navigation.kt              # Route definitions
│
├── GentleJournalApp.kt           # Application class
└── MainActivity.kt               # Entry point: in-app language, app lock, widget intents

app/schemas/                       # Exported Room schemas (commit one per DB version)
app/src/test/                      # JVM unit tests
tools/check_translations.py        # Translation completeness check (also run in CI)
```

## TDD Feature Mapping

| TDD Requirement | Implementation |
|---|---|
| Short text entries, optional | `EntryFormScreen` — textarea with placeholder |
| Basic formatting | Bold, italic, bulleted and numbered lists via a toolbar, stored as Markdown (`domain/format/`) |
| 1–2 emotion tags per entry | `EmotionSelection.toggle()` enforces max 2; a sub-emotion replaces its parent |
| User-customizable tags | Add from the entry form; rename, re-emoji or delete in Settings |
| Action tags (including "nothing") | Default seeded actions include "Nothing 💤" |
| Chronological timeline with emoji | `TimelineScreen` grouped by date |
| Find past entries | Search box (text, feelings, activities; ignores case and accents) and a month calendar showing each day's main feeling |
| Quick logging | Widget buttons open a new entry with that feeling chosen; the entry form starts with feelings |
| First run | Three-page welcome that ends in the first entry and offers the daily reminder |
| Privacy on a shared phone | Optional lock with fingerprint or the phone's screen lock; asks again after a minute away, hides the app in Recents (Android 13+) |
| Not losing the journal | Automatic daily or weekly backups, encrypted with a backup password, into a folder you pick (e.g. one synced by Drive or Nextcloud); the newest 5 are kept |
| Weekly surfaced patterns | `GenerateInsightsUseCase` — summary, frequency, trends, sequences, correlations, time of day, weekdays, keywords |
| Edit / delete entries | Edit screen via `loadEntryForEditing()`; delete asks for confirmation |
| Local storage, no account | Room DB encrypted with SQLCipher, no network permission |
| Export CSV / JSON / Markdown | `ExportEntriesUseCase` + FileProvider share; JSON doubles as a restorable backup |
| Optional donations, no paywalls | Settings screen donation card |
| No streaks / guilt / failure states | No streak counters anywhere |
| Explainable insights | Each insight has a typed label |
| Emotionally safe UX | Gentle language throughout, in 12 languages |

## Backup format

The JSON export is a versioned backup that import reads back exactly:

```json
{
  "format": "gentle-journal",
  "version": 2,
  "exportedAt": "2026-10-11T10:30:00+03:00",
  "emotions": [{ "id": "e_happy", "label": "Happy", "emoji": "😊", "parentId": null, "isDefault": true, "sortOrder": 0 }],
  "actions":  [{ "id": "a_nature", "label": "Nature", "emoji": "🌿", "isDefault": true, "sortOrder": 4 }],
  "entries":  [{ "id": "…", "timestampMillis": 1760167800000, "timestamp": "2026-10-11T10:30:00+03:00",
                 "text": "…", "emotionIds": ["e_happy"], "actionIds": ["a_nature"] }]
}
```

Automatic backups (`.gjbackup`) hold the same JSON, encrypted: the bytes `GJBK`, a format version, a 16-byte salt, the PBKDF2-HMAC-SHA256 iteration count (600,000), a 12-byte IV, then AES-256-GCM ciphertext with the header as associated data. The backup password alone opens a file on any phone (Settings → Import Data); the derived key is kept on the device, wrapped by the Android Keystore, so scheduled backups need no password.

Import also accepts the older export (a plain array with `yyyy-MM-dd HH:mm` timestamps and tag labels). Tags are matched by id, then by stored or translated name; unknown ones are recreated as custom tags. Entries already in the journal (same id, or same minute and text) are skipped, so importing a file twice is safe.

## Permissions

- **POST_NOTIFICATIONS** — only for the optional daily reminder; asked for when the reminder is turned on.
- **RECEIVE_BOOT_COMPLETED** — to reschedule that reminder after a reboot.
- **USE_BIOMETRIC** (added by androidx.biometric) — only for the optional app lock.
- Backups use the system folder picker, so no storage permission is needed.
- No internet permission: nothing ever leaves the device unless you share an export.

## Tech Stack

- **Kotlin** 2.4 with the Compose compiler plugin, **AGP** 9.4, **Gradle** 9.8
- **Jetpack Compose** (Material 3, BOM 2026.09)
- **Room** 2.8 on **SQLCipher** 4.19 (encrypted, on-device only)
- **DataStore** for preferences
- **Navigation Compose** for routing
- **androidx.biometric** for the app lock, **DocumentFile** for backup folders
- **ViewModel** + StateFlow for reactive state
- **kotlinx.serialization** JSON (JsonElement API) for backups
- **KSP** for Room annotation processing

## Build & Run

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
python3 tools/check_translations.py
```

Minimum SDK: 26 (Android 8.0)
Target SDK: 36 (Android 16), compile SDK: 37
Java 17 to build

**Database changes:** never bump the Room version without adding a `Migration` and committing the new schema from `app/schemas/`. Only the pre-release versions 1 and 2 are still allowed to be wiped.

## Install on your phone

Every push is built and tested by GitHub Actions (`.github/workflows/build.yml`). Pushes to `main` publish a signed APK:

**https://github.com/countzero7a/gentle-journal-android/releases/latest/download/gentle-journal.apk**

Open that link on the phone, then open the downloaded file. Android asks once to allow installs from your browser or file manager. Later builds install as updates and keep your journal.

Release signing uses a key stored in the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Keep an offline backup of that key: an app signed with a different key can't update this one, and uninstalling deletes the journal.

## Design Principles

1. **Explainability first** — All insights trace to specific entries
2. **Emotionally safe UX** — No streaks, no guilt, no failure states
3. **Offline-first & private** — Zero network, zero accounts, zero tracking
4. **Inference, not diagnosis** — Observations only, never labels
5. **ML as refinement** — Rule-based v1, extensible for on-device ML later
