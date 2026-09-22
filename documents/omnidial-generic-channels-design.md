# OmniDial — Generic Calling Channels: Requirement & Design

**Status:** Requirement + design approach. No code changed. Written 2026-09-21.
**Repo state at time of research:** `origin/main` @ `2e86138`.

---

## 1. Requirement (Hari, 2026-09-21)

OmniDial hardcodes its non-cellular calling channels: WhatsApp, WhatsApp Business,
and Google Voice. Channel discovery should be **generic and extendable**, not a list
of hardcoded package names — so the app stays resilient in countries/markets where
people use different tools (Telegram, Signal, Viber, regional VoIP apps, SIP providers).

In Hari's words: *"If this is discoverable on a phone why not make it generic and
extendable and not hardcode it... I would like OmniDial to be resilient and handle
these scenarios."*

---

## 2. What's hardcoded today (inventory)

| Hardcoding | Location |
|---|---|
| Package names `com.whatsapp`, `com.whatsapp.w4b`, `com.google.android.apps.googlevoice` probed via `isPackageInstalled()` | `telecom/ChannelDiscoveryManager.kt` (`refreshChannels`, steps 2–4) |
| Channel types as distinct classes (`WhatsApp`, `GoogleVoice`) in the sealed interface | `domain/model/CallingChannel.kt` |
| `when`/type-checks on channel types — 69 sites across 9 files | `ui/components/*` (KeypadChannelDock, CallChoiceDialogs, ChannelSetupDialog, ContactDetailsBottomSheet, FavoriteGridCard, PopularGridCard), `ui/screens/DialerScreen.kt`, `ui/screens/FavoritesScreen.kt`, `telecom/ChannelDispatchCoordinator.kt` |
| WhatsApp call recipe: contacts-provider MIME lookup (`vnd.android.cursor.item/vnd.com.whatsapp.voip.call`) + targeted `ACTION_VIEW` intent, URL-scheme fallback | `util/ContactHelper.kt:486` (`launchWhatsAppCall`) |
| Stored channel ids `"whatsapp"`, `"whatsapp_business"`, `"google_voice"` | `number_channel_preferences`, `ChannelConfig` |

Notes:
- `ChannelDispatchCoordinator.kt` is **dead code** (zero references outside its own file) — not a foundation, ignore it.
- `TelecomVoipHelper.kt` is about Android 14 self-managed VoIP audio routing — unrelated to channel discovery.
- Google Voice discovery *already* queries `TelecomManager.callCapablePhoneAccounts` — then filters the result down to packages containing "googlevoice"/"voice". The generic query exists; the filter is the hardcoding.

---

## 3. Key finding: discovery CAN be generic, call initiation CANNOT

**Discovery can be generic.** Android provides `TelecomManager.callCapablePhoneAccounts` —
every VoIP app that integrates with the system telecom stack (Google Voice, SIP apps)
appears here automatically with no package-name list. Installed messaging apps can
additionally be probed via `PackageManager` for dial/VoIP capabilities.

**Call initiation cannot be fully generic — platform limitation, not a code smell.**
There is no Android API for "start a VoIP call inside app X." Each app exposes a
proprietary recipe (WhatsApp's MIME-type + intent dance above is the proof). So a new
app like Telegram will always need its call recipe researched once and registered.

**Design consequence:** generic **registry** + pluggable per-app **adapters**.
Discovery finds what's on the phone; the registry maps known packages to their call
recipe + branding; unknown apps get graceful fallback instead of being hidden.

---

## 4. Design approach

### 4.1 Data model
Replace the hardcoded `CallingChannel.WhatsApp` / `CallingChannel.GoogleVoice` subtypes
with one generic data class, e.g.:

- `AppChannel(packageName, appLabel, icon, brandColorHex, callRecipeId, phoneAccountHandle?)`
- `phoneAccountHandle` covers the Telecom-integrated path (Google Voice, SIP) — placing
  the call is `redirectCall(handle, phoneAccountHandle)`, fully generic, no per-app code.
- `callRecipeId` covers the proprietary-intent path (WhatsApp-style) — resolved through
  the adapter registry.

The existing `when` statements are on a sealed interface, so the Kotlin compiler flags
every one of the 69 sites during refactor — mechanical, not a redesign.

### 4.2 Discovery (rework `ChannelDiscoveryManager.refreshChannels`)
1. **Cellular SIMs** — unchanged (`SimHelper.getActiveSimCards`).
2. **Telecom PhoneAccounts** — enumerate `callCapablePhoneAccounts` with **no package
   filter** (drop the googlevoice filter). Each becomes an `AppChannel` with its
   `phoneAccountHandle`; label/icon from the package. This auto-covers Google Voice,
   SIP providers, and any future telecom-integrated VoIP app — zero maintenance.
3. **Installed messaging/VoIP apps** — probe `PackageManager` for apps declaring VoIP-call
   MIME types / dial intents instead of the 3 hardcoded `isPackageInstalled` checks.
   For each discovered package: look up the **adapter registry**; known package →
   full channel with its call recipe; **unknown package → still listed**, with a
   graceful fallback (launch the app's dial activity via `ACTION_DIAL`) rather than
   being hidden. This is the resilience requirement.
4. **System Default / Ask Every Time** — unchanged (pseudo-channels, not apps).

### 4.3 Adapter registry
Small table (code or Room, code is fine to start): package name → call recipe +
branding overrides. Ships with WhatsApp / WhatsApp Business / Google Voice adapters
extracted from today's hardcoded logic. Adding Telegram = adding one registry entry
(package `org.telegram.messenger` + its researched call recipe), touching nothing else.

### 4.4 UI
Render channel pills, icons, and dialogs from the generic channel's label/icon/color
instead of per-type branches. `WhatsAppIcon.kt` becomes generic app-icon loading
(`PackageManager.getApplicationIcon`). Per-channel enable/disable and custom names
(`ChannelConfig`) key off the generic channel id.

### 4.5 Persistence / migration
Stored channel ids change from `"whatsapp"` / `"whatsapp_business"` / `"google_voice"`
to generic ids like `"app:com.whatsapp"`. One-time migration mapping old → new ids in
`number_channel_preferences` ("Remember choice" pins), `ChannelConfig`, and
`contact_sim_preferences` where applicable. No silent behavior changes: every
previously available channel must resolve to the same post-migration channel.

### 4.6 Interaction with the rules design
`channel_routing_rules.targetChannelId` (see `omnidial-rule-sets-design.md`) should
reference the **generic** channel ids from the start — do this refactor before, or
together with, the routing-rules build to avoid a second migration.

---

## 5. Difficulty assessment

**Moderate — a 2–3 day refactor for someone who knows the codebase, not a rewrite.**

- Easy: discovery is already centralized in `ChannelDiscoveryManager`; the
  `callCapablePhoneAccounts` enumeration is already written (just remove the filter).
- Mechanical: the 69 type-check sites — compiler-guided via sealed-`when` exhaustiveness.
- One-time research per new app: each new adapter (Telegram, Signal, …) needs its call
  recipe figured out once. This is the only part that can't be made generic.
- Migration: old → new channel ids, must be verified with backup → clear → restore.

---

## 6. Acceptance criteria

1. On a phone with WhatsApp + Google Voice installed, the channel list is identical
   before/after (same labels, colors, order, behavior) — no regressions.
2. Uninstalling WhatsApp removes its channel; reinstalling brings it back — no code change.
3. A telecom-integrated VoIP app that is NOT in the adapter registry (e.g. a SIP app)
   appears automatically via `callCapablePhoneAccounts`.
4. An installed messaging app with no known call recipe appears in the channel list
   with graceful fallback (opens the app's dialer) instead of being hidden or crashing.
5. Existing "Remember choice" pins and per-contact SIM preferences keep working after
   the id migration (backup → clear → restore test).
6. Adding a new app requires only a registry entry — no changes to discovery, UI,
   rules, or preferences code.

---

## 7. Open questions (Hari)

1. Which apps get first-party adapters first — Telegram? Signal? Viber? (Each needs its
   call recipe researched; WhatsApp's is already known.)
2. For unknown apps with no call recipe: is "open the app's dialer" the right fallback,
   or should unknown apps be hidden behind an "experimental" toggle?
3. Should the adapter registry live in code (simple, needs app update per new app) or
   be remotely updatable (new apps supported without a release)?
4. Timing: do this refactor before the outgoing-routing rules build (recommended —
   avoids a second channel-id migration), or after?
