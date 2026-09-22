# Phase 14.8.44 — Owner Backup export / restore / Drive connect

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Backup write depth is inside the existing Owner Backup surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — status panel, export/restore/connect forms, passphrase fields, confirms, loading, failure, draft leave-guards. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Backup wrappers under `OwnerScaffold(More)`; hardware Back clears reauth → form → status → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Owner Backup Foundation (host chrome ✓, foreign chrome ✗, Back → More menu, session intact) still covers the destination; writes are depth on that same entry.

**Forbidden:** mounting hybrid Backup React workspace; calling JSON bridge from Compose; inventing crypto or changing `BackupManager` / `CloudBackupCryptography` algorithms; inventing cloud restore list/inspect/restore in this slice.

## Connected (this slice)

Replace Backup “Export / Restore / Connect Drive” preview notices with typed write flows.

### Local encrypted export

1. Owner opens Export form → enters backup passphrase (validated by `AppPasswordPolicy.validateBackupPassphrase`) → picks destination via SAF `CreateDocument`.
2. Facade `exportEncrypted(session, uri, passphrase)` → `BackupManager.exportEncryptedSnapshot` after Owner + `CREATE_EXPORT_BACKUP` + recent authentication (quick PIN allowed, matching bridge).
3. Wipe passphrase `CharArray` in `finally`. Audit success like the bridge.

### Local restore (two-step)

1. Owner opens Restore form → enters passphrase → picks backup via SAF `OpenDocument`.
2. Facade `inspectEncrypted(session, uri, passphrase)` → `BackupManager.inspectEncryptedSnapshot`; retain one-use restore ticket (passphrase held only in ticket; wipe on cancel/expiry/restore).
3. Confirm with explicit checkbox + confirmation awareness → facade `confirmRestore(session, ticket, confirm=true)` → `BackupManager.restoreEncryptedReplacing` (full business+planning+preferences+security). Require `RESTORE_BACKUP` + recent full authentication.
4. Sessions invalidated after restore — native host returns to authentication (same as bridge). Surface wrong-passphrase / tampered / incompatible errors honestly. Wipe passphrase always.

### Drive connect / enable / run-now / disconnect

Wire through typed `NativeOwnerCloudBackupGateway` backed by `InteractiveCloudAuthorization` + `CloudBackupCoordinator` + `CloudBackupScheduler` (same shape as `CloudBackupBridgeHandler`, no bridge JSON):

1. **Connect** — Compose collects recovery passphrase + allow-metered; gateway runs Google account/Drive authorization Activity results, then `coordinator.configure` + schedule apply + run-now + notification permission request.
2. **Enable / pause** — `coordinator.updatePolicy(enabled, allowMetered)` + scheduler apply.
3. **Run now** — scheduler `runNow` when configured and enabled.
4. **Disconnect** — confirm revoke → `authorization.revokeSelectedAccount` + cancel schedules + `disconnectLocal`.

## Honest test / live boundaries

| Path | Instrumentation / unit | Live device |
|---|---|---|
| Local export → inspect → restore roundtrip | **Connected** — temp-file Uri via facade (real `BackupManager` crypto) | Same + real SAF pickers |
| Wrong passphrase / tamper errors | **Connected** — facade assertions | Same |
| Drive Connect / Disconnect / Enable / Run now UI + facade transitions | **Contract + fake gateway / settings store** — no live Google account picker in CI | **Manual** — Google account picker + Drive OAuth require device Google Play services and configured `GOOGLE_WEB_CLIENT_ID` |
| Cloud vault remote list / inspect / restore from Drive | **Deferred** | Deferred |

## Native data boundary

`NativeOwnerBackupFacade`:

- Keep `load` from `CloudBackupSettingsStore`
- `exportEncrypted` / `inspectEncrypted` / `confirmRestore` / `cancelRestoreTicket` / `reauthenticate`
- Cloud: `connectDrive` / `setDrivePolicy` / `runDriveBackupNow` / `disconnectDrive` via injectable `NativeOwnerCloudBackupGateway`
- never call bridge JSON; do not change Room schema, RolePermissions, or BackupManager algorithms

## Explicit preview / deferred

- Cloud remote snapshot list / inspect / restore from Drive
- Manager / Admin Backup shells
- Live Google account picker in instrumentation (fake-tested only)

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Backup local export + two-step restore + Drive connect/enable/run-now/disconnect typed wiring under CORE METHOD; live Google OAuth proven only on device; cloud remote restore remains deferred.
