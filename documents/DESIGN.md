# OmniDial — Architecture & System Design Document

This document serves as the primary technical specification and maintenance guide for **OmniDial**. It documents the system architecture, component contracts, data persistence models, telephony integrations, build pipelines, and maintenance runbooks.

---

## 1. System Overview & Core Capabilities

OmniDial is a native Android Default Phone Dialer application built with modern **Kotlin** and **Jetpack Compose (Material 3)**. It is designed to function either as the system's default dialer or as a standalone secondary communication hub.

### Primary Capabilities
1. **Dual-Channel Calling**: Direct one-tap placing of calls through either Native Cellular (`TelecomManager` / `Intent.ACTION_CALL`) or WhatsApp Voice/Chat (`whatsapp://send?phone=...`).
2. **Instant T9 Keypad Search**: Real-time algorithmic matching of digits against contact names and phone numbers without layout bounce.
3. **Adaptive 3-Panel & 4-Panel Navigation**: Configurable layout switching between Favorites, Keypad, Recents, and Contacts, styled with Google Pixel design tokens.
4. **Custom Favorites & Speed Dial**: Reorderable card grid with 1-9 keypad long-press shortcuts, nicknames, and multi-number assignment.
5. **Caller Automation & Gate Rules**: Configurable rule engine for auto-answering, auto-rejecting, busy SMS auto-reply, and VIP whitelisting.
6. **In-Call Telecom Experience**: Custom full-screen in-call interface with mute, hold, DTMF keypad, speaker/Bluetooth audio route switching, and flip-to-silence gestures.

---

## 2. High-Level Architecture (Clean MVVM)

The application adheres to clean Model-View-ViewModel (MVVM) architecture with unidirectional data flow (UDF).

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                              │
│  MainActivity  /  InCallActivity  (Compose UI, Scaffold, Material 3)   │
├───────────────────┬───────────────────┬────────────────────────────────┤
│  FavoritesScreen  │   DialerScreen    │  CallLogScreen  /  Contacts    │
│  FavoriteGridCard │   Keypad / T9     │  ContactRowItem / DetailsSheet │
└─────────▲─────────┴─────────▲─────────┴───────────────▲────────────────┘
          │                   │                         │
          │             StateFlow / Events              │
          ▼                   ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          VIEWMODEL LAYER                               │
│                         MainViewModel.kt                               │
│     (State management, Coroutine scopes, live search filtering)        │
└─────────▲───────────────────▲─────────────────────────▲────────────────┘
          │                   │                         │
          ▼                   ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          DOMAIN / SERVICE LAYER                        │
│  TelecomCallService (InCallService)   │  T9Helper.kt (Search engine)   │
│  CallManager (Audio & Call state)     │  SimHelper (Dual SIM handler)  │
│  RoleHelper (Default dialer role)     │  ContactHelper (Phone & WA)    │
└─────────▲───────────────────▲─────────────────────────▲────────────────┘
          │                   │                         │
          ▼                   ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           DATA LAYER                                   │
│  AppRepository.kt                                                      │
│  ├── Room DB (AppDatabase: Favorites, Rules, Ignored, AutoLogs)       │
│  ├── Android System Providers (ContactsContract, CallLog.Calls)        │
│  └── SharedPreferences (Panel order, theme, default SIM preference)   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Package & Module Directory Structure

```
app/src/main/java/com/example/
├── MainActivity.kt                  # App launch entry point, permission onboarding, main Scaffold
├── telecom/                         # Android Telephony & InCall subsystem
│   ├── TelecomCallService.kt        # Android InCallService binding incoming & outgoing calls
│   ├── CallManager.kt               # Active call state, hold/mute, speakerphone routing
│   ├── OngoingCallNotificationHelper.kt # Persistent status bar notification during active calls
│   ├── CallForegroundService.kt     # Foreground service keeping call state alive
│   ├── CallNotificationReceiver.kt  # BroadcastReceiver for Answer/Decline notification actions
│   ├── SimHelper.kt                 # Multi-SIM subscription query and account picker
│   ├── RoleHelper.kt                # System default dialer role request wrapper
│   ├── FlipToShhhManager.kt         # Accelerometer-based flip-to-mute sensor listener
│   └── SpamNotificationHelper.kt    # Notification alerts for blocked spam calls
├── ui/
│   ├── MainViewModel.kt             # Central ViewModel managing state across screens
│   ├── models/
│   │   ├── ContactSortModels.kt     # Enums and state holders for contact sorting/filtering
│   │   └── FavoritesModels.kt       # Favorite order and display config models
│   ├── screens/
│   │   ├── DialerScreen.kt          # Keypad, T9 live suggestions, dialed number display
│   │   ├── FavoritesScreen.kt       # Grid of favorite cards, drag-reorder, speed dial info
│   │   ├── CallLogScreen.kt         # Recents call history, incoming/outgoing/missed filters
│   │   ├── ContactsScreen.kt        # Alphabetical address book, search, multi-number actions
│   │   ├── InCallScreen.kt          # Active call interface (Mute, Speaker, DTMF, Hang up)
│   │   └── RulesScreen.kt           # Caller automation rules manager and execution logs
│   ├── components/
│   │   ├── CompactSearchBar.kt      # Unified 42dp pill search input with clear button
│   │   ├── Keypad.kt                # 12-key telephone dial pad (0-9, *, #) with haptics
│   │   ├── ContactRowItem.kt        # Unified contact row with Phone & WhatsApp quick icons
│   │   ├── ContactDetailsBottomSheet.kt # Bottom sheet with numbers, call stats, rule creation
│   │   ├── MultiNumberCallDialog.kt # Dialog to select cellular vs WhatsApp when multiple numbers exist
│   │   ├── FavoriteGridCard.kt      # Visual card for pinned favorite contacts
│   │   ├── AudioOutputSelectorDialog.kt # Audio routing picker (Earpiece, Speaker, Bluetooth)
│   │   └── WhatsAppIcon.kt          # Vector asset for WhatsApp branding
│   └── theme/
│       ├── Color.kt                 # Pixel-inspired dynamic color schemes
│       ├── Theme.kt                 # Material 3 dynamic light/dark theme wrapper
│       └── Type.kt                  # Typography scale definition
├── data/
│   ├── AppDatabase.kt               # Room Database declaration (Version 3)
│   ├── Entities.kt                  # DB Entities: FavoriteContact, CallerRule, IgnoredContact, etc.
│   ├── AppDao.kt                    # Room DAOs with Flow queries
│   └── AppRepository.kt             # Repository aggregating Room DB + System Content Providers
├── util/
│   ├── T9Helper.kt                  # T9 numeric mapping and search algorithm
│   ├── ContactHelper.kt             # Querying ContactsContract and launching calls / WhatsApp
│   ├── BackupManager.kt             # JSON import/export for rules and custom favorites
│   └── CommunityCallerIdService.kt  # Offline spam list and caller identification
└── widget/
    └── SmartDialerWidgetProvider.kt # Home screen speed-dial widget provider
```

---

## 4. Key Subsystems & Workflows

### 4.1 Dual-Channel Dispatch (Cellular vs WhatsApp)
OmniDial enables rapid switching between native carrier calls and WhatsApp:
- **Cellular Call**: Executed via `TelecomManager.placeCall()` when default dialer, or fallback via `Intent.ACTION_CALL` with `tel:<number>`.
- **WhatsApp Call / Chat**:
  1. Phone number is sanitized using `ContactHelper.cleanNumberForWhatsApp(number)`.
  2. Strips all punctuation, dashes, spaces, and ensures international E.164 country code format.
  3. Launches via standard Android Intent:
     ```kotlin
     val intent = Intent(Intent.ACTION_VIEW).apply {
         data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
         setPackage("com.whatsapp")
     }
     ```
  4. If WhatsApp is not installed, gracefully falls back to browser URL or prompts the user.

### 4.2 T9 Smart Search Engine (`T9Helper.kt`)
- Maps dial pad digits `2-9` to standard Latin letter clusters (`2=ABC`, `3=DEF`, etc.).
- When user enters digits on the keypad:
  - Checks if the dialed digits match any substring of the contact's phone numbers.
  - Converts contact name initials and sub-words to numeric equivalents and tests for prefix and sub-sequence matches.
  - Returns scored `T9SearchResult` list sorted by relevance (exact prefix > number match > partial substring).

### 4.3 Search Bar Architecture (`CompactSearchBar.kt`)
To preserve vertical screen space on mobile devices:
- All main tabs (Favorites, Recents, Contacts) use `CompactSearchBar`.
- Height is constrained to **42.dp** with an internal `BasicTextField`, 18dp leading search icon, and quick-clear trailing button.
- Live filtering is performed in-memory on the loaded dataset, preventing unnecessary Room or ContentProvider re-queries on every keystroke.

### 4.4 InCallService & Telephony Integration (`TelecomCallService.kt`)
When OmniDial is the default dialer:
1. Android Telecom subsystem binds `TelecomCallService` on any incoming or outgoing call.
2. `TelecomCallService` delegates the active call to `CallManager`.
3. `CallManager` evaluates **Caller Rules** (`AppDao.getMatchingRule()`):
   - **Auto-Reject**: Call is rejected immediately via `call.reject(Call.REJECT_REASON_DECLINED)`. If an SMS response is configured, `SmsManager` sends the text template.
   - **Auto-Answer**: Call is answered via `call.answer(VideoProfile.STATE_AUDIO_ONLY)` with a configurable delay.
   - **Normal Ringing**: Displays full-screen `InCallActivity` or notification heads-up display.
4. Provides continuous audio routing control:
   - `CallAudioState.ROUTE_EARPIECE`
   - `CallAudioState.ROUTE_SPEAKER`
   - `CallAudioState.ROUTE_BLUETOOTH`
   - `CallAudioState.ROUTE_WIRED_HEADSET`

---

## 5. Local Data Persistence Schema

Local data is persisted across two engines:
1. **Room Database (`AppDatabase.db`, SQLite)** for app-specific custom data.
2. **Android System Content Providers** for system contacts and call logs.

### Room Database Entities
- **`FavoriteContact`**:
  - `id: Long` (Primary Key, Auto-generate)
  - `contactId: String` (System Contact ID)
  - `name: String`
  - `phoneNumber: String`
  - `nickname: String?`
  - `displayOrder: Int` (Reorder sequence)
  - `speedDialKey: Int?` (1-9 numeric speed dial shortcut)
  - `avatarUri: String?`
- **`CallerRule`**:
  - `id: Long` (Primary Key)
  - `name: String`
  - `pattern: String` (Exact number, prefix match, or wildcard)
  - `action: RuleAction` (`AUTO_ANSWER`, `AUTO_REJECT`, `RING_SILENT`, `VIP_BYPASS_DND`)
  - `smsAutoReply: String?`
  - `isEnabled: Boolean`
- **`AutomationLogItem`**:
  - `id: Long` (Primary Key)
  - `timestamp: Long`
  - `callerNumber: String`
  - `callerName: String?`
  - `actionExecuted: String`
  - `details: String`

---

## 6. Testing Strategy

OmniDial employs local JVM Robolectric and Roborazzi screenshot verification tests to prevent UI regressions without requiring a physical device or emulator.

### Screenshot Tests (`RealScreenshotTest.kt`)
Tests capture pixel-perfect 1080x2400 Pixel 8 renderings of:
- **`panel_1_favorites`**: Verifies 6 favorite cards in 2-column grid, reorder mode, and compact search bar.
- **`panel_2_keypad`**: Verifies 12-key numeric dialer, dialed number display, and dual call action buttons.
- **`panel_3_contacts`**: Verifies alphabetized contact list, dual Phone & WhatsApp action icons, and 42dp search bar.

### Execution Commands
```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Verify screenshot regression
gradle :app:verifyRoborazziDebug

# Record new reference screenshots after UI modifications
gradle :app:recordRoborazziDebug
```

---

## 7. Build, Signing & Optimization Runbook

### 7.1 Keystore Auto-Restoration
The repository includes `debug.keystore.base64`. During any Gradle build or assemble task:
- `app/build.gradle.kts` automatically checks if `debug.keystore` exists.
- If missing, it decodes `debug.keystore.base64` into a valid `debug.keystore` file automatically.
- This ensures consistent APK signing certificates across different developer machines and CI/CD without manual setup.

### 7.2 APK Optimization (R8 & ProGuard)
- Minification and resource shrinking are enabled in `app/build.gradle.kts`.
- Reduces release APK size from **19.2 MB down to ~5.2 MB**.
- All critical Compose, Room, and Telecom reflection rules are preserved in `app/proguard-rules.pro`.

### 7.3 Building the Release Executable
```bash
# Assemble optimized debug APK
gradle :app:assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
```
Copy to the project root for distribution:
```bash
cp app/build/outputs/apk/debug/app-debug.apk OmniDial.apk
```

---

## 8. Maintenance & Extension Guide

### How to Add a New Panel / Tab
1. Add new route enum in `MainActivity.kt` navigation state (`NavScreen`).
2. Implement Composable screen in `app/src/main/java/com/example/ui/screens/`.
3. Add navigation icon in `NavigationBar` in `MainActivity.kt`.
4. Add test case in `RealScreenshotTest.kt` to ensure layout stability.

### How to Add a New Caller Rule Action
1. Extend `RuleAction` enum in `data/Entities.kt`.
2. Add action handling logic in `telecom/CallManager.kt` (`handleIncomingCall()`).
3. Add UI selector for the new action in `ui/components/RuleEditDialog.kt`.

### Troubleshooting Checklist
- **Calls don't open in-call screen**: Verify OmniDial is set as default phone app in Android Settings -> Apps -> Default apps -> Phone app.
- **WhatsApp icon does nothing**: Check that WhatsApp is installed and that the phone number contains a valid country code.
- **Missing contacts in Recents**: Ensure `READ_CALL_LOG` and `READ_CONTACTS` permissions are granted.

---

## 9. External Outgoing Call Redirection Architecture

### 9.1 The Hands-Free Profile (HFP) & Car Unit Challenge
When a user initiates an outgoing phone call from an external system—such as a vehicle infotainment head unit (connected via Bluetooth Hands-Free Profile / HFP), a smartwatch, Bluetooth headset, or voice assistant—the external device issues an `ATD` dial command directly into the Android OS telephony stack. 

Under normal circumstances, this bypasses any third-party app's UI and routes directly to the cellular radio modem.

### 9.2 CallRedirectionService Solution
To solve this generically without vehicle-specific hacks, OmniDial implements Android's native `android.telecom.CallRedirectionService` via `OmniCallRedirectionService`:

1. **System Registration**: Bound with `android.permission.BIND_CALL_REDIRECTION_SERVICE` and registered with the system `CallRedirectionService` action.
2. **Role Acquisition**: Managed via Android's `RoleManager.ROLE_CALL_REDIRECTION` (or granted automatically when set as the default dialer).
3. **Interception Pipeline (`onPlaceCall`)**:
   - The OS routes every outgoing call intent from external systems to `OmniCallRedirectionService.onPlaceCall(handle, initialPhoneAccount, allowInteractiveResponse)`.
   - The service resolves the destination number and queries the user's preferred communication channel (from `learned_call_modes` or international routing rules).
   - If the contact is set to standard cellular: calls `placeCallUnmodified()`.
   - If the contact is preferred for WhatsApp: calls `cancelCall()` to prevent cellular toll charges / dialing, and dispatches the native WhatsApp VoIP Intent (`vnd.android.cursor.item/vnd.com.whatsapp.voip.call` or direct scheme) with fallback notification support.

