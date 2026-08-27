# Phase 11 — UI / UX Overhaul

## 11C — Dashboard & shared contrast migration

### Objective

Apply the first substantive visual migration to the Dashboard while preserving all established business behavior and the existing dashboard data/reporting logic.

### Implemented

- Rebuilt the dashboard presentation hierarchy around explicit sections and reusable card surfaces.
- Added explicit semantic dark-on-light text roles for dashboard content so device/theme defaults cannot make the body unreadable.
- Restyled the four analytics period controls with explicit high-contrast primary colors, non-uppercase labels, and equal-width controls.
- Restyled analytics report cards with consistent padding, borders, radius, and readable body text.
- Applied the same established semantic foreground colors to legacy TextView and EditText content outside the Dashboard where the same inherited-color defect was confirmed.
- Kept the migration at the presentation layer: no service, query, calculation, persistence, period semantics, or event-writing behavior was changed.

### Screenshot-driven defect addressed

The rendered 11B/11C screenshots exposed a real presentation defect: legacy body content was inheriting a light/white foreground while the application surface remained light, making large parts of the forms unreadable. The Dashboard already established the correct semantic color roles; this checkpoint extends those same roles to the remaining legacy XML TextView/EditText content rather than creating a second color system.

### Verification

Static XML validation and duplicate-ID checks pass. The local execution environment cannot run the Android Gradle build because Gradle 9.3.0 is not available locally and the environment cannot reach `services.gradle.org`. Android Studio remains the authoritative build and instrumentation-test environment for this checkpoint.

### Safety contract

- No SQL schema or SQL blueprint changes.
- No accounting/business-rule changes.
- No changes to existing calculations or analytics semantics.
- No new persistence or data model behavior.
- Presentation-layer changes only.

### 11C regression correction — configured opening-section timing

Android Studio exposed a regression in the existing 11B navigation test: the configured Budgets opening section was not near the viewport when the assertion executed.

Exact cause of the remaining failure: `navigateToSection()` was called during `onCreate()`, before the target section had been laid out. The previous timing fix checked `target.height > 0 || mainScrollView.height > 0`; because the `ScrollView` itself could already have a non-zero height while `target.top` was still `0`, the code could take the immediate branch and permanently scroll to the top.

Correction: the navigation code now uses `isLaidOut` on both the target and `ScrollView`. Before layout it registers a one-shot global-layout listener; once the hierarchy is laid out, it computes the same target anchor and performs the same `scrollTo()`, then removes the listener. When the hierarchy is already laid out, the existing immediate path remains. This is a deterministic presentation-layer timing correction only; it does not alter which section is selected or any business behavior.


### Phase 11C navigation initialization correction
The configured opening-section navigation is now applied only after the existing asynchronous initial-state loading completes. This prevents dashboard/layout population from moving the target section after the initial scroll. No business logic, persistence semantics, or SQL behavior changed.

### 11C widget-control contrast migration
A rendered screenshot after the TextView/EditText migration showed a narrower remaining presentation defect: Spinner values, RadioButton/CheckBox labels, and disabled button labels were still using widget/default colors that could become effectively white on the light surface.

Correction: reuse the established semantic color system for these control types. Spinner adapters now render selected and dropdown text with `on_surface` on the standard surface and use a visible outline tint. RadioButton/CheckBox text uses `on_surface` with semantic choice-control tint. Buttons use an enabled/disabled text-color selector so disabled actions remain legible without changing their business enabled/disabled state.

This is presentation-only. No business logic, SQL, event-writing behavior, calculations, or persistence semantics changed.


## 11D — Transaction Screens

### 11D-1 Purchase — first transaction visual checkpoint

Redesigned the Milk Purchase screen using the Phase 11 component system while preserving the existing transaction behavior.

- Rebuilt the visual hierarchy into explicit Vendor, Milk measurements, and Calculation preview groups.
- Added aligned section labels so fields no longer read as floating, unlabeled controls.
- Standardized field heights, vertical rhythm, and horizontal alignment.
- Made the calculation summary a distinct surface with a clear title, explanatory subtitle, and aligned result rows.
- Kept the primary Save action visually dominant and the Add Vendor action secondary.
- Preserved all existing purchase view IDs and event wiring.
- Preserved the existing VendorPurchaseCalculator, VendorPurchaseService, validation, settings snapshots, persistence, and status/error behavior.
- Reused the established Phase 11 semantic colors and shared styles; no new business-specific color system was introduced.

Safety contract: presentation-layer changes only; no SQL, accounting formulas, event semantics, persistence behavior, or business rules changed.

### 11D — Transaction screen visual migration

Applied the 11D-1 Purchase visual hierarchy to the remaining transaction screens in one presentation-only pass: Yogurt Production, Vendor Payment, Sale, Credit Collection, Expense, and Salary Payment. Each screen is now presented as a consistent surface card with shared spacing, aligned controls, readable widget states, dominant primary actions, subordinate secondary actions, and consistent section typography. Sale retains its existing calculation preview and choice controls; all existing view IDs and event wiring remain intact.

No business logic, accounting formulas, validation semantics, SQL/schema, persistence behavior, or event-writing behavior was changed.

### 11D-1 verification note
A pre-existing Phase11b navigation instrumentation test was failing because the test asserted immediately after Activity launch while `loadInitialState()` runs asynchronously. The test was synchronized to the UI condition; production behavior was not altered.


### Navigation diagnostic checkpoint
The Phase11b configured-opening-section test now captures scrollY, target top, layout readiness, and navigation-section label when the wait condition times out. No production navigation code is changed by this diagnostic checkpoint.


### 11E — Real Screen Navigation

Implemented as a presentation-layer restructuring. The single giant scrolling page is replaced by independent top-level ScrollView destinations hosted inside a screen container. Menu items now switch actual destinations. Transaction/business logic, SQL, calculations, persistence, and domain behavior are unchanged.

The prior 11B viewport-scroll test is updated to verify visible destination selection and section labeling.


### 11F — History & Corrections

Redesigned the History & Corrections destination using the approved Phase 11 visual language. The screen now presents the history list, selected transaction detail, and correction editor as separate surfaces with consistent hierarchy and alignment. Type-specific correction groups retain their existing IDs and visibility logic. Primary/secondary correction actions use the established button styles.

No business logic, correction rules, accounting behavior, SQL, persistence semantics, or event-writing behavior changed. The change is presentation-only. A focused `Phase11fHistoryCorrectionsUiTest` verifies the new presentation surfaces and preserves the existing hidden-by-default correction state.

## 11F.1 — Window Insets & Analytics Period Controls

### Objective

Establish a single safe content-area treatment for system bars and correct the Performance Dashboard period-control layout without changing analytics period semantics.

### Implemented

- Applied system-bar insets at the application root so the top app bar and screen content remain inside the usable display area, including status-bar and bottom navigation/gesture regions.
- Kept the inset handling centralized rather than adding screen-specific top/bottom padding.
- Reworked the four Performance Dashboard period controls into a balanced 2×2 layout so Today, This Week, This Month, and This Year each have sufficient width and consistent alignment.
- Reused the established Phase 11 primary button style; no period text, period semantics, or analytics calculations changed.

### Safety contract

Presentation/layout only. No SQL, business logic, analytics formulas, persistence behavior, event-writing behavior, or reporting-period semantics changed.

## 11G — Budgets & Targets visual redesign

Redesigned the Budgets & Targets destination using the approved Phase 11 visual language.

- Grouped budget setup, budget status/history, and weekly targets into distinct surface cards.
- Added explicit section labels for category, period, period start, planned quantity, amount, target week, and collections target so controls are visually anchored rather than floating.
- Reused shared typography, spacing, input sizing, button variants, and surface treatments.
- Kept all existing view IDs and existing BudgetManagementService wiring unchanged.
- Preserved the existing budget/target semantics, period rules, revision/history behavior, snapshot logic, and persistence.

No SQL, business logic, calculations, validation semantics, persistence behavior, or event-writing behavior changed. Presentation-layer changes only.


## 11H — Management & Settings visual redesign

Redesigned the existing People & Accounts and Settings destinations using the established Phase 11 visual language.

- Grouped Vendor, Customer, and Employee management into distinct surface cards.
- Added a clear People & Accounts screen header without changing account semantics.
- Grouped Business Settings and App Preferences into distinct Settings surface cards.
- Added explicit labels for the six existing business-setting inputs while preserving their existing IDs, values, validation, and save behavior.
- Reused the established typography, spacing, surface, input, primary-button, and secondary-button tokens.
- Kept VendorManagementService, CustomerManagementService, EmployeeManagementService, BusinessSettingsService, and app-preference storage/wiring unchanged.

No SQL, business logic, accounting behavior, calculations, validation semantics, persistence behavior, account identity rules, or event-writing behavior changed. Presentation-layer restructuring only.

A focused `Phase11hManagementSettingsUiTest` verifies the composed management/settings surfaces and existing primary actions.

## Presentation Boundary Hardening — People & Accounts extraction

The first presentation-boundary extraction was performed from the pre-overhaul 11H checkpoint. The Vendor, Customer, and Employee management UI construction, dynamic row rendering, management dialogs, and management error presentation now live in `presentation/management/PeopleAccountsUi.kt`.

The existing management service calls remain in `MainActivity`, including the same create/rename operations and the same existing refresh calls. This is intentionally a presentation-only fragmentation step; no UI technology migration, business logic change, SQL change, or database change is included.

The extraction preserves all existing management view IDs and existing user-facing behavior. The next screen extraction should be performed only after this checkpoint is verified in Android Studio.


## Presentation Boundary Hardening — Backup & Restore extraction

The Backup & Restore presentation responsibilities were extracted from `MainActivity` into `presentation/backup/BackupUi.kt`. The extracted class owns the existing restore-source, restore-group selection, restore-warning, and typed-confirmation dialogs, plus the existing backup status view updates triggered by invalid restore selections.

The existing `BackupManager` operations remain authoritative and are still invoked by `MainActivity`; successful restore refreshes and automatic local-backup behavior remain unchanged. External file-picker requests are still issued by `MainActivity` through the same activity-result contracts.

No SQL, Room/data, domain/service implementation, backup semantics, restore selection semantics, persistence behavior, transaction behavior, navigation architecture, or UI technology migration was introduced. Existing view IDs and backup request codes remain unchanged.

### Presentation Boundary Hardening — Budgets

Budgets & Targets presentation responsibilities have been extracted from `MainActivity` into `presentation/budgets/BudgetUi.kt`. Existing budget service calls, database behavior, identifiers, and business semantics are unchanged. Runtime verification is pending.

### Presentation Boundary Hardening

The current 11H baseline is undergoing incremental presentation-layer extraction. People & Accounts, Settings/App Preferences, Backup & Restore, and Budgets & Targets have been extracted into dedicated presentation classes without changing business/data behavior. History & Corrections presentation ownership is now also extracted into `presentation/records/HistoryCorrectionsUi.kt`.

This work is intentionally limited to presentation responsibilities and preserves the existing service/domain/data contracts. Each extraction is verified independently before the next destination is migrated.

### 11H Presentation Boundary Hardening — Transactions Wiring

The transaction screen event-wiring and input-watcher responsibilities were extracted from `MainActivity` into `presentation/transactions/TransactionsUi.kt`.

The extraction covers only presentation event wiring and forwards existing save/add handlers back to `MainActivity`. Transaction business logic, service calls, calculations, persistence, validation semantics, and database behavior remain unchanged.

### Presentation Boundary Hardening — Transactions Operation Pilot
The Purchase save operation handler was extracted from MainActivity into `presentation/transactions/PurchaseTransactionAction.kt`. Existing validation, VendorPurchaseService invocation, automatic backup, dashboard/history refresh, success/error UI, and input clearing behavior were preserved. This is a presentation/application-boundary extraction only; no domain/data behavior was changed.


### Transaction Operation Handlers — Remaining Simple Batch

The remaining five simple transaction save handlers were extracted from `MainActivity` into `presentation/transactions/SimpleTransactionActions.kt`: Yogurt Production, Vendor Payment, Credit Collection, Expense, and Salary Payment. Existing validation, service calls, automatic backup, dashboard/history refreshes, success/error states, action-button state changes, and input clearing were preserved.

No SQL, Room/data, domain-service implementation, accounting behavior, transaction semantics, calculation semantics, persistence behavior, or navigation behavior changed. This is a presentation/application-boundary extraction only.


## Presentation Boundary Hardening — Dashboard

The Dashboard presentation responsibilities were extracted into `presentation/dashboard/DashboardUi.kt`. The extraction preserves existing dashboard stock/balance rendering, analytics period selection, analytics summary rendering, and preference-driven reporting-period behavior. Business/data semantics are unchanged.

### Presentation Boundary Hardening — Batch 1: Transaction Read/State

Transaction read-side loading and presentation-state orchestration were extracted from `MainActivity` into `presentation/transactions/TransactionStateUi.kt`.

The extracted class now owns the existing transaction option loading (vendors, payment accounts, employees, products, customers), transaction settings reads, spinner population, sale/purchase preview state, sale mode/payment state, action-button state, and the existing transaction selection callbacks used by the transaction action classes.

`MainActivity` retains application-shell responsibilities and existing business operation callbacks, while delegating transaction presentation state through the new class. Existing service/data queries, calculations, persistence behavior, validation semantics, navigation behavior, and view IDs are unchanged.

This checkpoint is a presentation-boundary refactor only. Android Studio remains the authoritative runtime/build environment.

## Presentation Boundary Hardening — Batch 2: Records & Management Orchestration

The second grouped presentation-boundary batch moves the remaining self-contained History read/detail orchestration and People & Accounts create/rename orchestration out of `MainActivity`.

### Extracted
- Added `presentation/records/HistoryCorrectionsActions.kt` for transaction-history loading, transaction-detail loading/building, and history/detail formatting.
- Added `presentation/management/PeopleAccountsActions.kt` for existing Vendor, Customer, and Employee create/rename service orchestration and the same UI refresh/error ordering.

### Preserved
- Existing DAO queries and detail formatting semantics for History & Corrections.
- Existing `VendorManagementService`, `CustomerManagementService`, and `EmployeeManagementService` calls and refresh ordering.
- Existing correction target state used by the correction editor.
- Existing transaction action callbacks and automatic-backup/dashboard/history refresh behavior.

### Deliberately unchanged
- SQL/schema/Room implementation.
- Domain-service implementation and business rules.
- Accounting, transaction semantics, calculations, validation contracts, persistence, and event-writing behavior.
- Navigation, layouts, and UI technology.

The remaining `saveCorrection()` application operation and app-shell responsibilities are intentionally retained in `MainActivity` for later boundary review rather than being moved speculatively.


---

## Phase 11H Presentation Boundary — Batch 3

Status: IMPLEMENTED — runtime verification pending after compatibility fix.

Batch 3 isolates application-shell presentation responsibilities from MainActivity and removes the remaining direct Event DAO read from MainActivity. Navigation, window insets, saved visual preference application, and shared spinner styling now live in `presentation/shell/AppShellUi.kt`. Corrected-transaction detail lookup is handled by `HistoryCorrectionsActions`. The corrected-transaction detail lookup is synchronous within the existing save-correction coroutine so the post-save status is not cleared by a racing detail-load coroutine. No business/data semantics were changed. An existing navigation-test reflection contract is preserved by a thin `MainActivity.navigateToSection(String, boolean)` delegating wrapper.

## First-Use Setup Wizard — planning only

Status: PLANNED — scope intentionally not finalized.

This phase will design and implement a first-time-use setup wizard that gathers enough initial business/state information for the application to start from a meaningful initial state rather than relying on incomplete or assumed setup data. Candidate inputs mentioned so far include the shop name, current milk quantity/stock, current money/balances, and other initial business-state information that may be needed.

The exact questions, ordering, defaults, persistence semantics, validation, and which existing business records/settings they map to are intentionally deferred for a later design decision. No implementation or new business rule is authorized by this placeholder alone.

The phase must preserve the existing core logic specification, SQL blueprint, accounting rules, transaction semantics, and existing data behavior unless the user explicitly approves any required change.



## Final Presentation Boundary Audit — Batch 3 completion

The incremental presentation-boundary hardening line is now runtime-verified through Batch 3. The audit confirms:

- `MainActivity` no longer directly accesses Room DAOs.
- Major destination presentation responsibilities are isolated under `presentation/` packages.
- Transaction UI wiring, transaction read/state orchestration, and transaction save actions are separated from the Activity.
- History/Corrections read-side orchestration and People/Accounts create/rename orchestration are separated.
- App-shell concerns (navigation, insets, saved visual preferences, preferred opening section, shared spinner styling) are isolated in `AppShellUi`.
- Existing navigation test reflection compatibility is preserved by a thin delegating wrapper.
- Remaining `MainActivity` responsibilities are primarily Activity lifecycle/shell coordination plus existing service-backed operations that have not been moved because the current boundary is sufficient for the next UI-technology decision and further extraction would add risk without a stated requirement.

No SQL, Room schema, domain-service implementation, accounting semantics, transaction semantics, calculations, validation contracts, or persistence behavior were changed by the boundary-hardening work.

## Roadmap status after boundary hardening

11H presentation-boundary hardening: COMPLETE.

Next: deliberate UI-technology decision, followed by the actual presentation modernization.

The First-Use Setup Wizard remains planning-only; scope, questions, persistence mapping, and phase assignment are intentionally undefined until a later requirements decision.


## Pre-11I ViewModel Architecture Preparation

Status: CURRENT — Purchase pilot verified; Dashboard + Sale + History/Corrections batch in progress.

The presentation boundary is complete. The Purchase ViewModel pilot is verified, and the current architectural preparation extends ViewModel-backed presentation state to Dashboard, Sale, and History/Corrections. Existing domain calculators/services remain authoritative for business semantics and persistence.

The ViewModel preparation intentionally leaves XML layouts, navigation, visual design, database behavior, accounting behavior, and UI technology choice unchanged. Its purpose is to establish a stable presentation-state boundary before the remaining Phase 11 visual modernization.

The project roadmap remains the original sequence: 11I Backup & Restore visual redesign, 11J Final UI consistency/accessibility/regression, 12 Cash Planning / Reserve Planner, and 13 Final Hardening / Release Readiness. The First-Use Setup Wizard is a future planning item with scope not yet finalized and is not assigned a new phase number.


## ViewModel Architecture Preparation — Settings and Backup

The ViewModel preparation now includes Settings and Backup/Restore presentation state. SettingsViewModel owns business-setting display state and the App Preferences draft without changing existing persistence keys or semantics. BackupViewModel owns restore busy/status presentation state; BackupManager and restore behavior remain authoritative. XML layouts and UI technology remain unchanged. Android Studio verification of this checkpoint is pending.


## Phase 16

Phase 16 is the ViewModel Integration / Presentation Ownership phase. It is a separate phase from the Phase 11 visual roadmap and follows the verified Phase 15 contract work.
