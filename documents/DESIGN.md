# OmniDial — Architecture & System Design Document

> **Current Version**: v1.5.0 (Build 18) — September 2026

This document serves as the primary technical specification and maintenance guide for **OmniDial**. It documents the system architecture, component contracts, data persistence models, telephony integrations, build pipelines, and maintenance runbooks.

---

## 1. System Overview & Core Capabilities

OmniDial is a native Android Default Phone Dialer application built with modern **Kotlin** and **Jetpack Compose (Material 3)**. It functions as the system's default dialer and a standalone smart communication hub.

### Primary Capabilities
1. **Dual-Channel Calling**: One-tap cellular (`TelecomManager` / `Intent.ACTION_CALL`) or WhatsApp VoIP/Chat calls.
2. **Instant T9 Keypad Search**: Real-time digit-to-name/nickname/number matching with zero layout bounce.
3. **5-Panel Navigation**: Favorites, Recents, Keypad, Contacts, Settings/Rules with pre-warmed swipe transitions.
4. **Custom Favorites & Speed Dial**: Reorderable card grid (Bento/Grid/Material) with 1-9 speed dial shortcuts, nicknames, and per-number SIM designation.
5. **Contextual Automation & Geofenced Recipes**: Visual rule pipeline with ambient Wi-Fi/Bluetooth guards, dry-run simulator, and recipe gallery.
6. **Intelligent Dual-SIM Management**: Three global SIM modes, per-number SIM routing, real-time roaming detection, and DAG conflict resolution.
7. **Tiered Caller Trust Badges**: Color-coded trust tiers for spam, logistics, verified businesses, and saved contacts.
8. **E.164 Number Intelligence**: Google `libphonenumber` normalization across all lookup, matching, and dispatch paths.
9. **Post-Call Quick Actions & Reminders**: 4-second bottom sheet with Save Contact, Block Spam, WhatsApp Message, and AlarmManager-backed Set Reminder.
10. **In-Call Telecom Experience**: Custom full-screen in-call interface with SIM name chip, roaming amber badge, mute, hold, DTMF, speaker/Bluetooth routing, ambient lift & touch ring silencing, and flip-to-silence.
11. **Partitioned Filter Contact Discovery**: Real-time two-pass search displaying qualifying filter matches at the top and partitioning non-qualifying matches into a dedicated "Other Matches Outside Filter" section with direct action sheets.
12. **Multi-Channel UX & Communication Hub Design**: Cognitive-overload-free paradigm for Cellular (SIM 1/2), WhatsApp, messaging, and video routing (see [MULTI_CHANNEL_UX_DESIGN_THINKING.md](file:///d:/Try/Git-Repos/Kishan-Dialer/documents/MULTI_CHANNEL_UX_DESIGN_THINKING.md) and [MULTI_CHANNEL_CALLING_BLUEPRINT.md](file:///d:/Try/Git-Repos/Kishan-Dialer/documents/MULTI_CHANNEL_CALLING_BLUEPRINT.md)).

---

## 2. High-Level Architecture (Clean MVVM + Domain Use Cases)

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                              │
│  MainActivity  (Compose UI, Scaffold, Material 3, FloatingCallPill)    │
├──────────┬─────────────┬──────────────┬──────────────┬────────────────┤
│ Favorites│   Dialer    │  CallLog     │  Contacts    │  Rules/Settings│
│  Screen  │   Screen    │  Screen      │  Screen      │  Screen        │
└────▲─────┴──────▲──────┴──────▲───────┴──────▲───────┴────────▲───────┘
     │             │             │              │                │
     │          StateFlow / Events / SharedFlow                  │
     ▼             ▼             ▼              ▼                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      VIEWMODEL LAYER                                   │
│  MainViewModel.kt (State, Coroutine scopes, callLoggedEvent collector) │
└────────────────────────────▲───────────────────────────────────────────┘
                             │
           ┌─────────────────┼──────────────────┐
           ▼                 ▼                  ▼
┌──────────────────┐ ┌─────────────────┐ ┌─────────────────────────────┐
│  Domain Use Cases│ │  Telecom Layer  │ │  Utility Layer              │
│ EvaluateSimRule  │ │ TelecomCallSvc  │ │ T9Helper (search engine)    │
│ ResolveCallerID  │ │ CallManager     │ │ ContactHelper               │
│ SearchT9Contacts │ │ SimHelper       │ │ PhoneNumberNormalizer       │
│ ManageFavorites  │ │ ReminderSched.  │ │ BackupManager               │
└────────┬─────────┘ │ TelecomVoipHelp │ │ CommunityCallerIdService    │
         │           └────────┬────────┘ └─────────────┬───────────────┘
         │                    │                         │
         ▼                    ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           DATA LAYER                                   │
│  AppRepository.kt                                                      │
│  ├── Room DB v12 (AppDatabase)                                         │
│  │   ├── FavoriteContact, RecentCall, CallerRule, SpamNumber           │
│  │   ├── ContactNumberPreference, LocalContact, IgnoredContact         │
│  │   └── AutomationLogItem                                             │
│  ├── Android System Providers (ContactsContract, CallLog.Calls)        │
│  └── SharedPreferences "kishan_dialer_prefs"                           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Package & Module Directory Structure

```
app/src/main/java/com/example/
├── MainActivity.kt                      # Entry point, permissions, navigation, FloatingCallPill
├── telecom/
│   ├── TelecomCallService.kt            # InCallService binding for incoming & outgoing calls
│   ├── CallManager.kt                   # Call state, automation pipeline, callLoggedEvent SharedFlow
│   ├── SimHelper.kt                     # Multi-SIM subscription resolution, roaming detection
│   ├── ReminderScheduler.kt             # AlarmManager-backed post-call reminder scheduling
│   ├── ReminderReceiver.kt              # BroadcastReceiver for reminder alarm firing
│   ├── TelecomVoipHelper.kt             # Android 14+ TelecomManager.addCall() VoIP continuity
│   ├── OmniCallRedirectionService.kt    # Outgoing call interception for SIM routing & WhatsApp redirect
│   ├── OngoingCallNotificationHelper.kt # Persistent status bar notification during active calls
│   ├── CallForegroundService.kt         # Foreground service keeping call state alive
│   ├── CallNotificationReceiver.kt      # BroadcastReceiver for Answer/Decline notification actions
│   ├── RoleHelper.kt                    # Default dialer role request wrapper
│   ├── FlipToShhhManager.kt             # Accelerometer flip-to-mute sensor listener
│   └── SpamNotificationHelper.kt        # Notification alerts for blocked spam calls
├── domain/
│   └── usecase/
│       ├── EvaluateSimRuleUseCase.kt    # DAG-weighted SIM rule conflict resolution + roaming avoidance
│       ├── ResolveCallerIdentityUseCase.kt  # Trust badge tier resolution
│       ├── SearchT9ContactsUseCase.kt   # Nickname + name + number T9 search
│       └── ManageFavoritesUseCase.kt    # Star, unstar, reorder, ignore contacts
├── ui/
│   ├── MainViewModel.kt                 # Central ViewModel, callLoggedEvent collector, dismissAllModals
│   ├── models/
│   │   ├── ContactSortModels.kt         # SmartContactSort enum (A-Z, Recent, NICKNAMES, etc.)
│   │   └── FavoritesModels.kt           # Favorite card style and display config
│   ├── screens/
│   │   ├── DialerScreen.kt              # T9 keypad, 2x2 action grid, nickname-aware suggestions
│   │   ├── FavoritesScreen.kt           # Grid hub, drag-reorder, dual-dialer buttons
│   │   ├── CallLogScreen.kt             # Recents, SIM badges, instant update via callLoggedEvent
│   │   ├── ContactsScreen.kt            # Directory, Nicknames filter, SIM routing, default number
│   │   ├── InCallScreen.kt              # Active call UI, roaming badge, DTMF, post-call notes
│   │   ├── RulesScreen.kt               # Automation pipeline, recipes gallery, simulator, history
│   │   └── SettingsScreen.kt            # SIM modes, backup/restore, spam, channel preferences
│   └── components/
│       ├── CompactSearchBar.kt          # Unified 42dp pill search input
│       ├── Keypad.kt                    # 12-key dial pad with haptics
│       ├── ContactRowItem.kt            # Contact row with Phone & WhatsApp quick icons
│       ├── ContactDetailsBottomSheet.kt # Numbers, SIM routing, call stats, channel preference
│       ├── RuleEditDialog.kt            # Full automation rule editor with IME insets
│       ├── RuleCard.kt                  # Visual pipeline chips, dry-run trigger, duplicate
│       ├── DialerSuggestionsList.kt     # T9 predictive suggestions (contactsByDigits + contactsByName)
│       ├── SpeedDialDialogs.kt          # Speed dial assignment and confirmation dialogs
│       ├── BackupManagementCard.kt      # Backup Now, list, restore, delete, browse import
│       ├── CallRedirectionCard.kt       # WhatsApp call mode selector
│       ├── MultiNumberCallDialog.kt     # SIM/channel picker for multi-number contacts
│       ├── AudioOutputSelectorDialog.kt # Audio routing picker
│       ├── SpamManagementDialog.kt      # Blocked numbers list, search, auto-block presets
│       └── WhatsAppIcon.kt              # Vector asset with dark-mode white contour ring
├── data/
│   ├── AppDatabase.kt                   # Room Database v12, MIGRATION_11_12, indices
│   ├── Entities.kt                      # All DB entities (see §5 for full schema)
│   ├── AppDao.kt                        # Room DAOs with Flow, indexed queries
│   └── AppRepository.kt                 # Aggregates Room DB + System Content Providers
└── util/
    ├── T9Helper.kt                      # T9 digit mapping + scored search results
    ├── ContactHelper.kt                 # ContactsContract queries, WhatsApp dispatch, number matching
    ├── PhoneNumberNormalizer.kt         # libphonenumber E.164 normalization & isInternational()
    ├── BackupManager.kt                 # JSON backup, public MediaStore, SHA-256 integrity
    ├── HapticFeedbackHelper.kt          # Android 13+ VibrationEffect primitives
    ├── CommunityCallerIdService.kt      # Offline caller trust tier lookup table
    └── AppCoroutineScope.kt             # SupervisorJob + CoroutineExceptionHandler app-wide scope
```

---

## 4. Key Subsystems & Workflows

### 4.1 Dual-Channel Dispatch (Cellular vs WhatsApp)
- **Cellular**: `TelecomManager.placeCall()` (default dialer) or `Intent.ACTION_CALL` fallback.
- **WhatsApp**: Number normalized via `PhoneNumberNormalizer.toE164()`, then dispatched via `https://api.whatsapp.com/send?phone=<e164>` with package constraint to `com.whatsapp`.
- **External call interception**: `OmniCallRedirectionService.onPlaceCall()` queries `ContactNumberPreference` for `preferredSimSlot` — binds the target `PhoneAccountHandle` or routes to WhatsApp and calls `cancelCall()`.

### 4.2 T9 Smart Search Engine (`T9Helper.kt`)
- Maps digits `2–9` to Latin letter clusters.
- Returns scored `T9SearchResult` list: exact prefix > number match > partial substring.
- `DialerSuggestionsList` uses dual lookup: `contactsByDigits` for number/T9 matching + `contactsByName` for caller-name nickname fallback (critical for incoming call recents).

### 4.3 E.164 Normalization (`PhoneNumberNormalizer.kt`)
- Wraps Google `libphonenumber` (`com.googlecode.libphonenumber:libphonenumber`).
- `toE164(raw, regionHint)`: normalizes any format to `+<country><number>`.
- `isInternational(context, number)`: returns `true` if number country differs from device SIM country — used to control SIM picker visibility in `ContactDetailsBottomSheet`.
- Used in: T9 search, contact matching, WhatsApp dispatch, spam lookup, call redirection.

### 4.4 InCallService & Telephony Integration
1. Android Telecom binds `TelecomCallService` on any call.
2. Delegates to `CallManager`, which enriches `ActiveCallInfo` with `simDisplayName` and `isRoaming` via `SimHelper.resolveSimInfo()`.
3. `CallManager` evaluates ambient geofence guards (`checkWifiSsid()`, `checkBluetoothDevice()`) before executing automation.
4. On call termination, emits `callLoggedEvent` SharedFlow and triggers `PostCallQuickActionCard`.
5. **Ambient Incoming Ring Silencing**: `CallManager.silenceRinger()` delegates to `telecomManager.silenceRinger()`, invoked on device pickup (accelerometer dynamic motion in `FlipToShhhManager`), proximity uncover, user touch/gestures on `InCallScreen`, audio route switching, quick SMS decline, or physical volume keys while keeping the call active in `STATE_RINGING`.

### 4.5 Ambient Geofencing (Zero-Battery Guards)
`CallManager.checkAndExecuteAutomation()` evaluates:
- **Wi-Fi SSID**: `WifiManager.connectionInfo.ssid` compared to `CallerRule.requiredWifiSsid`.
- **Bluetooth Device**: `BluetoothAdapter.bondedDevices` names compared to `CallerRule.requiredBluetoothDevice`.
Both checks are passive (no GPS, no geofence API) — zero additional battery drain.

### 4.6 Search Bar Architecture (`CompactSearchBar.kt`)
- All main tabs use `CompactSearchBar` constrained to **42dp** height.
- Live filtering performed in-memory on loaded dataset, preventing unnecessary DB re-queries per keystroke.

---

## 5. Local Data Persistence Schema (Room v12)

### `FavoriteContact`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK, auto-generate |
| `contactId` | `String` | System Contact ID |
| `name` | `String` | Full name |
| `phoneNumber` | `String` | Primary number |
| `nickname` | `String?` | User-assigned nickname |
| `displayOrder` | `Int` | Custom reorder sequence |
| `speedDialKey` | `Int?` | 1–9 speed dial shortcut |
| `avatarUri` | `String?` | Avatar URI |

### `CallerRule`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `name` | `String` | Rule display name |
| `phoneNumberPattern` | `String` | Exact, prefix (`+1*`), or wildcard |
| `isEnabled` | `Boolean` | Active/inactive toggle |
| `autoAnswer` | `Boolean` | Auto-answer on match |
| `answerDelaySec` | `Int` | Delay before auto-answer |
| `dtmfSequence` | `String?` | DTMF keys to send (e.g. `"9#"`) |
| `dtmfDelayMs` | `Long` | Delay before DTMF transmission |
| `autoHangup` | `Boolean` | Auto-hangup after action |
| `hangupDelaySec` | `Int` | Delay before auto-hangup |
| `autoSpeakerphone` | `Boolean` | Route audio to speaker on answer |
| `autoMuteMic` | `Boolean` | Mute mic during DTMF transmission |
| `requiredWifiSsid` | `String?` | Ambient Wi-Fi guard |
| `requiredBluetoothDevice` | `String?` | Ambient Bluetooth guard |
| `smsAutoReply` | `String?` | SMS auto-reply template |

### `ContactNumberPreference`
| Field | Type | Notes |
|---|---|---|
| `normalizedNumber` | `String` | PK (E.164) |
| `preferredSimSlot` | `Int` | -1=Ask, 0=Auto, 1=SIM 1, 2=SIM 2 |

### `SpamNumber`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `phoneNumber` | `String` | Raw formatted number |
| `normalizedNumber` | `String?` | E.164, indexed for O(1) lookup |
| `label` | `String?` | "Telemarketer", "Robocall", etc. |
| `reportCount` | `Int` | Community report count |

### `RecentCall`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `callerName` | `String?` | Resolved at query time |
| `phoneNumber` | `String` | Indexed |
| `callType` | `Int` | `CallLog.Calls.{INCOMING,OUTGOING,MISSED}_TYPE` |
| `timestamp` | `Long` | Epoch ms, indexed |
| `durationSeconds` | `Long` | |
| `simSlot` | `Int` | 1 or 2 |
| `note` | `String?` | Post-call user note |
| `reminderEpoch` | `Long?` | Scheduled reminder timestamp |

---

## 6. Testing Strategy

All tests run via `./gradlew testDebugUnitTest` using **Robolectric** (`@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [36])`). In-memory Room DB used for all DAO tests.

```bash
# Run all unit tests
./gradlew testDebugUnitTest --continue

# Screenshot regression (Roborazzi)
./gradlew verifyRoborazziDebug

# Record new reference screenshots after UI changes
./gradlew recordRoborazziDebug
```

See `documents/Verified.md` entry **#48** for full test suite coverage breakdown.

---

## 7. Build, Signing & Optimization Runbook

### 7.1 Keystore Auto-Restoration
`app/build.gradle.kts` auto-decodes `debug.keystore.base64` → `debug.keystore` on any build if missing.

### 7.2 APK Optimization (R8 & ProGuard)
Minification + resource shrinking reduce release APK from ~19 MB to ~5.2 MB. All Compose, Room, Telecom, and libphonenumber reflection rules preserved in `proguard-rules.pro`.

### 7.3 Building & Distributing Release APK
```bash
./gradlew :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk apks/OmniDial-v<version>.apk
cp apks/OmniDial-v<version>.apk apks/OmniDial.apk
```

---

## 8. External Outgoing Call Redirection Architecture

### 8.1 The HFP / Car Unit Challenge
When a call is initiated from a vehicle head unit (Bluetooth HFP `ATD` command), smartwatch, or voice assistant, it bypasses third-party app UIs and routes directly to the cellular radio modem.

### 8.2 `OmniCallRedirectionService` Solution
1. Registered with `BIND_CALL_REDIRECTION_SERVICE` permission and `RoleManager.ROLE_CALL_REDIRECTION`.
2. **`onPlaceCall(handle, initialPhoneAccount, allowInteractiveResponse)`**:
   - Resolves E.164 destination via `PhoneNumberNormalizer.toE164()`.
   - Queries `ContactNumberPreference` for `preferredSimSlot` → binds `PhoneAccountHandle`.
   - Queries `learned_call_modes` SharedPreferences for WhatsApp preference.
   - If cellular: `placeCallUnmodified()`.
   - If WhatsApp preferred: `cancelCall()` + dispatches WhatsApp VoIP intent.
   - If `"never"` mode: `placeCallUnmodified()` immediately, bypassing all redirections.

---

## 9. Maintenance & Extension Guide

### Adding a New CallerRule Automation Field
1. Add field to `CallerRule` in `data/Entities.kt`, bump Room DB version, add migration.
2. Add execution in `CallManager.executeAutomationWorkflow()`.
3. Add UI toggle in `RuleEditDialog.kt`.
4. Add field to `BackupManager` JSON serialization.
5. Add unit test assertion in `Phase11AutomationTest.kt`.

### Adding a New Panel / Tab
1. Add route in `MainActivity.kt` navigation state.
2. Implement Composable screen in `ui/screens/`.
3. Add navigation icon to `NavigationBar`.
4. Add Roborazzi screenshot test in `RealScreenshotTest.kt`.

### Troubleshooting Checklist
- **Calls don't open in-call screen**: Set OmniDial as default phone app → Android Settings → Apps → Default apps → Phone app.
- **WhatsApp icon does nothing**: WhatsApp must be installed; number must have a valid E.164 country code.
- **Roaming alert not showing**: Verify `READ_PHONE_STATE` permission granted and SIM has active subscription.
- **Backup not surviving reinstall**: Verify MediaStore / `WRITE_EXTERNAL_STORAGE` permissions on Android < 10.
- **Missing contacts in Recents**: Ensure `READ_CALL_LOG` and `READ_CONTACTS` permissions are granted.
