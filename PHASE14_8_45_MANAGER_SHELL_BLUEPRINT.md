# Phase 14.8.45 — Manager shell blueprint (M0 + M1 + M2 + M3)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Manager shell is a **leadership role chrome**, not a hosted Owner / Cashier / Ops screen:

1. **Role-neutral content** — Expense / Pay supplier / Cash close / Sale / Collect / Receive milk / Make yogurt / Spoilage / Deliveries / Records list-detail / Products·Customers·Suppliers·Employees bodies stay free of `ManagerSection` / foreign bottom nav (via `HostedScaffold` or Manager wrappers).
2. **Role-owned shell** — `ManagerScaffold` + Manager bottom nav + Manager Back path; session stays Manager-authenticated.
3. **Foundation proof** — Manager chrome present; Cashier / Owner / Ops / Admin chrome absent; Back on Work stays on Manager Work with session intact; hosted entries Back → Manager Work / Close / More menu.

**Forbidden:**

- Mounting Owner / Cashier / Ops / Admin full screens inside Manager.
- Showing Owner-only menu rows as disabled stubs (Pay salary, costing, shop settings, users, backup, reopen form, security audit).
- Translating foreign section navigation events.

## M0 scope (complete)

- Nav: **Work / Close / Records / More** (`ManagerSection`); default **Work**.
- Package: `nativeui/manager/` — `ManagerShell.kt`, `ManagerPreviewHost.kt`.
- Preview routing: `open_manager=true`; Manager does **not** fall through to Cashier.
- Preview account: `preview.manager`.

## M1 scope (complete) — connected

| Surface | Status |
| --- | --- |
| Manager Work home | **Connected** — Owner Work launcher density under Manager chrome; attention / all-clear strip; Expense + Pay supplier shortcuts; Cash close → Close tab |
| Manager Close tab | **Connected** — `ManagerCashCloseEntryScreen` hosts role-neutral `CashCloseEntryContent` with `allowReopen=false` + Owner-only reopen notice |
| Manager Expense entry | **Connected** — `ManagerExpenseEntryScreen` + shared `ExpenseEntryContent` |
| Manager Pay supplier entry | **Connected** — `ManagerPaySupplierEntryScreen` + shared `VendorPaymentEntryContent` |
| Facades | `NativeManagerWorkFacade` (MANAGER role); Expense / VendorPayment / CashClose remain permission-gated |

## M2 scope (complete) — Records + bounded More

| Surface | Status |
| --- | --- |
| Manager Records | **Connected** — `OwnerRecordsScreen` content under `ManagerScaffold(Records)` via `HostedScaffold`; list + read-only detail; corrections write UI deferred with honest notice |
| Manager More menu | **Connected** — bounded shop tools only: Products, Customers, Suppliers, Employees, Personal security, Switch account, Sign out |
| Products / Customers / Suppliers / Employees | **Connected** — shared catalog screens with Manager `HostedScaffold`; writes allowed (`MANAGE_MASTER_DATA`) |
| Personal security | **Connected** — Manager-owned security screen (Cashier/Owner pattern); reuses More facade Quick unlock |
| Omitted (no stubs) | Users & roles, Security history, Costing & plans, Shop settings, Backup & restore, Pay salary, Reopen |

## M3 scope (this checkpoint) — Sale / Collect / Ops Work hosts

| Surface | Status |
| --- | --- |
| Manager Sale entry | **Connected** — `ManagerSaleEntryScreen` + shared `SaleEntryContent` |
| Manager Collect / Record payment | **Connected** — `ManagerCollectionEntryScreen` + shared `CollectionEntryContent` |
| Manager Receive milk | **Connected** — `ManagerReceiveMilkEntryScreen` + shared `ReceiveMilkReadyContent` |
| Manager Make yogurt | **Connected** — `ManagerMakeYogurtEntryScreen` + shared `MakeYogurtReadyContent` |
| Manager Record spoilage | **Connected** — `ManagerRecordSpoilageEntryScreen` + shared `RecordSpoilageReadyContent` |
| Manager Deliveries | **Connected** — `ManagerDeliveriesEntryScreen` + shared `DeliveriesReadyContent` |
| Work home tiles | **Connected** — Record sale / payment / Receive milk / Expense / Make yogurt / Spoilage / Deliveries / Pay supplier / Cash close |

## Still deferred

- Corrections write UI on Records detail (inspect-only notice remains)
- Approval queue write UI
- Pay salary / reopen remain Owner-only by product IA

## Authority

- `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md` — Store manager nav and priorities
- Sibling chrome: `OwnerShell.kt` / `OwnerPreviewHost.kt`; CORE METHOD: `HostedScaffold` + Manager wrappers
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`

## Gate

- Foundation: M0–M3 hosts incl. `managerSale*`, `managerCollection*`, `managerReceiveMilk*`, `managerMakeYogurt*`, `managerRecordSpoilage*`, `managerDeliveries*`, plus M1/M2 suite
- Contract: `NativeManagerShellScreenContractTest`, `NativeManagerWorkScreenContractTest`
- `qaScreen -Pqa.screen=managershell` PASS
- `assembleDebug` + install + launch `open_manager=true`
