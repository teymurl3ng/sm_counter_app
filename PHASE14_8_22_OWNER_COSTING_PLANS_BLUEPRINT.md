# Phase 14.8.22 — Owner Costing & plans blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Costing & plans is a **leadership More deep destination** under “Planning and setup” — versioned costing-policy read, not Shop settings, not budget/yield write UIs, and not a foreign hybrid Costing workspace mount. Still:

1. **Role-neutral content** — policy list, detail, loading, failure, and approve/edit preview presentation must not import `OwnerSection` / `CashierSection` / `OperationsSection`, and must not render any role’s bottom nav when extracted for reuse.
2. **Role-owned shell** — Costing & plans wraps content in `OwnerScaffold` / Owner bottom nav; Back returns to Owner More menu (or clears detail first); session stays Owner-authenticated.
3. **Foundation proof** — Owner chrome present, Cashier/Ops chrome absent, Back → Owner More menu (detail → list → menu), session remains authenticated.

**Forbidden in this checkpoint (and forever):**

- Mounting the hybrid Costing/Planning React workspace, Cashier Collect, Ops screens, or any foreign shell as Owner Costing & plans.
- Translating `CashierSection` / `OperationsSection` events inside Owner.
- Treating callback-only “open Costing” intents as hosted-entry coverage.
- Inventing new costing formulas, conversion-rate engines, or approve/edit write flows in Compose unless already trivial and device-proven elsewhere.
- Shipping Shop settings as part of this destination (Shop settings remains deferred).

Reuse sibling **patterns** from Owner Employees (14.8.21) / Owner Suppliers (14.8.20) nesting under Owner More. Prefer a dedicated `NativeOwnerCostingPlansFacade` + `CostingPlans` destination under Owner More (same nesting model as Employees / Suppliers / Customers / Products / Personal security).

## Where this checkpoint starts

Owner **Overview**, **Work**, **Records**, **More**, More **Products**, **Customers**, **Suppliers**, and **Employees** are connected in the debug `NativePreviewActivity` harness (`open_owner=true`). Owner More **Costing & plans** still surfaces an explicit deferred preview notice (`PHASE14_8_17`).

Authority for product shape:

- `PHASE14_8_17_OWNER_MORE_BLUEPRINT.md` — deferred Costing & plans row under “Planning and setup”
- `PHASE14_8_21_OWNER_EMPLOYEES_BLUEPRINT.md` — primary structural sibling (list + read detail under More)
- `PHASE14_8_2_JOURNEY_DIRECTIONS.md` — “Planning and setup: Costing/plans”
- `PHASE14_8_2_ROLE_CALIBRATION.md` / Counter Ledger leadership — Owner planning ownership
- Shell law: `PHASE14_8_15A_SHELL_VS_CONTENT.md`
- Existing typed domain: `CostingPolicyService` / `CostingPolicySnapshot`; `CostingPolicyDao` versions + components — **read** approved/current policy list or snapshot only
- Sibling UI: Owner Employees / Suppliers list/detail under Owner More

This checkpoint owns **Owner Costing & plans** conservatively: costing policy version list (or current snapshot summary) + read detail of components already on the snapshot under Owner More. It does **not** implement approve/edit policy write UI, budgets, collections targets, yield policy, opening cost basis, or Shop settings.

## What the existing product already guarantees

- Owner has `MANAGE_BUSINESS_SETTINGS` and can already approve costing policies through typed `CostingPolicyService.approve` (recent authentication required for writes).
- `CostingPolicyService.active(calendarMonth)` and `CostingPolicyDao.allVersions()` / `components(policyId)` expose approved versions and component rows with `CostingPolicyService.ratePerKg` — no new formulas.
- `HybridCostingPlanningReadService.costingManagement` already maps the same DAO/service boundary for bridge consumers; Compose must not call bridge JSON.
- Owner More already provides shell, business-date context, and nested destination Back (Personal security, Products, Customers, Suppliers, Employees).

## Reuse / do-not-reuse

| Piece | Reuse? |
|---|---|
| Owner bottom nav + green header / `OwnerScaffold` | Yes |
| Owner Employees nested destination + Back stack (detail → list → More) | Yes — primary structural sibling |
| Hybrid CostingPlanning React workspace / bridge JSON | **No** |
| `CostingPolicyDao.allVersions` / `components` / `CostingPolicyService.active` / `ratePerKg` | Yes — read-only presentation |
| `CostingPolicyService.approve` write UI | **No** in first slice — explicit preview notice only |
| Budgets / collections targets / yield / opening cost basis editors | **No** — deferred |
| Shop settings | **No** — separate later phase |
| Bridge JSON / `query.costingManagement` from Compose | **No** |

## Normal path

1. An Owner signs in (`open_owner=true`) and opens **More**.
2. Tap **Costing & plans** → Owner Costing & plans list (not a deferred snackbar).
3. Header stays Owner More chrome (title **Costing & plans** under Owner header tokens); business date / Open-Closed from More context.
4. List shows costing policy versions: calendar month primary; Active/Superseded + conversion rate + review reason secondary from typed fields. If no versions exist, show an honest empty / incomplete Ready state (still a connected screen, not a snackbar-only preview).
5. Tap a row → read-only detail: month, capacity, review reason, notes, approved-at, conversion rate, and each component already on the snapshot (code, source amount, basis kg).
6. Back from detail → Costing list. Back from list → Owner More menu. Hardware Back matches that stack.
7. Optional **Approve / edit policy** affordance: explicit preview notice only (“comes next”); do not invent an approve form in Compose.
8. Load failure shows inline error + Retry.
9. Owner nav remains; no Cashier/Ops bottom nav.

## Connected vs preview / deferred in this checkpoint

### Connected (first slice)

- CostingPlans destination under Owner More (replace Costing & plans deferred notice).
- Policy version list from DAO / existing rate helper (or honest empty + current-month snapshot summary).
- Read detail with components already on the snapshot.
- Owner chrome + Back → More menu; inventory + Foundation + `qaScreen` (extend `ownermore`).

### Explicit preview / deferred

- Approve / supersede costing policy write UI — **connected in Phase 14.8.43**
- Budgets and collections targets planning UIs — **connected in Phase 14.8.49** (`ownermore` **213/213 PASS**; `adminshell` **38/38 PASS**)
- Yield policy approve — **connected in Phase 14.8.49**
- Opening cost basis, production-loss assessment UIs
- Free-form in-place edit of an already-approved version (domain is versioned approve — deferred)
- Shop settings / opening state / expense categories
- Manager / Admin Costing shells — Admin hosts Costing under Admin More (14.8.46+); Manager stays deferred

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Costing & plans row | Open Costing list | Policy versions or honest empty; Owner nav | Destination + tags |
| Policy row | Open detail | Read-only policy + components | Detail tags |
| Back (detail) | Return to list | List restored | Back contract |
| Back (list) | Return to More menu | Menu restored | Back contract |
| Retry | Reload list | Ready list | Failure recovery |
| Approve/edit (preview) | Explicit notice | Stay on Costing | **Superseded by 14.8.43 approve form** |
| Foreign chrome | Must not appear | No Cashier/Ops nav | Foundation |

## Material states

| State | Behavior |
|---|---|
| Loading | Owner chrome + progress |
| Ready list | Policies; empty / incomplete copy when none |
| Detail | Read-only facts + components |
| Load failure | Inline error + Retry |
| Approve deferred | Explicit preview notice — **replaced by approve form in 14.8.43** |
| Session expired | Leave via existing auth path |

## Native data boundary

`NativeOwnerCostingPlansFacade` (presentation-neutral) + Owner More ViewModel CostingPlans destination:

- require authenticated Owner session;
- require `MANAGE_BUSINESS_SETTINGS` for costing policy read;
- list via `costingPolicyDao().allVersions()` (+ components / `CostingPolicyService.ratePerKg` for labels);
- optional current-month summary via `CostingPolicyService.active(businessMonth)` when business date is available — display only;
- detail via version + `components(policyId)` — never invent missing component rows;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, Analytics, or costing engines.

## Visual / consistency notes

- Nested under Owner More like Employees (Back to More; detail → list).
- Dense list rows: month primary; status + conversion rate (+ review reason) secondary.
- Detail uses labeled facts and a simple component list — not a planning workspace dock.
- Deferred approve/edit is a real control with explicit notice — not a silent no-op.
- Copy must say **Costing & plans** (policy versions), never “Shop settings”.

## Out of scope

- Full costing/planning write suite
- Budget / yield / opening-basis workflow UIs
- Shop settings
- Foreign role Costing screens
- Launcher migration away from `HybridActivity`

## Verification

- Facade tests: Owner gate, list policies with status/rate, detail components, reject non-Owner.
- Compose contract: Costing list + detail tags; Costing row opens destination (not deferred notice).
- Inventory rows with real workflow test names (no `workflow_test=-`).
- `qaScreen -Pqa.screen=ownermore` extended with Costing Foundation/contract coverage.
- Foundation: Owner chrome ✓, foreign nav ✗, Back → More menu, session intact.
- Prove on device: `:app:assembleDebug` → `adb install -r` → launch `NativePreviewActivity` with `open_owner=true`.

## Accept gate

**AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION.** Connected scope: costing policy list + read detail under Owner More shell; approve/edit deferred with explicit preview; Shop settings remains deferred.
