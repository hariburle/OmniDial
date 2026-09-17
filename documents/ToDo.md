# OmniDial — Product Roadmap & Active Task Backlog

## 📐 Engineering Principles & Design Constraints
1. **Telecom & System Standard Compliance**: Strict alignment with Android Telecom framework (`InCallService`, `CallRedirectionService`, Telecom VoIP APIs), Room SQLite, and Jetpack Compose Material 3.
2. **Sub-300ms Performance**: Zero layout jump on keypad typing, instant in-memory T9 indexing, and sub-millisecond caller ID lookup.
3. **Privacy First & Local-Only**: Contact records, learned calling modes, rule triggers, and spam metadata remain strictly on-device.

---

## 🎯 Active Strategic Roadmap (Reordered: Patches → Minor → Major)

### 🛠️ Phase 8 (Patch Release — v1.1.3): Architecture Modernization & Sensory Haptics
*Refactoring, state isolation, and tactile polish without changing the fundamental feature surface.*
- [x] **Task 8.1: Decompose `MainViewModel` into Domain Use Cases**
  - *Goal*: Break up 1,900+ line ViewModel into focused, single-responsibility domain use cases to avoid recomposition bottlenecks.
  - *Deliverable*: Extract `EvaluateSimRuleUseCase`, `ResolveCallerIdentityUseCase`, `SearchT9ContactsUseCase`, and `ManageFavoritesUseCase`.
- [x] **Task 8.2: Volatile In-Call Timer State Isolation**
  - *Goal*: Prevent full-screen recompositions on high-frequency 1Hz updates.
  - *Deliverable*: Isolated `CallDurationStatusChip` composable separating high-frequency duration counter from in-call parent view tree.
- [x] **Task 8.3: Rich Sensory Haptics (Android 13+ Primitives)**
  - *Goal*: Provide tactile feedback matching Google Pixel and Material 3 standards.
  - *Deliverable*: Integrated `HapticFeedbackHelper` utilizing Android 13+ `VibrationEffect.createPredefined(EFFECT_CLICK / EFFECT_HEAVY_CLICK / EFFECT_TICK)` across keypad tapping, long presses, and call connection events.
- [x] **Task 8.4: Direct Automatic Backup & Restore Manager (v1.1.4 — Build 6)**
  - *Goal*: Eliminate manual SAF file prompts with single-tap auto-backup, restore, and file management across dual storage directories.
  - *Deliverable*: Modular `BackupManagementCard` and updated `BackupManager` supporting automatic multi-location JSON backup, live list scan, one-tap restore, and file import.
- [x] **Task 8.5: Zero-Lag Keypad Switching & Pager Pre-Warming (v1.1.5)**
  - *Goal*: Eliminate frame drops and UI freezes when navigating from Contacts or Rules to Keypad.
  - *Deliverable*: Asynchronous background contact normalization and deduplication via `searchContacts` StateFlow on `Dispatchers.Default`, paired with `beyondViewportPageCount = 4` pre-warmed pager rendering.

---

### 🚀 Phase 9 (Minor Release — v1.2.0): E.164 Number Intelligence & Truecaller-Grade Spam Defense
*Core parsing upgrade, automated blocking presets, and after-call quick action card.*
- [x] **Task 9.1: Google `libphonenumber` E.164 Normalization Engine**
  - *Goal*: Replace ad-hoc string sanitization with strict E.164 parsing, international dialing code detection, and national format handling.
  - *Deliverable*: Integrate `com.googlecode.libphonenumber:libphonenumber` across T9 search, contact matching, WhatsApp dispatch, and call redirection.
- [x] **Task 9.2: Indexed `normalized_number` Database Columns**
  - *Goal*: Guarantee $O(1)$ sub-millisecond lookup latency during incoming call broadcasts.
  - *Deliverable*: Add indexed `normalized_number` columns across `recent_calls`, `favorite_contacts`, and `offline_spam_numbers` tables in Room DB.
- [x] **Task 9.3: Automated Spam Auto-Block Presets**
  - *Goal*: Provide one-tap defense presets in Spam Management.
  - *Deliverable*: Auto-block top spammers (score > threshold), private/restricted numbers, and foreign prefix wildcards before phone rings.
- [x] **Task 9.4: Smart After-Call Quick Action Card**
  - *Goal*: Provide immediate post-call utility upon call termination.
  - *Deliverable*: 4-second bottom sheet offering 1-tap "Save Contact", "Block & Report Spam", "WhatsApp Message", and "Set Reminder".

---

### 🌐 Phase 10 (Minor Release — v1.3.0): Deep Telecom & Calling Intelligence
*Advanced multi-SIM automation, trust badges, and Android 14 Telecom VoIP.*
- [x] **Task 10.1: Roaming-Aware & DAG Conflict-Resolved SIM Routing**
  - *Goal*: Enhance multi-SIM automation for international travelers and complex rule setups.
  - *Deliverable*: Real-time `isNetworkRoaming()` checks and DAG-based weighted rule conflict resolution.
- [x] **Task 10.2: Tiered Caller ID & Trust Badges**
  - *Goal*: Give upfront trust clarity to inbound callers.
  - *Deliverable*: Visual badges for Verified Business (Green), Emergency/Delivery (Amber), and High-Risk Spam (Red).
- [x] **Task 10.3: Android 14+ Telecom VoIP Continuity**
  - *Goal*: Integrate VoIP endpoints directly into system-level in-call handling.
  - *Deliverable*: Implement `TelecomManager.addCall()` using `CallAttributesCompat` / `CallAttributes`.
- [x] **Task 10.4: Per-Number Preferred SIM Routing (Cellular)**
  - *Goal*: Allow users to configure a preferred SIM slot (Always Ask / SIM 1 / SIM 2 / Carrier Name) per contact when using Cellular calling mode.
  - *Deliverable*: Add preferred SIM selector in contact edit/details sheet, persist preference in local Room storage, and automatically route outgoing cellular calls via the designated SIM.

---

### 🚨 Immediate Reliability & System UX Fixes (v1.4.2)
- [x] **Task 10.7: Missed Call Notification Auto-Scroll & Entry Highlighting**
  - *Goal*: When tapping a missed call notification, ensure OmniDial not only deep-links to Recents but also scrolls directly to the target missed call entry and applies a visual highlight pulse.
  - *Deliverable*: In CallLogScreen, accurately match the deep-linked call timestamp/number, trigger listState.animateScrollToItem(), and apply a temporary highlight container tint.
- [x] **Task 10.5: In-Call Screen Priority & Modal Auto-Dismiss on External Outgoing Calls**
  - *Goal*: When an external app (Maps, Browser, etc.) or system intent initiates an outgoing call while OmniDial has a contact view, modal, or bottom sheet open, automatically dismiss all open sheets/dialogs and immediately surface the active InCallScreen.
  - *Deliverable*: In MainActivity, observe active call state and external call intents to dismiss any active ContactDetailsBottomSheet, dialogs, or sub-screens, ensuring the user immediately sees the active in-call controls.
- [x] **Task 10.6: Public Storage Backup Persistence (Survive Clean Reinstalls)**
  - *Goal*: Save automatic and manual backups to public Documents/OmniDial/ or Download/OmniDial/ via MediaStore so backup files survive app uninstalls and rebuilds.
  - *Deliverable*: Update BackupManager to write backups to public external storage (Documents/OmniDial) using MediaStore on Android 10+ and standard storage on older versions, scan public storage on startup, and preserve backup history across app reinstalls.

---

### ⚡ Phase 11 (Minor Release — v1.4.0): Context-Aware Automation & Geofenced IFTTT Recipes
*Location guards, ambient Wi-Fi/Bluetooth triggers, visual execution pipeline, and simulated test mode (documented in [`documents/AUTOMATION_RECIPES_BLUEPRINT.md`](AUTOMATION_RECIPES_BLUEPRINT.md)).*
- [x] **Task 11.1: Visual Rule Pipeline & Template Gallery UX**
  - *Goal*: Replace static tag chips with an interactive step-by-step pipeline (`Ringing` ➔ `Delay` ➔ `Answer` ➔ `DTMF` ➔ `Hangup`) and add low-density template starter cards (Gate, Office, Car, Voicemail).
  - *Deliverable*: Modernized `RulesScreen`, updated `RuleCard`, and top-bar history icon.
- [x] **Task 11.2: Zero-Battery Ambient Geofencing (Wi-Fi SSID & Bluetooth Triggers)**
  - *Goal*: Guard gate buzzer and auto-answer rules so they only execute when the user is actually at home or in vehicle, with 0% extra battery drain.
  - *Deliverable*: Ambient context evaluator checking active Wi-Fi SSID and paired Bluetooth device in `CallManager.checkAndExecuteAutomation()`.
- [x] **Task 11.3: Audio Routing & Auto-Mute in Automation Engine**
  - *Goal*: Automatically route auto-answered calls to speakerphone and auto-mute the mic while DTMF tones play.
  - *Deliverable*: `autoSpeakerphone` and `autoMuteMic` flags in `CallerRule` executed in `CallManager.executeAutomationWorkflow()`.
- [x] **Task 11.4: In-App Simulated Rule Dry-Run ("Test Rule")**
  - *Goal*: Allow users to test and verify DTMF sequences and delay timings in real time without placing actual phone calls.
  - *Deliverable*: Interactive simulation trigger on `RuleCard` dispatching test events through `CallManager`.

---

## 📋 Historical Implementation Archive
All prior implemented features, architectural specifications, and releases have been archived in their respective project documents:
- **System Specifications & Architecture**: See [`documents/ARCHITECTURE.md`](ARCHITECTURE.md) and [`documents/DESIGN.md`](DESIGN.md).
- **Completed Version Release History**: See [`documents/CHANGELOG.md`](CHANGELOG.md) and [`documents/RELEASE.md`](RELEASE.md).
- **Strategic Expert Product Reviews**: See [`documents/SUNDAR_PICHAI_PRODUCT_REVIEW.md`](SUNDAR_PICHAI_PRODUCT_REVIEW.md) and [`documents/TRUECALLER_PRODUCT_REVIEW.md`](TRUECALLER_PRODUCT_REVIEW.md).
- **QA & Verification Checkpoints**: See [`documents/ToTest.md`](ToTest.md).
