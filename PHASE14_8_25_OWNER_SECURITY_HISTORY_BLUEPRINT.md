# Phase 14.8.25 — Owner Security history blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Owner Security history is a **leadership More deep destination** under “Your account and staff access” — security audit list + read detail. Still:

1. **Role-neutral content** — audit list/detail without role section enums or foreign nav.
2. **Role-owned shell** — `OwnerScaffold` / Owner bottom nav; Back detail → list → More menu.
3. **Foundation proof** — Owner chrome ✓, foreign chrome ✗, Back → More, session intact.

**Forbidden:** mounting hybrid audit React workspace; inventing filter/export write tooling beyond read browse in this first slice.

Reuse Owner Employees / Users & roles list+detail nesting. Dedicated `NativeOwnerSecurityHistoryFacade` + SecurityHistory destination.

## Connected (first slice)

- SecurityHistory destination under Owner More.
- List from `securityAuditDao().all()`: auditId, occurredAt, action/result codes, actor label (name · role), details preview.
- Read detail: reasonCode, targetUserId, full details.

## Explicit preview / deferred

- Advanced filters, export, purge
- Manager / Admin Security history shells

## Native data boundary

`NativeOwnerSecurityHistoryFacade`: Owner + `VIEW_SECURITY_AUDIT`; DAO read only; never bridge JSON; no Room/schema changes.

## Accept gate

**AUTO-ACCEPTED.** Connected: Security history list + read detail under Owner More.
