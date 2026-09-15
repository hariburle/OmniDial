# OmniDial — Comprehensive Code Review & Engineering Improvement Plan

## 1. Executive Summary & Codebase Health

**OmniDial** is a rich, native Android Telecom dialer and call automation suite built with Jetpack Compose, Kotlin Coroutines/StateFlow, Room Database, and Android Telecom (`InCallService`, `TelecomManager`). 

This review assesses architectural modularity, performance & memory lifecycle, Jetpack Compose recomposition safety, Telecom & Android 14/15 system compatibility, data persistence, and error resiliency.
---

## 2. Deep-Dive Findings by Domain

### A. Architecture & State Management
- **God-Class Architecture in `MainViewModel.kt` (~2,000 lines)**:
  - `MainViewModel` currently manages dialer input, contacts querying, call log merging, favorite grid reordering, speed dials, automation rules, spam reporting, and cloud contact synchronization.
  - *Risk*: Difficult testability, high coupling across distinct screen lifecycles, and risk of state collisions.
  - *Recommendation*: Decompose into screen-focused or feature-focused ViewModels or UseCases (`DialerViewModel`, `CallLogViewModel`, `ContactsViewModel`, `RulesViewModel`, `FavoritesViewModel`), delegating to shared Repositories (`AppRepository`, `ContactHelper`).
- **Decoupling `CallManager.kt` from UI & Process Lifecycle**:
  - `CallManager` holds a static singleton `appContext` and interacts directly with UI notification helpers.
  - *Recommendation*: Encapsulate system telephony events in a dedicated `TelecomRepository` or reactive event stream (`SharedFlow<TelecomEvent>`), allowing ViewModels to consume call events cleanly without holding static Android context references.

---

### B. Concurrency, Coroutines & Memory Lifecycle
- **Unscoped `CoroutineScope(Dispatchers.IO)` Instances**:
  - Several components launch coroutines using ad-hoc `CoroutineScope(Dispatchers.IO)` without binding to a lifecycle supervisor.
  - *Risk*: Leaked background jobs if an activity or service is destroyed mid-operation (e.g., during rapid call disconnects or contact writes).
  - *Recommendation*: Enforce structured concurrency using `viewModelScope` for UI-triggered operations and a singleton `AppCoroutineScope` with a `SupervisorJob` for persistent background tasks (e.g., auto-hangup timers, background SMS dispatch).
- **ContentObserver Registration & Lifecycle Management**:
  - System `Contacts` and `CallLog` content observers in `MainViewModel` must safely unregister in `onCleared()` across process teardowns.
  - *Recommendation*: Verify `onCleared()` cleanly disposes of all `ContentObserver` callbacks and cancels pending search jobs.

---

### C. Jetpack Compose UI & Recomposition Performance
- **Data Class Stability & `@Immutable` / `@Stable` Annotations**:
  - Core domain models such as `ActiveCallInfo`, `RecentCall`, `DeviceContact`, and `SimInfo` are passed heavily across high-frequency composables (`InCallScreen`, `DialerScreen`, `CallLogItem`).
  - *Risk*: Without `@Immutable` / `@Stable` annotations, the Compose compiler may treat standard data classes with collection parameters as unstable, triggering unnecessary recompositions on parent layout changes.
  - *Recommendation*: Annotate UI state holders and list item models with `@Immutable` and ensure lists are wrapped in immutable collections or stabilized with `remember`.
- **High-Frequency Timer Isolation in `InCallScreen`**:
  - The 1Hz in-call duration timer is isolated into `CallDurationStatusChip`, which is a best practice.
  - *Recommendation*: Continue this pattern for audio waveform bars, DTMF visual feedback, and live search highlights by isolating high-frequency state into leaf composables.

---

### D. Telecom & Android System Compatibility (Android 14 & 15)
- **VoIP Integration with Android 14+ `TelecomManager.addCall`**:
  - `TelecomVoipHelper` successfully integrates Android 14 transactional VoIP endpoints.
  - *Recommendation*: Add robust graceful fallback logging and fallback audio routing for OEM skins (Samsung OneUI, Xiaomi MIUI) where self-managed `PhoneAccountHandle` registration may require explicit user-granted role permissions.
- **Foreground Service Types (`phoneCall` & `microphone`)**:
  - `CallForegroundService` correctly specifies `android:foregroundServiceType="phoneCall|microphone"`. Ensure `POST_NOTIFICATIONS` is always requested at runtime prior to posting ongoing call heads-up notifications.

---

### E. Data Persistence, Indexing & Storage
- **Room Database Indexes in `AppDatabase.kt`**:
  - The `recent_calls`, `caller_rules`, and `offline_spam_numbers` tables handle frequent lookup queries (e.g., matching normalized phone numbers).
  - *Recommendation*: Add composite database indexes (`@Entity(indices = [Index("phoneNumber"), Index("normalized_number")])`) on `recent_calls` and `offline_spam_numbers` to ensure sub-millisecond query performance on large databases (>10,000 calls).
- **Backup & Restore Validation**:
  - `BackupManager.kt` imports external JSON backup files.
  - *Recommendation*: Add JSON schema version validation and payload integrity checksums (`CRC32`/`SHA-256`) before deserializing and writing to Room, protecting against corrupted or malformed imported files.

---

## 3. Prioritized Improvement Roadmap

| Priority | Area | Action Item | Impact |
| :--- | :--- | :--- | :--- |
| **P0** | **Data & Queries** | Add Room table indices on `phoneNumber` and `normalized_number` in `RecentCall` and `SpamNumber`. | Fast query execution on large call logs |
| **P1** | **Architecture** | Split `MainViewModel` into domain sub-viewmodels (`DialerViewModel`, `CallLogViewModel`, `ContactsViewModel`). | Modularity, testability, and clean separation of concerns |
| **P1** | **Compose Perf** | Add `@Immutable` annotations to UI models and verify list item stability. | Smoother 120Hz scrolling on large contact lists |
| **P2** | **Telecom / VoIP** | Add OEM-specific fallback validation for self-managed PhoneAccount handles in `TelecomVoipHelper`. | Maximized hardware compatibility across devices |
| **P2** | **Backup Security** | Add JSON schema version check and integrity checksums in `BackupManager`. | Safe cross-device backup restore |

---
