# Phase 14.8.37 — Owner Customers rename + profile save (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Customers rename + profile save is Owner write depth inside the existing Owner Customers surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — edit form (fields, loading, failure, draft leave-guard) stays presentation-only inside Customers content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Customers wrappers under `OwnerScaffold(More)`; hardware Back clears edit → detail → list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while the edit draft is dirty.
3. **Foundation proof** — existing Owner Customers Foundation (host chrome ✓, foreign chrome ✗, Back stack, session intact) still covers the destination; edit is depth on that same entry.

**Forbidden:** mounting another role’s screen, calling JSON bridge from UI, inventing balance/credit engines, changing `CustomerManagement` / `Profiles` domain validation, or inventing a separate deactivate/archive command.

## Connected (this slice)

Replace the detail deferred notice (“Read only in this preview. Editing comes next.”) with real **rename + profile save** using typed domain services via `NativeOwnerCustomersFacade`:

From customer detail, open an edit form prefilled from detail:

| Field | Rules (match domain validation) |
|---|---|
| name | Required; `CustomerManagementService.rename` — non-empty trim, no newline |
| phone, areaRoute, address, notes | Optional strings → `CustomerProfileInput` (`cleanOptional` in domain) |
| creditLimit, dailyQuota | Optional Doubles; empty → null; if set must be `>= 0` per `CustomerProfileService.save` |
| active | Boolean; deactivate = `active=false` on profile save (no separate archive API) |

Save → facade `updateCustomer` (auth once, then `customers.rename` then `profiles.save` in order) → refresh detail + list; clear dirty draft. Leave-guard while dirty.

Permission: facade `requireOwnerMasterData` (`MANAGE_MASTER_DATA` + Owner).

Create remains name-only as delivered in Phase 14.8.36.

## Native data boundary

`NativeOwnerCustomersFacade.updateCustomer(session, accountId, name, CustomerProfileInput)`:

- `requireOwnerMasterData(session)` then `CustomerManagementService.rename` then `CustomerProfileService.save`;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, or domain validation.

## Explicit preview / deferred

- Balance / payment posting engines
- Manager / Admin Customers shells
- Separate “archive customer” command beyond `active=false`

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Customers rename + profile save write depth under CORE METHOD; create already in 14.8.36.
