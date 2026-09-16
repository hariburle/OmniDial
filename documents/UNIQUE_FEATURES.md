# 🌟 OmniDial — Unique Capabilities & Strategic Product Features

This document provides a comprehensive catalog of **OmniDial's unique capabilities, architectural innovations, and competitive differentiators** (compared to standard dialers like Google Phone, Samsung Phone, and Truecaller).

Use these sections to update the official OmniDial website (`index.html`), create landing page feature showcases, write app store listings, or generate product comparison matrices.

---

## 📋 Quick Feature Matrix: OmniDial vs. Standard Dialers

| Feature | Standard Stock Dialer | Truecaller | OmniDial |
| :--- | :---: | :---: | :---: |
| **Dual-Channel Routing (Cellular + WhatsApp)** | ❌ No | ❌ Manual only | ✅ **Native & Automated** |
| **Car Infotainment & Wearable WhatsApp Redirection** | ❌ No | ❌ No | ✅ **Automated via Telecom** |
| **Pre-Answer Audio Routing (Pick Bluetooth/Speaker first)** | ❌ No | ❌ No | ✅ **Built-in InCall HUD** |
| **Gate / Intercom Auto-Answer & DTMF Buzzer** | ❌ No | ❌ No | ✅ **Dedicated Rule Engine** |
| **2x2 Quick Actions for Non-Contacts** | ❌ Call only | ❌ Limited | ✅ **Call, SMS, WA Chat, WA Call** |
| **Roaming-Aware Smart Dual-SIM Routing** | ❌ Manual prompt | ❌ Basic | ✅ **Automated Roaming Avoidance** |
| **Post-Call Notes & Scheduled Callback Reminders** | ❌ No | ❌ No | ✅ **Immediate After-Call Card** |
| **Proximity Sensor Smart Screen-Off (Ear Detection)** | ⚠️ Basic | ⚠️ Basic | ✅ **Intelligent Audio-Route Aware** |
| **Continuous Multi-Row Drag-and-Drop Favorites** | ❌ Static list | ❌ Grid only | ✅ **Interactive Bento/Grid Reordering** |
| **Flip-to-Shhh / Silence on Flip** | ⚠️ Pixel only | ❌ No | ✅ **Universal on any Android** |
| **100% On-Device Privacy (Zero Cloud Uploads)** | ⚠️ Cloud-synced | ❌ Uploads address book | ✅ **Strictly Local & Private** |
| **Cryptographic Tamper-Proof Local Backup** | ❌ Cloud only | ❌ Cloud backup | ✅ **1-Tap Verified Local JSON** |

---

## 💎 Deep-Dive: OmniDial's Unique Feature Portfolio

### 1. 🔀 Intelligent Dual-Channel Calling (Cellular + WhatsApp VoIP)
* **The Capability**: Seamlessly choose or teach preferred calling channels per contact (Cellular Phone vs. WhatsApp) without placing a call.
* **Car & Wearable Redirection**: When driving or using a smartwatch, outgoing calls initiated via Android Auto, Bluetooth car head units, or voice assistants are intercepted by `OmniCallRedirectionService` and redirected to WhatsApp VoIP automatically if configured as preferred.
* **Adaptive Highlight & Dual Buttons**: In "Ask & Learn" mode, Favorite cards and contact sheets show side-by-side action buttons (`Phone` and `WhatsApp`). Once learned, the card automatically adapts with high-contrast preference borders.
* **International Smart Dialing**: Automatically resolves missing international dialing codes for overseas numbers typed on the dial pad.

---

### 2. 🎧 Pre-Answer Audio Route Selector
* **The Capability**: When an incoming call rings, you can choose how you want the call answered **before picking up**:
  - Handset Earpiece
  - Speakerphone
  - Specific paired Bluetooth device (Car head unit, AirPods, Galaxy Buds, hearing aid)
* **User Value**: No more answering on speaker accidentally in public, or scrambling to switch audio to your car headset after the conversation has already begun.

---

### 3. 🤖 Automated Gate & Intercom Buzzer Rules
* **The Capability**: A dedicated automation recipe engine designed for gate buzzers, building intercoms, and delivery gates:
  - **Auto-Answer Delay**: Answers incoming calls from gate numbers after a set delay (e.g. 1 second).
  - **Automated DTMF Key Sequence**: Plays in-band touch tones (e.g., `9#` or `*#`) to unlock the lobby door or gate.
  - **Auto-Hangup & SMS Confirmation**: Disconnects after 2 seconds and can send a confirmation SMS.
  - **Carrier Spam Bypass**: Automatically whitelists your gate and intercom numbers so carrier STIR/SHAKEN spam filters never auto-drop your deliveries.

---

### 4. 🎹 2x2 Non-Contact Keypad Action Grid
* **The Capability**: Enter any phone number on the dial pad—even if it is **not saved** in your contacts—and immediately get a stable 2x2 action grid:
  1. **Text Message (SMS)**
  2. **Cellular Phone Call**
  3. **WhatsApp Chat**
  4. **WhatsApp Voice Call**
* **User Value**: Reach businesses, delivery couriers, or temporary contacts over WhatsApp without cluttering your personal address book.

---

### 5. 📶 Dual-SIM Intelligence & Roaming Bill-Shock Protection
* **The Capability**:
  - **Per-Contact & Per-Rule Preferred SIM**: Assign specific SIM cards (SIM 1 or SIM 2) to family, business, or specific area codes.
  - **Live Keypad SIM Toggle**: Switch between SIM 1 and SIM 2 right from the dial pad before tapping call.
  - **Roaming-Aware Routing**: Real-time network roaming detection (`isNetworkRoaming()`) automatically redirects international calls to local eSIMs or VoIP to avoid carrier roaming charges.
  - **Visual SIM Badges**: Call logs display explicit `SIM 1` / `SIM 2` (or `WhatsApp`) badges alongside timestamps and durations.

---

### 6. 📝 Post-Call Notes & Scheduled Callback Reminders
* **The Capability**:
  - **Instant Post-Call Card**: After hanging up, a quick note sheet allows you to record key takeaways or action items while they are fresh.
  - **Scheduled Reminders**: Set a follow-up reminder (15 min, 1 hour, tomorrow morning) that triggers a native Android notification alarm with one-tap redialing.
  - **Persistent Call Log Notes**: Saved notes are permanently attached to the call log record for instant reference.

---

### 7. 👂 Proximity Sensor Smart Screen-Off (Ear Detection)
* **The Capability**:
  - Automatically turns off the screen when holding the phone to your ear during active handset/wired calls (`PROXIMITY_SCREEN_OFF_WAKE_LOCK`), preventing accidental cheek presses or muted calls.
  - **Route-Aware Intelligence**: Automatically keeps the screen awake if audio is switched to Speakerphone or Bluetooth hands-free.

---

### 8. ⭐ Continuous Multi-Row Drag-and-Drop VIP Favorites
* **The Capability**:
  - **Smooth Visual Reordering**: Unlock Configure Mode (`✓`) and drag any contact card across 1, 2, 5, or 10 rows. Surrounding cards glide out of the way smoothly.
  - **3 Card Styles**: Choose between **Bento**, **Grid (Quick Action)**, and **Material** card themes in Settings.
  - **1-Tap Speed Dial**: Bind favorites to numeric keypad keys 2–9 for instant long-press dialing.
  - **Nickname Auto-Resolution**: Stars popular contacts with their custom device nicknames to save screen space.

---

### 9. 🤫 "Flip to Shhh" (Silence on Flip Gesture)
* **The Capability**: Uses device accelerometer and proximity sensors to immediately silence an incoming ringing call or activate Do-Not-Disturb (DND) by flipping your phone face down on a table.
* **Universal Support**: Works across all Android devices, bringing the beloved Google Pixel gesture to Samsung, Xiaomi, OnePlus, and Motorola phones.

---

### 10. 🛡️ 100% On-Device Privacy & Truecaller-Grade Spam Defense
* **The Capability**:
  - **Strict Local Privacy**: Your contacts, call history, and rules are stored on-device in Room SQLite. **Zero data is uploaded** to remote third-party cloud servers.
  - **Tiered Caller Trust Badges**: Visual indicators for *Verified Business* (Green), *Delivery / Courier* (Amber), and *High-Risk Spam* (Red).
  - **Carrier STIR/SHAKEN Detection**: Inspects carrier network metadata (`VERIFICATION_STATUS_FAILED`) and warns you if the incoming caller's identity was spoofed.
  - **Auto-Block Presets**: One-tap auto-blocking of top telemarketers, hidden/restricted IDs, and foreign prefix wildcards.

---

### 11. ⏳ Pause (`,`) and Wait (`;`) IVR Dialing Support
* **The Capability**:
  - Long-press `*` key to enter a 2-second **Pause (`,`)**.
  - Long-press `#` key to enter a **Wait (`;`)**.
  - Overflow menu on the number bar to easily insert pauses for navigating automated banking and conference systems.

---

### 12. 🔍 Contextual Discovery Filters & Deep Search
* **The Capability**:
  - **Horizontal Filter Bar**: Filter your address book with one tap: **All**, **Favorites**, **Recents**, **Frequent**, and **Rediscover** (surfacing contacts you haven't spoken to in over 60 days).
  - **Zero-Lag T9 Search**: Real-time T9 matching across full names, initials, nicknames, and digits as you type.
  - **Two-Way Primary Number Sync**: Setting a default number updates both the in-app VIP card and the Android system address book (`ContactsContract`).

---

### 13. 💾 Cryptographic One-Tap Backup & Cross-Device Restore
* **The Capability**:
  - **1-Tap Auto-Backup**: Exports all rules, custom favorite grids, speed dials, and learned channel preferences to JSON without confusing folder pickers.
  - **SHA-256 Checksum Validation**: Every backup includes automated cryptographic integrity verification to prevent corrupted or tampered file restores.
  - **Cross-Device File Import**: Easily transfer your personalized setup when switching to a new Android phone.

---

## 🌐 Website Copy & Marketing Snippets (Ready for `index.html`)

### Hero Tagline
> **OmniDial: The Intelligent Android Dialer That Adapts to How You Actually Communicate.**  
> *Seamlessly unify Cellular & WhatsApp calling, automate gate buzzers, prevent spam, and protect your privacy—100% on-device.*

### 3 Key Pillars for Homepage Cards
1. **Multi-Protocol Calling (GSM + WhatsApp)**  
   *One dialer for cellular calls and WhatsApp voice. Automatically redirects car infotainment and smartwatch calls to your preferred channel.*
2. **Smart Gate & Intercom Automation**  
   *Never miss a delivery or get locked out. OmniDial auto-answers apartment gates, enters your buzzer DTMF code, and hangs up automatically.*
3. **True Privacy & Dual-SIM Mastery**  
   *No account required. No contact book uploads. Full roaming protection, custom multi-row drag-and-drop favorites, and tactile haptic feedback.*
