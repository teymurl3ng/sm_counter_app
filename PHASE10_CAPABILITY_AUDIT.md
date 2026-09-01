# Phase 10.1 — Functional Capability-Matrix Closure Audit

Status: **COMPLETE — 2026-09-01**

## 1. Audit question

Does every retained production capability have an honest implementation state before Phase 10 adds any new remote-data behavior?

The accepted states are:

1. genuinely native-backed and wired;
2. explicitly deferred to a named phase;
3. deliberately presentation-only and unable to create false business success;
4. explicitly rejected because it conflicts with the approved product.

## 2. Evidence inspected

- the approved Phase 1 capability inventory and Phase 2 product decisions;
- the Phase 5 DTO catalog;
- canonical bridge contract version 2 and both generated projections;
- the native dispatcher, action groups, handlers, read services, and command services;
- the production React entry point, session/data hooks, navigation, action adapters, and retained screen families;
- Phase 8's 21-milestone screen ledger and Phase 9 runtime evidence;
- the existing native app-preference implementation and backup boundary;
- the focused Phase 7–9 Android instrumentation inventory.

## 3. Contract and runtime closure

The canonical contract remains version 2 with SHA-256:

`AB374869603B9D773AD86B56EF801DA8C4493816C7301B05214AE596E33D0920`

Its 78 unique operations are:

- 20 queries;
- 54 commands;
- 4 system operations.

All 78 operation names have an active React/native-bridge reference and a native runtime action definition. The runtime advertises all 78, the dispatcher routes every action group to a focused handler, and the canonical drift verifier passes. The production `App.tsx` mounts only `HybridAppShell`; the older localStorage prototype is not imported by the packaged application.

## 4. Retained production screen families

| Screen/workflow family | Closure result |
|---|---|
| Owner setup, login, logout, restoration, recovery, own passphrase | Native-backed and wired |
| Products | Native-backed and wired |
| Customer profiles | Native-backed and wired |
| Supplier profiles | Native-backed and wired |
| Employees and attendance | Native-backed and wired |
| Shop settings, rates, expense categories, opening state | Native-backed and wired |
| Daily purchases, sales, production, expenses, payments, salary, spoilage | Native-backed and wired |
| Deliveries and wholesale linkage | Native-backed and wired |
| Customer and supplier ledgers | Native-backed and wired |
| History, event detail, and append-only corrections | Native-backed and wired |
| Physical cash count and close/reopen | Native-backed and wired |
| Dashboard, financial reporting, COGS/profitability coverage | Native-backed and wired |
| Costing policies, opening cost, yield/loss review | Native-backed and wired |
| Budgets and collection targets | Native-backed and wired, with two presentation gaps recorded below |
| Staff app users, private enrollment/reset, roles/status, security audit | Native-backed and wired |
| Encrypted export, inspection, and full-replacement restore | Native-backed and wired |

No retained screen was found to report a successful business mutation without awaiting a native response.

## 5. Confirmed Phase 10.2 local gaps

### 5.1 Derived sales-target presentation

The approved milk-plan service derives a sales target from saved liters and the captured retail rate, and `BudgetMutationResultDto` already returns `salesTargetPaisa`. The active React action adapter refreshes planning data after save but does not show that result, while the persisted `PlanningDto` does not expose the derived target for later viewing.

Close this gap natively: return the authoritative derived sales target with each applicable saved milk plan and display it in Cost & Plans. React must not multiply liters by a rate itself.

### 5.2 Budget-versus-actual presentation

Phase 2 approved native budget actuals, and the native analytics domain already provides the underlying capability. The active hybrid bridge and Cost & Plans screen expose saved plans but not actual-versus-plan results.

Close this gap through a native read model and an additive contract amendment. Preserve native period/category semantics and show unavailable data honestly; React must not calculate actuals or variances independently.

## 6. Explicitly deferred presentation capabilities

The following Phase 2 presentation features are not exposed by the active hybrid product:

- theme selection;
- English/Urdu preference selection, with Urdu translation still separately deferred;
- comfortable/compact display density;
- default reporting period;
- default opening section;
- receipt/image/PDF presentation export where appropriate.

The five preferences already have native `SharedPreferences` persistence and participate in full backup/restore, but their existing native XML behavior does not control the React product. Wiring them before the Phase 12 navigation and visual redesign would bind the hybrid app to a layout that the owner has already identified as unintuitive. They are therefore explicitly deferred to **Phase 12 — UI Fidelity**, where their allowed values and behavior must be reconciled with the final React navigation and theme system.

Receipt/image/PDF output is also deferred to Phase 12 so the final receipt/report layout and appropriate source records are defined before export is added. Android sharing and physical-device acceptance belong to Phase 14. Arbitrary JSON import remains forbidden.

## 7. Explicitly deferred or rejected non-gaps

- periodic encrypted Google Drive/cloud backup: Phase 10.3 design gate;
- owner-phone latest-snapshot viewing: separate Phase 10.3 decision;
- true multi-writer synchronization: not approved;
- concurrent-write, large-history, repeated recovery, and internal query batching: Phase 11;
- lower-of-cost/NRV machinery: Phase 11 materiality review;
- navigation intuition, natural task sequences, contextual next steps, final theme/density behavior, and presentation performance: Phase 12;
- inactive prototype/localStorage screens, unused presentation dependencies, placeholders, and dead code: Phase 13;
- release shrinking/profiles and release-grade benchmarking: Phase 16;
- direct stock setting, arbitrary UI-state import, and casual clear-all-data actions: rejected by the approved product decisions.

## 8. Verification

- bridge contract verifier — **PASS**, version 2, 78 operations, expected SHA-256;
- production React TypeScript check (`tsc --noEmit`) — **PASS**;
- debug Kotlin compilation — **PASS**;
- Android-test Kotlin compilation — **PASS**;
- source cross-check — all 78 contract action names present in the active hybrid/native boundary;
- existing isolated Samsung regression credit — **218/218 PASS**, retained from Phase 9 because this audit changed no application code.

## 9. Checkpoint result

Phase 10.1 is complete. The core functional product is closed and genuine. Phase 10.2 is narrowly scoped to the two native-authoritative planning presentation gaps:

1. persisted derived sales-target visibility;
2. budget-versus-actual visibility.

Cloud backup remains outside 10.2 and cannot begin before its separate design approval gate.

## Plain English

Every important shop action is real and connected to the Android database. The only local functions still worth completing now are showing the sales target created by a milk plan and showing whether saved plans matched actual shop results. Display preferences, Urdu selection, and printable receipts will be handled with the full UI redesign so we do not build them twice.
