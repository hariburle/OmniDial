# 🧭 Multi-Channel UX & Design Thinking Blueprint

> **Status**: Approved Specification & Implementation Guide  
> **Topic**: Resolving Cognitive Overload in Multi-Channel Communication (Cellular, Dual-SIM, WhatsApp, Messaging, Video)

---

## 1. 🔍 The Core Problem Statement

### From Simple Dialer to Communications Hub
OmniDial started with a crisp, simple proposition:
1. Allow users to choose between **Cellular** and **WhatsApp**.
2. Expand to **Voice Call** vs. **Instant Message**.
3. Extend to seamless **Dual-SIM (SIM 1 / SIM 2)** selection.
4. Scale to extended channels (**WhatsApp Business**, **Google Voice**, **VoIP**).

### The UX Hazard: The "Cockpit Syndrome"
When communication dimensions multiply:
$$\text{Channels (SIM 1, SIM 2, WhatsApp, WA Business)} \times \text{Modalities (Voice, Video, Chat, SMS)}$$
Presenting every choice on every screen risks transforming a fast, zero-friction 1-tap phone dialer into a cluttered, paralyzing aircraft cockpit with duplicate SIM switchers and overlapping buttons.

> [!IMPORTANT]
> **A phone dialer is an execution tool, not a browsing portal.**  
> When a user taps a contact, they expect an immediate, predictable connection. Introducing decision prompts at the moment of dialing destroys muscle memory and induces hesitation.

---

## 2. 👥 Approved UX Architecture Across Panels

### A. Favorites Section: Voice-First Execution
Favorites is where users quickly connect with VIPs over voice.

```
┌──────────────────────────────────────────────────┐
│ [Avatar]   Samanvitha Burle                      │
│            Mobile • +1 (571) 488-4823            │
│                                                  │
│   [ 📞 Call - WhatsApp ]  or  [ 📞 Call - Jio ]  │ <-- Action Button
└──────────────────────────────────────────────────┘
```

1. **Card Body Tap (Anywhere outside button)**:
   - Immediately opens `ContactDetailsBottomSheet` for that contact.
   - User can expand phone numbers, review call history, switch channels, or initiate messages.
2. **Call Button Action (Option B — Approved)**:
   - **Known Channel**: Button reads **`Call - <Channel Name>`** (e.g. `Call - Jio`, `Call - Airtel`, `Call - WhatsApp`). One tap places the call immediately via that channel.
   - **Unknown Channel**: Button reads **`Call`**.
     - First tap surfaces a lightweight 2-option sheet: *"Call via [SIM 1 / Carrier] or [WhatsApp]? [x] Remember this choice"*.
     - Once selected, preference is saved to Room DB and the button updates dynamically to `Call - <Channel Name>`.

---

### B. Keypad Panel: Unified Channel Dock + Dual Actions
Eliminates the redundant "SIM switcher row" by elevating cellular SIMs and VoIP into a unified **Channel Selector**.

```
┌─────────────────────────────────────────────────────────┐
│  [ SIM 1 (Jio) ]     [ SIM 2 (Airtel) ]    [ WhatsApp ] │ <-- Unified Channel Dock
├─────────────────────────────────────────────────────────┤
│                                                         │
│                   [ 📞 CALL ]    [ 💬 MESSAGE ]          │ <-- Clean Action Pair
└─────────────────────────────────────────────────────────┘
```

1. **Unified Channel Dock**:
   - Single row of segmented channel pills directly above Call/Message buttons.
   - Preselects the number's bound preference (or learned habit).
   - Tapping any pill (`[Jio]`, `[Airtel]`, `[WhatsApp]`) dynamically changes the active dispatch target.
2. **Action Buttons**:
   - **`[ CALL ]`**: Executes a voice call on the selected channel.
   - **`[ MESSAGE ]`**: Dispatches an SMS (if SIM 1/SIM 2 selected) or opens WhatsApp chat (if WhatsApp selected).
3. **International Number Auto-Recommendation**:
   - Typing an international dialing prefix (`+`, `011`, `00`, or foreign country code) automatically highlights/preselects `[WhatsApp]` to protect the user from unexpected carrier toll rates.

---

### C. Contacts Panel: Structured Discovery
1. Searching or browsing shows clean contact list items.
2. Tapping any contact opens `ContactDetailsBottomSheet`.
3. In `ContactDetailsBottomSheet`, expanding a phone number reveals direct actions:
   - **Call**: `[SIM 1]`, `[SIM 2]`, `[WhatsApp Voice]`, `[WhatsApp Video]`.
   - **Message**: `[SMS]`, `[WhatsApp Chat]`.
   - **Set Default Channel**: Sets the permanent hero channel for that specific phone number.

---

### D. Recents / Call Log: Channel-Faithful Redial
1. Tapping a call log entry **redials on the exact channel that the call occurred on** (e.g. missed call on SIM 2 redials via SIM 2; WhatsApp call redials via WhatsApp).
2. Tapping the contact avatar or info icon opens `ContactDetailsBottomSheet`.

---

### E. Safety Guardrails: Intelligent Country Emergency Discovery
Dialing emergency numbers must never fail or be misrouted to VoIP.

1. **Intelligent Real-Time Discovery (`TelephonyManager.isEmergencyNumber`)**:
   - In Android 10+ (API 29+), `telephonyManager.isEmergencyNumber(number)` queries the active cellular cell tower and SIM database in real time.
   - Intelligently recognizes `911` in the US, `112/100/108` in India, `999` in the UK, `000` in Australia, etc., matching the exact country the user is currently located in.
2. **Emergency Routing Strategy**:
   - When an emergency number is detected:
     - All VoIP/WhatsApp channels are forcefully hidden and disabled.
     - The dispatch coordinator selects an **active, domestic, non-roaming cellular SIM** (or the system default emergency phone account).
     - Places the call immediately through Android's native Telecom emergency stack.

---

## 3. 📊 Comparison: Before vs. After

| Feature | Prior Architecture | Approved Unified Architecture |
| :--- | :--- | :--- |
| **Keypad SIM Selector** | Separate SIM Switcher row + separate channel dialogs. | **Unified Channel Dock** (`[SIM 1]`, `[SIM 2]`, `[WhatsApp]`) + `[Call]` + `[Message]`. |
| **Favorite Card Tap** | Complex long-press and multi-button confusion. | **Card Body** $\rightarrow$ Contact Details.<br>**Call Button** $\rightarrow$ `Call` or `Call - <Channel>`. |
| **Favorite First-Run** | Ambiguous cellular fallback. | Option B prompt: asks once and locks preference. |
| **Emergency Numbers** | Hardcoded list or standard intent. | Real-time cell tower detection via `isEmergencyNumber()` locked to domestic SIM. |
| **Recents Redial** | Often defaulted to global cellular preference. | Exact channel & SIM redial matching the call event. |
