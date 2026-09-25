# Handover: OmniDial conference-call bugs (for Gemini)

Repo: `D:\Try\Git-Repos\Kishan-Dialer`, branch working tree (uncommitted).
Files: `CallManager.kt`, `InCallScreen.kt`. Device: Spectrum Mobile (3-way works in stock Phone app).
Build: `.\gradlew.bat installRelease`. History: `documents/conference-work-log.md`.

## Bug 1 (dangerous): dropping one of two conference participants shows "call ended" while the other line is still live

Repro: A+B merged → drop B from Manage → app shows "call ended", but the other phone
still has live two-way audio (confirmed by voice check on both ends).

Evidence from `adb logcat -s OmniConf:D`:
- After the drop: `primary <conf> children -> 0`, then `tracked child <id> parent -> null`,
  then `collapsing empty conference <conf> to survivor <id>`.
- The current code promotes the detached child to primary at that point — and the UI
  then ends up in the ended state while telecom audio is still live.

Key finding: the conference shell `Call` stays **ACTIVE** with 0 children after the
drop — it is the bearer of the remaining audio. The detached child object is a husk
that dies. Promoting the husk over the live shell loses the live call.

Suggested direction (not prescribed): keep a still-ACTIVE empty shell primary and
remember the detached survivor; promote the survivor only if the shell itself
disconnects/is removed; the End button must disconnect both legs so a user hangup can
never leave a live leg behind.

## Bug 2: first of two merged participants never gets a name

Repro: merge two calls to two numbers of the same contact → only the second shows a
name in Manage.

Evidence: only one pre-merge identity was ever retained/labeled. The other original
`Call` left `extraCalls` via a path that doesn't retain its identity (suspect
`onCallRemoved`; retention currently happens on the disconnect/reparent paths).

## Ground truths (from device logs — do not re-derive by guessing)

- After merge, the framework re-adds each participant as a NEW `Call` already parented
  to the conference; originals may be torn down, not reparented; replacement children
  carry numbers but no names.
- Dropping a participant detaches (not kills) the survivor's object; shell stays ACTIVE.
- Identity matching must use `ContactHelper.isSamePhoneNumber` (libphonenumber); naive
  digit-suffix matching misses format variants. Positional fallback is acceptable —
  retained identities can only belong to this conference's participants.

## Constraints

- Do not change merge/swap/split behavior that already works; do not regress the
  parented-child fix (parented arrivals must never become primary).
- Test on a real SIM: merge → both names in Manage; drop one → survivor continues with
  their name, no "0 people"/"Unknown"/false "call ended"; End → all legs die (verify on
  the other phone).
- If it still misbehaves, capture `adb logcat -s OmniConf:D` from before Merge through
  15s after the failure before changing anything.
