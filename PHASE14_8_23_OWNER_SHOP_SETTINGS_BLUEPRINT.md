# Phase 14.8.23 — Owner Shop settings blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21 (updated 2026-09-22 for 14.8.41 write depth)

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Shop settings is a **leadership More deep destination** under “Planning and setup” — store identity, rates, opening state, and expense categories. Still:

1. **Role-neutral content** — settings panel, loading, failure, edit forms must not import role section enums or foreign bottom nav.
2. **Role-owned shell** — wraps content in `OwnerScaffold` / Owner bottom nav; Back returns to Owner More menu (or clears edit depth first); session stays Owner-authenticated.
3. **Foundation proof** — Owner chrome present, Cashier/Ops chrome absent, Back → Owner More menu, session intact.

**Forbidden:** mounting hybrid Settings React workspace / foreign shells; calling bridge JSON from Compose; inventing opening record/correct UI outside the dedicated opening slice.

Reuse sibling patterns from Owner Costing & plans (14.8.22) / Employees (14.8.21) nesting under Owner More. Dedicated `NativeOwnerShopSettingsFacade` + ShopSettings destination.

## Connected

- ShopSettings destination under Owner More.
- Read panel from `HybridReadModelService.businessSettings`: shop identity, rate labels (PKR) + raw Doubles for prefills, opening date/notes, expense category rows.
- **Write depth (Phase 14.8.41):** Edit identity (`StoreIdentityService.save`), Edit rates (`BusinessSettingsService.update`), Add category + archive/unarchive (`ExpenseCategoryManagementService`) via facade typed methods. Leave-guards on dirty drafts.
- Owner chrome + Back → More menu.

## Explicit preview / deferred

- Opening state record / correct (recent auth + conflict guards) — explicit notice on opening section only (14.8.41)
- Manager / Admin Shop settings shells

## Native data boundary

`NativeOwnerShopSettingsFacade`: Owner + `MANAGE_BUSINESS_SETTINGS` for identity/rates reads/writes; Owner + `MANAGE_MASTER_DATA` for expense category create/archive; `businessSettings(session)`; optional `loadCategoryDetail`; never bridge JSON; no Room/schema changes.

## Accept gate

**AUTO-ACCEPTED.** Connected: Shop settings read panel + identity/rates/category write depth under Owner More; opening writes deferred with explicit section notice.
