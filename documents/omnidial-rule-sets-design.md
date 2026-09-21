# OmniDial — New Rule Sets: Design Exploration

**Status:** Exploration only. No code changed. Written 2026-09-21 for later revisiting.
**Repo state at time of research:** `origin/main` @ `2e86138`.

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

**Outgoing channel selection lives in four separate, unconnected systems:**

| Mechanism | Location | Notes |
|---|---|---|
| Keypad dock auto-resolution | `ui/screens/DialerScreen.kt:444-500` | emergency → SIM; pinned preference; hardcoded intl→WhatsApp |
| "Remember choice" pins | `number_channel_preferences` table, written at `DialerScreen.kt:1375-1380` | exact normalized number only, no patterns; manual, one number at a time |
| Per-contact SIM preference | `MainViewModel.placeCall`, `ui/MainViewModel.kt:1276-1320` | per-number SIM slot |
| Outgoing-call interception | `telecom/OmniCallRedirectionService.kt:26-135` | learned WhatsApp mode, `all_international` redirect, contact SIM redirect |

`EvaluateSimRuleUseCase` (`domain/usecase/EvaluateSimRuleUseCase.kt`) looks like
outgoing rule-based SIM selection but is **dead, unwired code** — zero production call
sites, and it resolves to the default SIM regardless. Not a foundation; ignore it.

**Conclusion:** Rules cannot pick calling channels today. Per-number channel choice
exists but is manual and pattern-less. Bridging the two is new development.

---

## 2. Proposed new rule sets

### Set A — Outgoing Channel Routing (the core ask)
- **Problem:** per-number channel choice is manual ("Remember choice," exact-number only). No way to say "all +91 numbers → WhatsApp" or "all 800-numbers → SIM 1."
- **Example rules:** international numbers → WhatsApp; `+1 800*` → SIM 1; roaming + non-emergency → always ask (force the choice dialog); **all employees of company X → SIM 2** (see §3).
- **Trigger:** at outgoing-call time, before the channel commits.

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
- **Problem:** rules evaluate only at call time or via the manual Test button. No "arm the gate rule 18:00–20:00 weekdays," no dry-run for routing rules.
- **Trigger:** on schedule (AlarmManager, same pattern as `ReminderScheduler.kt:11-55`) or manual.

---

## 3. Motivating example: company → SIM 2

"For all employees who work at a certain company, use SIM 2."

This fits Set A and is cleaner than manual tags, because **the data already exists**:
Android contacts carry an organization field (company + title), and the dialer already
resolves a dialed number to its contact at call time.

- **Rule form:** condition = contact's company *contains* "Acme" → action = route via SIM 2.
- **Flow:** dial → number resolves to contact (from the warm cache, not a fresh query, or dialing feels laggy) → company match → SIM 2 pre-selected in the dock with a "via rule" affordance and one-tap override.
- **Matching semantics:** "contains," so "Acme" catches "Acme Corp" / "Acme Inc."
- **Non-contacts:** no contact → no company → rule doesn't fire; falls through to the next rule or default.
- **Generalizes for free:** "title contains 'Client' → SIM 1," "family → WhatsApp," etc.

**Decision recorded 2026-09-21:** attribute-based conditions (company, title) preferred over
building a manual contact-tagging UI.

---

## 4. Architecture

### Central decision: hybrid — keep `caller_rules` frozen, add new tables per trigger domain

Why not a single `ruleType` discriminator on `CallerRule`? It would force nullable-field
soup (`dtmfSequence` meaningless for routing rules), complicate the one working
evaluation path with type switches, and risk regressions. Separate tables per
trigger-domain is cleaner; the shared pieces are the **pattern matcher**, the **log**,
and the **UI components**.

- **`caller_rules` — frozen.** Add one additive column `actionKind` (default
  `LEGACY_WORKFLOW`) so Set D (decline / decline+SMS / silent-ring) can reuse the table,
  UI, and Test button with zero behavior change for existing rows. Single
  `ALTER TABLE … DEFAULT` migration; existing evaluation order preserved.
- **`channel_routing_rules`** (new, Set A): `id`, `name`, `numberPattern` (reuse
  `matchesRulePattern` semantics — exact/endsWith/contains, `CallManager.kt:710-721`),
  `targetChannelId` (reuse `CallingChannel` ids: `system | sim_1 | sim_2 | whatsapp |
  whatsapp_business | google_voice | ask`), `priority` (user-ordered int),
  `conditionsJson` (nullable: company match, time windows, wifi SSID, roaming flag —
  schemaless for phase 1), `isEnabled`, timestamps. Vocabulary deliberately mirrors
  `number_channel_preferences` so the systems compose.
- **`quiet_hours_windows`** (new, Set B): multiple windows × per-window exception lists
  (favorites/VIPs).
- **`post_call_rules`** (new, Set E): pattern → action (`prompt_note | create_reminder | notify_callback`).
- Set C is condition types, not a table. Set F reuses the `ReminderScheduler`
  AlarmManager pattern against a small `scheduled_toggles` table.

### Evaluation engines and hook points (all verified in code)

- **Set A — `ChannelRoutingEvaluator`** (new pure function: number + context → channel id or null).
  - Phase 1 hook (UI path): inside the keypad dock resolution `LaunchedEffect`
    (`DialerScreen.kt:444-500`), between the "Remember choice" pin lookup (~line 470)
    and the hardcoded intl→WhatsApp recommendation (~line 478). Must run **below** the
    `userSelectedChannel` check (~line 445) so an explicit per-call tap always wins.
  - Phase 2 hook (external path): `OmniCallRedirectionService.onPlaceCall`
    (`OmniCallRedirectionService.kt:26-135`), before the learned-choices loop (~line 50).
    Mapping: `sim_1`/`sim_2` → `redirectCall(handle, SimHelper.getPhoneAccountForSimSlot(...))`;
    `whatsapp*` → existing cancel + `ContactHelper.launchWhatsAppCall` + notification
    fallback (~lines 78-95); `ask` → `placeCallUnmodified()`.
  - **New seam required for Phase 2:** the service reads SharedPreferences today;
    routing rules live in Room, so it needs the same warm in-memory cache pattern
    `ChannelPreferenceRepository` already uses — `onPlaceCall` must decide fast and
    cannot block on disk.
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

### Learned-model hygiene (important detail)

When a call is routed by a Set A rule, tag it so `ask_learn` mode
(`MainViewModel.kt:1317`, `saveLearnedCallMode`) does **not** record rule-driven
outcomes as user learning — otherwise rules would slowly corrupt the learned model.

---

## 5. Precedence & conflict resolution

**Outgoing** (first match wins, top to bottom):

1. **Emergency guardrail** — hardcoded, never overridable (`DialerScreen.kt:458-466`).
2. **Explicit per-call user action** — tapped dock pill or just-chosen channel. The user in the moment always wins.
3. **Exact-number "Remember choice" pins** — explicit teaching for *that* number outranks a general pattern. *(Open question — confirm.)*
4. **Pattern routing rules** (Set A), by user-defined priority, first match.
5. **Per-contact SIM preference** (legacy) — kept working; a user-authored pattern rule outranks it.
6. **Hardcoded intl→WhatsApp recommendation** — demoted to fallback (a user rule saying "intl → SIM 2" must beat the hardcode, or rules are pointless).
7. Current SIM slot default.

**Incoming** (first action wins): existing `CallerRule` automation → quiet-hours
(VIP/favorite exceptions punch *upward* through the silence) → screening rules → spam
blocklist (unchanged backstop).

**User-override principle:** *rules are defaults, never cages.* Every automated decision
must be (a) visible at decision time — e.g. dock pill shows "via rule: Acme → SIM 2"
with one tap to change; (b) logged in the automation log; (c) reversible per call
without disabling the rule; plus a global "pause all automation" kill-switch.
**Conflict UX:** priority order shown explicitly in the list (drag-to-reorder); when a
pin and a rule disagree, surface "pinned choice wins over rule" inline, never fail silently.

---

## 6. Natural-language rule creation

**Design direction (2026-09-21): on-device template parser, not a cloud LLM.**

The rule vocabulary is bounded (channels, number patterns, companies, times), so
pattern/template matching covers realistic utterances deterministically:

| Utterance | Parsed rule |
|---|---|
| "Everyone at Acme → use SIM 2" | company contains "Acme" → `sim_2` |
| "International numbers on WhatsApp" | number is international → `whatsapp` |
| "800 numbers on SIM 1" | number starts with +1800 → `sim_1` |
| "When roaming, ask me every time" | roaming → `ask` |
| "Silence unknown callers after 10pm" | quiet-hours rule |

Why on-device: works offline, zero per-rule cost, no network latency, fits the
on-device privacy story, and — critically — **deterministic and testable** (an LLM
might creatively misread "SIM 2"; a template either matches or it doesn't).

**The trust mechanism: preview before saving.** NL parsing is fallible, and a
mis-parsed rule silently reroutes real calls. Flow: **type → parsed preview → confirm.**
The preview shows the rule in plain words *plus a match count*:

> "When: contact's company contains 'Acme' → Use: SIM 2"
> *This matches 14 contacts.* [Test it] [Edit] [Save]

If the parser can't confidently parse an utterance, it doesn't guess — it pre-fills the
manual rule form with best guesses and highlights what's ambiguous.

**Scoping:** the NL layer produces the same rule objects the manual builder makes, so
it's separable — engine + manual builder first, NL input layered on top. Start with
Set A (tightest vocabulary); quiet hours and screening later.

---

## 7. UI direction

The current Rules screen is already two tabs + three modals — don't add a third tab.
**Proposed: an "Automation" hub.** The bottom-nav "Rules" destination becomes a hub of
cards, each opening a focused list screen reusing `RuleCard` patterns:

- **Incoming rules** (existing screen, untouched)
- **Call routing** (Set A: pattern → channel, priority ordering, per-rule dry-run "Test")
- **Quiet hours** (Set B: time windows + exceptions)
- **Screening** (Set D: VIP / spam / unknown policies)
- **Follow-ups** (Set E)

Each card shows an "on/off + n active" summary line. Recipes galleries per set (existing
`showRecipesModal` precedent) carry onboarding — e.g. a "Frequent flyer" recipe pack
for Set A. The keypad dock gets a small "rule applied" affordance with one-tap override.

---

## 8. Migration & coexistence — no silent behavior changes

- Existing `caller_rules` rows: byte-identical behavior (new `actionKind` defaults to legacy workflow).
- `number_channel_preferences` pins: keep full authority (precedence tier 3). Routing rules never overwrite/delete pins. Consider a "why this channel?" explainer on dock long-press.
- `contact_sim_preferences`: keep working at tier 5; document the demotion in release notes (only matters with an overlapping user rule — opt-in).
- Learned WhatsApp choices: keep; tag rule-driven outcomes so they don't pollute learning (§4).
- **Ship posture: every new set defaults to disabled/empty.** Day-one behavior = today's behavior; new engines run only when the user creates a rule in that set.

---

## 9. Phased rollout

- **Phase 1 (MVP): Set A, UI path only.** `channel_routing_rules` table +
  `ChannelRoutingEvaluator` + hook in `DialerScreen.kt:444-500` + "Call routing" hub
  screen + dry-run test + automation-log entries. No Telecom API changes, no background
  work, no new permissions, fully reversible per call. Directly answers the core ask.
- **Phase 2: Set A external path + Set D.** Evaluator into `OmniCallRedirectionService.onPlaceCall`
  (car / watch / voice-assistant coverage) with warm-cache; `actionKind`
  (decline / decline+SMS / silent-ring) on `caller_rules` + Screening hub screen.
- **Phase 3: Sets B, C, E, F.** Quiet-hours windows, roaming/location conditions (after
  the SSID/location-permission fix), post-call follow-ups, scheduled toggles.

Each phase independently shippable and testable. **Open:** whether NL rule creation
rides with Phase 1 or follows as its own phase.

---

## 10. Risks & guardrails

- **Redirect loops (Phase 2):** `redirectCall` re-invokes `onPlaceCall`. Existing guard
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
  must be kept for rule-triggered WhatsApp outcomes.
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

**Resolved 2026-09-21:**
- ~~Tag-based vs attribute-based conditions~~ → attribute-based (company, title); no manual tagging UI.

**Still open:**
1. **Precedence:** should an exact-number "Remember choice" pin outrank a pattern routing rule, or should the rule win? (Proposal: pins win — confirm.)
2. **Phase 1 scope:** keypad-UI only, or routing rules also covering car head-unit / watch / voice-assistant calls (redirection-service path) from day one?
3. **Quiet hours semantics:** silence the ringer, decline+SMS, or straight to voicemail?
4. **"Remember choice" migration:** one-tap "convert my pins into rules" bulk migration, or keep pins as the separate higher-priority system permanently?
5. **WhatsApp from scheduled/at-will triggers** (e.g. "call Mom on WhatsApp every Sunday") — ever in scope, or WhatsApp stays call-time-only?
6. **Log retention:** how long to keep per-rule fire history; should rule activity surface outside the Automation hub (e.g. in Recents)?
7. **NL layer timing:** fold natural-language rule creation into Phase 1, or spec it as the follow-on phase?
