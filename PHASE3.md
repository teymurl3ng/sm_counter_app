# Phase 3 — Native Phase 18 Capability Expansion

**Status: COMPLETE — reconciled 2026-08-28; all approved native expansion, security, and authoritative Milk/Yogurt costing work is implemented and device-verified**

Current cross-phase status and early-work credits are defined in `ROADMAP_RECONCILIATION_2026-08-27.md`. React/bridge delivery remains in its original later phases and is not a Phase 3 completion gate.

## Immutable baseline verification

- Logic specification SHA-256: `45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`
- SQL blueprint SHA-256: `fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

Both frozen files remain unchanged in this checkpoint.

## Locked implementation rule: native reuse / no duplicate computation

For every requested AI Studio capability, Phase 3 first asks:

1. **DIRECT REUSE** — Phase 18 already has the authoritative service/calculation. Reuse it unchanged.
2. **NATIVE DERIVATION** — existing authoritative events/logs/read models contain the answer. Add a native query/read model, not a new business formula.
3. **GENUINELY NEW DOMAIN CAPABILITY** — existing state/rules are insufficient. Define persistence and semantics, then obtain explicit approval before changing the frozen spec/SQL.

React will not become a second accounting engine.

---

# Phase 3 work completed in this checkpoint

## Account-specific customer/vendor ledgers — implemented natively

Classification: **NATIVE DERIVATION**.

No new accounting formula or SQL table was required. The ledger uses the exact `financial_log` movements already written by existing transaction and correction services.

Added:

- `data/AccountLedgerReadModels.kt`
- `FinancialLogDao.findAccountLedger(accountId)`
- `domain/AccountLedger.kt`
- `Phase3NativeExpansionTest.kt`

Correction behavior is explicit:

- the original ledger movement remains visible;
- a correction contributes its real delta movement;
- the original is marked `isSuperseded`;
- the correction is marked `isCorrection` and carries `correctsEventId`;
- running balance is derived only by summing canonical financial movements;
- current balance still comes from the existing canonical `account_balances` read model.

This avoids the React layer reconstructing account history.

---

# Native reuse map for the approved capability set

| Desired capability | Phase 18 source to reuse | Classification | Phase 3 action |
|---|---|---|---|
| Milk intake / TS computation | `VendorPurchaseCalculator`, `VendorPurchaseService` | DIRECT REUSE | Keep formulas unchanged. New supplier/profile UI must call this service. |
| Retail/wholesale/credit sales | `SaleService` | DIRECT REUSE | Keep quantity/value/rate/surcharge/account semantics. |
| Customer collection | `CreditCollectionService` | DIRECT REUSE | Reuse. |
| Supplier payment | `VendorPaymentService` | DIRECT REUSE | Reuse. |
| Expense transaction | `ExpenseService` | DIRECT REUSE for current categories | Custom category support requires a spec rule change because §9 currently enumerates categories. |
| Salary payment | `SalaryPaymentService` | DIRECT REUSE | Employee master fields must not replace salary transaction semantics. |
| Yogurt production | `YogurtProductionService` | DIRECT REUSE | Keep measured milk/yogurt movement semantics. |
| Corrections | `CorrectionService` | DIRECT REUSE / EXTEND | Existing restated-truth + delta model must be reused for any new event type. |
| Current stock | `current_stock` / `ReadModelDao` | NATIVE DERIVATION | Reuse; never set stock directly. |
| Customer/vendor balances | `account_balances` | NATIVE DERIVATION | Reuse. |
| Customer/vendor ledger | `financial_log` + corrections | NATIVE DERIVATION | **Implemented in this checkpoint.** |
| Budgets/targets | `BudgetManagementService` | DIRECT REUSE | No new budget math. |
| Existing analytics | `AnalyticsService` | DIRECT REUSE / EXTEND | Expand native queries, do not reproduce formulas in React. |
| Product management | `products` identity + current stock | NEW MASTER DATA | Full UI semantics need persistent product metadata/archive rules. |
| Customer profiles | existing Customer account identity | NEW PROFILE DATA | Add 1:1 native profile keyed by `account_id`. |
| Supplier profiles | existing Vendor account identity | NEW PROFILE DATA | Add 1:1 profile. Existing purchase rate remains global unless separately approved. |
| Employee profiles | existing Salary account identity | NEW PROFILE DATA | Add 1:1 profile; salary payments remain events. |
| Attendance | none | NEW OPERATIONAL DATA | Requires attendance persistence/service. |
| Delivery | sale/customer identity exists | NEW OPERATIONAL DATA | Requires explicit lifecycle semantics and sale linkage. |
| Spoilage/wastage | inventory log exists, event type absent | NEW EVENT TYPE | Requires schema/event/correction/report extension. |
| Counted physical cash | Cash balance exists | NEW RECONCILIATION DATA | Expected cash derives from existing ledger; only the physical count record is new. |
| Daily close/lock | reports/events exist | NEW CONTROL/ARCHIVE DATA | Requires close/reopen/correction semantics. |
| Store identity/currency | app preferences exist, DB text business config absent | NEW BUSINESS CONFIG | Decide backup scope and native persistence. |
| Opening cash/stock | current logs derive balances/stock | NEW OPENING EVENT SEMANTICS | Must be auditable events, not direct state assignment. |
| Custom expense categories | Operating Expense accounts exist | BUSINESS RULE CHANGE | Current frozen spec §9 explicitly enumerates categories. Approval required. |
| Authenticated users / authorization | schema-v6 close actions retain unverified actor assertions; no user/session subsystem exists | NEW SECURITY DOMAIN | Define native users, credentials, roles/permissions, sessions, employee links, recovery, audit identity, migration, and backup semantics before implementation. |
| COGS/profit | purchase/sale/production facts exist | COSTING MODEL REQUIRED | Existing data is useful but no authoritative costing rule exists. Do not invent one. |

---

# Proposed native persistence expansion — AUTHORIZED AND APPLIED AS SCHEMA VERSION 4

The following proposal was explicitly authorized by the user on 2026-08-27 using the recommended semantics. It is now implemented as the additive Room migration `MIGRATION_3_4`. The original frozen logic specification and SQL blueprint remain byte-for-byte unchanged.

## 1. Profile/master-data tables

Prefer 1:1 profile tables keyed by the existing canonical account/product IDs so historical accounting identity remains untouched.

### `customer_profiles`

- `account_id` → existing Customer account PK/FK
- phone
- area/route
- address/notes
- credit limit
- default daily quota
- active flag
- created timestamp

### `vendor_profiles`

- `account_id` → existing Vendor account
- phone
- address/location
- notes
- active flag
- created timestamp

**Important:** a supplier-specific rate is not proposed as an accounting override. The existing `VendorPurchaseCalculator` continues to use the canonical business settings unless a separate rule is explicitly approved.

### `employee_profiles`

- `account_id` → existing Salary account
- role
- phone
- reference/master salary amount
- salary period/type
- join date
- active flag

Salary payment remains the existing `SALARY_PAYMENT` event.

### Product metadata

The existing `products` row remains canonical product identity. Additional metadata is proposed separately rather than changing inventory arithmetic:

- default sale rate
- inventory tracking flag
- archived/active state
- optional presentation metadata

A cost-rate field must not become authoritative COGS until the costing model is approved.

## 2. Attendance

New attendance rows keyed to employee account and business date with status `present`, `absent`, or `half_day`, plus notes/timestamp.

No automatic salary calculation is proposed.

## 3. Delivery operations

A delivery record should reference:

- customer account;
- product;
- quantity;
- route;
- business date;
- status;
- delivered timestamp;
- linked `SALE` event when financial recognition occurs.

Revenue remains entirely in `SaleService`; delivery must never create duplicate financial movement.

## 4. Spoilage/wastage

Recommended extension:

- new `SPOILAGE` event type;
- negative `inventory_log` movement;
- structured reason/notes;
- correction support using the existing restated-truth + delta pattern;
- reporting support.

The loss valuation basis is held until the costing rule is approved. Inventory quantity reduction itself is independent of COGS valuation.

## 5. Physical cash counts

Persist business date, Cash account identity, counted amount, timestamp and optional note.

Expected cash is **not stored as a second accounting value**; it is derived from the canonical Cash account balance/read model. Variance = physical count − native expected balance is a read calculation.

## 6. Daily close

Persist close identity, business date, close timestamp, counted cash reference/amount, notes, status/revision relationship.

The close must not copy business transactions into a second ledger. Its snapshot/report fields should derive from existing native reads.

## 7. Business identity/opening state

Store identity/currency is configuration.

Opening cash and opening stock require explicit auditable opening-event semantics. They must not directly overwrite `account_balances` or `current_stock`.

## 8. Expense categories

Current Phase 18 already models each expense category as an `Operating Expense` account, which can naturally support persistence of additional categories.

However, the frozen logic specification §9 explicitly enumerates the allowed category list and `ExpenseService` enforces it. Making categories dynamic is therefore a **business-spec amendment**, even though no new accounting formula is needed.

---

# Approval gate resolution

Explicit authorization was received to implement the versioned native extensions below while preserving canonical product/account/event identity.

Approval should cover these principles:

1. New profile/operational tables may be added while preserving existing account/event identities.
2. `SPOILAGE` may be added as a new auditable inventory event type.
3. Delivery may be added as an operational table linked to `SALE`, with no duplicate revenue.
4. Physical cash counts and daily closes may be persisted as reconciliation/control records, not a second ledger.
5. Expense categories may become dynamic `Operating Expense` accounts instead of the frozen hard-coded list.
6. Product metadata/archive support may be added without allowing direct stock assignment.
7. At the schema-v4 checkpoint, opening state, daily-close locking semantics and COGS/costing remained separate design approvals before their business rules could be coded.

The SQL blueprint and frozen logic specification remain byte-for-byte unchanged. Store identity/opening state and daily-close controls were subsequently approved and implemented in schema versions 5 and 6. At that historical checkpoint, COGS/costing still required a separate future decision; it was later approved and delivered in schema version 8 as recorded below.

---

# Authorized schema version 4 implementation

## Additive persistence

Schema version 4 adds:

- `product_profiles` keyed by canonical `product_id`;
- `customer_profiles`, `vendor_profiles`, and `employee_profiles` keyed by canonical `account_id`;
- append-only `attendance_records` revisions using `supersedes_id` and archive state;
- `deliveries` plus append-only `delivery_status_history`;
- `spoilage_details` keyed by the canonical event ID;
- append-only `physical_cash_counts`;
- `expense_category_profiles` keyed by an `Operating Expense` account.

The migration backfills neutral profiles for all existing products, customers, vendors, employees, and seeded expense-category accounts. Room's exported version 4 schema is stored at `phase18/app/schemas/com.saaf.milkshop.data.MilkShopDatabase/4.json`.

## Native services and rules

- `ProductCatalogService` creates and edits product identity/metadata and archives products without deleting historical identity. It does not set stock.
- Customer, vendor, and employee profile services persist the approved optional profile fields. Existing management services create a neutral profile with each new canonical account.
- `AttendanceService` accepts `present`, `absent`, and `half_day`. Re-recording an employee/date archives the prior row and appends a revision.
- `DeliveryService` starts each delivery as `pending`, permits a single transition to `delivered` or `cancelled`, and retains status history. A delivery may link to one canonical `SALE`; it writes no event or financial movement and therefore cannot duplicate revenue.
- `SpoilageService` writes a `SPOILAGE` event and a negative canonical inventory movement. `CorrectionService` supports spoilage using the existing restated-truth event/detail plus inventory-delta rule. No monetary loss value is calculated.
- `PhysicalCashCountService` retains every count. Expected cash remains the canonical Cash account balance, and variance is derived as counted minus expected.
- `ExpenseCategoryManagementService` creates/archives account-backed categories. `ExpenseService` now validates against active persisted `Operating Expense` category profiles rather than the former hard-coded allow-list.
- Archived products and explicitly inactive customer, vendor, and employee profiles are rejected for new operations. Legacy accounts without profile rows remain active for backward compatibility, while historical settlement and correction paths remain available.
- Spoilage history hides superseded events by default, and cash reconciliation uses the canonical balance as of each count timestamp so historical variances do not drift after later postings.

## Backup/restore

At the version-4 checkpoint, backup schema metadata was version 4. Complete business backups included every new profile and operational table. Restore deletes/inserts rows in foreign-key-safe dependency order. Older schema-3 backups remain accepted; neutral profiles are backfilled when their new arrays are absent.

## Verification

Verified with Android Studio's bundled JDK, the installed Android SDK, Gradle 9.3.0, and connected device `SM-A065F`:

- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — PASS.
- Targeted original/new Phase 3 tests — 9/9 PASS.
- Phase 9 backup/restore tests — 3/3 PASS, including a complete version 4 extension-state round trip.
- Room schema 3→4 migration validation — PASS with identity/profile backfill assertions.
- Complete `:app:connectedDebugAndroidTest` regression suite — 80/80 PASS, 0 skipped, 0 failed.

Room 3.0.1's migration/testing graph resolved `serialization-json` 1.8.1 with `serialization-core` 1.7.3, causing an `AbstractMethodError` before migration execution. The runtime was aligned to the 1.8.1 serialization BOM; the migration test then ran and passed.

---

# Authorized schema version 5 implementation

The user subsequently approved editable store identity, fixed `PKR` currency, and optional one-time opening state for fresh/empty installations. This is implemented by additive migration `MIGRATION_4_5`; no frozen baseline file was edited.

## Store identity

- `store_profile` is a singleton native business record.
- Store name, phone, and address are editable; the store name is never hardcoded.
- Currency is persisted and database-validated as `PKR`.
- Migration does not seed a store name, so the user deliberately supplies it.

## One-time opening event series

- `OPENING_STATE` is an auditable canonical event type.
- The initial opening event is allowed only before the first business event and only after store identity is saved.
- It may establish product quantities plus Cash/Bank, Customer receivable, and Vendor payable balances.
- Canonical `inventory_log` and `financial_log` movements establish the derived stock and account balances; no read model is directly assigned.
- Only one initial opening batch is permitted. A zero-start shop skips opening state instead of writing a meaningless event.
- Corrections append a linked `OPENING_STATE` event. Detail rows contain the complete restated snapshot while ledger rows contain only the delta from the prior snapshot.
- Historical/archived identities remain correctable, consistent with the existing correction policy.

## Upgrade and backup safety

- Migration 4→5 creates empty `store_profile`, `opening_batches`, `opening_inventory_entries`, and `opening_account_entries` tables. It inserts no store identity, stock, balance, or event.
- Existing version-4 ledgers are preserved exactly and cannot receive an automatic opening state.
- Backup metadata is version 5. Complete business backup/restore includes store identity, opening batches, restated detail rows, canonical ledger movements, and the full correction chain.
- Schema-3 and schema-4 backups remain accepted; absent version-5 arrays restore as empty rather than being invented.

## Current verification

Verified with the connected `SM-A065F` device:

- Kotlin and Android-test Kotlin compilation — PASS.
- Focused opening-state, migration, and backup suite — 9/9 PASS.
- Migration 4→5 validation — PASS with a pre-existing event preserved and all new opening/store tables empty.
- Complete `:app:connectedDebugAndroidTest` regression suite — 85/85 PASS, 0 skipped, 0 failed.

---

# Authorized schema version 6 implementation

The user approved the recommended daily-close control policy after reviewing internal-control and accounting-system practices. It is implemented by additive migration `MIGRATION_5_6`; the frozen baseline files remain untouched.

## Append-only close control

- `daily_close_actions` records an append-only `close`/`reopen`/reclose chain per business date using `supersedes_action_id`.
- A close must reference the latest physical count for the canonical `Cash` account on that business date.
- A reclose must reference a new cash count recorded after the reopen action.
- Non-zero variance may be acknowledged but requires an explanation. It never creates an automatic ledger movement.
- Actor name and asserted role are retained. `owner` or `admin` is required for reopening; cashier, operator, manager, admin, or owner may perform an ordinary close.

## Posting lock

- A current close locks new canonical events dated on or before the closed date.
- An explicitly reopened date is a controlled exception while its current action remains `reopen`.
- SQLite triggers also block physical cash counts, attendance inserts/updates, delivery creation/updates, and delivery-status changes for locked dates.
- Original transactions are never edited or deleted. After reopening, the existing linked restated-truth plus delta correction services remain authoritative.

## Authorization boundary

At schema version 6 the application had no authenticated user/role subsystem. That schema therefore records actor identity/role assertions and enforces allowed role values, but it does not falsely claim cryptographic identity verification. Schema version 7 and the authenticated native shell now supply verified actors; the future bridge must preserve this boundary and prevent callers from self-asserting privileged roles.

## Upgrade and backup safety

- Migration 5→6 creates an empty close-action table and installs the lock triggers. It creates no close, count, event, balance, or stock movement.
- Existing version-5 data is preserved exactly.
- Backup metadata is version 6. Restore replays each date chronologically—count, close, reopen, new count, reclose—so integrity controls remain active during import.
- Older backups without close actions continue to restore with no invented close history.

## Current verification

Verified with the connected `SM-A065F` device:

- Kotlin and Android-test Kotlin compilation — PASS.
- Dedicated daily-close lifecycle/lock tests — 4/4 PASS.
- Migration tests through 5→6 — 3/3 PASS.
- Backup/restore tests — 5/5 PASS.
- Complete `:app:connectedDebugAndroidTest` regression suite — 91/91 PASS, 0 skipped, 0 failed.

---

# Approved authenticated-user capability — NATIVE FOUNDATION DELIVERED

The user approved application user accounts as a required part of the real product on 2026-08-27. The researched credential, session, recovery, permission, audit, migration, and backup model was subsequently approved and implemented as the schema-7 native foundation.

The researched contract in `PHASE3_USER_ACCOUNTS_DESIGN_PROPOSAL.md` was explicitly approved on 2026-08-27. Schema version 7, encrypted backup format 2, Android-Keystore snapshots, and the authenticated native UI are implemented and device-verified. The React/bridge integration is not yet delivered.

## Required native model

- Application users are security principals separate from employee/salary accounts.
- A user may optionally link one-to-one to an employee profile for display and operational attribution. Neither side is required to create the other automatically.
- Native code must authenticate credentials, create/expire sessions, resolve permissions, enforce every protected command, and retain a stable actor identifier in audit records.
- The UI and bridge must never supply an authoritative role. They carry a session/proof of authentication; Kotlin resolves the current user and permissions.
- Account deactivation/locking must revoke future access without deleting historical audit identity.
- The first installation needs an explicit first-owner bootstrap. Upgrades must not invent users, passwords, or privileged audit records.
- Backup/restore must define which account metadata is portable and how credential secrets are excluded or cryptographically protected. Plaintext credentials are prohibited.

## Decisions resolved by the approved schema implementation

1. credential method and secure-at-rest representation, including whether operator PINs are supported in addition to owner/admin credentials;
2. exact role/permission matrix (candidate operational roles already used by daily close are `owner`, `admin`, `manager`, `cashier`, and `operator`);
3. session lifetime, app restart/device-lock behavior, logout, failed-attempt throttling, and lockout;
4. owner/admin recovery and safeguards against removing or locking the last recoverable owner;
5. optional employee linkage and what profile data may be shown in audit history;
6. durable audit coverage and the transition from schema-v6 asserted close actors to verified user IDs;
7. migration and backup/restore policy, especially treatment of credential verifiers and active sessions.

## Cross-phase delivery gates

- **Phase 3:** approve and implement the native identity, credential, role/permission, session, recovery, employee-link, audit, migration, and backup model.
- **Phase 4:** prove authentication, authorization, deactivation/lockout, session, recovery, audit, migration, and backup behavior, including privilege-bypass attempts.
- **Phase 5:** expose only the native user/session/permission and audit read models required by the UI.
- **Phase 6:** define login/logout/session/user-administration contracts and permission errors; accept no caller-asserted role or actor identity.
- **Phases 7–10:** add the authenticated shell, user management, role-aware navigation, and protected feature wiring, while keeping native checks authoritative.
- **Phases 11–16:** harden credential storage, recovery, concurrency and backup; run role-by-role device and shopkeeper QA; ship without default credentials and with a deliberate first-owner setup.

---

# Authorized schema version 7 native security foundation

The user explicitly approved `PHASE3_USER_ACCOUNTS_DESIGN_PROPOSAL.md` on 2026-08-27. The additive Room migration `MIGRATION_6_7` and first native security slice are implemented without modifying the frozen baseline.

Implemented:

- separate `app_users` security principals with optional employee links;
- Argon2id credential verifiers at the OWASP 19 MiB / 2 iteration / parallelism 1 minimum;
- fixed owner/admin/manager/cashier/operator permission bundles;
- deliberate one-time owner bootstrap with no default or migrated user;
- owner-controlled account creation, role changes, enable/disable, credential-reset initiation, and final-owner protection;
- admins limited to manager/cashier/operator administration;
- hashed, single-use 128-bit owner recovery codes with rotation;
- persistent failed-attempt throttling state and generic authentication failures;
- native memory-only sessions with inactivity, overall, background, and recent-reauthentication limits;
- append-only security audit entries and event-actor attribution storage;
- verified daily-close/reopen user IDs once any account exists, while older history remains legacy/unverified;
- Android Auto Backup/data-transfer exclusions for database, credentials, sessions, internal files, and custom snapshots;
- native first-owner bootstrap, login, logout, recovery, passphrase change, reauthentication, and lifecycle-aware protected-content obscuring;
- private staff enrollment and reset, where administrators assign identity/role but never the staff member's credential;
- owner/admin user management with final-owner and privileged-role safeguards;
- app-owned session propagation, role-aware native navigation, and permission enforcement at protected transaction, management, budget, settings, correction, and backup command boundaries.

Migration 6→7 creates empty security tables and a nullable daily-close actor-user link. It creates no owner, credential, recovery code, session, audit row, or retroactive actor mapping. Existing installations must deliberately complete first-owner setup after upgrade.

Legacy format-1 `.milkbackup` remains readable checksummed JSON and excludes schema-7 security data. Portable format 2 is implemented as an authenticated encrypted envelope: an owner-supplied backup passphrase is normalized and processed with Argon2id (19 MiB, two iterations, parallelism one) to derive an AES-256-GCM key with a fresh salt and nonce. Security identities, credential/recovery verifiers, audit records, event attribution, and verified daily-close actor IDs exist only inside the encrypted payload. Active sessions are not serialized, and restore invalidates supplied in-memory sessions. Wrong passphrases and altered ciphertext are rejected before database mutation. Format-1 imports remain supported.

Automatic app-private snapshots now use AES-256-GCM under a non-exportable random key generated and held by Android Keystore. The authenticated payload contains user/security state but never active sessions or transient failed-login counters, failure timestamps, or lockout deadlines. Local files can be read only while the same installation's Keystore key remains available; security-bearing local restore requires a pre-owner-setup destination and invalidates all supplied sessions.

Schema-7 checkpoint verification on connected device `SM-A065F`:

- Kotlin and Android-test Kotlin compilation — PASS;
- targeted user-security, migration, and authenticated-shell suite — 11/11 PASS;
- focused backup/restore suite — 8/8 PASS;
- complete connected-device regression — 103/103 PASS, 0 skipped, 0 failed.

## Phase 3 completion — schema-8 costing foundation

The user approved `PHASE3_COGS_PROFITABILITY_DECISION_PROPOSAL.md`; `PHASE3_COGS_IMPLEMENTATION_DESIGN.md` records the implemented contract. Room schema version 8 now provides append-only costing-policy, opening-cost-basis, yield-policy, and production-loss source truth plus deterministic rebuildable projections.

The native layer implements:

- moving-weighted-average Milk acquisition cost with integer-paisa residual handling;
- effective-dated, component-backed monthly Yogurt conversion cost;
- exact-quantity, evidence-labelled opening cost bases without fabricated upgrade entries;
- median/MAD yield calibration after 30 accepted batches spanning at least 28 days;
- review alerts that never invent an abnormal loss, plus explicit authorized loss assessments;
- correction-leaf replay for purchase, production, sale, spoilage, and abnormal-loss costing;
- per-product sales, COGS, gross profit/margin, spoilage, abnormal loss, and recorded operating-result reads;
- null/withheld results and stable coverage codes where cost is not supportable;
- encrypted source-truth backup/restore with derived projection rebuild.

Formal lower-of-cost/NRV write-down/reversal support is deliberately deferred under the approved materiality trigger. Reports identify themselves as `cost_based_management` / `cost_only_no_nrv`; they do not claim statutory profit or IFRS compliance. General merchandise acquisition costing is also outside this first slice.

Verification on connected device `SM-A065F`:

- Kotlin and Android-test Kotlin compilation — PASS;
- focused costing plus backup/restore suite — 14/14 PASS;
- complete connected-device regression — 111/111 PASS, 0 skipped, 0 failed;
- migration 7→8 creates no artificial cost source/opening-cost truth;
- frozen specification and SQL blueprint hashes remain unchanged.

The canonical bridge contract and React/hybrid UI integration remain required, but they belong to Phases 6–10 rather than Phase 3. No React/bridge wiring is part of this checkpoint; the authenticated XML/native shell is complete and receives early cross-phase credit in the reconciliation record.

The proposed contextual financial info-button layer is deferred. This does not relax the later UI requirement to use plain shop language, translate reason/action codes, and avoid exposing raw accounting jargon.

## Build Fix / Verification Update

A compile error in `Phase3NativeExpansionTest.kt` was corrected in the `CreditCollectionInput` constructor call:

- incorrect: `depositAccountId = cashId`
- correct: `paymentAccountId = cashId`

`CreditCollectionInput` defines the parameter as `paymentAccountId`. After this correction, the Android test Kotlin compilation command `./gradlew :app:compileDebugAndroidTestKotlin` was reported as completing successfully.
