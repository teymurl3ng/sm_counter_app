# Phase 14.8.54a — interaction polish: motion, shared transition adoption, list performance

Date: 2026-09-25
Branch: `main` (merged account-bridge branches `99a6795`, `a476eeb` first)

## Scope

Part 1 of the whole-app interaction polish pass (status doc recommended-next #2). Presentation only; no domain, schema, or behavior changes.

## Survey findings (code-level sweep of all 118 nativeui files)

1. `NativeColdLoadingPane` / `NativeLoadFailurePane` have zero call sites — ~40 screens hand-roll duplicate loading/error panes (baseline-allowed cold panes, but duplicated code). → deferred to 14.8.54c.
2. ~24 ViewModels blank visible content to `Loading` on explicit refresh, violating the warm-refresh baseline; the correct `refreshing = true` + inline-progress pattern exists in OwnerRecords/OwnerReports. → deferred to 14.8.54b (this is the largest batch).
3. The only animation in nativeui (the 180 ms secondary Crossfade) hardcoded its duration and ignored the system animator scale; the Operations More family lacked the shared transition entirely.
4. All 18 lazy list `items(...)` calls have stable keys. Four filter computations ran inside composition on every recomposition.

## Fixed in this checkpoint

- `NativeSecondaryTransition` now scales with `Settings.Global.ANIMATOR_DURATION_SCALE` and snaps (1 ms) when the user removes animations; bounded to 1 s. Implemented once in `common/NativeAsyncUi.kt`; all five More families (Cashier, Owner, Manager, Admin, and now Operations) share it. `OperationsMoreScreen` migrated from a bare `when` switch to `NativeSecondaryTransition`.
- `remember`-keyed list filtering in `OwnerRecordsScreen` (balances search), `CashierCollectionScreen` (customer-picker dialog), and `OwnerCostingPlanningScreens` (budget/target lists hoisted above the `LazyColumn`).

## Deferred to later 14.8.54 batches

- 14.8.54b: warm-refresh content retention sweep across ~24 ViewModels (replicate the OwnerRecords/Reports `refreshing` pattern).
- 14.8.54c: adopt `NativeColdLoadingPane`/`NativeLoadFailurePane` across hand-rolled screens.
- Micro-feedback (haptics/snackbar usage) recorded as a design decision for the owner: the SnackbarHostState plumbing exists in 65 screens but is used only by preview scaffolding; no haptics anywhere.

## Verification

- `qaFast`: PASS (`975` inventory rows, `0` untested/orphans; `CHECK_UI_ONLY: PASS` — no logic exception needed)
- `:app:lintDebug`, `:app:assembleDebug`: PASS
