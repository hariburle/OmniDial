# ⚡ OmniDial Automation Engine — IFTTT, Geofencing & Smart Call Recipes Blueprint

This blueprint defines the architecture, trigger models, UX design, and competitive feature set for **OmniDial's Context-Aware Automation Engine**. It draws inspiration from industry-leading automation tools (**Apple Shortcuts**, **Tasker**, **Samsung Routines**, **Google Pixel Assistant**, and **IFTTT**).

---

## 1. 🌐 Industry Benchmark & Competitive Landscape

| Feature Category | Apple Shortcuts / Focus | Google Pixel Phone | Samsung Routines / Bixby | Tasker / Macrodroid | **OmniDial Vision** |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Intercom & Gate DTMF** | ❌ No | ❌ No (Direct My Call only) | ❌ No | ⚠️ Complex scripts | 🥇 **Dedicated 1-Tap Pipeline** |
| **Geofenced Call Rules** | ⚠️ Focus mode only | ❌ No | ⚠️ Change sound profile | ⚠️ Requires root/extra plugins | 🥇 **Location-Guarded Actions** |
| **Wi-Fi / BT Context Triggers** | ⚠️ Limited prompts | ❌ No | ⚠️ Sound profile only | ⚠️ High battery / setup complexity | 🥇 **Zero-Battery Ambient Matching** |
| **Call Channel Switching** | ❌ No | ❌ Cellular only | ❌ Cellular only | ❌ No | 🥇 **Cellular ↔ WhatsApp VoIP** |
| **Simulated Rule Dry-Run** | ❌ No | ❌ No | ❌ No | ❌ No | 🥇 **Built-in In-Call Simulator** |

---

## 2. 🧩 The OmniDial IFTTT Architecture: `Trigger -> Condition -> Action`

Every automation recipe is modeled as a composable pipeline:

```
[ TRIGGER (Event) ] 
       ⬇
[ CONDITIONS (Filters & Context) ]
       ⬇
[ ACTION PIPELINE (Sequenced Execution) ]
       ⬇
[ AUDIT LOG & NOTIFICATION ]
```

### A. Triggers (Events)
1. **Incoming Call Ringing** (`Telephony.INCOMING`)
2. **Call Answered / Connected** (`Call.STATE_ACTIVE`)
3. **Call Disconnected / Missed** (`Call.STATE_DISCONNECTED`)
4. **Outgoing Call Initiated** (`CallRedirectionService`)

### B. Conditions (Contextual Filters)
* **Caller Identity**: Specific contact, Regex pattern, Contact group (VIP, Family, Work), Unsaved/Unknown numbers, Spam score.
* **Geofence & Ambient Presence**:
  * *Wi-Fi SSID*: Connected to designated network (e.g. `Home-5G`, `Office-Guest`) — **0% battery overhead**.
  * *Bluetooth Device*: Connected to specific MAC/Name (e.g., `Car-Infotainment`, `Galaxy-Buds`, `Jabra-Headset`).
  * *Geofence Radius*: Low-power geofencing API (lat/long radius 100m–500m).
* **Time & Scheduling**:
  * *Time Window*: e.g., 09:00 to 17:00.
  * *Day of Week*: Weekdays vs. Weekends.
  * *Calendar Status*: Device calendar marked as "Busy / In Meeting".
* **Hardware & SIM**:
  * *SIM Slot*: Active on SIM 1 (Work) vs. SIM 2 (Personal).
  * *Power State*: On wireless charger / night dock.

### C. Action Pipelines (Step-by-Step)
* **Call Control**: Auto-Answer, Auto-Disconnect, Silence Ringer, Send to Voicemail.
* **Audio Routing**: Force Speakerphone, Force Bluetooth, Mute Microphone.
* **In-Band Telecom**: DTMF Touch-Tone Sequence (`0-9, *, #, , (pause)`).
* **Out-of-Band Messaging**: Send SMS auto-reply, trigger WhatsApp message template.
* **Redirection & Channel**: Forward to cellular number, redirect to WhatsApp VoIP.

---

## 3. 🚀 High-Value Automation Recipes & Scenarios

### Recipe 1: 🏢 Location-Guarded Apartment / Gate Buzzer
* **Problem**: Auto-answering and buzzing `9#` when you're away from home unlocks the front door for unwanted callers or parcel thieves.
* **IF**: Incoming call matches `Apartment Intercom` (+1-555-GATE).
* **CONDITION**: Phone is connected to **Home Wi-Fi** OR inside **Home Geofence**.
* **THEN (At Home)**: 
  1. Auto-answer after 1s.
  2. Mute microphone (prevent background room noise on street speaker).
  3. Send DTMF `9#`.
  4. Auto-hangup after 2s.
* **ELSE (Away)**:
  1. Do not unlock gate.
  2. Send instant SMS: *"I am currently away from home. Please leave the package in the lobby locker."*

---

### Recipe 2: 🚗 In-Car Commute & Driving Assistant
* **IF**: Incoming call from any contact.
* **CONDITION**: Connected to **Car Bluetooth** audio.
* **THEN**:
  1. Announce caller name via TTS over car stereo.
  2. Auto-answer after 3 rings (hands-free compliance).
  3. Force audio route to Bluetooth Head unit.
  4. If caller hangs up before answer: send auto-SMS *"Currently driving, will call you back when parked."*

---

### Recipe 3: 💼 Office Work Mode & Dual-SIM Isolation
* **IF**: Phone enters **Office Geofence** or connects to **Work Wi-Fi**.
* **THEN**:
  1. Set default outgoing calling SIM to **SIM 2 (Work)**.
  2. Divert personal calls from non-family contacts to silent voicemail.
  3. For missed client calls, auto-dispatch a professional WhatsApp or SMS follow-up: *"Thank you for calling. I am currently in a meeting, please leave a brief message."*

---

### Recipe 4: 🌙 Night Dock / Sleep DND Guard with Emergency Override
* **IF**: Phone is plugged into charger between **10:30 PM – 06:30 AM**.
* **CONDITION**: Caller is NOT in **Favorites / VIP List**.
* **THEN**:
  1. Mute call ringtone completely.
  2. If the same caller rings **twice within 3 minutes** (emergency breakthrough), escalate ringtone to 100% volume and vibrate.

---

### Recipe 5: 🤖 Delivery Driver "Direct-to-Voicemail" or Doorcode SMS
* **IF**: Incoming call from known Delivery Dispatchers (Amazon, DoorDash, UberEats patterns).
* **THEN**:
  1. Auto-answer.
  2. Send immediate SMS: *"Delivery pin is 4821. Gate code is #104."*
  3. Play pre-recorded DTMF or disconnect cleanly.

---

### Recipe 6: 📵 Instant Silent Robo-Shield (DTMF Captcha / Challenge)
* **IF**: Incoming call from unsaved number with suspected spam risk score.
* **THEN**:
  1. Auto-answer silently without ringing the handset.
  2. Transmit prompt / tone.
  3. If caller does not press a key or wait 5s, hang up and flag as spam.

---

## 4. 🎨 UX & Interaction Design for the Caller Rules Panel

### 1. Visual Execution Pipeline
Instead of passive text badges, each rule card displays a horizontal sequence indicator:
```
[ 📞 Ringing ] ➔ [ ⏱️ 1.0s Delay ] ➔ [ 🟢 Answer ] ➔ [ 🔇 Mute Mic ] ➔ [ 🔢 DTMF 9# ] ➔ [ 🔴 Hangup ]
```

### 2. Context Badging
Rules display active ambient condition tags:
* `📍 Home Geofence Only`
* `📶 Wi-Fi: "Home_5G"`
* `🕒 9:00 AM – 5:00 PM (Mon-Fri)`
* `💳 SIM 1 Only`

### 3. Quick-Start Recipe Carousel (Zero-State & Low-Density)
On screens with few or no rules, display interactive recipe starter cards:
* **"Apartment Gate Buzzer"** (Auto-Answer + 9# DTMF)
* **"Driving Auto-Responder"** (Car Bluetooth + SMS Auto-Reply)
* **"Office Hours Filter"** (Weekdays 9-5 + Work SIM)
* **"IVR Phone Tree Shortcut"** (Speed dial + automated DTMF extension)

### 4. Built-in Simulation & Test Mode ("Dry Run")
Each rule includes a **"Test Run"** button:
* Opens OmniDial's In-Call HUD in simulated mode.
* Step-by-step toast notifications and visual highlights illustrate each action (Delay ➔ Answer ➔ Tone ➔ Hangup).
* Validates rules safely without requiring a physical telephone call.

---

## 5. 🔋 Technical Feasibility & Battery Optimization

1. **Ambient Geofencing without GPS Polling**:
   * Leveraging `WifiManager.connectionInfo.ssid` and `BluetoothAdapter.getProfileConnectionState()` requires zero GPS hardware power and zero location tracking background drain.
2. **Android Native Geofencing API**:
   * Uses cellular tower and Wi-Fi AP signatures for 100m+ boundaries. GPS hardware is only awakened when near the perimeter transition boundary.
3. **Room Database Schema Extension**:
   * Extend `CallerRule` entity with optional metadata:
     `geofenceLatitude`, `geofenceLongitude`, `geofenceRadiusMeters`, `requiredWifiSsid`, `requiredBluetoothDevice`, `activeDaysMask`, `startTimeMinutes`, `endTimeMinutes`, `muteMic`, `speakerphone`.
