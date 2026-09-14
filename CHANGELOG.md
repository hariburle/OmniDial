# OmniDial — Ongoing Changelog & Running Work Log

> **Purpose**: This file serves as our real-time running log of every enhancement, UI refinement, and bug fix. As new changes are made, append them directly under `[Unreleased]` so nothing is ever forgotten when publishing future release notes and website updates.

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
