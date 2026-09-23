# Phase 14.8.53 — native menu coverage and UX baseline

Date: 2026-09-24  
Branch: `codex/native-menu-coverage`

## Outcome

The remaining native menu work that has an authoritative local API is integrated. The same pass establishes the interaction rule for current and future menus: primary navigation is immediate and state-retaining; secondary More destinations use a short 180 ms fade; cold loads may use a blocking pane; warm refreshes keep useful content visible and report progress inline.

## Completed menu depth

- Records now provides real Recent, Balances, Closing, and Corrections groups for Owner, Manager, and Administrator. Balances open the authoritative account ledger; Closing shows reconciliation close history.
- Cashier More now opens the existing typed Deliveries workflow.
- Owner Reports accepts validated inclusive custom date ranges and converts the end date to the reporting layer's exclusive boundary. Reversed ranges and ranges longer than one year are rejected.
- Costing & plans → Yield now records or revises production-loss assessments through `ProductionLossAssessmentService`, including ordinary/abnormal classification, abnormal quantity, reason, notes, supersession, and recent-auth retry.

## UX baseline

- Primary-tab switches are not animated and do not recreate destination ViewModels.
- Cashier, Owner, Manager, and Administrator More families use the same bounded 180 ms secondary transition.
- Records and Reports preserve their last successful content during explicit refresh and expose inline progress instead of replacing the screen with a spinner.
- Cold loading and load-failure components are centralized for reuse by later screens.
- Corrections read the current dirty draft synchronously before leaving, so a rapid edit-then-back gesture cannot bypass the discard confirmation.
- Empty, loading, error, and retry behavior remains explicit; the UI does not fabricate data or duplicate domain calculations.

## Authority boundary

Approval queue write UI remains excluded. There is no authoritative domain API for it, so this phase deliberately does not invent a local approval model.

The existing hosted account service is also unchanged. Reconciliation of that service with the local identity/session model is the next architecture phase and requires an explicit source-of-truth contract before networking is added to the native UI.

## Verification

- `qaFast`: PASS (`975` inventory rows, `0` untested, `0` orphans)
- Unit tests and Kotlin/Android-test compilation: PASS
- `lintDebug`: PASS
- Debug and release assembly: PASS
- Combined Records, Reports, Costing/Yield, and Cashier More device contracts: `45/45` PASS on SM-A065F
- Owner/Manager/Admin More and shell contracts after transition unification: `18/18` PASS
- Reports facade custom-range boundary tests: `5/5` PASS
- User-facing `com.saaf.milkshop.preview` APK installed with app data preserved; `NativePreviewActivity` confirmed foreground

