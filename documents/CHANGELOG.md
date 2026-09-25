# OmniDial — Ongoing Changelog & Running Work Log

> **Purpose**: This file serves as our real-time running log of every enhancement, UI refinement, and bug fix. As new changes are made, append them directly under `[Unreleased]` so nothing is ever forgotten when publishing future release notes and website updates.

## 🚀 [v2.1.0] — Build 22 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Redesigned Bento Card In-Call UI**: Replaced legacy button layouts with an ergonomic caller hero card and a tactile 2x3 Bento control grid with Mute, Keypad, Add Call, Hold, and dynamic Audio routing.
- **Smart Audio Routing & Route Picker**: In standard 2-route mode (earpiece + speaker), the audio card acts as an instant "Speaker" toggle with active status tint. In multi-route mode (Bluetooth headset, car audio, hearing aids), it becomes an "Audio ▾" selector opening a clean 1-tap route picker popup.
- **Split Conference Participant View**: In conference calls, `ConferenceSplitView` displays separated caller cards with active/on-hold status badges and individual disconnect buttons.
- **Dialer Suggestion Cards Tap-to-Details**: Touching any contact's profile picture or name in the dial-pad recent calls or T9 search suggestions opens the complete `ContactDetailsBottomSheet` (with quick actions, all numbers, defaults, and favoriting) while tapping the rest of the card still populates the dialer.
- **Favorites Search In-Row Calling**: Searching in the Favorites tab now features direct in-row phone number calling, a dedicated call icon button, and sheet opening from the contact header.
- **Fresh Install Auto-Restore & Clean Default Checkbox**: Restoring backups is now prompted via a convenient banner immediately following a fresh install. Default contact numbers in contact cards now use a compact Material 3 checkbox instead of an oversized circular icon button.

### 🔧 Technical / Architecture Notes
- **Bento Grid Architecture**: Created `CallerHeroCard.kt`, `BentoCallControlGrid.kt`, and `ConferenceSplitView.kt` in `com.example.ui.components` with fluid Material 3 layout bounds and animated active states.
- **Audio Route Detection**: Enhanced `CallManager.kt` and `InCallScreen.kt` audio state observation to distinguish 2-way toggle vs multi-route picker based on `CallAudioState.supportedRouteMask`.
- **Dialer Suggestions Integration**: Added `onOpenContactDetails` callback cascade across `DialerSuggestionsList.kt`, `DialerRecentSuggestionCard`, `DialerMatchSuggestionCard`, and `DialerScreen.kt`, wiring into `ContactDetailsBottomSheet`.
- **Auto-Backup Test Suite**: Added `AutoBackupTest.kt` verifying periodic automated backup generation and public storage persistence.
- Incremented `versionCode` to 22 and `versionName` to `"2.1.0"` in `app/build.gradle.kts`.

---

## 🚀 [v2.0.2] — Build 21 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Multi-Call & Conference Calling Subsystem**: Seamless handling of multiple simultaneous calls. Swap between active and on-hold calls with 1 tap, merge calls into a conference, view dynamic participant cards with active/hold status, hold/unhold the conference, and disconnect individual callers without ending the entire conference.
- **Floating Call Pill Auto-Dismiss**: The floating ongoing call pill and Picture-in-Picture window cleanly dismiss immediately when a call terminates (remote disconnect or local hangup), preventing stuck floating widgets.
- **Just-In-Time (JIT) Contextual Permission Reminders**: Inline warning banners and empty-state action cards in Contacts (for missing contacts permission), Recents (for missing call log permission), and Rules (for missing call redirection role) with direct 1-tap grant/enable buttons.
- **Settings Permissions Hub**: Comprehensive card in Settings showing the live real-time status of all 6 permissions/roles with "Active" vs "Enable" chips, 1-tap setup step launching, full Setup Wizard rerun, and direct link to Android App Settings.

### 🔧 Technical / Architecture Notes
- **Conference & Multi-Call Engine**: Integrated full conference and multiple-call management in `CallManager.kt`, tracking primary and secondary `Call` objects, conference parent/children hierarchies, and per-participant call states.
- **ConferenceUiGating**: Added `ConferenceUiGating.kt` and comprehensive unit test suite `ConferenceUiGatingTest.kt` to robustly determine carrier-agnostic Swap, Merge, and Add Call UI button states.
- **FloatingCallPill & PiP Teardown Guard**: Added lifecycle collectors in `MainActivity.kt` and `CallManager.kt` to guarantee `isCallScreenMinimized = false` and PiP `finishAndRemoveTask()` on call disconnect transitions.
- **SetupStep Reuse**: Exported `SetupStepInfo` and `setupStepInfo()` from `SetupWizard.kt` to power the Settings Permissions Hub and live role badges.
- Incremented `versionCode` to 21 and `versionName` to `"2.0.2"` in `app/build.gradle.kts`.

---

## 🚀 [v2.0.1] — Build 20 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Caller ID & Spam Default App**: OmniDial can now be set as Android's "Caller ID & spam app" (Settings → Apps → Default apps). Suspected spam calls are silenced and logged as missed calls — never silently dropped — so a flagged call you wanted is never lost. An optional "Block spam automatically" switch rejects them outright.
- **Honest Spam Badges**: Recents now truthfully names who dropped a call — "Blocked by OmniDial · Carrier flagged as spam" or "Blocked by OmniDial · Number in spam list" — instead of the misleading "Carrier Auto-Dropped".
- **Guided Setup Wizard**: New first-run wizard walks through permissions, default dialer, car/watch call forwarding, appear-on-top, and wake-screen setup in plain language.
- **Call Redirection Recovery**: If the Call Redirection role is ever lost, a banner on the dialpad (with car-aware wording when a car recently connected) guides you to re-grant it, backed by a once-daily reminder notification.

### 🔧 Technical / Architecture Notes
- **OmniCallScreeningService**: New `CallScreeningService` implementation; spam-list numbers are silenced and logged by default, rejected only when `spam_auto_block` is enabled. Honors the user not-spam whitelist and saved contacts.
- **Unified Spam Policy**: When the Caller ID & spam role is held, `CallManager` defers spam-list handling to the screening service instead of auto-declining, eliminating double-handling; carrier-flagged calls stay on the legacy toggle.
- **Role Permission Update**: The screening service declares `android.permission.BIND_SCREENING_SERVICE`, as required by the system role controller on newer Android versions.
- **`spam_auto_block` Backup Coverage**: The new screening auto-block preference is now included in transactional backups and restores.
- Incremented `versionCode` to 20 and `versionName` to `"2.0.1"` in `app/build.gradle.kts`.

---

## 🚀 [v2.0.0] — Build 19 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Dynamic Keypad Channel Dock**: Clean horizontal channel bar (`[SIM 1]`, `[SIM 2]`, `[WhatsApp]`) dynamically reflecting active SIM names, roaming indicators, and installed calling apps directly above the dial pad for 1-tap switching before placing a call.
- **Unified Call Choice & Remembered Preferences**: Standardized `MultiChannelChoiceDialog` and `CallConfirmationDialog` with clean styling and a "Remember choice for this contact" preference toggle.
- **Blazing Fast Contact & Digit Search**: Keypad T9 and directory search latency optimized to <16ms, eliminating frame drops on keystrokes across large contact directories.
- **Transactional Backup & Live Restore Progress**: Backups and restores now run in safe atomic batches with real-time progress updates and SHA-256 integrity validation, preventing corrupted or half-restored states.
- **Contact Default Number Prioritization**: Setting a default contact number synchronizes seamlessly across device contacts, Google Contacts, and Room cache.
- **Streamlined Settings UI**: Consolidated Appearance and Navigation settings into a modern, compact card layout while removing obsolete dual-SIM menus.

### 🔧 Technical / Architecture Notes
- **Multi-Channel Calling Engine (MCCE)**: Core implementation of `ChannelDiscoveryManager`, `CallingChannel`, `ChannelConfigRepository`, `ChannelPreferenceRepository`, and `ChannelDispatchCoordinator`.
- **Room Database v17 Schema**: Added `number_channel_preferences`, `channel_configurations`, and `contact_default_numbers` tables with indexed lookup columns.
- **Hot-Path Performance Tuning**: Reordered `ContactHelper.matchesNumberQuery` checks to prioritize raw digit substrings before libphonenumber parsing; hoisted country calling codes and regexes; converted O(M²) recent call merges to indexed 10-digit hash map lookups.
- **Telecom Lifecycle Cleanup**: Explicitly unregister per-call `Call.Callback` in `CallManager.kt` on call teardown, eliminating context leaks and duplicate `handleCallEnded` events.
- **Release Build Signing**: Enforced strict upload keystore checks in `app/build.gradle.kts` with warning alerts to prevent silent debug signing.
- **Test Suite Expansion**: Added `Phase13MultiChannelCoreTest` (409 lines) and expanded `Task10Test` for channel preference persistence and backup verification.
- Incremented `versionCode` to 19 and `versionName` to `"2.0.0"` in `app/build.gradle.kts`.

---

## 🚀 [v1.5.0] — Build 18 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Partitioned Contact Search Outside Filter**: When any filter (Nicknames, Favorites, Recent, Frequent, Device/App) is selected on the Contacts tab and the user types a search query, matching contacts that do not meet the active filter criteria are cleanly partitioned below into a dedicated "Other Matches Outside Filter (N)" section with one-tap action sheet access and contextual status badges (`No Nickname`, `Not in Favs`, etc.), ensuring no contact is hidden.
- **Ambient Incoming Ring Silencing**: Picking up the phone (accelerometer lift / proximity uncover), touching or interacting with the incoming call screen (tapping anywhere, changing audio route, selecting quick decline SMS, or pressing physical volume keys) automatically silences loud ringer audio while preserving the active ringing call state for user decision.
- **Dedicated Incoming Call Silence Chip**: Added a prominent `[ 🔕 Silence ]` / `[ Silenced ]` action chip to the top status bar of `InCallScreen` for immediate 1-tap manual ringer silencing.

### 🔧 Technical / Architecture Notes
- **Partitioned Search Architecture**: Implemented two-pass filtering in `ContactsScreen.kt` partitioning search results into `primaryFilteredMatches` and `otherFilteredOutMatches` with Section Headers and `SearchExclusionReasonBadge`.
- **InCallScreen Ring Silencing Triggers**: Integrated `CallManager.silenceRinger(context)` invoking `telecomManager.silenceRinger()` and updating `isRingerSilenced` StateFlow without mutating device-wide ringer mode.
- **FlipToShhhManager Ambient Lift Detection**: Added accelerometer dynamic magnitude detection (`diff > 1.25 m/s²`) and proximity transition detection to invoke `CallManager.silenceRinger()` when ringing and phone is lifted or uncovered.
- **Physical Volume Key Intercept**: Added `onKeyDown` intercept in `MainActivity.kt` for `KEYCODE_VOLUME_UP` and `KEYCODE_VOLUME_DOWN` while ringing to invoke `CallManager.silenceRinger()`.
- **Multi-Channel Calling Blueprint**: Completed comprehensive specification in [`documents/MULTI_CHANNEL_CALLING_BLUEPRINT.md`](MULTI_CHANNEL_CALLING_BLUEPRINT.md) for dynamic `ChannelDiscoveryManager`, `CallingChannel` model, and per-phone-number `number_channel_preferences` (Cellular + WhatsApp in Phase 13; WhatsApp Business + Google Voice in Phase 14).
- **Unit Test Coverage**: Created `Phase12PartitionedSearchAndRingSilencingTest.kt` validating partitioned search logic and ringer silence state transitions.
- Incremented `versionCode` to 18 and `versionName` to `"1.5.0"` in `app/build.gradle.kts`.

---

## 🚀 [v1.4.4] — Build 17 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Smart SIM & Roaming Call Alerts**: Incoming calls now display the receiving SIM card name (`SIM 1`, `SIM 2`) and show a prominent amber warning alert when the active SIM is currently on network roaming before answering.
- **Instant Call Log Updates**: Outgoing, incoming, and missed call records now immediately populate in the recent calls list upon call termination without waiting for background system delays.
- **Polished Nicknames & Contact Presentation**: Contact details sheet highlights full names with nicknames in parentheses `(Nickname)` below, while contact list items display nicknames cleanly without redundant bracket text.
- **Cleaned Contact Filter Row**: Removed redundant inline filter tags (`⭐ Favorite`, `🏷️ Nickname`) from list items to provide full horizontal width for contact names.

### 🔧 Technical / Architecture Notes
- **SimHelper Roaming Resolution**: Added `resolveSimInfo(context, accountHandle)` in `SimHelper.kt` utilizing `SubscriptionInfo` and `TelephonyManager.isNetworkRoaming` to populate `isRoaming` and `simDisplayName`.
- **ActiveCallInfo Data Model**: Enriched `ActiveCallInfo` with `simDisplayName: String?` and `isRoaming: Boolean` properties in `CallManager.kt`.
- **InCallScreen & Notification UI**: Added custom amber `ROAMING` alert badge in `InCallScreen.kt` and updated `OngoingCallNotificationHelper.kt` notification status text.
- **Reactive Call Logging Event**: Added `callLoggedEvent: SharedFlow<Long>` in `CallManager.kt` triggered upon Room DB insertion; subscribed `MainViewModel.kt` to update `_combinedRecentCalls` state flow instantly (<10ms).
- Incremented `versionCode` to 17 and `versionName` to `"1.4.4"` in `app/build.gradle.kts`.

---

## 🚀 [v1.4.3] — Build 16 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Dedicated Nicknames Filter**: Added a top-level "Nicknames" filter tab with a clear face icon to the Contacts directory, enabling 1-tap filtering of all contacts with saved nicknames.
- **Nickname-Aware Keypad Suggestions**: Predictive suggestions and recent calls in the dialer panel now prominently display contact nicknames alongside their formal names.
- **Bi-Directional Recents Name & Number Resolution**: Keypad recent call suggestions now resolve contacts via both digits and names to reliably surface nicknames for incoming and outgoing records.

### 🔧 Technical / Architecture Notes
- **SmartContactSort Enum**: Added `NICKNAMES` to `SmartContactSort` in `ContactSortModels.kt` and integrated filter and sorting routines in `ContactsScreen.kt`.
- **DialerSuggestionsList Enhancement**: Added `contactsByName` map alongside `contactsByDigits` for caller name fallback lookup in `DialerSuggestionsList.kt`.
- **Unit Test Coverage**: Added `testSmartContactSortNicknames` in `ExampleUnitTest.kt` validating nickname filtering and enum properties.
- Incremented `versionCode` to 16 and `versionName` to `"1.4.3"` in `app/build.gradle.kts`.

---

## 🚀 [v1.4.2] — Build 15 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Always-Accessible Automation Recipes**: Browse and apply pre-configured automation templates (such as Gate Buzzer, Auto-Answer, and VIP Ringing) at any time from the top "Recipes" header button or the "Automation Recipes Gallery" banner, even when rules already exist.
- **Auto-Scrolling Rule Editor Keyboard**: Editing rule details or custom DTMF tones now smoothly and automatically scrolls focused text fields above the on-screen keyboard to prevent blocked inputs.
- **Conflict-Free Presets Grid**: Replaced horizontal preset chip scrollers with an adaptive wrapping layout, preventing accidental swipe conflicts when editing rules.
- **Unified & Complete Settings Backup**: SharedPreferences and configuration settings (including multi-SIM preferences and spam filters) now backup and restore with complete fidelity.

### 🔧 Technical / Architecture Notes
- **RuleEditDialog IME Insets**: Added `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` with `Modifier.systemBarsPadding().imePadding()` and `FlowRow` presets layout in `RuleEditDialog.kt`.
- **Recipes Bottom Sheet**: Extracted recipe templates into a modal bottom sheet accessible via `showRecipeSheet` in `RulesScreen.kt`.
- **Purged Orphan Sub-ViewModels**: Deleted unused legacy sub-ViewModels and unified state management across `MainViewModel.kt` and `MainActivity.kt`.
- **Preferences Storage Unification**: Unified SharedPreferences under `"kishan_dialer_prefs"` across `MainViewModel`, `CallManager`, `CallNotificationReceiver`, `SpamManagementDialog`, and `BackupManager`.
- **Telecom Subsystem Test Coverage**: Added Robolectric unit tests for `OmniCallRedirectionService`, `SimHelper`, and `ReminderScheduler`.
- Incremented `versionCode` to 15 and `versionName` to `"1.4.2"` in `app/build.gradle.kts`.

---

## 🚀 [v1.4.1] — Build 14 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Visual Capabilities Showcase & High-Res Lightbox**: Interactive visual capability cards embedded into documentation and showcase highlighting per-number SIM routing, HUD routing, automated DTMF, and post-call notes.
- **Default Navigation Fix**: Caller Rules tab opens directly to active rules roster by default.

### 🔧 Technical / Architecture Notes
- Refactored `index.html` capabilities grid with phone frame mockups and interactive lightbox modal.
- Incremented `versionCode` to 14 and `versionName` to `"1.4.1"` in `app/build.gradle.kts`.

---

## 🚀 [v1.4.0] — Build 13 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Dual SIM Preference Modes**: Introduced 3 global SIM modes in Settings (`System`, `Ask & Learn`, and `International`).
  - `System` (Default): Highlights active system default SIM card across contacts without clutter.
  - `Ask & Learn`: Highlights default SIM and allows customizing per-contact SIM preferences.
  - `International`: Intelligently shows SIM selection options for overseas/non-domestic numbers while keeping domestic calls on default SIM.
- **Direct SIM Name Pills**: SIM selection pills in contact sheets now directly display custom carrier and SIM names without redundant `SIM 1:` prefix text.
- **Channel-Aware SIM Display**: Per-contact SIM options automatically hide when WhatsApp is set as the preferred channel.
- **International Roaming Cost Alerts**: Added proactive toast alerts when placing calls via an active roaming SIM.
- **Streamlined Preferred Channel Action**: Replaced text Reset button with a sleek `✕` clear icon.

### 🔧 Technical / Architecture Notes
- **Global SIM Preference State & Storage**: Added `global_sim_pref_mode` (`system`, `ask_learn`, `international`) in `MainViewModel`, SharedPreferences, and Settings/Rules UI.
- **Contact Sheet Reactive SIM Routing**: Updated `ContactDetailsBottomSheet` to query `PhoneNumberNormalizer.isInternational(context, number)` and toggle SIM chip interactivity based on global SIM mode.
- **SimHelper Direct Name Resolution**: Refactored SIM display names to prefer carrier/custom user labels and resolved roaming status via `TelephonyManager.isNetworkRoaming`.
- Incremented `versionCode` to 13 and `versionName` to `"1.4.0"` in `app/build.gradle.kts`.

---

## 🚀 [v1.3.0] — Build 12 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Per-Number Preferred SIM Routing**: Select a preferred SIM slot (SIM 1, SIM 2, Auto, or Always Ask) for specific phone numbers in multi-number contacts. Outgoing calls to that number automatically route via the chosen SIM card.
- **Unified Call Log Nicknames**: Recent calls now display the contact's familiar nickname as the primary title with their formal name shown inline in parentheses.
- **Instant Nickname Display in Contact Sheet**: Opening contact details from recents or favorites immediately displays their saved nickname under their avatar instead of falsely showing `+ Add Nickname`.
- **Synchronized Nickname Editing**: Updating or setting a nickname in the contact sheet immediately propagates to Room favorites, local contact records, and the call history.
- **Steady In-Call Navigation & Background Management**: Ongoing calls stay cleanly active and accessible when navigating across panels or locking the device.

### 🔧 Technical / Architecture Notes
- **Room `ContactNumberPreference` & DAO**: Added entity and queries for persisting `preferredSimSlot` (`-1` = Ask, `0` = Auto, `1` = SIM 1, `2` = SIM 2) mapped by normalized phone number.
- **Telecom Redirection Service Routing**: Integrated `OmniCallRedirectionService` to query `preferredSimSlot` and dynamically bind target `PhoneAccountHandle` before placing external calls.
- **Optimized Call Log Lookups**: Introduced `fastFavoritesMap` in `CallLogScreen` and enriched `matchedDc` with `matchedFav.nickname` during scroll-time lazy item composition.
- **ViewModel Favorite Nickname Sync**: Updated `MainViewModel.updateContact()` to synchronize `newNickname` to matching `FavoriteContact` rows in Room DB.
- Incremented `versionCode` to 12 and `versionName` to `"1.3.0"` in `app/build.gradle.kts`.

---

## 🚀 [v1.2.3] — Build 11 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Phonebook Default Number Sync**: Setting a default phone number on any contact now directly updates Android's system contacts provider and Google Contacts, keeping your primary numbers in sync across your entire device.
- **Synced Favorites & Speed Dial**: Marking a number as default automatically updates Favorites cards and Speed Dial shortcuts to call that number directly.
- **Refined Quick Filters Layout**: The 5 discovery filters in Contacts (`All`, `Favorites`, `Recents`, `Frequent`, `Rediscover`) are styled with elegant, wide rounded rectangles and subtle accent outlines, evenly spaced without horizontal scrolling.
- **Single-Line Spam & Blocked Search**: Search bars in Spam & Blocked panels now cleanly fit without line-wrapping or layout distortions.

### 🔧 Technical / Architecture Notes
- **Android ContactsContract Batch Execution**: Implemented `ContactHelper.setDefaultPhoneNumber()` applying batch `ContentProviderOperation` to set `IS_PRIMARY = 1` and `IS_SUPER_PRIMARY = 1` on the targeted `Data._ID` while clearing primary flags on other numbers.
- **Reactive Favorites Synchronization**: Added `MainViewModel.setDefaultContactNumber()` updating both `ContactHelper` and `FavoriteContact` entities in the Room database, triggering reactive StateFlow recompositions.
- **Compose TooltipBox Layout Fix**: Wrapped each filter button in `Box(modifier = Modifier.weight(1f))` to correctly bound `TooltipBox` layout constraints and distribute all 5 filter buttons across the screen.
- **Spam Search Optimization**: Migrated search inputs in `SpamManagementDialog.kt` to `CompactSearchBar` with single-line constraint and ellipsis overflow.
- Incremented `versionCode` to 11 and `versionName` to `"1.2.3"` in `app/build.gradle.kts`.

---

## 🚀 [v1.2.2] — Build 10 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Default Phone Number Prioritization**: Default primary numbers are sorted to position #1 when contacts are expanded or opened in detail sheets, clearly highlighted with a "DEFAULT" badge.
- **Scrollable Filter Bar Above Search**: Top-level discovery filters are positioned above the contacts search bar in a horizontally scrollable chip row with clear labels and icons.
- **Contextual Search Field**: Search bar placeholder dynamically displays the selected filter context (`Search in Favorites...`, `Search in Recents...`, etc.).

### 🔧 Technical / Architecture Notes
- **Number List Reordering Engine**: Updated contact list builders (`DeviceContact` accumulator, `ContactRowItem`, `ContactDetailsBottomSheet`) to check `ContactHelper.isSamePhoneNumber` against `contact.phoneNumber` and place primary matches at index 0.
- **Filter Chips Migration**: Replaced fixed weight Surface button layout with Material 3 `FilterChip` and `horizontalScroll(rememberScrollState())` to prevent clipping on compact displays.
- Incremented `versionCode` to 10 and `versionName` to `"1.2.2"` in `app/build.gradle.kts`.

---

## 🚀 [v1.2.1] — Build 9 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Full Dual-Contact Keypad Suggestions**: Dialing a full 10-digit number displays all matching contacts in the predictive suggestion drawer, including phonebook and in-app contacts sharing a number.
- **Instant In-App Contact Recognition**: Newly created app contacts appear immediately across keypad search suggestions and directory lists without requiring manual sync or app restart.
- **Preserved Contact Visibility**: In-app contacts and device contacts sharing identical phone numbers are now both preserved across the contacts book without one hiding or overwriting the other.

### 🔧 Technical / Architecture Notes
- **Dialer Suggestion Filter Refinement**: In `DialerScreen.kt`, adjusted the duplicate exclusion predicate for `filteredT9Matches` from a loose `OR` condition to exact match (`!(isSameNum && isSameName)`), preventing distinct contacts sharing a number from being filtered out when one matches the top header card.
- **Identity-Based Deduplication in T9 Engine**: Updated `T9Helper.search()` to deduplicate candidates based on composite keys (`name + phoneNumber`) rather than purely `phoneNumber`.
- **Direct Search Match Resolution**: In `DialerScreen.kt`, integrated `allSearchContacts` direct lookup in `LaunchedEffect(number, allSearchContacts)` so in-app contacts resolve alongside phone contacts.
- **Race-Condition Free Contact Refresh**: In `MainViewModel.kt`, replaced `AtomicBoolean` guard in `refreshContacts()` with `Mutex.withLock` to guarantee queued refreshes execute and eliminated automatic pruning of local contacts on load.
- Incremented `versionCode` to 9 and `versionName` to `"1.2.1"` in `app/build.gradle.kts`.

---

## 🚀 [v1.2.0] — Build 8 (September 2026)

### 🌟 Enhancements (User-Facing)
- **High-Performance History & Fast Search**: Room database indices on recent calls and caller rules significantly accelerate query response times across large histories.
- **Tamper-Proof Automated Backups**: Backups now include cryptographic payload checksums (SHA-256) and schema versioning, protecting against corrupted or malformed imports.
- **Enhanced Dialing & Telecom Stability**: Safe registration checks and fallback mechanisms for self-managed PhoneAccount handles on custom OEM skins (e.g. Samsung OneUI, Xiaomi MIUI).
- **Crash-Resilient Background Operations**: Notification actions and background workers use dedicated application coroutine scopes with `SupervisorJob` to avoid cancellation cascading.

### 🔧 Technical / Architecture Notes
- **Sub-ViewModel Modularization**: Extracted specialized sub-ViewModels (`DialerViewModel`, `CallLogViewModel`, `FavoritesViewModel`, `RulesViewModel`, `ContactsViewModel`) backed by domain use cases and `AppRepository`.
- **Database Schema Migration**: Bumped Room database version to 12 with migration `MIGRATION_11_12` introducing database indices (`index_recent_calls_phoneNumber`, `index_recent_calls_timestamp`, `index_caller_rules_phoneNumberPattern`).
- **Compose Stability**: Applied `@Immutable` annotations to UI and telecom state data models (`ContactPhoneNumber`, `DeviceContact`, `ActiveCallInfo`, `AutomationStep`, `BluetoothDeviceItem`, `SimInfo`) to skip redundant Compose recompositions.
- **Structured Concurrency**: Created `AppCoroutineScope` with `SupervisorJob` and `CoroutineExceptionHandler`, integrated `goAsync()` with structured coroutine launching in `CallNotificationReceiver`, and added lifecycle cancellation cleanup in `CallForegroundService`.
- Incremented `versionCode` to 8 and `versionName` to `"1.2.0"` in `app/build.gradle.kts`.

---

## 🚀 [v1.1.5] — Build 7 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Instant Keypad & Screen Switching**: Switching to the Keypad tab from Contacts, Rules, or Favorites is now completely instantaneous with zero lag or frame stutter, even with massive contact books.
- **Pre-Warmed Navigation**: Smooth swiping across all five core panels with zero hesitation or blank loading flickers.

### 🔧 Technical / Architecture Notes
- **Background Contact Ingestion**: Offloaded contact deduplication, favorite mapping, and nickname resolution from the UI composition thread to a background coroutine flow (`searchContacts` on `Dispatchers.Default`) in `MainViewModel`.
- **Pre-Warmed Viewport Retention**: Configured `HorizontalPager` with `beyondViewportPageCount = 4` so all 5 panels remain pre-warmed in memory.
- Incremented `versionCode` to 7 and `versionName` to `"1.1.5"` in `app/build.gradle.kts`.

---

## 🚀 [v1.1.4] — Build 6 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Direct Automatic Backups**: Replaced confusing manual file export prompts with a single-tap "Backup Now" action that automatically saves all rules, speed dials, favorites, and settings directly to internal and external device storage.
- **One-Tap Restore & Backup Manager**: Saved backups are automatically scanned and listed with their timestamp and file size, featuring instant one-tap Restore and Delete actions with safety confirmation dialogs.
- **File Import & Cross-Device Transfer**: Added a dedicated "Browse Files" option to easily import and restore backups transferred from other devices, cloud drives, or previous installations.

### 🔧 Technical / Architecture Notes
- **Multi-Location Storage Sync**: Updated `BackupManager.kt` to mirror backups to `context.getExternalFilesDir(null)/backups` as well as private internal storage, ensuring persistence and ease of access without requiring raw storage permissions.
- **Settings & Dialer Modularization**: Extracted speed dial dialogs (`SpeedDialDialogs.kt`), dialer suggestions (`DialerSuggestionsList.kt`), backup management (`BackupManagementCard.kt`), and call redirection (`CallRedirectionCard.kt`) out of bloated screens to enforce <500 line modularity guidelines.
- Incremented `versionCode` to 6 and `versionName` to `"1.1.4"` in `app/build.gradle.kts`.

---

## 🚀 [v1.1.3] — Build 5 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Pixel-Grade Sensory Haptics**: Implemented modern Android 13+ tactile vibration feedback (`EFFECT_CLICK`, `EFFECT_HEAVY_CLICK`, `EFFECT_TICK`) for crisp dial pad keystrokes, speed-dial long presses, and call connection confirmation.
- **Smoother In-Call Performance**: The active in-call screen duration timer is isolated from the main layout tree, preventing 1Hz frame drops and keeping controls fluid.

### 🔧 Technical / Architecture Notes
- **Domain Use-Case Extraction**: Modularized business logic into dedicated domain use cases (`EvaluateSimRuleUseCase`, `ResolveCallerIdentityUseCase`, `SearchT9ContactsUseCase`, `ManageFavoritesUseCase`).
- **State Isolation**: Extracted `CallDurationStatusChip` to handle high-frequency timer recompositions independently of parent UI.
- **Haptic Helper**: Created `HapticFeedbackHelper` managing API 33+ predefined `VibrationEffect` primitives with backwards compatibility to API 24.
- Incremented `versionCode` to 5 and `versionName` to `"1.1.3"` in `app/build.gradle.kts`.

---

## 🚀 [v1.1.2] — Build 4 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Accurate WhatsApp & SMS International Number Matching**: When dialing a 10-digit or domestic number for a contact saved in your address book with an international prefix (e.g. `+91 9663306802`), the app automatically retrieves and uses the contact's stored country code instead of defaulting to device locale.
- **Dialer Direct Action International Resolution**: All dial pad quick actions (WhatsApp Chat, WhatsApp Call, SMS, Hero Call button) immediately resolve and pass the matched contact's full international number format.

### 🐛 Fixes & Polish (User-Facing)
- **WhatsApp Fallback Region Fix**: Fixed an issue where typing a 10-digit number on a US-locale device/emulator for an Indian or international contact could inadvertently prepend `+1` instead of the contact's stored `+91` calling code.

### 🔧 Technical / Architecture Notes
- Enhanced `ContactHelper.resolveFullInternationalNumber()` to directly query `ContactsContract.CommonDataKinds.Phone.CONTENT_URI` by matching trailing digits against all saved numbers with a `+` prefix.
- Updated `DialerScreen.kt` with dynamic `effectiveNumber` resolution from `matchedContact` and `allSearchContacts`.
- Incremented `versionCode` to 4 and `versionName` to `"1.1.2"` in `app/build.gradle.kts`.

---

## 🚀 [v1.1.1] — Build 3 (September 2026)

### 🌟 Enhancements (User-Facing)
- **T9 Search for Nicknames**: Dial pad T9 search now indexes and searches contact nicknames alongside full names and phone numbers. If a contact matches by nickname, the nickname is displayed cleanly next to their name.
- **Default Speed Dial Layout set to 'Fav & T9'**: The Speed Dial Keypad display setting defaults to "Fav & T9", displaying speed dial contact names above digits and standard T9 letters below.
- **Non-Jumping Fixed Dial Pad Action Buttons**: Removed vertical dialer jumping when typing digits by keeping quick action buttons (SMS, WhatsApp Chat, Secondary Call) permanently visible below the keypad. Users can also toggle their visibility in Settings.
- **Automatic International Country Code Resolution**: When sending a WhatsApp message or making a WhatsApp/SMS call, the app automatically checks stored contact records for the complete international number format or prepends the device's country calling code (e.g. +91, +1, +44), preventing WhatsApp "contact not available" errors.
- **Key 1 Voicemail & Speed Dial Clean Rendering**: Eliminated duplicate "VM" and "Voicemail" labels on Key 1, showing a single clean icon and label or the assigned speed dial contact name.
- **Dual Display Keypad (Speed Dial + T9 Letters)**: Keys 2–9 display both assigned speed dial contact names and standard T9 letters simultaneously with optimized aspect ratio and slot heights.
- **Speed Dial Confirmation & Assignment Settings**: Added accidental touch protection toggle in Settings and an automatic prompt dialog when long-pressing unassigned keypad keys.
- **Dynamic Contact Name Resolution in Recents**: The call log dynamically resolves and displays saved contact names, avatars, and initials for numbers in real-time.
- **Bluetooth Audio Route Selector**: Route audio between earpiece, speakerphone, wired headsets, and Bluetooth audio devices.

### 🐛 Fixes & Polish (User-Facing)
- **WhatsApp Country Code Fix**: Numbers dialed without an explicit country code prefix automatically resolve their full international number before passing to WhatsApp.
- **Key 1 Duplicate Label Fix**: Prevented redundant speed-dial and T9 VM text from appearing stacked on key 1.
- **Keypad Jump Elimination**: The keypad maintains a fixed, stable height when typing, eliminating annoying layout shifts.
- **Speed Dial UI Polish**: Added dedicated number badge `#X` and formatted phone number labels inside speed dial dialog for improved readability.
- **Zero-Crash Dialer Resuming**: Fixed startup and resume crash (`SecurityException` on `cancelMissedCallsNotification`) by ensuring the app only clears system missed call notifications when it actively has the Default Dialer role.
- **Favorite Star Instant Refresh**: Fixed a bug where the favorite star icon inside the contact detail bottom sheet of the Recents list would not immediately update or reflect the correct state after starring.
- **Compact Call History Entries**: Refactored the call history layout in the contact detail bottom sheet to prevent text wrapping, eliminating massive empty spaces and visual stretching.

### 🔧 Technical / Architecture Notes
- Added `T9Helper.kt` nickname search matching and updated `T9SearchResult` model.
- Added `resolveFullInternationalNumber` and `launchSms` in `ContactHelper.kt`.
- Updated `MainViewModel.kt` with `showDialerQuickActions` and default `speedDialKeypadDisplay = "speed_dial_above"`.
- Updated `app/build.gradle.kts` versionCode to 3 and versionName to "1.1.1".
- Deduplicated `DialerScreen.kt` suggestions list and bound nickname displays to `DialerMatchSuggestionCard`.

---

## 🚀 [v1.1.0] — Build 2 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Cleaner 4-Button Dial Pad**: Quickly reach anyone with 4 organized buttons on the keypad: Send Text, Regular Phone Call, WhatsApp Message, or WhatsApp Voice Call (replaces scrolling carousels).
- **Smart Call Suggestions**: The app gently highlights how you usually contact each person using subtle high-contrast borders, eliminating guesswork.
- **Car Bluetooth & Hands-Free Calling**: Outgoing calls initiated from vehicle infotainment systems, smartwatches, or Bluetooth headsets automatically route through WhatsApp when preferred.
- **Default Phone Number Selection for Favorites**: Starred contacts with multiple numbers (work, home, mobile) allow explicitly choosing which line is dialed by default.
- **Crisp Dark Mode Contrast**: WhatsApp icons and action buttons feature high-contrast outlines to stay sharp on dark AMOLED screens.
- **Friendlier Nicknames**: Personal nicknames appear front-and-center on your favorites grid with legal names cleanly subtitled below.

### 🐛 Fixes & Polish
- **WhatsApp Call Confirmation Routing**: Resolved bug where tapping WhatsApp call on favorite cards could accidentally trigger cellular calls or bypass confirmation.
- **Back Navigation on Search**: Pressing the Android back button when searching favorites or contacts cleanly dismisses the search bar rather than closing the screen.
- **Bi-directional Nickname Sync**: Ensured nickname changes in device contacts and the in-app editor immediately stay in lockstep.

### 🔧 Technical Notes
- Implemented Android Telecom `CallRedirectionService` (`OmniCallRedirectionService`).
- Updated `DialerScreen.kt` call action area to a fixed 2x2 grid with adaptive `colorScheme.primary` container borders.
- Added white contour outer path stroke in `WhatsAppIcon.kt`.
- Added multi-number picker flow to `FavoritesScreen.kt`.

---

## 📦 [v1.0.0] — Build 1 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Dual Calling in One App**: Seamlessly place regular carrier phone calls or WhatsApp voice calls from a single unified dialer.
- **Smart Gate & Intercom Buzzer**: Auto-answers entrance intercoms, transmits door DTMF tones (e.g. `9#`), and hangs up automatically.
- **Instant T9 Search**: Fast contact and recent call search as you type letters or numbers on the dial pad.
- **Flip to Silence**: Turn phone face-down to immediately silence incoming rings.
- **Customizable VIP Favorites Hub**: Keep priority contacts within thumb's reach with customizable grid cards and 1-tap speed dial shortcuts.

---

### 📝 How to Update This Running Log
Whenever making code changes:
1. Add new items to `[Unreleased]` under the appropriate subhead.
2. Phrase user-facing bullets in terms of real-world everyday benefits.
3. Keep technical notes under `🔧 Technical / Architecture Notes` for engineering context.
4. When releasing a new version, promote `[Unreleased]` into a tagged release section and update `RELEASE.md` + website.
