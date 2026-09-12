# Phase 14.8.2 — Role-calibrated native experience proposal

Status: **SUPERSEDED FOR REVIEW BY THE ROLE-ARCHITECTURE RESEARCH — shell design will be revised after role policy is accepted**

Date: 2026-09-12

The visual shell proposal in this file was useful for proving that roles need different experiences, but it assumed the existing five permission bundles were already the correct product roles. `PHASE14_8_2_ROLE_ARCHITECTURE_RESEARCH.md` now reopens that assumption, defines the jobs, identifies over-broad permissions, and controls the next review checkpoint.

## Governing principle

Saaf Milk will not use one universal layout with controls merely hidden by permission. Every screen, menu, default destination, shortcut order, summary, list, form, and completion state will be calibrated to the signed-in role's real job.

- **Owner:** decision-first and data-driven, with complete operational capability still reachable.
- **Admin:** work-first and high-throughput, with staff, master-data, security-audit, and backup administration available without dominating routine entry.
- **Manager:** work-first and supervisory, with fast entry, cash close, corrections, attendance, balances, and operational records prioritized.
- **Cashier:** transaction-first, optimized for sale, collection, expense, delivery, cash count, and recent activity with minimal navigation.
- **Operator:** operations-first, optimized for purchase/receiving, production, spoilage, delivery, attendance, and operational status.

Role calibration is driven by the native `CurrentSessionDto.roleCode` and `effectivePermissionCodes`. It never replaces native authorization.

## Existing permission truth

| Role | Existing native permissions | Product emphasis |
|---|---|---|
| Owner | all permissions | overview, exceptions, balances, trends, recent activity, then operations and administration |
| Admin | all except restore and privileged-user management | fast operations plus ordinary staff/master-data/security/backup administration |
| Manager | operational and financial views; all daily operations; master data; attendance; corrections; cash count/close | daily work, staff/records, close, exceptions; no business-settings/security/backup emphasis |
| Cashier | operational view; sale/collection; expense; delivery; cash count/close | sale first, then collection, expense/delivery, shift/day close, recent records |
| Operator | operational view; purchase; production/spoilage; delivery; attendance | receiving and stock work, delivery, attendance, recent operational records |

## Shared rules

All roles retain:

- the same Saaf visual language, component behavior, vocabulary, record identity, and Back model;
- a compact account entry point for personal passphrase, Quick unlock, account switching, and sign out;
- direct confirmation that a mutation succeeded and what record/result it created;
- native permission checks even if a control is absent;
- predictable access to recent records created by that role when permitted;
- accessible text, touch targets, large-text reflow, keyboard behavior, and reduced motion.

The layouts may differ substantially. Consistency means the same concept behaves the same when it appears; it does not mean every role sees the same home screen.

## Owner experience

### Default destination: Overview

The owner opens to:

- business date and close state;
- a small number of decision-critical facts;
- cash and stock exceptions;
- customer and supplier exposure;
- current-period result with honest coverage disclosure;
- latest activity and actor;
- backup health when attention is required;
- quick operational actions in a secondary position.

### Primary navigation

`Overview / Work / Records / More`

`More` contains products, people, planning, shop settings, access/users, security history, and backup/recovery. Owner pages may show richer comparisons and drill-downs, but report configuration must not dominate the first viewport.

## Admin experience

### Default destination: Work desk

The admin opens to:

- a compact launcher for the most frequent permitted entries;
- unfinished or time-sensitive work;
- recent records and correction shortcuts;
- staff/master-data administration after daily tasks;
- concise operational context rather than an owner-style financial dashboard.

### Primary navigation

`Work / People / Records / More`

`People` combines staff access, employee profiles/attendance, customers, and suppliers with the correct permission and security separation. Backup export/cloud controls and security history live in `More`; restore remains unavailable by native permission.

## Manager experience

### Default destination: Today's work

The manager opens to:

- frequent operation shortcuts;
- cash-count/close state;
- deliveries and attendance needing work;
- balances and recent operational records;
- correction entry from record detail.

### Primary navigation

`Work / Close / Records / More`

The manager does not receive owner planning/settings/security/backup surfaces because the native role does not grant those permissions.

## Cashier experience

### Default destination: New sale

The cashier opens directly into a compact sale-entry workspace. Product, quantity/value, payment mode, payment account, optional delivery charge, and save are sequenced for thumb reach and minimal scrolling. Stock and allowed cash context appear only where they help the decision.

### Primary navigation

`Sell / Collect / Activity / More`

`More` contains Expense, Deliveries, Cash count/close, and personal access. It does not show products, suppliers, employees, financial reports, settings, staff administration, security audit, or backup.

## Operator experience

### Default destination: Operations

The operator opens to:

- Receive milk;
- Make yogurt;
- Record spoilage;
- Deliveries;
- Attendance;
- only the stock/status context needed for those tasks.

### Primary navigation

`Work / Deliveries / Attendance / More`

`More` contains recent operational records and personal access. Sales, payments, expenses, cash close, financial accounts, corrections, settings, user administration, and backup remain absent.

## Screen-level calibration rules

### Headers

- Owner headers identify the period/status being summarized.
- Work-role headers identify the active task and keep task switching close.
- Sign out is inside the account menu rather than a large repeated header button.

### Forms

- Field order follows the role's physical workflow.
- Frequent choices use direct controls; infrequent optional fields remain available through a clear secondary section.
- Forms do not repeat a dashboard above the first field.
- Safe defaults may depend on the active role and last non-secret local choice, never on invented business truth.
- Completion returns focus to the role's likely next action.

### Lists and records

- Owner lists emphasize amount, exception, trend, and actor.
- Admin/manager lists emphasize status, next action, age, and correction/close work.
- Cashier/operator lists emphasize records they need to resume or verify, with unnecessary financial columns removed.
- Detail screens preserve the same authoritative record and audit data; they reorder emphasis without changing facts.

### Menus

- Navigation is role-specific and stable within the signed-in session.
- Inaccessible destinations do not leave empty placeholders.
- Rare recovery and configuration actions never compete visually with daily entry.
- Account switching causes an explicit shell transition so the changed role/layout is understandable.

### Feedback

- A cashier sees sale amount/payment/result and `Record another` first.
- An operator sees stock/output impact and next operational task first.
- A manager/admin sees the saved record plus any remaining close/queue item.
- An owner sees the saved record plus refreshed overview consequences when relevant.

## Implementation boundary

The first native shell will model role experience as data derived from the authenticated native session. Composables receive a role-specific shell specification and permission-filtered actions. They do not contain authorization rules or call bridge JSON.

The architecture must allow a shared component such as Sale or Record detail to present role-calibrated density, defaults, shortcuts, and completion actions without forking the underlying business command or record model.

## Review checkpoint

The accompanying role switcher shows the five proposed starting experiences. Acceptance of this principle authorizes detailed owner and cashier journeys first, because they represent the two extremes: decision-first oversight and high-speed entry. Admin, manager, and operator journeys will then be checked against those shared components before Compose implementation begins.
