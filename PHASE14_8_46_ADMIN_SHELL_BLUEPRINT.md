# Phase 14.8.46 — Admin shell blueprint (M0 + A1 + A2)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Admin shell is a **leadership role chrome**, not a hosted Owner / Manager / Cashier / Ops screen:

1. **Role-neutral content** — Work Expense entry, Users & roles, Employees / Customers / Suppliers, Records list-detail, Security history, Costing / Shop settings / Products / local export bodies stay free of `AdminSection` / foreign bottom nav when shared (via `HostedScaffold` / Admin entry wrappers).
2. **Role-owned shell** — `AdminScaffold` + Admin bottom nav + Admin Back path; session stays Admin-authenticated.
3. **Foundation proof** — Admin chrome present; Cashier / Owner / Ops / Manager chrome absent; Back on Work stays on Admin Work with session intact; hosted People destinations Back → People hub; Records destinations Back → Records hub; More destinations Back → More menu; Expense Back → Admin Work.

**Forbidden:**

- Mounting Owner Overview as Admin home.
- Mounting Owner / Manager / Cashier / Ops full screens inside Admin.
- Owner / Admin account create from Admin (enrollable roles: Manager / Cashier / Ops only).
- Showing Owner-only recovery / Drive / restore rows as disabled stubs.
- Treating snackbar “not yet” notices as section destinations.

## M0 scope (complete)

- Nav: **Work / People / Records / More** (`AdminSection`); default **Work**.
- Package: `nativeui/admin/` — `AdminShell.kt`, `AdminPreviewHost.kt`.
- Preview routing: `open_admin=true`; Admin does **not** fall through to Cashier.
- Preview account: `preview.admin`.

## A1 scope (complete) — Work desk + People

| Surface | Status |
| --- | --- |
| Admin Work desk | **Connected** — setup health (enrollment pending, products/rates gaps, staff employee-link gaps); attention / all-clear; secondary Expense launcher |
| Admin Expense entry | **Connected** — `AdminExpenseEntryScreen` + shared `ExpenseEntryContent` (CORE METHOD) |
| Admin People hub | **Connected** — Staff access (Users & roles) + Employees / Customers / Suppliers directory |
| Users & roles under People | **Connected** — shared `OwnerUsersRolesScreen` via `HostedScaffold(AdminScaffold People)`; enroll Manager/Cashier/Ops only |
| Employees / Customers / Suppliers | **Connected** — shared catalog screens under Admin People chrome |
| Facades | `NativeAdminWorkFacade` (ADMIN role); Users / master-data facades allow Admin with existing permission gates |

## A2 scope (this checkpoint) — Records + system More

| Surface | Status |
| --- | --- |
| Admin Records hub | **Connected** — Business records + Security history rows under `AdminScaffold(Records)` |
| Business records | **Connected** — `OwnerRecordsScreen` via `HostedScaffold`; list + read-only detail; corrections write UI deferred with honest notice |
| Security history | **Connected** — `OwnerSecurityHistoryScreen` via `HostedScaffold`; Back → Records hub |
| Admin More menu | **Connected** — system tools: Costing & plans, Shop settings, Products, Local export, Personal security, Switch account, Sign out |
| Costing / Shop settings / Products | **Connected** — shared screens with Admin `HostedScaffold`; Admin role gates on facades |
| Local encrypted export | **Connected** — `OwnerBackupScreen(localExportOnly=true)`; Drive connect / restore / cloud remote **omitted** (not disabled stubs) |
| Personal security | **Connected** — Admin-owned security screen; reuses More facade Quick unlock |
| Omitted (no stubs) | Owner Overview, reopen, Drive connect, restore, recovery, ownership transfer, privileged Owner/Admin user controls beyond Users enroll restrictions |

## A2 deferred

- Corrections write UI on Records detail (inspect-only notice remains)
- Attendance nested under Employees (not a separate People row)
- Cloud remote list / inspect / restore remain Owner-only by product IA

## Authority

- `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md` — Administrator nav and priorities
- Sibling chrome: Manager M2 (`PHASE14_8_45`) / `HostedScaffold`
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`

## Gate

- Foundation: M0 + A1 + `adminRecords*`, `adminBusinessRecords*`, `adminSecurityHistory*`, `adminMore*`, `adminProducts*`, `adminCostingPlans*`, `adminShopSettings*`, `adminLocalExport*`
- Contract: `NativeAdminShellScreenContractTest`, `NativeAdminWorkScreenContractTest`
- `qaScreen -Pqa.screen=adminshell` PASS
- Re-run `qaScreen -Pqa.screen=managershell` when shared HostedScaffold / facades change
- `assembleDebug` + install + launch `open_admin=true`
