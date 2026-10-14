# 9b1 dependencies — two bounded repairs, then the same exchange

These are directly reproduced blockers of the existing physical-phone acceptance, not a new audit or feature backlog. Scope before production edits; this run did not implement either repair.

## 9b1a — app-generated profile dates

Touch only the profile `requiredCreatedAt` validation in SyncApplyEngine.kt and selected receiver/catch-up regression tests. Accept the existing ISO instant inputs and the app's valid SQLite timestamp representation. Preserve original stored text; do not invent a timezone, normalize historical values, alter seed/schema/migration files or relax malformed-date/boolean/amount validation. Include actual factory output from a fresh seeded database, all five migrated profile families with the known SQLite date, successful filtered catch-up and a malformed-date rollback control. Rerun only new cases plus the relevant existing profile receiver/catch-up controls. Finish when legitimate seed/migration data round-trips without skipped profiles while invalid input still preserves existing rows.

## 9b1b — fresh counter's built-in seed queue

Inspect only SyncEnrollment/SyncBaseline's boundary and the current seed payloads. Determine the smallest safe reconciliation for a newly initialized Cashier transport whose captured defaults predate enqueueAllHeld. Built-in defaults must be classified/reviewed against the existing shop before upload; server Cashier privileges remain unchanged. Preserve genuine queued master edits and ledger entries, identity/binding gates, FIFO, cancellation and interrupted-enrollment safety. No blanket outbox clearing, automatic keep-mine, general identity redesign, counter Owner privilege or whole-app onboarding change.

Selected tests must reproduce the initial 19-row captured queue, a freshly enrolled Cashier receiving existing master records, an intentionally changed local default/genuine unsent record preserved, and the Owner enrollment behavior remaining valid. Verify any explicit take-server decision through the existing review path. Do not label unheld queued rows as already reviewed. If a necessary choice cannot be resolved within this boundary, record it before expansion rather than hiding the queue with fixture cleanup.

## Recheck and remaining checkpoints

Once these two repairs pass, rerun the fixed 9b1 hosted exchange exactly: two physical phones, fresh isolated fixture, complete normalized raw business tables and literal balances, Cashier sale, duplicate-free repeat. Improve harness diagnostics to retain drain/pull reason strings and fail immediately on RetryLater; current simple type names hid the failed A06 receive from the first phase's assertion. That is a test reporting repair, not an expectation change.

Only after online convergence passes: separately scope 9b2 offline writes/close/reopen/refusal and 9b3 interrupted/retried recovery. Staff/human acceptance and phases4/6 stay distinct. No full test suite, provider/PROD mutation or unrelated digging.
