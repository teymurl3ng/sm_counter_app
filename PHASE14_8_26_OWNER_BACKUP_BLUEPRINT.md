# Phase 14.8.26 — Owner Backup blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Backup is a **leadership More deep destination** under “Safety and recovery” — Drive vault **status** read panel. Still:

1. **Role-neutral content** — status panel, loading, failure, export/restore/connect preview presentation without foreign nav.
2. **Role-owned shell** — `OwnerScaffold` / Owner bottom nav; Back → Owner More menu.
3. **Foundation proof** — Owner chrome ✓, foreign chrome ✗, Back → More, session intact.

**Forbidden:** mounting hybrid Backup React workspace; requiring full `CloudBackupCoordinator` for status; executing export / restore / Drive connect in this first slice.

Reuse Owner Shop settings / Costing status-panel pattern under Owner More. Dedicated `NativeOwnerBackupFacade` + Backup destination. Prefer `CloudBackupSettingsStore.read()` like Owner Overview backup attention.

## Connected (first slice)

- Backup destination under Owner More (replace Backup & restore deferred notice).
- Status from `CloudBackupSettingsStore`: configured/enabled/account, schedule copy, last success/attempt/error, stale/pending, statusHeadline.
- Export / Restore / Connect Drive: explicit preview notices only.

## Explicit preview / deferred

- Local encrypted export, restore, Drive connect/enable write flows
- Manager / Admin Backup shells

## Native data boundary

`NativeOwnerBackupFacade(database, security, cloudBackupSettings?)`: Owner + `CREATE_EXPORT_BACKUP`; settings-store status only; never bridge JSON; no Room/schema or crypto changes.

## Accept gate

**AUTO-ACCEPTED.** Connected: Backup status panel under Owner More; write actions deferred with explicit preview.
