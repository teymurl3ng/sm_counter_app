# Recorded finding; no ordering repair in this batch

The initial close/reopen/recount test queued all three actions before building a single real SyncEnvelopeFactory batch. It failed to establish receiver posting state. The factory groups by table, so both cash counts precede the close actions. The existing SQL guard requires closing with the latest count; the first historical close then cannot apply. The other close actions depend on that history. This is evidence of a queued-history ordering limitation; the immediate SQL refusal reason was not captured in the initial test log, so the precise failure chain is inferred from the implementation and test arrangement.

The scoped physical scenario syncs initial close, Owner reopen, and fresh recount/reclose in separate stages. Its focused test now retains real envelopes from each stage before creating later counts. It does not certify bulk history catch-up. The original failing run is retained in a06-test-console.txt and a06-test-summary.json. This finding stays attached to batch 2/3 sync acceptance; no new independent completion task, queue-order rewrite, schema change or broader audit is authorized by this result.

A separate test assertion expected an empty outbox after deliberately inserting two local test counts. Those are legitimate local captures. It now compares the exact local queue before/after remote replay, proving no echo and no loss instead of wiping the queue. No production code changed in response to either test update.


14 October follow-up: the SQL latest-count refusal is now observed in sync-recovery/baseline-test-console.txt. Returning new envelopes in original outbox order repairs the emitting defect without relaxing guards; focused and physical queued-cycle proof are accepted in [batch3 result](../sync-recovery/RESULT.md). Historical incorrectly ordered hosted bodies are not automatically rewritten; conditional C03 remains if actual affected history is identified.
