# 🌐 Multi-Channel Calling Engine (MCCE) — Architectural Blueprint

This blueprint defines the architecture, dynamic channel discovery, decision hierarchy, and user experience for **OmniDial's Unified Multi-Channel Calling Engine**. It establishes a pluggable, extensible foundation initially verified on **Cellular (SIM 1 / SIM 2)** and **WhatsApp (Personal)**, designed for zero-refactor expansion to **WhatsApp Business**, **Google Voice**, and future VoIP providers.

---

## 1. 🎯 Strategic Vision & Core Principles

Modern phone users manage communication across multiple identities: personal phone numbers, business lines, international VoIP, and messaging-first channels. Traditional dialers lock users into a rigid cellular-only model.

**OmniDial's Multi-Channel Engine** decouples phone numbers from the physical transport layer:
1. **Granular Per-Phone-Number Preferences**: Preferences are bound strictly at the individual phone number level (`normalized_number`), not just the high-level contact. A contact with 3 phone numbers (e.g., Mobile, Office, Home) can have completely independent channel preferences for each number.
2. **Pluggable & Extensible Architecture**: The engine is built using polymorphic channel abstractions (`CallingChannel`, `ChannelDiscoveryManager`, `ChannelDispatchCoordinator`). We implement and verify it end-to-end on **Cellular** and **WhatsApp** first, ensuring that adding WhatsApp Business or Google Voice requires zero changes to the UI shell or database architecture.
3. **Zero-Friction Discovery**: Automatically detect active cellular SIMs and installed VoIP apps on the device without manual setup.
4. **Tactile & Visual Clarity**: Distinct, branded visual indicators across the Keypad, Contacts, Recents, and In-Call screens so users always know which channel is dialing.

---

## 2. 🏛️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            COMPOSE PRESENTATION LAYER                       │
│   Keypad Channel Dock  •  Contact Details Selector  •  Smart Channel Picker │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   ResolveCallingChannelUseCase (Decision Pipeline)          │
│   1. Automation Rules  ➔  2. Per-Number Pin  ➔  3. Smart Learn  ➔  4. Default │
└──────────────────────┬───────────────────────────────┬──────────────────────┘
                       │                               │
                       ▼                               ▼
┌──────────────────────────────────────┐   ┌──────────────────────────────────┐
│       ChannelDiscoveryManager        │   │     ChannelPreferenceRepository  │
│  • SubscriptionManager (SIM 1/2)     │   │  • Per-Number Channel Preference │
│  • PackageManager (Extensible VoIP)  │   │    (@PrimaryKey normalizedNumber)│
│  • TelecomManager (Calling Accounts) │   │  • Historical calling mode stats │
└──────────────────────┬───────────────┘   └──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     ChannelDispatchCoordinator (Execution)                  │
│   • TelecomManager.placeCall()  • WhatsApp VoIP Intent  • Extensible Plugins│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 🧩 Domain Models & Per-Phone-Number Storage

### A. The Pluggable `CallingChannel` Hierarchy
```kotlin
sealed interface CallingChannel {
    val id: String                  // e.g. "sim_1", "sim_2", "whatsapp", "whatsapp_business", "google_voice"
    val displayName: String          // e.g. "SIM 1 (Verizon)", "WhatsApp", "Google Voice"
    val shortLabel: String           // e.g. "SIM 1", "WA", "WA Biz", "G Voice"
    val brandColorHex: Long          // Color accent for pills and badges
    val isAvailable: Boolean         // Dynamically checked based on hardware/package state
    val category: ChannelCategory    // CELLULAR, MESSAGING_VOIP, CLOUD_VOIP

    // Phase 13 (Core Foundation)
    data class CellularSim(
        val slotIndex: Int,          // 0 = SIM 1, 1 = SIM 2
        val subscriptionId: Int,
        val carrierName: String,
        val isRoaming: Boolean,
        override val isAvailable: Boolean = true
    ) : CallingChannel

    data class WhatsApp(
        val isBusiness: Boolean = false, // false = personal, true = WhatsApp Business
        val packageName: String = "com.whatsapp",
        override val isAvailable: Boolean
    ) : CallingChannel

    object SystemDefault : CallingChannel

    // Phase 14 (Plug-and-Play Extensibility)
    data class GoogleVoice(
        val packageName: String = "com.google.android.apps.googlevoice",
        val phoneAccountHandle: PhoneAccountHandle? = null,
        override val isAvailable: Boolean
    ) : CallingChannel
}

enum class ChannelCategory {
    CELLULAR,
    MESSAGING_VOIP,
    CLOUD_VOIP
}
```

### B. Granular Per-Phone-Number Database Entity
Preferences are explicitly keyed by the E.164 `normalized_number`, guaranteeing that each number associated with a single contact maintains its own dedicated channel choice:

```kotlin
@Immutable
@Entity(
    tableName = "number_channel_preferences",
    indices = [Index(value = ["normalized_number"])]
)
data class NumberChannelPreference(
    @PrimaryKey
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String,     // E.164 formatted number (e.g. "+15551234567")

    @ColumnInfo(name = "preferred_channel_id")
    val preferredChannelId: String,   // "system" | "sim_1" | "sim_2" | "whatsapp" | "whatsapp_business" | "google_voice" | "ask"

    @ColumnInfo(name = "custom_label")
    val customLabel: String? = null,  // e.g. "Work", "Mobile", "WhatsApp Line"

    @ColumnInfo(name = "updated_timestamp")
    val updatedTimestamp: Long = System.currentTimeMillis()
)
```

---

## 4. 🔍 Dynamic Channel Discovery (`ChannelDiscoveryManager`)

Instead of hardcoded booleans, `ChannelDiscoveryManager` observes Android system services and emits a reactive `StateFlow<List<CallingChannel>>`:

1. **Cellular Channels**:
   - Queries `SubscriptionManager.getActiveSubscriptionInfoList()`.
   - Detects dual-SIM slots, carrier names, and real-time roaming status.
2. **WhatsApp (Personal)**:
   - Queries `PackageManager.getPackageInfo("com.whatsapp", 0)` to verify presence.
   - Verifies if the phone number has indexed VoIP MIME data in `ContactsContract.Data`:
     - `vnd.android.cursor.item/vnd.com.whatsapp.voip.call`
3. **Pluggable Extension Points**:
   - Designed with an open `ChannelPlugin` interface so scanning `com.whatsapp.w4b` (Business) and `com.google.android.apps.googlevoice` simply plugs into the discovery pipeline without altering the core state machine.

---

## 5. 🧠 The 4-Tier Resolution Pipeline (`ResolveCallingChannelUseCase`)

When placing a call to any phone number, the system determines the transport channel using an intelligent fallback pipeline evaluated **per phone number**:

```
[ OUTGOING CALL REQUEST (Normalized Number, Contact) ]
                           │
                           ▼
  1. Automated Recipe Rule Match? ──────YES────➔ [ Apply Rule Channel (e.g. Roaming ➔ WhatsApp) ]
                           │ NO
                           ▼
  2. Per-Number Pinned Preference? ─────YES────➔ [ Apply Number's Pinned Channel (e.g. +1...99 ➔ SIM 2) ]
                           │ NO
                           ▼
  3. Learned History Bias (≥2 calls)? ──YES────➔ [ Suggest / Pre-Select Learned Channel ]
                           │ NO
                           ▼
  4. Global Strategy Mode:
     ├─ "Always Ask" ──────────────────────────➔ [ Show Smart Channel Picker Bottom Sheet ]
     ├─ "Cellular Preferred" ──────────────────➔ [ Use Default Cellular SIM ]
     └─ "Last Used Channel" ───────────────────➔ [ Use Most Recent Calling Channel ]
```

---

## 6. 🎨 User Experience & Interaction Design

### A. Keypad Channel Dock & Dual Action Execution
- Positioned seamlessly directly above the Keypad / Call action row.
- Replaces legacy redundant SIM switcher rows with a clean segmented channel dock:
  `[ SIM 1 (Carrier) ]` `[ SIM 2 (Carrier) ]` `[ WhatsApp ]` *(extensible to `[ WA Business ]` `[ Google Voice ]`)*
- Directly below the keypad, provides a balanced 3-column action row:
  - **Left**: `[ 💬 Message ]` — Dispatches an SMS (if SIM 1/SIM 2 active) or opens WhatsApp chat (if WhatsApp active).
  - **Center**: Hero green `[ 📞 Call ]` button — Places a voice call on the currently selected active channel.
  - **Right**: `[ ⌫ Backspace ]` button.
- Typing an international dialing prefix (`+`, `011`, `00`) automatically pre-selects `[WhatsApp]` to prevent accidental carrier toll fees.

### B. Favorites Section (Option B — Voice-First Execution)
- VIP Favorites focus strictly on instant voice calling:
  - **Tapping anywhere on the Card Body**: Opens `ContactDetailsBottomSheet` to view numbers, call history, or initiate alternate actions.
  - **Call Button Action**:
    - **Known Preference**: Displays `Call - <Channel Name>` (e.g. `Call - Jio`, `Call - WhatsApp`). 1-tap immediately dials via that channel.
    - **Unknown Preference**: Displays `Call`. 1-tap presents a lightweight 2-option sheet: *"Call via [SIM 1 / Carrier] or [WhatsApp]? [x] Remember this choice"*. Saving preference immediately updates the button.

### C. Contact Details & Per-Number Channel Chips
- In `ContactDetailsBottomSheet`, each phone number row displays its own independent channel chips:
  - Phone Number 1 (+1 555-0100): `[ Cellular (SIM 1) ]` `[ WhatsApp ]` `[ Always Ask ]`
  - Phone Number 2 (+1 555-0199): `[ Cellular (SIM 2) ]` `[ WhatsApp ]` `[ Always Ask ]`
- Changing the channel on Number 1 does **not** affect Number 2.
- Direct message and video call actions are accessible upon expanding the number.

### D. Recents / Call Log Channel-Faithful Redial
- Tapping a call log entry **redials on the exact channel and SIM slot** that the call occurred on (e.g. missed call on SIM 2 redials via SIM 2; WhatsApp call redials via WhatsApp).
- Tapping the contact avatar or info icon opens `ContactDetailsBottomSheet`.

### E. Safety Guardrails: Intelligent Country Emergency Discovery
- Real-time cell tower detection via `TelephonyManager.isEmergencyNumber()` (API 29+) and `PhoneNumberUtils.isEmergencyNumber()`.
- Automatically adapts to the country the user is in (`911` in US, `112/100/108` in India, `999` in UK, `000` in Australia, etc.).
- When an emergency number is detected:
  - VoIP and WhatsApp channels are forcefully hidden and disabled.
  - Outgoing call is locked to an active, domestic, non-roaming cellular SIM.
  - Dispatched directly through native Android Telecom emergency calling stack.

---

## 7. 🚀 Phased Implementation Roadmap

### 📦 Phase 13 (Current Focus): Core MCCE Foundation (Cellular & WhatsApp)
*Build the complete multi-channel architecture and verify end-to-end on Cellular SIM 1/2 and WhatsApp.*
- **Task 13.1: Granular Per-Number Channel Preference Entity & Repository**
  - Create Room entity `number_channel_preferences` keyed by E.164 `normalized_number`.
  - Migrate legacy split `contact_sim_preferences` and SharedPreferences `learned_call_modes` into unified `ChannelPreferenceRepository`.
- **Task 13.2: Dynamic Channel Discovery Service (`ChannelDiscoveryManager`)**
  - Implement reactive `ChannelDiscoveryManager` detecting active SIM 1, SIM 2, and WhatsApp.
  - Implement `CallingChannel` domain models with extensible pluggable design.
- **Task 13.3: Per-Phone-Number Channel Selector in Contact Details**
  - In `ContactDetailsBottomSheet`, render independent channel chip selectors for each individual phone number.
  - Store and observe per-number channel bindings in real time.
- **Task 13.4: Adaptive Keypad Channel Dock & Dispatch Coordinator**
  - Implement `KeypadChannelDock` above the dial pad on `DialerScreen`.
  - Implement `ChannelDispatchCoordinator` executing calls through Telecom Cellular or WhatsApp VoIP.
  - Automated unit tests validating per-number resolution and dispatch.

### 🌟 Phase 14 (Future Extension): Pluggable VoIP Expansion (WhatsApp Business & Google Voice)
*Leverage the MCCE foundation to plug in additional communication providers.*
- **Task 14.1: WhatsApp Business Provider Plugin**
  - Add `com.whatsapp.w4b` detection in `ChannelDiscoveryManager`.
  - Route direct VoIP calls via `vnd.android.cursor.item/vnd.com.whatsapp.w4b.voip.call` in `ChannelDispatchCoordinator`.
- **Task 14.2: Google Voice Provider Plugin**
  - Add `com.google.android.apps.googlevoice` detection and Telecom calling account integration.
  - Support direct Google Voice intent dispatch and carrier shadow-routing.
