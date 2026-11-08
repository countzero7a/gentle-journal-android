# Retention and Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Gentle Journal something people keep using and can find: an optional app lock, a welcoming first run with quicker logging, automatic backups, a calendar and search, simpler navigation, and a proper name and icon.

**Architecture:** Each feature keeps its decision logic in plain Kotlin under `domain/` (unit-tested on the JVM in CI) and its Android/Compose glue under `data/` and `ui/`. Preferences live in `ThemePreferences` (DataStore). No feature adds network access.

**Tech Stack:** Kotlin 2.4, Jetpack Compose (BOM 2026.09, Material3 1.4), Room 2.8 + SQLCipher, DataStore, AGP 9.4. New: `androidx.biometric:biometric:1.1.0`, `androidx.fragment:fragment-ktx:1.9.1`, `androidx.documentfile:documentfile:1.1.0`.

## Global Constraints

- No INTERNET permission; nothing leaves the device except files the user explicitly shares or a folder the user picks.
- Every user-visible string goes into `values/strings.xml` and all 12 `values-xx` files; `tools/check_translations.py` (run in CI) must pass. French uses "vous", Portuguese is Brazilian, all others informal.
- Tone: gentle, no pressure, no streaks, no guilt.
- Verification is the GitHub Actions build (`testDebugUnitTest assembleRelease`); there is no local Android SDK.
- One branch per task; merge to `main` (publishes the APK) only after CI passes. Commit messages end with the Co-Authored-By line.
- Database schema stays at version 3 (no Room migrations in this plan).

---

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `domain/lock/LockPolicy.kt` | When the app must ask to unlock | 1 |
| `ui/lock/AppLock.kt` | BiometricPrompt wrapper (screen lock / fingerprint) | 1 |
| `ui/screens/locked/LockScreen.kt` | "Journal locked" screen | 1 |
| `domain/onboarding/Welcome.kt` | Whether to show the welcome | 2 |
| `ui/screens/welcome/WelcomeScreen.kt` | 3-page welcome | 2 |
| `ui/QuickLog.kt` | Intent extra for "new entry with this feeling" | 2 |
| `res/layout/widget_mood.xml`, `ui/widget/MoodWidgetProvider.kt` | Widget with quick-log emojis | 2 |
| `domain/backup/BackupSchedule.kt` | When a backup is due, which old ones to delete | 3 |
| `domain/backup/BackupCrypto.kt` | Optional password encryption of backup files | 3 |
| `data/BackupManager.kt` | Writes backups into the chosen folder (SAF) | 3 |
| `domain/calendar/CalendarMonth.kt` | Month grid and each day's main feeling | 4 |
| `domain/usecase/EntrySearch.kt` | Matching entries to a search query | 4 |
| `ui/screens/timeline/CalendarView.kt` | Month calendar under the timeline | 4 |
| `ui/screens/patterns/PatternsScreen.kt` | Insights and Charts as one tab | 5 |
| `ui/screens/settings/SettingsScreen.kt` | Grouped sections, export moved here | 5 |
| `res/mipmap-anydpi-v26/*`, `res/drawable/ic_launcher_*` | Adaptive and themed icon | 6 |

---

### Task 1: Optional app lock

**Behaviour:** Off by default. Settings → Privacy → "Lock with fingerprint or screen lock". Turning it on or off asks to unlock first. When on, the app asks on start and when coming back after more than 60 seconds away (the grace period covers the share sheet and file picker). Uses the phone's own lock: fingerprint/face if set up, otherwise the phone PIN/pattern/password; no separate app PIN to forget. If the phone has no screen lock, the switch explains that one is needed. While locked, the recents thumbnail is hidden (`setRecentsScreenshotEnabled(false)` on API 33+) and the widget shows "Open to see today" instead of the mood.

**Files:**
- Create: `domain/lock/LockPolicy.kt`, `ui/lock/AppLock.kt`, `ui/screens/locked/LockScreen.kt`
- Modify: `MainActivity.kt` (becomes `FragmentActivity`, gates content), `data/ThemePreferences.kt` (`appLockEnabled`), `ui/JournalViewModel.kt`, `ui/JournalAppComposable.kt`, `ui/screens/settings/SettingsScreen.kt`, `ui/widget/MoodWidgetProvider.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, all `strings.xml`
- Test: `app/src/test/java/com/gentlejournal/domain/lock/LockPolicyTest.kt`

**Interfaces:**
- Produces: `object LockPolicy { const val GRACE_MILLIS = 60_000L; fun mustUnlock(enabled: Boolean, unlockedThisSession: Boolean, backgroundedAt: Long?, now: Long): Boolean }`
- Produces: `class AppLock(activity: FragmentActivity) { fun canLock(): Boolean; fun authenticate(title: String, onSuccess: () -> Unit, onFailure: () -> Unit) }` using `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` (supported from API 28 by androidx.biometric; it falls back to the keyguard on 26–27).
- Produces: `ThemePreferences.appLockEnabled: Flow<Boolean>`, `suspend fun setAppLockEnabled(enabled: Boolean)`.

- [ ] Write `LockPolicyTest`: disabled never locks; enabled and not yet unlocked locks; unlocked and never backgrounded doesn't lock; backgrounded 59 s ago doesn't lock; 61 s ago locks.
- [ ] Implement `LockPolicy` (pure).
- [ ] Add biometric + fragment dependencies; `MainActivity` extends `FragmentActivity`; record `backgroundedAt` in `onStop`, re-check in `onStart`, show `LockScreen` (auto-starts the prompt, "Unlock" button to retry).
- [ ] Settings Privacy section with the switch (authenticate before changing; disabled with explanation when `canLock()` is false).
- [ ] Widget hides the mood when the lock is on.
- [ ] Strings (+11 languages), CI green, merge, publish.

### Task 2: Welcome, feeling-first entry, widget quick-log

**Behaviour:**
- First start with an empty journal shows 3 short pages: what the app is for; private by design (encrypted, no account, no internet); no pressure (optional daily reminder switch). "Write your first entry" opens the new-entry screen. Existing users never see it (`welcomeDone` is set immediately when entries exist).
- Entry form order becomes: feelings → what you did → text (optional), so a quick log is two taps.
- Widget: today's main feeling on top (or "How are you feeling?"), and 5 emoji buttons (happy, calm, tired, anxious, sad). Tapping one opens a new entry with that feeling already selected (still behind the app lock).

**Files:**
- Create: `domain/onboarding/Welcome.kt`, `ui/screens/welcome/WelcomeScreen.kt`, `ui/QuickLog.kt`
- Modify: `ui/screens/entry/EntryFormScreen.kt` (section order), `MainActivity.kt` (`onNewIntent` → quick-log), `ui/JournalViewModel.kt` (`startNewEntry(preselectedEmotionId: String?)`, `pendingQuickLog`), `ui/JournalAppComposable.kt` (welcome route, navigate on quick-log), `data/ThemePreferences.kt` (`welcomeDone`), `res/layout/widget_mood.xml`, `res/xml/widget_mood_info.xml` (min width 250dp), `ui/widget/MoodWidgetProvider.kt`, all `strings.xml`
- Test: `domain/onboarding/WelcomeTest.kt`

**Interfaces:**
- Produces: `object Welcome { fun shouldShow(welcomeDone: Boolean, entryCount: Int): Boolean }`
- Produces: `object QuickLog { const val EXTRA_EMOTION_ID = "com.gentlejournal.extra.EMOTION_ID"; val WIDGET_EMOTIONS: List<String> = listOf("e_happy", "e_calm", "e_tired", "e_anxious", "e_sad") }`

- [ ] `WelcomeTest`: shows only when not done and no entries.
- [ ] Welcome screen (HorizontalPager, Skip/Next/Start), preference, routing.
- [ ] Reorder entry form sections.
- [ ] Quick-log intent handling (cold start and `onNewIntent`), widget layout and PendingIntents (distinct request codes).
- [ ] Strings, CI, merge, publish.

### Task 3: Automatic backups

**Behaviour:** Settings → Privacy & data → "Automatic backup": pick a folder (Android's folder picker; can be a Google Drive or other cloud folder, so the app itself stays offline), frequency daily or weekly. A backup is written when the app opens or an entry is saved and the last one is older than the frequency. Files are named `gentle-journal-backup-YYYY-MM-DD.json` (or `.gjbackup` when encrypted); the 5 newest are kept, older ones the app made are deleted. Settings shows the last backup time and a "Back up now" button. Import accepts these files. If automatic backup is off, the journal has 10+ entries and nothing was exported for 30 days, the timeline shows a dismissable card suggesting a backup (dismiss = ask again in 30 days).

**Decision (2026-11-08):** backups are protected with a backup password chosen once (PBKDF2 + AES-GCM). The password is needed to restore, e.g. on a new phone, and can't be recovered; the derived key is kept Keystore-wrapped so automatic backups run unattended.

**Files:**
- Create: `domain/backup/BackupSchedule.kt`, `domain/backup/BackupCrypto.kt`, `data/BackupManager.kt`
- Modify: `data/ThemePreferences.kt` (folder URI, frequency, lastBackupAt, lastExportAt, reminderDismissedAt), `ui/JournalViewModel.kt`, `ui/screens/settings/SettingsScreen.kt`, `ui/screens/timeline/TimelineScreen.kt` (reminder card), `domain/usecase/ImportEntriesUseCase.kt` (encrypted files), `gradle/libs.versions.toml`, `app/build.gradle.kts`, all `strings.xml`
- Test: `domain/backup/BackupScheduleTest.kt`, `domain/backup/BackupCryptoTest.kt`

**Interfaces:**
- Produces: `enum class BackupFrequency(val days: Int) { DAILY(1), WEEKLY(7) }`
- Produces: `object BackupSchedule { fun isDue(lastBackupAt: Long?, frequency: BackupFrequency, now: Long): Boolean; fun filesToDelete(names: List<String>, keep: Int = 5): List<String>; fun fileName(date: LocalDate, encrypted: Boolean): String; fun shouldSuggestBackup(autoBackupOn: Boolean, entryCount: Int, lastExportAt: Long?, dismissedAt: Long?, now: Long): Boolean }`
- Produces: `object BackupCrypto { fun encrypt(plain: ByteArray, password: CharArray): ByteArray; fun decrypt(data: ByteArray, password: CharArray): ByteArray; fun isEncrypted(data: ByteArray): Boolean }` (PBKDF2-HMAC-SHA256, AES-256-GCM, magic header).

- [ ] `BackupScheduleTest`: due/not due at the boundary; only the app's own files are deleted, newest 5 kept; suggestion rules.
- [ ] `BackupCryptoTest`: round trip, wrong password fails, header detection, tampering detected.
- [ ] Implement schedule (+ crypto), `BackupManager` (DocumentFile, persisted URI permission, handles a revoked folder by turning backup off and telling the user).
- [ ] Settings UI, triggers (app start, after save), timeline reminder card, import of backup files.
- [ ] Strings, CI, merge, publish.

### Task 4: Calendar view and search

**Behaviour:** The timeline's top bar gets a search icon and a list/calendar switch. Calendar: month grid starting on the locale's first weekday, each day shows its most frequent feeling's emoji, today highlighted; arrows change month; tapping a day lists that day's entries below. Search: filters entries whose text (formatting markers ignored) or feeling/activity names contain the query, case-insensitively, in the app language.

**Files:**
- Create: `domain/calendar/CalendarMonth.kt`, `domain/usecase/EntrySearch.kt`, `ui/screens/timeline/CalendarView.kt`
- Modify: `ui/screens/timeline/TimelineScreen.kt`, `ui/JournalAppComposable.kt` (top bar), all `strings.xml`
- Test: `domain/calendar/CalendarMonthTest.kt`, `domain/usecase/EntrySearchTest.kt`

**Interfaces:**
- Produces: `object CalendarMonth { fun cells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?>; fun mainFeelingByDay(entries: List<EntryWithTags>, zone: ZoneId): Map<LocalDate, EmotionEntity> }`
- Produces: `object EntrySearch { fun matches(entry: EntryWithTags, query: String, emotionLabel: (EmotionEntity) -> String, actionLabel: (ActionEntity) -> String): Boolean }`

- [ ] `CalendarMonthTest`: Monday- and Sunday-first grids, month lengths incl. February in a leap year, leading blanks; main feeling ties broken by the earliest entry.
- [ ] `EntrySearchTest`: text match ignoring `**`/`*`/list markers, label match via resolver, case-insensitive, blank query matches all.
- [ ] Implement, wire UI.
- [ ] Strings, CI, merge, publish.

### Task 5: Simpler navigation

**Behaviour:** Bottom bar: Journal, Patterns, +, Settings. Patterns has two tabs, Insights and Charts (existing screens). Export moves from the timeline's top bar to Settings → Privacy & data (next to Import and backups). Settings is grouped under headers: Appearance (theme, language, typography), Reminders & widget, Privacy & data (lock, backup, export, import, delete), Tags, About & support. Bottom-bar labels grow from 10sp to 12sp.

**Files:**
- Create: `ui/screens/patterns/PatternsScreen.kt`
- Modify: `ui/Navigation.kt` (`Patterns` replaces `Insights`/`Charts` in the bar), `ui/JournalAppComposable.kt`, `ui/screens/settings/SettingsScreen.kt`, all `strings.xml`

- [ ] Patterns screen with tabs; navigation and back stack.
- [ ] Settings regrouping; export entry point.
- [ ] Strings, CI, merge, publish.

### Task 6: Name, adaptive icon, README

**Decision (2026-11-08):** the app is called "Gentle Journal" everywhere, untranslated.

**Behaviour:** Adaptive icon (sage background, cream motif as a vector foreground inside the 66dp safe zone) plus a monochrome layer for Android 13 themed icons, so launchers stop shrinking it into a white circle. App name per the decision. README tech stack and target SDK corrected (they still list the pre-upgrade versions).

**Files:**
- Create: `res/mipmap-anydpi-v26/ic_launcher.xml`, `res/mipmap-anydpi-v26/ic_launcher_round.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/drawable/ic_launcher_monochrome.xml`, `res/values/ic_launcher_background.xml`
- Modify: `res/values*/strings.xml` (`app_name`, widget hint), `README.md`

- [ ] Icon resources; verify in CI (aapt) and on the phone.
- [ ] Name and README.
- [ ] CI, merge, publish.

---

## Self-review

- Spec coverage: lock (1), welcome + feeling-first + widget quick-log (2), backups + reminder (3), calendar + search (4), navigation + settings grouping + export move (5), name + adaptive icon + README (6). All six recommendations have a task.
- Both user decisions are recorded in their tasks (backup password, app name "Gentle Journal").
- Names used across tasks: `ThemePreferences` gains `appLockEnabled`, `welcomeDone`, backup preferences; `JournalViewModel.startNewEntry(preselectedEmotionId)` is used by Task 2 only.
