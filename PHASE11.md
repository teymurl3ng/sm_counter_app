# Phase 11 — Integrity / Recovery / Concurrency

Status: **IN PROGRESS — checkpoints 11.1–11.5 complete**

## 1. Purpose

Phase 11 stress-tests the already implemented native product, security, backup, and restore boundaries. It does not reopen approved accounting semantics or turn cloud backup into synchronization.

## 2. Locked boundaries

- Room on one writable installation remains the only business truth.
- Contract v4 remains frozen at 86 operations unless a separately approved capability requires an additive versioned change.
- React remains presentation and intent only; native Kotlin owns validation, authorization, persistence, encryption, recovery, and audit.
- The encrypted Google Drive feature remains periodic recovery. It is not a multi-writer or live-sync system.
- Frozen logic and SQL baselines must remain unchanged.
- Lower-of-cost/NRV remains assessment-first. Implementing new valuation semantics requires an explicit materiality/business trigger and approval.

## 3. Ordered checkpoints

### 11.1 — Cloud outbox durability and overlapping-run control ✅

Completed 2026-09-04.

- A finalized encrypted outbox file is recovered if the process stops before its metadata is committed.
- Recovery cryptographically authenticates the envelope with the configured local vault key before any upload.
- A pending state whose local file was already removed can finish from the matching verified remote snapshot, including providers that require a verification download because no checksum is exposed.
- Successful remote state is committed before best-effort local outbox removal, closing the second process-death window.
- Immediate and periodic coordinators share a process-wide mutex. An overlapping waiter reuses the just-completed snapshot instead of producing a duplicate upload.
- Upload results now report whether this invocation actually uploaded or reused an idempotent remote copy.

Focused physical-device evidence: `Phase11CloudBackupDurabilityTest` passes 4/4 on Samsung SM-A065F.

### 11.2 — Cancellation, scheduling, offline, and provider failure matrix ✅

Completed 2026-09-04.

- Cancellation during coherent snapshot reads, upload, download verification, and retention propagates cleanly without being converted into a false failure or success. Pending identity and bytes remain retryable where they exist.
- Repeated offline failures reuse one immutable snapshot and one checksum. A durable failure-start timestamp now delays transient-network notification until the failure series reaches 24 hours, unless the last verified backup is already more than 48 hours stale.
- The approved scheduler policy is centralized and device-tested: approximate 24-hour periodic work, 30-minute exponential backoff, unmetered/Wi-Fi by default, optional metered network, battery-not-low and storage-not-low, with no charging requirement.
- The missed-run status boundary remains non-stale at exactly 48 hours and becomes stale after it.
- Revoked authorization and a wrong active Google account stop before snapshot creation. Quota failure retains the pending encrypted snapshot and all existing remote recovery points. Keystore loss creates no unprotected outbox data.
- Retention deletion failures remain warnings after a verified backup; retention cancellation leaves the verified pending identity available for idempotent retry.

Focused physical-device evidence: both Phase 11 cloud classes pass 15/15 on Samsung SM-A065F.

### 11.3 — Repeated restore, identity, and session recovery ✅

Completed 2026-09-04.

- Repeated full-replacement restore is device-proven across two generations: each successful restore removes post-snapshot mutations and invalidates every pre-existing in-memory session.
- Security identities, credentials, recovery state, creator/disablement links, security audit, and authenticated daily-close actor attribution survive replacement. Disabled users remain unable to authenticate after recovery.
- Historical actor replay now temporarily suspends only the daily-close active-actor insertion trigger inside the verified security restore transaction, allowing valid history from users who were later disabled or changed roles; the trigger is recreated before verification and commit.
- Cancellation now propagates from backup/export/restore boundaries instead of being wrapped as an ordinary failure. Safety-snapshot failure or cancellation leaves the current database and sessions untouched, and failed local snapshot finalization removes temporary data and wipes encrypted buffers.
- SharedPreferences restore is compensating: if Room replay or post-replay verification fails or is cancelled, the previous preference state is synchronously restored while the database transaction rolls back.
- Restore selection is device-proven to replace only the chosen business, planning, and preference groups.
- Three consecutive wrong cloud-recovery passphrases and an unknown recovery-point selection leave the cached recovery point unchanged and usable by a later correct passphrase.

Focused physical-device evidence: `Phase11RecoveryIdentitySessionTest` passes 5/5 on Samsung SM-A065F.

### 11.4 — Concurrent writes and large-history measurement ✅

Completed 2026-09-04.

- A backup-held Room read transaction blocks an actual concurrent write and `CorrectionService` correction until the coherent snapshot is complete; the snapshot contains the committed pre-write generation only.
- A backup started while a correction write transaction is paused between its event header and financial deltas never serializes a partial correction. It waits for the write boundary and observes a fully committed generation.
- A physical-device stress history of 5,000 events (4,500 original expenses plus 500 corrections), 10,000 financial rows, and 5,000 actor links is covered by complete history, detail, analytics, account-ledger, encrypted backup, and two full replacement restores.
- On Samsung SM-A065F, measured times were 1,093 ms seed, 164 ms complete history, 7 ms detail, 17 ms analytics, 88 ms account ledger, 1,478 ms encrypted backup, 5,243 ms first restore, and 12,945 ms second restore. The encrypted payload was 6,117,978 bytes (5.84 MiB), and measured heap growth was 23,437,264 bytes (22.35 MiB) within a 256 MiB heap.
- Both restores preserve the 5,000-event/10,000-row/5,000-link generation, remove between-restore mutations, and invalidate supplied sessions.
- The measured queries, memory, backup, and recovery paths remain comfortably inside the checkpoint bounds. No production query, schema, snapshot-boundary, or frozen-contract optimization is justified at this scale.

Focused physical-device evidence: `Phase11ConcurrentHistoryTest` passes 3/3 on Samsung SM-A065F.

### 11.5 — Lower-of-cost/NRV trigger reassessment ✅

Completed 2026-09-04. See `PHASE11_5_NRV_TRIGGER_REASSESSMENT.md`.

- The current official IAS 2 and IFRIC boundaries were rechecked: lower-of-cost/NRV remains a formal requirement where applicable, and necessary selling costs require entity-specific evidence and judgement.
- No approved trigger is demonstrated by the repository or supplied business facts: no routine material shelf-life carryover, repeated evidence-based below-cost markdown, material period-end price deterioration, longer-lived/general-merchandise authoritative costing, or external reporting requirement is established.
- Existing actual sale prices and the mutable default sale rate are insufficient to derive NRV. The model has no approved period-end expected-price, completion/selling-cost, inventory-aging, materiality, or assessment-evidence policy.
- Formal NRV write-down/reversal support remains conditionally deferred. Reports continue to disclose `cost_based_management` / `cost_only_no_nrv` and make no statutory or IFRS-compliance claim.
- No formula, threshold, Room schema, bridge operation, UI action, or production code changed. The decision record defines the exact reopen questions and minimum separately approved design if a trigger later appears.

### 11.6 — Full regression and handoff ⏳

Run host, bridge, UI/package, focused device, and complete isolated-device gates; verify immutable hashes; record limitations; and hand off to Phase 12 only after the hardening matrix closes.

## 4. Verification at this checkpoint

- Bridge drift gate: v4, 86 operations, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Frozen logic specification: `45E5BDDABE8861A1B3DF7C5666AC7673078439A89FA5628061AD293A865F9DEA` (unchanged).
- Frozen SQL blueprint: `FB701C90D2DD97A1E6D962154692C48EA0F4E77E9300F9DFAC653D4AB67BBC51` (unchanged).
- Kotlin and Android-test Kotlin compilation: pass.
- Phase 11 focused device tests: 23/23 pass (15 cloud durability/provider-failure + 5 restore/identity/session + 3 concurrent-write/large-history).
- Reused costing/reporting boundary device tests for checkpoint 11.5: 16/16 pass (`Phase3CostingTest`, `Phase8DashboardReportsTest`, and `Phase8CostingPlanningTest`; already included in the complete suite total).
- Host-side debug unit tests: 7/7 pass.
- Complete isolated-QA Samsung instrumentation regression: 247/247 pass, 0 skipped, 0 failed.

## 5. Known external gates

Production OAuth consent, live Drive upload/revocation/quota testing, and replacement-device recovery require a configured Google OAuth client and remain final acceptance work for Phases 14–16. The optional owner-phone viewer remains deferred and, if approved, must be read-only.

## 6. Current next action

Continue checkpoint 11.6 with the final host, bridge, UI/package, focused-device, complete isolated-device, immutable-hash, limitations, and Phase 12 handoff gates.

## Plain English

Phase 11 has proven cloud failure handling, destructive recovery, coherent backup during writes/corrections, and repeated recovery of a measured 5,000-event history. It also rechecked the advanced lower-selling-value question: the app lacks the business evidence needed to calculate it honestly, and no trigger has been supplied, so the clear “recorded cost only” warning remains instead of guessing. The final Phase 11 regression and handoff are next.
