# OmniDial Code Review

**Repository:** github.com/hariburle/OmniDial
**Reviewed:** 2026-09-20 (commit f301cc9)
**Scope:** App performance, rogue/redundant code, over-engineering
**Note:** Review only. No code was changed.

---

## Verdict

The app works, but it has a god ViewModel, real jank on the incoming-call and typing paths, and a surprising amount of dead code. A few of these are genuine bugs, not just style. Findings are grouped by priority below; all file/line references are approximate.

---

## Fix now — actual bugs and risks

### 1. Release builds can silently sign with the debug key
- **File:** `app/build.gradle.kts` (~40–60)
- If the upload keystore is missing, the release signing config falls back to `debug.keystore` instead of failing. A Play release signed with a debug key is a security and update-integrity problem.
- **Fix:** fail the build loudly when the keystore is absent.

### 2. Hard-coded personal test number in the app
- **File:** `app/src/main/java/com/example/ui/MainViewModel.kt:600`
- `removeSpam("+1 469-731-3343")` runs on every ViewModel creation. A leftover from personal testing is shipping to users.
- **Fix:** delete it.

### 3. Incoming-call callback leak
- **File:** `app/src/main/java/com/example/telecom/CallManager.kt:275`
- `call.registerCallback(...)` has no matching `unregisterCallback` anywhere; each call leaks its callback (which captures a Context) for the process lifetime.
- **Fix:** keep a field reference and call `call.unregisterCallback(...)` in `onCallRemoved`/`handleCallEnded` before dropping `nativeCall`.

### 4. Backup restore can leave a half-wiped database
- **File:** `app/src/main/java/com/example/util/BackupManager.kt` (~403–453)
- Restore does `clearAllFavorites()`, then per-row inserts with no transaction — N transactions for N rows, and an interrupted restore leaves a cleared-but-half-filled DB.
- **Fix:** add `@Transaction` DAO methods like `replaceAllFavorites(list)` (clear + bulk `@Insert`) and call once per section.

### 5. Migration hazard — silent data wipe possible
- **File:** `app/src/main/java/com/example/data/AppDatabase.kt`
- `fallbackToDestructiveMigration()` is combined with hand-written migrations that swallow all exceptions (`catch (_: Throwable) {}`), so a missing or failed migration silently wipes user data instead of surfacing an error.
- **Fix:** remove the destructive fallback or the swallowed exceptions — keep one.

---

## Performance — things users will feel

### 6. Every incoming call scans the entire favorites table 4 times
- **File:** `app/src/main/java/com/example/telecom/CallManager.kt:330, 639, 1228, 1301`
- Each site loads `dao.getAllFavoritesList()` (whole `favorite_contacts` table) then linear-scans in memory — on the hot ring path. The `normalized_number` index already exists but is never used. Also a redundant double spam lookup per call (`:363`), and at `:761` a spam lookup leaves the DAO's normalized default unparsed so the index is never hit.
- **Fix:** add `getFavoriteByNormalizedNumber(normalizedNumber)` to `AppDao` and replace all four scans with one indexed lookup; do one normalized spam lookup.

### 7. Database work on the main thread
- **File:** `app/src/main/java/com/example/ui/components/CreateContactDialog.kt:136–155`
- `fetchDeviceContacts()` and `lookupContactByNumber()` run inside `LaunchedEffect` with no dispatcher switch — jank/ANR risk when the dialog opens.
- **Fix:** move the queries to `Dispatchers.IO` (the same pattern is done correctly in `DialerScreen.kt`).

### 8. Typing janks on large address books
- **Files:** `app/src/main/java/com/example/ui/screens/ContactsScreen.kt:229, 286`, `DialerScreen.kt:366`, `FavoritesScreen.kt:508`
- Every keystroke re-runs an O(n) filter with per-number regex/digit work plus an O(n log n) sort on the main thread; the dialer re-scans all contacts per digit typed.
- **Fix:** debounce the query and move filter+sort off the main thread; pre-index contacts by normalized digits for O(1) lookup.

### 9. Un-debounced observers fire full refreshes; triple refresh per call event
- **File:** `app/src/main/java/com/example/ui/MainViewModel.kt` (~588–598, 607–750, 810–845)
- Every contacts-provider change triggers a full contacts fetch + Room queries + O(n²) dedup; every call-logged event runs `refreshRecentCalls()` three times with hard-coded `delay(600)`/`delay(1200)` — three full queries for one event.
- **Fix:** debounce observer callbacks on a trigger `SharedFlow`; replace the delay-retry chain with a single refresh (the CallLog ContentObserver already covers updates).

### 10. Startup and recomposition waste
- **Files:** `app/src/main/java/com/example/MainActivity.kt:430–475, 851`; `ui/components/CallAnswerViews.kt:60–74`
- ~50 `collectAsStateWithLifecycle()` calls at the root recompose the whole app on any emission; the pager composes all 5 tabs on first frame (`beyondViewportPageCount = 4`); the incoming-call answer row recomposes every animation frame because animated values are read at the top of the composable.
- **Fix:** collect state in the screens that consume it (or combine in the ViewModel); reduce `beyondViewportPageCount` to 1; read animated states only in leaf composables.

---

## Rogue / redundant code

- **Two dead use-case classes** — `EvaluateSimRuleUseCase` is only instantiated in a test; `ResolveCallerIdentityUseCase` has zero references (only its `TrustTier` enum is used elsewhere).
- **Seven dead DAO methods** — `getAllRules`, `getCallHistoryForContact`, `deleteIgnoredContact`, `getContactSimPreference`, `clearAllContactSimPreferences`, `deleteLocalContactById`, `getAllContactSimPreferences` have zero callers.
- **Dead miscellany** — `simulatedTimerJob` (declared, cancelled, never assigned); `normalizePhoneNumber` (defined, never called); a duplicate `FilterChip` import.
- **`normDigits` copy-pasted 12 times** as local functions, plus `Regex` objects recompiled per call in hot paths. Extract one shared util and one top-level compiled `Regex`.
- **`AppRepository` is 101 lines / ~45 methods of pure pass-through** to the DAO — the abstraction adds nothing, and `CallManager` bypasses it anyway. Either delete it or make it the single normalization boundary.
- **Call-metadata enrichment copy-pasted 3×** across call-start paths (`onCallAdded`, `startSimulatedIncomingCall`, `startSimulatedOutgoingCall`). Extract one shared helper.
- **Duplicated UI** — `ContactRowItem` vs `CallLogItem`, `FavoriteGridCard` vs `PopularGridCard`, nickname dialog built twice, `FloatingCallPill` vs `PipCallContent` (~90% identical). Extract shared components.
- **`automation_logs` grows forever**, has no index, and is only read as "latest 50" — index `timestamp` and enforce a cap/TTL.
- **`markAsSpam()`/`removeSpam()` load the entire recents table** to flip one flag. Add a single `UPDATE ... WHERE normalizedNumber = :digits` query.

---

## Over-engineering

- **God ViewModel:** `MainViewModel.kt` is 2,356 lines with ~101 functions and ~33 state declarations, and it re-exports ~12 `CallManager` flows 1:1 — two sources of truth for the same streams.
- **SIM preferences live in Room AND SharedPreferences**, written non-atomically ("for fast cross-service access") — pick one authoritative store.
- **libphonenumber parsing runs inside Room entity default constructors** (`Entities.kt:70, 89, 103`) — every `copy()` and every restore row pays for a full parse. Normalize once at the insert boundary and require the value.
- **Ambient automation on the hot call path** — every incoming call evaluates Wi-Fi SSID and Bluetooth matching backed by hard-coded lists of ~30 car brands and ~15 headphone brands. `WifiManager.connectionInfo` usually returns `<unknown ssid>` anyway. Drop from the hot path or make it opt-in.
- **Firebase BOM + Google Services + Secrets plugins configured, zero Firebase usage**, no `google-services.json` — build baggage.
- **Two parallel phone-number matching stacks** — `ContactHelper` re-implements what `PhoneNumberNormalizer` already does. Consolidate behind one.
- **Repo hygiene:** `apks/` holds 17 APKs at 218MB (repo is 319MB). They belong in GitHub Releases, not git.

---

## Phased plan

**Fix now:** fail the build instead of debug-signing releases; delete the hard-coded test number; unregister the call callback; wrap restore in `@Transaction` DAO methods; replace the 4 favorites scans with one indexed query (small change, biggest perf win).

**Next refactor:** split the ViewModel; debounce the observers; move per-keystroke filtering off the main thread; delete the dead code; collapse or actually use the repository; single store for SIM prefs.

**Optional:** dedupe the UI components; simplify ambient automation; hoist entity normalization out of constructors; move `apks/` to GitHub Releases.
