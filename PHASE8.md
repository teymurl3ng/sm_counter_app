# Phase 8 — Screen-by-Screen Native UI Wiring

**Status: IN PROGRESS — Products and Customer Profiles complete 2026-08-28**

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
| Customers | `query.masterData`, create/rename/save-profile customer commands | **Milestone 2 complete** | Balance remains native-read-only; inactive customers retain history. |
| Suppliers | `query.masterData`, create/rename/save-profile vendor commands | **Next** | Use native canonical vendor identity and balances. |
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

## 4. Milestone 2 — Customer Profiles

The active app now provides a native-backed Customers area beside Products:

- active and inactive customer counts, filters, and local presentation-only search;
- add customer identity and save its optional profile;
- rename without changing account identity or rewriting history;
- phone, route/area, address, notes, optional PKR credit limit, and optional usual daily quantity;
- explicit deactivation warning and restoration;
- canonical native balance display with no editable balance field and no client-side balance calculation;
- native rejection of duplicate names, missing/non-customer IDs, negative limits/quantities, expired sessions, and insufficient permissions;
- durable actor snapshots for create, rename, profile change, deactivate, and restore;
- cleaned shop-facing customer names while the internal `Customer: ` account prefix remains a persistence concern only.

The runtime advertises thirteen implemented actions: the eight Phase 7 actions, the two additional Product actions, and `command.createCustomer`, `command.renameCustomer`, and `command.saveCustomerProfile`. The canonical contract remains version 1 with 78 operations; unimplemented actions are not advertised by the runtime handshake.

## 5. Safety and business boundaries

- Archiving changes future availability; it does not delete products or rewrite transaction history.
- Inventory quantity remains event-derived. This screen has no direct “set stock” operation.
- Product default selling rate is optional and does not become an authoritative inventory cost.
- React supplies neither actor identity nor role; Kotlin resolves both from the memory-only session.
- Cashier update/archive attempts are rejected by the native permission boundary even if the bridge is called directly.
- Customer balances are read-only projections of native financial events. The profile screen cannot create, edit, or clear a balance.
- Deactivating a customer prevents new operations but preserves their identity, balance, and history.
- Customer credit limit is a profile control, not the customer's balance.
- The old prototype view model remains inactive and is not imported by the packaged app.
- No Room schema, frozen business formula, frozen DTO, or canonical bridge endpoint changed.

## 6. Structural refactor checkpoint

Before starting Supplier Profiles, the active hybrid path was reorganized without changing its public behavior:

- the 733-line Phase 8 shell was split into authentication, workspace navigation, Product, Customer, shared UI, and shared model modules; the shell is now 207 lines and no new handwritten React module exceeds 207 lines;
- the 518-line bridge dispatcher was split into protocol validation, domain handlers, JSON mapping, and a 56-line dispatcher;
- the 660-line Phase 5 read-model service remains the stable caller-facing façade but is now 47 lines, delegating to focused identity/master-data, operational, financial, costing/planning, and security readers;
- generated contract projections and inactive inherited prototype screens were deliberately left alone;
- future handwritten files should be reviewed for separation when they approach roughly 300–400 lines, with responsibility and cohesion taking priority over an arbitrary number.

The checkpoint changed no Room schema, DTO shape, bridge action, permission rule, accounting formula, element ID, or user workflow. Its focused device regression covered the Phase 5 read model and Phase 7–8 hybrid flows: **13/13 PASS**.

## 7. Verification checkpoint

Physical device: `SM-A065F`.

- Phase 8 focused product-management suite — **3/3 PASS**;
- Phase 8 focused Customer Profiles suite — **3/3 PASS**;
- complete connected-device regression — **133/133 PASS**, 0 skipped, 0 failed, 0 errors;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript check and production Vite build — PASS;
- explicit web bundle packaging and Android asset gate — PASS;
- Kotlin and Android-test Kotlin compilation — PASS;
- Phase 6 contract drift check — version 1, 78 operations, original SHA-256 — PASS;
- frozen logic-spec and SQL hashes — unchanged.

The focused tests prove Product owner update/archive/restore and Customer owner create/rename/profile/deactivate/restore with Room readback and durable actor audit, direct cashier denial, missing-record and duplicate-name error mapping, absence of an editable customer balance, and packaged React round trips.

## 8. Current limitations and next milestone

Phase 8 is not complete. Authentication, Products, and Customer Profiles execute through the active React product. Supplier, employee, transactions, ledgers, reports, settings, users, and backup screens are still unwired; their prototype code must not be mistaken for operational functionality.

The next milestone is **Supplier Profiles**, followed by Employees. It should reuse the established master-data screen pattern, retain native supplier balances, and avoid reviving the prototype's unsupported per-supplier milk-rate behavior.

## Plain English

The Products and Customers parts of Saaf Milk are now real. Their code has also been separated into smaller, clearly named parts before more screens are added. The owner can maintain customer details and deactivate someone without erasing what they owe or their past transactions. Customer balances cannot be typed over; they still come from actual shop activity. Phase 8 remains open, and Suppliers are next.
