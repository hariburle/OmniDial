# Rollback Guide — 2026-09-20 performance fixes

Everything below is local. **Nothing has been pushed.** `origin/main` is still at `f301cc9`.

## The safety net

| Ref | Points at | Meaning |
|---|---|---|
| tag `pre-perf-fixes-baseline` | `bb9dd38` | the exact tree as it was before any fix. Annotated, so it cannot be garbage-collected. |
| branch `main` | `bb9dd38` | untouched. All fixes live on a separate branch. |
| branch `perf/fixes-phase1-3` | `3e2c82d` | the three fix commits. **Currently checked out.** |
| `stash@{0}` (tag `omnidial-broken-normalized-refactor-20260920`) | — | Gemini's abandoned, non-compiling refactor. Unrelated to these fixes; left alone. |

## Undo everything

```bash
cd /d/Try/Git-Repos/Kishan-Dialer
git status                 # confirm nothing you care about is uncommitted
git checkout main          # main never moved; this alone restores the old code
```

`main` is still at `bb9dd38`, so switching to it is the whole rollback. If you would rather
keep working on the fix branch but reset its contents:

```bash
git checkout perf/fixes-phase1-3
git reset --hard pre-perf-fixes-baseline
```

## Undo one phase at a time

The fixes are three independent commits, oldest first. Revert from newest to oldest.

| Commit | Phase | Contents |
|---|---|---|
| `2eeeb2e` | 1 | phone-matching hot path, `toE164` memoization, debug `removeSpam` deletion, ProGuard keeps, CallLog item keys, keystore warning |
| `ace68fd` | 2 | recents-merge indexing, preference cache, notification channel, `commit()`→`apply()`, singleton races |
| `3e2c82d` | 3 | `Call.Callback` leak, preference-cache revert to in-place mutation |

```bash
git revert --no-edit 3e2c82d      # undo phase 3 only
git revert --no-edit ace68fd      # undo phase 2 only
git revert --no-edit 2eeeb2e      # undo phase 1 only
```

Phase 3 already modifies a file that phase 2 introduced changes to, so revert phase 3
before phase 2 if you are unwinding more than one.

To undo a single file instead of a whole phase:

```bash
git checkout pre-perf-fixes-baseline -- app/src/main/java/com/example/util/ContactHelper.kt
```

## Verify after any rollback

```bash
./gradlew :app:compileDebugKotlin --offline -q      # expect exit 0
./gradlew :app:testDebugUnitTest --offline          # expect 63/63
```

Both were run clean at `3e2c82d`.

## What has NOT been verified

These fixes are compile-verified and unit-test-verified. They have **not** been run on a
device. Before publishing, check on hardware:

1. **Search** — type a partial number, a full international number (`+91…`), and a name.
   Matching predicates were reordered; results should be identical.
2. **Call log** — place and receive a call; confirm it appears once, not duplicated, and
   that the list does not visibly rebuild when a new call arrives.
3. **In-call notification** — confirm the elapsed timer in the notification body still
   advances (the 1 Hz rebuild was deliberately kept for this reason).
4. **Channel routing** — set a preferred channel for a contact, then call them.
5. **Release build** — `assembleRelease` now emits a warning that the upload keystore is
   missing and that the APK is being debug-signed. Resolve the keystore before publishing;
   the ProGuard keeps were removed, so install the release APK and exercise search, calling
   and the database at least once.
