# Close sync repair: accepted at the scoped boundary

An initialized receiving phone now accepts another phone's closing record without inventing a local user or assigning a foreign user ID. Remote actor name, role, time and count/history UID links remain unchanged. Only the installed local-login actor trigger is temporarily removed for the remote insertion inside the existing writer transaction and record savepoint, then restored from its exact installed SQL. All other count/date/history/role guards remain active. Missing actor guard fails closed.

Refused daily_close_actions are retained with their complete body and server sequence in the existing parked-row storage. Missing parents retry after arrival. A valid current body clears an older refusal; older replay cannot discard a newer refusal. Existing immutable close history must match before a replay can count as present. No historical crawler or old-skip repair is implemented.

Ten distinct selected A06 methods accepted: eight passed initially, two affected methods passed after the test-only updates documented in DEFERRED_FINDING.md. The original receiving regression and empty-user control pass; ordinary NULL/fake user closes remain refused. Native Cashier reopening remains refused; Owner reopening still requires reason, credentials/recent authentication and a fresh recount. Sequential close/reopen/reclose retains all three actions and remaps every count/supersedes reference. Money and stock history and local users remain unchanged; local queue entries remain intact and imports do not echo.

Semantic insertion refusal proves exact guard restoration and no partial action. Synthetic cancellation of the surrounding Room transaction after count/close import rolls back both records and parked-row changes while retaining the original guard; this is not a process-kill or power-loss test.

Only SyncApplyEngine.kt, one metadata query in SyncEntities.kt, and SyncClosePreflightTest.kt changed relative to the saved baseline. Schema and other implementation/test files are unchanged. Build succeeds. Ordinary and preview package versions/install timestamps match before/after; only the isolated QA app was installed. No remote database, ordinary business data, radio setting, full suite, Git commit or push was changed.

## Recorded limitation and next finite batch

Bulk queued closing-history catch-up has an ordering limitation recorded in DEFERRED_FINDING.md. The test originally coalescing an entire cycle failed; it is not counted as accepted. The planned scenario synchronizes each closing action before the next, and that sequential scenario passes. Do not claim that all queued closing histories converge.

Next is finish batch 2, the frozen physical offline-sales and staged closing scenario using both phones and the 20-table comparison. Keep the recorded ordering finding attached to sync acceptance/recovery; scope any repair separately before changing it. S24 was absent at the last check. Interrupted recovery, staff/human acceptance, hosted recovery, reset rollout and signed release remain separate unfinished batches.
