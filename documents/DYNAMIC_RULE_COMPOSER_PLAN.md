# Architectural Design: Dynamic `@` Category Slot Rule Composer & Dual-Profile Preferences

## Status
* **Status**: Planned / Under Review
* **Target Version**: Phase 15 (v1.3.0 / v1.4.0)
* **Author / Collaborators**: User & Antigravity

---

## 1. Executive Summary

This architecture expands OmniDial's telecom routing engine into an expressive, natural-language rule environment paired with dual-profile preference management (Home vs. Roaming).

### Key Pillars
1. **Dynamic `@` Category Slots**: Rather than hardcoding specific keywords, users type category triggers (`@channel`, `@location`, `@numbers`, `@guard`) that display live, interactive in-place chip pickers reflecting the device's actual hardware and installed apps.
2. **Dual-Profile Partitioning**: `NumberChannelPreference` in Room DB stores preferences under a `profileContext` (`"home"` vs. `"roaming"`), ensuring "Ask & Learn" adapts independently while traveling without corrupting baseline home preferences.
3. **Synergistic Foundation**: Built on top of the newly implemented `TravelRoamingManager` and `ChannelDispatchCoordinator`. The existing travel logic serves as the out-of-the-box baseline rule; custom user rules sit atop the pipeline with higher priority.
4. **Last-Mile Roaming Safety Intercept**: An absolute barrier before placing any cellular call on a roaming SIM, prompting the user with estimated tariff warnings and free VoIP (WhatsApp Business) or local SIM alternatives.

---

## 2. Dynamic `@` Category Slot Architecture

### In-Composer Interaction Model
When the user types any `@` trigger in the rule composer, a contextual floating popup appears:

```
"When @location: Roaming, route @numbers: US (+1) via @channel|"
┌────────────────────────────────────────────────────────┐
│ 📱 Discovered Channels:                                 │
│   • 🟢 WhatsApp Business (US #)                        │
│   • 🟢 WhatsApp (Personal)                             │
│   • 📶 SIM 1 (Spectrum Mobile • US • Roaming)          │
│   • 📶 SIM 2 (Airtel • India • Domestic)               │
│   • 🔵 Google Voice                                    │
│   • ❓ Ask Always Prompt                               │
└────────────────────────────────────────────────────────┘
```

Selecting an option inserts a structured, styled token (e.g., `[@channel: WhatsApp Business]`).

### Dynamic Category Slots Definition

| Trigger | Description | In-Place Picker Options | Backing Engine Source |
| :--- | :--- | :--- | :--- |
| **`@channel`** | Target calling transport | • WhatsApp Business (`com.whatsapp.w4b`)<br>• WhatsApp Personal (`com.whatsapp`)<br>• SIM 1 (`carrier` • `isRoaming`)<br>• SIM 2 (`carrier` • `isRoaming`)<br>• Google Voice<br>• Ask Every Time | `ChannelDiscoveryManager.availableChannels` |
| **`@location`** | Device physical/network location | • `Home Region` (`US`)<br>• `Roaming Abroad` (`India` / Foreign Carrier)<br>• `Any Roaming Network`<br>• `Airplane Mode (Wi-Fi Calling)` | `TravelRoamingManager.getCurrentCountryIso()` & `SimInfo.isRoaming` |
| **`@numbers`** | Destination group or prefix | • `US Numbers (+1)`<br>• `India Numbers (+91)`<br>• `All International`<br>• `Favorites Only`<br>• `All Contacts`<br>• `Specific Contact...` | `PhoneNumberNormalizer` & `ContactHelper` |
| **`@guard`** | Pre-call tariff protection | • `Confirm If Roaming Charges Apply`<br>• `Warn On ISD Rates`<br>• `Silent Auto-Dispatch` | Telecom Tariff Intercept Guard |

---

## 3. Dual-Profile "Ask & Learn" (Home vs. Roaming)

### Schema Evolution (`number_channel_preferences`)
```sql
CREATE TABLE number_channel_preferences (
    normalizedNumber TEXT NOT NULL,
    profileContext TEXT NOT NULL DEFAULT 'home', -- 'home' | 'roaming'
    preferredChannelId TEXT NOT NULL,
    updatedAt INTEGER NOT NULL,
    PRIMARY KEY (normalizedNumber, profileContext)
);
```

### Context Resolution
* **In the US (`networkCountryIso == "us"` and non-roaming)**:
  * "Ask & Learn" prompts update `profileContext = 'home'`.
  * Outgoing calls resolve using the `home` profile.
* **In India (`networkCountryIso == "in"` or roaming)**:
  * "Ask & Learn" prompts update `profileContext = 'roaming'`.
  * Outgoing calls resolve using the `roaming` profile.
* **Returning to US**:
  * Switching back to `home` profile is instantaneous with zero manual synchronization or backup restoration.

---

## 4. Last-Mile Roaming Safety Intercept

Regardless of whether a call originates from a custom rule, a learned preference, speed dial, or an external dialer (Android Auto):
* If the selected transport is a **Cellular SIM** AND `SimInfo.isRoaming == true`:
* OmniDial blocks immediate dispatch and displays a warning dialog:
  > **⚠️ Roaming Cellular Warning**  
  > *SIM 1 (Spectrum Mobile) is actively roaming. Carrier roaming rates ($2.00+/min) may apply.*  
  > `[🟢 Use WhatsApp Business (US #) - Free]`  
  > `[📱 Use SIM 2 (Airtel Domestic)]`  
  > `[⚠️ Proceed on Roaming SIM]`

---

## 5. Implementation Roadmap (Phased)

* [ ] **Phase 1: Dual-Profile Preference Schema**: Update `NumberChannelPreference` entity to composite key `(normalizedNumber, profileContext)`.
* [ ] **Phase 2: Last-Mile Roaming Tariff Intercept**: Implement pre-call confirmation dialog in `MainViewModel` and `InCallActivity`.
* [ ] **Phase 3: Free-Text Rule Composer UI**: Implement rich text field in `RulesScreen` with `@` slot auto-complete anchored dropdown.
* [ ] **Phase 4: Rule Pipeline Integration**: Wire rule evaluation into `ChannelDispatchCoordinator.resolveChannel()`.
