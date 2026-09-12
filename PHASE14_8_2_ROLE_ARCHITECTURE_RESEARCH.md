# Phase 14.8.2 — Role architecture research and Saaf proposal

Status: **ACCEPTED AS THE DESIGN FOUNDATION — implementation still gated by journey review**

Date: 2026-09-12

## Decision first

Saaf should keep five built-in role profiles in the product model, but it should not present all five as equally necessary:

1. **Owner** — required and unique in authority.
2. **Cashier** — the normal sales account.
3. **Store manager** — optional; used only when someone other than the owner runs a day or shift.
4. **Operations staff** — optional; the clearer name for the current `Operator` account. It is for milk receiving, production, spoilage, delivery, and work-floor tasks.
5. **Administrator** — optional, advanced, and high trust. It is a delegated head-office/business-system account, not another name for manager.

A small shop can operate with only Owner and Cashier. Store manager and Operations staff appear when the real division of work needs them. Administrator should sit behind an advanced role choice so it is not assigned casually.

## What the industry does consistently

Established products differ in labels, but their structures converge on six useful rules.

### 1. Owner is a special authority, not an ordinary editable role

Lightspeed separates Account owner from Admin. Its owner alone controls account-level powers such as ownership, subscription, critical export, and cancellation; Admin is a delegated full-store/head-office role. Loyverse also makes Owner the one immutable all-rights role while allowing other role groups to be edited.

Sources: [Lightspeed X-Series roles](https://x-series-support.lightspeedhq.com/hc/en-us/articles/25534171377819-Setting-user-roles-and-permissions), [Loyverse access rights](https://help.loyverse.com/help/how-manage-access-rights-employees)

### 2. Manager and administrator are different jobs

Lightspeed describes Manager as responsible for an assigned store, its reports, products, and staff; Admin can view and change all stores and is intended for owners or head office. Shopify similarly describes a store manager as a POS supervisor who can approve restricted actions and perform store operations. An administrator controls the system; a manager controls the day.

Sources: [Lightspeed R-Series roles](https://retail-support.lightspeedhq.com/hc/en-us/articles/229129608-Setting-up-employee-roles-and-access), [Shopify predefined POS roles](https://help.shopify.com/en/manual/your-account/users/roles/predefined-roles)

### 3. Cashier is a narrow transaction role

Shopify's Cashier role centers on checkout, customers needed during checkout, and register/payment tracking. Lightspeed's Cashier processes sales and closes a register but is restricted from reporting and product editing. Loyverse separately controls refunds, all-receipt access, shift reports, item cost, item management, and settings.

Sources: [Shopify predefined POS roles](https://help.shopify.com/en/manual/your-account/users/roles/predefined-roles), [Lightspeed X-Series roles](https://x-series-support.lightspeedhq.com/hc/en-us/articles/25534171377819-Setting-user-roles-and-permissions), [Loyverse access rights](https://help.loyverse.com/help/how-manage-access-rights-employees)

### 4. Work-floor staff use a task surface, not the back office

Retail-only systems often omit an `Operator` preset because they do not include production. Manufacturing systems use the term for a worker who performs assigned work orders. Odoo Shop Floor shows operators their own work orders and the actions needed to complete production; deeper manufacturing administration remains elsewhere. Saaf genuinely has receiving, yogurt production, spoilage, and delivery work, so this job exists even though the current label is vague.

Source: [Odoo Shop Floor overview](https://www.odoo.com/documentation/18.0/applications/inventory_and_mrp/manufacturing/shop_floor/shop_floor_overview.html)

### 5. Mature systems use permission sets and scope, not an ever-growing role enum

Square defines permission sets across POS, Team, and Dashboard and scopes a team member to assigned locations. Shopify and Loyverse let businesses create or edit permission-based roles. Odoo separates read, create, write, and delete rights and can further restrict which records a user may touch.

Sources: [Square permission sets](https://squareup.com/help/us/en/article/5822-employee-permissions), [Shopify POS permissions](https://help.shopify.com/en/manual/your-account/users/roles/permissions/pos-permissions), [Odoo access rights](https://www.odoo.com/documentation/16.0/applications/general/users/access_rights.html)

### 6. Risky actions can require a supervisor without changing the worker's role

Shopify permissions can be Allowed, Denied, or Approval required, with a manager entering a PIN for the one action. Loyverse likewise opens a PIN approval panel when a worker tries a restricted function. This prevents role proliferation and fits a shared shop phone.

Sources: [Shopify POS permissions](https://help.shopify.com/en/manual/your-account/users/roles/permissions/pos-permissions), [Loyverse access rights](https://help.loyverse.com/help/how-manage-access-rights-employees)

## Recommended Saaf role definitions

### Owner — business authority and data custodian

**Who this is:** the proprietor or the person ultimately accountable for the shop and its records.

**Primary product experience:** decision-first overview: sales, cash, stock, customer debt, supplier debt, profitability, exceptions, closing state, and backup health.

**Direct access:** every operational, financial, people, configuration, security, backup, and recovery capability.

**Protected even for Owner:** restore, reopening a day, changing an Owner/Administrator, changing opening balances or costing policy, and recovery changes require recent strong authentication and an audit entry. The final active owner cannot be disabled or demoted.

### Administrator — delegated business-system control

**Who this is:** a highly trusted head-office or business administrator who maintains the system across the business. This is not a technical support account and not the normal shift supervisor.

**Primary product experience:** administration queue: staff accounts, master data, configuration health, audit issues, backups, and cross-business records. Daily transaction tools remain available as secondary tools.

**Direct access:** ordinary daily work, full reports, master data, settings, costing/planning, ordinary staff accounts, security audit, and encrypted local backup export.

**No access:** create/promote/demote/disable Owners or Administrators; owner recovery policy; cloud-backup account connection; backup restore; ownership transfer; final-owner controls.

**Product rule:** the normal `Create staff account` flow offers Manager, Cashier, and Operations staff. Administrator is created from a separate `Add trusted administrator` action with explicit reauthentication.

### Store manager — daily shop/shift authority

**Who this is:** the person trusted to run the shop when the Owner is absent.

**Primary product experience:** today's work and exceptions: incomplete operations, cash count, close readiness, deliveries, attendance, and records needing correction.

**Direct access:** all normal daily entries, supplier payments, ordinary expenses, production, spoilage review, deliveries, daily operational reporting, assigned-shop customer/supplier balances, products/customers/suppliers, attendance management, corrections, cash count, and day close.

**No access by default:** full profitability and long-period owner analytics; employee salary values or salary payments; opening balances; costing/yield policy; business identity/rates; users/security audit; backups/restore; owner recovery; day reopen.

**Reason:** Lightspeed gives managers store reporting and operational/catalog control, but sensitive cost, report, setup, and user rights remain separately configurable. Saaf should start conservatively and make later grants explicit.

### Cashier — checkout and customer payment worker

**Who this is:** the person serving customers and handling the active cash shift.

**Primary product experience:** New sale, with collection and recent activity close at hand.

**Direct access:** sales; customer collections; select an existing customer; quick-add a customer's basic name/phone during checkout; view only the selected customer's usable credit context; create/link a delivery from the current sale; view the Cashier's own/current-shift transactions; submit a blind physical cash count; personal security.

**Supervisor approval or separate grant:** correcting/voiding a sale, recording a petty-cash expense, exceeding a credit limit, changing a sale rate outside allowed policy, or accessing older/other employees' receipts.

**No access:** full history, profit, product cost, supplier/employee records, supplier or salary payments, production/spoilage, product/rate management, expected cash before counting, global day close/reopen, settings, users, audit, or backup.

### Operations staff — receiving, production, stock, and delivery worker

**Who this is:** the worker who receives raw milk, makes yogurt, records loss, or carries out deliveries. `Operations staff` replaces the unclear display label `Operator`; the stored wire value can remain `operator` for compatibility.

**Primary product experience:** assigned work: Receive milk, Make yogurt, Record spoilage, Deliveries, and My attendance, plus the quantities needed to do those jobs.

**Direct access:** choose a supplier for receipt; record received quantity using the configured rate; record production; record spoilage with reason; update assigned delivery status; view relevant stock quantities and own/current-day operational records; record own attendance; personal security.

**Supervisor approval or separate grant:** overriding the configured purchase rate, unusually large spoilage, cancelling/archiving a delivery, correcting a saved event, or reconciling an inventory count.

**No access:** sales and collections; customer/supplier/employee balances; cash; expenses or payments; profit, costs, or payroll; product/customer/supplier editing; attendance for other employees; settings; users; audit; backup.

## Which roles are needed for Saaf today?

| Role profile | Needed in every installation? | When it earns its place |
|---|---:|---|
| Owner | Yes | The app needs one accountable authority and recovery/data custodian. |
| Cashier | Yes as a preset | Any staff member handles sales without receiving owner powers. |
| Store manager | No | Someone other than Owner supervises the day, exceptions, records, and close. |
| Operations staff | No | Receiving/production/delivery work is delegated to a worker who should not see money. |
| Administrator | No; advanced | A larger business delegates system/head-office control but retains owner-only recovery authority. |

The roles should remain available, but setup should explain that a two-person shop can start with Owner and Cashier only.

## Screen and action policy

Legend:

- **Full** — direct access to the complete screen/action.
- **Scoped** — only the facts or records necessary for that person's work.
- **Approve** — allowed only after an authorized supervisor approves that action.
- **No** — hidden from navigation and rejected by the native service boundary.

### Home, reports, and records

| Screen or action | Owner | Administrator | Store manager | Cashier | Operations staff |
|---|---:|---:|---:|---:|---:|
| Role-specific home | Full business overview | Full control overview | Daily shop overview | Own shift summary | Assigned work and stock |
| Daily sales and cash status | Full | Full | Full | Scoped to own shift | No |
| Stock quantities | Full | Full | Full | Scoped to sale availability | Scoped to handled products |
| Customer/supplier balance totals | Full | Full | Full | Selected customer only | No |
| Full profitability, margin, and long-period trends | Full | Full | No | No | No |
| Product cost and stock value | Full | Full | No by default | No | No |
| Payroll and employee salary balances | Full | Full | No | No | No |
| Complete event history and search | Full | Full | Assigned shop | Own/current shift | Own/current-day operations |
| Event detail | Full | Full | Assigned shop | Scoped sale/collection detail | Scoped operation detail |
| Corrections | Full | Full | Full | Approve | Approve |
| Customer and supplier ledgers | Full | Full | Full | Selected customer context | No |
| Cash-close archive | Full | Full | Assigned shop | Own submitted counts only | No |
| Security history | Full | Full | No | No | No |

### Sales, money, and close

| Screen or action | Owner | Administrator | Store manager | Cashier | Operations staff |
|---|---:|---:|---:|---:|---:|
| Record sale | Full | Full | Full | Full | No |
| Record customer collection | Full | Full | Full | Full | No |
| Change/override selling rate | Full | Full | Full with audit | Approve | No |
| Record ordinary expense | Full | Full | Full | Approve | No |
| Pay supplier | Full | Full | Full | No | No |
| Pay salary | Full | Full | No | No | No |
| Enter physical cash count | Full | Full | Full | Full, blind count | No |
| See expected cash before count | Full | Full | Full | No | No |
| Close global business date | Full | Full | Full | No | No |
| Reopen closed business date | Full | No by default | No | No | No |

Because Saaf currently closes one global business date rather than one employee's register, `Cashier` must not retain the current ability to close the whole day. A Cashier can submit a count; a Manager, Administrator, or Owner reviews and closes.

### Receiving, production, stock, and delivery

| Screen or action | Owner | Administrator | Store manager | Cashier | Operations staff |
|---|---:|---:|---:|---:|---:|
| Receive milk at configured rate | Full | Full | Full | No | Full |
| Override purchase rate | Full | Full | Full with audit | No | Approve |
| Record yogurt production | Full | Full | Full | No | Full |
| Record spoilage/loss | Full | Full | Full | No | Full; large loss flagged/approved |
| Review spoilage/yield exceptions | Full | Full | Full | No | Own entries only |
| Create delivery | Full | Full | Full | Scoped from sale | Full |
| Link delivery to sale | Full | Full | Full | Own sale | View only |
| Update assigned delivery status | Full | Full | Full | View only | Full |
| Cancel/archive delivery | Full | Full | Full | No | Approve |
| Inventory count entry (future) | Full | Full | Full | No | Full |
| Reconcile inventory count (future) | Full | Full | Full | No | Approve |

### Products, customers, suppliers, and employees

| Screen or action | Owner | Administrator | Store manager | Cashier | Operations staff |
|---|---:|---:|---:|---:|---:|
| Product list | Full | Full | Full | Sale picker only | Stock/task picker only |
| Add/edit/archive product | Full | Full | Full with audit | No | No |
| Change default selling rate | Full | Full | Full with audit | No | No |
| Customer list and profile | Full | Full | Full | Checkout search only | No |
| Quick-add customer name/phone | Full | Full | Full | Full | No |
| Change credit limit/quota or deactivate customer | Full | Full | Full | No | No |
| Supplier list and profile | Full | Full | Full | No | Receipt picker only |
| Add/edit/deactivate supplier | Full | Full | Full | No | No |
| Employee directory without pay | Full | Full | Full | Own profile | Own profile |
| Employee salary, salary balance, and pay settings | Full | Full | No | No | No |
| Add/edit/deactivate employee profile | Full | Full | Scoped non-pay fields only | No | No |
| Record own attendance | Full | Full | Full | Full | Full |
| View/record/revise other employees' attendance | Full | Full | Full | No | No |
| Expense categories | Full | Full | Full | Picker only | No |

### Settings, access, and backup

| Screen or action | Owner | Administrator | Store manager | Cashier | Operations staff |
|---|---:|---:|---:|---:|---:|
| Shop identity and business rates | Full | Full | View only if useful | No | No |
| Opening balances and opening stock cost | Full | Full with strong auth | No | No | No |
| Budgets and collection targets | Full | Full | View assigned targets | No | No |
| Costing/yield policies and loss assessment | Full | Full | View approved policy | No | No |
| Own passphrase and Quick unlock | Full | Full | Full | Full | Full |
| Create/disable Manager, Cashier, Operations accounts | Full | Full | No by default | No | No |
| Create/change Owner or Administrator | Full with strong auth | No | No | No | No |
| View security audit | Full | Full | No | No | No |
| Create encrypted local backup | Full | Full | No | No | No |
| Connect/configure/run owner Drive backup | Full | No | No | No | No |
| Inspect and restore backup | Full with strong auth | No | No | No | No |

## Problems in the current frozen permission model

The existing five roles are enforced correctly, but several permission bundles are too broad for the clarified jobs:

1. `VIEW_OPERATIONAL` also unlocks complete event history and event detail. It cannot restrict a Cashier to their shift or Operations staff to operational events.
2. `VIEW_FULL_FINANCIAL` gives Manager the same financial read boundary as Owner/Admin. It cannot separate daily shop totals from profit, cost, payroll, and long-period analysis.
3. `MANAGE_MASTER_DATA` combines products, customers, suppliers, employees, salary-bearing employee profiles, and expense categories.
4. `SALE_AND_COLLECTION` cannot separately grant sales and customer-debt collection.
5. `PRODUCTION_AND_SPOILAGE` treats ordinary production and loss adjustment as the same level of risk.
6. `DELIVERY` combines create, link, transition, cancel, and archive.
7. `ATTENDANCE` allows the holder to record attendance for any employee; it does not express `own attendance`.
8. `CASH_COUNT_AND_CLOSE` combines entering a count with closing the global business date. This currently makes Cashier too powerful.
9. `MANAGE_BUSINESS_SETTINGS` combines shop identity/rates, opening state, budgets, costing, yield policy, and loss assessment.
10. `CREATE_EXPORT_BACKUP` does not express the difference between local export and the owner-controlled cloud account.
11. The current Manager can pay salaries and see salary-bearing employee records; the proposed job does not require those powers.
12. `Admin` is almost Owner in code but was being designed as another work role. Its product meaning must become explicit before its screen is designed.

## Permission model Saaf should grow toward

Do not create many more fixed account types. Keep the five profiles as understandable presets and make the authorization decision from four parts:

1. **Capability** — sale, collection, milk receipt, production, expense, customer edit, product edit, cash count, close, correction, and so on.
2. **Data scope** — own record, current shift, assigned shop/branch, assigned branches, or whole business.
3. **Decision mode** — direct, manager approval required, or denied.
4. **Sensitive fields** — explicit visibility for cost/margin, expected cash, customer credit, supplier balance, payroll, and security/recovery data.

This is a hybrid of simple role presets and scoped policy. It avoids both extremes: five rigid bundles that become inaccurate, and a wall of dozens of permission switches that a small-shop owner cannot understand.

The first editable-role UI should offer job-language bundles such as `Sales`, `Cash handling`, `Receiving`, `Production`, `Deliveries`, `People`, `Reports`, and `Shop control`. A detailed permission editor can remain an advanced future feature.

## Future hierarchy without role explosion

Future jobs become saved permission profiles with data scope rather than new hard-coded application roles:

- Branch manager = Manager capabilities + one branch scope.
- Regional manager = Manager capabilities + several branch scopes.
- Inventory supervisor = products/receiving/count/reconcile capabilities, no cash or payroll.
- Production supervisor = production/spoilage review and costing-policy visibility.
- Accountant/bookkeeper = financial reports, ledgers, expenses/payments, and read-only operations.
- HR/payroll = employee compensation, salary payment, and attendance, no sales or inventory.
- Delivery rider = assigned deliveries and status updates only.
- Auditor = read-only records and audit for a defined period/scope.

The UI shell should be derived from effective capabilities and job profile. A future custom role can therefore receive a suitable work surface without duplicating business logic.

## Native enforcement and UI rules

- Hidden menus improve clarity; the Kotlin service boundary remains the authority.
- Read queries enforce the same record scope and sensitive-field rules as commands. Filtering only in Compose is insufficient.
- New capabilities default to denied until deliberately added to a profile.
- Every sensitive action records actor, role/profile, scope, time, result, and approving actor when approval was required.
- Account switching rebuilds the shell from the new effective policy.
- Personal passphrase, Quick unlock, account switching, and sign out remain available to every active account.
- Role/job title and employee profile remain separate. `Delivery rider` may be a job title attached to an Operations-staff permission profile.

This follows OWASP guidance to enumerate user/resource/operation combinations, grant only what a job needs, deny by default, validate every request, and use record/context attributes where simple role checks are too coarse: [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html).

## Review checkpoint and next step

The owner should review three decisions before any role-specific screen journey is drawn:

1. keep Administrator as an advanced high-trust head-office role rather than merging it with Manager;
2. retain the operational job but rename it `Operations staff` and narrow its data visibility;
3. replace the current broad yes/no bundles with the proposed screen/action boundaries, beginning with scoped history, split master data, split cash count/day close, and split attendance.

After those decisions are accepted, the next show-before-build checkpoint is a complete visual journey for Owner, Cashier, Store manager, Operations staff, and Administrator based on this policy. Compose implementation remains later.
