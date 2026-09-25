# Phase 14.8.54b — warm-refresh content retention sweep

Date: 2026-09-25
Branch: `main` (follows 14.8.54a, commit `302dd82`)

## Outcome

All 19 ViewModels that blanked visible content to a full-screen spinner on explicit refresh now retain their last Ready content with an inline `LinearProgressIndicator` and, where the screen lacked one, an inline error line. This closes the largest warm-refresh violation of the 14.8.52/53 UX baseline.

## Pattern applied (canonical: `NativeOwnerRecordsViewModel`)

- `Ready` gained `refreshing: Boolean = false` (plus `error: String? = null` where missing).
- `refresh()`: capture `previous`; emit `previous?.copy(refreshing = true, error = null)` or `Loading` (cold load keeps the full-screen pane); success rebuilds `Ready` exactly as before with `refreshing = false`; failure routes to `handleLoadFailure` only on `SessionExpired || previous == null`, otherwise stays on `previous` with `refreshing = false` and an error message.
- Per-VM specifics preserved: Work VMs keep `previousDestination`; Activity keeps `detailGeneration`; Collection keeps `selectionGeneration`; More/OperationsMore keep parallel two-call loads; the six gate-less VMs (MakeYogurt, ReceiveMilk, RecordSpoilage, Expense, SalaryPayment, VendorPayment) keep their plain try/catch shape; OwnerMore keeps its `OwnerMoreRuntime.restartSession()` token and sub-state reset semantics.

## Files

ViewModels: activity, collection, more, sale, cashclose, expense, salarypayment, vendorpayment, operations (Attendance, Deliveries, MakeYogurt, OperationsMore, OperationsWork, ReceiveMilk, RecordSpoilage), owner (More, Overview, Work), admin Work, manager Work. Screens: the matching Ready bodies for each, plus the Admin/Manager More personal-security panes re-pointed to `state.personalSecurity.error` after OwnerMore's Ready gained an explicit top-level `error` field (replacing the computed property).

Known minor gap (accepted): `resetForMenu()` clears the personal-security error but not the new top-level refresh error, so a soft refresh failure message can persist on the Owner More menu until the next successful refresh. Follows from the prescribed pattern; revisit if it shows in practice.

## Verification

- `qaFast`: PASS (`975` rows, `0` untested/orphans, `CHECK_UI_ONLY: PASS`)
- `lintDebug` + `assembleDebug`: PASS
- Device (SM-A065F), combined contract suites for all touched screens — More, OwnerRecords, OwnerMore, OwnerWork, OwnerOverview, AdminWork, ManagerWork, OperationsWork, OperationsMore: **84/84 PASS**
