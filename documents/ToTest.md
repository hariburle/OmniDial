# OmniDial — Active Testing Checklist (`ToTest.md`)

Items currently pending verification or undergoing testing. Verified items are archived in `documents/Verified.md`.

---

## 📋 Active Items To Test

### 0. Task 15.1 (Release 2.0.2): Multi-Call & Conference Calling Subsystem
- [ ] **To Test**
- **Test Steps**:
  1. While in an active call, tap **Add Call** and dial a second contact or number.
  2. Verify the first call is placed on hold and the second call connects.
  3. Tap **Swap** to toggle between the active and held call. Verify audio and UI switch cleanly.
  4. Tap **Merge** to combine the two calls into a single conference.
  5. Verify the participant list card appears showing both parties with individual status.
  6. Tap **Hold Conf** to put the entire conference on hold; tap again to resume.
  7. In the participant card, tap disconnect next to one participant. Verify only that participant is dropped and the other remains active.
- **Expected Result**:
  - Full multi-call swap, merge, hold/resume, and participant-level disconnect function smoothly without dropping the main call session.

### 0.1 Task 15.2 (Release 2.0.2): Floating Call Pill & PiP Auto-Dismiss
- [ ] **To Test**
- **Test Steps**:
  1. Start or receive a call.
  2. Minimize the call screen or press Home to trigger the floating pill or Picture-in-Picture window.
  3. Disconnect the call from either end (local hangup or remote caller disconnect).
  4. Verify the floating pill and PiP window cleanly and immediately dismiss without lingering on screen.
- **Expected Result**:
  - Zero stuck floating widgets or PiP overlays after call termination.

### 0.2 Task 15.3 (Release 2.0.2): Just-in-Time Contextual Permission Reminders
- [ ] **To Test**
- **Test Steps**:
  1. If contacts permission is revoked or skipped in wizard, navigate to **Contacts**.
  2. Verify an inline warning banner appears at the top and an empty state card appears prompting to enable contacts. Tap **Enable Contacts** and verify permission request triggers.
  3. If call log permission is missing, navigate to **Recents**. Verify banner and empty state card appear with **Grant** button.
  4. If Call Redirection role is missing, navigate to **Rules**. Verify "Call Redirection Not Active" banner appears with **Enable** button.
- **Expected Result**:
  - Missing permissions/roles are contextually surfaced right where users need them with 1-tap resolution.

### 0.3 Task 15.4 (Release 2.0.2): Settings Permissions Hub & Setup Wizard Rerun
- [ ] **To Test**
- **Test Steps**:
  1. Open **Settings** and locate the **App Permissions & Setup** hub card.
  2. Verify all 6 setup wizard options are listed with accurate live status chips ("Active" vs "Enable").
  3. Tap "Enable" on any pending permission/role. Verify its respective system dialog opens.
  4. Tap **Rerun Setup Wizard**. Verify the full guided walkthrough reopens with all steps unskipped.
  5. Tap **Open Android App Settings**. Verify system app details settings page opens.
- **Expected Result**:
  - Live status reflection, direct step triggers, full wizard replay, and deep link to system settings.

### 1. Task 14.1 (Release 2.0.1): Caller ID & Spam Default Role & Screening Policy
- [ ] **To Test**
- **Test Steps**:
  1. Open **Settings** and find the **Caller ID & Spam** card. Verify it shows "Action Needed".
  2. Tap **Set as Caller ID & Spam App**. Verify the system role dialog appears and grants the role.
  3. Verify the card flips to "Active" and OmniDial appears under Android **Settings → Apps → Default apps → Caller ID & spam app**.
  4. Add a test number to the spam blocklist (leave "Block spam automatically" OFF). Call it from another phone.
  5. Verify the call is silenced (not declined), appears as a missed call in Recents with an accurate badge, and a notification is shown.
  6. Toggle "Block spam automatically" ON and repeat. Verify the call is rejected outright.
  7. Whitelist the number (or save it as a contact) and repeat. Verify it rings normally.
- **Expected Result**:
  - Screening silences/logs by default and never silently drops wanted calls; the legacy `block_telemarketers_robocalls` auto-decline does not double-handle spam-list numbers while the role is held.


### 1. Task 13.1 (Release 2.0): Dynamic Keypad Channel Dock & Channel Switching
- [ ] **To Test**
- **Test Steps**:
  1. Open the **Keypad** tab (Tab 3).
  2. Observe the Channel Dock directly above the dial pad (`[SIM 1]`, `[SIM 2]`, `[WhatsApp]`).
  3. Verify SIM pills display user/carrier labels and amber badge if on roaming.
  4. Tap the WhatsApp pill to set WhatsApp as the active calling channel.
  5. Enter a phone number and tap Call. Verify WhatsApp voice call intent launches.
  6. Tap SIM 1 or SIM 2 and dial. Verify cellular call initiates on the selected subscription.
- **Expected Result**:
  - The Channel Dock provides frictionless, 1-tap channel switching directly from the keypad with dynamic status indicators.

### 2. Task 13.2 (Release 2.0): Unified Multi-Channel Choice Dialog & "Remember Choice" Workflow
- [ ] **To Test**
- **Test Steps**:
  1. In Contacts or Favorites, tap Call on a contact with no saved channel preference.
  2. In the `MultiChannelChoiceDialog`, check "Remember choice for this contact".
  3. Select "WhatsApp Voice" or "SIM 2".
  4. Verify the call launches on the selected channel.
  5. Return to the app and tap Call on the same contact again.
- **Expected Result**:
  - The call immediately places via the remembered channel without prompting.
  - Tapping the contact sheet shows the updated preference pill, which can be cleared via ✕.

### 3. Task 13.3 (Release 2.0): Transactional Backup & Live Restore Progress Bar
- [ ] **To Test**
- **Test Steps**:
  1. Open **Settings** → **Backup & Restore**.
  2. Tap "Backup Now" and verify backup completes with SHA-256 integrity hash.
  3. Tap Restore on a saved backup.
  4. Observe the restore progress dialog showing real-time section progress (Rules, Favorites, Channels, Settings).
  5. Cancel or interrupt restore halfway (or simulate failure) to verify database is not left in an inconsistent state.
- **Expected Result**:
  - Room `@Transaction` executes batch operations atomically; progress bar updates smoothly without UI freezes.

### 4. Task 13.4 (Release 2.0): Hot-Path Search (<16ms) and Contact Default Number Prioritization
- [ ] **To Test**
- **Test Steps**:
  1. Open Keypad or Contacts on a device with >2,000 contacts.
  2. Rapidly type digits or names.
  3. Measure keystroke responsiveness and frame rendering.
  4. Open a multi-number contact and set a default number.
  5. Check Android system contacts and Room DB.
- **Expected Result**:
  - Keystroke latency remains strictly under 16ms with zero UI jank.
  - Default number is prioritized at the top with `DEFAULT` chip and synchronized to system contacts.

### 5. Task 13.5 (Release 2.0): Telecom Callback Cleanup & Zero-Leak Teardown
- [ ] **To Test**
- **Test Steps**:
  1. Place and receive multiple consecutive calls (cellular and WhatsApp).
  2. Hang up each call and inspect logcat for `CallManager.unregisterCallback`.
  3. Verify notification is cancelled once and no duplicate `handleCallEnded` events fire.
- **Expected Result**:
  - No memory leaks of `InCallService` context, clean callback teardown, and single notification dismissal.

### 6. Task 12.1 (Phase 12): Partitioned Contact Search Outside Active Filter
- [ ] **To Test**
- **Test Steps**:
  1. Open the **Contacts** tab (Tab 4).
  2. Select an active filter chip (e.g. "Nicknames" or "Favorites"). Verify list only shows qualifying contacts.
  3. Type a search query for a contact name that exists in the phonebook but does NOT have a nickname or is NOT starred in favorites.
  4. Verify the search results view:
     - Top section displays qualifying filter matches (if any).
     - Below, a dedicated section appears: `"Other Matches Outside Filter (N)"`.
     - Each item displays the contact name, phone numbers, and a contextual reason badge (e.g., `[No Nickname]`, `[Not in Favs]`).
  5. Tap an item in the partitioned section to open its `ContactDetailsBottomSheet`.
  6. Confirm you can add a nickname or favorite star directly from the sheet, and the contact immediately shifts into the primary filtered section.
- **Expected Result**:
  - Contacts are never hidden by active filters during search. Non-qualifying contacts are cleanly partitioned with 1-tap sheet access.

### 2. Task 12.2 (Phase 12): Ambient Incoming Call Ring Silencing on Lift & Screen Interaction
- [ ] **To Test**
- **Test Steps**:
  1. Receive an incoming phone call on the device while it is placed flat on a table (or simulate an incoming ringing call).
  2. Notice the phone ringing out loud.
  3. Pick up the phone from the table (or uncover the proximity sensor).
  4. Verify the loud ringer is immediately silenced, while the screen remains on `STATE_RINGING` with full answer/decline/audio controls.
  5. In a second incoming call test, leave the phone on the table and tap anywhere on the screen background, tap the audio route selector bar, or tap a quick decline SMS chip.
  6. Verify the ringer is immediately silenced while the call remains ringing.
  7. In a third incoming call test, tap the dedicated `[ 🔕 Silence ]` top status chip. Verify it turns to `[ Silenced ]` and stops the ringer.
  8. In a fourth test, press the physical volume up or volume down button during an incoming call. Verify ringer is silenced.
- **Expected Result**:
  - `TelecomManager.silenceRinger()` is invoked seamlessly without altering system-wide ringer mode. Call stays active in `STATE_RINGING` until user decides.

### 3. Task 10.1 (Phase 10): Roaming-Aware & DAG Conflict-Resolved SIM Routing
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

### 8. Conference calling: Add call / Merge / Swap / Manage participants
- [ ] **To Test**
- **Test Steps**:
  1. Place a cellular call to person A; verify the in-call screen is locked to portrait and shows an **Add call** button (no Merge/Swap/Manage).
  2. Tap **Add call** → verify the chooser has **Keypad** and **Contacts** tabs; dial person B from the keypad tab.
  3. Verify person A is put on hold, person B dials, and the in-call UI now tracks the second call (primary).
  4. If the carrier supports it, verify **Merge** appears; tap it and verify both calls merge into one conference (header shows "Conference" + participant count, SIM badge hidden, Manage button appears).
  5. Tap **Manage** → verify both participants are listed; end one participant from the list and verify the other stays connected.
  6. With two unmerged calls (active + held), verify **Swap** appears if the carrier supports it; tap and verify the active/held calls switch.
  7. During a merged conference, verify **Add call** is still available.
  8. End the conference; verify each participant's call is logged exactly once in Recents (no duplicate or blank conference-parent entries).
  9. With two unmerged calls, hang up the second (primary) call; verify the first (held) call is promoted back to the in-call UI instead of showing a dead post-call screen.
- **Expected Result**:
  - Add call, merge, swap, and per-participant end/split all work where the carrier supports them; unsupported buttons stay hidden.
  - Recents shows one entry per real participant call; ending one call never strands the UI on a dead screen while another call is live.

### 9. Post-call auto-close and minimized-call popup dismissal
- [ ] **To Test**
- **Test Steps**:
  1. End a call without touching the post-call note UI; verify the in-call screen auto-closes after ~5 seconds.
  2. End a call, tap the note field (but type nothing); verify the screen stays up ~60 seconds after the last interaction, then closes.
  3. End a call with typed (unsaved) note text; verify the text is never auto-discarded and the screen closes at the 10-minute backstop at the latest.
  4. Minimize the in-call screen to the green popup, then end the call from the notification or the other side; verify the green popup disappears immediately (no stuck popup 20+ minutes later).
  5. Verify minimizing the call restores rotation (portrait lock released) and reopening the call re-locks portrait.
- **Expected Result**:
  - Untouched post-call screens close in 5s; interacted-but-blank in ~60s; typed notes are preserved until saved or the 10-minute cap.
  - The minimized green popup never survives a disconnected call; orientation returns to normal when the call UI is minimized or closed.

---

## 🔍 Open Issues & Feedback Items Under Investigation

1. [x] **Task 5 (Phase 3)**: Fresh install call log & popular section import behavior — **VERIFIED FIXED**.
2. [x] **Task 7 (Phase 3) / Task 10.7**: Missed Call notification deep link navigation & auto-scroll highlighting — **VERIFIED FIXED** (auto-target, smooth scroll, and 3.5s pulse fade implemented).
3. [x] **Task 19 (Phase 6)**: WhatsApp outgoing call log entries in Recents visual badge/icon — **VERIFIED FIXED** (green WhatsApp indicator displays cleanly).
4. [x] **Task 29 (Phase 6)**: Keyboard auto-opening behavior on app startup — **VERIFIED FIXED** (stays dismissed on launch).
5. [x] **Task 20 (v1.4.2)**: Rule Editor UX overhaul, sticky Save button, and IME auto-scroll — **VERIFIED FIXED**.
