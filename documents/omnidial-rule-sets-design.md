# OmniDial — New Rule Sets: Design Exploration

**Status:** Exploration only. No code changed. Written 2026-09-21 for later revisiting.
**Repo state at time of research:** `origin/main` @ `2e86138`.

> **Note:** Set A (outgoing channel routing) was removed from this doc — the feature it designs was removed from the app in v2.1.3. What remains covers incoming-call and post-call rule sets only.

---

## 1. Background: what exists today

**The existing Rules engine is incoming-call automation only.** `CallerRule`
(`app/src/main/java/com/example/data/Entities.kt:15-33`) supports:

- Conditions: number pattern, required Wi-Fi SSID, required Bluetooth device
  (matched in `matchesRuleConditions`, `telecom/CallManager.kt:722`)
- Actions: auto-answer, DTMF touch-tones, auto-reply SMS, auto-hangup, speakerphone
  (executed in `executeAutomationWorkflow`, `CallManager.kt:804`)
- It returns immediately for non-incoming calls (`CallManager.kt:764`).
- A rule has **no field for naming a calling channel** (SIM 1 / SIM 2 / WhatsApp).

**Conclusion:** Incoming-call automation exists; time, screening, and post-call handling are the gaps. The sets below build on the incoming-call engine.

---

## 2. Proposed new rule sets

### Set B — Quiet Hours (time-based incoming handling)
- **Problem:** no time-of-day awareness anywhere in the call path.
- **Example rules:** 22:00–07:00 → silence unknown numbers, favorites ring through; work hours → auto-decline + SMS for non-VIP.
- **Trigger:** at incoming-call time, alongside the existing automation check.

### Set C — Location & Roaming conditions
- Not a rule table; **new condition types** consumable by the other engines.
- Caveat: the only ambient conditions today are the Wi-Fi SSID / Bluetooth gates on
  `CallerRule`, and the SSID read uses deprecated `connectionInfo`, which returns
  `<unknown ssid>` on Android 10+ without location permission — **half-broken, must be
  fixed before location-dependent rules are trustworthy.**
- Roaming state is detected (`ActiveCallInfo.isRoaming`, `CallManager.kt:248-295`) but never actionable today.
- **Trigger:** at call time, both directions.

### Set D — Incoming Screening+ (beyond the gate buzzer)
- **Problem:** incoming handling is binary (automation rule runs, else spam blocklist may decline). No "decline + SMS" rule action, no VIP ring-through, no unknown-number policy. (The primitives `declineCall`/`declineWithSms` exist at `CallManager.kt:953-976` but no rule action invokes them.)
- **Example rules:** spam-suspect + not in contacts → decline + auto-SMS; VIP list → distinctive ring during Quiet Hours.
- **Trigger:** at incoming-call time, same pass as existing automation.

### Set E — Post-call follow-ups
- **Problem:** post-call notes and callback reminders (`ReminderScheduler.kt`) are manual every time; nothing pattern-driven.
- **Example rules:** after calls to `+91*` family numbers → prompt for a note; missed VIP call → immediate callback notification.
- **Trigger:** at call end — hook is `CallManager.handleCallEnded` (`CallManager.kt:510`).

### Set F — Scheduled / at-will automation toggles (supporting set)
- **Problem:** rules evaluate only at call time or via the manual Test button. No "arm the gate rule 18:00–20:00 weekdays."
- **Trigger:** on schedule (AlarmManager, same pattern as `ReminderScheduler.kt:11-55`) or manual.

---

## 4. Architecture

### Central decision: hybrid — keep `caller_rules` frozen, add new tables per trigger domain

Why not a single `ruleType` discriminator on `CallerRule`? It would force nullable-field
soup (`dtmfSequence` meaningless for a new set), complicate the one working
evaluation path with type switches, and risk regressions. Separate tables per
trigger-domain is cleaner; the shared pieces are the **pattern matcher**, the **log**,
and the **UI components**.

- **`caller_rules` — frozen.** Add one additive column `actionKind` (default
  `LEGACY_WORKFLOW`) so Set D (decline / decline+SMS / silent-ring) can reuse the table,
  UI, and Test button with zero behavior change for existing rows. Single
  `ALTER TABLE … DEFAULT` migration; existing evaluation order preserved.
- **`quiet_hours_windows`** (new, Set B): multiple windows × per-window exception lists
  (favorites/VIPs).
- **`post_call_rules`** (new, Set E): pattern → action (`prompt_note | create_reminder | notify_callback`).
- Set C is condition types, not a table. Set F reuses the `ReminderScheduler`
  AlarmManager pattern against a small `scheduled_toggles` table.

### Evaluation engines and hook points (all verified in code)

- **Sets B/D — extend the existing incoming pass.** `checkAndExecuteAutomation`
  (`CallManager.kt:763`) is the right and only seam (invoked on `STATE_RINGING`,
  `CallManager.kt:495` and `:1285`). Order inside: (1) existing `CallerRule`
  automations, unchanged, first-match; (2) quiet-hours windows (VIP exceptions first);
  (3) screening rules incl. new `actionKind`s; (4) spam blocklist, unchanged, still last.
- **Set E — new branch in `handleCallEnded`** (`CallManager.kt:510`), after logging. Isolated; cannot affect call behavior.
- **Set F — `ReminderScheduler` pattern** (`telecom/ReminderScheduler.kt:11-55`); new small table + receiver flipping `isEnabled`.
- **Shared seams to reuse:** `matchesRulePattern` semantics, `PhoneNumberNormalizer.toE164`,
  the `automation_logs` table (extend `actionsSummary` with a rule-set tag — free audit
  trail), `RuleCard`/`RuleEditDialog` UI components, the Recipes-gallery modal precedent
  (`RulesScreen.kt` `showRecipesModal`).

---

## 5. Precedence & conflict resolution

**Outgoing** (first match wins, top to bottom):

1. **Emergency guardrail** — hardcoded, never overridable (`DialerScreen.kt:458-466`).
2. **Explicit per-call user action** — tapped dock pill or just-chosen channel. The user in the moment always wins.
3. **Exact-number "Remember choice" pins** — explicit teaching for *that* number outranks a general pattern. *(Open question — confirm.)*
4. **Per-contact SIM preference** (legacy) — kept working.
5. **Hardcoded intl→WhatsApp recommendation** — fallback.
6. Current SIM slot default.

**Incoming** (first action wins): existing `CallerRule` automation → quiet-hours
(VIP/favorite exceptions punch *upward* through the silence) → screening rules → spam
blocklist (unchanged backstop).

**User-override principle:** *rules are defaults, never cages.* Every automated decision
must be (a) visible at decision time with one tap to change; (b) logged in the automation log; (c) reversible per call
without disabling the rule; plus a global "pause all automation" kill-switch.
**Conflict UX:** priority order shown explicitly in the list (drag-to-reorder); never fail silently.

---

---

## 7. UI direction

The current Rules screen is already two tabs + three modals — don't add a third tab.
**Proposed: an "Automation" hub.** The bottom-nav "Rules" destination becomes a hub of
cards, each opening a focused list screen reusing `RuleCard` patterns:

- **Incoming rules** (existing screen, untouched)
- **Quiet hours** (Set B: time windows + exceptions)
- **Screening** (Set D: VIP / spam / unknown policies)
- **Follow-ups** (Set E)

Each card shows an "on/off + n active" summary line. Recipes galleries per set (existing
`showRecipesModal` precedent) carry onboarding.

---

## 8. Migration & coexistence — no silent behavior changes

- Existing `caller_rules` rows: byte-identical behavior (new `actionKind` defaults to legacy workflow).
- `number_channel_preferences` pins: keep full authority (precedence tier 3).
- `contact_sim_preferences`: keep working at tier 4.
- **Ship posture: every new set defaults to disabled/empty.** Day-one behavior = today's behavior; new engines run only when the user creates a rule in that set.

---

## 9. Phased rollout

- **Phase 1: Set D.** `actionKind` (decline / decline+SMS / silent-ring) on `caller_rules` + Screening hub screen.
- **Phase 2: Sets B, C, E, F.** Quiet-hours windows, roaming/location conditions (after
  the SSID/location-permission fix), post-call follow-ups, scheduled toggles.

Each phase independently shippable and testable.

---

## 10. Risks & guardrails

  (`targetAccount != initialPhoneAccount`, `~line 119`) must be kept; the routing engine
  must be **idempotent** — resolved channel == incoming account → `placeCallUnmodified()`,
  never redirect. Unit-test the loop case explicitly.
- **Android Telecom constraints:** `CallRedirectionService` is API 29+; `onPlaceCall` is
  **not** invoked for emergency calls; the service can only cancel / redirect-to-account /
  place-unmodified — no number rewriting, no UI injection. Decisions must be synchronous
  and fast → warm-cache requirement. **Unverified:** whether `onPlaceCall` fires for calls
  placed by OmniDial's own keypad when OmniDial is the default dialer — device test needed
  before Phase 2 relies on it.
- **WhatsApp invocation limits:** `ContactHelper.launchWhatsAppCall` is intent-based and
  needs WhatsApp installed; background-activity-launch restrictions apply from a service —
  the existing full-screen notification fallback (`showWhatsAppRedirectionNotification`)
  must be kept for redirection-triggered WhatsApp outcomes.
- **Battery/background (Sets B/F):** quiet hours need no background work (evaluated at ring
  time). Scheduled toggles via `setExactAndAllowWhileIdle` need `SCHEDULE_EXACT_ALARM` on
  Android 12+ — request the permission, degrade gracefully to inexact when denied. No
  `WorkManager` in the codebase today; don't introduce one — the AlarmManager precedent suffices.
- **Location/SSID gating (Set C):** don't ship location-dependent conditions until the
  deprecated-`connectionInfo` issue is fixed or location permission is properly requested —
  otherwise rules silently never fire.
- **The override principle as backstop:** no rule may place/answer/decline/redirect a call
  without the decision being visible and one-tap reversible, plus the global pause switch.
  (Horror story to prevent: `*` pattern + WhatsApp → every call leaves the phone app.)

---

## 11. Open questions (for Hari, before build work)

**Still open:**
1. **Quiet hours semantics:** silence the ringer, decline+SMS, or straight to voicemail?
2. **WhatsApp from scheduled/at-will triggers** (e.g. "call Mom on WhatsApp every Sunday") — ever in scope, or WhatsApp stays call-time-only?
3. **Log retention:** how long to keep per-rule fire history; should rule activity surface outside the Automation hub (e.g. in Recents)?
