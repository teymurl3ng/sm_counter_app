# Phase 14.8.24 — Owner Users & roles blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Users & roles is a **leadership More deep destination** under “Your account and staff access” — staff **login** accounts list + read detail. Distinct from Employees salary accounts (14.8.21). Still:

1. **Role-neutral content** — user list/detail, loading, failure, enroll-preview presentation without role section enums or foreign nav.
2. **Role-owned shell** — `OwnerScaffold` / Owner bottom nav; Back detail → list → More menu; Owner session intact.
3. **Foundation proof** — Owner chrome ✓, foreign chrome ✗, Back → More, session intact.

**Forbidden:** mounting hybrid Users admin workspace; inventing enroll/disable/credential write UIs in this first slice; conflating with Employees.

Reuse Owner Employees list+detail nesting. Dedicated `NativeOwnerUsersRolesFacade` + UsersRoles destination.

## Connected (first slice)

- UsersRoles destination under Owner More.
- List from `UserSecurityService.listUsers` (fields aligned with administration DTO mapping): login, display, role label, status, setupRequired.
- Read detail: timestamps + employeeAccountId label when linked.
- Enroll / disable / role-change writes: **connected in Phase 14.8.42**.

## Explicit preview / deferred

- Credential reset write UI (deferred in 14.8.42 with explicit detail notice)
- Manager / Admin Users shells

## Native data boundary

`NativeOwnerUsersRolesFacade`: Owner + `MANAGE_STAFF_USERS`; list/detail from `listUsers`; enroll/enable/role-change via typed `UserSecurityService` (see 14.8.42); never bridge JSON; no Room/schema changes.

## Accept gate

**AUTO-ACCEPTED.** Connected: Users & roles list + read detail under Owner More; write depth landed in 14.8.42.
