# Phase 3 batch 5a — cashier Activity expenses

Cashier Activity now includes expenses recorded by the signed-in cashier on the selected business date. An expense row shows a positive amount and its category/payment account. The inspect-only detail shows category, paid-from account, amount and note. Category names still resolve after a category is archived. The same actor-link check gates direct detail loads; another cashier's or the owner's expense is not returned.

The change is limited to `NativeActivityFacade`, `CashierActivityScreen` and their focused contracts. Existing sale/collection filtering, permission, date and detail behavior remain. No shop-wide ledger, expense mutation, schema, correction or visual-hierarchy work was added. Batch 5b still owns the planned row/detail design pass.

Verification used the isolated QA app on S24 (`SM_S921W`, `RFCX116CYYX`): `assembleDebug` and `assembleDebugAndroidTest` passed; five selected Activity facade/screen checks passed (`OK (5 tests)`). After extending the expense fixture to archive its category and verify the second cashier's own list, only that test was rebuilt and rerun (`OK (1 test)`). `git diff --check` passed on touched source/tests. The ordinary app was not installed; no full suite, commit or push was performed.

Next bounded unit: phase 3 batch 5b, mapped transaction/product presentation only.
