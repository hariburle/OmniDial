# OmniDial: Automatic Backup + Fresh-Install Restore Prompt — Spec

## 1. Goal

Lose no user data across uninstalls, signature-mismatch reinstalls (release ↔ debug),
or accidental wipes — without ever silently overwriting the user's real data.

Two halves, deliberately asymmetric:
- **Auto-backup: quiet and automatic** (throttled, rotating).
- **Restore: loud and manual** — always a one-tap prompt, never silent.

## 2. Automatic backup

### 2.1 Dirty flag
- New `SharedPreferences` key `auto_backup_dirty` (boolean, default false).
- Set to true by a single helper `BackupManager.markBackupDirty(context)` called from
  every user-data write path: notes/reminders, favorites add/remove/reorder,
  spam list changes, rules changes, channel preferences, settings changes.
  (Cheapest correct hook: the `AppRepository` write methods + `MainViewModel`
  mutating functions that bypass it.)
- Cleared only after a successful auto-backup write.

### 2.2 When it runs
- Check on **app background** (`MainActivity.onStop`) and on **app start**.
- Run only if ALL are true:
  - `auto_backup_dirty == true`
  - automatic-backup setting is ON (default ON)
  - `now - last_auto_backup_time >= 6 hours`
  - local database is NOT empty (never back up an empty DB over real backups)
  - no backup/restore already in progress (serialize with a Mutex)
- Runs on `Dispatchers.IO`, fully silent — no toast, no notification. Log only.
- Failures are best-effort: catch, log, leave dirty flag set for next time.

### 2.3 File naming and rotation
- Auto-backups: `omnidial_auto_yyyyMMdd_HHmmss.bak` (distinct from manual
  `omnidial_backup_*.bak`).
- Keep the **newest 5** auto-backups; delete older auto-backups.
- **Never delete manual backups** during rotation.
- Reuse existing `BackupManager.saveLocalBackup()` / signature logic; add a
  filename-parameter or prefix variant.

### 2.4 Settings UI
- In the existing backup settings card: one toggle **"Automatic backup"**
  (default ON) + a read-only line "Last automatic backup: <date/time or Never>".
- No interval setting — fixed 6h keeps it simple.

## 3. Fresh-install restore prompt

### 3.1 Trigger (ALL must hold)
- App start (after onboarding/permissions), AND
- local DB is effectively empty: zero favorites AND zero recents AND zero rules, AND
- `listLocalBackups()` returns ≥ 1 backup passing the existing SHA-256 check, AND
- the newest valid backup is newer than `restore_prompt_dismissed_for`
  (empty/missing = never dismissed).

### 3.2 Behavior
- Show a banner/card on the main screen:
  "We found a backup from <date>. Restore your notes, favorites, and settings?"
  Buttons: **[Restore]** / **[Not now]**.
- **Restore** → run the existing `restoreBackupFromFile` flow, then refresh UI.
- **Not now** → store `restore_prompt_dismissed_for = <newest backup key>`;
  re-prompt only if a strictly newer valid backup appears later.
- NEVER auto-restore. NEVER prompt when the DB is non-empty
  (protects deliberate deletions from being resurrected).

### 3.3 Why not silent auto-load
A "new" backup file may be older than current data, from another device state,
or predate a deliberate cleanup. Silent restore has no undo; a prompt costs one tap.

## 4. Edge cases
| Case | Handling |
|---|---|
| Backup fails signature check | Skip file, try next newest |
| Storage unavailable / low space | Skip silently, retry next trigger |
| Manual "Backup now" tapped | Runs immediately, clears dirty flag, updates last-backup time |
| Auto-backup fires during manual restore | Mutex serializes; backup runs after |
| User downgrades app version | Backup JSON is forward-tolerant (existing `validAppNames` + unknown-field ignore); restore best-effort as today |
| Multiple backups, newest corrupt | Fall back to next valid one for the prompt |

## 5. Test items (for `documents/ToTest.md`)
1. Toggle auto-backup OFF → make changes → background app → no new `omnidial_auto_*` file.
2. Toggle ON → change a note → background → within throttle window no backup; after 6h (or manipulated clock) backup appears silently.
3. Make 7 auto-backups → only newest 5 remain; manual backups untouched.
4. Fresh install (clear data) with backup present → prompt appears, shows correct date.
5. Fresh install → Restore → notes/favorites/settings return.
6. Fresh install → Not now → no re-prompt on restart; new manual backup added later → prompt reappears.
7. Normal start with existing data + backup files present → NO prompt.
8. Deliberately delete all favorites/notes, then restart → NO prompt (DB non-empty check uses recents/rules too; document the exact emptiness rule).
9. Corrupt newest backup (tamper bytes) → prompt offers next valid backup.
10. Release→debug reinstall (data wiped) → prompt rescues data on first start.
