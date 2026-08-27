# Saaf Milk Hybrid Project — Postmortem & Fresh-Start Playbook

**Status: LOCKED WORKING GUIDE — v2 (bridge, build, and hybrid-boundary hardening incorporated)**

## Purpose

This document captures what the test project taught us about integrating the AI Studio React UI with the existing Phase 18 Kotlin/Room accounting application.

It is intended to be used as an implementation playbook for the real project.

The goal is not to repeat the same phase sequence with better discipline. The goal is to change the architecture and workflow so that the classes of failures discovered in the test project are prevented before they happen.

---

# 1. Non-Negotiable Rules

## 1.1 The Phase 18 business model remains authoritative

The existing logic specification and SQL blueprint are the source of truth.

Never change the schema, accounting semantics, inventory semantics, correction semantics, or business formulas merely because the UI is inconvenient to wire.

If the UI conflicts with the core:

1. determine whether the UI is wrong;
2. determine whether a genuine domain capability is missing;
3. only change the domain when explicitly authorized and after proving the requirement.

Never make a UI workaround that silently changes accounting behavior.

## 1.2 React is not an accounting engine

React may handle:

- rendering;
- navigation;
- filtering;
- local form state;
- display-only calculations;
- formatting;
- temporary UI state.

React must not authoritatively calculate or persist:

- money;
- balances;
- receivables;
- payables;
- inventory;
- COGS;
- profit;
- reconciliation;
- correction effects;
- persisted business state.

Those facts originate in Kotlin/Room.

## 1.3 Every UI action must have a capability status

Before implementation, every visible action is classified as:

- WIRED: native capability exists and the complete path is defined.
- BRIDGE-MISSING: native capability exists but the bridge/UI adapter does not.
- DOMAIN-MISSING: UI requests a capability the domain does not support.
- REMOVE/DEFER: the UI action should not exist yet.

A button must never exist merely because the mockup contains it.

## 1.4 No silent failures

No mutation may use a silent `return` for a condition that means the user asked for an operation and it could not be performed.

Use an explicit result/error path.

The user must receive:

- success confirmation after native success;
- validation error before submission when appropriate;
- native/domain error when the command fails.

---

# 2. What Went Wrong in the Test Project

## 2.1 The hybrid was wired before the runtime boundary was fully proven

We initially had the UI, bridge, native services, tests, and build pipeline evolving together.

This caused us to discover basic integration problems late:

- blank/white WebView;
- static startup overlays;
- Tailwind not actually being emitted;
- WebView asset-origin issues;
- Android system-bar overlap;
- stale/incorrect scroll containers;
- legacy XML tests launching a different UI;
- package checkpoints missing the full `ui` tree.

### Fresh-start rule

Do not implement the whole application first.

Prove a tiny vertical slice:

React button
→ Vite production build
→ Android assets
→ WebView
→ JS bridge
→ Kotlin command
→ Room
→ JSON response
→ React update

Do not scale beyond the first vertical slice until that path works from a clean build on a real device.

---

# 3. The Read Model Must Come Before Most UI Wiring

The biggest architectural improvement is to establish the native UI read model before migrating screens.

Use a stable native DTO boundary, conceptually:

AppSnapshot
- business settings
- accounts
- customers
- suppliers
- employees
- products
- inventory
- daily totals
- transactions/history
- receivables
- payables
- cash/JazzCash/Bank reconciliation
- reports
- capabilities

The React layer should render this model.

Do not make individual screens infer business meaning from raw events.

For example, React should not decide that an event "looks like a wholesale sale". Kotlin should provide the channel, payment account, product identity, customer/vendor identity, and correction relationship already interpreted.

---

# 4. The Bridge Must Be a Contract, Not an Accumulation of Functions

Define the bridge before implementing screens.

## Queries

- getAppSnapshot
- getHistory
- getAccountLedger
- getBudgetSnapshot
- getReconciliation
- getReport

## Commands

- recordPurchase
- recordSale
- recordProduction
- recordCustomerCollection
- recordVendorPayment
- recordExpense
- recordSalaryPayment
- createCustomer
- renameCustomer
- createVendor
- renameVendor
- createEmployee
- renameEmployee
- updateBusinessSettings
- correctTransaction

## System actions

- exportBackup
- pickRestoreBackup
- restoreBackup

Every bridge command needs a documented contract:

- request fields;
- canonical enum/string values;
- validation rules;
- success response;
- error response;
- whether it mutates the ledger;
- refresh behavior;
- idempotency expectations where applicable.

The bridge must use canonical Phase 18 values. Do not invent a UI-specific second vocabulary.

Example from the test project:

Phase 18 accepted:
- retail
- wholesale
- credit-retail
- credit-wholesale

The UI previously sent:
- cash_retail
- cash_wholesale
- credit_delivery
- credit_wholesale

That mismatch caused a real SQLite constraint failure.

Fresh-start rule:
Keep one canonical enum vocabulary at the domain boundary.

---


## 4A. Three Contracts

The real project must maintain three distinct contracts:

### Business Contract

Defines what the accounting/business system means.

Source of truth:
- frozen Phase 18 logic specification;
- frozen SQL blueprint;
- explicitly approved domain extensions.

### UI Capability Contract

Defines what the desired product/UI is supposed to do.

Source of truth:
- Phase 1 capability inventory;
- Phase 2 approved capability decisions.

### Bridge Contract

Defines how React communicates with native code.

Source of truth:
- `bridge/CONTRACT.md`.

The dependency chain is:

`UI capability → business capability → bridge contract → native implementation`

The UI may reveal desired capabilities, but may not silently define accounting semantics.
The bridge may translate between layers, but may not invent a second business vocabulary.

---

## 4B. Bridge Drift Must Be Machine-Checked

Add:

`scripts/verify-bridge-contract.py`

Run it before Android compilation.

At minimum it checks:
- action names used by TypeScript against Kotlin handlers;
- request field names where statically discoverable;
- required/optional fields where statically discoverable;
- canonical enum/string values;
- unexpected actions or enum values.

This is intentionally cheap and device-independent. It complements, rather than replaces,
runtime and instrumentation tests.

---

## 4C. WebView Asset Delivery

Use AndroidX `WebViewAssetLoader` for packaged local React assets when supported by the
selected AndroidX WebKit version.

The shell must prove:
- production ES modules load;
- relative imports load;
- CSS loads;
- static assets load;
- the APK can run the packaged production `dist/`;
- there are no resource-loading errors.

Custom asset interception is only permitted when a concrete platform constraint requires it.

---

## 4D. Web Build and Android Build Are Separate Stages

Provide an explicit:

`scripts/build-web-ui`

that produces a verified `ui/dist/`.

Then Android packaging consumes that already-built output.

A convenience wrapper may run both stages, but the stages remain independently callable.

The project must pin:
- Node/npm expectations;
- package-lock;
- Gradle wrapper;
- Android Gradle Plugin;
- JDK/Android SDK requirements.

A React build failure must be reported as a React build failure, not as an opaque Gradle failure.

---

# 5. Separate UI State From Domain State

The test project repeatedly suffered from asynchronous snapshot initialization.

Example:

- products were initially empty;
- a product selector was initialized from products[0];
- the native snapshot arrived later;
- the selector remained empty;
- the submit handler saw no product;
- the handler returned without calling the bridge.

This is a lifecycle problem, not a business-rule problem.

### Fresh-start pattern

Use explicit asynchronous states:

- loading;
- ready;
- error.

For dependent selectors:

- initialize empty;
- populate when the authoritative collection arrives;
- clear selection if the selected entity disappears;
- disable submission until required identity is valid.

Do not derive one-time state from an array that is expected to be populated asynchronously.

---

# 6. Mutation Handling Must Be Awaited End-to-End

Another repeated failure pattern was:

submit
→ fire async native mutation
→ immediately close modal
→ reset state

If the native operation fails, the user sees no meaningful feedback.

### Fresh-start pattern

All mutations are handled as:

submit
→ validate
→ set submitting=true
→ await native command
→ on success: close/reset/refresh
→ on failure: retain form and show error
→ finally: submitting=false

The UI never declares "Saved" until native success is returned.

---

# 7. Runtime Shell Ownership Must Be Defined Early

The test project showed repeated confusion about:

- Android status bar;
- Android navigation bar;
- WebView bounds;
- React padding;
- header;
- bottom navigation;
- footer;
- safe areas.

### Fresh-start shell contract

Android owns:

- activity lifecycle;
- system bars/insets;
- file pickers;
- WebView container;
- native bridge.

React owns:

- application header;
- page content;
- application navigation;
- dialogs;
- application-level scrolling.

There must be exactly one vertical application scroll region.

The bottom app navigation is application chrome, not a footer.

It should be a fixed/floating layer above the Android navigation area.

Only explicitly designed child controls, such as a tab strip, may scroll horizontally.

---

# 8. Do Not Use Startup Screens as a Substitute for Runtime Resilience

The project accumulated multiple startup surfaces:

- Android/WebView bootstrap;
- React startup loader;
- snapshot loading.

This made the application feel slower and masked actual startup defects.

### Fresh-start pattern

Render the UI immediately.

Load the native snapshot asynchronously.

Use small inline loading states where data is unavailable.

Keep:

- error boundary;
- native timeout;
- retry;
- logging.

Remove:

- static full-screen boot overlay;
- unnecessary React full-screen loading gate.

A user should only see a full-screen loading screen when there is a genuinely long operation that cannot be presented incrementally.

---

# 9. Tests Must Follow the Architecture

The old test suite proved a major lesson.

Tests were written for:

MainActivity
→ XML views

while the real application had become:

WebUiActivity
→ React/WebView

The tests were therefore testing the wrong product.

### Fresh-start test architecture

## Layer 1 — Domain tests

Kotlin/Room:

- transaction formulas;
- balances;
- inventory;
- corrections;
- reporting;
- backup;
- concurrency.

## Layer 2 — Bridge tests

Verify:

React-shaped request
→ bridge
→ native command
→ valid response/error.

Include enum/string contract validation.

## Layer 3 — Hybrid UI tests

Verify the actual React application:

- page loads;
- navigation works;
- forms accept input;
- buttons trigger the bridge;
- success and error states render.

## Layer 4 — Device acceptance

Verify:

- system bars;
- touch;
- keyboard;
- scrolling;
- layout;
- visual hierarchy;
- dialogs;
- real-device behavior.

Never maintain a second UI test suite for an obsolete XML interface.

---

# 10. UI Must Be Tested as a Built Artifact

Source inspection is not enough.

The Tailwind failure proved this.

The source contained the correct classes, but the built APK did not contain the generated styling because the Vite/Tailwind integration was incomplete.

### Fresh-start UI gate

For every visual milestone:

1. npm build;
2. inspect generated dist/assets;
3. Android build;
4. install APK;
5. inspect on physical device;
6. capture screenshot;
7. only then mark the UI milestone passed.

Never declare a UI phase complete from source inspection alone.

---

# 11. Dependencies and Build Environment Must Be Frozen

The project repeatedly hit environment limitations around:

- npm dependencies;
- optional native packages;
- esbuild scripts;
- Rollup binaries;
- Gradle distribution.

### Fresh-start build baseline

Commit:

- package-lock.json;
- exact Node/npm expectations;
- Gradle wrapper;
- Gradle version;
- Android plugin versions;
- SDK requirement;
- JDK requirement.

The first setup task should verify:

node
npm
Gradle wrapper
JDK
Android SDK

Then:

npm ci
npm run build
gradlew clean assembleDebug

No implementation work should begin until that clean-build gate passes.

---

# 12. Seed Data and Onboarding Need to Be Treated as Product Behavior

The core spec explicitly warns that there are no seed Vendor/Customer/Salary accounts and that the first related transaction will fail until the required account exists.

That is not just a test concern.

The real app needs an onboarding path that makes this understandable.

### Fresh-start rule

At first-run:

- create/select business;
- create at least one vendor;
- create at least one customer if credit sales are needed;
- create at least one employee if salary functionality is enabled.

Transactions should not fail with a raw database-style error simply because onboarding is incomplete.

---

# 13. Form Dependencies Must Be Modeled Explicitly

Examples:

Milk intake requires:
- supplier;
- raw liters;
- Fat%;
- LR.

Credit wholesale sale requires:
- customer;
- product;
- quantity/rate;
- credit payment method.

Cash sale requires:
- product;
- quantity/rate;
- valid Cash account.

Do not let the user reach a form that cannot succeed without explaining why.

Forms should expose prerequisites before submission.

---

# 14. Account / Ledger Views Need Native Identity, Not UI Reconstruction

A customer or supplier detail screen should request a native account-specific ledger.

The UI should not filter the entire global event stream and guess which events belong to the account.

The native response should include:

- account identity;
- balance;
- effective entries;
- event ID;
- time;
- event type;
- amount;
- notes;
- correction relationship where relevant.

---

# 15. Audit Log Semantics Must Be Singular

The test UI invented a React-only session "Activity/Audit Log".

That was confusing because the native event/history system is already the durable audit trail.

### Fresh-start rule

Have one authoritative durable history.

If a session activity feed is useful for UX, call it something like:

Recent Activity

and make its non-authoritative nature explicit.

Do not call two different mechanisms "Audit Log".

---

# 16. Delivery Must Remain a Deliberate Product Decision

The UI contained delivery concepts before the domain had a delivery workflow.

Do not create fake delivery statuses merely to satisfy the mockup.

Future implementation, if explicitly approved, should model delivery as an operational layer that references the underlying financial sale.

The financial sale remains authoritative.

---

# 17. Inventory Must Remain Event-Derived

Do not add a convenient "set stock to X" API just because the UI wants a stock editor.

Inventory should derive from the supported event types.

If a future stock adjustment/spoilage mechanism is needed, define it as a real domain event with correct accounting treatment and auditability.

---

# 18. Correction Semantics Must Be Tested as Restated Truth + Delta

The domain rule is:

- correction event header = corrected true value;
- attached accounting/inventory movement = delta needed to reconcile the old state to the new state.

This must be represented consistently in:

- history;
- inventory;
- reports;
- balances;
- account ledgers.

The test project already found that naïvely summing correction deltas into movement presentation can produce incorrect read models.

Fresh-start rule:
Every corrected read model must have explicit "effective event" semantics.

---

# 19. Packaging Discipline Is Part of Engineering

The Phase L package containing only `core` demonstrated that a technically correct code change can still produce an unusable checkpoint.

### Fresh-start checkpoint rule

Every phase package must contain:

- core/
- ui/
- tests/
- documentation
- build files

and must exclude:

- build/
- .gradle/
- node_modules/
- machine-specific local.properties

Before delivery, automatically verify:

- expected directories exist;
- critical files exist;
- SQL hash unchanged;
- archive opens;
- package has the expected file count/profile.

---

# 20. The Locked Roadmap for the Real Project

This roadmap is the working guide for the real rebuild.

The purpose is to prevent the failure classes discovered in the test project:
premature wiring, duplicate vocabularies, missing native capabilities, async no-op
forms, UI/domain ambiguity, obsolete UI tests, WebView/runtime problems, and source/build drift.

Phases are not skipped for convenience.

## Phase 0 — Freeze & Baseline

Preserve and hash:
- original Phase 18 source;
- frozen logic specification;
- frozen SQL blueprint;
- latest AI Studio source.

Create the immutable baseline archive.

No implementation changes.

## Phase 1 — Full AI Studio ↔ Phase 18 Capability Inventory

Perform a source-to-source inventory before wiring.

For each meaningful UI capability record:
- screen/component;
- action;
- inputs;
- expected result;
- required data;
- persistence implications.

For each native capability record:
- service;
- DAO/entity;
- event/transaction;
- validation;
- read model;
- persistence.

Classify:
- EXISTING / READY TO WIRE;
- EXISTING / BRIDGE REQUIRED;
- NATIVE EXTENSION REQUIRED;
- BUSINESS DECISION REQUIRED;
- UI-ONLY / COSMETIC.

No desired UI capability is deleted merely because Phase 18 lacks it.

## Phase 2 — Desired Product Capability Decisions

Phase 2 uses the explicit product goal established for the real rebuild: meaningful capabilities present in the new AI Studio UI are retained and moved into the native implementation plan rather than being removed merely because the original Phase 18 core lacks them. Direct stock mutation, arbitrary state import, and unprotected destructive reset remain excluded.

Review every NATIVE EXTENSION REQUIRED and BUSINESS DECISION REQUIRED capability.

Explicitly decide which capabilities belong in the real product.

Examples:
- product management;
- richer customer/vendor records;
- delivery;
- spoilage/wastage;
- planning/budgets;
- persisted physical cash counts;
- account-specific ledgers;
- additional reporting.

The result is the approved native capability list.

No native feature is implemented merely because it appears in the mockup.

## Phase 3 — Native Phase 18 Capability Expansion

Implement the approved missing capabilities in Kotlin first.

For each:
- domain behavior;
- persistence;
- validation;
- transaction semantics;
- correction implications;
- backup implications;
- read-model implications.

Application users are a first-class native security domain, separate from employee/salary profiles. Phase 3 must explicitly define users, credentials, roles/permissions, sessions, optional employee links, first-owner bootstrap, deactivation/lockout, recovery, durable audit actors, migrations, and secure backup/restore semantics before implementation. React-provided roles are never authority.

React is not used to hide an incomplete native capability.

## Phase 4 — Native Automated Tests for Every New Capability

Prove each new native capability independently before UI wiring.

Test:
- happy path;
- validation;
- failure;
- persistence;
- restart;
- correction where applicable;
- account/inventory effects where applicable;
- backup/restore interaction;
- concurrency where applicable.

For user accounts also test credential verification, session creation/expiry/logout, role enforcement, privilege-bypass attempts, account disablement/lockout, last-owner safeguards, recovery, audit attribution, migration, and backup/restore behavior. Never use plaintext credentials in tests or fixtures intended for release.

## Phase 5 — Native Read-Model Expansion

Build the authoritative native read model required by the approved UI.

It should expose the business facts needed for:
- settings;
- accounts;
- people;
- products;
- inventory;
- daily totals;
- history;
- effective corrected history;
- receivables/payables;
- reconciliation;
- reports;
- newly approved capabilities.

For security, expose only the current-user/session status, effective permissions, authorized user-administration data, and durable audit actor display required by the UI. Credential material is never a read model.

Freeze the DTO contract after review.

## Phase 6 — Bridge / API Contract

Create `bridge/CONTRACT.md`.

Separate:
- Queries;
- Commands;
- System operations.

For every endpoint define:
- exact action name;
- request schema;
- canonical enum values;
- required/optional fields;
- response schema;
- errors;
- validation;
- persistence effect;
- refresh behavior.

Authentication contracts must cover first-owner setup, login, logout, session expiry, credential change/recovery, and authorized user administration. Protected commands carry session proof; callers never supply an authoritative role or actor identity. Define consistent unauthenticated, session-expired, and permission-denied errors.

Run the bridge drift checker as a mandatory pre-build gate.

## Phase 7 — Hybrid Shell and Build-Proof Vertical Slice

Build the minimum React/WebView/Android shell.

Use `WebViewAssetLoader` where supported.

Build web output explicitly:
`scripts/build-web-ui` → `ui/dist/` → Android packaging.

Prove:
React → web build → packaged assets → WebView → bridge → Kotlin → Room → result → React.

The proof slice includes first-owner/login handling, authenticated session propagation, logout, lifecycle resume, and at least one command whose permission is enforced in Kotlin.

Also prove:
- clean web build;
- bridge drift check;
- clean Gradle build;
- APK install;
- ES modules/CSS/assets;
- system-bar ownership;
- viewport;
- application scrolling.

Do not scale until this passes on a physical device.

## Phase 8 — AI Studio → Bridge → Native Wiring

Wire the approved UI capability matrix screen-by-screen.

For every mutation:
validate → await native command → success/failure → refresh.

No silent returns.
No false "Saved".
No invented bridge values.

Every migrated screen gets a hybrid integration test.

Wire login/logout and authorized user management. Role-aware visibility improves usability but must not replace native authorization.

## Phase 9 — Runtime / Navigation / Device Integration

Complete:
- system insets;
- fixed/floating application chrome;
- central vertical scrolling;
- controlled horizontal child scrolling;
- dialogs;
- keyboard behavior;
- lifecycle/process recreation;
- WebView error handling.

Also prove session restoration/expiry, device-lock/background behavior, logout back-stack protection, and process recreation without leaking authenticated content.

There is one production presentation activity/path.

## Phase 10 — Functional Product Completion

Run the capability matrix to closure.

At this stage every intended capability is either:
- genuinely native-backed and wired;
- explicitly deferred;
- or deliberately UI-only.

All protected capabilities must use the verified native user/session, including daily close/reopen and administrative operations. Transitional caller-asserted actor roles are removed from production command authority.

## Phase 11 — Integrity / Recovery / Concurrency

Prove:
- rapid repeated transactions;
- concurrent writes;
- snapshot reads during writes;
- lifecycle/process recreation;
- backup during operations;
- restore;
- corruption rejection;
- correction integrity;
- large history;
- repeated recovery cycles.

Security hardening also proves secure credential storage, failed-attempt throttling/lockout, owner recovery and last-owner safeguards, account disablement, concurrent sessions, audit retention, session invalidation, and the approved policy for credential secrets in backup/restore. Plaintext or default credentials are prohibited.

## Phase 12 — UI Fidelity

Compare actual Android rendering with the approved AI Studio UI.

Audit:
- typography;
- spacing;
- alignment;
- controls;
- navigation;
- dialogs;
- responsive behavior;
- loading/error/empty states.

Do not change business semantics.

## Phase 13 — Dead / Mock / Misleading UI Cleanup

Search for:
- TODO;
- mock;
- fake;
- placeholder;
- disabled;
- coming soon;
- not implemented;
- no-op;
- dead bridge;
- dead state;
- obsolete tests;
- misleading labels.

Remove accidental leftovers and preserve documented intentional restrictions.

Also remove mock users, default credentials, client-only permission checks, self-asserted roles, and controls that appear available to users who cannot perform the native command.

## Phase 14 — Final Device QA

Use the actual APK on the physical device.

Verify:
- startup;
- system bars;
- scrolling;
- touch;
- keyboard;
- navigation;
- dialogs;
- performance;
- persistence;
- errors.

Run the complete authentication and authorization matrix for every role, including direct bridge attempts to bypass hidden/disabled UI and lifecycle checks after logout or session expiry.

Source inspection alone cannot pass this phase.

## Phase 15 — Final Shopkeeper / Real-World QA

Simulate the complete business day and deliberately introduce:
- wrong transactions;
- duplicate submission;
- correction;
- backup;
- restore;
- restart.

Exercise realistic account administration: first-owner setup, staff-user creation, role assignment/change, login/logout, account disablement, recovery, and verified attribution for close/reopen and other audited actions.

Compare application results with independently calculated expected results.

## Phase 16 — Release Readiness

Only after all previous phases pass:
- reproducible final build;
- release configuration;
- dependency review;
- artifact-size review;
- migration/version review;
- final test report;
- source archive;
- documentation.

Release security also requires deliberate first-owner setup, no default password/PIN, reviewed role permissions, tested recovery, session invalidation, migration safety, and the approved secure backup policy for user/account data.

No feature development starts here.

---

# 21. Definition of Done for the Real Project

The project is done only when all of these are true:

### Build

A clean environment can reproduce the Android build.

### UI

The actual physical-device application behaves and looks like the approved product UI.

### Capabilities

Every intentionally retained product capability is genuinely native-backed.

### Transactions

Every supported transaction can be completed through the UI.

### Accounting

Independent expected results agree with the native/domain results.

### Persistence

Data survives restart and lifecycle events.

### Corrections

Corrections produce the correct effective history and financial/inventory state.

### Backup

Backup and restore work without corrupting the ledger.

### Errors

Failures are visible and actionable.

### Testing

Tests validate the actual hybrid architecture.

### Integrity

Concurrency and recovery do not corrupt the ledger.

### Domain honesty

No UI feature claims persistence or business semantics that do not exist.

### Packaging

Every project checkpoint is a complete reproducible hybrid source package.

---

# 22. Rules for Deviations From the Locked Roadmap

The roadmap is the default implementation guide. The three contracts (Business,
UI Capability, Bridge) and their validation gates are part of that locked process.

A deviation is permitted only when a newly discovered fact makes the current
sequence technically or product-wise invalid.

When that occurs, document:

1. what was discovered;
2. why the current roadmap step is insufficient;
3. what is being changed;
4. what existing behavior remains protected;
5. what new acceptance criteria are introduced.

Do not renumber or rewrite the roadmap casually.

The roadmap describes process.
The frozen logic specification and SQL describe business truth.

# 21. Definition of Done for the Real Project

The project is done only when all of these are true:

### Build

A clean machine can build the complete APK from source.

### UI

The actual physical-device UI looks and behaves like a real mobile application.

### Transactions

Every supported visible transaction can be completed from the UI.

### Accounting

Independent expected calculations match native results.

### Persistence

Data survives restart.

### Corrections

Corrections update effective history, balances, inventory, and reports correctly.

### Backup

Backup → restart → modify → restore → verify works.

### Failure handling

Users get actionable errors instead of silent failures.

### Tests

Tests validate the actual architecture, not an obsolete UI.

### Integrity

Concurrency and recovery do not corrupt the ledger.

### Domain honesty

Unsupported features are either genuinely implemented or deliberately absent.

---

# 22. The Most Important Change in Mindset

The real project should not be approached as:

"Take this beautiful AI Studio UI and wire it into the Kotlin app."

It should be approached as:

"Take this UI as the presentation specification, compare it to the authoritative domain, define the contracts, and then implement a hybrid application where each layer has one job."

That distinction prevents most of the pain we experienced.

The AI Studio UI remains valuable because it is excellent at:

- interaction design;
- presentation;
- discovering useful business workflows;
- visual hierarchy;
- options and settings.

But it cannot be allowed to silently define business semantics.

Likewise, the Kotlin core remains authoritative for:

- accounting;
- inventory;
- persistence;
- corrections;
- balances;
- reporting facts.

Neither side should impersonate the other.

---

# 23. Practical Starting Checklist for the Next Project

Before asking an AI agent to modify anything, provide it with:

1. Core logic specification.
2. SQL blueprint.
3. Original Kotlin source.
4. AI Studio source.
5. This postmortem/playbook.
6. Capability matrix.
7. Bridge API contract.
8. Read-model DTO contract.
9. Build instructions.
10. Rule: do not modify frozen logic/schema without explicit authorization.

The agent's first deliverable should be an audit, not code.

Its second deliverable should be the minimal hybrid vertical slice.

Only then should implementation begin.
