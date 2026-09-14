# OmniDial — Interactive Testing Checklist (`ToTest.md`)

Mark `[x]` on the checkbox for each task once you verify it. Each section below includes the exact test steps and expected results.

## 📋 Quick Checkpoints
- [TESTED] **Task 1 (Phase 1)** — Bento/Grid/Material card names & thin Light theme borders
- [TESTED] **Task 2 (Phase 1)** — Search placeholder `"Search by name or number"` & Bento/Slide defaults
- [TESTED] **Task 12 (Phase 1)** — Keypad dial button highlight ring inside circle (no key movement)
- [TESTED] **Task 4 (Phase 2)** — Dialog overhaul (rounded corners, avatar monograms, clean styling)
- [TESTED] **Task 8 (Phase 2)** — Recents `"Recents"` naming, `PersonAdd (+)` avatar & `"Add to Contacts"` workflow
- [TESTED] **Task 3 (Phase 3)** — Zero initial 4-card flash in Favorites on app launch
- [TESTED] **Task 5 (Phase 3)** — Fresh install call log import & Auto-Backup persistence across reinstalls. ISSUE: after a fresh install call log and popular sections are empty, this is not an issue when a new build is made using the same tool AI Studio or Android Studio but when a fresh build is made when i switch the development tool.
- [TESTED] **Task 7 (Phase 3)** — Missed call notification deep-link & auto-scroll highlight. ISSUE:  Missed Call notification deep link not working , it goes to the recents panel but not highlight the actual call entry 
- [TESTED] **Task 13 (Phase 3)** — Instant configure-mode favorite deletion persistence
- [TESTED] **Task 6 (Phase 4)** — Bento `"Phone"` label, `"Ask and Learn"` dual dialers & WhatsApp call logging in Recents
- [TESTED] **Task 11 (Phase 4)** — Dedicated Spam & Blocked window + `"International Numbers"` setting label
- [TESTED] **Task 9 (Phase 5)** — Swipe left/right to switch between main panels
- [ ] **Task 10 (Phase 5)** — Secondary item-level swipe gestures when panel swiping is disabled
- [TESTED] **Task 14 (Phase 6)** — Recents Search & Live Filtering by name, number, or note
- [TESTED] **Task 15 (Phase 6)** — Favorites Reorder Persistence & Material Card Hairline Border
- [TESTED] **Task 16 (Phase 6)** — Ask & Learn Preferred Channel Recomposition & Equal Contacts Icon Sizing
- [TESTED] **Task 17 (Phase 6)** — Configure Mode Popular Contact Ignore `[X]` Button
- [TESTED] **Task 18 (Phase 6)** — Horizontal Pager Transition Smoothness & Sub-Tab Navigation
- [TESTED] **Task 19 (Phase 6)** — WhatsApp Outgoing Call Logging in Recents. ISSUE: while the whatsapp outgoing call entries are listed, not able to differentiate that its a whatsapp call.
- [TESTED] **Task 20 (Phase 6)** — Phone Contact vs Local Contact Edit Routing
- [TESTED] **Task 21 (Phase 6)** — Popular Contact Favoriting Nickname Auto-Lookup
- [TESTED] **Task 22 (Phase 6)** — Dual Dialers Across All Favorite Card Styles (Bento, Grid, Material)
- [TESTED] **Task 23 (Phase 6)** — Keypad Hybrid Action Buttons (`[ 📞 Phone Call ]` & `[ 💬 WhatsApp ]`)
- [TESTSED] **Task 24 (Phase 6)** — Rules FAB Cleanup & "Never" Mode Choice Preservation
- [TESTED] **Task 25 (Phase 6)** — Dynamic Star Icon Unstarring Reactive Sync
- [TESTED] **Task 26 (Phase 6)** — Favorites Header Cleanup (Removed redundant header line)
- [TESTED] **Task 27 (Phase 6)** — Settings Card Design Sample Previews Update (Dual Dialers)
- [TESTED] **Task 28 (Phase 6)** — Recents Category Filters & `🤖 Rule` Badges
- [TESTED] **Task 29 (Phase 6)** — Startup Panel & Bottom Navigation Bar Tray Sync, ISSUE: When I open the app the keyboard opens assuming I am going to type, why ? What's the best experience? I can understand that in Contacts but not in recents or favorites page 

---

## 🧪 Detailed Test Scripts & Verification Steps

### 1. Task 1 (Phase 1): Favorite Card Style Refinements
- [x] **Verified** TESTED
- **Test Steps**:
  1. Open the app and navigate to **Rules / Settings** (Tab 4).
  2. Scroll down to the **Favorite Card Style** section.
  3. Verify style names are concise: **Bento**, **Grid**, and **Material**.
  4. Select **Material** or **Grid** style in Light Theme and switch to the **Favorites** tab.
- **Expected Result**:
  - Style names in Settings/Rules are short and clean (**Bento**, **Grid**, **Material**).
  - Card borders in Light Theme display an ultra-thin `0.5.dp` hairline outline without heavy/thick dark borders.

### 2. Task 2 (Phase 1): Universal Search Placeholder & Default Settings
- [x] **Verified** TESTED
- **Test Steps**:
  1. Inspect search fields in **Favorites** (Tab 0), **Contacts** (Tab 3), and **Contact Picker Dialog** (when adding a favorite/rule).
  2. Clear app data or inspect default settings in **Settings** (Tab 4).
  3. Force-close and re-open the app.
- **Expected Result**:
  - Search field placeholders consistently read: `"Search by name or number"`.
  - Default settings are pre-configured:
    - **Favorite Card Theme**: Bento
    - **WhatsApp Calling**: Ask & Learn
    - **Call Answering Style**: Horizontal Slide
  - App opens directly to the **Favorites** panel (Tab 0) by default.

### 3. Task 12 (Phase 1): Keypad Dial Button Internal Highlight Geometry
- [] **Verified** 
- **Test Steps**:
  1. Navigate to the **Keypad** (Tab 2).
  2. Enter a phone number for which a preferred channel (Phone vs WhatsApp) is detected.
- **Expected Result**:
  - The preference highlight ring renders *inside* the circular dial button container.
  - The dial button outer dimensions stay locked to `56.dp`, and zero vertical jitter or key shifting occurs when entering digits.

---

### 4. Task 4 (Phase 2): Comprehensive Pop-up & Dialog Overhaul
- [x] **Verified** TESTED
- **Test Steps**:
  1. Open **Contacts** (Tab 3) and tap **+ New Contact**.
  2. Open **Recents** (Tab 1) or **Favorites** (Tab 0) and tap a contact row to open **Contact Details Bottom Sheet**.
  3. Edit a favorite contact or trigger the Audio Output Selector during a call.
- **Expected Result**:
  - Dialogs feature modern `24.dp`/`28.dp` rounded corners, pleasant surface container tints, leading icons (`Person`, `Phone`, `Label`), monogram avatar previews, and responsive primary buttons.

### 5. Task 8 (Phase 2): Recents List Unknown Number '+' Action & "Recents" Naming
- [x] **Verified** TESTED
- **Test Steps**:
  1. Check bottom navigation bar label and header for Tab 1.
  2. Locate an unknown/unsaved number in **Recents** (Tab 1) and tap the row to view details.
  3. Tap the **PersonAdd (+)** avatar or the **"Add to Contacts"** primary button.
  4. Save the contact with a name in `CreateContactDialog`.
- **Expected Result**:
  - Navigation tab is named **"Recents"** (plural).
  - Unknown number details view displays a `PersonAdd` avatar with a `+` badge and a prominent `"Add to Contacts"` button.
  - Tapping `"Add to Contacts"` opens `CreateContactDialog` pre-filled with the phone number, updating the caller name across the app immediately upon saving.

---

### 6. Task 3 (Phase 3): Eliminate Initial 4-Card Flash in Favorites
- [x] **Verified** TESTED
- **Test Steps**: 
  1. Force-close the app and re-launch it on the **Favorites** tab.
  2. Observe the initial screen render before database emissions complete.
- **Expected Result**:
  - No 4 dummy/popular cards flash before real favorites load.
  - If no favorites are starred, the screen displays a clean, static *"No Favorites Added Yet"* empty layout.

### 7. Task 5 (Phase 3): Fresh Install Call Log Import & Auto-Backup
- [x] **Verified** TESTED
- **Test Steps**:
  1. Install a fresh build or clear app data with `READ_CALL_LOG` permission granted.
  2. Open the **Recents** tab immediately upon first launch.
  3. Reinstall or update the app on a device with Google Auto-Backup.
- **Expected Result**:
  - System call history from `CallLog.Calls` automatically seeds into Room SQLite, populating the Recents list immediately on fresh install.
  - Learned choices and ignored popular contacts persist across app updates/reinstalls via Android Auto-Backup rules (`backup_rules.xml` & `data_extraction_rules.xml`).

### 8. Task 7 (Phase 3): Missed Call Notification Deep-Link & Highlighting
- [] **Verified** 
- **Test Steps**:
  1. Simulate an unanswered incoming call (or generate a missed call notification).
  2. Tap the missed call notification from the Android notification shade.
- **Expected Result**:
  - Tapping the notification opens the app directly to the **Recents** tab (Tab 1).
  - The screen auto-scrolls to the missed call item and highlights it with a primary container border.

### 9. Task 13 (Phase 3): Persist Favorite Card Deletion in Configure Mode
- [x] **Verified**
- **Test Steps**:
  1. On the **Favorites** tab, tap the **Configure (Tune)** button to enter edit mode.
  2. Tap the **Delete (Trash)** icon on a favorite card.
  3. Refresh the screen or restart the app.
- **Expected Result**:
  - The card is removed instantly from local state and deleted/unmarked in Room SQLite without popping up blocking prompts or reappearing on refresh/restart.

---

### 10. Task 6 (Phase 4): Bento Style "Phone" Labeling, "Ask & Learn" Dual Dialers & WhatsApp Call Logging
- [x] **Verified**
- **Test Steps**:
  1. Set **Favorite Card Style** to **Bento** in Settings/Rules.
  2. Set **WhatsApp Calling** to **Ask and Learn** in Settings/Rules.
  3. View a Bento favorite card for a contact with no learned channel preference.
  4. Trigger a WhatsApp call from the app (via Bento card, Contact sheet, or Dialer).
  5. Return to the app and check the **Recents** tab.
- **Expected Result**:
  - Cellular calling button label is **"Phone"** (not "Direct Call").
  - For contacts with undetermined preference ("Ask and Learn"), Bento cards display side-by-side **dual buttons** (**Phone** & **WhatsApp**).
  - Every WhatsApp call initiated from the app is immediately logged into **Recents** with caller name, number, timestamp, and a `"WhatsApp Call"` badge.

### 11. Task 11 (Phase 4): Dedicated Spam Window & International Numbers Option
- [x] **Verified**
- **Test Steps**:
  1. Open **Rules / Settings** (Tab 4).
  2. Inspect the **WhatsApp Calling Integration Mode** options.
  3. Locate the **Protection & Spam** entry card and check the indicator badge (e.g. `"{X} blocked"`).
  4. Tap **Spam & Blocked Calls** to open the dedicated window.
  5. Test searching blocked numbers, tapping **Block** to add a number, and tapping **Unblock**.
- **Expected Result**:
  - International WhatsApp option in Settings is named **"International Numbers"** with the description: *"Directs numbers outside your country (+1 for US, +91 for India, etc.) to WhatsApp automatically."*
  - The long embedded spam list in Settings is replaced with a clean, single entry row.
  - Tapping the entry opens `SpamManagementDialog`, allowing complete management of blocked numbers, search, manual blocking, auto-block carrier spam toggles, and unblocking.

---

### 12. Task 9 (Phase 5): Swipe to Switch Main Panels
- [x] **Verified**
- **Test Steps**:
  1. Ensure **"Swipe to switch panels"** is **Enabled** in **Rules / Settings** (Tab 4).
  2. Horizontally swipe left or right across the screen on any main panel (**Favorites** <-> **Recents** <-> **Keypad** <-> **Contacts** <-> **Settings**).
- **Expected Result**:
  - Smooth horizontal swipe navigation seamlessly switches between the 5 main app panels.

### 13. Task 10 (Phase 5): Secondary Gestures When Panel Swiping Is Disabled
- [x] **Verified**
- **Test Steps**:
  1. Go to **Rules / Settings** (Tab 4) and disable **"Swipe to switch panels"**.
  2. Try swiping horizontally on main screens.
  3. Perform item-level swipes in **Recents** or **Contacts**.
- **Expected Result**:
  - Main panel horizontal swiping is disabled.
  - List items support item-level swipe gestures (e.g., swipe right to call, swipe left for details/WhatsApp) without triggering full-screen tab switches.

---

### 14. Task 14 (Phase 6): Recents Search & Live Filtering
- [ ] **Verified**
- **Test Steps**:
  1. Open **Recents** (Tab 1).
  2. Type a caller name, number, or note in the top `"Search by name or number"` search bar.
- **Expected Result**:
  - Recents list filters dynamically in real time matching the search query.

### 15. Task 15 (Phase 6): Favorites Reorder Persistence & Material Card Hairline Border
- [ ] **Verified**
- **Test Steps**:
  1. On **Favorites** (Tab 0), tap **Configure** and drag-and-drop cards to reorder them.
  2. Restart the app and verify custom card order.
  3. Switch between Bento, Grid, and Material card formats.
- **Expected Result**:
  - Custom drag-and-drop card order persists across app restarts.
  - Material card style maintains an ultra-thin hairline border without heavy dark outlines.

### 16. Task 16 (Phase 6): Ask & Learn Preferred Channel Recomposition & Equal Contacts Icon Sizing
- [ ] **Verified**
- **Test Steps**:
  1. In **Contacts** (Tab 3), expand a contact's phone number options.
  2. Inspect the WhatsApp Call button vs Phone Call button.
- **Expected Result**:
  - Both Phone and WhatsApp call action buttons have 1:1 identical 36.dp button dimensions and 18.dp icons.
  - The preferred channel button displays a filled container background highlight without increasing icon size.

### 17. Task 17 (Phase 6): Configure Mode Popular Contact Ignore [X] Action
- [ ] **Verified**
- **Test Steps**:
  1. On **Favorites** (Tab 0), tap **Configure** mode.
  2. Locate a card in the **Popular** section and tap the prominent red **[X]** ignore button.
- **Expected Result**:
  - The popular contact is immediately ignored and removed from the popular list.

### 18. Task 18 (Phase 6): Horizontal Pager Smoothness & Sub-Tab Swipe Integration
- [ ] **Verified**
- **Test Steps**:
  1. Swipe horizontally between main panels at various speeds.
  2. In **Rules / Settings** (Tab 4), swipe horizontally between sub-tabs.
- **Expected Result**:
  - Main panel swiping is pre-rendered (`beyondViewportPageCount = 2`) and transitions smoothly without getting stuck midway.

### 19. Task 20 (Phase 6): Phone Contact vs Local Contact Edit Routing
- [x] **Verified**
- **Test Steps**:
  1. Open a **Phone Contact** details sheet and tap **Edit**.
  2. Open a **Local App Contact** details sheet and tap **Edit**.
- **Expected Result**:
  - Phone Contacts open the Android Phone Contacts app editor directly.
  - Local App Contacts open the in-app `EditContactDialog`.

### 20. Task 21 (Phase 6): Popular Contact Favoriting Nickname Auto-Lookup
- [x] **Verified**
- **Test Steps**:
  1. Locate a contact in the Popular section that has a nickname in device contacts.
  2. Tap the star to favorite that contact.
- **Expected Result**:
  - The created Favorite card automatically resolves and displays the contact's custom nickname.

### 21. Task 22 (Phase 6): Dual Dialers Across All Card Styles
- [x] **Verified**
- **Test Steps**:
  1. Switch Favorite Card Style between **Bento**, **Grid**, and **Material** in Settings.
  2. Ensure WhatsApp mode is set to **Ask & Learn** or **Ask Always**.
- **Expected Result**:
  - All card styles render dual side-by-side action buttons (`Phone` and `WhatsApp`) when calling preference is undetermined or in Ask Always mode.

### 22. Task 23 (Phase 6): Keypad Hybrid Action Buttons
- [x] **Verified**
- **Test Steps**:
  1. Navigate to **Keypad** (Tab 2) and enter digits.
  2. Inspect the bottom action buttons.
- **Expected Result**:
  - Action buttons render as wide Hybrid Buttons displaying both Icon AND Text Label (`[ 📞 Phone Call ]` in emerald `#059669` and `[ 💬 WhatsApp ]` in signature `#25D366`).

### 23. Task 30 (Phase 7): Keypad 2x2 Call Action Grid & Preference Highlighting
- [ ] **To Test**
- **Test Steps**:
  1. Navigate to the Keypad tab and enter a phone number.
  2. Inspect the 4 call action buttons laid out in a 2x2 grid (`Text Message`, `Phone`, `WhatsApp - Msg`, `WhatsApp - Voice`).
  3. Enter a number known to prefer WhatsApp or Phone.
- **Expected Result**:
  - The 4 buttons render cleanly in a non-scrolling 2x2 grid.
  - The preferred channel is emphasized with a dynamic high-contrast container border without intrusive badge labels.

### 24. Task 31 (Phase 7): WhatsApp Dark Mode Icon Contrast
- [ ] **To Test**
- **Test Steps**:
  1. Switch device to Dark Theme.
  2. Inspect the WhatsApp icons on Keypad 2x2 grid, Favorites cards, and Contact details.
- **Expected Result**:
  - The WhatsApp icon features a crisp white outer contour ring around the bubble, preventing it from blending into dark backgrounds.

### 25. Task 32 (Phase 7): External Outgoing Call Redirection Service
- [ ] **To Test**
- **Test Steps**:
  1. Set OmniDial as Default Phone App.
  2. Initiate an outgoing call from a connected Bluetooth vehicle head-unit, smartwatch, or assistant to a contact with WhatsApp preference.
- **Expected Result**:
  - `OmniCallRedirectionService` intercepts the cellular call and routes it directly to WhatsApp VoIP.

### 26. Task 33 (Phase 7): Multi-Version APK Downloads on Website
- [ ] **To Test**
- **Test Steps**:
  1. Open `docs/index.html` in browser.
  2. Scroll down to the **Release Notes & Version History** section.
  3. Verify download links for **v1.1.0** (`OmniDial-v1.1.0.apk`) and **v1.0.0** (`OmniDial-v1.0.0.apk`).
- **Expected Result**:
  - Both release versions have distinct download buttons, version badges, and changelog lists.

---

### 💬 Feedback & Notes (If any test fails or needs adjustment)
*(Write any notes here, or type them in our chat)*
