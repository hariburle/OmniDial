# OmniDial — Active Testing Checklist (`ToTest.md`)

Items currently pending verification or undergoing testing. Verified items are archived in `documents/Verified.md`.

---

## 📋 Active Items To Test

### 1. Task 15 (Phase 6): Favorites Reorder Persistence & Material Card Hairline Border
- [ ] **To Test**
- **Test Steps**:
  1. On **Favorites** (Tab 0), tap **Configure** and drag-and-drop cards to reorder them.
  2. Restart the app and verify custom card order.
  3. Switch between Bento, Grid, and Material card formats in **Settings**.
- **Expected Result**:
  - Custom drag-and-drop card order persists across app restarts.
  - Material card style maintains an ultra-thin hairline border without heavy dark outlines.

### 2. Task 32 (Phase 7): External Outgoing Call Redirection Service
- [ ] **To Test**
- **Test Steps**:
  1. Set OmniDial as Default Phone App.
  2. Ensure "Display over other apps" permission is granted.
  3. Initiate an outgoing call from a connected Bluetooth vehicle head-unit, smartwatch, or assistant to a contact with WhatsApp preference.
- **Expected Result**:
  - `OmniCallRedirectionService` intercepts the cellular call and routes it directly to WhatsApp VoIP.

### 3. Task 34 (Phase 8): Instant Keypad Switching & Zero Frame Drops (v1.1.5 Performance)
- [ ] **To Test**
- **Test Steps**:
  1. Navigate to **Contacts** (Tab 3) with dozens or hundreds of device contacts loaded.
  2. Tap the bottom navigation bar or swipe directly to **Keypad** (Tab 2).
  3. Navigate to **Rules** (Tab 4) and tap back to **Keypad** (Tab 2).
  4. Repeat switching rapidly between Contacts, Rules, and Keypad.
- **Expected Result**:
  - Transition to Keypad is immediate (<50ms) with zero frame drops, stuttering, or UI freeze.
  - Keypad renders pre-warmed instantly with all speed dials, T9 candidate contacts, and recent calls ready.

### 4. Task 35 (Phase 8): One-Tap Direct Backup Creation (v1.1.4)
- [ ] **To Test**
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

---

## 🔍 Open Issues & Feedback Items Under Investigation

1. **Task 5 (Phase 3)**: Fresh install call log & popular section import behavior when switching between development build environments.
2. **Task 7 (Phase 3)**: Missed Call notification deep link navigation & active call entry auto-scroll highlighting.
3. **Task 19 (Phase 6)**: WhatsApp outgoing call log entries in Recents — visual badge/icon differentiation.
4. **Task 29 (Phase 6)**: Keyboard auto-opening behavior on app startup across different panels.
