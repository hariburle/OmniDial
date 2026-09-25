# 📞 OmniDial — Smart Android Phone & Call Manager

[![Android](https://img.shields.io/badge/Android-11%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Version-2.1.0%20(Build%2022)-brightgreen.svg)](documents/CHANGELOG.md)
[![Changelog](https://img.shields.io/badge/Changelog-Running_Log-orange.svg)](documents/CHANGELOG.md)
[![Architecture & Design](https://img.shields.io/badge/Architecture-Design_Doc-teal.svg)](documents/DESIGN.md)

**OmniDial** is an intelligent, feature-rich native Android Telecom phone dialer designed for modern Android devices. Built with **Jetpack Compose (Material 3)**, **Kotlin Coroutines/StateFlow**, and Android's native **Telecom `InCallService`**, it combines T9 smart search, horizontal panel swipe navigation, automated call screening/gate buzzer rules, persistent drag-and-drop VIP favorite grid reordering, SIM slot badges, dedicated spam protection, and dual-SIM management into a fast, private, dark-mode-first experience.

---

## 🚀 Key Features & Capabilities

### 1. 🎹 Smart T9 Dialer & Hybrid Keypad
- **Non-Scrolling 2x2 Call Action Grid**: Stable 2x2 grid (`Text Message`, `Phone`, `WhatsApp - Msg`, `WhatsApp - Voice`) with zero layout jumps.
- **Dynamic Keypad Channel Dock**: Clean horizontal channel bar (`[SIM 1]`, `[SIM 2]`, `[WhatsApp]`) dynamically reflecting active SIM names, roaming indicators, and installed VoIP channels for 1-tap switching.
- **Unified Call Choice Dialogs**: Streamlined channel selection modal with "Remember choice for this contact" preference toggle.
- **Adaptive Preference Highlighting**: Dynamically highlights preferred communication channels using high-contrast borders and subtle background tints based on learned caller intelligence.
- **Enhanced Dark Mode WhatsApp Icon**: Rendered with a high-contrast white outer contour ring for perfect AMOLED visibility.
- **Bluetooth & Car Head Unit Call Redirection**: `OmniCallRedirectionService` automatically routes outgoing calls from vehicle infotainment, smartwatches, or third-party dialers to WhatsApp VoIP when preferred.
- **Instant T9 Search**: Sub-millisecond indexed digit matching (<16ms) searching contact names, **nicknames**, and phone numbers as you type.
- **Pause (`,`) & Wait (`;`) Support**: Long-press `*` for Pause, long-press `#` for Wait, or insert via the overflow menu (`⋮`).
- **Quick Recents Bar**: Most recent caller avatars above the keypad for instant redialing.
- **Nickname-Aware Keypad Suggestions**: Predictive suggestions prominently display contact nicknames alongside formal names.

---

### 2. ⭐ VIP Favorites Hub & Grid Management
- **3 Responsive Card Themes**: **Bento**, **Grid (Quick Action)**, and **Material** card styles selectable in App Settings.
- **Dual Dialers Across All Styles**: In "Ask & Learn" or "Ask Always" mode, all card styles render side-by-side `Phone` and `WhatsApp` buttons. Once learned, cards collapse to the preferred channel.
- **Continuous Multi-Row Drag-and-Drop Reordering**: Custom grid order saved to Room DB & `SharedPreferences`, persisting across reinstalls.
- **Popular Contact Nickname Auto-Lookup**: Starring a popular contact automatically resolves its custom nickname from device contacts.
- **1-Tap Speed Dial Shortcuts**: Assign numbers 2–9 for instant long-press dialing on the keypad.

---

### 3. 📜 Call History & Recent Logs
- **SIM Slot Badges**: Every call record displays `SIM 1` / `SIM 2` (or `WhatsApp`) badge alongside timestamp and duration.
- **Instant Call Log Updates**: Outgoing, incoming, and missed calls appear in Recent Calls the moment a call ends — no refresh delay.
- **SIM & Roaming Call Alerts**: Incoming calls display the receiving SIM name with a prominent amber roaming warning when the active SIM is on network roaming.
- **Recents Search & Live Filtering**: Real-time filtering by caller name, number, or note.
- **Fresh Install Call Log Import**: Automatically seeds Room SQLite with system call history on first run.
- **Missed Call Notification Deep-Linking**: Tapping missed call notifications switches to Recents and auto-scrolls with a 3.5s highlight pulse on the target entry.
- **Post-Call Notes & Callback Reminders**: Add timestamped notes to recent call records and set `AlarmManager`-backed callbacks immediately after hanging up.
- **Post-Call Quick Action Card**: 4-second bottom sheet after call end offering 1-tap "Save Contact", "Block & Report Spam", "WhatsApp Message", and "Set Reminder".
- **Ambient Incoming Ring Silencing**: Picking up the phone (accelerometer lift / proximity uncover), interacting with the screen, selecting an audio route, tapping a quick decline message, or pressing the volume button automatically silences the loud ringer while keeping the call active in ringing state, giving you quiet focus to decide.

---

### 4. 📇 Unified Contact Directory
- **Smart Edit Routing**: Phone contacts open the native Android Contacts editor; local app contacts open the in-app `EditContactDialog`.
- **Per-Number Preferred SIM Routing**: Assign a preferred SIM slot (SIM 1, SIM 2, Auto, or Always Ask) per phone number. Outgoing calls auto-route via the designated SIM.
- **Nicknames Filter**: One-tap "Nicknames" filter tab instantly surfaces all contacts with saved nicknames.
- **Default Number Prioritization**: Default primary number always sorted to top with a `DEFAULT` badge when expanding contact details.
- **Partitioned Filter Search**: When a filter (Nicknames, Favorites, Recent, Frequent, Device/App) is active and you search, qualifying contacts appear at the top while any other matching contacts are cleanly partitioned into "Other Matches Outside Filter" with one-tap action sheets.
- **Phonebook Default Number Sync**: Setting a default number updates Android's system contacts and Google Contacts, keeping everything in sync.
- **Teach Preferred Channel Without Calling**: Set or reset channel preferences per contact directly in the Contact Details sheet.

---

### 5. 🤖 Automated Call Screening & Contextual Recipes
- **Visual Rule Pipeline**: Rule cards display the full execution sequence as colored Material 3 chips (`Ring` ➔ `Delay` ➔ `Answer` ➔ `Speaker` ➔ `Mute` ➔ `DTMF` ➔ `Hangup`).
- **Quick-Start Recipe Gallery**: Pre-configured automation templates (Gate Buzzer, Delivery Gate, Office Extension, VIP Ring) available at any time via the top "Recipes" button.
- **Gate / Intercom Buzzer**: Auto-answers after 1s → Sends DTMF `9#` → Auto-hangs up after 2s → Optional confirmation SMS.
- **Ambient Geofencing Guards**: Rules only execute when connected to the configured Wi-Fi SSID or Bluetooth device — zero extra battery drain.
- **Dynamic Audio Routing**: Auto-route answered calls to speakerphone and auto-mute mic during DTMF transmission.
- **Interactive Dry-Run Simulator**: Test and verify DTMF sequences and delay timings with a step-by-step simulation without placing real calls.
- **Rule Execution History**: Top-bar History icon opens an execution log sheet of recent rule triggers.
- **Rule Duplication**: One-tap clone of any rule with all actions and constraints.
- **Carrier Spam Filter Bypass**: Whitelists automation-rule matched calls, starred favorites, and saved contacts from carrier STIR/SHAKEN auto-rejection.

---

### 6. 📡 Intelligent Dual-SIM Management
- **Three Global SIM Modes** (selectable in Settings):
  - **System** (Default): Highlights your active system default SIM without clutter.
  - **Ask & Learn**: Allows full per-contact SIM preference customization.
  - **International**: Presents SIM selection for non-domestic numbers while keeping domestic calls on the primary SIM.
- **Direct SIM Name Pills**: Contact sheets display custom carrier/user SIM names without redundant technical labels.
- **Roaming Cost Alerts**: Proactive toast warnings when routing a call through an active roaming SIM.
- **DAG Conflict-Resolved SIM Rules**: Weighted rule resolver gives precedence to exact-match rules over prefix rules, preventing unintended routing conflicts.
- **Roaming Protection**: Real-time `isNetworkRoaming()` check automatically avoids roaming SIM and routes to local non-roaming SIM when possible.

---

### 7. 🏅 Tiered Caller ID & Trust Badges
- **High-Risk Spam (Red)**: Red `Spam Risk` badge on spam callers in both In-Call screen and Call Log.
- **Priority Delivery & Logistics (Amber)**: Amber `Priority Delivery` badge for verified logistics numbers.
- **Verified Business & Saved Contacts (Green)**: Green `Verified Caller` badge for saved contacts and community-verified businesses.
- All badges render with high-contrast Material 3 containers.

---

### 8. 🛡️ Dedicated Spam & Blocked Calls Center
- **Caller ID & Spam Default App**: Set OmniDial as Android's "Caller ID & spam app" — suspected spam is silenced and logged as missed calls (never silently dropped), with optional automatic blocking.
- **Dedicated Spam Window**: `SpamManagementDialog` with blocked list, quick search, manual blocking, auto-block presets, and unblocking.
- **E.164 Number Normalization**: Google `libphonenumber` integration for strict international number parsing across T9 search, contact matching, WhatsApp dispatch, and call redirection.
- **Auto-Block Presets**: One-tap defense against top spammers, private/restricted numbers, and foreign prefix wildcards.
- **Location-Aware International Detection**: Detects international numbers relative to cellular tower location and SIM country.

---

### 9. 💾 Backup & Restore
- **One-Tap Transactional Backups**: Single "Backup Now" tap saves all rules, speed dials, favorites, multi-channel configurations, and settings with atomicity and SHA-256 integrity verification.
- **Live Restore Progress**: Real-time restore progress updates without freezing the UI or leaving partially restored tables.
- **Public Storage Persistence**: Backups written to `Documents/OmniDial/` via MediaStore survive app uninstalls and reinstalls.
- **One-Tap Restore & File Manager**: Auto-scanned backup list with instant restore and safe-delete. "Browse Files" imports backups from other devices or cloud.
- **Cryptographic Integrity**: SHA-256 checksums and schema versioning protect against corrupted imports.

---

## 🛠️ How to Use

### Teaching Preferred Calling Channel Without Placing Calls
1. Open **Contacts** (Tab 3) or **Recents** (Tab 1) and tap a contact row to open the **Contact Details Sheet**.
2. Locate the **Preferred Channel** selector row directly below the phone number.
3. Tap **`Phone`** or **`WhatsApp`** to set the preference (or tap ✕ to return to Ask & Learn).
4. *Result*: The choice is saved immediately with a Toast confirmation without making a call.

### Reordering Favorites
1. Go to the **Favorites** tab.
2. Tap the top-right **Configure Mode** checkmark button (`✓`).
3. Touch and hold any favorite card, then drag it across rows. Release to drop.
4. Tap the checkmark again to lock the new layout. Custom order is persisted across reinstalls.

---

## 📦 Download & Installation

The latest release and prior versions are available in the `apks/` directory and on the [OmniDial Website](index.html):

- **[OmniDial-v2.1.0.apk](apks/OmniDial-v2.1.0.apk)** *(Latest — Build 22)*: In-call Bento Card UI overhaul with dynamic audio routing (Speaker toggle & multi-route Bluetooth picker), split conference participant view, fresh-install auto-restore banner, dial-pad avatar & contact name tap-to-open contact details, favorites in-row direct calling, and clean default number checkbox.
- **[OmniDial-v2.0.2.apk](apks/OmniDial-v2.0.2.apk)** *(Build 21)*: Multi-call & conference calling subsystem (Swap, Merge, individual participant hangup & conference hold/resume), floating call pill auto-dismiss on call termination, Just-in-Time contextual permission reminders, and Settings Permissions Hub.
- **[OmniDial-v2.0.1.apk](apks/OmniDial-v2.0.1.apk)** *(Build 20)*: Caller ID & spam default app (silence-and-log spam handling), honest spam badges, guided setup wizard, and call redirection recovery banner.
- **[OmniDial-v2.0.0.apk](apks/OmniDial-v2.0.0.apk)** *(Build 19)*: Multi-channel calling engine (Cellular SIM 1/2 + WhatsApp), dynamic keypad channel dock, unified call choice dialogs with remembered preferences, hot-path search indexing (<16ms), transactional backup/restore with live progress, and telecom callback lifecycle fixes.
- **[OmniDial-v1.5.0.apk](apks/OmniDial-v1.5.0.apk)** *(Build 18)*: Partitioned contact search outside active filters, ambient incoming call ring silencing on lift or interaction, and multi-channel calling engine architecture.
- **[OmniDial-v1.4.4.apk](apks/OmniDial-v1.4.4.apk)** *(Build 17)*: Smart SIM & roaming call alerts, instant call log updates, and polished nickname presentation.
- **[OmniDial-v1.4.3.apk](apks/OmniDial-v1.4.3.apk)** *(Build 16)*: Dedicated Nicknames filter tab and nickname-aware keypad suggestions.
- **[OmniDial-v1.4.2.apk](apks/OmniDial-v1.4.2.apk)** *(Build 15)*: Automation recipes gallery, keyboard auto-scroll in rule editor, and unified settings backup.
- **[OmniDial-v1.4.1.apk](apks/OmniDial-v1.4.1.apk)** *(Build 14)*: Interactive visual feature previews and direct caller rules navigation.
- **[OmniDial-v1.4.0.apk](apks/OmniDial-v1.4.0.apk)** *(Build 13)*: Dual SIM management modes, direct SIM name pills, and roaming cost alerts.
- **[OmniDial-v1.3.0.apk](apks/OmniDial-v1.3.0.apk)** *(Build 12)*: Per-number preferred SIM routing and unified nickname call log display.
- **[OmniDial-v1.2.0.apk](apks/OmniDial-v1.2.0.apk)** *(Build 8)*: Architecture modularization, faster call history search, and tamper-proof backups.
- **[OmniDial-v1.1.5.apk](apks/OmniDial-v1.1.5.apk)** *(Build 7)*: Instant keypad switching and pre-warmed panel navigation.
- **[OmniDial-v1.1.0.apk](apks/OmniDial-v1.1.0.apk)** *(Build 2)*: 2x2 Call Action grid, adaptive highlights, car call redirection, and WhatsApp dark mode contrast.
- **[OmniDial-v1.0.0.apk](apks/OmniDial-v1.0.0.apk)** *(Build 1)*: Initial release — Cellular + WhatsApp integration, Caller Rules, T9 search, and Flip-to-Shhh.
- **[OmniDial.apk](apks/OmniDial.apk)** *(Latest build direct alias)*

---

## 🔒 Permissions & Security

OmniDial requires standard telephony permissions to operate as your default phone handler:
- `READ_CONTACTS` & `WRITE_CONTACTS`: Sync contacts, nicknames, and starred favorites.
- `CALL_PHONE` & `MANAGE_OWN_CALLS`: Initiate and manage phone calls via Android Telecom.
- `READ_CALL_LOG` & `WRITE_CALL_LOG`: Display and manage recent call history.
- `ANSWER_PHONE_CALLS`: Auto-answer calls for user-configured automation rules.
- `SEND_SMS`: Send optional auto-response SMS messages for busy/gate rules.
- `READ_PHONE_STATE`: Detect active SIM slots, roaming status, and carrier information.

*All user data, call logs, contacts, and automation rules remain 100% private and stored locally on your device in Room SQLite database and SharedPreferences.*
