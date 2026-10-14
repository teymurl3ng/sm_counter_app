# Batch 3 fixed execution boundary

## Observed cause

One selected A06 initialized-receiver diagnostic fails with the actual SQL reason: Daily close must use the latest cash count. SyncEnvelopeFactory groups queries and emits all rows of a table together, putting both counts ahead of close/reopen/reclose. Later actions then park for missing superseded parents. Saved baseline-test-console.txt contains the refusal. No production fix preceded this diagnosis.

## Minimum production change

SyncEnvelopeFactory.kt only: retain grouped reads/body encoding/orphan classification, but return the envelopes in the same order as the input outbox rows. The actual DAO supplies strict first-change order. Do not reorder by timestamps or relax receiving count/date/history/actor guards. No schema/version, foreign identity, hosted RPC/RLS or ledger change. Existing server bodies produced in the wrong order are not automatically rewritten; known affected history requires the separately recorded exact-UID replay condition, not a crawler.

## Focused verification and physical story

Run the queued-close initialized-receiver regression, sequential close and refusal-retention controls, existing enqueue-before-close, push FIFO, transient retry and edit-during-upload acknowledgment controls. No full suite.

Use one fresh isolated two-phone DEV fixture. Starting state Cash10000/Milk80/Yogurt17. On S24, authenticate local Cashier and save one 1L cash sale@190. Let the real sync_push endpoint return2xx with ok=true, then deliberately discard only that response via the injected SyncHttp seam before the queue acknowledges it. This is a lost-response simulation after real server acceptance, not a radio outage or physical power loss. Confirm queue and all ledger rows stay intact; A06 independently pulls the accepted event while S24 still has its queue. End the instrumentation process, record PID disappearance/new PID for retry stage, restart the isolated app and retry. Both phones must match all20 UID-normalized business tables at Cash10190/Milk79 with exactly one new sale, no duplicate event/log/actor, and zero queue/parks/holds. Repeat exchange preserves exact tables.

Then queue one full count/close/Owner authenticated reasoned reopen/fresh count/reclose cycle on A06 using its Owner transport (local Cashier still closes; local Owner authenticates reopening) without exchange between actions. Push the new FIFO-preserving five-record batch and compare all20 rows on both phones. Money/stock and original18 tables remain unchanged; two counts plus exactly close/reopen/close and all UID links/authorship match. Repeat preserves exact history. This proves newly emitted queued cycles; it does not certify legacy incorrectly grouped server feeds or arbitrary page interleaving.

Use supported disposable Auth retirement, exact shop deactivation and unchanged existing DEV staff/shop/sync hashes and ordinary package provenance. No radio change, ordinary app DB, PROD, broad audit or other finish batch. If a mismatch occurs, stop and record it under this existing batch.
