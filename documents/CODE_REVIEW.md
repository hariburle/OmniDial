# OmniDial — Principal Android Engineering System Review & Audit

**Date**: September 2026  
**Role**: Principal Android Engineer / System Architect  
**Scope**: Android Telecom Architecture, InCallService State Machine, Jetpack Compose Performance, Concurrency, and SQLite Storage  

---

## 1. Executive Summary & Health Scorecard

OmniDial is a native Android phone dialer and call automation suite built with Jetpack Compose (Material 3), Room SQLite, Android Telecom (`InCallService`, `CallRedirectionService`), and Kotlin Coroutines/StateFlow.

This audit evaluates the codebase against modern Android standards (targeting Android 14/15/16, API 36), focusing on main-thread responsiveness, telephony reliability, memory lifecycle, and UI smoothness.

| Domain | Rating | Status | Summary |
| :--- | :---: | :---: | :--- |
| **Telecom & InCallService Compliance** | **B+** | ⚠️ Needs Hardening | Native `InCallService` and `CallRedirectionService` well structured, but blocking operations during incoming call setup pose ANR risks. |
| **Concurrency & Thread Safety** | **C+** | 🚨 Critical Issues | Multiple `runBlocking` calls on Android's Main/UI thread in service callbacks; potential ANR and background service crash vectors. |
| **Architecture & Modularity** | **B-** | ⚠️ Partially Decomposed | Domain use cases and sub-ViewModels exist in `com.example.ui.viewmodels`, but `MainActivity` and screens still tightly couple to the 2,126-line `MainViewModel`. |
| **Jetpack Compose UI Performance** | **B** | 🟡 Moderate Optimization | `@Immutable` models applied to core data structures, but T9 search triggers regex re-compilation and full collection scans on raw keypad strokes without debounce. |
| **Data Layer & SQLite Indexing** | **A-** | 🟢 Solid | Room schema v12 with composite indices on `normalized_number`, `phoneNumber`, and `timestamp` is solid. Unbounded queries exist on the system `CallLog` provider. |

---

## 2. 🚨 Critical & High-Severity Vulnerabilities (Must-Fix)

### 🔴 P0: Main-Thread Blocking (`runBlocking`) on Incoming Calls
- **File & Lines**: [`app/src/main/java/com/example/telecom/CallManager.kt:143-183`](../app/src/main/java/com/example/telecom/CallManager.kt#L143-L183) & [`CallManager.kt:455-485`](../app/src/main/java/com/example/telecom/CallManager.kt#L455-L485)
- **Problem**:
  Android's `InCallService.onCallAdded(call: Call)` executes directly on the **Main (UI) Thread**. Within `onCallAdded` and `isWhitelistedOrRuleMatched`, synchronous `runBlocking` calls are invoked:
  1. `runBlocking { dao.getAllFavoritesList() }`
  2. `runBlocking { dao.getSpamByNormalizedNumber(...) }`
  3. `runBlocking { dao.getEnabledRules() }`
  4. Synchronous ContentResolver query: `ContactHelper.lookupContactByNumber(context, number)`
- **Impact**:
  If Room or ContentResolver is engaged in a background transaction (e.g. contact refresh or call log sync), the Main thread is locked while an incoming call is ringing, leading to UI freezes or an immediate **ANR (Application Not Responding)**.
- **Remedy**:
  Make incoming call evaluation reactive. Post an immediate lightweight `ActiveCallInfo` placeholder to display the incoming call UI immediately (< 16ms), and asynchronously enrich spam status, rule triggers, and contact details on `Dispatchers.IO`.

---

### 🔴 P0: Fatal `RemoteServiceException` in Foreground Service
- **File & Lines**: [`app/src/main/java/com/example/telecom/CallForegroundService.kt:84-106`](../app/src/main/java/com/example/telecom/CallForegroundService.kt#L84-L106)
- **Problem**:
  `CallForegroundService.start(context)` issues `context.startForegroundService(intent)`. Inside `startForegroundWithCall()`:
  ```kotlin
  val currentCall = CallManager.activeCall.value
  if (currentCall == null || currentCall.state == Call.STATE_DISCONNECTED) {
      stopServiceInternal()
      return
  }
  ```
  If a call quickly disconnects (e.g. 1-ring drop or rejected call) before the service executes `onStartCommand`, `stopServiceInternal()` is called **without ever calling `startForeground(...)`**.
- **Impact**:
  On Android 8.0 through Android 14, Android OS enforces a strict contract: any service started via `startForegroundService()` that returns without calling `startForeground()` crashes the entire process with:
  `Fatal Exception: android.app.RemoteServiceException: Context.startForegroundService() did not then call Service.startForeground()`
- **Remedy**:
  Always invoke `startForeground(...)` with a fallback/teardown notification before calling `stopSelf()`, or check call validity synchronously before calling `startForegroundService()`.

---

### 🟠 P1: Unbounded Full System `CallLog` Table Scan
- **File & Lines**: [`app/src/main/java/com/example/util/ContactHelper.kt:1532-1548`](../app/src/main/java/com/example/util/ContactHelper.kt#L1532-L1548)
- **Problem**:
  In `fetchDeviceCallHistoryForContact`:
  ```kotlin
  val cursor = context.contentResolver.query(
      android.provider.CallLog.Calls.CONTENT_URI,
      projection,
      null, // selection is null!
      null,
      "${android.provider.CallLog.Calls.DATE} DESC"
  )
  ```
  The query does not supply a SQL `selection` filter or SQL `LIMIT`. If a user has 10,000 call log entries on their device, every time a contact details bottom sheet opens, Android's system CallLog provider streams all 10,000 rows across IPC, and filtering is performed in software memory inside a `while (cursor.moveToNext())` loop.
- **Impact**:
  Noticeable UI stutter when opening the Contact Details Sheet, high memory allocation spikes, and unnecessary battery drain.
- **Remedy**:
  Pass a SQL selection clause (`Calls.NUMBER IN (...)`) and a `LIMIT 50` parameter into the `ContentResolver` query URI.

---

### 🟠 P1: Incomplete Architectural Decomposition (Orphan Sub-ViewModels)
- **File & Lines**: `app/src/main/java/com/example/ui/viewmodels/` vs [`app/src/main/java/com/example/MainActivity.kt`](../app/src/main/java/com/example/MainActivity.kt)
- **Problem**:
  Sub-ViewModels (`DialerViewModel`, `CallLogViewModel`, `ContactsViewModel`, `FavoritesViewModel`, `RulesViewModel`) were generated under `com.example.ui.viewmodels` to break up `MainViewModel.kt` (2,126 lines). However, `MainActivity.kt` and the respective screen composables (`DialerScreen`, `CallLogScreen`, etc.) were **never wired** to these sub-ViewModels.
- **Impact**:
  `MainViewModel` remains a monolithic "God class" handling contact syncing, database operations, telecom routing, and screen state, while the sub-ViewModels remain dead, unmaintained code.
- **Remedy**:
  Complete the wiring by scoping sub-ViewModels to their respective navigation destinations / tabs.

---

## 3. ⚡ Jetpack Compose & Keypad Performance Audit

### 🟡 T9 Keystroke Search Overhead
- **File & Lines**: [`app/src/main/java/com/example/util/T9Helper.kt:50-85`](../app/src/main/java/com/example/util/T9Helper.kt#L50-L85) & [`app/src/main/java/com/example/ui/screens/DialerScreen.kt:263`](../app/src/main/java/com/example/ui/screens/DialerScreen.kt#L263)
- **Observation**:
  - `T9Helper.search` splits strings with `Regex("\\s+")` inside a nested loop over all contacts on every digit press. For 2,000 contacts, entering 5 digits can generate tens of thousands of temporary object allocations.
  - In `DialerScreen.kt`, `LaunchedEffect(number, allSearchContacts)` dispatches search on every single character change without debouncing.
- **Recommendation**:
  - Pre-compute and index T9 digit representations on contact models once when contacts load (`DeviceContact.nameT9Digits`).
  - Add a small 100–150ms debounce flow (`queryFlow.debounce(100)`) in `DialerViewModel` before executing searches on larger contact lists.

---

## 4. 🛡️ Telecom & Android 14 / 15 / 16 (API 36) Readiness

1. **API 36 Target**: `compileSdk = 36` and `targetSdk = 36` are configured. Android 14+ requires explicit foreground service types (`phoneCall`), which are correctly declared.
2. **Lockscreen Call Presentation**: `MainActivity` has `showWhenLocked="true"` and `turnScreenOn="true"` in the Manifest, ensuring incoming calls wake the display when ringing.
3. **Background Activity Launch (BAL)**: `OmniCallRedirectionService` correctly anticipates Android 10+ background activity launch restrictions by using `showWhatsAppRedirectionNotification` with a `fullScreenIntent` fallback when launching WhatsApp.

---

## 5. 🎯 Prioritized Implementation Roadmap

| Priority | Task | Target File | Expected Benefit |
| :---: | :--- | :--- | :--- |
| **P0** | Eliminate `runBlocking` from `CallManager.onCallAdded` and decouple incoming call UI presentation from disk I/O | `CallManager.kt` | Eliminates ANR risk on incoming calls; instant ringing response |
| **P0** | Ensure `startForeground` is always called before `stopSelf` in `CallForegroundService` | `CallForegroundService.kt` | Prevents fatal `RemoteServiceException` crash on rapid disconnects |
| **P1** | Add SQL selection and limit to `fetchDeviceCallHistoryForContact` | `ContactHelper.kt` | 10x faster Contact Details sheet opening; lower memory footprint |
| **P1** | Wire `DialerViewModel` and screen sub-ViewModels into `MainActivity` | `MainActivity.kt` | Resolves god-class architectural debt and isolates recompositions |
| **P2** | Pre-index T9 name and nickname digits on contact load | `T9Helper.kt`, `DeviceContact.kt` | Zero-allocation instant keypad filtering even with 5,000+ contacts |
