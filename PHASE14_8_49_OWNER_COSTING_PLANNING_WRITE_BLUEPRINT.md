# Phase 14.8.49 — Owner Costing & plans budgets + collections targets + yield approve

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Budgets, collections targets, and yield policy approve are Owner/Admin write depth inside the existing Owner Costing & plans surface** — not new More tiles and not a foreign-role host:

1. **Role-neutral content** — planning lists/editors, yield batch selection, reauth presentation, loading, failure, draft leave-guard. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Costing & plans wrappers under `OwnerScaffold(More)` (Admin hosts the same content under Admin More chrome); hardware Back clears reauth → editor → Costing hub → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Owner/Admin Costing & plans Foundation (host chrome ✓, foreign chrome ✗, Back → More menu, session intact) still covers the destination; planning/yield are depth on that same entry.

**Forbidden:** mounting hybrid Costing/Planning React workspace; calling JSON bridge from Compose; changing `BudgetManagement.kt` / `Costing.kt` domain formulas or validation; inventing opening cost basis or approval-queue UIs; inventing mutate-in-place budget formulas.

## Connected (this slice)

Replace Costing deferred planning depth with real writes via typed domain (same permission rules as `CostingPlanningBridgeCommandService` — `MANAGE_BUSINESS_SETTINGS`, Owner/Admin only):

### A. Budgets

1. Planning reads: active (+ archived history optional) budget rows from `BudgetPlanDao` / `BudgetManagementService` categories (`Milk purchases` + `OPERATING_BUDGET_CATEGORIES`).
2. Form fields match `BudgetPlanInput`: `category`, `periodStart`, `periodEndExclusive`, `plannedLiters` (milk) **or** `plannedAmount` (operating PKR).
3. Save → `NativeOwnerCostingPlansFacade.upsertBudget(session, input)` → `BudgetManagementService.createOrRevise`.
4. Copy previous → `copyPreviousBudget(session, category, start, end)` → `BudgetManagementService.copyPreviousPeriod`.
5. Leave-guard while budget draft is dirty.

### B. Collections targets

1. Read active collections targets from DAO / `BudgetManagementService.activeCollectionsTarget`.
2. Editor: Monday `weekStart` + PKR `amount` → facade `createOrReviseCollectionsTarget(session, weekStart, amount)`.
3. Leave-guard while collections draft is dirty.

### C. Yield policy approve

1. Read yield policy versions + candidate production batches (effective production events + `YieldPolicyService.assess` status labels — no inventing ratios).
2. Form fields match `YieldPolicyInput`: selected `acceptedProductionEventIds` (≥30, distinct), `effectiveFrom`, optional `supersedesYieldPolicyId` when an Active baseline exists.
3. Save → `approveYieldPolicy(session, input)` → `YieldPolicyService.approve` (requires recent authentication).
4. On `SecurityFailure.RecentAuthenticationRequired`: same Owner/Admin passphrase reauth + retry pattern as costing policy approve (14.8.43); wipe `CharArray` in `finally`.
5. Leave-guard while yield draft is dirty.

## Native data boundary

`NativeOwnerCostingPlansFacade`:

- `loadPlanning(session)` / budget upsert / copy / collections upsert
- `loadYieldContext(session)` / `approveYieldPolicy(session, YieldPolicyInput)`
- reuse existing `reauthenticate(session, passphrase: CharArray)`
- never call bridge JSON; do not change Room schema, RolePermissions, or domain validation

## Explicit preview / deferred

- Opening cost basis editors (Step 5 — next batch)
- Approval queue (no domain)
- Production-loss assessment write UI
- Free-form inventing of variance/analytics formulas beyond existing entity + `YieldPolicyService.assess` presentation

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner/Admin Costing & plans budgets + collections targets + yield approve under CORE METHOD; opening basis and approval queue remain deferred.

### Gate evidence (2026-09-22)

- `qaScreen -Pqa.screen=ownermore` → **213/213 PASS** (log: `phase14-evidence/_ownermore-14849.log`)
- `qaScreen -Pqa.screen=adminshell` → **38/38 PASS** (log: `phase14-evidence/_adminshell-14849d.log`)
- Inventory: `rows=947 untested=0 orphans=0`
- Debug preview: `:app:assembleDebug` + install `-r` + `open_owner=true`
