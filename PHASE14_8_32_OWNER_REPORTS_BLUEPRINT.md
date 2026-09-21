# Phase 14.8.32 — Owner Month reports blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## Where this checkpoint starts

Owner Overview / Work / Records / More (read deep dirs + Work money posts) and Cashier Expense host are connected. Owner still lacks a dedicated **Month reports** surface. Authority: `PHASE14_8_2_JOURNEY_DIRECTIONS.md` (Reports = fast presets; custom behind choice), `PHASE14_8_14_OWNER_OVERVIEW_BLUEPRINT.md`, `HybridReadModelService.profitabilityReport` / `ProfitabilityReportDto`.

## Connected (this slice)

- Owner-only read report under **Overview** (not a 5th bottom-nav tab).
- Entry: clear **Month reports** control on Overview home; Overview stays selected in OwnerScaffold.
- Fast presets: **This month** (default), **Last month**. Custom range deferred (preview notice only if shown).
- Honest coverage: complete / incomplete / empty from `coverageCode` + empty products.
- Summary: delivery surcharge, operating expenses, salary payments, estimated result when present.
- Product rows: name, sales revenue, cost/profit when present, coverage label.
- Back / hardware Back → Overview home. No foreign chrome. No Cashier host.

## Native data boundary

`NativeOwnerReportsFacade.load(session, startInclusive, endExclusive)`:

- require Owner + `VIEW_FULL_FINANCIAL`;
- call typed `HybridReadModelService.profitabilityReport` (no bridge JSON);
- map DTO → presentation rows/labels only — **do not invent profitability formulas**.

## Out of scope

- Manager / Admin Reports shells
- Custom date range UI (beyond deferred notice)
- Reopen day; More write depth
- Changing costing / Room / RolePermissions

## Verification

- Facade + Compose contract + Foundation (opens under Owner chrome, Back → Overview, no Cashier chrome)
- Inventory COVERED → those tests
- `qaScreen -Pqa.screen=owner` PASS
- `assembleDebug` → `adb install -r` → `open_owner=true`

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Overview nested Month reports read panel from existing profitability report.
