# 🌿 Gentle Journal

A private, offline-first journaling Android app that helps you notice patterns between emotions, thoughts, actions, and time — without diagnosis, judgment, or surveillance.

## Architecture

```
com.gentlejournal/
├── data/                          # Data layer (Room + Repository)
│   ├── entity/                    # Room entities & relations
│   │   ├── EntryEntity.kt        # Journal entry
│   │   ├── EmotionEntity.kt      # Emotion tag (user-customizable)
│   │   ├── ActionEntity.kt       # Action tag (user-customizable)
│   │   ├── EntryEmotionCrossRef.kt  # Many-to-many join
│   │   ├── EntryActionCrossRef.kt   # Many-to-many join
│   │   └── EntryWithTags.kt      # Embedded relation
│   ├── dao/
│   │   ├── EntryDao.kt           # Entry CRUD + relations
│   │   └── TagDao.kt             # Emotion/Action tag CRUD
│   ├── repository/
│   │   └── JournalRepository.kt  # Single source of truth
│   └── JournalDatabase.kt        # Room DB with default tag seeding
│
├── domain/                        # Business logic
│   ├── model/
│   │   ├── Insight.kt            # Insight data class + types
│   │   └── ExportFormat.kt       # JSON / CSV / Markdown enum
│   └── usecase/
│       ├── GenerateInsightsUseCase.kt  # On-device pattern detection
│       └── ExportEntriesUseCase.kt     # Data export formatter
│
├── ui/                            # Presentation layer (Compose)
│   ├── theme/
│   │   ├── Color.kt              # Warm organic palette
│   │   ├── Type.kt               # Serif typography
│   │   └── Theme.kt              # Material3 theme
│   ├── components/
│   │   └── Components.kt         # TagChip, JournalCard, EmptyState, etc.
│   ├── screens/
│   │   ├── timeline/             # Chronological entry list
│   │   ├── entry/                # New & Edit entry form
│   │   ├── insights/             # Weekly pattern display
│   │   ├── settings/             # About, tags, data, donation
│   │   └── export_screen/        # Format selector + export
│   ├── JournalViewModel.kt       # State orchestration
│   ├── JournalAppComposable.kt   # Navigation + scaffold
│   └── Navigation.kt             # Route definitions
│
├── GentleJournalApp.kt           # Application class
└── MainActivity.kt               # Entry point
```

## TDD Feature Mapping

| TDD Requirement | Implementation |
|---|---|
| Short text entries, optional | `EntryFormScreen` — textarea with placeholder |
| 1–2 emotion tags per entry | `toggleFormEmotion()` enforces max 2 |
| User-customizable tags | `addCustomEmotion()` / `addCustomAction()` |
| Action tags (including "nothing") | Default seeded actions include "Nothing 💤" |
| Chronological timeline with emoji | `TimelineScreen` grouped by date |
| Weekly surfaced patterns | `GenerateInsightsUseCase` — 4 pattern types |
| Edit / delete entries | Edit screen via `loadEntryForEditing()` |
| Local storage, no account | Room DB, no network permissions |
| Export CSV / JSON / Markdown | `ExportEntriesUseCase` + FileProvider share |
| Optional donations, no paywalls | Settings screen donation card |
| No streaks / guilt / failure states | No streak counters anywhere |
| Explainable insights | Each insight has a typed label |
| Emotionally safe UX | Gentle language throughout |

## Tech Stack

- **Kotlin** 1.9.22
- **Jetpack Compose** (Material 3 + BOM 2024.01)
- **Room** 2.6.1 (SQLite, on-device only)
- **Navigation Compose** for routing
- **ViewModel** + StateFlow for reactive state
- **KSP** for Room annotation processing
- **No network permissions** — fully offline

## Build & Run

```bash
# Open in Android Studio Hedgehog+ (2023.1+)
# Sync Gradle, then:
./gradlew assembleDebug

# Or run directly on device/emulator from Android Studio
```

Minimum SDK: 26 (Android 8.0)
Target SDK: 34 (Android 14)

## Design Principles

1. **Explainability first** — All insights trace to specific entries
2. **Emotionally safe UX** — No streaks, no guilt, no failure states
3. **Offline-first & private** — Zero network, zero accounts, zero tracking
4. **Inference, not diagnosis** — Observations only, never labels
5. **ML as refinement** — Rule-based v1, extensible for on-device ML later
