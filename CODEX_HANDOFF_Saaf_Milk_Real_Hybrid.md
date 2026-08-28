# CODEX HANDOFF — Saaf Milk Real Hybrid Rebuild

## Purpose

You are taking over active implementation of the **Saaf Milk real hybrid rebuild**.

This is not the earlier experimental hybrid project. The working source is the clean rebuild based on:

- the untouched **Phase 18 Kotlin/Room checkpoint**;
- the latest **AI Studio React/Vite UI**;
- the locked **POSTMORTEM.md** playbook;
- the root phase records `PHASE0.md` through `PHASE5.md`;
- the root `README.md`.

Read those files before changing code.

## Current superseding checkpoint — 2026-08-28

The authoritative current phase ledger is `ROADMAP_RECONCILIATION_2026-08-27.md`. It preserves the locked roadmap while assigning valid early credit to native work originally planned across Phases 7, 9, and 11. Root Phases 3–6 are complete. `PHASE4.md` contains the criterion-by-criterion native test audit; `PHASE5.md` and `PHASE5_DTO_CATALOG.md` contain the frozen permission-aware hybrid read boundary; `PHASE6.md`, `bridge/CONTRACT.md`, and `bridge/contract.json` contain canonical bridge contract version 1 and its drift gate. The Phase 7 authenticated physical-device vertical slice is next. Broad React delivery remains later.

Sections below that describe the original schema-expansion approval gate are retained as implementation history and are superseded by `README.md` and root phase records through `PHASE6.md` for current status. Additive Room schema versions 4 through 8 are implemented and device-verified. Schema 7 security, schema 8 costing, encrypted portable backup format 2, Android-Keystore local snapshots, the authenticated native shell/user-administration flow, the Phase 5 read-model catalog, and the Phase 6 message contract are implemented. The complete connected-device suite passes 124/124 with 0 skipped and 0 failed; host-side debug unit tests pass 7/7. The runtime WebView bridge and React wiring remain incomplete.

Application user accounts and native role-based authorization are explicitly approved as a required cross-phase capability. Their schema-7 model, encrypted backup boundary, native credential UI, app-owned sessions, private staff enrollment/reset, user administration, role-aware navigation, and command-boundary authorization are implemented; React/bridge integration is not. Application users are security principals separate from employee/salary profiles, with an optional link between them. The schema-v6 daily-close actor name/role is transitional asserted metadata, not authenticated identity.

The two completed Phase 3 design tracks were:

1. authenticated users, credentials, roles/permissions, sessions, recovery, audit actor IDs, migration, and backup/restore security;
2. authoritative costing/COGS and profitability semantics.

The researched recommendation for gate 1 is `PHASE3_USER_ACCOUNTS_DESIGN_PROPOSAL.md`. The user explicitly approved it on 2026-08-27. Its native security and native-shell scope is implemented and device-verified. Gate 2 was approved and implemented in schema 8 with moving-weighted-average Milk cost, versioned Yogurt conversion cost, opening-basis provenance, yield/loss controls, deterministic replay, honest coverage states, and costing-aware encrypted backup/restore. Phase 4 audited their executable evidence, Phase 5 exposed the safe native facts and stable codes required by future screens, and Phase 6 froze their message contract. None of this should be mistaken for a completed runtime bridge or React delivery. Phase 7 is next.

---

# 1. Source-of-truth hierarchy

The project has three contracts.

## Business contract

Authoritative:

1. frozen Phase 18 logic specification;
2. frozen Phase 18 SQL blueprint;
3. only explicitly approved native extensions.

## UI capability contract

Authoritative:

- `PHASE1.md` capability inventory;
- `PHASE2.md` approved product capability set.

The latest AI Studio UI is the desired product surface, but it is **not** allowed to invent accounting semantics.

## Bridge contract

Authoritative:

- `bridge/CONTRACT.md`;
- `bridge/contract.json`;
- generated Kotlin/TypeScript projections validated by `scripts/verify-bridge-contract.py`.

Do not create bridge vocabulary ad hoc or bypass the mandatory Android/UI pre-build drift gate.

---

# 2. Non-negotiable engineering rules

## Preserve existing business behavior

The Phase 18 accounting/business core is authoritative.

**Reuse existing business logic/calculations wherever relevant.**

Before writing a new formula, determine whether the answer already exists through:

1. **DIRECT REUSE** — existing Phase 18 service/calculator;
2. **NATIVE DERIVATION** — existing authoritative events/logs/read models;
3. **GENUINELY NEW DOMAIN LOGIC** — only if the first two are insufficient.

Do **not** duplicate an existing computation in Kotlin or React.

Examples that must remain authoritative:

- `VendorPurchaseCalculator` / purchase Fat-LR-SNF-TS calculations;
- `SaleService`;
- `CreditCollectionService`;
- `VendorPaymentService`;
- `ExpenseService`;
- `SalaryPaymentService`;
- `YogurtProductionService`;
- `CorrectionService`;
- `BudgetManagementService`;
- canonical account balances;
- canonical inventory movements;
- existing analytics where applicable.

React is a renderer/interaction layer, **not a second accounting engine**.

## No fake data

No mock business records or localStorage business truth in the final implementation.

The AI Studio `localStorage` logic is product-intent evidence only.

## Do not change frozen baseline silently

Current immutable hashes:

Logic specification SHA-256:

`45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`

SQL blueprint SHA-256:

`fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

A schema/business-rule change must:

1. establish that a genuine product capability requires it;
2. document the proposed change;
3. obtain explicit user authorization;
4. use a versioned migration;
5. update phase documentation;
6. keep the original baseline files/hash record intact.

Do not modify code merely to make a test pass. Establish the actual cause.

---

# 3. Product scope approved in Phase 2

The rule is:

> Meaningful capabilities from the latest AI Studio UI remain in scope unless explicitly excluded.

## Existing Phase 18 capabilities to retain/reuse

- milk purchase / supplier intake;
- retail sale;
- wholesale cash sale;
- wholesale credit sale;
- customer credit collection;
- supplier payment;
- expense;
- salary payment;
- yogurt production;
- corrections;
- customer/vendor/employee account creation and rename;
- business-rate settings;
- budgets/targets;
- history;
- analytics;
- backup/restore.

## Approved native expansions

- product management;
- customer profiles;
- supplier profiles;
- employee profiles;
- employee attendance;
- delivery operations;
- spoilage/wastage;
- persisted physical cash counts / reconciliation records;
- customer/vendor account-specific ledgers;
- daily close/day locking;
- persistent business identity/opening state;
- custom expense categories;
- authenticated application users and native role-based authorization;
- authoritative COGS;
- gross profit;
- net profit;
- per-product profitability.

Some of these are approved **in principle** but still need exact business semantics before coding, especially:

- opening cash/opening stock events;
- daily-close locking/reopen/correction behavior;
- COGS/costing model;
- credentials, sessions, role/permission matrix, recovery, audit attribution, and backup treatment for application users;
- supplier-specific purchase-rate behavior if ever desired.

## Explicitly excluded unsafe options

Do not implement these in their unsafe/direct form:

1. direct `set stock = X`;
2. arbitrary JSON/localStorage business-state import;
3. unprotected `clear all business data`.

Inventory remains event-derived.

---

# 4. Current phase status

## Phase 0 — COMPLETE

Baseline frozen and hashed.

## Phase 1 — COMPLETE

Full AI Studio ↔ Phase 18 capability inventory.

## Phase 2 — COMPLETE

Desired product capability decisions approved.

## Phase 3 — COMPLETE

Goal:

> Expand the native Phase 18 layer so it genuinely supports the approved product before bridge/UI wiring.

Phase 3 started with a safe no-schema native derivation and closed after the approved schema-8 costing foundation was implemented and device-verified. Phase 4 later closed the native/security test matrix. Use `ROADMAP_RECONCILIATION_2026-08-27.md` for cross-phase credits and the current execution order.

## Phase 4 — COMPLETE

The native automated-test gate is closed in `PHASE4.md`. Nine focused security tests filled the audited gaps, and credential verification supports safe work-factor upgrades and equalized unavailable-account comparison paths.

## Phase 5 — COMPLETE

The permission-aware hybrid-facing native query/DTO layer is implemented in `application/readmodels` and frozen in `PHASE5_DTO_CATALOG.md`. It covers operational and financial positions, settings/opening state, profiles, references, corrected history/detail, ledgers, delivery/attendance/spoilage, reconciliation, profitability/costing controls, planning, current session/permissions, authorized users, and durable audit actors. Money is integer PKR paisa, quantities are canonical decimal strings, and credential/recovery/session-handle material is excluded. Four new tests bring the complete device suite to 124/124.

## Phase 6 — COMPLETE

Canonical bridge contract version 1 is frozen in `bridge/CONTRACT.md` and `bridge/contract.json` with 78 query/command/system operations. It defines lossless scalar encoding, authenticated envelopes, native-owned authority and actor attribution, 19 stable error codes, validation, persistence, refresh, backup picker/ticket boundaries, and version/hash negotiation. Generated Kotlin and TypeScript projections compile, and Android `preBuild` plus UI `prebuild` run the drift checker. Phase 7 is the current continuation point; no runtime bridge is implied.

---

# 5. Phase 3 work already implemented

## Native account-specific customer/vendor ledger

Classification:

**NATIVE DERIVATION**

No new accounting formula or SQL table was required.

Implemented:

- `data/AccountLedgerReadModels.kt`
- `FinancialLogDao.findAccountLedger(accountId)`
- `domain/AccountLedger.kt`
- `Phase3NativeExpansionTest.kt`

Semantics:

- original financial movement remains visible;
- correction delta movement remains visible;
- corrected original is marked `isSuperseded`;
- correction entry is marked `isCorrection`;
- correction carries `correctsEventId`;
- running balance is derived only from canonical financial movements;
- current balance remains sourced from canonical `account_balances`.

Do not replace this with UI filtering/reconstruction.

## Phase 3 test fix

`CreditCollectionInput` parameter was corrected from:

`depositAccountId`

to:

`paymentAccountId`

Gemini then reported this command successful:

`./gradlew :app:compileDebugAndroidTestKotlin`

Treat that compile result as the current verified checkpoint. Re-run it yourself in the actual repo before further work.

---

# 6. Historical Phase 3 schema-expansion gate

This section records the earlier schema-4 gate. It is no longer the current continuation point; see the superseding checkpoint above and `PHASE3.md`.

**No schema/business-rule amendment should be assumed approved merely from this handoff.**

Before applying the proposed persistence expansion, confirm explicit authorization from the user.

The documented proposed principles are:

1. add profile/operational tables while preserving existing account/event identity;
2. add `SPOILAGE` as an auditable inventory event;
3. add delivery as operational state linked to `SALE`, with no duplicate revenue;
4. persist physical cash counts and daily closes as reconciliation/control records, not a second ledger;
5. potentially make expense categories dynamic `Operating Expense` accounts;
6. add product metadata/archive support without direct stock assignment;
7. keep opening-state semantics, daily-close locking semantics, and COGS/costing as separate design approvals until defined.

Read the full proposal in `PHASE3.md`.

---

# 7. Recommended immediate Codex workflow

Do this before implementing more Phase 3 functionality.

### A. Establish repo/build state

Inspect:

- root `README.md`
- `PHASE0.md`
- `PHASE1.md`
- `PHASE2.md`
- `PHASE3.md`
- `POSTMORTEM.md`
- `phase18/`
- `ui/`

Then run the available clean/compile/test checks and report exact results.

At minimum verify the command already reported as passing:

`./gradlew :app:compileDebugAndroidTestKotlin`

Also inspect the Gradle project structure before inventing task names.

### B. Verify the frozen files

Compute and compare the two immutable hashes.

If either differs, stop and investigate.

### C. Inspect Phase 3 ledger implementation

Review the account-ledger changes for correctness against:

- existing `financial_log`;
- account balances;
- correction semantics;
- existing service behavior.

Do not refactor the core merely for style.

### D. Continue only after schema-extension authorization

Once authorized, implement approved native capability families in controlled vertical native slices.

Recommended Tier 1 order from `PHASE2.md`:

1. product metadata/management;
2. customer profiles;
3. supplier profiles;
4. employee profiles;
5. attendance;
6. delivery operations;
7. spoilage/wastage;
8. physical cash-count persistence;
9. account-ledger completion/verification.

For each capability:

1. inspect existing native code first;
2. reuse existing data/services/calculations;
3. define persistence/validation;
4. implement native service;
5. write native tests;
6. run tests;
7. update `PHASE3.md` and `README.md`;
8. verify frozen hashes;
9. do not touch React wiring yet.

Phase 4 is the dedicated native-test phase, but Phase 3 additions should still receive implementation tests immediately rather than accumulating unverified code.

---

# 8. Decisions that must not be guessed

Ask the user before inventing semantics for any of these.

## COGS / profitability

Existing purchase/sale/production data is useful, but there is no approved authoritative costing method yet.

Do not assume:

- FIFO;
- weighted-average cost;
- last cost;
- standard cost;
- purchase-price approximation.

## Authenticated users / authorization

Do not treat employee profiles or the schema-v6 daily-close actor role text as authenticated identity.

Before implementing the security schema, obtain explicit approval for:

- credential/PIN method and secure storage;
- the exact role/permission matrix;
- sessions, timeout, logout, failed-attempt throttling, and lockout;
- owner recovery and protection of the last recoverable owner;
- optional employee linkage;
- durable audit actor IDs and migration of transitional close attribution;
- backup/restore handling for account metadata, credential verifiers, and sessions.

Native Kotlin must authenticate and authorize. The bridge/UI may not self-assert user identity or privileged roles.

## Daily close

Do not guess:

- whether close blocks transactions;
- whether closed days may be reopened;
- whether corrections are allowed after close;
- how a reopen is audited;
- what exactly is frozen/snapshotted.

## Opening state

Opening cash and opening stock must be auditable.

Do not directly write current account balance/current stock.

## Supplier-specific milk rate

The existing purchase calculation/business settings remain authoritative.

Do not silently make a supplier profile rate override the current purchase-rate mechanism.

## Spoilage valuation

Quantity reduction can be implemented as inventory semantics once approved, but loss valuation must wait for the costing model.

---

# 9. Canonical vocabulary already learned from the test project

Do not reintroduce the earlier sales-channel bug.

Canonical Phase 18 sale channels:

- `retail`
- `wholesale`
- `credit-retail`
- `credit-wholesale`

Do not invent UI variants such as:

- `cash_retail`
- `cash_wholesale`
- `credit_delivery`
- `credit_wholesale`

All bridge values are now defined in contract version 1. Change them only through the documented manifest/version/drift workflow.

---

# 10. Later-phase hardening already locked

Do not implement these prematurely, but preserve the plan.

## Phase 4

Add native authentication and authorization tests alongside the other capability suites: credential verification, sessions, role enforcement, deactivation/lockout, recovery and last-owner safeguards, audit attribution, migration, backup/restore, and direct privilege-bypass attempts.

## Phase 5

Authoritative native read-model expansion, including the minimum current-user, permission, user-administration, and audit-actor views required by the approved UI. Never expose credential material.

## Phase 6

Completed in `PHASE6.md`: canonical `bridge/CONTRACT.md`, machine-readable manifest, generated projections, and mandatory drift checker.

Then add:

`scripts/verify-bridge-contract.py`

It should detect TypeScript/Kotlin contract drift before device testing.

Define authenticated-session, login/logout, user-administration, permission-denied, and session-expired contracts. Commands carry session proof; the bridge must not accept caller-supplied roles or actor identities as authority.

## Phase 7

Hybrid shell / vertical slice.

Preferred asset delivery:

AndroidX `WebViewAssetLoader`.

Web build must be a separate stage:

`scripts/build-web-ui` → `ui/dist/` → Android packaging.

Do not bury `npm run build` inside Gradle `preBuild`.

The first vertical slice must include first-owner/login handling, authenticated session propagation, logout, resume behavior, and at least one natively protected command.

## Phase 8

Screen-by-screen AI Studio → bridge → native wiring.

All mutations:

validate → await native command → explicit success/failure → refresh.

No silent returns and no fake "Saved".

Wire login/logout and authorized user administration. Navigation and controls may be role-aware for usability, but hiding a control never replaces native permission enforcement.

## Phases 9–10

Prove session persistence/expiry across lifecycle and process recreation, protect the back stack after logout, and wire verified user attribution and authorization across every protected capability. Replace transitional caller-asserted daily-close actor metadata with the verified native user.

## Phases 11–13

Harden credential storage, failed-attempt throttling/lockout, owner recovery, account disablement, concurrent session behavior, audit retention, and secure backup/restore. Remove mock users, default credentials, UI-only guards, and misleading controls that appear usable without permission.

## Phases 14–16

Run a physical-device role/permission and bypass matrix; shopkeeper-test user creation, role change, disablement, logout, recovery, close/reopen attribution, and a complete business day under real roles. Release must require deliberate first-owner setup, contain no default password/PIN, and include migration, recovery, session, and backup security checks.

---

# 11. Documentation rule

Every phase must have a root file:

`PHASE0.md`
`PHASE1.md`
`PHASE2.md`
`PHASE3.md`
...

Each phase record should contain:

- status;
- scope;
- changes;
- business-rule decisions;
- migrations;
- tests/verification;
- relevant hashes;
- known limitations;
- next gate.

Do not overwrite prior phase history.

Keep root `README.md` current with:

- current phase;
- current build/test state;
- immutable hashes;
- architecture;
- next action.

---

# 12. Checkpoint/package rule

Every deliverable/checkpoint must include the complete working project:

- native/core source;
- UI source;
- tests;
- documentation;
- build files.

Exclude generated/machine-specific material:

- `build/`
- `.gradle/`
- `node_modules/`
- `local.properties`

Verify the archive/repo before declaring a phase complete.

---

# 13. What the user expects from Codex

The reason for moving this work to Codex is to use an actual coding/debug loop:

edit
→ compile
→ inspect exact failure
→ fix root cause
→ rerun
→ test
→ inspect diff
→ document.

Do not return speculative patches when the repo/compiler can establish the answer.

When a build or test fails:

1. inspect the exact code/API/schema involved;
2. identify root cause;
3. patch only the real cause;
4. preserve existing business behavior;
5. rerun the relevant command;
6. report the command and result.

---

# 14. Historical first message / instruction

The following was the original takeover instruction and is retained for provenance. It is superseded by the current checkpoint at the top of this handoff.

After opening the repo, begin with:

> Read `README.md`, `PHASE0.md`, `PHASE1.md`, `PHASE2.md`, `PHASE3.md`, `POSTMORTEM.md`, and this `CODEX_HANDOFF_Saaf_Milk_Real_Hybrid.md`. Inspect the current Phase 3 native ledger implementation and the untouched Phase 18 business core. Verify the frozen logic-spec and SQL hashes and rerun the existing native Android-test Kotlin compile. Do not modify the frozen SQL/spec or invent new formulas. Report the exact repo/build status and the next safe Phase 3 implementation step. If the next step requires a schema/business-rule amendment, stop for explicit authorization before changing it.

That text is historical provenance only. The current continuation point is the Phase 7 authenticated physical-device vertical slice using the frozen Phase 6 contract, described at the top of this handoff and in `ROADMAP_RECONCILIATION_2026-08-27.md`.
