# Phase 14.8.43 — Owner Costing & plans policy approve

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Costing policy approve is Owner write depth inside the existing Owner Costing & plans surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — approve form, reauth presentation, loading, failure, draft leave-guard. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Costing & plans wrappers under `OwnerScaffold(More)`; hardware Back clears reauth → approve form → detail or list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Owner Costing & plans Foundation (host chrome ✓, foreign chrome ✗, Back → More menu, session intact) still covers the destination; approve is depth on that same entry.

**Forbidden:** mounting hybrid Costing/Planning React workspace; calling JSON bridge from Compose; changing `Costing.kt` domain formulas or validation; inventing mutate-in-place edit of an already-approved version; inventing budget/yield/opening-basis UIs in this slice.

## Connected (this slice)

Replace Costing “Approving or editing a policy comes next” / Approve-policy preview notice with real **approve** via typed `CostingPolicyService.approve(session, CostingPolicyInput)`.

### Approve new / superseding policy

1. Form fields required by `CostingPolicyInput`:
   - `calendarMonth` (`YearMonth`, e.g. `2026-09`)
   - `normalCapacityKg` (positive)
   - `reviewReason` (one of `CostingPolicyService.REVIEW_REASONS`)
   - `components` — exactly the five `COMPONENT_CODES` once each: culture, packaging, variable_energy, direct_labor, fixed_production_overhead; each with non-negative `sourceAmountPaisa` and positive `basisQuantityKg`; fixed overhead basis must equal normal capacity
   - `notes` (optional)
   - `supersedesPolicyId` — must equal the active policy for that month when one exists; must be `null` when the month has no active policy (domain supersede rule)
2. UI may collect component amounts as PKR and convert to paisa before building typed input — no bridge JSON, no invented rates.
3. Save → `NativeOwnerCostingPlansFacade.approvePolicy(session, input)` → on success refresh list (and clear detail / close form).
4. From list: **Approve policy** opens a blank/new draft (default month = business month; auto-wire `supersedesPolicyId` when that month already has an Active row).
5. From Active detail: **Approve corrected policy** opens a prefilled superseding draft (`supersedesPolicyId` = that policy). Superseded detail keeps an explicit notice that in-place edit is not available — domain is versioned approve/supersede only.

### Recent authentication

`CostingPolicyService.approve` always calls `requireRecentAuthentication`. On `SecurityFailure.RecentAuthenticationRequired`: prompt Owner full-passphrase reauth via facade `reauthenticate` (wipe `CharArray` in `finally`), then retry the pending approve. Mirror Users 14.8.42 / Cash close reopen.

### Leave-guard

While the approve draft is dirty, `EntryLeaveGuard` on hardware Back and bottom-nav exits.

## Native data boundary

`NativeOwnerCostingPlansFacade`:

- `approvePolicy(session, CostingPolicyInput)` → approved snapshot / detail presentation fields; Owner + `MANAGE_BUSINESS_SETTINGS`; delegates only to `CostingPolicyService.approve`
- `reauthenticate(session, passphrase: CharArray)` — wipe passphrase in `finally`
- expose read helpers for review-reason / component code lists from `CostingPolicyService` companions (no new formulas)
- never call bridge JSON; do not change Room schema, RolePermissions, or `Costing.kt` validation

## Explicit preview / deferred

- Budget upsert / collections targets — **connected in Phase 14.8.49** (ownermore **213/213**; adminshell **38/38**)
- Yield policy approve — **connected in Phase 14.8.49** (same Costing destination + recent-auth mirror)
- Opening cost basis editors
- Free-form “edit” of an already-approved version without approve/supersede (domain is versioned approve — do not invent mutate-in-place)
- Manager Costing shells

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Costing & plans approve new/superseding policy write depth under CORE METHOD; budgets / yield / opening basis connected or deferred per later slices.
