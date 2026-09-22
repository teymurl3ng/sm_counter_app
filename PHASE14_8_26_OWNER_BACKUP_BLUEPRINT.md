# Phase 14.8.26 — Owner Backup blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Backup is a **leadership More deep destination** under “Safety and recovery” — Drive vault **status** read panel. Still:

1. **Role-neutral content** — status panel, loading, failure, export/restore/connect preview presentation without foreign nav.
2. **Role-owned shell** — `OwnerScaffold` / Owner bottom nav; Back → Owner More menu.
3. **Foundation proof** — Owner chrome ✓, foreign chrome ✗, Back → More, session intact.

**Forbidden:** mounting hybrid Backup React workspace; requiring full `CloudBackupCoordinator` for status-only reads.

Reuse Owner Shop settings / Costing status-panel pattern under Owner More. Dedicated `NativeOwnerBackupFacade` + Backup destination. Prefer `CloudBackupSettingsStore.read()` like Owner Overview backup attention. Write depth landed in **14.8.44**.

## Connected (first slice + 14.8.44 write depth)

- Backup destination under Owner More (replace Backup & restore deferred notice).
- Status from `CloudBackupSettingsStore`: configured/enabled/account, schedule copy, last success/attempt/error, stale/pending, statusHeadline.
- **14.8.44 write depth:** local encrypted export, two-step inspect→restore, Drive connect / enable / run-now / disconnect via typed facade + gateway (no bridge JSON).

## Explicit preview / deferred

- Cloud remote snapshot list / inspect / restore from Drive
- Manager / Admin Backup shells
- Live Google account picker in instrumentation (fake-gateway tested; device-manual for real OAuth)

## Native data boundary

`NativeOwnerBackupFacade(database, security, cloudBackupSettings?)`: Owner + `CREATE_EXPORT_BACKUP`; settings-store status only; never bridge JSON; no Room/schema or crypto changes.

## Accept gate

**AUTO-ACCEPTED.** Connected: Backup status panel under Owner More; write actions connected in 14.8.44.
