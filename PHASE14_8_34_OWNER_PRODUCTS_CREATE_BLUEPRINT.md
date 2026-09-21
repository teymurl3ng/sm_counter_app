# Phase 14.8.34 — Owner Products create (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Products create is Owner write depth inside the existing Owner Products surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — create form (fields, loading, failure, draft leave-guard) stays presentation-only inside Products content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Products wrappers under `OwnerScaffold(More)`; hardware Back clears create → list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while the draft is dirty.
3. **Foundation proof** — existing Owner Products Foundation (host chrome ✓, foreign chrome ✗, Back stack, session intact) still covers the destination; create is depth on that same entry.

**Forbidden:** mounting another role’s screen, calling JSON bridge from UI, inventing costing/stock editors, or changing `ProductCatalog` domain formulas/validation.

## Connected (this slice)

Replace deferred **Add product** preview with a real create form using typed `ProductCatalogService.create(ProductCatalogInput)` via `NativeOwnerProductsFacade.createProduct`:

| Field | Rules (match `ProductCatalogInput` validation) |
|---|---|
| name | Required, non-empty trim, no newline |
| unit | `Liters` \| `Kg` only |
| defaultSaleRate | Optional; if set must be `> 0` |
| inventoryTracking | Boolean, default `true` |
| notes | Optional |

On success: return to Products list; clear draft; refresh catalog. Edit/archive connected in Phase 14.8.35.

Permission: facade `requireOwnerMasterData` (`MANAGE_MASTER_DATA` + Owner) then `catalog.create`.

## Native data boundary

`NativeOwnerProductsFacade.createProduct(session, input)`:

- `requireOwnerMasterData(session)` then `ProductCatalogService.create(input)`;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, or domain validation.

## Explicit preview / deferred

- Edit product / archive-restore write UI — **connected edit + soft archive in Phase 14.8.35**; unarchive remains deferred
- Costing, stock on hand, COGS editors
- Manager / Admin Products shells

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Products create write depth under CORE METHOD; edit/archive continue in 14.8.35.
