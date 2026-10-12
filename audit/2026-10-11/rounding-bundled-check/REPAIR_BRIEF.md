# Next bounded run — B5 canonical cash rounding

## Problem and target

A single quantity sale of 0.5 L at Rs 2.01 creates raw money 1.005. Sale and Activity correctly expose 101 paisa under the selected decimal HALF_UP read policy. The SQL cash balance and count pre-round that raw value to Rs 1.00. The target is one agreed cash amount across the balance display, physical-count expectation, variance and daily-close decision. Preserve original event and ledger facts.

## Exact affected seams

1. Entities.kt::AccountBalanceView, currently ROUND(SUM(amount), 2), and Daos.kt::ReadModelDao consumers. A fix must not merely replace the Sale screen's displayed cash value.
2. Phase3ExtensionDaos.kt::PhysicalCashCountDao.canonicalBalanceAt, used by PhysicalCashCountService and DailyCloseService. The same calculation must apply at the count's timestamp, not include later money movement.
3. DatabaseSql.kt daily-close reason guard, which independently repeats the SQL rounding. Legitimate equal cash must close without a fabricated variance reason; a genuine one-paisa mismatch must still require a reason.
4. MilkShopDatabase.kt migration and Room view metadata only if the chosen representation changes a view/schema/trigger. Review historical counts and restored databases; do not edit frozen source-of-truth spec/SQL files or rewrite financial history.

The existing decimal converter in HybridReadFormatting.kt remains the numerical reference. Removing one SQL ROUND is insufficient because another layer can still disagree. Adding an epsilon, loosening the variance threshold, or automatically supplying a reason would conceal the defect.

## Representation decision before editing

Prepare the smallest explicit rounding-policy amendment covering raw saved facts, aggregation order, half-paisa/signed values and expected cash at count time. Select how the database guard receives or reproduces the authoritative canonical value. Review whether a persisted immutable integer-paisa count expectation is necessary, and how old counts/export/restore/sync would remain compatible if so. That is a candidate, not an approved or implemented schema design.

Keep the repair limited to these monetary read/count/close seams. If enforcing one calculation requires a wider money-storage or ledger redesign, stop with the concrete amendment and its impact rather than introducing a partial workaround. The selected policy must be tested against both drivers and literal sums; a device-specific SQL result is not the policy itself.

## Acceptance checks

- Both failing bundled tests turn green without changing their desired expectations.
- Positive/negative 1.005 and 2.125, values immediately beside ties, ordinary exact-paisa amounts and a safe large value match the selected rule. Aggregate rounding and correction deltas have independently stated expectations.
- Cash count of Rs 1.01 after the reproduced sale has zero variance; close succeeds without a reason. A real one-paisa short count remains denied without a reason. Correction preserves original rows.
- Existing focused count/close, post-count cash-change refusal and relevant migration/restore cases pass. Add only compatibility checks demanded by the actual implementation.
- No UI redesign, drawing correction, product/quantity precision migration, server work, historical rewrite, full-suite chase or ordinary-app install in this run.

Run this before declaring B5 complete. New unrelated findings go to the later queue. The shop-day fixture and release gates remain separate.
