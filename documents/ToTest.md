# OmniDial — Active Testing Checklist (`ToTest.md`)

Items currently pending verification or undergoing testing. Verified items are archived in `documents/Verified.md`.

---

## 📋 Active Items To Test

### 1. Task 12 (Phase 1): Keypad Dial Button Internal Highlight Geometry
- [ ] **VERIFIED**
- **Test Steps**:
  1. Navigate to the **Keypad** (Tab 2).
  2. Enter a phone number for which a preferred channel (Phone vs WhatsApp) is detected.
- **Expected Result**:
  - The preference highlight ring renders *inside* the circular dial button container.
  - The dial button outer dimensions stay locked to `56.dp`, and zero vertical jitter or key shifting occurs when entering digits.

### 2. Task 14 (Phase 6): Recents Search & Live Filtering
- [ ] **To Test**
- **Test Steps**:
  1. Open **Recents** (Tab 1).
  2. Type a caller name, number, or note in the top `"Search by name or number"` search bar.
- **Expected Result**:
  - Recents list filters dynamically in real time matching the search query.

### 3. Task 15 (Phase 6): Favorites Reorder Persistence & Material Card Hairline Border
- [ ] **To Test**
- **Test Steps**:
  1. On **Favorites** (Tab 0), tap **Configure** and drag-and-drop cards to reorder them.
  2. Restart the app and verify custom card order.
  3. Switch between Bento, Grid, and Material card formats.
- **Expected Result**:
  - Custom drag-and-drop card order persists across app restarts.
  - Material card style maintains an ultra-thin hairline border without heavy dark outlines.

### 4. Task 16 (Phase 6): Ask & Learn Preferred Channel Recomposition & Equal Contacts Icon Sizing
- [ ] **To Test**
- **Test Steps**:
  1. In **Contacts** (Tab 3), expand a contact's phone number options.
  2. Inspect the WhatsApp Call button vs Phone Call button.
- **Expected Result**:
  - Both Phone and WhatsApp call action buttons have 1:1 identical 36.dp button dimensions and 18.dp icons.
  - The preferred channel button displays a filled container background highlight without increasing icon size.

### 5. Task 17 (Phase 6): Configure Mode Popular Contact Ignore [X] Action
- [ ] **To Test**
- **Test Steps**:
  1. On **Favorites** (Tab 0), tap **Configure** mode.
  2. Locate a card in the **Popular** section and tap the prominent red **[X]** ignore button.
- **Expected Result**:
  - The popular contact is immediately ignored and removed from the popular list.

### 6. Task 18 (Phase 6): Horizontal Pager Smoothness & Sub-Tab Swipe Integration
- [ ] **To Test**
- **Test Steps**:
  1. Swipe horizontally between main panels at various speeds.
  2. In **Rules / Settings** (Tab 4), swipe horizontally between sub-tabs.
- **Expected Result**:
  - Main panel swiping is pre-rendered (`beyondViewportPageCount = 2`) and transitions smoothly without getting stuck midway.

### 7. Task 32 (Phase 7): External Outgoing Call Redirection Service
- [ ] **To Test**
- **Test Steps**:
  1. Set OmniDial as Default Phone App.
  2. Ensure "Display over other apps" permission is granted.
  3. Initiate an outgoing call from a connected Bluetooth vehicle head-unit, smartwatch, or assistant to a contact with WhatsApp preference.
- **Expected Result**:
  - `OmniCallRedirectionService` intercepts the cellular call and routes it directly to WhatsApp VoIP.

---

## 🔍 Open Issues & Feedback Items Under Investigation

1. **Task 5 (Phase 3)**: Fresh install call log & popular section import behavior when switching between development build environments.
2. **Task 7 (Phase 3)**: Missed Call notification deep link navigation & active call entry auto-scroll highlighting.
3. **Task 19 (Phase 6)**: WhatsApp outgoing call log entries in Recents — visual badge/icon differentiation.
4. **Task 29 (Phase 6)**: Keyboard auto-opening behavior on app startup across different panels.
