# Phase 8 — Screen-by-Screen Native UI Wiring

**Status: IN PROGRESS — product-management milestone complete 2026-08-28**

## 1. Purpose and working rule

Phase 8 replaces the approved React prototype's mock/local business behavior one screen at a time. A screen is credited only when it:

1. reads authoritative native DTOs through an already-frozen Phase 6 action;
2. validates user input without inventing business rules;
3. awaits the native command and shows its real success or failure;
4. refreshes the queries declared by the bridge contract;
5. relies on native session, permission, actor, and persistence enforcement;
6. has packaged-WebView integration evidence; and
7. no longer executes its former localStorage/mock business path.

The frozen logic specification, SQL blueprint, Phase 5 DTO catalog, and Phase 6 contract remain authoritative. Phase 8 may implement contracted runtime adapters and presentation, but it must not silently amend accounting or persistence semantics.

## 2. Screen capability ledger

| Screen/workflow | Contract boundary | Status | Next work |
|---|---|---|---|
| Owner bootstrap, login, logout, resume | `query.bootstrapStatus`, `query.currentSession`, auth commands | **Proven in Phase 7; retained** | Recovery and own-passphrase screens remain. |
| Products | `query.masterData`, create/update/archive product commands | **Milestone 1 complete** | Preserve as the reference screen pattern. |
| Customers | `query.masterData`, create/rename/save-profile customer commands | Queued | Migrate customer profile management without client-side balances. |
| Suppliers | `query.masterData`, create/rename/save-profile vendor commands | Queued | Use native canonical vendor identity and balances. |
| Employees and attendance | `query.masterData`, `query.attendance`, employee/profile/attendance commands | Queued | Keep app users separate from employee profiles. |
| Expense categories and settings | `query.businessSettings`, master-data/settings commands | Queued | Use editable store identity and PKR-native settings. |
| Daily operations | dashboard/reference queries and purchase/sale/production/expense/payment/spoilage commands | Queued | No UI-side totals, stock mutation, or cost formulas. |
| Deliveries / wholesale | transaction references, deliveries, sale and delivery commands | Queued | Preserve delivery status and sale-link semantics. |
| Customer/supplier ledgers | account-ledger query plus collection/payment commands | Queued | Display canonical native balances only. |
| History and corrections | event history/detail and correction command | Queued | Never edit or delete historical events in place. |
| Cash count and daily close | reconciliation query and count/close/reopen commands | Queued | Keep verified native actor and closed-day enforcement. |
| Dashboard and reports | dashboard, cashier, financial, profitability queries | Queued | Translate coverage/costing codes into plain shop language. |
| Costing and planning controls | costing/planning queries and versioned policy/target commands | Queued | Sensitive changes require the contract's recent-authentication rule. |
| Staff users and security audit | users/audit queries and user-administration commands | Queued | Owner controls role/status; employee linkage remains optional. |
| Encrypted backup and restore | backup system actions | Queued | Retain picker/ticket, inspection, confirmation, and session invalidation boundaries. |

## 3. Milestone 1 — Products

The Phase 7 product proof has been replaced by a complete native-backed Products workspace:

- active and archived product counts and filtered lists;
- add product;
- edit name, unit, optional default selling rate, stock-tracking choice, and notes;
- archive confirmation that explains history is retained;
- restore archived product;
- exact string-based PKR/paisa conversion with no JavaScript money arithmetic;
- owner/manager usability based on native effective permissions;
- native authorization repeated for every command;
- awaited mutation followed by a fresh `query.masterData` read;
- distinct user-facing error paths for validation, duplicate name, missing record, expired session, and permission denial;
- durable actor snapshots for create, update, archive, and restore.

The runtime advertises ten implemented actions: the eight Phase 7 actions plus `command.updateProduct` and `command.setProductArchived`. The canonical contract remains version 1 with 78 operations; unimplemented actions are not advertised by the runtime handshake.

## 4. Safety and business boundaries

- Archiving changes future availability; it does not delete products or rewrite transaction history.
- Inventory quantity remains event-derived. This screen has no direct “set stock” operation.
- Product default selling rate is optional and does not become an authoritative inventory cost.
- React supplies neither actor identity nor role; Kotlin resolves both from the memory-only session.
- Cashier update/archive attempts are rejected by the native permission boundary even if the bridge is called directly.
- The old prototype view model remains inactive and is not imported by the packaged app.
- No Room schema, frozen business formula, frozen DTO, or canonical bridge endpoint changed.

## 5. Verification checkpoint

Physical device: `SM-A065F`.

- Phase 8 focused product-management suite — **3/3 PASS**;
- complete connected-device regression — **130/130 PASS**, 0 skipped, 0 failed, 0 errors;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript check and production Vite build — PASS;
- explicit web bundle packaging and Android asset gate — PASS;
- Kotlin and Android-test Kotlin compilation — PASS;
- Phase 6 contract drift check — version 1, 78 operations, original SHA-256 — PASS;
- frozen logic-spec and SQL hashes — unchanged.

The focused tests prove owner update/archive/restore with Room readback and durable actor audit, direct cashier denial, missing-record error mapping, and a packaged React add/edit/archive/restore round trip.

## 6. Current limitations and next milestone

Phase 8 is not complete. Only authentication and Products execute through the active React product. Customer, supplier, employee, transactions, ledgers, reports, settings, users, and backup screens are still unwired; their prototype code must not be mistaken for operational functionality.

The next milestone is **Customer Profiles**, followed by Suppliers. It should extend the established master-data screen pattern without exposing client-side balance editing or mixing customer profiles with application-user accounts.

## Plain English

The Products part of Saaf Milk is now real. The owner can add a product, correct its details, hide it from future work without erasing its history, and bring it back later. Android checks the owner's authority and saves who made each change. The rest of the app is still being connected screen by screen, so Phase 8 remains open and Customers are next.
