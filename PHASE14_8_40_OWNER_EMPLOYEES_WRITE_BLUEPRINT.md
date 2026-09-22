# Phase 14.8.40 — Owner Employees create + rename/profile (write depth)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Employees create + rename/profile save is Owner write depth inside the existing Owner Employees surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — create/edit forms (fields, loading, failure, draft leave-guard) stay presentation-only inside Employees content. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Employees wrappers under `OwnerScaffold(More)`; hardware Back clears create → list → More menu, or edit → detail → list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Owner Employees Foundation (host chrome ✓, foreign chrome ✗, Back stack, session intact) still covers the destination; create/edit are depth on that same entry.

**Forbidden:** mounting another role’s screen, calling JSON bridge from UI, inventing salary-payment or attendance write UIs, conflating Employees (Salary accounts) with Users & roles (login accounts), changing `EmployeeManagement` / `EmployeeProfile` domain validation, or inventing a separate deactivate/archive command.

## Connected (this slice)

### Create

Replace deferred **Add employee** preview with a real name-only create form using typed `EmployeeManagementService.create(name)` via `NativeOwnerEmployeesFacade.createEmployee`:

| Field | Rules (match `EmployeeManagementService.create` validation) |
|---|---|
| name | Required, non-empty trim, no newline |

On success: return to Employees list; clear draft; refresh list.

### Edit (rename + profile)

Replace the detail deferred notice (“Read only in this preview. Editing comes next.”) with real **rename + profile save** using typed domain services via `NativeOwnerEmployeesFacade.updateEmployee`:

From employee detail, open an edit form prefilled from detail:

| Field | Rules (match domain validation) |
|---|---|
| name | Required; `EmployeeManagementService.rename` — non-empty trim, no newline |
| role, phone | Optional strings → `EmployeeProfileInput` (`cleanOptional` in domain) |
| referenceSalary | Optional; null or ≥ 0 (`EmployeeProfileService.save`) |
| salaryPeriod | Optional string (no enum restriction in domain; `cleanOptional`) |
| joinDate | Optional; empty or ISO-8601 `LocalDate` (`yyyy-MM-dd`) — domain parses via `LocalDate::parse` |
| active | Boolean; deactivate = `active=false` on profile save (no separate archive API) |

Save → facade `updateEmployee` (auth once, then `employees.rename` then `profiles.save` in order) → refresh detail + list; clear dirty draft. Leave-guard while dirty.

Permission: facade `requireOwnerMasterData` (`MANAGE_MASTER_DATA` + Owner) — same as Suppliers / Customers.

## Native data boundary

`NativeOwnerEmployeesFacade.createEmployee(session, name): Long`:

- `requireOwnerMasterData(session)` then `EmployeeManagementService.create(name)`;

`NativeOwnerEmployeesFacade.updateEmployee(session, accountId, name, EmployeeProfileInput)`:

- `requireOwnerMasterData(session)` then `EmployeeManagementService.rename` then `EmployeeProfileService.save`;
- never call bridge JSON from Compose;
- do not change Room schema, RolePermissions, or domain validation.

## Explicit preview / deferred

- Salary payment posting as admin UI
- Attendance marking / history as Employees admin UI (Ops Attendance remains separate)
- Users & roles / staff login enrollment
- Manager / Admin Employees shells
- Separate “archive employee” command beyond `active=false`

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Employees create + rename/profile write depth under CORE METHOD (mirrors Suppliers 14.8.39 / Customers 14.8.36 + 14.8.37 in one blueprint).
