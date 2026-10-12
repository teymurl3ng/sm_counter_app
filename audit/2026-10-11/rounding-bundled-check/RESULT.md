# Production-driver follow-up — real cash rounding mismatch confirmed

The A06 was connected as R8VXA02SNKH, SM-A065F, Android 16. Instrumentation used the separate com.saaf.milkshop.qa.tests identity with runner bootstrap disabled. Every fixture opened a new in-memory database with the production BundledSQLiteDriver. The observed bundled SQLite version was **3.50.1**.

## Observed results

| Check | Result |
|---|---|
| SQL ROUND(2.125, 2) and ROUND(-2.125, 2) | 2.13 / -2.13; the preceding host driver's 2.12 result does not reproduce for this value |
| Amount / quantity / surcharge at 2.125 and 5.125 | Receipt, Activity, cash balance, physical count and authenticated close agreed; passed |
| Correction from 2.125 to 2.00, original history preserved, unexplained one-paisa shortage denied | Passed |
| SQL ROUND(1.005, 2) | 1.00, while the decimal HALF_UP boundary requires 1.01; failed |
| Quantity sale: 0.5 L at Rs 2.01 | Raw ledger 1.005; receipt 101 paisa; Activity 101 paisa; cash balance 100 paisa; count expected Rs 1.00; counted Rs 1.01 appears short/over by one paisa; failed |

The count's signed variance was +0.010000000000000009: a false overage relative to the rounded receipt. No claim of existing customer loss is made; this is an isolated synthetic reproduction of contradictory app values.

First connected run: **3 tests, 2 passed, 1 failed**. Follow-up: **only the new quantity-sale method**, 1 test, failed. Combined latest evidence: 4 distinct checks, 2 passed, 2 failed. The tests assert the desired equality and remain failing; expectations were not rewritten to manufacture a PASS. Signed/small values after 1.005 in the first SQL loop were not reached and are not reported as verified.

Logs/XML/logcat are preserved in first-connected-results/ and quantity-connected-results/, plus connected.log and quantity-check.log. launch-argument-error.log records an earlier Gradle argument-parsing failure before any test ran; quoting the property corrected it. verification.json captures source/package evidence.

## What this run changed

Added NativeBundledMoneyBoundaryTest.kt only. Existing production source still matches the delivered batch-1a source: no additional application, domain, DAO, view, schema, trigger or migration edit was made. No ordinary/preview app update, shop-data change, account reset, commit or publication occurred. The isolated instrumentation APK was built and installed for the checks; this does not deliver the prior Sale repair to the user's installed ordinary app.

B5 remains **PARTIAL / CONFIRMED PRODUCTION CASH GAP**. Batch 1b drawings remains unstarted. The next bounded repair is specified in REPAIR_BRIEF.md. This run closes the driver-verification task, not the money-correctness phase.
