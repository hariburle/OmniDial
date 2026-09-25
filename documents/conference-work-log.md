# OmniDial conference calling — work log

Date: 2026-09-23. Device: Hari's phone, Spectrum Mobile (supports 3-way calling).
Repo: `D:\Try\Git-Repos\Kishan-Dialer` (authoritative). Workspace mirror: `~/workspace/omnidial`
(no compiler on the VM — all work statically reviewed; Hari builds on Windows with
`.\gradlew.bat installRelease`). Never commit/push without explicit approval.

## 1. Base feature — conference calling + portrait lock + stuck-popup fix

Delivered as `omnidial-conference-portrait.patch` (7 files) + `ConferenceUiGatingTest`
(7 tests) + `ToTest.md` §8–9.

- Multi-call tracking with surviving-call promotion; Add-call chooser (Keypad + Contacts tabs).
- Merge / Swap / Split / end-one-participant; participant list from `Call.getChildren()`.
- Per-participant Recents logging (blank parent entries skipped).
- Portrait lock while the in-call overlay is visible.
- Stuck green minimized-call popup fix (auto-dismiss of minimized post-call state).
- Reworked post-call auto-close: 5s untouched / 60s interacted-blank / 10-min backstop;
  typed notes never discarded. Capability-gated buttons. Failure toasts.

## 2. Patch sequence (all `CallManager.kt` unless noted)

1. **`omnidial-conference-gating-fix.patch`** — Merge/Swap buttons became visible.
   They did nothing when tapped.
2. **`omnidial-conference-merge-fix.patch`** — Root cause: the app called
   `Call.mergeConference()` / `Call.swapConference()` on two *separate* calls; per AOSP's
   `ConnectionService` those only work on an *existing* conference object, so they were
   silent no-ops. Fix: `primary.conference(otherCall)` to merge,
   hold(active)+unhold(held) to swap; keeps merge/swapConference for the
   already-a-conference case. **Hari tested: calls merge successfully.**
3. **`omnidial-conference-postmerge-fix.patch`** — Root cause: after merge, Android
   reparents the two calls as children of the new conference `Call`, but CallManager kept
   them in `extraCalls` as two separate calls, so gating still showed Merge/Swap and the
   conference UI never engaged. Fix: `onParentChanged` cleanup removes reparented calls
   from `extraCalls`; `onChildrenChanged` refreshes participants; falls back to live
   tracked calls when the framework reports no children; adds `OmniConf` logcat
   diagnostics (`adb logcat -s OmniConf:D`). After applying: Manage appeared briefly,
   then vanished mid-conference.
4. **`omnidial-conference-parented-child-fix.patch`** — Root cause (from Hari's OmniConf
   log): after merge, the framework re-adds the two participants as NEW `Call` objects
   that *already have the conference as their parent*. `onCallAdded` mistook them for
   standalone calls, demoted the real conference into `extraCalls`, and made a child the
   primary — flipping `isConference` false, hiding Manage mid-conference. Fix: calls
   arriving already-parented are light-tracked only (never primary, never in
   `extraCalls`, never written to Recents); pre-merge name/number identities of absorbed
   calls are retained so blank framework child rows in Manage still show real names.
5. **`omnidial-conference-names-and-collapse-fix.patch`** (`CallManager.kt` +
   `InCallScreen.kt`) — Issues from the next log: (a) merged participants showed
   "Unknown"/big-U — on this stack the originals are *torn down* (not reparented)
   during merge, and the framework's replacement child objects carry numbers but no
   names; (b) disconnecting one participant left "Conference • 0 people" while the other
   line was still connected, then "Unknown". Fix: retain pre-merge identities on the
   disconnect path too (matched to children by number, positionally as fallback); when
   the primary conference hits 0 children with a live detached child, promote the
   survivor to primary; conference header shows a group icon instead of "U" and lists
   both names when 1–2 participants are named; Manage auto-dismisses when the
   conference ends.
6. **`omnidial-conference-labeling-v2.patch`** (`CallManager.kt` + `InCallScreen.kt`) —
   Issues: only the *second* participant got a name (naive last-10-digits matching
   missed format variants); the collapse survivor became "Unknown" (its details are
   blank by collapse time). Fix: match with the app's libphonenumber-backed
   `ContactHelper.isSamePhoneNumber`, positional fallback when number matching fails
   (retained identities can only belong to this conference's participants);
   `labeledChildIdentities` map records which identity labeled each child so the
   promotion reuses it directly; Add-person dialog tabs reordered — Contacts first,
   Keypad second (Hari's request).

## 3. Established ground truths (from Hari's device logs)

- Spectrum Mobile + this phone support 3-way calling; stock Phone app merges fine.
- After merge, the framework re-adds each participant as a **new** `Call` object,
  already parented to the conference; the originals may be torn down instead of
  reparented; replacement child objects carry numbers but no names.
- Dropping one of two participants: the conference shell stays **ACTIVE** with 0
  children and the survivor's child object detaches (`parent -> null`).
- `adb logcat -s OmniConf:D` is the diagnostic channel (all conference decisions log
  there; note: the primary `Call.Callback` state lines use the default TAG, not
  OmniConf, so they are invisible under that filter).

## 4. Known issues (confirmed 2026-09-23 ~23:34, labeling-v2 applied)

1. **First participant still unlabeled.** Only one pre-merge identity was retained, so
   only one child could be labeled. The other original left `extraCalls` via a path
   that doesn't retain (suspect: `onCallRemoved`). Planned fix: retain the identity in
   `onCallRemoved` as well (belt and suspenders alongside the disconnect/reparent
   paths).
2. **DANGEROUS — app reports "call ended" while a line is still live.** Dropping one
   of two participants: the app promoted the detached child and showed "call ended",
   but the other phone still had live two-way audio (Hari confirmed he could talk and
   listen). Analysis: the conference **shell stays ACTIVE as the bearer of the
   remaining audio**; the detached child object is a husk that dies. Dethroning the
   live shell for the husk loses the live call. Planned fix — collapse-pending state
   machine:
   - While the conference shell is still ACTIVE with 0 children, **keep it primary**
     and remember the detached survivor; do not promote yet.
   - Show the survivor's identity (from `labeledChildIdentities`/retained) as a
     synthetic single-participant row so the UI reflects the real 1:1 state.
   - Promote the survivor **only if the shell itself disconnects or is removed**
     (intercept in primary `onStateChanged(DISCONNECTED)` and `onCallRemoved`).
   - The End button must disconnect **both** the shell and the survivor, so no leg
     can survive a user-initiated hangup.

## 5. Device test checklist (real SIM)

- Merge two calls → Manage stays visible for the whole conference; both names appear
  in Manage; header shows names + group icon (no "U").
- Drop one participant from Manage → the remaining caller continues as a normal call
  with their name (no "0 people", no "Unknown", no false "call ended").
- End button during/after a collapse → **all** legs actually die (verify on the other
  phone).
- Add-person dialog opens on the Contacts tab, Keypad second.
- If anything misbehaves: capture `adb logcat -s OmniConf:D` from before Merge
  through 15s after the failure (the primary callback's own state lines are invisible
  under this filter — that gap is known).

## 6. Patch locations

All patches are delivered in `~/workspace/your_files/` — apply straight from
Downloads on the Windows machine; never copy a `.patch` into the repo:

```powershell
cd D:\Try\Git-Repos\Kishan-Dialer
git apply --check "$env:USERPROFILE\Downloads\<name>.patch"
git apply "$env:USERPROFILE\Downloads\<name>.patch"
.\gradlew.bat installRelease
```

Copies of the earlier patches also live in this goal's `files/` directory.
