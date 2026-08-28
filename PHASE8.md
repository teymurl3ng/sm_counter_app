# Phase 8 — Screen-by-Screen Native UI Wiring

**Status: IN PROGRESS — Products, Customers, Suppliers, Employees/Attendance, and core Settings complete 2026-08-28**

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
| Suppliers | `query.masterData`, create/rename/save-profile vendor commands | **Milestone 3 complete** | Balance remains native-read-only; no per-supplier milk rate. |
| Employees and attendance | `query.masterData`, `query.transactionReferences`, `query.attendance`, employee/profile/attendance commands | **Milestone 4 complete** | Employee records remain separate from app logins; operators can mark attendance without profile authority. |
| Expense categories and core settings | `query.businessSettings`, store/rate/category commands | **Milestone 5 complete** | Editable store identity, fixed PKR, shop-wide rates, and non-destructive category lifecycle. |
| Opening state setup/correction | business-settings/reference queries, opening commands, recent authentication | **Next** | Complete before daily operations so a fresh shop can deliberately record real starting balances. |
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

## 5. Milestone 3 — Supplier Profiles

The active app now provides a native-backed Suppliers area beside Products and Customers:

- active and inactive supplier counts, filters, and local presentation-only search;
- add and rename supplier identity without changing the canonical account ID or rewriting history;
- phone, location, and notes profile fields;
- explicit deactivation warning and restoration;
- canonical balance display sourced from saved purchases and payments, with no editable balance field;
- no per-supplier milk-rate field—the approved shop-wide purchase settings remain authoritative;
- native duplicate-name, missing-record, session, and permission enforcement;
- durable actor snapshots for create, rename, profile change, deactivate, and restore;
- clean shop-facing supplier names while the internal `Vendor: ` account prefix remains a persistence concern only.

## 6. Milestone 4 — Employees and Attendance

The active app now provides a native-backed Employees area:

- owner/manager employee profile maintenance with active/inactive lists, search, add, rename, deactivate, and restore;
- optional job label, phone, reference salary, salary period, and join date;
- canonical salary balance displayed read-only from native records;
- explicit in-app wording that an employee record does not create an application login;
- a daily attendance register using the device-local date, with present, absent, and half-day choices plus optional notes;
- same-day attendance revision that archives the earlier record instead of editing or deleting history;
- attendance-only operator access through restricted employee references, without master-data profile access;
- native duplicate-name, missing/inactive employee, date, status, session, and permission enforcement;
- durable actor snapshots for employee profile changes and attendance writes;
- clean shop-facing employee names while the internal `Employee: ` account prefix remains a persistence concern only.

The runtime advertises twenty-two implemented actions. This adds `query.transactionReferences`, `query.attendance`, the three employee profile commands, and `command.recordAttendance` to the prior sixteen. The canonical contract remains version 1 with 78 operations; unimplemented actions are not advertised by the runtime handshake.

## 7. Milestone 5 — Expense Categories and Core Settings

The active app now provides a permission-aware Settings area:

- editable shop name, optional phone, and optional address, persisted as the singleton store identity;
- fixed read-only `PKR` currency enforced again by the native store service;
- exact PKR/paisa handling for supplier milk rate, supplier delivery fee, retail milk/yogurt rates, and default delivery surcharge;
- canonical decimal handling for the native total-solids quality standard;
- clear wording that these are shop-wide defaults and that React performs no purchase/sale calculation;
- manager-accessible expense-category add, deactivate, and restore without granting business-rate authority;
- retained category/account identity and historical expense meaning when a category is deactivated;
- native positive-value, duplicate-name, missing-record, session, and permission enforcement;
- durable actor snapshots for store, rate, and category changes.

The runtime now advertises twenty-seven implemented actions: `query.businessSettings`, the two store/rate commands, and the two expense-category commands join the prior twenty-two. The canonical contract remains version 1 with 78 operations.

## 8. Safety and business boundaries

- Archiving changes future availability; it does not delete products or rewrite transaction history.
- Inventory quantity remains event-derived. This screen has no direct “set stock” operation.
- Product default selling rate is optional and does not become an authoritative inventory cost.
- React supplies neither actor identity nor role; Kotlin resolves both from the memory-only session.
- Cashier update/archive attempts are rejected by the native permission boundary even if the bridge is called directly.
- Customer balances are read-only projections of native financial events. The profile screen cannot create, edit, or clear a balance.
- Deactivating a customer prevents new operations but preserves their identity, balance, and history.
- Customer credit limit is a profile control, not the customer's balance.
- Supplier balances are read-only projections of native purchase and payment events.
- Deactivating a supplier prevents new operations but preserves identity, payable balance, and history.
- Supplier profiles do not override the shop-wide milk purchase rate or delivery-fee settings.
- Employee profile creation never creates a login, credential, role, or app permission.
- Reference salary is planning/profile information; the canonical employee balance still comes only from saved salary transactions.
- Attendance revisions preserve the superseded record. Inactive employees cannot receive new attendance marks.
- Operators with attendance permission receive active employee references, not unrestricted master-data access.
- Store identity is editable; it is not hardcoded in React. Currency remains database-constrained to PKR.
- Business rates must be greater than zero and are stored through the existing atomic native settings service.
- Supplier profiles still cannot override the shop-wide milk buying rate.
- Expense-category deactivation removes future selection only; it does not delete the account or old expenses.
- The old prototype view model remains inactive and is not imported by the packaged app.
- No Room schema, frozen business formula, frozen DTO, or canonical bridge endpoint changed.

## 9. Structural refactor checkpoint

Before starting Supplier Profiles, the active hybrid path was reorganized without changing its public behavior:

- the 733-line Phase 8 shell was split into authentication, workspace navigation, Product, Customer, Supplier, Employee Profile, Attendance, Business Settings, Expense Categories, bridge-action, shared UI, and shared model modules; after Settings wiring the shell is 265 lines and no new handwritten React module exceeds 265 lines;
- the 518-line bridge dispatcher was split into protocol validation, domain handlers, JSON mapping, and a 56-line dispatcher;
- the 660-line Phase 5 read-model service remains the stable caller-facing façade but is now 47 lines, delegating to focused identity/master-data, operational, financial, costing/planning, and security readers;
- generated contract projections and inactive inherited prototype screens were deliberately left alone;
- future handwritten files should be reviewed for separation when they approach roughly 300–400 lines, with responsibility and cohesion taking priority over an arbitrary number.

The checkpoint changed no Room schema, DTO shape, bridge action, permission rule, accounting formula, element ID, or user workflow. Its focused device regression covered the Phase 5 read model and Phase 7–8 hybrid flows: **13/13 PASS**.

## 10. Verification checkpoint

Physical device: `SM-A065F`.

- Phase 8 focused product-management suite — **3/3 PASS**;
- Phase 8 focused Customer Profiles suite — **3/3 PASS**;
- Phase 8 focused Supplier Profiles suite — **3/3 PASS**;
- Phase 8 focused Employees/Attendance suite — **4/4 PASS**;
- Phase 8 focused Settings/Expense Categories suite — **4/4 PASS**;
- complete connected-device regression — **144/144 PASS**, 0 skipped, 0 failed, 0 errors;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript check and production Vite build — PASS;
- explicit web bundle packaging and Android asset gate — PASS;
- Kotlin and Android-test Kotlin compilation — PASS;
- Phase 6 contract drift check — version 1, 78 operations, original SHA-256 — PASS;
- frozen logic-spec and SQL hashes — unchanged.

The focused tests prove Product, Customer, Supplier, and Employee lifecycle; attendance access/revision history; editable PKR store identity; exact shop-wide rate persistence; and expense-category lifecycle with Room readback and durable actor audit. Settings evidence also covers positive-value validation, manager/operator separation, duplicate and missing category handling, and a packaged React-to-Room round trip. A full-suite-only timing failure in the new packaged test was traced to checking Room before the awaited UI save completed; the assertion now waits for the native success notice, after which both focused and complete suites pass.

## 11. Current limitations and next milestone

Phase 8 is not complete. Authentication, Products, Customer Profiles, Supplier Profiles, Employees, Attendance, editable store identity, core business rates, and expense categories execute through the active React product. Opening state, transactions, ledgers, reports, costing/planning controls, users, and backup screens are still unwired; their prototype code must not be mistaken for operational functionality.

The next milestone is **Opening State Setup and Correction**. It must remain optional, be available only before the first business event for a fresh installation, never create artificial entries on upgrade, require recent native authentication, and append corrections rather than rewriting the original record. Completing it before Daily Operations prevents a new shop from accidentally starting transactions before deciding whether real opening balances need recording.

## Plain English

The shop name, contact details, PKR rates, and expense categories are now real and saved by the native app. Managers can maintain expense categories without being allowed to change shop-wide prices. Deactivating a category does not erase old expenses. Phase 8 remains open; the next step is the optional opening-balance workflow that a fresh shop may use before recording its first real transaction.
