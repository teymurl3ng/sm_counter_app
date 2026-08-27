# Phase 7A — Analytics / Reporting Foundation

## Status
Implementation prepared; Android Studio/device verification pending.

## Scope
Create the first read-only analytics layer above the existing core. No dashboard UI is included in 7A.

## Architectural contract

7A consumes:

- existing `events` data through read-only queries;
- existing `current_stock` and `account_balances` read models.

7A may add read-only DAO queries or views when required for aggregation.

7A does **not** create writable analytics tables, cached business totals, new accounting state, or new ledger logic.

## Metrics in this checkpoint

The period summary supports:

- event count;
- sales value and quantity;
- vendor purchase amount owed and purchased milk liters;
- yogurt produced and milk used in production;
- vendor payments;
- credit collections;
- operating expenses;
- salary payments.

Current position exposes the existing canonical stock and account-balance read models.

## Period semantics

The period query uses a half-open range:

`startDateInclusive <= occurred_at < endDateExclusive`

This keeps adjacent periods non-overlapping.

## Correction semantics

A corrected event is treated as replacing the event it corrects for period reporting purposes. The read-only analytics query therefore excludes any event that has a child correction and reports the effective leaf event instead.

## Files added

- `app/src/main/java/com/saaf/milkshop/data/AnalyticsReadModels.kt`
- `app/src/main/java/com/saaf/milkshop/domain/Analytics.kt`
- `app/src/androidTest/java/com/saaf/milkshop/Phase7aAnalyticsTest.kt`

## Files changed

- `app/src/main/java/com/saaf/milkshop/data/Daos.kt`
- `app/src/main/java/com/saaf/milkshop/data/MilkShopDatabase.kt`
- `README.md`
- `PHASE6_ARCHITECTURE_AUDIT.md`
- `PHASE7_ARCHITECTURE_AUDIT.md`

## Phase 8 analytics extension
The Phase 8 budget layer requires category/product actuals, so the 7A read-only analytics API is extended with `budgetActuals(...)`. This remains read-only, derives from effective event truth, and does not add writable analytics state or change accounting formulas.

## Integrity constraints

- No SQL schema version change.
- No writable analytics table.
- No changes to transaction services or `CorrectionService`.
- No changes to canonical balance or stock views.
- No changes to existing business formulas.
