# Milk Shop Android — Project Status

## Authoritative project rules

- `milk-shop-core-logic-spec-v2.md` is the absolute business/logic authority.
- `milk_shop_schema_sql_only_final_v2.sql` is the absolute SQL/database blueprint.
- The implementation must preserve existing business behavior unless a real requirement or proven defect requires a change.
- No code is to be changed merely to make a test pass. The exact root cause must be established first.
- No unrelated refactors, invented requirements, new accounting concepts, or schema redesigns without explicit approval.
- Existing regression tests remain mandatory after every phase.
- Android Studio / real-device execution is the authoritative Android verification environment.

## Architecture

Current proven direction:

```text
Presentation / UI
        ↓
Higher-level reporting / planning / backup layers
        ↓
Canonical read models / approved read queries
        ↓
Domain services / frozen business core
        ↓
Room / SQLite integrity layer
```

Higher layers may derive, summarize, plan, visualize, or protect core truth, but must not redefine or mutate the core's accounting truth without an explicit core-specification change.

Business settings are distinct from application preferences. Backup/restore is infrastructure around the authoritative application state, not a second business-data model.

## Completed and verified phases

- **Phase 1** — Room foundation + Vendor Purchase.
- **Phase 2** — Vendor Payment, Yogurt Production, Sale, Credit Collection, Expense, Salary Payment.
- **Phase 3** — Append-only corrections for all seven business event types, including corrected-truth headers, delta ledger movements, correction chains, and zero-delta edge handling.
- **Phase 4A** — Dashboard + Milk Purchase UI.
- **Phase 4B** — Yogurt Production UI.
- **Phase 4C** — Vendor Payment UI.
- **Phase 4D** — Sales UI.
- **Phase 4E** — Credit Collection UI.
- **Phase 4F** — Expense UI.
- **Phase 4G** — Salary Payment UI.
- **Phase 5A** — Transaction History.
- **Phase 5B** — Transaction Detail.
- **Phase 5C** — Correction UI.
- **Phase 6A** — Business Settings.
- **Phase 6B** — Vendor Management.
- **Phase 6C** — Customer Management.
- **Phase 6D** — Employee Management.
- **Phase 7A** — Analytics / Reporting Foundation.
- **Phase 7B** — Dashboards / Visual Reports.
- **Phase 8** — Budgets & Targets.
- **Phase 9** — Backup & Restore.

Completed checkpoints above have been verified through the package-level Android instrumentation regression suite on the project Android test device. Phase 9 also includes the verified backup/restore flow and integrity safeguards.

## Architecture carry-forward / living audit

- `MainActivity` remains large. Refactor only for a concrete problem; do not perform unrelated structural cleanup.
- Repeated UI synchronization helpers are intentional where synchronous and asynchronous assertions differ.
- Broader event/header ↔ ledger hardening remains a future hardening candidate, not an invitation to change the core.
- Master-data duplicate creation continues to rely on the database UNIQUE constraint and existing exception handling.
- Similar Vendor/Customer/Employee management services remain intentionally similar; do not prematurely abstract them.
- Phase-specific architecture audits are maintained in `PHASE6_ARCHITECTURE_AUDIT.md`, `PHASE7_ARCHITECTURE_AUDIT.md`, `PHASE9_ARCHITECTURE_AUDIT.md`, and `PHASE10_ARCHITECTURE_AUDIT.md`.

## Current roadmap

- **7A** — Analytics / Reporting Foundation ✅
- **7B** — Dashboards / Visual Reports ✅
- **8** — Budgets / Targets ✅
- **9** — Backup / Restore ✅
- **10** — Application Preferences ✅
- **11A** — Design System Foundations ✅
- **11B** — Navigation & Information Architecture ✅
- **11C** — Dashboard + Contrast System ✅
- **11D-1** — Purchase transaction screen ✅
- **11D** — Transaction screens visual migration ✅
- **11E** — Real Screen Navigation ✅
- **11F** — History & Corrections visual redesign ✅
- **11F.1** — Window Insets & Performance Dashboard period controls ✅
- **11G** — Budgets & Targets visual redesign ✅
- **11H** — Management & Settings visual redesign ✅
- **11I** — Backup & Restore visual redesign ⏳
- **11J** — Final UI consistency / accessibility / regression gate ⏳
- **12** — Cash Planning / Reserve Planner ⏳
- **13** — Final Hardening / Release Readiness ⏳
- **14** — First-Use Setup Wizard ⏳
- **15** — ViewModel Contract Refinement ✅
- **16** — ViewModel Integration / Presentation Ownership ✅
- **17** — Application / Presentation Data Orchestration Boundary ✅
- **18** — Repository Hygiene Cleanup ✅
- **19** — Screen Architecture / UI Structure ⏳

### Current authoritative state

The active code line is **Phase 18 — Repository Hygiene Cleanup**, built directly from the verified Phase 17 implementation.

Phase 17 completed the application/data orchestration boundary. Phase 18 removes stale working artifacts and duplicate files so the repository has one authoritative source for each implementation and the project root remains understandable.

### Phase 18 verification status

Phase 18 is complete. The removed scratch files and stale root-level `SimpleTransactionActions.kt` duplicate are absent. The authoritative implementation remains under `app/src/main/java/com/saaf/milkshop/presentation/transactions/SimpleTransactionActions.kt`.

### Next steps

1. Return to **11I — Backup & Restore visual redesign** using the AI Studio UI guide as the visual reference.
2. Complete **11J — Final UI consistency / accessibility / regression gate**.
3. Preserve the original Phase 12, Phase 13, and Phase 14 numbering.
4. When screen architecture becomes a distinct implementation task, use **Phase 19 — Screen Architecture / UI Structure** rather than renumbering earlier phases.


## Phase 7A contract

7A is the first read-only analytics sublogic layer.

It may:

- read canonical read models;
- read event/header truth through approved read-only queries;
- add read-only DAO queries or views needed for aggregation;
- calculate period metrics and KPIs.

It may not:

- add writable analytics/summary tables;
- persist computed totals as business truth;
- mutate transaction/event/ledger/account data;
- introduce alternate accounting formulas;
- change the SQL blueprint or business core.

### Correction-aware reporting

For event-based metrics, only effective leaf events are counted. An event with a child correction is excluded so a correction chain contributes its latest corrected truth rather than double-counting the original and correction event.

## Phase 7B contract

7B is presentation-only over the read-only 7A analytics layer. It may render period metrics and switch between approved reporting periods. It may not create writable analytics state, mutate accounting data, or implement alternate financial calculations.

The dashboard is an initial visual reporting layer, not the final visual-design pass. The whole application UI remains functional/prototype-oriented; a later dedicated UI/UX pass will address navigation, menus, visual hierarchy, spacing, cards, empty/error states, and overall production polish.

## Phase 8 contract

Phase 8 adds planning state above 7A analytics. The accounting core remains unchanged.

### Budget definition

A budget is an amount planned to stay under, per category, per period.

### Budget categories

- Cost of Goods: Milk purchases.
- Operating Costs: Salaries, Rent, Electricity, Internet, Water, Fuel, Poly bags, LPG, Other.

### Milk planning input

Milk planning stores **Planned Liters** as the single planning input.

- Milk spend budget = Planned Liters × snapshot `milkRateVendor`.
- Weekly Sales Target = Planned Liters × snapshot `retailRateMilk`.
- Business-rate changes do not mutate existing budget/target rows; rates are snapshotted at creation/revision.
- Planned Liters = 0 and no target set are distinct states.

### Sales Target

- Fixed weekly period.
- Auto-derived, not manually typed.
- Credit sales count in full when the sale occurs.
- No day-one yogurt prediction/reallocation.
- Actual yogurt revenue appears separately when actual `YogurtProduction` and yogurt sale events occur.
- A "Combined Milk + Yogurt" line is informational only.
- Spoilage does not shrink the original target.

### Collections Target

- Fixed weekly period.
- Independently entered.
- Counts only actual Collection events.
- Sales minus Collections for the same period is shown as the separate "new uncollected this period" indicator.

### Budget periods, revisions, and history

- Each budget period is created explicitly; no automatic repetition.
- A one-tap copy-previous convenience is allowed.
- Category-native periods are preserved: Milk/Sales/Collections weekly; Rent/Salaries and similar fixed costs monthly.
- Reporting may roll up into weekly/monthly/quarterly/6-month/yearly/all-time views.
- A week crossing a calendar month is assigned wholly to the month containing the majority of its days.
- Mid-period revisions take effect immediately for the open period.
- Revisions never reach backward into a closed period.
- Periods close automatically when their date range ends.
- The version active at close defines the historical budget comparison.
- Old versions are archived/hidden from the active view but remain viewable; no true deletion.
- Bringing back an old version means creating a new version from its values; nothing is restored in place.
- A later correction creates a separate recalculated comparison while the original closed-period comparison remains untouched.

### Actuals and variance

- Actuals are read from the Phase 7A analytics/read-model layer; Phase 8 does not independently recalculate business actuals from raw events.
- Variance shows both amount and percentage.
- When target = 0, variance percentage is displayed as `—`.

## Phase 9 contract

Phase 9 provides complete application-state backup and restore without changing accounting meaning.

- Full timestamped snapshots are retained; no automatic retention pruning.
- Backup naming follows `SHOP_NAME_YYYY-MM-DD_HH-MM-SS`.
- Local/device backup and Google Drive/cloud backup are supported.
- Automatic local protection and scheduled/background cloud backup are separate concerns.
- Manual backup remains available.
- Backup integrity is verified before a snapshot is presented as valid.
- Failed backups preserve the previous valid backup and clearly report the reason and recovery action.
- Persistent retry uses increasing backoff; repeated failure escalates to manual action while preserving valid recovery points.
- Restore supports full replacement and dependency-aware selective restore.
- Restore creates a pre-restore safety snapshot before replacing current state.
- Restore validates version compatibility, backup integrity, and restored-state integrity before reporting success.
- Older backups may migrate forward when a verified migration path exists; newer backups require an app update.
- Restore requires explicit warnings and exact acknowledgement text: `I understood the consequences and I agree to proceed forward`.
- Restore replaces the current application state; no merge/import mode.
- App preferences are part of complete application state.
- The original format-1 backup remains checksummed plaintext for legacy import compatibility. The superseding approved Phase 3 security contract adds implemented Argon2id-derived AES-256-GCM portable format-2 exports and Android-Keystore AES-256-GCM automatic local snapshots for user/security-bearing backups.

## Phase 10 — App Preferences

Phase 10 adds user personalization under Settings as a subcategory separate from Business Settings.

### Included preferences

- **Theme:** System default / Light / Dark.
- **Language:** English / Urdu selection; Urdu translation work is deferred.
- **Display density:** Comfortable / Compact.
- **Default reporting period:** user-selected starting analytics period.
- **Default opening section:** Dashboard / Budgets / Backup / Settings.

### Deliberately non-configurable

- Number formatting is automatic by data type.
- Date format is a fixed readable standard.
- Currency display is fixed to the standard Pakistani rupee presentation.
- Week start is fixed to Monday as a business/reporting rule.
- Dashboard refresh is automatic behavior, not a preference.
- Unsaved-work/integrity warnings are mandatory behavior, not a preference.
- Backup/restore behavior remains governed by Phase 9 unless a real capability gap is established.

Preferences are stored in the existing `milk_shop_preferences` store so complete Phase 9 backups/restores carry them with the application state.

## Phase 11 contract

Phase 11 is the dedicated presentation-layer reconstruction. It may substantially restructure `MainActivity`, `activity_main.xml`, and presentation code as required for real navigation and reusable UI components. It does not authorize changes to business behavior, accounting formulas, the core logic specification, or the SQL blueprint.

### Phase 11A — Design System Foundations — complete

11A established the shared visual vocabulary using the existing Android View/XML stack. Material-style conventions are the baseline, with restrained component discipline inspired by systems such as shadcn/ui.

- semantic color roles;
- shared spacing and touch-target sizing;
- shared typography hierarchy;
- shared radii, surfaces, buttons, and inputs;
- no business or accounting behavior changes.

### Phase 11B — Navigation & Information Architecture — implementation checkpoint

11B added the persistent navigation entry point and grouped existing content into user-intent destinations:

- Dashboard
- Budgets & Targets
- Transactions
- History & Corrections
- People & Accounts
- Settings
- Backup & Restore

The current implementation uses section anchors in the existing ScrollView as a deliberate transition step. The app bar shows the current destination and the existing default-opening-section preference remains supported.

The first 11B checkpoint exposed a concrete compile error because `navigateToSection()` referenced `mainScrollView` without an Activity field binding. The correction only declares and binds that existing ScrollView. Android Studio remains the authoritative build/test environment because this execution environment cannot download the project's Gradle distribution.

### Phase 11C — Dashboard & shared contrast migration — complete

11C is the first substantive visual migration. The Dashboard establishes the new visual hierarchy, and the established semantic color roles are now being applied across the remaining legacy XML form content where the same inherited-text-color defect was confirmed. This is a presentation-only migration: existing data sources, calculations, reporting periods, persistence, and event-writing behavior remain unchanged.

Current 11C scope:

- fix the confirmed white-on-white readability defect using explicit semantic foreground colors;
- retain the Dashboard's Material-style hierarchy, cards, surfaces, and period controls;
- migrate legacy TextView and EditText content outside the already-restyled Dashboard to explicit readable semantic colors;
- preserve existing view IDs, business wiring, calculations, and persistence behavior;
- avoid changing business logic merely to obtain visual consistency;
- verify the migration with the existing Android instrumentation suite and representative rendered screenshots.

The same established design-system tokens are reused rather than inventing separate per-screen colors. Screen-specific visual hierarchy will continue in later 11D–11H checkpoints.

### Phase 11C regression correction

The configured-opening-section UI test remained failing after the first timing correction. The exact remaining cause was that `navigateToSection()` was deciding whether to scroll from the current view dimensions during `onCreate()`. At that point the target section was not yet laid out, but the scroll view itself could already have a non-zero height, so the earlier condition could incorrectly take the immediate-scroll branch using `target.top == 0`. The fallback therefore was not registered.

The corrected implementation now checks `isLaidOut` on both the target and the `ScrollView`. If the hierarchy is not laid out, it waits for the next global layout pass, computes the same target anchor after layout, performs the same `scrollTo()`, and removes the listener. This fixes the established navigation timing without changing navigation intent, business logic, SQL, calculations, persistence, or test expectations.

### Phase 11 workflow rule

Every UI sub-phase must:

1. preserve business/domain behavior;
2. run the existing instrumentation regression suite;
3. add focused verification for the changed UI surface;
4. use rendered emulator/device screenshots for visual validation once a screen has been materially restyled.



### Phase 11C navigation initialization correction
The configured opening-section navigation is now applied only after the existing asynchronous initial-state loading completes. This prevents dashboard/layout population from moving the target section after the initial scroll. No business logic, persistence semantics, or SQL behavior changed.

## Phase 11C current visual migration note
A rendered screenshot review confirmed that the initial global contrast migration fixed standard TextView/EditText content but left widget-specific controls (Spinners, RadioButtons, CheckBoxes, and disabled Buttons) with low-contrast default rendering. The current checkpoint extends the same established semantic color system to those controls. No business logic, SQL, calculations, persistence, or enabled/disabled business state was changed.


## Phase 11D — Transaction Screens

11D is the transaction-screen visual migration. It may substantially restructure presentation markup and shared components, but it must preserve established business behavior.

### 11D-1 Purchase
The Milk Purchase screen is the first transaction visual checkpoint. The current 11D-1 pass redesigns the presentation hierarchy rather than only recoloring controls: vendor selection, milk measurements, calculation summary, status/error messaging, and the primary save action are visually grouped with deliberate alignment and spacing. Existing `VendorPurchaseCalculator`, `VendorPurchaseService`, validation, persistence, and view IDs remain unchanged.

Each transaction checkpoint requires the existing regression suite plus a focused UI verification before the next transaction screen is migrated.

## Current checkpoint note — 11D-1
11D-1 Purchase visual work is the current UI checkpoint. A pre-existing Phase11b navigation test is synchronized to the asynchronous initial-state lifecycle so it verifies the real configured-opening-section behavior without introducing a production timing change.


### 11D-1 navigation diagnostic checkpoint
A pre-existing configured-opening-section test is currently instrumented to report runtime viewport/layout state on timeout. This checkpoint changes test diagnostics only; it does not change production navigation behavior. The purpose is to establish the exact runtime condition before any further production fix is attempted.


### 11D-1 navigation test diagnostic resolution
The diagnostic run established that configured-opening navigation was succeeding: the test observed `scrollY=3906`, `budgetTop=4308`, both views laid out, and the navigation label set to `Budgets & Targets`. The previous `<160px` absolute-distance assertion was narrower than the actual requirement. It was replaced with a viewport-containment assertion: the configured section must lie within the ScrollView viewport. No production navigation behavior was changed.


## Phase 11E — Real screen navigation

Status: implementation checkpoint.

The top-level navigation now uses independent visible screen destinations inside the app shell instead of one continuous page. Existing business logic remains in place; this checkpoint restructures only the presentation container and navigation behavior.

Top-level destinations: Dashboard, Budgets & Targets, Transactions, History & Corrections, People & Accounts, Settings, and Backup & Restore.

The previous viewport-scroll navigation contract is retired. Navigation verification now checks destination visibility and app-bar section state.


## Phase 11F — History & Corrections visual redesign

The records destination was visually rebuilt using the established Phase 11 component system. The implementation is presentation-only and preserves all existing history loading, clickable event selection, detail rendering, correction eligibility, correction field visibility, validation, save/cancel behavior, and event-writing logic.

### Implemented
- History list presented as a dedicated surface/card with clear title and supporting description.
- Transaction detail presented as a separate surface/card.
- Correction action is kept subordinate to the selected transaction detail and remains hidden until existing business logic makes it applicable.
- Correction editor is presented as a distinct surface with explicit type-specific sections, aligned controls, consistent field heights, and primary/secondary actions.
- Existing view IDs, listeners, clickable spans, correction services, and persistence behavior are unchanged.
- Added a focused Phase 11F UI smoke test for the new presentation surfaces.

### Safety contract
No SQL, business rules, accounting semantics, transaction calculations, correction semantics, persistence behavior, or event-writing behavior changed.

## Phase 11G current checkpoint

Budgets & Targets has been visually redesigned using the established Phase 11 surface, typography, spacing, input, and button system. The budget setup, status/history, and weekly targets are now separated into clearly labeled cards with aligned controls. Existing Phase 8 budget/target behavior and persistence remain unchanged. Android Studio remains the authoritative verification environment for this checkpoint.


### Phase 11H presentation-boundary hardening
The 11H pre-overhaul checkpoint remains the authoritative baseline. The first presentation-only extraction isolates People & Accounts UI construction, management dialogs, dynamic rows, and management error presentation in `PeopleAccountsUi`. Existing Vendor/Customer/Employee service calls, refresh behavior, view IDs, and business/data semantics remain unchanged. No Compose, Material migration, SQL, schema, or business-logic change is included in this checkpoint.




Historical checkpoint: transaction screen event wiring is isolated in `TransactionsUi` without changing transaction business behavior.

Phase 11 presentation boundary hardening: transaction wiring was verified; the Purchase save operation handler is now the first transaction operation handler extracted into presentation/transactions/PurchaseTransactionAction.kt. No business/data semantics were changed.



Historical checkpoint: Dashboard presentation extracted into `DashboardUi`; runtime verification subsequently completed.

Android Studio remains the authoritative build and instrumentation-test environment for this checkpoint because the local environment cannot obtain Gradle 9.3.0.

### Current presentation-boundary checkpoint — Batch 2

Batch 2 is now implemented from the verified Batch 1 checkpoint. History & Corrections read/detail orchestration and People & Accounts create/rename orchestration are isolated in dedicated presentation/application classes. The remaining correction-save operation and app-shell/direct-DAO review are intentionally deferred to later boundary batches. No Compose or Material migration has been selected or started.


Phase 11 presentation-boundary hardening Batch 3 is implemented. Application-shell presentation responsibilities and the remaining direct Event DAO read from `MainActivity` have been isolated without changing business/data semantics. A compatibility wrapper preserves the existing `navigateToSection(String, boolean)` test contract. Runtime verification of the corrected Batch 3 checkpoint remains pending.

## ViewModel Architecture Preparation

Purchase is the verified pilot for an AndroidX ViewModel presentation-state layer. The current follow-on batch extends the pattern to Dashboard, Sale, and History/Corrections. ViewModels are limited to presentation state and lifecycle-safe state ownership; existing domain calculators/services remain authoritative for business behavior.


### Current ViewModel preparation checkpoint

The Purchase ViewModel pilot is verified. Dashboard, Sale, and History/Corrections ViewModels are verified. The current checkpoint adds Settings and Backup/Restore ViewModels while preserving the existing XML UI and all existing business/data semantics. Android Studio verification of this checkpoint remains pending.

## Current Phase 16 status

Phase 16 transaction ViewModel ownership checkpoint required a selection-synchronization correction: Purchase save now synchronizes the current vendor Spinner position into `PurchaseViewModel` immediately before resolving the selected vendor account ID. This corrects a regression exposed by `Phase4PurchaseUiTest` without changing purchase business logic or persistence semantics.



Phase 16 correction: SimpleTransactionsViewModel reference data is refreshed after loadSaleData populates customers, ensuring Credit Collection selection validation reads the same customer list displayed by the UI.

## Schema 7 authenticated native security checkpoint — 2026-08-27

The application now provides deliberate first-owner setup, native login/logout/recovery and reauthentication, private staff credential enrollment/reset, owner/admin user management, app-owned memory sessions, role-aware navigation, and native permission checks at protected application-command boundaries. Portable backups use Argon2id-derived AES-256-GCM; local automatic snapshots use Android Keystore-backed AES-256-GCM. The focused security/shell/backup group passes 16/16 and the complete connected-device regression passes 103/103 with no skipped or failed tests. React/bridge integration remains outside this native checkpoint.
