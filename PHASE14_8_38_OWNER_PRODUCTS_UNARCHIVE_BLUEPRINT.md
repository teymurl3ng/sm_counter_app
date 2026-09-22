# Phase 14.8.38 — Owner Products unarchive (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Products unarchive is Owner write depth inside the existing Owner Products surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — Active | Archived list filter, archived detail, and unarchive confirm stay presentation-only inside Products content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Products wrappers under `OwnerScaffold(More)`; hardware Back clears detail → list → More menu; filter stays on the list surface.
3. **Foundation proof** — existing Owner Products Foundation (host chrome ✓, foreign chrome ✗, Back stack, session intact) still covers the destination; filter + unarchive are depth on that same entry.

**Forbidden:** mounting another role’s screen, calling JSON bridge from UI, inventing costing/stock editors, changing `ProductCatalog` domain formulas/validation, or shipping edit-on-archived in this slice.

## Connected (this slice)

Complete Products write depth: restore soft-archived products.

### List filter

Active (default) | Archived toggle/chips on the Products list:

- Active → `facade.loadProducts(session, includeArchived = false)`
- Archived → `facade.loadProducts(session, includeArchived = true)` filtered to `archived == true` only (facade responsibility)

Add product remains on Active only.

### Detail for archived

`loadDetail` must resolve archived products (list with `includeArchived = true`). Active detail keeps Edit + Archive. Archived detail: Unarchive only (no edit required this slice).

### Unarchive

From archived detail: confirm → `facade.unarchiveProduct(session, productId)` → `catalog.setArchived(productId, false)`. On success: leave detail and refresh the archived list (item gone) — or switch to Active showing it again.

Permission: existing `requireOwnerMasterData` (`MANAGE_MASTER_DATA` + Owner).

Create (14.8.34) and edit + soft archive (14.8.35) remain as delivered.

## Native data boundary

`NativeOwnerProductsFacade.loadProducts(session, includeArchived = false)` / `loadDetail` / `unarchiveProduct`:

- `requireOwnerMasterData(session)` then `ProductCatalogService.list` / `setArchived(productId, false)`;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, or domain validation.

## Explicit preview / deferred

- Edit while archived
- Costing, stock on hand, COGS editors
- Manager / Admin Products shells

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Products Active|Archived filter + unarchive write depth under CORE METHOD; create 14.8.34; edit+archive 14.8.35.
