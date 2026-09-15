# OmniDial — Verified & Tested Archive (`Verified.md`)

This document archives all verified and tested test scripts, features, and regression tests from `ToTest.md`.

---

## 📋 Verified Test Scripts

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

### 3. Task 4 (Phase 2): Comprehensive Pop-up & Dialog Overhaul
- [x] **Verified** TESTED
- **Test Steps**:
  1. Open **Contacts** (Tab 3) and tap **+ New Contact**.
  2. Open **Recents** (Tab 1) or **Favorites** (Tab 0) and tap a contact row to open **Contact Details Bottom Sheet**.
  3. Edit a favorite contact or trigger the Audio Output Selector during a call.
- **Expected Result**:
  - Dialogs feature modern `24.dp`/`28.dp` rounded corners, pleasant surface container tints, leading icons (`Person`, `Phone`, `Label`), monogram avatar previews, and responsive primary buttons.

### 4. Task 8 (Phase 2): Recents List Unknown Number '+' Action & "Recents" Naming
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

### 5. Task 3 (Phase 3): Eliminate Initial 4-Card Flash in Favorites
- [x] **Verified** TESTED
- **Test Steps**: 
  1. Force-close the app and re-launch it on the **Favorites** tab.
  2. Observe the initial screen render before database emissions complete.
- **Expected Result**:
  - No 4 dummy/popular cards flash before real favorites load.
  - If no favorites are starred, the screen displays a clean, static *"No Favorites Added Yet"* empty layout.

### 6. Task 5 (Phase 3): Fresh Install Call Log Import & Auto-Backup
- [x] **Verified** TESTED
- **Test Steps**:
  1. Install a fresh build or clear app data with `READ_CALL_LOG` permission granted.
  2. Open the **Recents** tab immediately upon first launch.
  3. Reinstall or update the app on a device with Google Auto-Backup.
- **Expected Result**:
  - System call history from `CallLog.Calls` automatically seeds into Room SQLite, populating the Recents list immediately on fresh install.
  - Learned choices and ignored popular contacts persist across app updates/reinstalls via Android Auto-Backup rules (`backup_rules.xml` & `data_extraction_rules.xml`).

### 7. Task 13 (Phase 3): Persist Favorite Card Deletion in Configure Mode
- [x] **Verified** TESTED
- **Test Steps**:
  1. On the **Favorites** tab, tap the **Configure (Tune)** button to enter edit mode.
  2. Tap the **Delete (Trash)** icon on a favorite card.
  3. Refresh the screen or restart the app.
- **Expected Result**:
  - The card is removed instantly from local state and deleted/unmarked in Room SQLite without popping up blocking prompts or reappearing on refresh/restart.

### 8. Task 6 (Phase 4): Bento Style "Phone" Labeling, "Ask & Learn" Dual Dialers & WhatsApp Call Logging
- [x] **Verified** TESTED
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

### 9. Task 11 (Phase 4): Dedicated Spam Window & International Numbers Option
- [x] **Verified** TESTED
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

### 10. Task 9 (Phase 5): Swipe to Switch Main Panels
- [x] **Verified** TESTED
- **Test Steps**:
  1. Ensure **"Swipe to switch panels"** is **Enabled** in **Rules / Settings** (Tab 4).
  2. Horizontally swipe left or right across the screen on any main panel (**Favorites** <-> **Recents** <-> **Keypad** <-> **Contacts** <-> **Settings**).
- **Expected Result**:
  - Smooth horizontal swipe navigation seamlessly switches between the 5 main app panels.

### 11. Task 10 (Phase 5): Secondary Gestures When Panel Swiping Is Disabled
- [x] **Verified** TESTED
- **Test Steps**:
  1. Go to **Rules / Settings** (Tab 4) and disable **"Swipe to switch panels"**.
  2. Try swiping horizontally on main screens.
  3. Perform item-level swipes in **Recents** or **Contacts**.
- **Expected Result**:
  - Main panel horizontal swiping is disabled.
  - List items support item-level swipe gestures (e.g., swipe right to call, swipe left for details/WhatsApp) without triggering full-screen tab switches.

### 12. Task 20 (Phase 6): Phone Contact vs Local Contact Edit Routing
- [x] **Verified** TESTED
- **Test Steps**:
  1. Open a **Phone Contact** details sheet and tap **Edit**.
  2. Open a **Local App Contact** details sheet and tap **Edit**.
- **Expected Result**:
  - Phone Contacts open the Android Phone Contacts app editor directly.
  - Local App Contacts open the in-app `EditContactDialog`.

### 13. Task 21 (Phase 6): Popular Contact Favoriting Nickname Auto-Lookup
- [x] **Verified** TESTED
- **Test Steps**:
  1. Locate a contact in the Popular section that has a nickname in device contacts.
  2. Tap the star to favorite that contact.
- **Expected Result**:
  - The created Favorite card automatically resolves and displays the contact's custom nickname.

### 14. Task 22 (Phase 6): Dual Dialers Across All Card Styles
- [x] **Verified** TESTED
- **Test Steps**:
  1. Switch Favorite Card Style between **Bento**, **Grid**, and **Material** in Settings.
  2. Ensure WhatsApp mode is set to **Ask & Learn** or **Ask Always**.
- **Expected Result**:
  - All card styles render dual side-by-side action buttons (`Phone` and `WhatsApp`) when calling preference is undetermined or in Ask Always mode.

### 15. Task 23 (Phase 6): Keypad Hybrid Action Buttons
- [x] **Verified** TESTED
- **Test Steps**:
  1. Navigate to **Keypad** (Tab 2) and enter digits.
  2. Inspect the bottom action buttons.
- **Expected Result**:
  - Action buttons render as wide Hybrid Buttons displaying both Icon AND Text Label (`[ 📞 Phone Call ]` in emerald `#059669` and `[ 💬 WhatsApp ]` in signature `#25D366`).

### 16. Task 24 (Phase 6): Rules FAB Cleanup & "Never" Mode Choice Preservation
- [x] **Verified** TESTED
- **Test Steps**:
  1. Open Rules panel; verify FAB is cleanly styled without overlapping controls.
  2. Select "Never" for WhatsApp mode and verify selection persists reliably.
- **Expected Result**:
  - Mode choice stays preserved across app sessions.

### 17. Task 25 (Phase 6): Dynamic Star Icon Unstarring Reactive Sync
- [x] **Verified** TESTED
- **Test Steps**:
  1. Unstar a contact from Contact Details or Favorites.
  2. Verify star icon and list update reactively across all tabs.
- **Expected Result**:
  - Immediate state reflection with zero stale starred icons.

### 18. Task 26 (Phase 6): Favorites Header Cleanup
- [x] **Verified** TESTED
- **Test Steps**:
  1. Inspect Favorites header in both normal and configure mode.
- **Expected Result**:
  - Redundant secondary header line removed; clean and unified top bar.

### 19. Task 27 (Phase 6): Settings Card Design Sample Previews Update
- [x] **Verified** TESTED
- **Test Steps**:
  1. In Settings, inspect Bento, Grid, and Material card preview mockups.
- **Expected Result**:
  - Previews accurately reflect the dual-dialer layout and actual card aesthetics.

### 20. Task 28 (Phase 6): Recents Category Filters & `🤖 Rule` Badges
- [x] **Verified** TESTED
- **Test Steps**:
  1. Navigate to Recents and toggle category filter chips (All, Missed, WhatsApp, Rules).
- **Expected Result**:
  - Calls matched by automated rules show the `🤖 Rule` badge; list filters cleanly.

### 21. Task 30 (Phase 7): Keypad 2x2 Call Action Grid & Preference Highlighting
- [x] **Verified** TESTED
- **Test Steps**:
  1. Navigate to the Keypad tab and enter a phone number.
  2. Inspect the 4 call action buttons laid out in a 2x2 grid (`Text Message`, `Phone`, `WhatsApp - Msg`, `WhatsApp - Voice`).
  3. Enter a number known to prefer WhatsApp or Phone.
- **Expected Result**:
  - The 4 buttons render cleanly in a non-scrolling 2x2 grid.
  - The preferred channel is emphasized with a dynamic high-contrast container border without intrusive badge labels.

### 22. Task 31 (Phase 7): WhatsApp Dark Mode Icon Contrast
- [x] **Verified** TESTED
- **Test Steps**:
  1. Switch device to Dark Theme.
  2. Inspect the WhatsApp icons on Keypad 2x2 grid, Favorites cards, and Contact details.
- **Expected Result**:
  - The WhatsApp icon features a crisp white outer contour ring around the bubble, preventing it from blending into dark backgrounds.

### 23. Task 33 (Phase 7): Multi-Version APK Downloads on Website
- [x] **Verified** TESTED
- **Test Steps**:
  1. Open `index.html` in browser.
  2. Scroll down to the **Release Notes & Version History** section.
  3. Verify download links for **v1.1.0** (`OmniDial-v1.1.0.apk`) and **v1.0.0** (`OmniDial-v1.0.0.apk`).
- **Expected Result**:
  - Both release versions have distinct download buttons, version badges, and changelog lists.

### 24. Task 8.1 (Phase 8): Domain Use-Case Modularization
- [x] **Verified** TESTED
- **Test Steps**:
  1. Use Keypad T9 to search contacts by name, nickname, and phone number (`SearchT9ContactsUseCase`).
  2. Make an outgoing call with configured SIM rules to verify rule matching (`EvaluateSimRuleUseCase`).
  3. Receive an incoming call and verify caller identification name, label, and spam badge (`ResolveCallerIdentityUseCase`).
  4. Star/unstar contacts in Favorites and ignore contacts in Popular list (`ManageFavoritesUseCase`).
- **Expected Result**:
  - All operations perform immediately without UI lag or stale state.

### 25. Task 8.2 (Phase 8): Isolated In-Call Duration Timer
- [x] **Verified** TESTED
- **Test Steps**:
  1. Place an active call (or simulated call).
  2. Observe the call duration status chip in the top bar.
  3. Interact with in-call controls (Mute, Speaker, Dialpad, Notes) while the timer counts seconds.
- **Expected Result**:
  - Timer increments smoothly every second without causing parent screen recomposition or stuttering when interacting with bottom controls.

### 26. Task 8.3 (Phase 8): Android 13+ Sensory Haptic Primitives
- [x] **Verified** TESTED
- **Test Steps**:
  1. Tap digits 0–9, *, and # on the dial pad.
  2. Long-press an unassigned digit (e.g. 3 or 4) to trigger the Speed Dial prompt.
  3. Place a call and observe tactile feedback when the call transitions to the active/connected state.
- **Expected Result**:
  - Crisp tactile tick on digit tap (`EFFECT_CLICK`), solid feedback on speed dial long-press (`EFFECT_HEAVY_CLICK`), and distinct connection confirmation.

### 27. Task 12 (Phase 1): Keypad Dial Button Internal Highlight Geometry
- [x] **Verified** TESTED
- **Test Steps**:
  1. Navigate to the **Keypad** (Tab 2).
  2. Enter a phone number for which a preferred channel (Phone vs WhatsApp) is detected.
- **Expected Result**:
  - The preference highlight ring renders *inside* the circular dial button container.
  - The dial button outer dimensions stay locked to `56.dp`, and zero vertical jitter or key shifting occurs when entering digits.

### 28. Task 14 (Phase 6): Recents Search & Live Filtering
- [x] **Verified** TESTED
- **Test Steps**:
  1. Open **Recents** (Tab 1).
  2. Type a caller name, number, or note in the top `"Search by name or number"` search bar.
- **Expected Result**:
  - Recents list filters dynamically in real time matching the search query.

### 29. Task 16 (Phase 6): Ask & Learn Preferred Channel Recomposition & Equal Contacts Icon Sizing
- [x] **Verified** TESTED
- **Test Steps**:
  1. In **Contacts** (Tab 3), expand a contact's phone number options.
  2. Inspect the WhatsApp Call button vs Phone Call button.
- **Expected Result**:
  - Both Phone and WhatsApp call action buttons have 1:1 identical 36.dp button dimensions and 18.dp icons.
  - The preferred channel button displays a filled container background highlight without increasing icon size.

### 30. Task 17 (Phase 6): Configure Mode Popular Contact Ignore [X] Action
- [x] **Verified** TESTED
- **Test Steps**:
  1. On **Favorites** (Tab 0), tap **Configure** mode.
  2. Locate a card in the **Popular** section and tap the prominent red **[X]** ignore button.
- **Expected Result**:
  - The popular contact is immediately ignored and removed from the popular list.

### 31. Task 18 (Phase 6): Horizontal Pager Smoothness & Sub-Tab Swipe Integration
- [x] **Verified** TESTED
- **Test Steps**:
  1. Swipe horizontally between main panels at various speeds.
  2. In **Rules / Settings** (Tab 4), swipe horizontally between sub-tabs.
- **Expected Result**:
  - Main panel swiping is pre-rendered and transitions smoothly without getting stuck midway.

