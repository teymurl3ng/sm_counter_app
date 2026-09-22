# Phase 14.8.35 — Owner Products edit + archive (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Products edit + archive is Owner write depth inside the existing Owner Products surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — edit form and archive confirm (fields, loading, failure, draft leave-guard) stay presentation-only inside Products content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Products wrappers under `OwnerScaffold(More)`; hardware Back clears edit → detail → list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while the edit draft is dirty.
3. **Foundation proof** — existing Owner Products Foundation (host chrome ✓, foreign chrome ✗, Back stack, session intact) still covers the destination; edit/archive are depth on that same entry.

**Forbidden:** mounting another role’s screen, calling JSON bridge from UI, inventing costing/stock editors, changing `ProductCatalog` domain formulas/validation. Unarchive is connected in Phase 14.8.38.

## Connected (this slice)

Replace the detail deferred notice (“Read only in this preview. Editing comes next.”) with real **edit** + soft **archive** using typed `ProductCatalogService` via `NativeOwnerProductsFacade`:

### Edit

From product detail, open an edit form prefilled from detail (same fields as create in 14.8.34):

| Field | Rules (match `ProductCatalogInput` validation) |
|---|---|
| name | Required, non-empty trim, no newline |
| unit | `Liters` \| `Kg` only |
| defaultSaleRate | Optional; if set must be `> 0` |
| inventoryTracking | Boolean |
| notes | Optional |

Save → `facade.updateProduct(session, productId, ProductCatalogInput)` → `catalog.update`. On success: return to refreshed detail (or list); clear dirty draft. Leave-guard while dirty.

### Archive

From detail, confirm then `facade.archiveProduct(session, productId)` → `catalog.setArchived(productId, true)`. On success: leave detail and refresh active list (archived products drop out).

Permission: facade `requireOwnerMasterData` (`MANAGE_MASTER_DATA` + Owner).

Create remains as delivered in Phase 14.8.34.

## Native data boundary

`NativeOwnerProductsFacade.updateProduct` / `archiveProduct`:

- `requireOwnerMasterData(session)` then `ProductCatalogService.update` / `setArchived`;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, or domain validation.

## Explicit preview / deferred

- Unarchive / restore archived products UI — **connected in Phase 14.8.38**
- Costing, stock on hand, COGS editors
- Manager / Admin Products shells

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Products edit + archive write depth under CORE METHOD; create already in 14.8.34; unarchive follows in 14.8.38.
