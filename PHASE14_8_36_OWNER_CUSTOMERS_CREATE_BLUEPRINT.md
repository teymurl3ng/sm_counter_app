# Phase 14.8.36 — Owner Customers create (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Customers create is Owner write depth inside the existing Owner Customers surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — create form (name field, loading, failure, draft leave-guard) stays presentation-only inside Customers content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Customers wrappers under `OwnerScaffold(More)`; hardware Back clears create → list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while the draft is dirty.
3. **Foundation proof** — existing Owner Customers Foundation (host chrome ✓, foreign chrome ✗, Back stack, session intact) still covers the destination; create is depth on that same entry.

**Forbidden:** mounting another role’s screen, calling JSON bridge from UI, inventing balance/credit engines, or changing `CustomerManagement` domain validation.

## Connected (this slice)

Replace deferred **Add customer** preview with a real create form using typed `CustomerManagementService.create(name)` via `NativeOwnerCustomersFacade.createCustomer`:

| Field | Rules (match `CustomerManagementService.create` validation) |
|---|---|
| name | Required, non-empty trim, no newline |

On success: return to Customers list; clear draft; refresh list. Rename / profile edit / deactivate remain deferred (detail still shows editing-comes-next copy).

Permission: facade `requireOwnerMasterData` (`MANAGE_MASTER_DATA` + Owner) then `customers.create(name)`.

## Native data boundary

`NativeOwnerCustomersFacade.createCustomer(session, name): Long`:

- `requireOwnerMasterData(session)` then `CustomerManagementService.create(name)`;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, or domain validation.

## Explicit preview / deferred

- Rename / profile save / deactivate write UI
- Phone, address, credit limit, daily quota editors
- Balance / payment posting engines
- Manager / Admin Customers shells

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Customers create write depth under CORE METHOD; rename/profile/deactivate remain deferred.
