# Phase 2, B2b/B10 — final planning sync unit

Recover planning rows whose server sequence was already consumed by an older receiver. Use a one-time filtered replay of current server rows, keyed to server/shop/installation, after the ordinary pull completes. Keep the ordinary cursor and local outbox untouched; the existing apply engine parks pending local edits and late parents. Verify pages, retry, and active planning values with focused instrumentation.

B10 is verify-first. Reproduce planning-only restore on an isolated in-memory database and pre-user local snapshot. Check the outbox, held state, pull cursor, and unrelated queued business rows. If a stale planning row would push unreviewed after re-enrollment, fix only that restore boundary and rerun its fixture. Do not change full business restore, remote RPCs, UI, or release packaging. Stop when these two paths pass selected tests; leave physical two-phone acceptance to phase 5.
