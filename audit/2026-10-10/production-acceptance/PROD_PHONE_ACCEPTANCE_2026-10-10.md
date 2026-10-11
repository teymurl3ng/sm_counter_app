# Production phone acceptance — 2026-10-10

The owner completed the local owner setup, enrolled the ordinary signed production app with their existing PROD account and reported push drained / updated 16 / other counts zero. The actual A06 release UI and read-only Supabase MCP checks verify this result. No credentials were extracted, no PROD test account was created, and the agent did not clear app data or alter server history through SQL.

## Verified result

- Actual `com.saaf.milkshop` Sync screen: PROD, Shop 1, owner, server identity verified, session active, trusted time server-anchored, Waiting to upload 0, Last pulled record 107.
- Initial message: `Push: Drained · Pull: applied=0 updated=16 skipped=0 parked=0 conflicts=0`.
- PROD has 36 sync records through sequence 107, matching the phone cursor. The 16 rows with sequences newer than the deployment baseline 72 are eight accounts, two products and six settings. Event/delivery/status-history sequence values remain below that baseline. These are updates to existing mutable rows; the total record count remains 36. This does not prove full historical payload equivalence or correctness of every mutable value.
- Existing PROD owner `teimur` is active and not terminated. Its real Auth sign-in is recorded at 2026-10-10 14:44:59 UTC, after release installation. The phone's verified identity/session provides real release Auth/enrollment evidence, unlike earlier SQL claim simulations.
- One agent-triggered normal repeat sync completes: `Push: Drained · Pull: applied=0 updated=0 skipped=0 parked=0 conflicts=0`. The visible upload queue remains 0 and cursor remains 107. Read-only server recheck at 14:48:43 UTC confirms 36 rows / max sequence 107, no non-object payloads and no Auth reconciliation rows.
- Read checks find zero unknown sync table names and zero business/shop binding mismatches. Counts remain one business, one shop, three staff and three Auth identities. Ordinary authenticated direct INSERT remains denied after the S01 repair; no privilege was reopened for client acceptance.

## Remaining immediate owner check

Sign in to https://sm-server-view.vercel.app/login with the existing PROD username and server credential, and confirm the expected shop/staff information. Actual authenticated production Viewer behavior and production admin-key acceptance are not yet verified. Phone login/enrollment/sync is now verified for this bounded workflow; the earlier release report's pending phone steps are a historical snapshot.

Release storage is not debuggable. The agent checked visible real-app state and server metadata without reading the private Room database. No claim is made that all ledger children have been compared, that no older persistent park exists, or that every feature converges across phones. Last-batch parked/conflict counts are zero; no review-hold section is present in the captured UI. Server CLI/DR source publication/restore, power-loss durability, production motion budgets and previously scoped design/policy choices remain separate unfinished work.

`phone-sync-result.xml`, `phone-sync-repeat-first.xml`, `phone-sync-verification.json` and `phone-sync-repeat.png` preserve actual UI evidence. `prod-read-check.sql`, its raw/normalized result and `prod-repeat-check.json` preserve read-only MCP checks. `verification-prod-phone-final.json` records this later evidence snapshot; previous release and migration manifests are preserved and have not been rewritten to claim later tests passed earlier.
