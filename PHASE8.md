# Phase 8 — Screen-by-Screen Native UI Wiring

**Status: COMPLETE — all 21 screen-wiring milestones device-verified 2026-09-01**

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
| Owner bootstrap, login, logout, resume, recovery, own passphrase | bootstrap/session/auth commands | **Phase 7 proof retained; recovery/passphrase complete in Milestone 20** | Preserve one-time recovery-code display and post-change sign-in. |
| Products | `query.masterData`, create/update/archive product commands | **Milestone 1 complete** | Preserve as the reference screen pattern. |
| Customers | `query.masterData`, create/rename/save-profile customer commands | **Milestone 2 complete** | Balance remains native-read-only; inactive customers retain history. |
| Suppliers | `query.masterData`, create/rename/save-profile vendor commands | **Milestone 3 complete** | Balance remains native-read-only; no per-supplier milk rate. |
| Employees and attendance | `query.masterData`, `query.transactionReferences`, `query.attendance`, employee/profile/attendance commands | **Milestone 4 complete** | Employee records remain separate from app logins; operators can mark attendance without profile authority. |
| Expense categories and core settings | `query.businessSettings`, store/rate/category commands | **Milestone 5 complete** | Editable store identity, fixed PKR, shop-wide rates, and non-destructive category lifecycle. |
| Opening state setup/correction | business-settings/reference queries, opening commands, recent authentication | **Milestone 6 complete** | Optional fresh-install snapshot, explicit directions, and append-only correction. |
| Daily operations | dashboard/reference/financial queries and purchase/sale/production/expense/payment/spoilage commands | **Milestones 7–13 complete** | No UI-side totals, stock mutation, cost rates, or accounting formulas. |
| Deliveries / wholesale | transaction references, deliveries, sale and delivery commands | **Milestone 14 complete** | Pending/terminal history and optional sale linkage remain logistical records, not duplicate revenue. |
| Customer/supplier ledgers | account-ledger query plus collection/payment commands | **Milestone 15 complete** | Canonical balances, movements, running balances, inactive history, and contextual payments are device-proven. |
| History and corrections | event history/detail and correction command | **Milestone 16 complete** | Append-only correction chains, native authority, actor attribution, and superseded visibility are device-proven. |
| Cash count and daily close | reconciliation query and count/close/reopen commands | **Milestone 17 complete** | Independent physical counts, honest variances, verified close/reopen actors, and date locking are device-proven. |
| Dashboard and reports | dashboard, cashier, financial, profitability queries | **Milestone 18 complete** | Canonical date-range totals, role-filtered detail, and honest costing coverage are device-proven. |
| Costing and planning controls | costing/planning queries and versioned policy/target commands | **Milestone 19 complete** | Native versioning, recent authentication, yield calibration, and human loss review are device-proven. |
| Staff users and security audit | users/audit queries and user-administration commands | **Milestone 20 complete** | Owner/admin controls are native-authorized; employee linkage remains optional and credential setup is private. |
| Encrypted backup and restore | backup system actions | **Milestone 21 complete** | Preserve native picker/ticket, inspection, confirmation, confidentiality, and session-invalidation boundaries. |

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

## 8. Milestone 6 — Opening State Setup and Correction

The active Settings workspace now includes the approved optional opening-balance workflow:

- a fresh installation may record real pre-existing stock, cash/bank, customer dues, and supplier dues before its first business event;
- all-zero shops skip setup and receive no artificial opening event;
- upgraded or already-used installations cannot receive a late artificial opening entry;
- shop-facing directions distinguish “customer owes shop” from “shop owes supplier” without debit/credit terminology;
- initial entries accept only active, inventory-tracked products and active customer/supplier accounts; correction retains previously referenced archived/inactive records;
- the complete corrected opening picture is restated, while inventory and financial ledgers receive only the difference from the previous snapshot;
- the original event remains immutable and every correction points to the prior current event;
- owner/admin business-settings permission and a passphrase confirmed within five minutes are enforced natively;
- each opening event has a durable actor link and security-audit entry;
- the React workflow awaits reauthentication and the native command, then refreshes business settings and references.

The runtime now advertises thirty implemented actions. `command.auth.reauthenticate`, `command.recordOpeningState`, and `command.correctOpeningState` join the prior twenty-seven. The canonical contract remains version 1 with 78 operations.

## 9. Milestone 7 — Daily Operations: Supplier Milk Purchase

The active app now provides the first native-backed Daily Work slice:

- today's authoritative Milk and Yogurt stock plus purchased-milk total from `query.dashboard`;
- active, permission-scoped supplier choices and current shop-wide purchase-rate references from `query.transactionReferences`;
- supplier, business date, physical milk liters, fat test, LR reading, and optional notes entry;
- native-only SNF, TS, adjusted-liters, payable, stock, and supplier-balance calculation/posting through `command.recordPurchase`;
- stock increase by the physical milk received while supplier payment remains based on the snapshotted quality formula and delivery fee;
- an explicit result showing the amount owed and native quality results, without a competing JavaScript preview calculation;
- friendly native `day_closed`, missing/inactive supplier, validation, session, and permission failures;
- durable event actor attribution and a security-audit entry for each successful bridge purchase;
- awaited mutation followed by refreshed dashboard and permission-scoped transaction references;
- packaged physical-device proof from the React form through the bridge into Room.

Inactive customer and supplier profiles are now excluded from operational reference lists, preventing archived parties from appearing in future-transaction selectors. The runtime now advertises thirty-two implemented actions: `query.dashboard` and `command.recordPurchase` join the prior thirty. The canonical contract remains version 1 with 78 operations, and no Room schema or purchase formula changed.

## 10. Milestone 8 — Daily Operations: Sales

The Daily Work area now also provides native-backed sales:

- cashiers, managers, admins, and owners can enter retail or wholesale sales; operators remain purchase/production focused;
- quantity mode accepts the physical quantity sold, while value mode accepts the PKR sale value and lets native code derive quantity from the entered unit rate;
- milk and yogurt show the saved shop-wide retail rate as an editable starting value; no total is previewed or calculated by React;
- paid-now sales post to the selected Cash/Bank account, while credit sales post the amount owed to an active customer;
- the approved `retail`, `wholesale`, `credit-retail`, and `credit-wholesale` channel codes are the only accepted non-null sale channels;
- optional delivery charge can use the native saved default or an explicitly entered PKR amount;
- the success result displays native quantity, sale value, delivery charge, and total after stock and money postings commit together;
- `query.cashierSummary` provides today's base sales value and canonical Cash/Bank balances;
- inactive customers, archived products, missing accounts, invalid combinations, insufficient stock, closed history, expired sessions, and insufficient permission are rejected without a sale posting;
- closed-history detection mirrors the database rule: a later active close also protects earlier dates unless that exact date was reopened;
- each successful sale receives durable event actor attribution and a security-audit entry;
- packaged device evidence covers the React form through the bridge to canonical Room inventory and financial logs.

The runtime now advertises thirty-four implemented actions: `query.cashierSummary` and `command.recordSale` join the prior thirty-two. The canonical contract remains version 1 with 78 operations. No Room schema, sale formula, surcharge rule, or account-direction rule changed.

## 11. Milestone 9 — Daily Operations: Production

The Daily Work area now also provides native-backed milk-to-yogurt production:

- operators, managers, admins, and owners with production permission can record a completed yogurt batch;
- the form accepts business date, physical milk liters used, physical yogurt kilograms produced, and optional notes;
- current authoritative Milk stock is shown as a reference, while React performs no stock, yield, cost, or loss calculation;
- Kotlin validates positive quantities, active seeded Milk/Yogurt products, available Milk stock, permission, session, and open business history;
- a successful batch atomically reduces Milk stock and increases Yogurt stock through the existing `YogurtProductionService`;
- the saved batch remains available to the separate native yield-policy review, including the truthful `calibration_pending` state when no policy is active;
- recording production never automatically creates an abnormal-loss assessment or invents a financial loss;
- each successful production event receives durable actor attribution and a security-audit entry;
- the awaited mutation refreshes dashboard stock, event history, costing management, and profitability reporting as declared by the frozen contract;
- packaged device evidence covers the React form through the bridge to canonical Room inventory movements and actor audit.

The runtime now advertises thirty-five implemented actions: `command.recordProduction` joins the prior thirty-four. The canonical contract remains version 1 with 78 operations. No Room schema, production formula, yield policy, loss rule, or costing rule changed.

## 12. Milestone 10 — Daily Operations: Expenses and Contract v2

The Daily Work area now also provides native-backed shop expenses:

- owners, admins, managers, and cashiers with `EXPENSE` permission can record a shop expense;
- the form accepts an active expense category, Cash/Bank payment account, exact PKR amount, business date, and optional notes;
- Kotlin resolves the category ID to its saved active profile and verifies both backing account categories before posting;
- a successful expense atomically reduces the selected Cash/Bank balance and increases the selected Operating Expense account through the existing `ExpenseService`;
- invalid, archived, or mismatched accounts, non-positive amounts, closed history, expired sessions, and insufficient permission are rejected without an expense posting;
- each successful expense receives durable actor attribution and a security-audit entry;
- owners/admins/managers receive the authoritative current-day expense total through the newly wired `query.financialOverview`; cashiers remain denied the full financial response;
- packaged device evidence covers the React form through the bridge to canonical Room financial movements and actor audit.

Expense wiring exposed a least-privilege omission in frozen contract version 1: cashiers had `EXPENSE` permission but no permitted query returned the dynamic active category IDs required by `command.recordExpense`. Contract version 2 explicitly adds role-filtered active `expenseCategories` to `TransactionReferencesDto`. It retains all 78 endpoints and every command request shape; it grants no settings or master-data authority. Its canonical manifest SHA-256 is `AB374869603B9D773AD86B56EF801DA8C4493816C7301B05214AE596E33D0920`.

The runtime now advertises thirty-seven implemented actions: `query.financialOverview` and `command.recordExpense` join the prior thirty-five. No Room schema, expense posting rule, account direction, permission bundle, or business formula changed.

## 13. Milestone 11 — Daily Operations: Supplier and Customer Payments

The Daily Work area now also provides native-backed supplier payments and customer credit collections:

- owners, admins, and managers with `SUPPLIER_PAYMENT` permission can pay an active supplier; cashiers, managers, admins, and owners with `SALE_AND_COLLECTION` permission can collect money from an active customer;
- the shared form accepts the relevant supplier/customer, Cash/Bank account, exact PKR amount, business date, and optional notes;
- a supplier payment atomically reduces both the supplier payable and selected Cash/Bank balance through the existing `VendorPaymentService`;
- a customer collection atomically reduces the customer receivable and increases the selected Cash/Bank balance through the existing `CreditCollectionService`;
- payments greater than the current saved party balance remain valid advances, matching the existing native business rule instead of introducing an undocumented UI cap;
- inactive or mismatched party accounts, invalid payment accounts, non-positive amounts, closed history, expired sessions, and insufficient permission are rejected without posting;
- each successful payment receives durable actor attribution and a security-audit entry;
- the authoritative financial overview refreshes supplier/customer balances and period payment totals for full-financial roles, while the cashier summary refreshes collection totals and Cash/Bank balances without revealing the full financial report;
- packaged device evidence covers both form modes through the bridge to canonical Room financial movements and actor audit.

The runtime now advertises thirty-nine implemented actions: `command.recordVendorPayment` and `command.recordCreditCollection` join the prior thirty-seven. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, payment direction, advance rule, permission bundle, or business formula changed.

## 14. Milestone 12 — Daily Operations: Salary Payments

The Daily Work area now also provides native-backed salary payments:

- owners, admins, and managers with `SALARY_PAYMENT` permission can record wages actually paid to an active employee; cashiers and operators remain denied;
- the focused form accepts the employee, Cash/Bank payment account, exact PKR amount, business date, and optional notes;
- a successful payment atomically reduces the selected Cash/Bank balance and increases the employee's Salary expense account through the existing `SalaryPaymentService`;
- the employee's optional reference salary remains profile/planning information and is neither copied into the payment nor treated as an amount owed;
- inactive or mismatched employee accounts, invalid payment accounts, non-positive amounts, closed history, expired sessions, and insufficient permission are rejected without posting;
- each successful payment receives durable actor attribution and a security-audit entry;
- the authoritative financial overview refreshes the period salary-payment total for full-financial roles;
- packaged device evidence covers the React form through the bridge to canonical Room financial movements and actor audit.

The runtime now advertises forty implemented actions: `command.recordSalaryPayment` joins the prior thirty-nine. The React handshake list was also aligned with the already-live reauthentication and Opening State commands, so it now asserts all forty current runtime actions. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, salary direction, salary-payable/accrual rule, permission bundle, or business formula changed.

## 15. Milestone 13 — Daily Operations: Spoilage

The Daily Work area now provides native-backed spoilage/wastage recording:

- owners, admins, managers, and operators with `PRODUCTION_AND_SPOILAGE` permission can record a physical stock loss; cashiers remain denied;
- the focused form accepts the active product, exact quantity lost, plain reason, business date, and optional notes, and displays current saved stock plus recent spoilage history;
- the React form never asks for or invents a loss cost/rate; the existing native inventory and costing records remain authoritative;
- a successful entry atomically creates the canonical `SPOILAGE` event, reason/detail row, and negative inventory movement through `SpoilageService`;
- unknown, archived, non-inventory-tracked, non-positive, over-stock, closed-history, expired-session, and insufficient-permission requests are rejected without posting;
- each successful entry receives durable actor attribution and a security-audit entry;
- the screen awaits the command and refreshes native dashboard stock and spoilage history before showing the saved result;
- packaged device evidence covers the React form through the bridge to canonical Room event/detail/inventory rows, actor audit, stock refresh, and visible history refresh.

The runtime now advertises forty-two implemented actions: `query.spoilageHistory` and `command.recordSpoilage` join the prior forty. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, stock rule, spoilage valuation rule, permission bundle, or business formula changed.

## 16. Milestone 14 — Deliveries / Wholesale

The active app now provides a separate native-backed Deliveries area:

- all current application roles receive only the existing `DELIVERY` permission boundary; React visibility does not grant authority;
- staff can create a pending delivery for an active customer and product with exact quantity, business date, optional route, notes, and optional originating sale record number;
- the active list renders native customer/product names, quantity/unit, route, date, sale link, and latest status instead of keeping a second local delivery list;
- only a pending delivery may append one terminal `delivered` or `cancelled` status, preserving the earlier pending row and any completion/cancellation note;
- completed or cancelled deliveries can be archived from the active list without deleting the delivery or status history;
- an unlinked delivery can later reference one matching canonical `SALE` event; product and credit-customer mismatches are rejected, and linking never creates revenue, stock, cash, or customer-balance movements;
- create, transition, and archive reject protected closed business dates, while missing/inactive records, invalid quantities/statuses, duplicate links, expired sessions, and direct unauthenticated calls fail without partial writes;
- each successful delivery command writes a verified actor security-audit entry;
- awaited mutations refresh the authoritative delivery list, and packaged physical-device evidence covers create, status transition, archive, Room persistence, and visible screen refresh;
- the bridge handler, command service, action adapter, create form, list, and workspace are separate focused files rather than growing the Daily Operations modules.

The runtime now advertises forty-seven implemented actions: `query.deliveries` and the four delivery commands join the prior forty-two. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, sale formula, revenue rule, stock movement, permission bundle, or frozen business formula changed.

## 17. Milestone 15 — Customer and Supplier Ledgers

The active app now provides a permission-aware Accounts area:

- owners, admins, and managers with `VIEW_FULL_FINANCIAL` can browse customer and supplier balances; cashiers and operators remain denied by Kotlin even if they call the bridge directly;
- the balance list uses `query.financialOverview`, presents “customer owes shop,” “shop owes supplier,” and advance states in plain language, and performs no money calculation beyond formatting and sign explanation;
- selecting an account loads `query.accountLedger` on demand, with event movements, native running balance after each entry, correction links, superseded markers, dates, notes, and the canonical current balance;
- internal `Customer: ` and `Vendor: ` prefixes remain in persistence but are removed from shop-facing overview and ledger names;
- inactive customers and suppliers retain readable balances and history, while new payments remain disabled because operational references include active parties only;
- the existing native-backed supplier/customer Payment form is reused rather than copied, preselects the chosen account, and reloads that ledger after a successful payment;
- the ledger service derives entries only from canonical `financial_log` rows and native correction relationships; React never rebuilds a balance or posts an accounting movement;
- focused physical-device evidence covers exact customer and supplier movements/running balances, inactive-history retention, permission and record failures, runtime advertisement, packaged ledger display, and contextual customer payment posting.

The runtime now advertises forty-eight implemented actions: `query.accountLedger` joins the prior forty-seven. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, account direction, payment rule, permission bundle, or frozen business formula changed.

## 18. Milestone 16 — History and Corrections

The active app now provides a permission-aware History area:

- users with operational-view permission can search the canonical event history by type, ID, note, or verified staff name and open event detail on demand;
- event detail shows native inventory movements, role-filtered money movements, verified actor attribution, correction links, and whether a record is current or replaced;
- correction controls are available only to roles with native `CORRECTION` permission, and direct unauthorized bridge calls remain rejected by Kotlin;
- corrections append a typed replacement event and only its native-calculated difference; no existing event, inventory movement, or money movement is edited or deleted;
- the form requests the complete corrected truth, fixes the correction to the original business date, and warns the user not to enter only the difference;
- purchase, production, sale, monetary, and spoilage correction families use exact contracted fields and native validation, including impossible-stock rejection;
- only the latest event in a correction chain may be corrected, preventing competing branches while preserving every earlier version;
- corrections on closed business dates remain blocked, and every successful bridge correction records a verified event actor plus security-audit entry;
- internal customer, supplier, employee, expense, and salary account prefixes are removed from shop-facing movement names without changing persisted account identity;
- awaited correction commands refresh authoritative History data, and packaged physical-device evidence covers History display, a PKR expense correction, linked replacement visibility, Room persistence, and staff attribution.

The runtime now advertises fifty-one implemented actions: `query.eventHistory`, `query.eventDetail`, and `command.correctEvent` join the prior forty-eight. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, accounting formula, stock rule, correction formula, permission bundle, or frozen business contract changed.

## 19. Milestone 17 — Cash Count and Daily Close

The active app now provides a permission-aware Cash Close area:

- owners, admins, managers, and cashiers with `CASH_COUNT_AND_CLOSE` can record a physical drawer count and close a business date; operators remain denied by Kotlin;
- the screen selects only the canonical physical `Cash` account and explicitly excludes bank and mobile-wallet balances;
- staff enter the independently counted notes and coins before the app reveals expected cash and describes the difference as exact, short, or extra;
- every count appends a physical reconciliation record and verified security-audit entry but never creates an event, financial movement, or artificial balancing entry;
- closing requires the latest count for that date, requires a plain explanation for every non-zero variance, and appends a verified close action instead of changing prior records;
- a current close protects that date and earlier business dates from posting, attendance, delivery changes, corrections, and further cash counts through the existing native rules;
- reopening is visible only to owners/admins with `REOPEN_DAY`, requires a reason and fresh passphrase authentication, and appends a verified reopen action plus security audit;
- a reopened date requires a new physical count taken after the reopen before it can be closed again;
- the archive retains every count, close, reopen, superseding relationship, reason, timestamp, and verified actor;
- packaged physical-device evidence covers count, comparison, variance explanation, close, visible lock, passphrase-protected reopen, Room persistence, and actor attribution.

The runtime now advertises fifty-five implemented actions: `query.reconciliationArchive`, `command.recordPhysicalCashCount`, `command.closeBusinessDate`, and `command.reopenBusinessDate` join the prior fifty-one. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, cash-balance formula, variance rule, date-lock rule, role bundle, or frozen business contract changed.

## 20. Milestone 18 — Dashboard and Reports

The active app now provides a permission-aware Dashboard area:

- every signed-in role with `VIEW_OPERATIONAL` can choose an inclusive From/Through period and view canonical current stock plus recorded purchase, sale, and production quantities;
- owners, admins, managers, and cashiers with `SALE_AND_COLLECTION` can also see period sales, customer collections, and current payment-account positions;
- only owners, admins, and managers with `VIEW_FULL_FINANCIAL` can see customer/supplier positions, purchase/expense/salary/payment totals, product cost, margins, spoilage cost, abnormal loss, and the estimated recorded result;
- date inputs are converted once to the contract's end-exclusive range and are rejected when reversed or invalid;
- React renders the native dashboard, cashier, financial, and profitability DTOs without summing accounts, deriving cost, calculating margin, or inventing a result;
- the overall figure is deliberately labelled “Estimated result from recorded data,” not net profit or take-home money, and is described as separate from cash and bank balances;
- missing opening cost, stale yogurt conversion policy, unsupported stock flow, and partial coverage are translated into plain shop language; unavailable cost and result values remain unavailable rather than becoming zero;
- the screen discloses that the report is a management estimate using saved product costs and that the current scope is recorded cost only, with no lower-selling-price/NRV adjustment;
- current stock and current account positions are explicitly labelled as current even when the selected activity period is historical;
- the former synthetic prototype Reports view remains inactive and is not used as business truth.

The runtime now advertises fifty-six implemented actions: `query.profitabilityReport` joins the prior fifty-five. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, costing formula, reporting basis, valuation scope, permission bundle, or frozen business contract changed.

## 21. Milestone 19 — Costing and Planning Controls

The active app now provides a permission-aware Cost & Plans area with four focused workspaces:

- Plans & Targets creates or revises weekly Milk purchase plans, monthly operating-cost plans, and separate weekly customer-collection targets; changing a plan archives the prior version, and copying requires a real earlier plan;
- Milk purchase plans submit liters only. Kotlin snapshots the current supplier/retail rates and calculates planned buying amount and sales target; React neither copies those rates nor performs the multiplication;
- monthly operating plans submit their exact PKR amount, while every saved plan explicitly states that it does not post a purchase, expense, sale, collection, or payment;
- Yogurt Cost records the five approved conversion-cost components, normal production capacity, review reason, and optional notes; Kotlin validates component coverage, fixed-overhead capacity, material-change review reason, versioning, actor, and the final standard cost per kg;
- Opening Stock Cost attaches documented history or an explained owner estimate to positive opening Milk/Yogurt quantities only; it never creates an artificial purchase or opening event, and corrections preserve the prior basis;
- Yield & Loss Review requires at least 30 owner-selected representative batches, while Kotlin enforces at least 28 observed days and calculates the median/deviation alert limits;
- a yield warning remains a warning. It never posts a loss; an owner/admin must separately review the batch and record either ordinary variation or an identified abnormal loss with quantity and reason;
- the four cost-source commands reauthenticate the owner/admin before submission, while Kotlin remains authoritative for recent-authentication time, permission, validation, versioning, and actor attribution;
- all seven successful commands receive durable security-audit entries, and both queries remain unavailable to roles without `MANAGE_BUSINESS_SETTINGS`;
- the React work is split into planning, costing policy, opening basis, yield/loss, bridge-action, model, and presentation modules; no handwritten file in this milestone exceeds 115 lines.

The runtime now advertises sixty-five implemented actions: `query.costingManagement`, `query.planning`, `command.upsertBudget`, `command.copyPreviousBudget`, `command.upsertCollectionsTarget`, `command.approveCostingPolicy`, `command.recordOpeningCostBasis`, `command.approveYieldPolicy`, and `command.recordProductionLossAssessment` join the prior fifty-six. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, costing formula, yield formula, budget rule, permission bundle, or frozen business contract changed.

## 22. Milestone 20 — Staff Users and Security Audit

Completed on 2026-09-01.

- the Access area is visible to every signed-in user and provides a personal passphrase-change screen;
- owners and administrators can list app accounts, create permitted roles, optionally link one account to an existing employee profile, change roles/status, resume unfinished setup, and begin credential reset;
- owner/admin authority remains native-enforced: only owners control owner/admin accounts, ordinary administrators control only manager/cashier/operator accounts, a current account cannot disable itself, and the final active owner cannot be disabled or demoted;
- new enrollment and reset use a private device-handoff flow: the administrator creates the identity, then the staff member privately chooses the passphrase without the enrollment token being displayed or persisted by React;
- owner recovery is available from sign-in, rotates the saved recovery code after success, starts a verified owner session, and displays the replacement code once;
- Security History presents durable actor, target, outcome, reason, and non-secret details with failed/denied filters; passphrases, recovery codes, credential verifiers, and session handles remain excluded;
- every mutation is awaited and refreshes authorized native user/audit state; React never assigns authority or accepts typed actor identity;
- native bridge parsing is strict, duplicate login and employee-link conflicts are explicit, missing linked employees/users are explicit, and passphrase character buffers are cleared after use;
- account/security UI, bridge actions, presentation helpers, native handler/service/JSON code, and test coverage remain split into focused modules.

The runtime now advertises seventy-five implemented actions: `query.users`, `query.securityAudit`, `command.auth.changeOwnPassphrase`, `command.auth.recoverOwner`, `command.auth.beginUserEnrollment`, `command.auth.resumeUserEnrollment`, `command.auth.completeEnrollment`, `command.auth.changeUserRole`, `command.auth.setUserEnabled`, and `command.auth.beginCredentialReset` join the prior sixty-five. Contract version 2 remains at 78 operations with the same manifest hash. No Room schema, role/permission bundle, credential policy, recovery rule, accounting formula, or frozen business contract changed.

## 23. Milestone 21 — Encrypted Backup and Restore

Completed on 2026-09-01.

- owners and administrators with `CREATE_EXPORT_BACKUP` can create a portable encrypted backup after fresh account-passphrase confirmation;
- only owners with `RESTORE_BACKUP` can inspect and restore a portable backup, again after fresh account-passphrase confirmation;
- the backup uses a separate user-chosen passphrase of at least 15 characters, while React clears its form state after each attempt and never receives file bytes, filesystem paths, Android content URIs, or retained backup passphrases;
- Android owns the create/open document pickers, and cancellation leaves current data unchanged;
- export retains the existing portable format-2 Argon2id-derived AES-256-GCM protection, authenticated integrity checks, app-user/security portability, and transient-session/lockout exclusions;
- inspection rejects wrong passphrases, altered files, incompatible backups, and security-free backups before any current data is changed;
- a successful inspection returns only safe metadata and a short-lived, one-use, memory-only restore ticket bound to the inspecting user, session, and selected document;
- the final restore screen plainly states that restore is a full replacement rather than a merge and requires an explicit acknowledgement;
- authenticated restore creates the existing device-local safety snapshot, atomically replaces the active database from the inspected portable backup, rebuilds protected triggers/projections, invalidates every session, clears every pending restore ticket, and returns to sign-in;
- export and restore append security-audit entries without falsely attaching the restore action to a coincidentally matching user ID from the restored file;
- the screen explains that backup is a protected snapshot for recovery or transfer, not live synchronization between two writable phones;
- Google Drive scheduling, retention, retry, and owner-phone viewing remain deferred to their recorded Phase 10 design gate.

The runtime now implements and advertises all seventy-eight frozen operations. `system.exportEncryptedBackup`, `system.inspectEncryptedBackup`, and `system.restoreInspectedBackup` join the prior seventy-five. Contract version 2 remains unchanged at 78 operations with the same manifest hash. No Room schema, accounting formula, role/permission bundle, cryptographic format, or frozen business contract changed.

## 24. Safety and business boundaries

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
- Opening state is optional and one-time; zero balances create no event.
- Initial opening state is rejected after any real business event, including on upgraded installations with history.
- Opening corrections append a new full snapshot and delta postings; they never overwrite or delete the original event.
- React never decides whether recent authentication is valid; the native session clock and credential verifier remain authoritative.
- React never calculates purchase quality, payable, stock, or supplier balance; it renders the native result.
- React never calculates sale quantity/value conversion, delivery charge, total, stock, cash, or customer balance; it renders the native result.
- React never calculates production conversion, yield, cost, or loss; it records the physical inputs/outputs and renders the committed native result.
- React never calculates expense postings, balances, or daily totals; it submits exact inputs and renders native results.
- React never calculates supplier payable, customer receivable, payment-account movements, or payment totals; it submits exact inputs and renders native results.
- Supplier/customer payments intentionally allow advances; the native ledgers remain the authority for the resulting signed balance.
- React does not sum ledger entries or calculate running/current balances; it displays the movement and balance-after values returned by Kotlin and only translates their sign into shop-facing words.
- Inactive customer and supplier histories remain readable, but only active operational references can start a new payment.
- React never calculates salary due, copies the reference salary into a payment, or calculates Salary/Cash movements; it records only the amount actually paid and renders the native result.
- A salary payment records paid salary cost. It does not create or settle a separate salary-payable balance.
- React never calculates spoilage cost or changes stock locally; it submits the lost quantity and reason, then renders native stock/history results.
- Spoilage is limited to active inventory-tracked products and cannot reduce stock below zero.
- A delivery is a logistical record. Creating, linking, completing, cancelling, or archiving it never records a sale or payment.
- Sale linkage accepts only one matching canonical sale and preserves the sale as the sole source of revenue, stock, cash, and customer-balance effects.
- Delivery status changes append history; archive hides a record from the active list without deleting its identity or status trail.
- A physical cash count records what is actually in the drawer; it never changes the canonical Cash balance or posts a balancing entry.
- Non-zero cash variance always requires an explanation before close, but the explanation does not convert the variance into income or expense.
- Close/reopen identity comes only from the native session; reopening requires native recent authentication and cannot be authorized by React.
- A closed business date returns `day_closed` before posting, and the database close-day trigger remains the final guard.
- React never sums report fields, derives COGS, margin, spoilage cost, abnormal loss, or an overall result; it displays native read-model values and translates stable coverage codes only.
- A missing report cost or result remains visibly unavailable; it is never presented as zero.
- The estimated recorded result is a cost-based management view, not cash, bank balance, statutory net income, or owner take-home money.
- Plans and targets are versioned comparison records; they never post business transactions or alter actual results.
- React never derives Milk plan cost/sales targets, Yogurt conversion cost, yield limits, or abnormal-loss value; it renders native results.
- Opening stock cost applies only to the saved one-time opening event and never fabricates an opening or purchase record.
- Yield alerts never create a loss assessment. Abnormal loss exists only after an authenticated human review is saved.
- Backup files and passphrases remain inside Android-owned boundaries; React receives only safe metadata and an opaque restore ticket.
- Inspection never changes active shop data. Restore is an explicit full replacement, never a merge or synchronization mechanism.
- A successful restore invalidates all sessions, including the restoring owner's session, so every user must sign in against the restored security state.
- The old prototype view model remains inactive and is not imported by the packaged app.
- No Room schema, frozen business formula, permission bundle, or canonical bridge endpoint changed. The additive `TransactionReferencesDto.expenseCategories` field is the documented contract-v2 amendment.

## 25. Structural refactor checkpoint

Before starting Supplier Profiles, the active hybrid path was reorganized without changing its public behavior:

- the 733-line Phase 8 shell was split into authentication, workspace navigation, Product, Customer, Supplier, Employee Profile, Attendance, Business Settings, Expense Categories, Opening State, Daily Work, Deliveries, bridge-action, shared UI, and shared model modules; Daily Work orchestration and its operation forms remain separate, while Deliveries has separate workspace, create-form, list, and action-adapter modules and the coordinating shell remains compact;
- the former 353-line multi-domain bridge-handler container was removed. Authentication, Master Data, Employees/Attendance, Settings/Opening State, and Daily Operations now live in focused handlers; purchase orchestration and actor audit are further separated into a focused command service;
- the 660-line Phase 5 read-model service remains the stable caller-facing façade but is now 47 lines, delegating to focused identity/master-data, operational, financial, costing/planning, and security readers;
- generated contract projections and inactive inherited prototype screens were deliberately left alone;
- future handwritten files should be reviewed for separation when they approach roughly 300–400 lines, with responsibility and cohesion taking priority over an arbitrary number.

The structural checkpoint changed no Room schema, permission rule, accounting formula, or existing user workflow. The two growing Daily Operations Kotlin files remain cohesive at 343 and 342 lines, inside the 300–400-line review band. Delivery, ledger, history/correction, cash-close, reporting, costing/planning, security administration, and backup/restore remain isolated from Daily Operations. Backup/restore adds a focused native handler, React action adapter, and workspace while reusing the existing native backup manager. The focused encrypted-backup suite is **5/5 PASS**. The complete suite is now **211/211 PASS**.

## 26. Verification checkpoint

Physical device: `SM-A065F`.

- Phase 8 focused product-management suite — **3/3 PASS**;
- Phase 8 focused Customer Profiles suite — **3/3 PASS**;
- Phase 8 focused Supplier Profiles suite — **3/3 PASS**;
- Phase 8 focused Employees/Attendance suite — **4/4 PASS**;
- Phase 8 focused Settings/Expense Categories suite — **4/4 PASS**;
- Phase 8 focused Opening State suite — **4/4 PASS**;
- Phase 8 focused Daily Operations purchase suite — **4/4 PASS**;
- Phase 8 focused Daily Operations sales suite — **4/4 PASS**;
- Phase 8 focused Daily Operations production suite — **4/4 PASS**;
- Phase 8 focused Daily Operations expense suite — **4/4 PASS**;
- Phase 8 focused Daily Operations payment suite — **4/4 PASS**;
- Phase 8 focused Daily Operations salary-payment suite — **4/4 PASS**;
- Phase 8 focused Daily Operations spoilage suite — **4/4 PASS**;
- Phase 8 focused Deliveries/wholesale suite — **4/4 PASS**;
- Phase 8 focused Customer/Supplier Ledgers suite — **4/4 PASS**;
- Phase 8 focused History/Corrections suite — **4/4 PASS**;
- Phase 8 focused Cash Count/Daily Close suite — **4/4 PASS**;
- Phase 8 focused Dashboard/Reports suite — **4/4 PASS**;
- Phase 8 focused Costing/Planning suite — **5/5 PASS**;
- Phase 8 focused Staff Users/Security Audit suite — **5/5 PASS**;
- Phase 8 focused Encrypted Backup/Restore suite — **5/5 PASS**;
- complete connected-device regression — **211/211 PASS**, 0 skipped, 0 failed, 0 errors;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript check and production Vite build — PASS;
- explicit web bundle packaging and Android asset gate — PASS;
- Kotlin and Android-test Kotlin compilation — PASS;
- bridge contract drift check — version 2, 78 operations, SHA-256 `AB374869603B9D773AD86B56EF801DA8C4493816C7301B05214AE596E33D0920` — PASS;
- frozen logic-spec and SQL hashes — unchanged.

The focused tests prove Product, Customer, Supplier, and Employee lifecycle; attendance access/revision history; editable PKR store identity; exact shop-wide rate persistence; expense-category lifecycle; opening-state record/correction; supplier milk purchases; sales; production; expenses; supplier payments; customer collections; salary payments; spoilage; delivery lifecycle; account ledgers; History/Corrections; Cash Count/Daily Close; Dashboard/Reports; Costing/Planning; Staff/Security; and Encrypted Backup/Restore with Room readback and durable actor audit. The final evidence covers the Android-owned file boundary, ciphertext confidentiality, wrong-passphrase/damaged-file/cancellation rejection, role enforcement before picker access, user/session-bound one-use tickets, current-data replacement, post-restore session invalidation, strict payloads, all 78 runtime actions, and the packaged create/inspect/confirm/restore experience.

## 27. Current limitations and next phase

Phase 8 is complete. Every frozen bridge operation is implemented and advertised by the active runtime, and every retained Phase 8 screen family now executes through native-backed queries and commands with packaged physical-device evidence.

Phase 8 remains device-local. Multiple named users and native role enforcement are supported on one installation. Periodic encrypted Google Drive/cloud backup is recorded for a later Phase 10 design gate and Phase 11 hardening; optional read-only owner-phone snapshot viewing will be decided there. Phase 8 must not improvise networking or present backup as live synchronization.

The next phase is **Phase 9 — Runtime / Navigation / Device Integration**. Preserve the now-complete 78-action product while proving WebView lifecycle/recreation behavior, protected Back/logout navigation, targeted refresh behavior, keyboard/system-bar/inset handling, and fixed application navigation on physical devices. Periodic cloud backup remains outside Phase 9.

## Plain English

You can now create an encrypted copy of the whole shop from the Backup screen and save it wherever Android allows. Before restoring, the app checks the file and shows a final warning; restore replaces the current copy, makes a local safety snapshot, and signs everyone out. It is a recovery/transfer tool, not automatic sharing between phones. With this final screen complete, Phase 8 is finished and the next work is making navigation, phone lifecycle behavior, keyboard spacing, and system-bar handling production-ready in Phase 9.
