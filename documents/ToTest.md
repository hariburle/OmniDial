# OmniDial — Active Testing Checklist (`ToTest.md`)

Items currently pending verification or undergoing testing. Verified items are archived in `documents/Verified.md`.

---

## 📋 Active Items To Test

### 1. Task 15 (Phase 6): Favorites Reorder Persistence & Material Card Hairline Border
- [x] **Verified** TESTED
- **Test Steps**:
  1. On **Favorites** (Tab 0), tap **Configure** and drag-and-drop cards to reorder them.
  2. Restart the app and verify custom card order.
  3. Switch between Bento, Grid, and Material card formats in **Settings**.
- **Expected Result**:
  - Custom drag-and-drop card order persists across app restarts.
  - Material card style maintains an ultra-thin hairline border without heavy dark outlines.

### 2. Task 32 (Phase 7): External Outgoing Call Redirection Service
- [x] **Verified** TESTED
- **Test Steps**:
  1. Set OmniDial as Default Phone App.
  2. Ensure "Display over other apps" permission is granted.
  3. Initiate an outgoing call from a connected Bluetooth vehicle head-unit, smartwatch, or assistant to a contact with WhatsApp preference.
- **Expected Result**:
  - `OmniCallRedirectionService` intercepts the cellular call and routes it directly to WhatsApp VoIP.

### 3. Task 34 (Phase 8): Instant Keypad Switching & Zero Frame Drops (v1.1.5 Performance)
- [x] **Verified** TESTED
- **Test Steps**:
  1. Navigate to **Contacts** (Tab 3) with dozens or hundreds of device contacts loaded.
  2. Tap the bottom navigation bar or swipe directly to **Keypad** (Tab 2).
  3. Navigate to **Rules** (Tab 4) and tap back to **Keypad** (Tab 2).
  4. Repeat switching rapidly between Contacts, Rules, and Keypad.
- **Expected Result**:
  - Transition to Keypad is immediate (<50ms) with zero frame drops, stuttering, or UI freeze.
  - Keypad renders pre-warmed instantly with all speed dials, T9 candidate contacts, and recent calls ready.

### 4. Task 35 (Phase 8): One-Tap Direct Backup Creation (v1.1.4)
- [x] **Verified** TESTED (Note: Survives until reinstall; public storage persistence tracked in Issue #40)
- **Test Steps**:
  1. Open **Rules / Settings** (Tab 4) and scroll to the **Backup & Restore** card.
  2. Tap the **"Backup Now"** action button.
- **Expected Result**:
  - A new JSON backup file is created instantly without popping up complex system file picker prompts.
  - Success message displays showing the exact backup timestamp.
  - The new backup entry appears immediately in the "Available Backups" list with its timestamp and file size.

### 5. Task 36 (Phase 8): Automatic Backup Scan & One-Tap Restore (v1.1.4)
- [ ] **To Test**
- **Test Steps**:
  1. In the **Backup & Restore** card under "Available Backups", locate an existing backup entry.
  2. Tap the **"Restore"** icon button next to that backup.
  3. In the confirmation dialog, tap **"Restore"**.
- **Expected Result**:
  - Dialog confirms what will be restored (Rules, Speed Dials, Favorites, App Settings).
  - Data restores cleanly and screen updates immediately.
  - A confirmation notification confirms the successful restore.

### 6. Task 37 (Phase 8): Backup Deletion & Storage Management (v1.1.4)
- [ ] **To Test**
- **Test Steps**:
  1. In the **Backup & Restore** card, tap the **Delete (Trash)** icon on a listed backup.
  2. Confirm deletion in the safety prompt.
- **Expected Result**:
  - The backup file is permanently deleted from device storage.
  - The item vanishes immediately from the list with zero orphaned files left behind.

### 7. Task 38 (Phase 8): External Backup File Import & Cross-Device Transfer (v1.1.4)
- [ ] **To Test**
- **Test Steps**:
  1. In the **Backup & Restore** card, tap **"Browse Files"**.
  2. Select an `omnidial_backup_*.json` file from device Downloads or cloud drive.
- **Expected Result**:
  - File is safely parsed, validated, and copied into OmniDial's managed backup storage.
  - OmniDial restores rules, favorites, and speed dial settings from the external file.

### 8. Task 10.1 (Phase 10): Roaming-Aware & DAG Conflict-Resolved SIM Routing
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

### 9. Task 10.2 (Phase 10): Tiered Caller ID & Trust Badges
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

### 10. Task 10.3 (Phase 10): Android 14+ Telecom VoIP Continuity
- [ ] **To Test**
- **Test Steps**:
  1. Initiate an outgoing or incoming simulated/WhatsApp VoIP call on an Android 14+ device.
  2. Connect a Bluetooth headset or car audio system while call is active.
  3. Toggle call mute, place call on hold (inactive), and resume.
  4. Hang up the call from the in-call screen.
- **Expected Result**:
  - `TelecomManager.addCall()` cleanly integrates into the system-level audio endpoint router without dropping audio channels.
  - Bluetooth device transitions, mute state, and disconnect actions execute reliably and release system audio locks.

### 11. Task 11.1 (Phase 11): Modernized Automation Pipeline & Quick-Start Gallery
- [ ] **To Test**
- **Test Steps**:
  1. Navigate to **Rules** (Tab 4).
  2. Inspect the horizontal **Quick-Start Recipes** carousel at the top (Gate Buzzer, Delivery Gate, SMS Responder).
  3. Tap **"Gate Buzzer"** recipe. Verify dialog opens pre-filled with recommended actions (Auto-Answer 1s, Speakerphone ON, Mute Mic ON, DTMF 9#, Auto-Hangup 2s).
  4. Enter a test phone number pattern (e.g. `+1555*`) and tap **Save**.
  5. Inspect the saved rule card: verify the visual execution pipeline chips (`Ring` ➔ `Delay` ➔ `Answer` ➔ `Speaker` ➔ `Mute` ➔ `DTMF` ➔ `Hangup`).
  6. Tap the **Duplicate** icon button on the rule card. Verify a cloned copy named `... (Copy)` appears immediately.
  7. Tap the **History** icon button in the top app bar to view the execution log sheet.
- **Expected Result**:
  - Recipes pre-fill valid, tested automation pipelines in 1 tap.
  - Rule card visualizes the sequence of triggers and actions with colored Material 3 chips.
  - Rule duplication clones all actions and constraints accurately.
  - History sheet opens cleanly and displays recent trigger activity.

### 12. Task 11.2 (Phase 11): Interactive Dry-Run Rule Simulator
- [ ] **To Test**
- **Test Steps**:
  1. On any rule card in **Rules** (Tab 4), tap the **"Test Rule"** (beaker/flask) action button.
  2. In the Rule Simulation bottom sheet, review the listed execution steps.
  3. Tap **"Start Simulation"**.
  4. Watch the step-by-step simulated progress bar and real-time execution node highlights.
  5. Observe simulated answering, speakerphone engagement, mic mute, DTMF tone delivery, and auto-hangup.
- **Expected Result**:
  - Simulator runs through all rule actions with visual countdown timers.
  - DTMF tones play audibly if tone playback is supported.
  - Successful dry-run displays a green completion banner and logs the simulated event into history.

### 13. Task 11.3 (Phase 11): Ambient Geofencing (Wi-Fi SSID & Bluetooth Device Constraints)
- [ ] **To Test**
- **Test Steps**:
  1. In **Rules** (Tab 4), tap **Edit** on a rule or create a new rule.
  2. In the **"Ambient Geofence & Device Guards"** section, enter your current connected Wi-Fi SSID (e.g. `Home_WiFi`) in the Wi-Fi field.
  3. Save the rule. Verify the rule card displays the `📶 Home_WiFi` badge.
  4. Simulate or receive an incoming call while connected to that Wi-Fi network -> Verify rule executes.
  5. Disconnect Wi-Fi (or edit rule to require an unmatched SSID like `Unknown_WiFi`) and receive/simulate a call -> Verify rule does **NOT** trigger.
  6. Repeat with Bluetooth device name (e.g. `CarKit` or `AirPods`) and verify the rule only fires when connected to the designated Bluetooth peripheral.
- **Expected Result**:
  - Zero-battery ambient geofencing guards rule execution based on immediate physical environment.
  - Rule conditions fail gracefully if ambient constraints are not satisfied, preventing unintended automation outside target locations.

### 14. Task 11.4 (Phase 11): Dynamic Audio Routing (Auto-Speakerphone & Mic-Muting)
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

### 15. Task 11.5 (Phase 11): Backup & Restore of Ambient Automation Fields
- [ ] **To Test**
- **Test Steps**:
  1. In **Rules** (Tab 4), create rules with Wi-Fi SSID, Bluetooth Device, Auto-Speakerphone, and Auto-Mute Mic configured.
  2. In **Backup & Restore**, tap **"Backup Now"**.
  3. Delete the rule from the list.
  4. Tap **"Restore"** on the created backup.
- **Expected Result**:
  - Backup file preserves all ambient geofence fields and audio routing flags.
  - Restored rule retains all exact settings: Wi-Fi SSID, Bluetooth Device, Auto-Speakerphone, and Auto-Mute Mic.

### 16. Task 10.5 (Issue #39): In-Call Screen Priority & Modal Auto-Dismiss on External Outgoing Calls
- [ ] **To Test**
- **Test Steps**:
  1. In OmniDial, open any contact's `ContactDetailsBottomSheet` in Contacts, Favorites, or Recents (or open a rule dialog in Rules).
  2. Switch to another app (e.g. Chrome, Google Maps) and tap a telephone number link or place a call via Bluetooth / Google Assistant.
  3. Observe OmniDial being brought to the foreground for the outgoing call.
  4. Also test when permission prompts or dialogs are active and an incoming call arrives.
- **Expected Result**:
  - All open bottom sheets (`ContactDetailsBottomSheet`), dialogs (`SimChoiceDialog`, `WhatsAppChoiceDialog`, `CloudContactSyncDialog`, `RuleEditDialog`), and prompt overlays are immediately dismissed.
  - Active `InCallScreen` is surfaced with 100% priority and zero visual obstruction.

### 17. Task 10.6 (Issue #40): Public Storage MediaStore Backup Persistence (Survive Clean Reinstalls)
- [ ] **To Test**
- **Test Steps**:
  1. In **Rules / Settings** (Tab 4), scroll to **Backup & Restore** and tap **"Backup Now"**.
  2. Confirm the backup file appears in "Available Backups".
  3. Open a file explorer app on the device and navigate to `Documents/OmniDial/` — verify `omnidial_backup_*.bak` exists in public storage.
  4. Uninstall OmniDial completely from the device (`adb uninstall com.example` or drag to uninstall).
  5. Re-install OmniDial.
  6. Open OmniDial and navigate to **Rules / Settings** (Tab 4) -> **Backup & Restore**.
- **Expected Result**:
  - OmniDial automatically queries public MediaStore on startup and mirrors the preserved backups from `Documents/OmniDial/`.
  - The backup is immediately visible in the "Available Backups" list with its original timestamp and file size.
  - Tapping **"Restore"** cleanly restores all rules, speed dials, favorites, and settings.

### 18. Task 10.7: Missed Call Notification Auto-Scroll & Highlight Pulse
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
