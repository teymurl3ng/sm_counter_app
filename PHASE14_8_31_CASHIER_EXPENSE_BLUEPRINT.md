# Phase 14.8.31 — Cashier Expense host blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Expense is **multi-role** (`AppPermission.EXPENSE` for Owner + Cashier). Owner already hosts shared content via `OwnerExpenseEntryScreen` (14.8.27). This slice adds the **Cashier shell** only:

1. **Role-neutral content** — reuse `ExpenseEntryContent` / loading / failure / discard. No `OwnerSection` / `CashierSection` / foreign bottom nav inside content.
2. **Role-owned shell** — `CashierExpenseEntryScreen` wraps content in Cashier scaffold with More selected; hardware Back → Cashier More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits.
3. **Foundation proof** — Cashier chrome present, Owner/Ops chrome absent, Back → More menu, session stays authenticated.

**Forbidden (P0):**

- Mounting `OwnerExpenseEntryScreen` or any Owner shell inside Cashier.
- Translating `OwnerSection` events inside Cashier.
- Putting role nav inside `ExpenseEntryContent`.
- Treating menu-callback-only as hosted-entry coverage.
- Inventory `COVERED` with `workflow_test=-`.

## Where this checkpoint starts

Cashier Sell / Collect / Activity / More (Count cash + Personal security) are connected. Expense content + `NativeExpenseFacade` already post via `OperationsBridgeCommandService.recordExpense` under Owner Work. Cashier More still lacks Expense.

Authority: `PHASE14_8_2_ROLE_CALIBRATION.md` (Cashier More includes Expense), `PHASE14_8_27_OWNER_EXPENSE_BLUEPRINT.md`, Cashier Collect shell pattern.

## Connected

- Cashier More → **Expense** row → `CashierExpenseEntryScreen` + shared content.
- Same facade / VM posting path as Owner.
- Back → More menu; leave-guard on exits.

## Out of scope

- Deliveries from Cashier More, day close/reopen UI, Owner/Manager hosts.
- Changing expense domain / category CRUD.
- Rewriting `ExpenseEntryContent`.

## Verification

- Contract: Cashier chrome ✓, Owner nav ✗.
- Foundation: open under Cashier More + hardware Back → More + session intact.
- Inventory COVERED → those tests.
- `qaScreen -Pqa.screen=more` PASS.
- `assembleDebug` → `adb install -r` → launch.

## Accept gate

**AUTO-ACCEPTED.** Connected = Cashier-hosted expense form under CORE METHOD. Shared content unchanged.
