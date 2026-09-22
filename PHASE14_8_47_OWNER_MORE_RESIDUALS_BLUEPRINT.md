# Phase 14.8.47 — Owner More residuals (opening / credential reset / cloud restore)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

These three residuals are **write depth inside existing Owner More destinations** (Shop settings / Users & roles / Backup) — not new tiles and not foreign-role hosts:

1. **Role-neutral content** — opening form, credential-reset passphrase step, cloud remote list/inspect/confirm, loading, failure, draft leave-guards. No foreign bottom nav / section enums in reusable content.
2. **Role-owned shell** — continue existing Owner wrappers under `OwnerScaffold(More)`; hardware Back clears nested forms → destination panel → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Foundation tests for Shop settings / Users / Backup still cover chrome + Back → More + session; residual forms are depth on those entries.

**Forbidden:** bridge JSON from Compose; inventing crypto / domain formulas; Admin Drive restore (`localExportOnly` stays); rewriting Manager packages.

## Connected (this slice)

### A. Shop settings — opening record / correct (closes 14.8.41 deferred)

Replace the opening-section deferred notice with state-driven UI:

| State | UI |
|---|---|
| No store identity | Honest notice: save shop identity first |
| No opening + events already exist | Honest notice: opening can no longer be recorded |
| No opening + record allowed | **Record** form |
| Opening recorded | Summary + **Correct** form |

Typed path mirrors `SettingsBridgeCommandService`:

1. `MANAGE_BUSINESS_SETTINGS` + Owner/Admin gate (existing Shop settings boundary)
2. Recent **full** passphrase authentication (`requireRecentAuthentication`, no quick PIN)
3. Conflict guards: already recorded / business events present (record); missing opening (correct)
4. `OpeningStateService.record` / `correct` with `OpeningStateInput` (openingDate, inventoryBalances, accountBalances, notes)
5. Account balances entered as PKR Doubles (same money units as domain); wipe reauth `CharArray`s

Leave-guard while the opening draft is dirty. Snapshot exposes products/accounts for the form plus current balances for correction prefills.

### B. Users & roles — credential reset (closes 14.8.42 deferred)

From user detail:

1. **Reset credentials** → `UserSecurityService.beginCredentialReset` via `NativeOwnerUsersRolesFacade.beginCredentialReset`
2. If `RecentAuthenticationRequired` → Owner full-passphrase reauth then retry
3. On pending success → same passphrase step as enroll → `completeUserEnrollment(token, passphrase)` (reuse enroll complete)
4. Wipe all passphrase `CharArray`s; leave-guard on passphrase draft

### C. Backup — cloud remote list / inspect / restore (closes 14.8.44 deferred)

Owner-only (`RESTORE_BACKUP`). Wire through typed `NativeOwnerCloudBackupGateway` + `CloudBackupCoordinator` patterns (same shape as `CloudBackupBridgeHandler`, no JSON):

1. **Refresh remote list** when Drive is configured → snapshots on status panel
2. **Inspect** remote file → download/decrypt (local key, else recovery passphrase) → one-use restore ticket
3. **Confirm** checkbox → restore replacing installation → sessions invalidated; disable cloud after restore like bridge
4. Admin Backup stays `localExportOnly` (no Drive list/restore)

#### Honest test / live boundaries

| Path | Instrumentation | Live device |
|---|---|---|
| Cloud list / inspect / restore UI + facade | **Fake gateway** seeded snapshots + real `BackupManager` complete-snapshot bytes | Manual Google Drive |
| Live Google list/network | Not required in CI | Needs Play services + OAuth |

## Native data boundary

`NativeOwnerShopSettingsFacade`:

- `recordOpening(session, OpeningStateInput)` / `correctOpening(session, OpeningStateInput)`
- `reauthenticate(session, passphrase: CharArray)` — wipe in `finally`
- snapshot fields for opening editor options + recorded flag / can-record

`NativeOwnerUsersRolesFacade`:

- `beginCredentialReset(session, targetUserId): NativeOwnerPendingEnrollment`
- reuse `completeEnroll` + `reauthenticate`

`NativeOwnerBackupFacade` + `NativeOwnerCloudBackupGateway`:

- `refreshCloudSnapshots` / `inspectCloudBackup` / `confirmCloudRestore` / `cancelCloudRestoreTicket`
- gateway: `refreshSnapshots`, `downloadAndDecrypt(remoteFileId, recoveryPassphrase?)`, `disableAfterRestore`
- never invent crypto; never call bridge JSON

## Explicit preview / deferred (unchanged)

- Costing budgets / yield / opening cost basis (14.8.43)
- Manager / Admin shells for these Owner-only depths (Admin Shop settings may share opening content; Admin Backup must not gain Drive restore)
- Live Google OAuth in instrumentation

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Shop settings opening record/correct + Users credential reset + Backup cloud remote list/inspect/restore under CORE METHOD; `qaScreen -Pqa.screen=ownermore` PASS; live Google list remains device-manual beyond fake-gateway tests.
