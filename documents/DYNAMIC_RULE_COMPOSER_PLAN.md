# Architectural Design: Dynamic `@` Category Slot Rule Composer & Dual-Profile Preferences

## Status
* **Status**: Planned / Under Review
* **Target Version**: Phase 15 (v1.3.0 / v1.4.0)
* **Author / Collaborators**: User & Antigravity

---

## 1. Executive Summary

This architecture expands OmniDial's telecom routing engine into an expressive, natural-language rule environment paired with multi-profile preference management (Home vs. Roaming vs. International Relocation).

### Key Pillars
1. **Dynamic `@` Category Slots**: Rather than hardcoding specific keywords, users type category triggers (`@channel`, `@location`, `@numbers`, `@guard`) that display live, interactive in-place chip pickers reflecting the device's actual hardware and installed apps.
2. **Country-Scoped Dual-Profile Partitioning**: `NumberChannelPreference` in Room DB stores preferences under a country-scoped `profileContext` (`home:US`, `travel:IN`, `travel:GB`), ensuring "Ask & Learn" adapts independently while traveling without corrupting baseline home preferences.
3. **First-Install Confirmation (No Assumptions)**: Explicitly confirms Home Region and Domestic SIM during initial onboarding to prevent errors if the app is installed while already abroad.
4. **Permanent Relocation Assistant**: Seamlessly handles moving permanently to another country by allowing users to promote travel preferences to their new home profile while archiving previous country profiles.
5. **Dynamic Runtime Evaluation (No Batch DB Mutation)**: Rules are evaluated dynamically before call dispatch. They cover unsaved and future numbers automatically, avoid database clutter, and allow instant rule toggling without database rollbacks.
6. **Last-Mile Roaming Safety Intercept**: An absolute barrier before placing any cellular call on a roaming SIM, prompting the user with estimated tariff warnings and free VoIP (WhatsApp Business) or local SIM alternatives.

---

## 2. First-Install Confirmation & Onboarding

### The Problem With Silent Hardware Detection
If a US resident installs OmniDial while already standing in an airport in Mumbai or a hotel in Delhi:
* The phone's cell tower reports `networkCountryIso == "in"`.
* If the app silently assumed "current network = home", it would mistakenly treat India as "Home" and the US as "Roaming".

### The Solution: First-Install Home Region Setup
During initial setup (and available anytime under *Settings → Channel Preferences → Home Region*):
```
┌────────────────────────────────────────────────────────┐
│ 🌍 Confirm Your Home Region & Primary Domestic Line    │
├────────────────────────────────────────────────────────┤
│ Detected SIM Cards:                                    │
│   🔘 United States — SIM 1 (Spectrum Mobile • +1)     │
│   ⚪ India — SIM 2 (Airtel • +91)                     │
│   ⚪ Other / Custom Country...                         │
│                                                        │
│ [ Confirm Home Region ]                                │
└────────────────────────────────────────────────────────┘
```

* If installed in India, the user selects **United States**.
* OmniDial recognizes: *Home is US, but current connection is India (Roaming)*.
* It activates the `travel:IN` profile immediately without misclassifying your home country.

---

## 3. Country-Scoped Profiles & Relocation

### Schema Evolution (`number_channel_preferences`)
```sql
CREATE TABLE number_channel_preferences (
    normalizedNumber TEXT NOT NULL,
    profileContext TEXT NOT NULL DEFAULT 'home:US', -- e.g. 'home:US', 'travel:IN', 'travel:GB'
    preferredChannelId TEXT NOT NULL,
    updatedAt INTEGER NOT NULL,
    PRIMARY KEY (normalizedNumber, profileContext)
);
```

### Profile Context Matrix
* **Physical US network + US SIM non-roaming**: Resolves and learns under `home:US`.
* **Physical India network or US SIM roaming**: Resolves and learns under `travel:IN`.
* **Third country (e.g. UK vacation)**: Resolves and learns under `travel:GB`.

### Relocation Assistant (Moving Abroad Permanently)
When a user permanently relocates from the US to India and updates their Home Country in Settings:
OmniDial presents the **Relocation Assistant**:
1. **🔄 Promote Travel to Home (Recommended)**: Promotes preferences learned in India (`travel:IN`) to become the new `home:IN` baseline.
2. **🆕 Start Fresh Home Profile**: Keeps `home:US` archived and starts clean for `home:IN`.
3. **Cancel / Extended Vacation**: Retains US as home.

---

## 4. Dynamic `@` Category Slot Rule Composer

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

## 5. Runtime Rule Evaluation Hierarchy

Rules do **not** run batch database writes across thousands of contact records. Instead, they are evaluated dynamically at call time:

```
                                  [ Outgoing Call Initiated ]
                                               │
                                               ▼
                              ┌──────────────────────────────────┐
                              │ Tier 1: Explicit Number Pin?     │
                              │ (Individual Contact Override)    │
                              └────────────────┬─────────────────┘
                                               │ No
                                               ▼
                              ┌──────────────────────────────────┐
                              │ Tier 2: Custom User Rules Match? │
                              │ (Dynamic @ Composer Rules)       │
                              └────────────────┬─────────────────┘
                                               │ No
                                               ▼
                              ┌──────────────────────────────────┐
                              │ Tier 3: Built-in Travel Baseline │
                              │ (TravelRoamingManager Safeguard) │
                              └────────────────┬─────────────────┘
                                               │
                                               ▼
                              ┌──────────────────────────────────┐
                              │ Last-Mile Roaming Safety Intercept│
                              │ (Is selected cellular roaming?)   │
                              └──────────────────────────────────┘
```

* **Covers Unsaved / Keypad Numbers**: Any `+1` number typed on the keypad is instantly protected, even if not in contacts.
* **Instant Reversibility**: Modifying or turning off a rule takes effect immediately with zero database rollback needed.

---

## 6. Last-Mile Roaming Safety Intercept

Regardless of whether a call originates from a custom rule, a learned preference, speed dial, or an external dialer (Android Auto):
* If the selected transport is a **Cellular SIM** AND `SimInfo.isRoaming == true`:
* OmniDial blocks immediate dispatch and displays a warning dialog:
  > **⚠️ Roaming Cellular Warning**  
  > *SIM 1 (Spectrum Mobile) is actively roaming. Carrier roaming rates ($2.00+/min) may apply.*  
  > `[🟢 Use WhatsApp Business (US #) - Free]`  
  > `[📱 Use SIM 2 (Airtel Domestic)]`  
  > `[⚠️ Proceed on Roaming SIM]`

---

## 7. Implementation Roadmap (Phased)

* [ ] **Phase 1: First-Install Home Region Confirmation**: Add Home Country and domestic SIM selector to the setup flow and Settings.
* [ ] **Phase 2: Country-Scoped Preference Schema**: Upgrade `NumberChannelPreference` to composite key `(normalizedNumber, profileContext)`.
* [ ] **Phase 3: Last-Mile Roaming Tariff Intercept**: Implement pre-call warning dialog in `MainViewModel` and `InCallActivity`.
* [ ] **Phase 4: Free-Text Rule Composer UI**: Implement rich text field in `RulesScreen` with `@` slot auto-complete anchored dropdown.
* [ ] **Phase 5: Relocation Assistant**: Add profile migration flow when changing Home Country in Settings.
