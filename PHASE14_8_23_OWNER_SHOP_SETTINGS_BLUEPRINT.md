# Phase 14.8.23 — Owner Shop settings blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Shop settings is a **leadership More deep destination** under “Planning and setup” — store identity, rates, opening state, and expense categories read panel. Still:

1. **Role-neutral content** — settings panel, loading, failure, edit-preview presentation must not import role section enums or foreign bottom nav.
2. **Role-owned shell** — wraps content in `OwnerScaffold` / Owner bottom nav; Back returns to Owner More menu; session stays Owner-authenticated.
3. **Foundation proof** — Owner chrome present, Cashier/Ops chrome absent, Back → Owner More menu, session intact.

**Forbidden:** mounting hybrid Settings React workspace / foreign shells; inventing edit/write settings UI in Compose for this first slice.

Reuse sibling patterns from Owner Costing & plans (14.8.22) / Employees (14.8.21) nesting under Owner More. Dedicated `NativeOwnerShopSettingsFacade` + ShopSettings destination.

## Connected (first slice)

- ShopSettings destination under Owner More (replace Shop settings deferred notice).
- Read panel from `HybridReadModelService.businessSettings`: shop identity, rate labels (PKR), opening date/notes, expense category rows.
- Edit settings: explicit preview notice only.
- Owner chrome + Back → More menu.

## Explicit preview / deferred

- Edit shop settings / rates / opening state / expense category write UIs
- Manager / Admin Shop settings shells

## Native data boundary

`NativeOwnerShopSettingsFacade`: Owner + `MANAGE_BUSINESS_SETTINGS`; `businessSettings(session)`; optional `loadCategoryDetail`; never bridge JSON; no Room/schema changes.

## Accept gate

**AUTO-ACCEPTED.** Connected: Shop settings read panel under Owner More; writes deferred with explicit preview.
