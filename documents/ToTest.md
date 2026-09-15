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

---

## 🔍 Open Issues & Feedback Items Under Investigation

1. **Task 5 (Phase 3)**: Fresh install call log & popular section import behavior when switching between development build environments.
2. **Task 7 (Phase 3)**: Missed Call notification deep link navigation & active call entry auto-scroll highlighting.
3. **Task 19 (Phase 6)**: WhatsApp outgoing call log entries in Recents — visual badge/icon differentiation.
4. **Task 29 (Phase 6)**: Keyboard auto-opening behavior on app startup across different panels.
