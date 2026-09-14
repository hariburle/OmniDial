# OmniDial — System Architecture & Technical Documentation

## 1. Executive Summary

**OmniDial** is a native Android Telecom dialer and call management application built with Jetpack Compose, Kotlin Coroutines/StateFlow, Room SQLite database, and Android Telecom framework (`InCallService`).

The app unifies phone contacts, app-created local contacts, T9 smart dialing, automated call screening/rules, carrier STIR/SHAKEN spam detection, and persistent drag-and-drop VIP favorite shortcuts into a clean Material 3 design.

---

## 2. Core Components & Architectural Overview

```
+---------------------------------------------------------------------------------+
|                                 MainActivity                                    |
|   (Hosts Bottom Navigation, Tab Bar, Floating In-Call Pill, and Root Insets)    |
+---------------------------------------------------------------------------------+
        |
        +---> MainViewModel (StateFlow, Repositories, Preferences Sync)
                |
                +---> AppRepository & AppDatabase (Room DB v10)
                |       ├── LocalContact
                |       ├── RecentCall
                |       ├── FavoriteContact
                |       ├── AutomationRule
                |       ├── IgnoredContact
                |       └── OfflineSpamNumber
                |
                +---> ContactHelper (System Contacts Provider & CallLog Merging)
                |
                +---> CallManager (Telecom InCallService & Automation Rule Pipeline)
```

### Key UI Screens & Components
- **`FavoritesScreen.kt`**: VIP grid hub featuring:
  - Configure Mode for drag-and-drop reordering.
  - Local state tracking (`localFavorites`) for zero-latency, continuous multi-row drag-and-drop.
  - Hit-testing against real-time physical screen bounds (`activeItemBounds[contact.id]`).
  - Top-layer `Box` overlay (`zIndex = 10000f`) ensuring dragged cards never draw under other cards.
  - Direct positional swapping (`Collections.swap`) for natural grid reordering.
  - Persistent sort order synced to Room and `SharedPreferences` (`favorite_sort_orders`).
- **`DialerScreen.kt`**: Ergo-width T9 smart keypad featuring:
  - Quick Recents bar shown when the keypad field is empty.
  - Overflow menu `⋮` on number bar providing "Add 2-sec pause (,)" and "Add wait (;)".
  - Long-press `*` -> `,` (Pause) and `#` -> `;` (Wait).
  - Long-press Backspace button clearing the entire number field with haptic feedback.
  - Flex-weight keys filling the horizontal width without vertical scrolling.
- **`CallLogScreen.kt`**: Rich call history merging system `CallLog.Calls` and Room `recent_calls` using unique composite keys (`"${group.primaryCall.id}_${group.primaryCall.timestamp}_$index"`), preventing LazyColumn key duplication crashes.
- **`ContactsScreen.kt`**: Unified directory aggregating system and local contacts into single person rows with right-thumb ergonomic action buttons on expanded rows.
- **`InCallScreen.kt`**: Active call UI with automatic soft keyboard dismissal upon connect, active call controls (Mute, Speaker, Hold, Audio Output Selector, Keypad), and post-call notes.
- **`MainActivity.kt`**: Hosts `FloatingCallPill` when an active call is minimized (`activeCall != null && activeCall.state != STATE_DISCONNECTED`), offering status bar inset shielding and a red hangup button.

---

## 3. Data Persistence & Migration

- **Database Version**: Bumped to Version 10 in `AppDatabase.kt`.
- **Migration Strategy**: `fallbackToDestructiveMigration(dropAllTables = true)` used during development to purge obsolete schema tables. Fake test seed data completely removed.
- **Local Contacts Table (`local_contacts`)**: Stores app-created contacts locally without requiring Google Account synchronization.
- **Favorites Sort Order (`favorite_sort_orders`)**: `SharedPreferences` string set storing `phoneNumber:sortOrder:name` mappings, ensuring custom grid order is restored immediately on app launch or reinstall.

---

## 4. Call Management & Automation Priority

```
Incoming Call Arrives
        │
        ▼
Evaluate Automation Rules (CallManager.kt)
  ├── Match rule 10-digit pattern (matchesRulePattern)
  └── Execute Rule Actions (e.g. Auto-Answer, DTMF sequence, Auto-Hangup, Auto-SMS)
        │
        ├── Rule Matched ──> Execute Action & Finish
        │
        └── No Rule Matched
                │
                ▼
      Evaluate Spam Blocklist & STIR/SHAKEN
        ├── Check local offline spam database
        └── Check Connection.VERIFICATION_STATUS_FAILED
                │
                ├── Is Spam ──> Reject / Silence
                └── Is Safe ──> Ring & Display Incoming Screen
```

---

## 5. Recent Fixes & Quality Upgrades

1. **Unified Contact Creation**: `CreateContactDialog` allows selecting between "Phone Contacts" and "App Only" local storage.
2. **Keypad UX**: Added Quick Recents bar, Pause/Wait overflow menu, long-press `,` / `;` T9 subtext, and long-press Backspace clear.
3. **In-Call Screen Keyboard Overlap**: Automatically hides soft keyboard when call connects (`LocalSoftwareKeyboardController.current?.hide()`).
4. **Recents Panel Crash Fix**: Resolved duplicate key exceptions in `LazyColumn` by generating unique composite keys for merged system/local call logs.
6. **Gate Buzzer & User Rule/Contact Whitelisting over Carrier Spam Filter**:
   - `CallManager.isWhitelistedOrRuleMatched` checks if an incoming number matches an active Automation Rule (e.g. Gate / Intercom Buzzer), Starred Favorites, or Saved Contacts BEFORE applying carrier STIR/SHAKEN or carrier spam checks.
   - Prevents legitimate gate buzzers or user contacts from being wrongly auto-rejected or flagged as carrier spam threats.

7. **Directional Grid Reordering Controls**:
   - In Configure Mode, every card provides explicit directional arrow buttons (`▲` Up Row, `▼` Down Row, `◄` Left Column, `►` Right Column).
   - Each directional button is dynamically enabled **only if there is space to move in that direction** (e.g., `▲` is enabled only if `index >= 2` in a 2-column grid).

---

## 6. Phase 6 Advanced Features & System Polish

1. **Smart Contact Discovery & Sorting**:
   - Provides 5 dynamic sort modes (`A-Z`, `Recent`, `Long Time No Talk`, `Frequent`, `Rediscover`) uncoupled from alphabetical grouping.
2. **Keypad Hybrid Action Buttons**:
   - Redesigned Keypad action row into wide, high-visibility Hybrid Buttons (`[ 📞 Phone Call ]` in Deep Emerald `#059669` and `[ 💬 WhatsApp ]` in signature `#25D366`) with distinct branding and glowing preferred borders.
3. **Dedicated Spam Management Center**:
   - `SpamManagementDialog` provides blocked numbers list, quick search, manual blocking, and auto-block toggles.
4. **Recents Category Filters & Rule Pattern Matching**:
   - Filter chips (`All`, `Missed`, `In`, `Out`, `Spam`, `Rules`, `Notes`) and `🤖 Rule` badges. Rules filter matches historical calls matching active rule patterns.
5. **Phone vs Local Contact Edit Routing**:
   - System phone contacts (`contactId > 0`) open directly in the native Android Phone Contacts editor via `Intent.ACTION_EDIT`, while local app-only contacts open `EditContactDialog`.
6. **Cloud Auto-Backup & Persistence**:
   - `backup_rules.xml` and `data_extraction_rules.xml` ensure shared preferences, learned calling choices, and Room SQLite databases persist across reinstalls and cloud restorations.

---

## 7. Phase 7 (Release 1.1.0) Architecture & Enhancements

1. **Keypad 2x2 Action Button Architecture**:
   - Replaced scrolling carousels with a stable 2x2 grid in `DialerScreen.kt`:
     - Row 1: `Text Message` (SMS) and `Phone` (Cellular)
     - Row 2: `WhatsApp - Msg` and `WhatsApp - Voice`
   - **Situational Intelligence Highlighting**: Automatically highlights the preferred channel based on `getPreferredCallingMode(number)` using subtle container borders (`colorScheme.primary` or signature WhatsApp green) without visual clutter from text badges.
2. **High-Contrast Dark Mode WhatsApp Icon**:
   - `WhatsAppIcon.kt` uses custom vector drawing with an outer white contour stroke around the bubble path.
   - Prevents dark theme background blending and eliminates sizing anomalies across the keypad, favorites, and contact rows.
3. **Android `CallRedirectionService` Integration**:
   - Implemented `OmniCallRedirectionService` to intercept outgoing calls originating outside the app (such as vehicle Bluetooth head-units, Android Auto, smartwatches, voice assistants, and third-party dialers).
   - If the target contact prefers WhatsApp VoIP, the call is canceled via `cancelCall()` and seamlessly rerouted to WhatsApp VoIP.
4. **Favorites Per-Number Designation & Nickname Synchronization**:
   - Favorites search results provide distinct per-number buttons for contacts with multiple numbers, allowing users to designate the specific primary number for instant dialing.
   - Bi-directional sync between Android Contacts Provider (`ContactsContract.CommonDataKinds.Nickname`) and Room SQLite local database.
5. **Release Distribution & Versioned Storage Architecture**:
   - Maintained semantic versioning with dual hosting: latest release (`OmniDial-v1.1.0.apk` / `OmniDial.apk`) and previous builds (`OmniDial-v1.0.0.apk`) in `/docs` and root directory.
