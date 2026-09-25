# OmniDial — System Architecture & Technical Documentation

> **Current Version**: v2.1.0 (Build 22) — September 2026

## 1. Executive Summary

**OmniDial** is a native Android Telecom dialer and call management application built with Jetpack Compose, Kotlin Coroutines/StateFlow, Room SQLite database (v17), and Android Telecom framework (`InCallService`).

The app unifies phone contacts, app-created local contacts, T9 smart dialing, automated call screening/rules, ambient geofencing guards, dual-SIM management, E.164 number normalization, tiered caller trust badges, carrier STIR/SHAKEN spam detection, and persistent drag-and-drop VIP favorite shortcuts into a clean Material 3 design.

---

## 2. Core Components & Architectural Overview

```
+---------------------------------------------------------------------------------+
|                                 MainActivity                                    |
|   (Hosts Bottom Navigation, Tab Bar, Floating In-Call Pill, and Root Insets)    |
+---------------------------------------------------------------------------------+
        |
        +---> MainViewModel (StateFlow, Domain UseCases, Preferences Sync)
                |
                +---> Domain Use Cases
                |       ├── EvaluateSimRuleUseCase  (DAG conflict-resolved SIM routing)
                |       ├── ResolveCallerIdentityUseCase  (trust badges, spam lookup)
                |       ├── SearchT9ContactsUseCase  (T9 nickname/name/number search)
                |       └── ManageFavoritesUseCase  (star, unstar, reorder, ignore)
                |
                +---> AppRepository & AppDatabase (Room DB v17)
                |       ├── FavoriteContact
                |       ├── RecentCall
                |       ├── CallerRule  (+ ambient geofence + automation fields)
                |       ├── ContactSimPreference  (per-number preferred SIM slot)
                |       ├── NumberChannelPreference  (per-normalized-number VoIP/channel binding)
                |       ├── ChannelConfig  (dynamic channel labels, enabled state, ordering)
                |       ├── ContactDefaultNumber  (primary number prioritization & sync)
                |       ├── SpamNumber
                |       ├── LocalContact
                |       ├── IgnoredContact
                |       └── AutomationLog
                |
                +---> Multi-Channel Calling Engine (MCCE)
                |       ├── ChannelDiscoveryManager (active SIM detection, installed VoIP packages, emergency cell tower checks)
                |       ├── ChannelDispatchCoordinator (domestic cellular emergency lock, Telecom/WhatsApp VoIP intent dispatch)
                |       └── ChannelPreferenceRepository (Room-backed per-number channel preferences)
                |
                +---> ContactHelper (System Contacts Provider & CallLog Merging)
                |
                +---> CallManager (InCallService, Automation Pipeline, Trust Badges,
                |                  callLoggedEvent SharedFlow, SIM roaming resolution;
                |                  defers spam-list auto-decline while screening role held)
                |
                +---> OmniCallScreeningService (CallScreeningService: silences+logs or
                |       rejects spam-list numbers per spam_auto_block; honors not-spam
                |       whitelist and saved contacts; requires BIND_SCREENING_SERVICE)
                |
                +---> RoleHelper (ROLE_DIALER / ROLE_CALL_REDIRECTION /
                |       ROLE_CALL_SCREENING request intents + held/available checks)
                |
                +---> PhoneNumberNormalizer (libphonenumber E.164 engine)
                |
                +---> SimHelper (Multi-SIM subscription resolution, roaming detection)
                |
                +---> ReminderScheduler (AlarmManager post-call callback scheduling)
                |
                +---> BackupManager (JSON backup, public MediaStore storage, restore)
```

### Key UI Screens & Components
- **`FavoritesScreen.kt`**: VIP grid hub with Configure Mode drag-and-drop reordering, persistent sort order, Option B single voice-first button (`Call` with 2-option sheet on unknown, `Call - <Channel>` on known preference), and full contact sheet opening on card tap.
- **`DialerScreen.kt`**: T9 smart keypad with Quick Recents bar, Pause/Wait overflow menu, nickname-aware suggestions, Unified Keypad Channel Dock (`[SIM 1]`, `[SIM 2]`, `[WhatsApp]`), domestic cellular emergency locking, and dedicated `[Message]` + `[Call]` action buttons.
- **`CallLogScreen.kt`**: Rich call history merging `CallLog.Calls` + Room `recent_calls`. Channel-faithful 1-tap redial (dials back on exact channel and SIM slot), missed call highlight pulse, and `🤖 Rule` badges.
- **`ContactsScreen.kt`**: Unified directory with Nicknames filter tab (`SmartContactSort.NICKNAMES`), default number prioritization, per-number SIM routing, and partitioned search outside active filters (`otherFilteredOutMatches`).
- **`InCallScreen.kt`**: Active call UI with SIM display name chip, amber `ROAMING` alert badge, DTMF keypad, audio output selector, post-call notes, manual `[ 🔕 Silence ]` ringer chip, touch-to-silence gestures, and isolated `CallDurationStatusChip` to prevent 1Hz recomposition cascades.
- **`RulesScreen.kt`**: Automation rule manager with visual pipeline chips, Quick-Start Recipe Gallery bottom sheet, rule dry-run simulator, execution history log, and rule duplication.
- **`MainActivity.kt`**: Hosts `FloatingCallPill`, observes `callLoggedEvent` for reactive Recents updates, handles `dismissAllModals()` when external calls arrive, and processes missed call deep-link intents.

---

## 3. Data Persistence & Migration

- **Database Version**: v17 in `AppDatabase.kt`.
- **Entities (v17)**: `CallerRule`, `AutomationLog`, `RecentCall`, `FavoriteContact`, `SpamNumber`, `IgnoredContact`, `LocalContact`, `ContactSimPreference`, `NumberChannelPreference`, `ChannelConfig`, `ContactDefaultNumber`.
- **Indices**: Indexed on primary query paths (`phoneNumber`, `normalized_number`, `timestamp`, `channel_id`, `contact_id`) for O(1) / indexed sub-millisecond retrieval.
- **Migration Strategy**: Sequential migrations `MIGRATION_11_12` through `MIGRATION_16_17` applied; fallback to destructive migration retained for non-production environments.
- **`NumberChannelPreference` entity**: Persists `preferredChannelId` (`sim_1`, `sim_2`, `whatsapp`, `ask`, etc.) mapped to normalized phone number with custom labels.
- **`ContactDefaultNumber` entity**: Persists default phone number mappings synchronized with device ContactsContract.
- **`SpamNumber` entity**: Stores both raw and `normalizedNumber` (E.164) for sub-millisecond indexed lookup via `getSpamByNormalizedNumber()`.
- **`CallerRule` entity**: Stores full automation pipeline including `autoAnswer`, `answerDelaySec`, `dtmfSequence`, `dtmfDelayMs`, `autoHangup`, `hangupDelaySec`, `autoSpeakerphone`, `autoMuteMic`, `requiredWifiSsid`, `requiredBluetoothDevice`.
- **Favorites Sort Order**: `SharedPreferences` key `favorite_sort_orders` storing `phoneNumber:sortOrder:name` mappings.
- **Unified SharedPreferences key**: All modules (`MainViewModel`, `CallManager`, `CallNotificationReceiver`, `SpamManagementDialog`, `BackupManager`) share `"kishan_dialer_prefs"`.

---

## 4. Call Management & Automation Priority

```
Incoming Call Arrives
        │
        ▼
OmniCallScreeningService (only when CALL_SCREENING role held)
  ├── Spam-list number ──> silence + log as missed call, or reject if spam_auto_block
  ├── Whitelisted / saved contact ──> always allowed through
  └── Anything else ──> allowed through untouched
        │
        ▼
SimHelper.resolveSimInfo(context, accountHandle)
  └── Populates: simDisplayName, isRoaming  ──> ActiveCallInfo enrichment
        │
        ▼
Evaluate Automation Rules (CallManager.kt)
  ├── Match rule pattern  (matchesRulePattern — exact > prefix > wildcard, DAG weighted)
  ├── Check ambient geofence: Wi-Fi SSID + Bluetooth device name
  └── Execute Rule Actions:
        ├── Auto-Answer (delay)
        ├── Auto-Speakerphone
        ├── Auto-Mute Mic
        ├── DTMF Sequence (dtmfDelayMs)
        └── Auto-Hangup (delay)
        │
        ├── Rule Matched ──> executeAutomationWorkflow() & Finish
        │
        └── No Rule Matched
                │
                ▼
      ResolveCallerIdentityUseCase (Trust Badge Assignment)
        ├── isSpam  ──>  TrustTier.HIGH_RISK_SPAM (Red)
        ├── communityInfo logistics  ──>  TrustTier.PRIORITY_LOGISTICS (Amber)
        ├── communityInfo business / hasContact  ──>  TrustTier.VERIFIED_BUSINESS (Green)
        └── Unknown  ──>  TrustTier.UNKNOWN
                │
                ▼
      Evaluate Spam Blocklist & STIR/SHAKEN
        ├── E.164 normalized lookup: getSpamByNormalizedNumber()
        └── Check Connection.VERIFICATION_STATUS_FAILED
                │
                ├── Is Spam ──> Reject / Silence + SpamNotificationHelper
                └── Is Safe ──> Ring & Display InCallScreen
                                (with SIM name chip + roaming amber badge if applicable)
```

---

## 5. Dual-SIM Management Architecture

- **Global SIM Mode** (`global_sim_pref_mode` in SharedPreferences):
  - `"system"`: Highlights active system default SIM; no per-contact prompts.
  - `"ask_learn"`: Allows per-contact SIM preference customization via `ContactNumberPreference`.
  - `"international"`: Shows SIM selection only for numbers where `PhoneNumberNormalizer.isInternational(context, number)` returns true.
- **`EvaluateSimRuleUseCase`**: DAG-based weighted resolver — exact match rules (`weight > 1000`) take precedence over prefix/wildcard rules (`weight ~501`). Roaming SIM auto-avoidance: if `isRoaming == true` on target SIM and a non-roaming alternative exists, sets `isRoamingAvoided = true` and routes to the local SIM.
- **`SimHelper.resolveSimInfo()`**: Queries `SubscriptionManager` for `SubscriptionInfo`, resolves `simDisplayName` (preferring user-assigned label over carrier name), and queries `TelephonyManager.isNetworkRoaming(subscriptionId)` per slot.
- **`OmniCallRedirectionService`**: Queries `ContactNumberPreference` DAO for `preferredSimSlot` and calls `placeCallWithConference()` binding the corresponding `PhoneAccountHandle` before cellular dial.

---

## 6. Reactive Call Log Update Architecture

- **`callLoggedEvent: SharedFlow<Long>`** in `CallManager`: Emitted with the Room `RecentCall.id` upon DB insertion after call termination.
- **`MainViewModel`** subscribes via `viewModelScope.launch { callManager.callLoggedEvent.collect { ... } }` and triggers `_combinedRecentCalls` StateFlow refresh within <10ms of call end.
- Eliminates polling or system `ContentObserver` delays — Recents list updates are instant and reactive.

---

## 7. Post-Call Quick Action Architecture

After call termination, `CallManager` emits a post-call state to `MainViewModel`. A 4-second bottom sheet (`PostCallQuickActionCard`) surfaces four 1-tap actions:
- **Save Contact**: Opens `CreateContactDialog` pre-filled with caller number.
- **Block & Report Spam**: Inserts a `SpamNumber` entry and updates block UI.
- **WhatsApp Message**: Launches WhatsApp chat intent with normalized number.
- **Set Reminder**: Opens `ReminderScheduler` with pre-filled caller name and number, scheduling an `AlarmManager` exact-time broadcast to `ReminderReceiver`.

---

## 8. Backup Architecture

`BackupManager` uses a dual-storage strategy:
1. **Internal storage** (`context.filesDir/backups/`): Fast local access, survives app updates.
2. **Public MediaStore** (`Documents/OmniDial/` via `MediaStore.Files`): Survives app uninstalls; scanned and mirrored into internal storage on every app startup.

Backup JSON payload includes: `CallerRule` list, `FavoriteContact` list, `SpeedDial` map, all SharedPreferences keys (SIM mode, spam presets, screening auto-block, WhatsApp mode, learned choices).

SHA-256 checksum and schema version are embedded in the JSON header for tamper detection on restore.

---

## 9. Unit Test Coverage (Build 20)

All tests run via `./gradlew testDebugUnitTest` using Robolectric (`@Config(sdk = [36])`):

| Test File | Coverage |
|---|---|
| `ConferenceUiGatingTest` | Gating logic for Swap, Merge, Add Call, and participant controls across call states and capabilities |
| `Phase13MultiChannelCoreTest` | Dynamic channel discovery, SIM slot labeling, WhatsApp channel dispatch, `ChannelPreferenceRepository`, `ChannelDispatchCoordinator` |
| `Task10Test` | Multi-channel dock interactions, modal dismissal, transactional backup & restore verification, deduplication |
| `Phase12PartitionedSearchAndRingSilencingTest` | Partitioned contact filtering outside active filters, ringer silence state flow transitions |
| `Phase11AutomationTest` | CallerRule automation field persistence, Gate Buzzer recipe template, ambient geofence backup/restore |
| `Phase10TelecomTest` | DAG rule conflict resolution, roaming-aware SIM selection, trust badge scoring, VoIP continuity lifecycle |
| `Phase9SpamDefenseTest` | E.164 normalization, spam DB indexed lookups, spam preset SharedPrefs |
| `ExampleUnitTest` | Country ISO lookup, descriptive number labels, voicemail/short-code isolation, `SmartContactSort.NICKNAMES` filtering |
| `MissedCallHighlightTest` | Explicit missed call notification deep link, system intent auto-targets latest missed call |
| `OmniCallRedirectionServiceTest` | WhatsApp preference matching, never-mode bypass, per-contact SIM resolution, voicemail URI bypass |
| `SimHelperTest` | `SimInfo` data integrity, `resolveSimSlot` default, `getPhoneAccountForSimSlot` safe on empty Telecom |
| `ReminderSchedulerTest` | Schedule creates alarm, past epoch not scheduled, cancel removes alarm |
| `ExampleRobolectricTest` | Robolectric baseline, Room in-memory DB, basic DAO operations |

---

## 10. Recent Fixes & Quality Upgrades (v1.5.0–v2.0.2)

1. **Multi-Channel Calling Engine (MCCE)**: Introduced `ChannelDiscoveryManager`, `CallingChannel`, `ChannelConfigRepository`, `ChannelPreferenceRepository`, and `ChannelDispatchCoordinator`.
2. **Dynamic Keypad Channel Dock**: Added `KeypadChannelDock` above dial pad for 1-tap channel switching between SIM 1, SIM 2, and WhatsApp with live roaming and carrier labels.
3. **Unified Call Choice Dialogs**: Standardized `MultiChannelChoiceDialog` and `CallConfirmationDialog` with "Remember choice" preference persistence.
4. **Hot-Path Search Optimization**: Reordered `ContactHelper.matchesNumberQuery` to check raw digit substrings before libphonenumber parsing, hoisted regexes and country codes, reducing search latency to <16ms.
5. **Telecom Callback Leak Prevention**: Explicitly unregister per-call `Call.Callback` in `CallManager.kt` upon call teardown, eliminating context leaks and duplicate call ended events.
6. **Transactional Backup & Live Restore**: Wrapped Room restore operations in `@Transaction` with incremental progress reporting to prevent half-restored states.
7. **Contact Default Number Sync**: Integrated `ContactDefaultNumber` Room DAO and device ContactsContract sync for default number prioritization.
8. **Partitioned Contact Search Outside Active Filters**: Two-pass filtering displays qualifying filter matches at the top and cleanly partitions non-qualifying matches into "Other Matches Outside Filter" with one-tap action sheets.
9. **Ambient Incoming Ring Silencing**: Integrated `TelecomManager.silenceRinger()` triggered on device pickup/motion, proximity uncover, screen touch, audio route switching, quick decline SMS, or physical volume buttons while keeping the call active in `STATE_RINGING`.
10. **Release Signing Security**: Added upload keystore integrity validation in `app/build.gradle.kts` to warn on missing upload keys and prevent unintended debug key signing.
11. **Caller ID & Spam Role (v2.0.1)**: Added `OmniCallScreeningService` (`BIND_SCREENING_SERVICE`), `RoleHelper`, and `CallScreeningCard`; `CallManager` defers spam-list auto-decline while the role is held.
12. **Honest Spam Badges (v2.0.1)**: `CallDropAttribution` replaces the misleading "Carrier Auto-Dropped" badge with truthful "Blocked by OmniDial" labels.
13. **Setup Wizard & Redirection Recovery (v2.0.1)**: `SetupWizard` first-run flow plus `CallRedirectionBanner`/`RoleReminderNotification` for Call Redirection role recovery.
14. **Multi-Call & Conference Calling Subsystem (v2.0.2)**: Added full multi-call management in `CallManager.kt` and `InCallScreen.kt` with Swap, Merge, participant list, individual hangup, and conference hold/resume controls, backed by `ConferenceUiGating.kt`.
15. **Floating Call Pill & PiP Auto-Dismiss (v2.0.2)**: Fixed lingering call pill and Picture-in-Picture window by tying teardown directly to `DISCONNECTED`/`DISCONNECTING` call state transitions.
16. **Just-In-Time Contextual Reminders (v2.0.2)**: Added warning banners and empty-state action cards in Contacts, Recents, and Rules for missing permissions or roles with 1-tap grant actions.
17. **Settings Permissions Hub (v2.0.2)**: Added dedicated `PermissionsHubCard` in Settings displaying real-time status of all 6 wizard setup steps, individual launchers, and full wizard rerun.

---

## 11. Build, Signing & Optimization Runbook

### 11.1 Keystore Auto-Restoration
`app/build.gradle.kts` auto-decodes `debug.keystore.base64` → `debug.keystore` on any build if missing.

### 11.2 APK Optimization (R8 & ProGuard)
- Minification + resource shrinking enabled in release builds.
- Reduces release APK from ~19 MB to ~5.2 MB.
- Compose, Room, Telecom, and libphonenumber reflection rules preserved in `proguard-rules.pro`.

### 11.3 Building Release APK
```bash
./gradlew :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk apks/OmniDial-v<version>.apk
cp apks/OmniDial-v<version>.apk apks/OmniDial.apk
```

### 11.4 Running Unit Tests
```bash
./gradlew testDebugUnitTest --continue
./gradlew verifyRoborazziDebug   # Screenshot regression
./gradlew recordRoborazziDebug   # Record new reference screenshots
```

---

## 12. Maintenance & Extension Guide

### Adding a New Caller Rule Action
1. Add field to `CallerRule` entity in `data/Entities.kt` and bump Room DB version.
2. Add execution logic in `CallManager.executeAutomationWorkflow()`.
3. Add UI toggle in `RuleEditDialog.kt`.
4. Add `CallerRule` field to `BackupManager` JSON serialization.
5. Add unit test in `Phase11AutomationTest.kt`.

### Adding a New Panel / Tab
1. Add route in `MainActivity.kt` navigation state.
2. Implement Composable screen in `ui/screens/`.
3. Add navigation icon to `NavigationBar`.
4. Add Roborazzi screenshot test in `RealScreenshotTest.kt`.

### Troubleshooting Checklist
- **Calls don't open in-call screen**: Set OmniDial as default phone app in Android Settings → Apps → Default apps → Phone app.
- **WhatsApp icon does nothing**: WhatsApp must be installed; number must have a valid E.164 country code.
- **Missing contacts in Recents**: Ensure `READ_CALL_LOG` and `READ_CONTACTS` permissions are granted.
- **Roaming alert not showing**: Verify `READ_PHONE_STATE` permission is granted and device has an active SIM with telephony subscription.
- **Backup not surviving reinstall**: Verify `WRITE_EXTERNAL_STORAGE` / MediaStore permissions granted on Android < 10.
