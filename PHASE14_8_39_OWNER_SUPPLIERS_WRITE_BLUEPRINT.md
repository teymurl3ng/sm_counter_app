# Phase 14.8.39 — Owner Suppliers create + rename/profile (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Suppliers create + rename/profile save is Owner write depth inside the existing Owner Suppliers surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — create/edit forms (fields, loading, failure, draft leave-guard) stay presentation-only inside Suppliers content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Suppliers wrappers under `OwnerScaffold(More)`; hardware Back clears create → list → More menu, or edit → detail → list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Owner Suppliers Foundation (host chrome ✓, foreign chrome ✗, Back stack, session intact) still covers the destination; create/edit are depth on that same entry.

**Forbidden:** mounting another role’s screen, calling JSON bridge from UI, inventing purchase/payment formulas, changing `VendorManagement` / `VendorProfile` domain validation, or inventing a separate deactivate/archive command.

## Connected (this slice)

### Create

Replace deferred **Add supplier** preview with a real name-only create form using typed `VendorManagementService.create(name)` via `NativeOwnerSuppliersFacade.createSupplier`:

| Field | Rules (match `VendorManagementService.create` validation) |
|---|---|
| name | Required, non-empty trim, no newline |

On success: return to Suppliers list; clear draft; refresh list.

### Edit (rename + profile)

Replace the detail deferred notice (“Read only in this preview. Editing comes next.”) with real **rename + profile save** using typed domain services via `NativeOwnerSuppliersFacade.updateSupplier`:

From supplier detail, open an edit form prefilled from detail:

| Field | Rules (match domain validation) |
|---|---|
| name | Required; `VendorManagementService.rename` — non-empty trim, no newline |
| phone, location, notes | Optional strings → `VendorProfileInput` (`cleanOptional` in domain) |
| active | Boolean; deactivate = `active=false` on profile save (no separate archive API) |

Save → facade `updateSupplier` (auth once, then `vendors.rename` then `profiles.save` in order) → refresh detail + list; clear dirty draft. Leave-guard while dirty.

Permission: facade `requireOwnerMasterData` (`MANAGE_MASTER_DATA` + Owner) — same as Customers.

## Native data boundary

`NativeOwnerSuppliersFacade.createSupplier(session, name): Long`:

- `requireOwnerMasterData(session)` then `VendorManagementService.create(name)`;

`NativeOwnerSuppliersFacade.updateSupplier(session, accountId, name, VendorProfileInput)`:

- `requireOwnerMasterData(session)` then `VendorManagementService.rename` then `VendorProfileService.save`;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, or domain validation.

## Explicit preview / deferred

- Purchase posting / vendor payment / receive-milk admin UIs
- Manager / Admin Suppliers shells
- Separate “archive supplier” command beyond `active=false`

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Suppliers create + rename/profile write depth under CORE METHOD (mirrors Customers 14.8.36 + 14.8.37 in one blueprint).
