# 9b1 — Physical hosted test stopped on two reproduced sync defects

**Online convergence is not accepted.** A06 and S24 used the actual DEV Auth/enrollment/HTTPS sync stack and separate run-specific databases in the isolated instrumentation package. Both initializations and role-specific enrollments passed. A06's 30-row publish was accepted. S24's exchange failed because 19 queued built-in master rows remained; no full raw-table/balance comparison, Cashier sale, replay, offline or close/recovery checkpoint passed.

Run `b30e1527cc0b`, DEV shop `ce7cc79a-ba15-4e45-a465-c9b03ec63504` / `qa_two_phone_b30e1527cc0b`. A06 local origin `edd49c4f0bba45e181d1e9169627096a`; S24 `db5047278d2a4ea1a085ff24da03f24c`. Origins are distinct existing isolated-package identities. No fake transport or simulator is substituted for this result.

## Confirmed defects, not speculative additions

1. **Seeded profile timestamp compatibility.** A06's pull/catch-up logged ten unique rejected default profiles (Milk/Yogurt and eight expense categories): `Text '2026-01-01 00:00:00' could not be parsed at index 10`. Saved product-profile dates contain that exact timestamp. `Phase3MigrationSql.backfillStatements` creates all five profile families with this SQLite-form timestamp; `SyncApplyEngine.requiredCreatedAt()` accepts only `Instant.parse`. The serializer transmits the original profile timestamp. The receiver therefore rejects legitimate app-generated data. A06's public pull returned RetryLater after HTTP-200 responses; a successful publish is not successful receive/catch-up. Prior five-family ISO-timestamp tests remain evidence for their inputs, but B1's date compatibility is reopened by this real failure.

2. **Fresh Cashier enrollment uploads its built-in master queue.** S24 starts with 19 captured seed rows: two products, eleven accounts and six settings. Those rows existed before `SyncBaseline.enqueueAllHeld`'s boundary and stayed `held=0`. Saved rebaseline audit reports zero released/equal/held rows. The actual server then returns `master_data_privilege_required`, with `Updating products requires an owner or admin account`, and the FIFO remains halted. Nineteen pulled versions are parked under `pending_local_edit`. This is not evidence that the server role gate is wrong: Cashier must not acquire master-edit permissions to work around initialization. Normal counter setup must reconcile built-in defaults without losing real pending edits. The test stopped before seed review because these were unheld queued rows, not reviewable held entries.

The receiver did apply some remote business rows (saved counts include three events and the custom profile/plan parents); this run does **not** establish total loss or corrupted balances. No Cashier sale was attempted after the failure. The underlying saved audits, queue groups and profile timestamps are in `saved-diagnostics.json`. These two defects directly block the already planned exchange; no unrelated screen or subsystem was investigated.

## Cleanup and containment

Both test transports logged out and cleared their run-specific connection/token files. Both created Auth identities were retired through the existing termination command; their staff rows remain inactive/terminated with Auth references cleared, preserving audit history. Only the newly created shop was deactivated. Thirty synthetic sync rows remain under that inactive fixture for evidence; no purge or existing-shop deletion ran.

Before/after server hashes match for all unrelated selected rows: **51 existing staff**, **4 existing shops**, **66 existing sync records**. The created `qa_2p_…` staff and `qa_two_phone_…` shop are explicitly excluded from those comparisons. Both phones' ordinary, preview and `.qa` package versions/install timestamps are unchanged. Only isolated test APKs were installed; run-specific Room/sync storage is separate from the default test DB. No ordinary phone credentials/records, PROD, server schema/RPC change or release signing/publication ran.

The test harness and host orchestrator were added; main source is unchanged. The build passed; exact APK/source hashes and staged logs are retained. Credentials were held in host memory, sent via ADB stdin to a private disposable input file, and removed immediately on the phone. No credential/token/Admin key appears in the saved evidence. The full test suite was not run.

## Next fixed scope

See [FIX_SCOPE.md](FIX_SCOPE.md). First fix only profile timestamp compatibility with selected regression inputs, then resolve only the fresh Cashier seed-queue enrollment boundary with role/pending-work preservation tests. After those pass, rerun this same physical online exchange with a fresh disposable fixture. Do not relax Cashier server privileges, rewrite historical dates, erase unsent work, or introduce a new identity design.

The whole plan remains:

1. Money/withdrawals — complete at bounded QA scope.
2. Receiving sync — profile/planning implementation and earlier selected checks retained; **B1 timestamp compatibility reopened**. B2/B10 remain accepted at their recorded scope.
3. Mapped menus/lists — complete at bounded QA scope.
4. Access/recovery/setup — local recovery/copy checks retained; **fresh counter seed enrollment defect now confirmed**. Hosted recovery/reset and actual shop-device handover remain open.
5. Acceptance — shop-day oracle/automation and seven A06 checks complete; **physical online exchange failed**, full two-phone acceptance and staff/human checks remain open. Offline/close/reopen and interrupted recovery checkpoints have not started.
6. Freeze/commit/push/sign/install — pending. No new finished item is crossed out from this failed run.
