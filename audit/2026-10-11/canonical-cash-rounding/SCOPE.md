# Cash rounding — bounded implementation

Scope: B5 only. Align the account balance view, physical count expected balance, and daily-close SQL guard with the existing decimal HALF_UP converter. Sum raw ledger facts once, then round; preserve ledger values, quantities, IDs and close reasons. No UI redesign, withdrawal remedy, general sync repair, server deployment, ordinary-app installation or production-data action.

Compatibility amendment: Room 12 -> 13 adds `daily_close_actions.money_rounding_version`. Migrated closes and missing fields in legacy backups/sync payloads use version 1 (historical SQLite ROUND); new closes use version 2 (decimal HALF_UP). Carry the marker through backup and sync. Evaluate saved close history using its recorded policy. Preserve older intermediate migrations by installing the historical trigger until version 13.

Implementation: SQL printf selects the first 15/16/17-significant-digit representation which round-trips to the same binary64 value; decimal digit cutting performs HALF_UP without floating multiplication. All three calculations share this expression. LIMIT -1 OFFSET 0 retains the aggregate boundary on old SQLite planners. Out-of-range values remain raw for the application's exact paisa converter to reject rather than silently saturate.

Finite acceptance:
- literal positive/negative ties, adjacent representable values, tiny/large values against an independent BigDecimal oracle on the actual bundled SQLite driver;
- Rs 1.005 quantity sale: receipt/activity/cash/count/close = 101 paisa, with raw Rs 1.005 retained;
- one-paisa shortage still refused without a reason by the SQL guard; unknown rule versions refused;
- immutable correction and ordinary amount/quantity/surcharge/collection/expense behavior;
- v12 migration preserves raw ledger, count, close UID/reason and historical result;
- old missing-policy and new-policy backup restore and local sync replay remain accepted and idempotent;
- selected existing cash-close, backup and earlier migration checks; frozen baselines unchanged.

The wider boundary oracle initially failed on Robolectric's legacy SQLite decimal parser: it incorrectly casts the rounded candidate 963571.795 back to 963571.7949999999. That oracle is moved unchanged to Android's production bundled driver. Host sale and conversion tests remain. Failed first/second runs are retained as development evidence.

This keeps raw Double aggregation; it is not an integer-money storage migration or proof of unlimited precision for arbitrarily long/large ledgers. Two writable devices must run matched app builds before using the corrected close policy. No mixed-version rollout is authorized by this batch.

Necessary history-read dependency: PhysicalCashCountService.latest also reads a saved closed count. It must use the linked close policy, or the cash-count pane would reinterpret an old accepted close while DailyClose history preserved it. Add the indexed count-to-close DAO read and assert the saved count in migration/backup/sync fixtures.
