# OmniDial — Active Testing Checklist (`ToTest.md`)

Items currently pending verification or undergoing testing. Verified items are archived in `documents/Verified.md`.

---

## 📋 Active Items To Test

### 1. Task 10.1 (Phase 10): Roaming-Aware & DAG Conflict-Resolved SIM Routing
- [ ] **To Test**
- **Test Steps**:
  1. Insert two active SIM cards (or configure dual-SIM state where SIM 1 is roaming and SIM 2 is local non-roaming).
  2. In **Rules** (Tab 4), create two overlapping rules:
     - Broad rule: Prefix `+1*` (Weight ~501) targeting SIM 1.
     - Specific rule: Exact number `+14155550199` (Weight ~1012) targeting SIM 2.
  3. Dial `+14155550199` from the keypad.
  4. Test roaming protection by placing an outgoing call to a non-rule number while SIM 1 has roaming enabled.
- **Expected Result**:
  - DAG weighted resolver selects the exact-match rule over the prefix rule.
  - Roaming detection identifies roaming status in real-time. If roaming is active on the default SIM, OmniDial routes or prompts with `[Roaming Protected]` on the local non-roaming SIM to prevent bill shock.

### 2. Task 10.2 (Phase 10): Tiered Caller ID & Trust Badges
- [ ] **To Test**
- **Test Steps**:
  1. Simulate or receive an incoming call from a suspected spam number (e.g. `+18005550199`).
  2. Simulate or receive an incoming call from a verified delivery service (e.g. `+18003662255` Amazon Logistics).
  3. Simulate or receive an incoming call from a verified financial/business institution (e.g. `+18009359935` Chase Bank) or a saved contact.
  4. Observe the in-call screen and call history log list.
- **Expected Result**:
  - High-risk spam callers display a red **Spam Risk** badge.
  - Delivery and logistics callers display an amber **Priority Delivery** badge.
  - Verified businesses and saved contacts display a green **Verified Caller** badge.
  - Badges render with high-contrast Material 3 containers on both the In-Call screen and Call Log items.

### 3. Task 10.3 (Phase 10): Android 14+ Telecom VoIP Continuity
- [ ] **To Test**
- **Test Steps**:
  1. Initiate an outgoing or incoming simulated/WhatsApp VoIP call on an Android 14+ device.
  2. Connect a Bluetooth headset or car audio system while call is active.
  3. Toggle call mute, place call on hold (inactive), and resume.
  4. Hang up the call from the in-call screen.
- **Expected Result**:
  - `TelecomManager.addCall()` cleanly integrates into the system-level audio endpoint router without dropping audio channels.
  - Bluetooth device transitions, mute state, and disconnect actions execute reliably and release system audio locks.

### 4. Task 11.4 (Phase 11): Dynamic Audio Routing (Auto-Speakerphone & Mic-Muting)
- [ ] **To Test**
- **Test Steps**:
  1. Configure a rule with **Auto-Speakerphone** enabled and **Auto-Mute Mic** enabled.
  2. Trigger the rule with an incoming call.
  3. When the call is auto-answered, observe the in-call audio route.
  4. Observe the microphone mute status while the DTMF sequence is transmitted.
  5. Check that call auto-hangs up after the configured duration.
- **Expected Result**:
  - Call audio routes directly to speakerphone upon answer without requiring manual toggle.
  - Microphone is automatically muted during DTMF transmission to prevent acoustic ambient noise interference at entry buzzers/IVRs.
  - Call cleanly terminates automatically.

### 5. Task 10.5 (Issue #39): In-Call Screen Priority & Modal Auto-Dismiss on External Outgoing Calls
- [ ] **To Test**
- **Test Steps**:
  1. In OmniDial, open any contact's `ContactDetailsBottomSheet` in Contacts, Favorites, or Recents (or open a rule dialog in Rules).
  2. Switch to another app (e.g. Chrome, Google Maps) and tap a telephone number link or place a call via Bluetooth / Google Assistant.
  3. Observe OmniDial being brought to the foreground for the outgoing call.
  4. Also test when permission prompts or dialogs are active and an incoming call arrives.
- **Expected Result**:
  - All open bottom sheets (`ContactDetailsBottomSheet`), dialogs (`SimChoiceDialog`, `WhatsAppChoiceDialog`, `CloudContactSyncDialog`, `RuleEditDialog`), and prompt overlays are immediately dismissed.
  - Active `InCallScreen` is surfaced with 100% priority and zero visual obstruction.

### 6. Task 10.7: Missed Call Notification Auto-Scroll & Highlight Pulse
- [ ] **To Test**
- **Test Steps**:
  1. Trigger a missed call (either simulated incoming call left unanswered or an actual phone call).
  2. Pull down the Android system notification drawer and tap the Missed Call notification.
  3. Observe OmniDial opening Recents (Tab 1).
  4. Verify the list automatically scrolls smoothly (`listState.animateScrollToItem()`) directly to the target missed call card.
  5. Verify the card displays an animated primary highlight container and border stroke, which gracefully fades out over 3.5 seconds.
- **Expected Result**:
  - Tapping missed call notification navigates to Recents, smoothly scrolls to the exact entry, and pulses the highlight before fading back cleanly.

---

## 🔍 Open Issues & Feedback Items Under Investigation

1. [x] **Task 5 (Phase 3)**: Fresh install call log & popular section import behavior — **VERIFIED FIXED**.
2. [x] **Task 7 (Phase 3) / Task 10.7**: Missed Call notification deep link navigation & auto-scroll highlighting — **VERIFIED FIXED** (auto-target, smooth scroll, and 3.5s pulse fade implemented).
3. [x] **Task 19 (Phase 6)**: WhatsApp outgoing call log entries in Recents visual badge/icon — **VERIFIED FIXED** (green WhatsApp indicator displays cleanly).
4. [x] **Task 29 (Phase 6)**: Keyboard auto-opening behavior on app startup — **VERIFIED FIXED** (stays dismissed on launch).
5. [x] **Task 20 (v1.4.2)**: Rule Editor UX overhaul, sticky Save button, and IME auto-scroll — **VERIFIED FIXED**.
