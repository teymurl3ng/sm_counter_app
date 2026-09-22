# Phase 14.8.42 — Owner Users & roles enroll + disable (+ role change)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

**Users & roles enroll / disable-enable / role change is Owner write depth inside the existing Owner Users & roles surface** — not a new More tile and not a foreign-role host:

1. **Role-neutral content** — enroll form, passphrase step, detail actions, confirm/reauth presentation, loading, failure, draft leave-guard. No `OwnerSection` / `CashierSection` / `OperationsSection` / foreign bottom nav in reusable content.
2. **Role-owned shell** — continue using existing Owner Users & roles wrappers under `OwnerScaffold(More)`; hardware Back clears enroll/role-change/reauth → detail or list → More menu; `EntryLeaveGuard` on Back **and** bottom-nav exits while a draft is dirty.
3. **Foundation proof** — existing Owner Users & roles Foundation (host chrome ✓, foreign chrome ✗, Back → More menu, session intact) still covers the destination; write steps are depth on that same entry.

**Forbidden:** mounting hybrid Users admin / foreign shells; calling JSON bridge from Compose; conflating with Employees salary accounts; changing `Security.kt` domain validation; skipping passphrase `CharArray` wipe; inventing credential-reset UI in this slice unless trivial reuse (prefer defer).

## Connected (this slice)

Mirror legacy UX intent from `presentation/security/SecurityUi.kt` (`showCreateUser` / `showEnrollmentPassphrase` / `setUserEnabled` / role change) via typed `UserSecurityService` only.

### A. Enroll

1. Form: login name, display name, role picker (`AppRole` wire values the domain allows — Owner may enroll any role including Owner/Admin).
2. Save → `beginUserEnrollment(session, CreateUserInput(...))`.
3. If `SecurityFailure.RecentAuthenticationRequired` → Owner full-passphrase reauth (`reauthenticate`) then retry begin.
4. On pending success → new-user passphrase step (passphrase + confirm) → `completeUserEnrollment(token, passphrase)`.
5. Success → refresh list; wipe all passphrase `CharArray`s after use.
6. Leave-guard while identity or passphrase draft is dirty. Optional `employeeAccountId` omitted from first enroll form.

### B. Disable / Enable

From user detail: confirm → `setUserEnabled(session, userId, enabled)` (always requires recent auth). Reauth then retry when needed. Domain errors (cannot disable self / last owner) surface as messages.

### C. Change role

From user detail: role picker + save → `changeRole(session, userId, newRoleValue)` (always requires recent auth). Reauth then retry when needed. Leave-guard while role draft dirty.

## Native data boundary

`NativeOwnerUsersRolesFacade`:

- `beginEnroll(session, CreateUserInput)` → `PendingUserEnrollment`
- `completeEnroll(enrollmentToken, passphrase: CharArray)` — wipe passphrase in `finally`
- `setEnabled(session, userId, enabled)`
- `changeRole(session, userId, newRoleValue)`
- `reauthenticate(session, passphrase: CharArray)` — wipe passphrase in `finally`
- Owner + `MANAGE_STAFF_USERS` on management paths; never bridge JSON; do not change Room schema, RolePermissions, or domain validation

## Explicit preview / deferred

- Credential reset (`beginCredentialReset` + complete passphrase) — **deferred** with an explicit detail notice
- Resume incomplete enrollment as a dedicated action (pending users with setup required remain visible; full resume UI deferred)
- Optional employeeAccountId link on enroll
- Manager / Admin Users shells

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner Users & roles enroll + disable/enable + role change write depth under CORE METHOD; credential reset remains deferred with an explicit notice.
