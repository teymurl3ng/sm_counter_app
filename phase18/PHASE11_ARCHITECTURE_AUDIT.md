# Phase 11 Architecture Audit

## Baseline
The Phase 10 checkpoint contains a large single `MainActivity` (~3k lines) and a single XML layout (~1.5k lines). Phase 11 is permitted to restructure the presentation layer because navigation and visual consistency require it.

## 11B decision
11B uses a low-risk transitional architecture:

```text
MainActivity
  ├─ app bar / navigation menu
  └─ existing section-based content in one ScrollView
       ├─ Dashboard
       ├─ Budgets & Targets
       ├─ Transactions
       ├─ History & Corrections
       ├─ People & Accounts
       ├─ Settings
       └─ Backup & Restore
```

Navigation currently changes the viewport rather than splitting business logic into fragments/activities. This avoids moving service calls, listeners, and transaction calculations until the navigation model has been proven.

## Data-sharing rule
During 11B, all existing domain service wiring remains where it is. No new shared mutable business state is introduced. Later destination extraction must define the presentation-state ownership explicitly before moving transaction logic out of MainActivity.

## Regression rule
The complete pre-existing instrumentation suite remains mandatory. A focused 11B test verifies that the configured default opening section resolves to its intended navigation anchor.


## 11B defect record
- Observed: `:app:compileDebugKotlin` failed with `Unresolved reference 'mainScrollView'` at `navigateToSection()`.
- Cause: the navigation code referenced the existing `ScrollView` by field name, but the Activity did not declare/bind that field.
- Fix: declare `lateinit var mainScrollView: ScrollView` and bind it with `findViewById(R.id.mainScrollView)`.
- Scope: presentation-layer compile fix only; no business logic, SQL, or navigation behavior changes.
- Environment note: local compilation remains blocked by the unavailable Gradle 9.3.0 distribution in the execution environment; Android Studio is the authoritative build verification.


## 11C audit update

The first rendered 11B screenshot provided concrete evidence that existing content inherited an unreadable text color against the light application background. This is a presentation-layer defect, not a business-logic defect. 11C corrects it locally on the Dashboard while introducing reusable card/surface treatment. The correction does not require changes to the accounting or data layers.

## 11C regression defect record
- Observed: `Phase11bNavigationUiTest.preferredSection_navigationStartsAtConfiguredAnchor_withoutWritingEvents` continued to fail because the configured Budgets section was not near the current viewport when the assertion ran.
- Exact cause: `navigateToSection()` runs during `onCreate()` before the target section is laid out. The previous fix used `target.height > 0 || mainScrollView.height > 0` as the readiness condition. The `ScrollView` could already have a non-zero height while the target still had its pre-layout `top == 0`, causing the immediate branch to scroll to the top and skipping the fallback.
- Fix: use `target.isLaidOut && mainScrollView.isLaidOut` as the immediate-path condition. Otherwise register a one-shot `OnGlobalLayoutListener`, compute the same anchor after layout, call `scrollTo()`, and remove the listener.
- Scope: presentation/navigation timing only. No business logic, SQL, calculations, persistence semantics, or test-only bypass was introduced.
- Verification limitation: the local environment still cannot obtain Gradle 9.3.0 because `services.gradle.org` is unreachable; Android Studio remains the authoritative build/instrumentation environment.


### Phase 11C navigation initialization correction
The configured opening-section navigation is now applied only after the existing asynchronous initial-state loading completes. This prevents dashboard/layout population from moving the target section after the initial scroll. No business logic, persistence semantics, or SQL behavior changed.


## Current 11C implementation status

The Dashboard is the reference implementation for the Phase 11 visual system. The shared semantic color resources established in 11A/11C are now also applied to legacy TextView/EditText content outside the Dashboard where the same inherited light-foreground defect was confirmed. This is a presentation-layer migration only; MainActivity business/domain behavior and the SQL/logic contracts remain unchanged.

## 11C widget-control contrast update
- Observed in rendered screenshots: the earlier contrast migration fixed ordinary TextView/EditText content, but Spinner values, RadioButton/CheckBox labels, and disabled button labels still inherited widget-specific/default colors and remained low-contrast on the light surface.
- Cause: these controls obtain visible text/tint from their own widget/adapter styling rather than the TextView/EditText foreground attributes already migrated.
- Fix: extend the existing semantic design tokens to those widget types; Spinner adapters now explicitly style selected/dropdown TextViews, choice controls use explicit text/tint selectors, and Buttons use an enabled/disabled text selector. No alternate color system was introduced.
- Scope: presentation/UI only.


## 11D-1 Purchase visual checkpoint

Purchase is the first transaction screen migrated using the established Phase 11 component styles. The implementation intentionally keeps the existing one-Activity wiring and view IDs for this checkpoint. The card/surface wrapper and style applications are presentation-only.

A secondary button text selector was added because the primary button text selector is intentionally specific to dark primary surfaces; secondary actions need the same disabled-state readability with a dark-on-light label.

## 11D-1 regression-test synchronization correction
The Phase11b navigation UI test was racing asynchronous initial-state loading. The production navigation sequence remains unchanged. The test now waits for the configured Budgets anchor and scroll position to become observable before asserting the existing navigation requirement, then performs the same assertions. No production business or navigation behavior was changed.


## 11D-1 purchase redesign audit
The Purchase screen remains on the existing Activity/view wiring for this checkpoint. The presentation layer was restructured within the existing purchase card to introduce semantic sections and aligned form groups without moving calculator/service calls or changing view IDs. No SQL, accounting, persistence, or transaction semantics changed.


## 11D transaction visual migration

The Purchase screen established the 11D visual pattern. The remaining transaction screens were migrated in one presentation-only batch so the same component discipline is applied consistently. No business/domain/service/SQL logic was moved or changed by this checkpoint. Any future restructuring of MainActivity remains limited to concrete presentation-layer architecture needs.


### Navigation diagnostic checkpoint
The Phase11b configured-opening-section test now captures scrollY, target top, layout readiness, and navigation-section label when the wait condition times out. No production navigation code is changed by this diagnostic checkpoint.


## 11E checkpoint audit

Presentation architecture changed: top-level functional areas are now isolated into independent screen containers. This removes the dependency on a single continuous ScrollView for navigation. MainActivity still owns the existing business logic in this checkpoint; further extraction of screen controllers remains a later presentation-only refactor if needed.


## 11F checkpoint audit

History & Corrections were visually restructured within the existing `MainActivity`/XML presentation architecture. The records destination now uses separate history, detail, and correction surfaces while retaining the existing view IDs, clickable history spans, correction editor visibility rules, and domain-service wiring.

No business/domain/service/SQL logic was moved or changed. The new labels and styles are presentation-only. A focused UI smoke test was added for the new surface structure. Local Gradle verification remains blocked by unavailable Gradle 9.3.0/network access; Android Studio is authoritative.


## 11F.1 review

- System-bar inset handling is centralized at the app root instead of duplicated across screens.
- Performance Dashboard period controls use a 2×2 layout to preserve legibility on narrow screens.
- No business/domain/persistence layer changes were made.

### 11G checkpoint audit — Budgets & Targets

The 11G checkpoint is presentation-only. The Budgets & Targets destination remains wired to the existing `BudgetManagementService` and existing view IDs. The redesign introduces only additional presentation containers, labels, shared styles, and surface hierarchy. No domain/service/database code was changed.

The existing Phase 8 budget/target instrumentation suite remains the behavioral authority for this destination.

## 11H checkpoint audit — Management & Settings

The 11H work remains presentation-only within the established one-Activity/screen-host architecture.

- People & Accounts now presents Vendor, Customer, and Employee management as separate surface cards inside the existing management destination.
- Settings now presents Business Settings and App Preferences as separate surface cards inside the existing settings destination.
- Existing domain services remain authoritative: `VendorManagementService`, `CustomerManagementService`, `EmployeeManagementService`, and `BusinessSettingsService`.
- Existing preference storage remains the `milk_shop_preferences` SharedPreferences store.
- Existing view IDs used by domain/UI tests were preserved; new IDs are presentation containers only.
- No SQL schema, accounting rule, account identity rule, transaction semantics, persistence behavior, or event-writing behavior changed.

A focused `Phase11hManagementSettingsUiTest` was added for the composed-surface structure. Local Gradle/instrumentation execution remains subject to the same environment limitation; Android Studio is the authoritative runtime verification environment.

## Presentation Boundary Hardening — People & Accounts extraction

The 11H pre-overhaul checkpoint was used as the baseline. The first presentation-boundary extraction targets only the People & Accounts management UI because it is the most self-contained management destination.

### Extracted
- Added `presentation/management/PeopleAccountsUi.kt` as a presentation-only owner for Vendor, Customer, and Employee management rows, empty states, management dialogs, and management error-display state.
- `MainActivity` now owns only the existing management operation callbacks and existing refresh/service calls for this destination.
- Existing service operations remain in `MainActivity` and continue to use `VendorManagementService`, `CustomerManagementService`, and `EmployeeManagementService` exactly as before.
- Existing screen IDs and management list/error view IDs are unchanged.

### Deliberately not changed
- No Room/database code.
- No SQL/schema code.
- No domain/service implementation.
- No accounting, transaction, calculation, validation, persistence, or event semantics.
- No navigation architecture.
- No UI technology/framework migration.

### Verification contract
This extraction is intended to be behavior-neutral. The existing `Phase11hManagementSettingsUiTest` remains applicable to the same management/settings views, and the pre-existing Phase 6 vendor/customer/employee management instrumentation tests remain the behavioral authorities for these operations. Android Studio remains the authoritative environment for instrumentation execution because the local environment cannot obtain Gradle 9.3.0.

### Presentation Boundary Hardening — Settings / App Preferences extraction

The App Preferences dialog presentation was extracted from `MainActivity` into `presentation/settings/AppPreferencesUi.kt`.

This extraction preserves the existing preference keys, defaults, radio-group values, persistence semantics, dialog actions, and post-save recreation behavior. `MainActivity` remains responsible for applying the saved application state after the dialog is accepted. No business logic, Room/database code, navigation, or preference semantics were changed.


### Presentation Boundary Hardening — Backup & Restore extraction

The Backup & Restore destination is the next presentation-only extraction after the verified People & Accounts and Settings/App Preferences extractions. Dialog construction and selection/confirmation state now live in `presentation/backup/BackupUi.kt`.

`MainActivity` retains the existing `BackupManager` calls, activity-result contracts, restore side effects, dashboard/history/budget/settings refresh behavior, and automatic local-backup behavior. No backup semantics or data-layer behavior was changed.

This checkpoint is behavior-neutral by design and should be verified in Android Studio before another screen extraction is attempted.

### Boundary Hardening — Budgets

People & Accounts and Settings/App Preferences are user-verified extractions. Backup & Restore is extracted, with the known navigation-test contract corrected. Budgets & Targets is now extracted into `presentation/budgets/BudgetUi.kt`; runtime verification is pending.


### Incremental Presentation Boundary Work

History & Corrections presentation responsibilities are isolated in `presentation/records/HistoryCorrectionsUi.kt`. Database queries, correction construction, and `CorrectionService` execution remain in `MainActivity` for this step; the extracted class owns the view wiring, rendering, editor presentation, correction-form state capture, and callbacks.

### Transactions presentation boundary — wiring extraction

The transaction screens now have a dedicated `TransactionsUi` presentation-wiring class. This checkpoint intentionally moves only click/listener/watcher registration and keeps existing transaction operation handlers unchanged in `MainActivity` so behavior can be verified before deeper extraction.

### Transaction Operation Handler Pilot
First transaction operation extraction: Purchase save handler moved to `PurchaseTransactionAction` with explicit UI/state callbacks to preserve existing behavior. Remaining transaction save handlers remain in MainActivity pending verification of this pilot.


### Transaction operation-boundary extraction — simple handlers

The five remaining simple transaction save handlers now live in `presentation/transactions/SimpleTransactionActions.kt`. `MainActivity` retains the existing state/data loading and shared screen coordination while delegating these save operations through explicit callbacks. The extraction uses selected-account/provider callbacks rather than exposing the Activity's private option-model types.

Behavior-preservation checks confirmed the same service calls, inputs, automatic-backup ordering, dashboard/history refresh ordering, success/error UI updates, and input clearing as the pre-extraction handlers. No domain/data implementation was changed.


## Current extraction checkpoint

Dashboard presentation is now isolated in `DashboardUi`. MainActivity retains only a thin `refreshDashboard()` delegation for existing callers.

### Presentation Boundary Hardening — Batch 1: Transaction Read/State

The transaction presentation layer now has a dedicated `TransactionStateUi` owner for read-side loading and derived UI state. `MainActivity` no longer owns the transaction option-model collections or the transaction preview/action-state implementations.

The extraction deliberately does not introduce a new domain abstraction. Existing Room queries and existing domain-service reads are preserved exactly; the current presentation class remains the consumer boundary for this step. Direct DAO access from presentation is therefore still an identified later boundary concern, not silently redefined here.

No SQL, Room schema, domain-service implementation, accounting behavior, transaction semantics, calculations, or persistence behavior changed.

## Presentation Boundary Hardening — Batch 2: Records & Management Orchestration

The grouped Batch 2 extraction moves two coherent remaining presentation/application responsibilities out of `MainActivity`:

- `HistoryCorrectionsActions` now owns History read-side loading, transaction-detail loading/building, and history/detail formatting. Existing DAO queries and output formatting are preserved.
- `PeopleAccountsActions` now owns the existing Vendor/Customer/Employee create and rename orchestration, including the exact existing service calls, UI error/status updates, reloads, and dashboard refreshes.

The correction save operation is intentionally not moved in this batch because it contains the existing construction/validation of `CorrectionInput`; it remains in `MainActivity` pending a separate exact-cause/behavior-preservation review. App-shell and direct-DAO boundary work likewise remain for Batch 3.

No SQL, schema, Room implementation, domain-service implementation, accounting rule, transaction semantic, calculation, persistence behavior, or UI technology was changed.


---

## Batch 3 Audit

`MainActivity` no longer directly accesses Room DAOs. Application-shell UI concerns extracted: saved theme application, display density application, window insets, navigation, preferred opening section, and shared spinner adapter creation. The correction flow still constructs and executes the existing `CorrectionInput`/`CorrectionService` path; only the post-correction event lookup moved behind `HistoryCorrectionsActions`.

## First-Use Setup Wizard — planning only

A future first-use setup wizard has been added to the Phase 11 roadmap as a planning placeholder only. The scope is intentionally undecided. It is expected to collect initial business/setup information needed for first use (for example shop identity, opening milk quantity/stock, opening money/balances), but no specific field set or persistence mapping is approved yet.



## Final boundary audit — current verified checkpoint

### Findings

The current `MainActivity` is approximately 989 lines, down from the original ~3,139-line presentation monolith. The application has dedicated presentation classes for dashboard, budgets, backup, settings/preferences, people/accounts, history/corrections, transaction wiring, transaction state, and transaction actions.

The final audit found no remaining direct Room DAO access from `MainActivity`. Remaining direct `database` references are used to provide the existing database instance to already-separated components or to preserve existing service-backed application operations (for example business settings persistence, correction execution, and backup/restore orchestration). These are intentionally not reworked further because no additional boundary requirement has been established and the existing separation is sufficient for the next architectural decision.

### Decision

Presentation-boundary hardening is considered complete for the current scope. No further extraction is required before selecting the future UI presentation technology. Any future extraction must be justified by a concrete requirement or newly established defect rather than by file size alone.


## ViewModel Architecture Preparation

Status: Purchase pilot verified; Dashboard + Sale + History/Corrections batch in progress.

`PurchaseViewModel` is the verified first AndroidX ViewModel introduced after completion of presentation-boundary hardening. The current batch adds `DashboardViewModel`, `SaleViewModel`, and `HistoryCorrectionsViewModel` as presentation-state holders. No database, business-rule, transaction-semantics, XML-layout, or UI-technology changes are part of this preparation.

This is not a new numbered roadmap phase. Its purpose is to establish a stable presentation-state layer before Phase 11I/11J UI work.


## ViewModel Architecture Preparation — Settings and Backup

The ViewModel preparation now includes Settings and Backup/Restore presentation state. SettingsViewModel owns business-setting display state and the App Preferences draft without changing existing persistence keys or semantics. BackupViewModel owns restore busy/status presentation state; BackupManager and restore behavior remain authoritative. XML layouts and UI technology remain unchanged. Android Studio verification of this checkpoint is pending.
