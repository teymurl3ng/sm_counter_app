# Phase 2, B2a — planning receiver

This run adds receive/apply for `budget_plans` and `collections_targets`. The server already receives their envelopes; the Android receiver currently skips them. Match rows by UID, resolve `supersedes_uid` to a device-local ID, park an unresolved parent, and keep create/revise/archive replay idempotent. Reject an unrelated sibling revision rather than showing two active values for one period. Verify on the isolated QA package with planning-specific instrumentation only.

Stop boundary: do not alter restore classification, cursor reset, server schema, menus, or release packaging in this run. After the receiver is verified, use a separate B2b/B10 run to determine whether an older consumed planning row needs historical catch-up, then test that path narrowly. Mixed-version Android receivers still skip planning rows, so full two-phone planning convergence requires both clients to update.
