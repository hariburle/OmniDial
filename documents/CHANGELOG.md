# OmniDial — Ongoing Changelog & Running Work Log

> **Purpose**: This file serves as our real-time running log of every enhancement, UI refinement, and bug fix. As new changes are made, append them directly under `[Unreleased]` so nothing is ever forgotten when publishing future release notes and website updates.

---

## 🛠️ [Unreleased]

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
