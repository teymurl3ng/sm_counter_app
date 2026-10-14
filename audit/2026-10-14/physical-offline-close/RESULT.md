# Finish batch 2: physical offline sales and staged closing accepted

Both A06 and S24 passed one fixed real DEV scenario in isolated com.saaf.milkshop.qa.tests databases. Run cefb3dc1cfe7 used disposable shop 5577806a-e0be-494c-b536-11ebbef96ee5 and two disposable transport accounts. Native local Cashier/Owner sessions authorized commands independently of those transport accounts.

## Outcome

The complete 20 UID-normalized business tables match at eight checkpoints: initial setup, the accepted online starting sale, two recovered independent sales, sales repeat, closed, reopened, reclosed and closing repeat. Every exchange ends with zero outbox/parks/holds and no skipped/conflicted records. Different local account/product/event/planning/count/action integers are verified; foreign references are compared by UID. Only device-local actor integers are excluded, while author names, roles and times remain equal.

A06 Wi-Fi was actually disabled while mobile data was already off. The instrumented phone reports zero validated networks, real UrlConnectionSyncHttp transport failures and RetryLater for both drain and pull. Its queued sale and business ledger remain unchanged. S24 stayed connected, recording its independent sale before any exchange. Each phone independently shows Cash10570/Milk77; reconnection converges to Cash10760/Milk76 with exactly two new sale events and one stock/money movement for each. Repeat exchanges do not duplicate data. Python independently sums the raw Cash financial rows and stock rows rather than relying only on app views.

A Cashier count/close reaches both phones. Same-date sale and Cashier reopening fail on both without changing any of their complete business tables or outbox. Owner reopening with a reason and passphrase authentication reaches both phones; the secret buffer is cleared. Reclosing with the old count is refused on both. A fresh Cashier count/reclose reaches both, leaving two count rows and exactly close/reopen/close history. Count and supersedes references remap correctly, received foreign author IDs stay NULL, and all original 18 business tables remain identical to the post-sale state. Final totals: Cash10760, Milk76, Yogurt17, supplier17576.92, customer0; six events, five financial movements, six stock movements.

## Preservation and cleanup

Original A06 settings were restored exactly: wifi_on1/mobile_data0/airplane_mode_on0. Both phone transports are unenrolled; both created Auth users are retired by the supported administration command; the exact created shop is inactive, retaining only its synthetic history. Thirty-eight synthetic server records include six events, two cash counts and three daily actions. Existing DEV staff51, shops4 and sync_records66 counts and deterministic hashes match before/after. Ordinary/preview package versions and installation timestamps match on both phones; only the isolated QA APKs were installed.

No production main source, schema or existing test changed. One new physical instrumentation probe and its scoped host/evidence files were added. Build passes. All 45 selected stage invocations pass; independent verification passes. No full suite, ordinary shop database change, PROD, provider/RLS/API change, Git commit or push occurred.

## Limits and next finite batch

This result accepts sequential closing stages, each exchanged before the next. It does not accept aggregate queued closing-history catch-up; the ordering limitation recorded in close-sync-repair/DEFERRED_FINDING.md remains pending. Interrupted sync/lost acknowledgment/recovery is batch 3, with that finding kept under its existing sync-recovery boundary. Staff/human acceptance, hosted recovery, reset rollout and the signed ordinary release remain open. Do not add an eighth reporting batch or broaden into a general audit.
