# OmniDial Combined Code & Performance Review

**Repository:** github.com/hariburle/OmniDial
**Date:** 2026-09-20
**Reviewed against:** `main` @ `bb9dd38` (2 commits ahead of `origin/main` @ `f301cc9`)
**Sources merged:** `documents/omnidial-code-review.md` (Meta muse, against `f301cc9`) and `documents/PERFORMANCE_REVIEW.md` (Qoder, against `bb9dd38`). Both originals are kept unmodified.
**Rollback baseline:** git tag `pre-perf-fixes-baseline` → `bb9dd38`. See `documents/ROLLBACK.md`.

---

## Verification key

- **[V]** — cited lines read directly.
- **[R]** — reported with a precise cite, not independently re-read. High confidence; spot-check before acting.
- **[M]** — from the muse review; not re-verified line-by-line here, but consistent with independently verified findings.

---

## Root cause

There is no single catastrophic bottleneck. There are **three expensive primitives called inside O(n) loops that re-run on every keystroke, every scroll frame, and every call event.** Almost every reported symptom traces back to one of them.

| Primitive | Cost per invocation | Call sites |
|---|---|---|
| `ContactHelper.matchesNumberQuery` (`ContactHelper.kt:961`) | 6 regex compilations, 2 full sorts of the country-code table, 2 libphonenumber parses | per contact × per number × per keystroke **[V]** |
| `PhoneNumberNormalizer.toE164` (`PhoneNumberNormalizer.kt:51`) | `parse` + `isValidNumber` + `isPossibleNumber` + `format`. **No memoization anywhere.** Also runs inside Room entity default constructors (`Entities.kt:70,89,103`), so every `copy()` and every restore row pays for a full parse. | ~30 sites **[V]**, constructor issue **[M]** |
| `AppDao.getAllRecentCallsList` / `getAllLocalContactsList` / `getAllFavoritesList` (`AppDao.kt:73,148,83`) | unbounded full-table scan, no `LIMIT`, followed by an in-memory linear scan | 6 / 8 / **20** **[V]**; muse counted 4 favorites scans on the ring path alone **[M]** |

Fixing the primitives fixes most call sites at once, and is feature-preserving by construction — it changes *how* work is done, not *what* work is done.

---

## P0 — bugs and risks, fix regardless of performance

### 0.1 Release builds silently sign with the debug key **[M]**
`app/build.gradle.kts:46-59`. `my-upload-key.jks` is **not present in the repo**, so the `else` branch is what actually runs today: every release APK is signed with `debug.keystore` / password `android`, which *is* committed. A Play release signed this way is an update-integrity and security problem.

**Fix chosen:** emit a loud build warning naming the missing keystore. Deliberately *not* a hard failure — `assembleRelease` is the publishing path required by `AGENTS.md`, and failing it would break releases until the upload key is restored. Promote to a hard failure once `my-upload-key.jks` (or `KEYSTORE_PATH`) is available.

### 0.2 Hard-coded personal number ships to all users and runs two full-table pipelines at cold start **[V]** + **[M #2]**
`ui/MainViewModel.kt:610-613`:
```kotlin
viewModelScope.launch(Dispatchers.IO) {
    removeSpam("+1 469-731-3343")
    removeSpam("4697313343")
}
```
Each `removeSpam` (2151-2183) pulls `getAllSpamNumbersList()` **and** `getAllRecentCallsList()`, loops every row normalizing digits, issues per-row `updateRecentCall`, and writes SharedPreferences. Two of these run on the startup critical path alongside `refreshContacts()` and `refreshRecentCalls()`.

**Fix:** delete. (The abandoned stash `omnidial-broken-normalized-refactor-20260920` had already correctly removed this.)

### 0.3 Incoming-call `Call.Callback` is never unregistered **[V]** + **[M #3]**
`telecom/CallManager.kt:275`. No `unregisterCallback` exists anywhere in the repository. Each call leaks a callback capturing the service `Context` for the process lifetime, and keeps firing through teardown — which is the mechanism behind the duplicate `handleCallEnded` in 0.4.

**Fix:** hold a field reference; unregister in `onCallRemoved`/`handleCallEnded` before dropping `nativeCall`.

### 0.4 `handleCallEnded` runs twice per call; notification cancelled three times **[R]**
`CallManager.kt:293-295` and `:479-484`, plus `TelecomCallService.kt:54`. Service stop and notification cancel execute repeatedly, with two contexts at `494-499`.

### 0.5 Backup restore can leave a half-wiped database **[M #4]**
`util/BackupManager.kt:403-453`. `clearAllFavorites()` then per-row inserts with no transaction: N transactions for N rows, and an interrupted restore leaves a cleared-but-half-filled DB.

**Fix:** `@Transaction` DAO methods (`replaceAllFavorites(list)` = clear + bulk `@Insert`), one call per section.

### 0.6 Migration hazard — silent data wipe **[M #5]**
`data/AppDatabase.kt`. `fallbackToDestructiveMigration()` combined with hand-written migrations that `catch (_: Throwable) {}`. A missing or failed migration silently wipes user data instead of erroring.

**Fix:** remove the swallowed exceptions, keep the fallback. (Removing the fallback instead would crash existing installs on any schema bump.)

---

## P1 — highest user-visible performance impact

### 1.1 `matchesNumberQuery` performs the cheap check last **[V]** + **[M #8]**
`util/ContactHelper.kt:961-997`. The plain digit-substring test that resolves most searches sits at **line 981** — *after* two `extractCountryCallingCode` calls (972-973), each compiling `Regex("[^0-9+]")` (879) and rebuilding `countryCallingCodes.values.distinct().sortedByDescending { it.length }` (887, a sort of the whole country-code table per invocation); and after `isSamePhoneNumber` (978), which calls libphonenumber (919), compiles two more regexes (923-924), calls `extractCountryCallingCode` twice more (928-929) and scans the country-code table twice via `normalizeToLocalDigits` (937-938, loop 864-868).

**Cost:** ~5,000 contacts × 1.5 numbers ≈ **10,000 libphonenumber parses and 45,000 regex compilations per keystroke**, on the main thread.

**Fix:** reorder so the digit-substring check runs first; consult libphonenumber only for genuinely ambiguous cases. Hoist `sortedCodes` and all regex literals to file-level `val`s.

**Feature risk:** none — pure reordering, truth table unchanged.

### 1.2 The same filter pass runs twice, then again per visible row **[V]** + **[M #8]**
`ui/screens/ContactsScreen.kt:250-278` and `:384-400` apply the **identical** predicate over the **same** list. When `smartSortBy == ALL && sourceFilter == ALL` the second pass feeds `otherFilteredOutMatches` (403-404), which immediately returns `emptyList()` — the expensive pass runs and its result is discarded.
`ui/components/ContactRowItem.kt:63-68` — `remember(searchQuery, contact)` re-runs `matchesNumberQuery` up to `phoneNumbers.size + 1` times **for every visible row on every keystroke**, only to choose which number to highlight. The filter pass already knows the answer.
`util/T9Helper.kt:75-82` — four `matchesNumberQuery` calls per contact (twice for `matchedPhone`, twice for `matchingPn`).
Also `ContactsScreen.kt:313-316` splits `Regex("\\s+")` **inside the comparator**, and `:300` compiles `Regex("[^0-9+]")` per favorite.

**Fix:** one filter pass carrying the matched number alongside the contact; pass it into `ContactRowItem`; in `T9Helper` derive `matchedPhone = (matchingPn != null)`.

### 1.3 `refreshRecentCalls()` is O(M²) and fires up to six times per call **[V]** + **[M #9]**
`ui/MainViewModel.kt:616-733`. Loads the entire `recent_calls` table (638, unbounded), fetches 100 system call-log rows, then does a per-row deleted-registry scan (631-635), a `roomCalls.firstOrNull { isSamePhoneNumber(…) }` per system call (647-659), and a dedup pass using `deduplicated.indexOfFirst {…}` (684-712) — **O(M²)**. At M ≈ 2,100 rows that is ~4.4M `isSamePhoneNumber` calls, each a libphonenumber parse pair.

**Triggers [V]:** `init` (585); `activeCall` disconnect collector (596); the `callLoggedEvent` collector which calls it **three times with hard-coded `delay(600)`/`delay(1200)`** (601-608); the CallLog `ContentObserver` (893, fired multiple times per call by Android); `placeWhatsAppCall` (1346); `placeGoogleVoiceCall` (1385); note save (1631); deletes (1658, 1683); `MainActivity.onNewIntent` (222); `onResume` (238); permission callback (654); channel-config save (1183). One hangup ⇒ ~5 full merges in 2 s. The `AtomicBoolean` at 617 prevents *concurrent* runs, not *repeated* ones.

**Fix:** delete the triple-delay collector and the `activeCall` disconnect refresh (the `ContentObserver` covers both); replace the `indexOfFirst` dedup with a `HashMap<last10Digits, RecentCall>`.

**Feature risk:** low. The delays were presumably a workaround for late system log writes — verify timing on a real device.

### 1.4 Every incoming call scans the entire favorites table 4× **[M #6]** + **[V]**
`telecom/CallManager.kt:330, 639, 1228, 1301` — each loads `dao.getAllFavoritesList()` (whole table) and linear-scans in memory, on the hot ring path. The `normalized_number` index already exists and is never used. Plus a redundant double spam lookup per call (`:363`), and at `:761` a spam lookup that leaves the DAO's normalized default unparsed so the index is never hit.

**Fix:** add `getFavoriteByNormalizedNumber(normalizedNumber)` to `AppDao`; replace all four scans with one indexed lookup; one normalized spam lookup. This is the change the abandoned stash was mid-way through.

### 1.5 Root composition collects ~45 StateFlows and keeps all five pages composed **[V]** + **[M #10]**
`MainActivity.kt:439-484` collects every StateFlow at the root, making the root the recomposition scope for all of them. `:863` sets `beyondViewportPageCount = 4`, so Contacts, CallLog, Favorites, Rules and Settings are **all** composed at all times. Any single emission — including **every dialer keystroke** via `dialerNumber` and every per-second call tick — recomposes the root. Because `MainViewModel` is an unstable capture and ~60 inline lambdas are passed down (872-938, 948-1001), each is a **new instance per recomposition**, defeating child skipping and invalidating every `remember` keyed on them. Related: `ui/components/CallAnswerViews.kt:60-74` reads animated values at the top of the composable, so the answer row recomposes every animation frame **[M]**.

**Fix:** `beyondViewportPageCount = 1`; move `collectAsStateWithLifecycle` into the consuming pages; pass method references (`viewModel::isSpamNumber`) instead of inline lambdas; enable Compose strong-skipping; read animated state only in leaf composables.

**Feature risk:** low, but test swipe feel on a low-end device.

---

## P2 — structural

### 2.1 Contact-sync feedback loop **[R]** + **[M #9]**
`ui/MainViewModel.kt:872-882`, `735-868`, `925-1094`. The contacts `ContentObserver` uses `notifyForDescendants = true`, so the app's **own** writes (e.g. `updateContactNickname` inside `addFavorite`, 1995) trigger `refreshContacts()` → `syncWithDeviceContacts()` → further writes → further notifications. Inside `syncWithDeviceContacts`, `getAllFavoritesList()` is called **five separate times** (931, 951, 1050, 1088, 1089) and matching is `allDevContacts.firstOrNull { … phoneNumbers.any … }` per favorite — O(F × D × P) ≈ 50 × 3,000 × 1.5. Extra triggers: every `onResume` (`MainActivity.kt:235`), permission grant (655-656, which calls `syncWithDeviceContacts()` a **second** time), and after every contact mutation (1824, 1872, 1882, 1920, 2359, 2388, 2428).

**Fix:** debounce the observer (~1 s); fetch favorites once and thread it through; index device contacts by normalized digits; remove the redundant second sync at `MainActivity.kt:656`.

### 2.2 In-call notification fully rebuilt every second **[V]**
`telecom/CallForegroundService.kt` (`manageTicker`, ~144-163): `while(isActive) { delay(1000); showCallNotification(...) }` re-posts at 1 Hz for the whole call, rebuilding the channel, five `PendingIntent`s and the `CallStyle` on the main thread. Entirely redundant — `setUsesChronometer(true)` is already set (`OngoingCallNotificationHelper.kt:151-156`) and ticks natively at zero CPU cost. The notification is additionally posted **three times on call add** within milliseconds: `CallManager.kt:321`, `TelecomCallService.kt:34-36`, `CallForegroundService.kt:136`.

**Fix:** delete the ticker; keep only the foreground-service post; remove `createNotificationChannel` from `buildCallNotification` (already created in `CallManager.init:163`).

### 2.3 Accelerometer and proximity sensors never stop **[R]**
`telecom/FlipToShhhManager.kt:64-124`. With the feature enabled (default `true`, 69), both sensors stay registered at `SENSOR_DELAY_UI` for the **entire app lifetime**, not just while ringing. `onSensorChanged` (124) runs on the main thread at ~60 Hz, executing `evaluateFaceOrientation` (192) per event. Separately, `initialize()` launches a new `activeCall` collector on the singleton scope (79) **every time MainActivity is created** (`MainActivity.kt:166`) and never cancels the previous one — after N recreations there are N collectors, each calling `startListening()` per ring.

**Fix:** register only while `activeCall.state == RINGING`; unregister when no call is active; `SENSOR_DELAY_NORMAL` with batching; store the `Job` and cancel before relaunching.

**Feature risk:** medium — flip-to-shhh would only operate while ringing, which is the only time it is meaningful. Confirm product intent.

### 2.4 Database work on the main thread **[M #7]** + **[R]**
`ui/components/CreateContactDialog.kt:136-155` — `fetchDeviceContacts()` and `lookupContactByNumber()` inside `LaunchedEffect` with no dispatcher switch: jank/ANR when the dialog opens. `DialerScreen.kt` does this correctly.
`util/BackupManager.kt:872-1003` — `listLocalBackups` is **not** suspend. Two MediaStore `LIKE` queries (898-924), file copies into internal storage (913-917, 960), 8+ directory scans, duplicate deletion — invoked synchronously from `refreshLocalBackups` (`MainViewModel.kt:124-126`) inside `init` (587), i.e. on the main thread on **every cold start and every activity recreation**.

**Fix:** `withContext(Dispatchers.IO)`; load backups lazily when the Backup screen first opens.

### 2.5 Resource leaks and accumulating state

| Leak | Location | Detail |
|---|---|---|
| `Call.Callback` never unregistered | `CallManager.kt:275` **[V]** | see 0.3 |
| `handleCallEnded` twice per call | `CallManager.kt:293-295`, `:479-484` **[R]** | see 0.4 |
| `loggedCallSessionIds` unbounded | `CallManager.kt:155`, added `:507` **[R]** | one string per call for process lifetime; the 4-s dedupe at `:525` already exists, so the set is redundant |
| Proximity wakelock replaced without release | `CallManager.kt:167-179` **[R]** | recreated on every `init`/`setTelecomService` while the old instance may still be held |
| Two leaked `CoroutineScope(Dispatchers.IO)` | `ChannelPreferenceRepository.kt:18`, `ChannelConfigRepository.kt:14` **[V]** | unmanaged, never cancelled, each holding a permanent Room observer |
| Broken double-checked locking | `ChannelDispatchCoordinator.kt:172-178`, `ChannelDiscoveryManager.kt:220-226`, `ChannelConfigRepository.kt:147-156`, `ChannelPreferenceRepository.kt:75-83` **[V]** | `INSTANCE ?: synchronized { new instance }` with no re-check inside the lock — a race creates duplicate singletons, each running `refreshChannels()` and its own permanent collector |
| `automation_logs` grows forever | **[M]** | no index, only ever read as "latest 50" |

### 2.6 Missing indexes and blocking preference writes **[V]** + **[M]**
`data/Entities.kt`: `local_contacts` (117-125) has no index on `phoneNumber` or `name`, yet `AppDao.kt:163` filters by `phoneNumber` and `:145`/`:148` sort by `name` — all 8 `getAllLocalContactsList()` sites pay a full scan plus a sort. `ignored_contacts` (107-114) unindexed but sorted by `timestamp` (`:124`/`:127`). `offline_spam_numbers` sorted by unindexed `reportCount` (`:100`/`:103`). `automation_logs` (35-43) unindexed on `timestamp` despite `ORDER BY timestamp DESC LIMIT 50`. Contrast `recent_calls` (46-53), correctly indexed on `normalized_number`, `phoneNumber`, `timestamp` — the pattern exists in the codebase, it just isn't applied consistently.

`ChannelConfigRepository` calls `editor.commit()` — a synchronous fsync — in **seven** places (46, 70, 88, 96, 113, 127, 136). Line 46 is inside `syncCustomNamesToPrefs`, which runs on **every emission** of `channel_configurations` via the collector at 22-26. Use `apply()`.

**Note:** adding indexes requires a Room migration — resolve 0.6 first.

### 2.7 Caches that don't cache **[V]**
`data/ChannelPreferenceRepository.kt` maintains a `ConcurrentHashMap` (15) fed by a permanent collector (17-26), but `getCachedPreference` (28-31) still calls `toE164` on every lookup — avoiding the DB query while paying the most expensive part — and `getPreferenceForNumber` (33-36) **ignores the cache entirely** and hits the database, still paying `toE164`. `getPreferredChannelId` (38-40) routes through it. The hot path gets neither benefit.

**Fix:** memoize `toE164` with an `LruCache<String,String>` (input space is bounded by the user's contacts and call log); make `getPreferredChannelId` consult `cachedPreferences` first.

### 2.8 Per-item work in list rows **[R]** + **[V]**
`ui/components/CallLogItem.kt`: `getCustomNameSync(...)` per composed item (`:81,90,503`); `CommunityCallerIdService.lookup(call.phoneNumber)` — a linear directory scan with normalization — **per item while scrolling** (`:394-396`); every visible item independently `collectAsState()`s `availableChannels` (`:75-76`); `SimpleDateFormat("MMM d, h:mm a", …)` allocated per composition with no `remember` (`:103-104`).
`ui/screens/CallLogScreen.kt:726` **[V]** keys items as `"${id}_${timestamp}_$index"` — including `index` means prepending one call **re-keys the entire list**, destroying item state and animations.

**Fix:** resolve channel labels once in `CallLogScreen` and pass strings down; batch the community lookup into a derived map; share one `remember`ed formatter; drop `$index`.

---

## P3 — build configuration **[V]**

### 3.1 ProGuard rules disable R8 optimization on the two hottest paths
`app/proguard-rules.pro`:
```
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.google.i18n.phonenumbers.** { *; }
-keep class com.google.i18n.phonenumbers.data.** { *; }
```
`-keep … { *; }` bars R8 from optimizing (inlining, devirtualization, constant folding) every member of Room and libphonenumber — precisely the two libraries on the hot paths above. `isMinifyEnabled = true` is set for release, so R8 runs but cannot touch these. Both libraries ship their own consumer ProGuard rules. **All five lines are redundant.**

### 3.2 No baseline profile
No `baseline-prof.txt` and no `androidx.baselineprofile` plugin anywhere. For a Compose app this typically costs 20-40% of cold-start time and scroll jank, because startup code runs JIT-only until the profile is built on-device. **Highest value-to-effort item in this review.**

### 3.3 No Compose stability configuration
With `List<T>` passed into many composables, a stability config file would materially improve skippability. Entities are already correctly `@Immutable`, so the groundwork exists.

### 3.4 Measurement caveat
`AGENTS.md:15` directs `installDebug` for device testing. Debug builds are unminified, R8-free and JIT-only — Compose in debug is several times slower than release. **Any performance judgement made against a debug install is not representative.** Confirm which build type the reported slowness was observed on before reprioritizing.

---

## Redundant and dead code

### Confirmed dead **[V]**
- **`domain/usecase/ResolveCallerIdentityUseCase.kt`** — zero references anywhere, including tests.
- **`domain/usecase/EvaluateSimRuleUseCase.kt`** — referenced only from `Phase10TelecomTest.kt`; never used in production. **[M]** reached the same conclusion.
- `CallManager.kt:325-476` re-implements both use cases inline, duplicating `:1444-1469` vs `ResolveCallerIdentityUseCase.kt:98-112`.

The whole `domain/usecase` package is dead weight while its logic is duplicated in the telecom layer.

### Dead DAO methods **[M]** — zero callers
`getAllRules`, `getCallHistoryForContact`, `deleteIgnoredContact`, `getContactSimPreference`, `clearAllContactSimPreferences`, `deleteLocalContactById`, `getAllContactSimPreferences`.

### Confirmed dead / unused **[R]** + **[M]**
- `CallManager.normalizePhoneNumber` (`:1471-1473`) — private, never called. **[M]** independently flagged it.
- `OngoingCallNotificationHelper.buildCallNotificationWithoutStyle` (`:244-361`) — ~120 lines duplicating `buildCallNotification`, reachable only from a catch at `:235`; `CallStyle` already has its own try/catch (169-186).
- **`ContactRowItem` accepts 9 unused parameters** (`onRequestCall`, `onCallDirect`, `onSelectNumber`, `onSmsClick`, `onCreateRule`, `onToggleFavorite`, `onPlaceWhatsAppCall`, `onSyncToPhone`, `getPreferredCallingMode`) plus an unused `LocalContext`. All three call sites allocate lambdas for them on every recomposition — which is exactly why the row cannot skip.
- `DialerScreen.kt:219` — `val effectiveContacts = deviceContacts`, a pointless alias.
- `ChannelConfigRepository.kt:54` — `val now = nextTimestamp()` unused (55 calls it again inside the `map`).
- `simulatedTimerJob` — declared, cancelled, never assigned. Duplicate `FilterChip` import. **[M]**
- Unobserved state in `MainViewModel`: `favoriteCardStyle` + setter (270-276), `isCallRedirectionRoleHeld` (321-322; UI uses its own `RoleHelper` call at `CallRedirectionCard.kt:34`), `notSpamWhitelist` public flow (202), `selectedCallReason`/`selectCallReason` (1155-1160; `DialerScreen.kt:227` keeps its own local state).
- `MainViewModel.kt:2515-2518` — static `inMemoryCached*` companion caches duplicate StateFlow content and leak across ViewModel instances.
- `SettingsScreen.kt:91` re-collects `allDiscoveredChannels`, already collected at `MainActivity.kt:576` and passed in as `discoveredChannels`.

### Duplicated logic **[R]** + **[M]**
- `normDigits` **copy-pasted 12 times** as local functions, plus `Regex` objects recompiled per call in hot paths **[M]**. Extract one shared util and one top-level compiled `Regex`.
- `ContactHelper.isSamePhoneNumber` (`:901-954`) wraps `PhoneNumberNormalizer.isSamePhoneNumber` (`:121-192`) then repeats its own digit/country-code fallbacks (923-951) that largely duplicate the normalizer's — **two parallel phone-number matching stacks** **[M]**, double parse work per comparison.
- `ContactHelper.fetchStarredContacts` (`:1256-1337`) vs `fetchDeviceContacts` (`:1583-1661`) — ~95% identical cursor/accumulator code; only the `STARRED = 1` selection differs.
- `shouldSuggestWhatsApp(String)` / `shouldSuggestWhatsApp(Context?, String)` (`:849-855`) — trivial aliases of `isInternationalNumber`.
- `ContactsScreen.kt:897-913`, `:951-967`, `:1049-1059` — three near-identical `isFav` blocks, each compiling `Regex("[^0-9+]")` per contact per number; a fourth at `:326-337`.
- `MainViewModel.kt:1594-1633` (`updateRecentCallNoteAndReminder`) vs `:1726-1775` (`savePostCallNote`) — near-identical note/reminder persistence plus scheduler logic.
- `MainViewModel.kt:1331-1347` (WhatsApp) vs `:1370-1386` (Google Voice) — copy-pasted call-logging, each with an O(D) `deviceContacts.firstOrNull { contains }` name lookup.
- Call-metadata enrichment **copy-pasted 3×** across `onCallAdded`, `startSimulatedIncomingCall`, `startSimulatedOutgoingCall` **[M]**.
- `MainViewModel.kt:141-155` — learned call modes written to **two** duplicate preference keys (`whatsapp_learned_choices`, `learned_call_modes`), then unioned on read (128-139).
- `FavoritesScreen.kt:335-360` builds `callCounts`; `:362-454` builds the same map again (407-413) and rebuilds ignored-contact norms (371) that already exist at 284-289.
- `markAsSpam` (`MainViewModel.kt:2139-2147`) and `removeSpam` (`:2165-2182`) load entire tables then loop to flip one flag. **[M]** same. Replace with a single `UPDATE … WHERE normalized_number = :digits` on the already-indexed column.
- `CallManager.kt:363` — `getSpamByNormalizedNumber(n) ?: getSpamByNumber(number, n)`. The second query's `WHERE phoneNumber = :number OR normalized_number = :normalizedNumber` (`AppDao.kt:106`) is a strict superset of the first. One call suffices.
- Duplicated cold-start loads: `MainViewModel.kt:556-583` loads `recent_calls` and `local_contacts` into state, then `refreshContacts()` (584) and `refreshRecentCalls()` (585) immediately load the same two tables again.
- Duplicated UI **[M]**: `ContactRowItem` vs `CallLogItem`; `FavoriteGridCard` vs `PopularGridCard`; the nickname dialog built twice; `FloatingCallPill` vs `PipCallContent` (~90% identical).

### Over-engineering **[M]**
- **God ViewModel:** `MainViewModel.kt` is 2,356 lines, ~101 functions, ~33 state declarations, and re-exports ~12 `CallManager` flows 1:1 — two sources of truth for the same streams.
- **`AppRepository`** is 101 lines / ~45 methods of pure pass-through to the DAO, and `CallManager` bypasses it anyway. Either delete it or make it the single normalization boundary.
- **SIM preferences live in Room *and* SharedPreferences**, written non-atomically. Pick one authoritative store.
- **Ambient automation on the hot call path** — every incoming call evaluates Wi-Fi SSID and Bluetooth matching against hard-coded lists of ~30 car brands and ~15 headphone brands. `WifiManager.connectionInfo` usually returns `<unknown ssid>` anyway.
- **Firebase BOM + Google Services + Secrets plugins configured with zero Firebase usage** and no `google-services.json` — build baggage. (`build.gradle.kts:115`, `:109`.)
- **Repo hygiene:** `apks/` holds 17 APKs at ~218 MB (repo ~319 MB). These belong in GitHub Releases.

### Already done well — do not "fix" these
- `DialerSuggestionsList` correctly hoists lookup maps into `remember(contacts)` and keys its `items` (48-67, 97, 188).
- `ContactsScreen` LazyColumn `key`s are correct (896, 950, 1048).
- `DialerScreen`'s T9 search is debounced (80 ms) and runs on `Dispatchers.Default` (307-311) — the problem is per-item cost, not threading.
- In-call duration is already isolated in `CallDurationStatusChip` (`InCallScreen.kt:279-284`).
- `ChannelDiscoveryManager` caches discovery results in StateFlows and uses cheap `getPackageInfo` probes (207-214). **`getInstalledPackages()` appears nowhere in the codebase.**
- `CommunityCallerIdService.lookup` is a pure in-memory list operation.
- `recent_calls` indexing (`Entities.kt:46-53`) is correct.
- `TelecomApplication.kt`, `SmartDialerWidgetProvider.kt`, `AppCoroutineScope`, `HapticFeedbackHelper` are clean.
- `AppRepository`'s 10 eager `Flow` vals are **not** a defect — Room flows are cold and cost nothing until collected. (An earlier suspicion, retracted.)

---

## Fix sequence

Ordered by felt improvement per unit of effort. **Phases 1-3 remove no features.**

**Phase 1 — no behaviour change.** 0.2 delete the debug `removeSpam` block · 1.1 reorder `matchesNumberQuery` + hoist regexes/`sortedCodes` · 3.1 delete the five ProGuard keeps · 2.8 drop `$index` from the CallLog key · 0.1 loud keystore warning.

**Phase 2 — low risk.** 2.7 memoize `toE164` and make the preference cache actually cache · 1.3 remove the triple-delay refresh and index the dedup · 2.2 remove the 1 Hz ticker and duplicate notification posts · 2.6 `commit()` → `apply()` · 2.5 leaked scopes + double-checked locking · dead-code deletion (`domain/usecase`, unused `ContactRowItem` params, dead DAO methods).

**Phase 3 — contained structural.** 1.4 indexed favorites/spam lookups replacing full-table scans · 0.3 unregister the call callback · 2.4 move `CreateContactDialog` and backup scanning off the main thread · 1.2 single filter pass + pass the matched number into `ContactRowItem`.

**Phase 4 — needs product judgement / migration.** 2.6 add the four missing indexes (after 0.6) · 0.5 `@Transaction` restore · 0.6 migration hardening · 1.5 restructure root composition · 2.1 debounce the contacts observer · 2.3 gate sensors to ringing state.

**Phase 5 — optional.** 3.2 baseline profile · 3.3 Compose stability config · dedupe the UI components · simplify ambient automation · hoist entity normalization out of constructors · split the ViewModel · move `apks/` to GitHub Releases.

---

## Implementation log — 2026-09-20

Branch `perf/fixes-phase1-3`, three commits on top of `bb9dd38`. Rollback instructions: `documents/ROLLBACK.md`. Verified with `:app:compileDebugKotlin` (clean) and `:app:testDebugUnitTest` (63/63). **Not yet run on a device.**

### Done

| Commit | Items |
|---|---|
| `2eeeb2e` | 0.1 (warning only) · 0.2 · 1.1 · 1.3 (trigger side) · 2.8 (`$index` key) · 3.1 · `toE164` memoization from 2.7 |
| `ace68fd` | 1.3 (O(M²) dedup and merge indexed by trailing digits) · 2.6 (`commit()`→`apply()`) · 2.5 (four broken double-checked-locking singletons) · 2.2 (partially — see below) · 2.7 (cache-first `getPreferredChannelId`) |
| `3e2c82d` | 0.3 (`Call.Callback` leak) |

### Corrections to the findings above

Two claims in the source reviews did not survive contact with the code:

- **§1.1 overstated the libphonenumber cost.** For a search query shorter than 7 digits, `ContactHelper.isSamePhoneNumber` exits through its short-code branch (`d1.length < 7 || d2.length < 7`) and never reaches libphonenumber. Since most typing is short prefixes, the per-keystroke cost was dominated by the **regex compilations and the country-code table sort**, not by parsing. Those are now hoisted, which addresses the real cost. libphonenumber is still reached for queries of 7+ digits, and is now consulted last rather than first.
- **§1.4 / muse #6 (favorites table scanned 4× per call) is not worth fixing.** A favorites table holds on the order of 5–50 rows, so four scans are ~200 comparisons per call — noise next to the recents merge, which was doing millions. Replacing them with a `normalized_number` index lookup would also narrow matching, and a missed favorite silently loses both the caller name on the in-call screen and the spam-whitelisting at `CallManager.kt:639`. **Rejected: real regression risk, negligible gain.** `favorite_contacts` is already correctly indexed, so the change remains available later if the table ever grows.

### Deliberately not done

- **§2.2 the 1 Hz in-call ticker stays.** The review called it fully redundant with `setUsesChronometer`. It is not: `OngoingCallNotificationHelper.kt:120,127,138` embed a formatted elapsed time in `statusText`, `contentText` and the subtext, and the chronometer drives a *separate* field. Deleting the loop would freeze the visible call timer. Only the genuinely redundant part was removed — the per-tick `createNotificationChannel` binder call.
- **§0.1 is a warning, not a build failure.** `my-upload-key.jks` is absent from the repo, so the debug-keystore fallback is what runs *today*; failing the build would break `assembleRelease`, which `AGENTS.md` requires for publishing. Promote to an error once the upload key is restored.
- **The `domain/usecase` package was not deleted.** `TrustTier` is declared in `ResolveCallerIdentityUseCase.kt` and is used in production by `TrustBadge.kt`, `InCallScreen.kt:481` and `CallManager.kt`. Removing the package means relocating the enum — a refactor, not a deletion.
- **The seven "dead" DAO methods were not deleted.** All seven are called by `AppRepository` pass-throughs; whether they are truly dead depends on whether those wrappers have callers, which was not traced.
- **§1.2, §1.5, §2.1, §2.3, §2.4, §0.4, §0.5, §0.6 and all of Phases 4–5 remain open.** §1.2 (single filter pass, passing the matched number into `ContactRowItem`) and §2.1 (debouncing the contacts observer) are the highest-value remaining items; §1.5 and §2.3 change observable behaviour and need product judgement.

### Still outstanding, unrelated to performance

- `bb9dd38` and `5651a63` have never been pushed to `origin/main`.
- `stash@{0}` still holds Gemini's abandoned refactor. Its `activeCallCallback` field is now superseded by `3e2c82d`; the rest of it does not compile. Safe to drop.
- §3.4 is unanswered: it is still not known whether the reported slowness was observed on a debug or a release build. If it was debug, the felt improvement from these fixes will be larger than the release-build improvement, and §3.2 (baseline profile) moves to the top of the list.

