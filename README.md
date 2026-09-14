# 📞 OmniDial — Smart Android Phone & Call Manager

[![Android](https://img.shields.io/badge/Android-11%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Changelog](https://img.shields.io/badge/Changelog-Running_Log-orange.svg)](CHANGELOG.md)
[![Architecture & Design](https://img.shields.io/badge/Architecture-Design_Doc-teal.svg)](DESIGN.md)

**OmniDial** is an intelligent, feature-rich native Android Telecom phone dialer designed for modern Android devices. Built with **Jetpack Compose (Material 3)**, **Kotlin Coroutines/StateFlow**, and Android's native **Telecom `InCallService`**, it combines T9 smart search, horizontal panel swipe navigation, automated call screening/gate buzzer rules, persistent drag-and-drop VIP favorite grid reordering, SIM slot badges, dedicated spam protection, and dual-SIM management into a fast, private, dark-mode-first experience.

---

## 🚀 Key Features & Capabilities

### 1. 🎹 Smart T9 Dialer & Hybrid Keypad
- **Non-Scrolling 2x2 Call Action Grid**: Replaced scrolling carousels with a stable 2x2 grid (`Text Message`, `Phone`, `WhatsApp - Msg`, `WhatsApp - Voice`).
- **Adaptive Preference Highlighting**: Dynamically highlights preferred communication channels using high-contrast borders and subtle background tints based on learned caller intelligence, avoiding intrusive badge tags.
- **Enhanced Dark Mode WhatsApp Icon**: Rendered with a high-contrast white outer contour ring around the bubble to guarantee perfect visibility and size harmony in dark AMOLED themes.
- **Bluetooth & Car Head Unit Call Redirection**: Android `CallRedirectionService` (`OmniCallRedirectionService`) automatically routes outgoing calls triggered from vehicle infotainment systems, smartwatches, or third-party dialers to WhatsApp VoIP when preferred.
- **T9 Search**: Instant T9 matching on digits that searches contact names, nicknames, and phone numbers as you type.
- **Pause (`,`) & Wait (`;`) Support**:
  - Long-press `*` key to enter a 2-second Pause (`,`).
  - Long-press `#` key to enter a Wait (`;`).
  - Dedicated overflow menu (`⋮`) on the number bar to insert Pause or Wait directly.
- **Quick Recents Bar**: Displays your most recent caller avatars right above the keypad when the input field is empty for instant redialing.

---

### 2. ⭐ VIP Favorites Hub & Grid Management
- **3 Responsive Card Themes**: Select between **Bento**, **Grid (Quick Action)**, and **Material** card styles in App Settings.
- **Dual Dialers Across All Styles**: In "Ask & Learn" or "Ask Always" mode, all favorite card styles render side-by-side dual action buttons (`Phone` and `WhatsApp`). Once learned, cards collapse to the preferred channel.
- **Continuous Multi-Row Drag-and-Drop Reordering**:
  - Unlock Configure Mode via the top-right checkmark button.
  - Touch and drag any card smoothly across 1, 2, 5, or 10 rows.
  - Custom grid order is saved to Room DB & `SharedPreferences` for persistence across reinstalls.
- **Popular Contact Nickname Auto-Lookup**: Starring a popular contact or number automatically resolves its custom nickname from device contacts.
- **1-Tap Speed Dial Shortcuts**: Assign numbers 2–9 to VIP favorites for instant long-press dialing on the keypad.

---

### 3. 📜 Call History & Recent Logs
- **SIM Slot Badges**: Every call record explicitly displays a `SIM 1` / `SIM 2` badge (or `WhatsApp` badge) alongside the timestamp and duration.
- **Recents Search & Live Filtering**: Top search bar (`"Search by name or number"`) to filter call history by caller name, number, or note in real time.
- **Fresh Install Call Log Import**: Automatically seeds Room SQLite database with system call history logs on first run.
- **Missed Call Notification Deep-Linking**: Tapping missed call notifications switches directly to Recents and auto-scrolls to highlight the call.
- **Post-Call Notes & Callback Reminders**: Add timestamped notes to recent call records immediately after hanging up.

---

### 4. 📇 Unified Contact Directory & Edit Routing
- **Official Full Names Directory**: The main Contacts Directory always displays official full names (`contact.name`), reserving nicknames for space-limited Favorite cards.
- **Smart Edit Routing**: Tapping **Edit** on a Phone ContactSynced from Android system opens the native **Android Phone Contacts app** editor directly (`Intent.ACTION_EDIT`). Tapping Edit on a Local App Contact opens the in-app `EditContactDialog`.
- **Equal Action Icons**: All 5 action buttons (Copy, WhatsApp Chat, WhatsApp Call, SMS, Phone Call) use 1:1 identical 36dp circular containers with crisp 18dp vector glyphs, high-contrast borders, and adaptive theme colors for Light and Dark modes.
- **Teach Preferred Channel Without Calling**: Set or reset channel preferences (`Phone`, `WhatsApp`, `Ask & Learn`) per contact directly in the Contact Details sheet without placing a call.

---

### 5. 🤖 Automated Call Screening & Gate Buzzer Rules
- **Carrier Spam Filter Bypass**: Automatically whitelists incoming calls that match an active **User Automation Rule**, **Starred Favorite**, or **Saved Contact**, ensuring gate buzzers and VIP contacts are NEVER auto-rejected by carrier STIR/SHAKEN filters.
- **Gate / Intercom Buzzer Recipe**:
  - *Trigger*: Incoming call from Gate / Lobby number (e.g., `+1 469-731-3343` or `5550199`).
  - *Action*: Auto-answers after 1 second $\rightarrow$ Sends in-band DTMF `9#` $\rightarrow$ Auto-hangs up after 2 seconds $\rightarrow$ Sends optional confirmation SMS.
- **Custom Rule Builder**: Create rules triggered by exact numbers or 10-digit patterns with customizable auto-answer delays, DTMF key sequences, auto-hangup delays, and auto-reply SMS messages.
- **Fail-Safe Persistence**: Automation rules and learned calling choices are backed up via Android Auto-Backup (`backup_rules.xml` & `data_extraction_rules.xml`) and `SharedPreferences` for zero data loss across reinstalls.

---

### 6. 🛡️ Dedicated Spam & Blocked Calls Center
- **Dedicated Spam Window**: `SpamManagementDialog` provides blocked numbers list, quick search, manual blocking, auto-block carrier spam toggles, and unblocking.
- **Location-Aware International Detection**: Detects international numbers relative to your physical cellular tower location (`TelephonyManager.networkCountryIso`) and SIM country, routing foreign calls to WhatsApp automatically if configured.
- **Safety Confirmation**: Global "Reset Learned Choices" in Settings prompts with a safety confirmation dialog before clearing learned channel memories.

---

## 🛠️ How to Use

### Teaching Preferred Calling Channel Without Placing Calls
1. Open **Contacts** (Tab 3) or **Recents** (Tab 1) and tap a contact row to open the **Contact Details Sheet**.
2. Locate the **Preferred Channel** selector row directly below the phone number.
3. Tap **`Phone`** or **`WhatsApp`** to set the preference (or tap **`Reset`** to return to Ask & Learn).
4. *Result*: The choice is saved immediately with a Toast confirmation without making a call!

### Reordering Favorites
1. Go to the **Favorites** tab.
2. Tap the top-right **Configure Mode** checkmark button (`✓`).
3. Touch and hold any favorite card, then drag it up, down, left, or right across rows. Surrounding cards will slide out of the way. Release to drop.
4. Tap the checkmark button again to lock in the new layout. Your custom order is automatically saved and persisted across app reinstalls.

---

## 📦 Download & Installation

The latest release and prior versions are available directly in the project repository and on the [OmniDial Website](docs/index.html):

- **[OmniDial-v1.1.1.apk](OmniDial-v1.1.1.apk)** *(Latest Release — Build 3)*: T9 Nickname search, Fav & T9 default keypad layout, automatic international country code resolution for WhatsApp/SMS, Key 1 VM deduplication, and non-jumping quick action buttons.
- **[OmniDial-v1.1.0.apk](OmniDial-v1.1.0.apk)** *(Prior Release — Build 2)*: 2x2 Call Action layout, adaptive preference highlighting, dark mode WhatsApp contrast, car call redirection, and favorites UX updates.
- **[OmniDial-v1.0.0.apk](OmniDial-v1.0.0.apk)** *(Prior Release — Build 1)*: Initial release with Cellular + WhatsApp integration, Caller Rules, T9 search, and Flip-to-Shhh.
- **[OmniDial.apk](OmniDial.apk)** *(Latest build direct alias)*.

---

## 🔒 Permissions & Security

OmniDial requires standard telephony permissions to operate as your default phone handler:
- `READ_CONTACTS` & `WRITE_CONTACTS`: Sync contacts, nicknames, and starred favorites.
- `CALL_PHONE` & `MANAGE_OWN_CALLS`: Initiate and manage phone calls via Android Telecom.
- `READ_CALL_LOG` & `WRITE_CALL_LOG`: Display and manage recent call history.
- `ANSWER_PHONE_CALLS`: Auto-answer calls for user-configured automation rules.
- `SEND_SMS`: Send optional auto-response SMS messages for busy/gate rules.

*All user data, call logs, contacts, and automation rules remain 100% private and stored locally on your device in Room SQLite database and SharedPreferences.*
