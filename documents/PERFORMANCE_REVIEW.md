# OmniDial Performance Review

**Repository:** github.com/hariburle/OmniDial
**Reviewed against:** commit `bb9dd38` (current `main`, 2 commits ahead of `origin/main` @ `f301cc9`)
**Date:** 2026-09-20
**Scope:** runtime performance, redundant/duplicated work, dead code. Build config included.
**Method:** four parallel read-only passes (data layer, contacts pipeline, UI/state layer, telecom/services), then manual verification of the highest-impact claims at their exact lines.
**Status:** review only. No code was changed.

**Companion document:** `documents/omnidial-code-review.md` is an earlier, intact review of commit `f301cc9`. It is not superseded by this one — see [Overlap with the earlier review](#overlap-with-the-earlier-review). Where both documents report the same defect, this one adds measured call-site counts and verified line numbers against current `main`.

---

## Verification key

Every finding is tagged:

- **[V]** — verified by reading the cited lines directly.
- **[R]** — reported by a review pass with a precise line cite, not independently re-read. Treat as high-confidence but spot-check before acting.

---

## Root cause

The app has no single catastrophic bottleneck. It has **three expensive primitives that are called inside O(n) loops which re-run on every keystroke, every scroll frame, and every call event.** Almost every symptom traces back to one of these.

| Primitive | True cost per invocation | Call-site count |
|---|---|---|
| `ContactHelper.matchesNumberQuery` (`ContactHelper.kt:961`) | 6 regex compilations, 2 full sorts of the country-code table, 2 libphonenumber parses | per contact × per phone number × per keystroke **[V]** |
| `PhoneNumberNormalizer.toE164` (`PhoneNumberNormalizer.kt:51`) | libphonenumber `parse` + `isValidNumber` + `isPossibleNumber` + `format`. **No memoization anywhere.** | ~30 sites incl. entity constructors and "cached" lookups **[V]** |
| `AppDao.getAllRecentCallsList` / `getAllLocalContactsList` / `getAllFavoritesList` (`AppDao.kt:73`, `:148`, `:83`) | unbounded full-table scan, no `LIMIT` | 6 / 8 / **20** call sites **[V]** |

Fixing the primitives fixes most of the call sites at once. That is the recommended strategy — it preserves every feature because it changes *how* work is done, not *what* work is done.

---

## Tier 1 — highest user-visible impact

### 1.1 `matchesNumberQuery` performs the cheap check last **[V]**

**File:** `util/ContactHelper.kt:961-997`

The plain digit-substring test that resolves the overwhelming majority of searches sits at **line 981** — *after*:

- two `extractCountryCallingCode` calls (972-973), each of which compiles `Regex("[^0-9+]")` (line 879) and rebuilds `countryCallingCodes.values.distinct().sortedByDescending { it.length }` (line 887) — a sort of the entire country-code table, per invocation
- `isSamePhoneNumber` (line 978), which itself calls libphonenumber (line 919), compiles two more regexes (923-924), calls `extractCountryCallingCode` **two more times** (928-929), and scans the country-code table twice again via `normalizeToLocalDigits` (937-938, loop at 864-868)

**Cost:** at 5,000 contacts with an average of 1.5 numbers each, roughly 10,000 libphonenumber parses and 45,000 regex compilations **per keystroke**, on the main thread.

**Fix:** reorder so the digit-substring check runs first and libphonenumber is only consulted for genuinely ambiguous cases. Hoist `sortedCodes` and all regex literals to file-level `val`s.

**Feature risk:** none. Pure reordering — the predicate's truth table is unchanged.

---

### 1.2 The same filter pass runs twice, then again per visible row **[V]**

**Files:** `ui/screens/ContactsScreen.kt:250-278`, `:384-400`; `ui/components/ContactRowItem.kt:63-68`; `util/T9Helper.kt:75-82`

- `ContactsScreen.kt:250-278` (`filteredContacts`) and `:384-400` (`allSearchQueryMatches`) apply the **identical** predicate over the **same** list. When `smartSortBy == ALL && sourceFilter == ALL`, the second pass feeds `otherFilteredOutMatches` (403-404), which immediately returns `emptyList()` — the expensive pass runs and its result is discarded.
- `ContactRowItem.kt:63-68` — `remember(searchQuery, contact)` re-runs `matchesNumberQuery` up to `phoneNumbers.size + 1` times **for every visible row on every keystroke**, purely to pick which number to highlight. The filter pass already knows the answer.
- `T9Helper.kt:75-82` — calls `matchesNumberQuery` **four times per contact**: twice to compute `matchedPhone` (75-76), twice more to compute `matchingPn` (79-80).

**Fix:** run one filter pass and carry the matched number alongside the contact; pass it into `ContactRowItem` as a parameter instead of recomputing; in `T9Helper`, derive `matchedPhone = (matchingPn != null)`.

**Feature risk:** none.

---

### 1.3 `refreshRecentCalls()` is O(M²) and fires up to six times per call **[V]**

**File:** `ui/MainViewModel.kt:616-733`

Loads the **entire** `recent_calls` table (line 638, unbounded), fetches 100 system call-log rows, then:

- per-row deleted-registry scan via `deletedCallKeys.any { startsWith… }` (631-635)
- `roomCalls.firstOrNull { isSamePhoneNumber(…) }` per system call (647-659)
- a dedup pass using `deduplicated.indexOfFirst {…}` (684-712) — **O(M²)**

At M ≈ 2,100 rows the dedup alone is on the order of 4.4M `isSamePhoneNumber` calls, each a libphonenumber parse pair.

**Trigger count — verified:** `init` (585); `activeCall` disconnect collector (596); the `callLoggedEvent` collector which calls it **three times with hardcoded `delay(600)` and `delay(1200)`** (601-608); the CallLog `ContentObserver` (893, which Android fires multiple times per call); `placeWhatsAppCall` (1346); `placeGoogleVoiceCall` (1385); note save (1631); deletes (1658, 1683); `MainActivity.onNewIntent` (222); `onResume` (238); permission callback (654); channel-config save (1183).

One hangup therefore triggers roughly five full merges within two seconds. The `AtomicBoolean` guard at line 617 prevents *concurrent* runs but not *repeated* ones.

**Fix:** delete the triple-delay collector and the `activeCall` disconnect refresh (the `ContentObserver` already covers both). Replace the `indexOfFirst` dedup with a `HashMap<last10Digits, RecentCall>` index so matching is O(1).

**Feature risk:** low. The `ContentObserver` remains as the trigger, so system call-log rows still appear. Verify timing on a real device — the delays were presumably added to work around late system log writes.

---

### 1.4 Hard-coded debug number executes two full-table pipelines on every cold start **[V]**

**File:** `ui/MainViewModel.kt:610-613`

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    removeSpam("+1 469-731-3343")
    removeSpam("4697313343")
}
```

Each `removeSpam` (2151-2183) pulls `getAllSpamNumbersList()` **and** `getAllRecentCallsList()`, loops every row doing per-row digit normalization, issues per-row `updateRecentCall`, and writes SharedPreferences. Two of these run on the startup critical path, competing with `refreshContacts()` and `refreshRecentCalls()`.

This is also a **personal phone number shipping to all users.**

**Fix:** delete. Independently identified in `omnidial-code-review.md` finding #2.

**Note:** the stashed working-tree edit (`git stash@{0}`, tag `omnidial-broken-normalized-refactor-20260920`) had already correctly removed this block.

**Feature risk:** none.

---

### 1.5 Root composition collects ~45 StateFlows and keeps all five pages permanently composed **[V]**

**Files:** `MainActivity.kt:439-484`, `:863`

Every StateFlow is collected at the root composable, making the root the recomposition scope for all of them. `beyondViewportPageCount = 4` (line 863, verified) means Contacts, CallLog, Favorites, Rules and Settings are **all** composed at all times.

Consequence: any single emission — including **every dialer keystroke** via `dialerNumber`, and every per-second call tick — recomposes the root. Because `MainViewModel` is an unstable capture and ~60 inline lambdas are passed down (872-938, 948-1001), each lambda is a **new instance per recomposition**, which defeats child skipping and invalidates every `remember` keyed on those lambdas.

**Fix:** `beyondViewportPageCount = 1`; move `collectAsStateWithLifecycle` down into each page composable; pass method references (`viewModel::isSpamNumber`) rather than inline lambdas; enable Compose strong-skipping mode.

**Feature risk:** low. Page state is retained, but an off-screen page may compose on demand during a swipe. Test swipe feel on a low-end device.

---

## Tier 2 — structural

### 2.1 Contact-sync feedback loop **[R]**

**File:** `ui/MainViewModel.kt:872-882`, `735-868`, `925-1094`

The contacts `ContentObserver` is registered with `notifyForDescendants = true`, so the app's **own** contact writes (e.g. `updateContactNickname` inside `addFavorite`, line 1995) trigger `refreshContacts()` → `syncWithDeviceContacts()` → further writes → further notifications.

Inside `syncWithDeviceContacts`, `getAllFavoritesList()` is called **five separate times** (931, 951, 1050, 1088, 1089) and matching is `allDevContacts.firstOrNull { … phoneNumbers.any … }` per favorite — O(F × D × P), roughly 50 × 3,000 × 1.5.

Additional triggers: every `onResume` (`MainActivity.kt:235`), permission grant (655-656, which calls `syncWithDeviceContacts()` a second time), and after every contact mutation (1824, 1872, 1882, 1920, 2359, 2388, 2428).

**Fix:** debounce the observer (~1s); fetch favorites once and thread the list through; index device contacts by normalized digits for O(1) matching; remove the redundant second `syncWithDeviceContacts()` at `MainActivity.kt:656`.

---

### 2.2 In-call notification is fully rebuilt every second **[V]**

**File:** `telecom/CallForegroundService.kt` (`manageTicker`, ~144-163)

A `while(isActive) { delay(1000); showCallNotification(...) }` loop re-posts the notification at 1 Hz for the whole call. Each post rebuilds the notification channel, five `PendingIntent`s and the `CallStyle` — on the main thread.

This is entirely redundant: `setUsesChronometer(true)` is already set (`OngoingCallNotificationHelper.kt:151-156`), which ticks natively at zero CPU cost.

The notification is additionally posted **three times on call add** within milliseconds: `CallManager.kt:321`, `TelecomCallService.kt:34-36`, `CallForegroundService.kt:136`.

**Fix:** delete the ticker; keep only the foreground-service post. Remove `createNotificationChannel` from `buildCallNotification` (already created in `CallManager.init`, line 163).

**Feature risk:** low — confirm no in-call text depends on the 1 Hz rebuild rather than the chronometer.

---

### 2.3 Accelerometer and proximity sensors never stop **[R]**

**File:** `telecom/FlipToShhhManager.kt:64-124`

When the feature is enabled (default `true`, line 69), the accelerometer and proximity sensor are registered at `SENSOR_DELAY_UI` for the **entire app lifetime**, not just while ringing. `onSensorChanged` (line 124) runs on the main thread at roughly 60 Hz, executing `evaluateFaceOrientation` (192) per event.

Separately, `initialize()` launches a new `activeCall` collector on the singleton's scope (line 79) **every time MainActivity is created** (`MainActivity.kt:166`) and never cancels the previous one — after N recreations there are N collectors, each calling `startListening()` per ring.

**Fix:** register only while `activeCall.state == RINGING`; unregister in the collector when no call is active; use `SENSOR_DELAY_NORMAL` with batching (`maxReportLatencyUs`); store the `Job` and cancel before relaunching.

**Feature risk:** medium — flip-to-shhh would only operate while ringing. That is the only time it is meaningful, but confirm it matches product intent.

---

### 2.4 Backup scanning runs on the main thread during startup **[R]**

**Files:** `util/BackupManager.kt:872-1003`, `ui/MainViewModel.kt:124-126`, `:587`

`listLocalBackups` is **not** a suspend function. It performs two MediaStore `LIKE` queries (898-924), copies files into internal storage (913-917, 960), scans 8+ directories, and deletes duplicates. It is invoked synchronously from `refreshLocalBackups` (124-126) inside the ViewModel `init` (587) — i.e. on the main thread on every cold start and every activity recreation.

**Fix:** make it `suspend` with `withContext(Dispatchers.IO)`; load lazily when the Backup screen is first opened.

---

### 2.5 Resource leaks and accumulating state

| Leak | Location | Detail |
|---|---|---|
| `Call.Callback` never unregistered | `CallManager.kt:275` **[V — no `unregisterCallback` exists anywhere in the repo]** | Captures the service `Context`; keeps firing through teardown, causing the double `handleCallEnded` below |
| `handleCallEnded` runs twice per call | `CallManager.kt:293-295` and `:479-484` **[R]** | Service stop + notification cancel then execute twice more with two contexts (494-499); `TelecomCallService.kt:54` cancels a third time |
| `loggedCallSessionIds` grows unbounded | `CallManager.kt:155`, added at `:507` **[R]** | One string per call for the process lifetime; the 4-second dedupe at `:525` already exists, so the set is redundant |
| Proximity wakelock replaced without release | `CallManager.kt:167-179` **[R]** | Recreated on every `init` and `setTelecomService` while the old instance may still be held |
| Two leaked `CoroutineScope(Dispatchers.IO)` | `ChannelPreferenceRepository.kt:18`, `ChannelConfigRepository.kt:14` **[V]** | Unmanaged, never cancelled, each holding a permanent Room observer |
| Broken double-checked locking | `ChannelDispatchCoordinator.kt:172-178`, `ChannelDiscoveryManager.kt:220-226`, `ChannelConfigRepository.kt:147-156`, `ChannelPreferenceRepository.kt:75-83` **[V]** | `INSTANCE ?: synchronized { new instance }` with no re-check inside the lock — a race creates duplicate singletons, each running `refreshChannels()` and its own permanent collector |

**Note on the callback leak:** the stashed edit at `git stash@{0}` had begun this fix — it introduced an `activeCallCallback` field and stored the callback. That portion of the abandoned work was sound and can be recovered with `git stash apply stash@{0}`.

---

### 2.6 Database indexes and blocking preferences **[V]**

**Missing indexes** (`data/Entities.kt`):

- `local_contacts` — no index on `phoneNumber` or `name`, yet `AppDao.kt:163` filters by `phoneNumber` and `:145`/`:148` sort by `name`. Every one of the 8 `getAllLocalContactsList()` call sites pays a full scan plus a sort.
- `ignored_contacts` — no index; `AppDao.kt:124`/`:127` sort by `timestamp` on every emission.
- `offline_spam_numbers` — `AppDao.kt:100`/`:103` sort by `reportCount`, which is not indexed.
- `automation_logs` — no index on `timestamp` despite `ORDER BY timestamp DESC LIMIT 50`.

Contrast with `recent_calls` (Entities.kt:46-53), which is correctly indexed on `normalized_number`, `phoneNumber` and `timestamp`. The pattern is known to the codebase; it just isn't applied consistently.

**Blocking SharedPreferences:** `ChannelConfigRepository` calls `editor.commit()` — a synchronous fsync — in **seven** places (lines 46, 70, 88, 96, 113, 127, 136). Line 46 is inside `syncCustomNamesToPrefs`, which runs on **every emission** of the `channel_configurations` table via the collector at 22-26. Use `apply()`.

---

### 2.7 Caches that don't cache **[V]**

**File:** `data/ChannelPreferenceRepository.kt`

The class maintains a `ConcurrentHashMap` cache (line 15) fed by a permanent Flow collector (17-26). But:

- `getCachedPreference` (28-31) still calls `PhoneNumberNormalizer.toE164(phoneNumber)` on every lookup — the cache avoids the DB query but still pays the most expensive part.
- `getPreferenceForNumber` (33-36) **ignores the cache entirely** and hits the database, while still paying `toE164`. `getPreferredChannelId` (38-40) routes through it.

So the hot path gets neither benefit.

**Fix:** memoize `toE164` (an `LruCache<String, String>` is sufficient — the input space is bounded by the user's contacts and call log), and make `getPreferredChannelId` consult `cachedPreferences` first.

---

### 2.8 Per-item work in list rows **[R]**

**File:** `ui/components/CallLogItem.kt`

- `:81`, `:90`, `:503` — `ChannelConfigRepository.getCustomNameSync(...)` per composed item
- `:394-396` — `CommunityCallerIdService.lookup(call.phoneNumber)`, a linear directory scan with normalization, **per item while scrolling**
- `:75-76` — every visible item independently `collectAsState()`s `availableChannels`
- `:103-104` — `SimpleDateFormat("MMM d, h:mm a", …)` allocated per composition, with no `remember`

**Fix:** resolve channel labels once in `CallLogScreen` and pass strings down; batch the community lookup into a derived map; share one `remember`ed formatter.

**Related:** `ui/screens/CallLogScreen.kt:726` **[V]** uses item key `"${id}_${timestamp}_$index"`. Including `index` means prepending a single call **re-keys the entire list**, destroying item state and animations. Drop `$index`.

---

## Tier 3 — build configuration **[V]**

### 3.1 ProGuard rules disable R8 optimization on the two hottest paths

**File:** `app/proguard-rules.pro`

```
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.google.i18n.phonenumbers.** { *; }
-keep class com.google.i18n.phonenumbers.data.** { *; }
```

`-keep … { *; }` prevents R8 from optimizing (inlining, devirtualization, constant folding) every class and member of Room and libphonenumber — precisely the two libraries on the hot paths identified above. `isMinifyEnabled = true` is set for release (`app/build.gradle.kts`), so R8 runs, but it is barred from touching these.

Both libraries ship their own consumer ProGuard rules. **All five lines are redundant and should be deleted.**

### 3.2 No baseline profile

No `baseline-prof.txt`, no `androidx.baselineprofile` plugin anywhere in the project. For a Compose app this typically costs 20-40% of cold-start time and scroll jank, because startup code runs JIT-only until the profile is built on-device.

This is the highest value-to-effort item in the entire review.

### 3.3 No Compose stability configuration

With `List<T>` parameters passed into many composables, a stability config file would materially improve skippability. Entities are already correctly annotated `@Immutable` (`Entities.kt`), so the groundwork exists.

### 3.4 Measurement caveat

`AGENTS.md` line 15 directs the use of `installDebug` for device testing. Debug builds are unminified, have no R8, and run JIT-only — Compose in debug is several times slower than release. **Any performance judgement made against a debug install is not representative.** Confirm which build type the reported slowness was observed on before prioritizing.

---

## Redundant and dead code

### Confirmed dead **[V]**

- **`domain/usecase/ResolveCallerIdentityUseCase.kt`** — **zero references** anywhere in the repository, including tests.
- **`domain/usecase/EvaluateSimRuleUseCase.kt`** — referenced only from `Phase10TelecomTest.kt`. Never used in production code.
- `CallManager.kt:325-476` re-implements both use cases' logic inline (duplicating `:1444-1469` vs `ResolveCallerIdentityUseCase.kt:98-112`).

The entire `domain/usecase` package is dead weight while its logic is duplicated in the telecom layer. Either delete it or route `onCallAdded` through it — the earlier review's "collapse or actually use" observation applies here too.

### Confirmed dead / unused **[R]**

- `CallManager.normalizePhoneNumber` (`:1471-1473`) — private, never called.
- `OngoingCallNotificationHelper.buildCallNotificationWithoutStyle` (`:244-361`) — ~120 lines duplicating `buildCallNotification`, reachable only from a catch block at `:235`; `CallStyle` already has its own try/catch (`:169-186`), so the outer path is near-unreachable.
- **`ContactRowItem` accepts 9 unused parameters** (`onRequestCall`, `onCallDirect`, `onSelectNumber`, `onSmsClick`, `onCreateRule`, `onToggleFavorite`, `onPlaceWhatsAppCall`, `onSyncToPhone`, `getPreferredCallingMode`) plus an unused `LocalContext`. All three call sites allocate lambdas for them on every recomposition — which is exactly why the row cannot skip.
- `DialerScreen.kt:219` — `val effectiveContacts = deviceContacts`, a pointless alias.
- `ChannelConfigRepository.kt:54` — `val now = nextTimestamp()` is never used (line 55 calls `nextTimestamp()` again inside the `map`).
- Unobserved state in `MainViewModel`: `favoriteCardStyle` + `setFavoriteCardStyle` (270-276), `isCallRedirectionRoleHeld` (321-322, UI uses its own `RoleHelper` call at `CallRedirectionCard.kt:34`), `notSpamWhitelist` public flow (202), `selectedCallReason`/`selectCallReason` (1155-1160, `DialerScreen.kt:227` keeps its own local state).
- `MainViewModel.kt:2515-2518` — static `inMemoryCached*` companion caches duplicate StateFlow content and leak across ViewModel instances.
- `SettingsScreen.kt:91` re-collects `allDiscoveredChannels`, already collected at `MainActivity.kt:576` and passed in as `discoveredChannels`.

### Duplicated logic **[R]**

- `ContactHelper.isSamePhoneNumber` (`:901-954`) wraps `PhoneNumberNormalizer.isSamePhoneNumber` (`:121-192`) and then repeats its own digit and country-code fallbacks (923-951) that largely duplicate the normalizer's E.164/NSN fallbacks — double parse work per comparison. Two parallel phone-number matching stacks.
- `ContactHelper.fetchStarredContacts` (`:1256-1337`) vs `fetchDeviceContacts` (`:1583-1661`) — ~95% identical cursor/accumulator code; only the `STARRED = 1` selection differs.
- `shouldSuggestWhatsApp(String)` / `shouldSuggestWhatsApp(Context?, String)` (`:849-855`) — trivial aliases of `isInternationalNumber`.
- `ContactsScreen.kt:897-913`, `:951-967`, `:1049-1059` — three near-identical `isFav` blocks, each compiling `Regex("[^0-9+]")` per contact per number. The same logic appears a fourth time at `:326-337`.
- `MainViewModel.kt:1594-1633` (`updateRecentCallNoteAndReminder`) vs `:1726-1775` (`savePostCallNote`) — near-identical note/reminder persistence plus scheduler logic.
- `MainViewModel.kt:1331-1347` (WhatsApp) vs `:1370-1386` (Google Voice) — copy-pasted call-logging blocks, each including an O(D) `deviceContacts.firstOrNull { contains }` name lookup.
- `MainViewModel.kt:141-155` — learned call modes written to **two** duplicate preference keys (`whatsapp_learned_choices` and `learned_call_modes`), then unioned on read (128-139).
- `FavoritesScreen.kt:335-360` builds `callCounts`; `:362-454` builds the same map again (407-413) and rebuilds ignored-contact norms (371) that already exist at 284-289.
- `markAsSpam` (`MainViewModel.kt:2139-2147`), `removeSpam` (`:2165-2171`, `:2174-2182`) — load entire tables then loop to flip one flag. Replaceable by a single `UPDATE … WHERE` on the already-indexed `normalized_number` column.
- `CallManager.kt:363` — `getSpamByNormalizedNumber(n) ?: getSpamByNumber(number, n)`. The second query's `WHERE phoneNumber = :number OR normalized_number = :normalizedNumber` (`AppDao.kt:106`) is a strict superset of the first. One call suffices.
- Duplicated cold-start loads: `MainViewModel.kt:556-583` loads `recent_calls` and `local_contacts` into state, then `refreshContacts()` (584) and `refreshRecentCalls()` (585) immediately load the same two tables again.

### Already done well

Worth recording so it isn't "fixed" by mistake:

- `DialerSuggestionsList` correctly hoists lookup maps into `remember(contacts)` and keys its `items` (48-67, 97, 188).
- `ContactsScreen` LazyColumn `key`s are correct (896, 950, 1048).
- `DialerScreen`'s T9 search is debounced (80 ms) and runs on `Dispatchers.Default` (307-311) — the problem is per-item cost, not threading.
- In-call duration is already isolated in `CallDurationStatusChip` (`InCallScreen.kt:279-284`).
- `ChannelDiscoveryManager` caches discovery results in StateFlows and uses cheap `getPackageInfo` probes (207-214). **`getInstalledPackages()` appears nowhere in the codebase.**
- `CommunityCallerIdService.lookup` is a pure in-memory list operation.
- `recent_calls` indexing (`Entities.kt:46-53`) is correct.
- `TelecomApplication.kt` and `SmartDialerWidgetProvider.kt` are clean.
- `AppCoroutineScope` and `HapticFeedbackHelper` are clean.

---

## Overlap with the earlier review

`documents/omnidial-code-review.md` (commit `f301cc9`) independently found:

| Earlier finding | Also in this review | Delta |
|---|---|---|
| #2 hard-coded test number | §1.4 | Same. Confirmed still present at `bb9dd38` |
| #3 incoming-call callback leak | §2.5 | Same. Confirmed no `unregisterCallback` exists in the repo |
| #6 favorites table scanned 4× per call | §root cause table | This review adds the full count: **20** `getAllFavoritesList()` call sites, incl. five inside one function |
| #8 typing jank | §1.1, §1.2 | This review identifies the specific mechanism (cheap check ordered last) and the per-row recomputation in `ContactRowItem` |
| `markAsSpam`/`removeSpam` full-table loads | §redundant code | Same |
| Two parallel phone-number matching stacks | §duplicated logic | Same |
| libphonenumber in entity constructors | §2.7 | This review adds that no memoization exists anywhere |

**Found only in the earlier review** (not re-verified here, but should be actioned):

- #1 release builds silently fall back to `debug.keystore` when the upload key is missing — a genuine security issue
- #4 backup restore can leave a half-wiped database (no `@Transaction`)
- #5 `fallbackToDestructiveMigration()` combined with migrations that swallow all exceptions
- #7 main-thread DB work in `CreateContactDialog.kt:136-155`
- Ambient automation on the hot call path (Wi-Fi SSID / Bluetooth matching against ~30 hard-coded car brands)
- Firebase BOM + Google Services + Secrets plugins configured with zero Firebase usage
- `apks/` holding 17 APKs at ~218 MB in git

**Found only in this review:** §1.3 (trigger count for `refreshRecentCalls`), §1.5 (`beyondViewportPageCount = 4` and root-level flow collection), §2.2 (1 Hz notification ticker and triple posting), §2.3 (always-on sensors and collector leak), §2.6 (missing indexes, `commit()` on every emission), §2.7 (caches that don't cache), §2.8 (per-item row work and the index-in-key bug), §3.1 (ProGuard disabling R8 optimization), §3.2 (no baseline profile), and the dead `domain/usecase` package.

---

## Suggested sequence

Ordered by felt improvement per unit of effort. Nothing in Phases 1-3 removes a feature.

**Phase 1 — an afternoon, no behaviour change**
1. §1.4 delete the debug `removeSpam` block
2. §1.1 reorder `matchesNumberQuery`; hoist regexes and `sortedCodes`
3. §3.1 delete the five redundant ProGuard keeps
4. §2.8 drop `$index` from the `CallLogScreen` item key

**Phase 2 — a day, low risk**
5. §1.2 single filter pass; pass matched number into `ContactRowItem`; dedupe T9
6. §1.3 remove triple-delay refresh; index the dedup
7. §2.2 remove the 1 Hz ticker and the duplicate notification posts
8. §2.7 memoize `toE164`

**Phase 3 — build config**
9. §3.2 add a baseline profile
10. §2.6 add the four missing indexes (requires a Room migration — see earlier review finding #5 about the destructive fallback before doing this)

**Phase 4 — needs product judgement**
11. §1.5 restructure root composition
12. §2.1 debounce the contacts observer
13. §2.3 gate sensors to ringing state
14. §2.5 fix the leaks and the double-checked locking

**Phase 5 — cleanup**
15. Delete the dead `domain/usecase` package, the unused `ContactRowItem` parameters, and the duplicated helpers listed above.
