# Phase 14.8 — Native UI redesign and migration

Status: **ACTIVE — checkpoint 14.8.1 complete; high-fidelity Cashier visual directions ready for review**

Date started: 2026-09-12

## Decision and reason

The owner approved moving the final product presentation from the React/WebView shell to native Android UI before uncoached shopkeeper QA. Native UI was the intended final architecture, and the current interface is not accepted as the product experience. Implementing another large React redesign and then recreating it natively would duplicate the highest-cost work and add a second parity cycle.

The redesign will therefore be specified once and implemented in Jetpack Compose. The verified hybrid application remains runnable as a behavioral reference and temporary fallback until native replacement screens pass their parity gates.

This insertion does not reopen or rewrite Phase 14. Phase 14's functional, security, recovery, accessibility, and device evidence remains the frozen pre-migration baseline. Phase 15 shopkeeper QA moves behind the native migration because it must assess the interface intended for release.

## Product outcome

Deliver a fully native Saaf Milk interface that lets an owner or staff member understand the app, complete daily work, notice the result of each action, recover from mistakes, and find less common controls without coaching or accounting expertise.

The native interface must preserve the proven Room/Kotlin behavior, native authorization, audit attribution, Quick unlock boundaries, encrypted backup format, cloud-recovery behavior, and frozen business calculations.

## Architecture decision

### Retain as authority

- Room database, schema version 8, migrations, DAOs, and repositories;
- domain models, calculations, validation, and transaction semantics;
- `UserSecurityService`, memory-only sessions, roles, permissions, and audit;
- Quick unlock services and Android device-security integration;
- local and Google Drive backup, cryptography, scheduling, retention, and restore;
- Kotlin read models and typed command orchestration;
- existing host, instrumentation, migration, security, backup, and regression tests.

### Replace

- React/TypeScript presentation;
- WebView navigation, layout, dialogs, form state, and action feedback;
- JavaScript lifecycle, viewport, Back, and targeted-refresh glue;
- JSON request/response transport once no WebView caller remains;
- packaged web assets and the web packaging requirement after final retirement.

### Refactor without changing behavior

Several typed Kotlin command services currently live under `application.bridge` even though their work is useful outside the bridge. Native screens must not call JSON handlers or manufacture bridge envelopes. Before a screen family migrates, its typed read/command boundary will be exposed through a presentation-neutral Kotlin facade or moved to a neutral application package with behavior-preserving tests.

The older XML presentation and ViewModels are reference material. Useful state and validation can be retained selectively, but the oversized imperative `MainActivity` is not the new shell.

## Migration rule

Migration is incremental:

1. keep the final Phase 14 hybrid build runnable;
2. add Compose alongside the existing presentation;
3. implement and test one complete native vertical slice;
4. route only accepted screen families to native UI;
5. leave unfinished families on the hybrid fallback;
6. delete WebView and bridge code only after the endpoint-ownership count reaches zero.

There is one business database and one native security authority throughout. No second data model, business engine, credential store, or sync mechanism may be introduced by the migration.

## Standing show-before-build rule

For every major native screen family:

1. show two or more concrete structure/interaction directions when the choice is material;
2. recommend one direction and explain the tradeoff in plain language;
3. obtain the owner's direction decision from a reviewable visual;
4. show the accepted journey's normal and material edge states before production implementation;
5. implement only the accepted direction;
6. show the running result and device evidence before declaring the family native-owned.

This is the default workflow for the rest of Phase 14.8. It prevents avoidable implementation churn and keeps the owner involved at decisions that materially change how the product feels, without reopening settled native business or security behavior.

## Checkpoints

### 14.8.1 — Native surface and dependency inventory: COMPLETE

- froze the Phase 14.7 build and test evidence as the behavioral oracle;
- mapped four authentication states and fourteen role-filtered management areas;
- grouped all 91 bridge-v5 actions by native owner and migration disposition;
- identified the reusable Kotlin read models and typed command services;
- confirmed there is no existing Compose dependency or Compose shell;
- recorded the launch, coexistence, parity, rollback, and bridge-retirement boundaries.

See `PHASE14_8_1_NATIVE_MIGRATION_INVENTORY.md`.

### 14.8.2 — Journey evidence and direction

- audit the current owner and staff journeys using the Phase 14 evidence and live app;
- establish the navigation and dashboard information hierarchy;
- compare a small number of relevant Android interaction directions;
- define normal, loading, empty, success, error, permission-denied, locked, offline, and destructive states;
- select one coherent direction before production screen implementation.

Checkpoint output: an approved native information architecture, complete representative journeys, and a design specification that can be implemented directly in Compose.

The current audit first compared three universal structures. Owner review established a stronger requirement: each role needs its own calibrated default, menu, density, action order, and feedback while preserving shared concepts and native permission truth. The role review then defined Owner, Administrator, Store manager, Cashier, and Operations staff; mapped every screen/action; and identified permission bundles that must be split or scoped. That policy is now accepted as the design foundation.

`PHASE14_8_2_CASHIER_OPERATIONS_DESIGN.md` records the first Cashier and Operations interaction wireframes. They specify navigation, field order, safe defaults, save feedback, error recovery, Back behavior, empty states, and supervisor approval boundaries. The owner explicitly rejected their rough presentation as an implementation blueprint. A high-fidelity Cashier sale direction must now establish the native visual world before the remaining journeys or production Compose work continue.

### 14.8.3 — Compose foundation

- enable the Compose compiler and a stable Compose BOM compatible with the project toolchain;
- create the native Activity/root, theme, typography, spacing, shape, color, and motion tokens;
- add reusable accessible controls and adaptive navigation;
- preserve the hybrid launcher/fallback until the vertical slice is accepted;
- add focused Compose tests for semantics, navigation, restoration, and large text.

### 14.8.4 — Native vertical slice

Implement startup/session handling, owner setup or login, dashboard, Sale, and the resulting account/history detail as one end-to-end native path. Prove native authorization, saved-result feedback, refresh, Back, draft protection, process restoration, accessibility, and actual Room results.

This is the first source-changing product gate. A weak vertical slice must be corrected before scaling the design across the remaining areas.

### 14.8.5 — Screen-family migration

Migrate in bounded families: shell/authentication; dashboard; daily work; deliveries; accounts; history/corrections; closing; master data/attendance; costing/planning/settings; access/Quick unlock; backup/cloud recovery.

Each family requires functional parity, permission parity, material UI states, accessibility, Android behavior, and device evidence before its WebView route becomes fallback-only.

### 14.8.6 — WebView and bridge retirement

Remove WebView, packaged assets, bridge transport, generated bridge projections, JavaScript runtime glue, and web-build enforcement only after every retained capability has a native caller and automated dependency scans report zero live WebView/bridge routes.

### 14.8.7 — Native regression and handoff

Run the complete host/device suite, native visual and interaction coverage, physical-device accessibility checks, backup/restore and Quick unlock acceptance, and an installed-artifact hash check. Only then hand off to Phase 15 uncoached shopkeeper QA.

## Frozen baseline

- Application ID: `com.saaf.milkshop.qa`
- Bridge contract: version 5, 91 actions
- Bridge SHA-256: `50CBB9E0DD21B703EBEB400DBF415BFD259A3A14DC1C0A5EF73D9EC056ECCBBA`
- Ordinary connected tests: 273/273
- Blank-install visual audit: 1/1
- Host tests: 7/7
- QA APK SHA-256: `5581218C0761ED3C33CA9D09877D19E4D2654B5B6DF5AC2790C80B364F223A24`

This baseline is a comparison point, not a visual target.

## Current next gate

Checkpoint 14.8.2 is now at native visual-direction review. `PHASE14_8_2_CASHIER_VISUAL_DIRECTIONS.md` compares three polished Cashier sale worlds using the same real content and workflow across Ready, Credit, Stock error, Approval, Saving, and Success. After the owner selects one, it will be carried across the complete Cashier and Operations journeys. The remaining roles and production Compose implementation wait behind those approvals.
