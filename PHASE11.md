# Phase 11 — Integrity / Recovery / Concurrency

Status: **IN PROGRESS — checkpoint 11.1 complete**

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

### 11.2 — Cancellation, scheduling, offline, and provider failure matrix ◐

Next: prove cancellation and retry at snapshot, outbox, authorization, upload, verification, retention, and cleanup boundaries; prolonged offline and missed-run behavior; quota; provider sign-out/revocation; account switch; Keystore loss; and retention races.

### 11.3 — Repeated restore, identity, and session recovery ⏳

Stress repeated wrong-passphrase attempts, repeated full replacement, safety-snapshot failure paths, restore selection, actor preservation, session invalidation, disablement, and concurrent-session boundaries.

### 11.4 — Concurrent writes and large-history measurement ⏳

Exercise backup during writes and corrections, large business histories, read-model/query bounds, memory use, and repeated recovery. Optimize only measured bottlenecks without weakening the coherent Room snapshot boundary.

### 11.5 — Lower-of-cost/NRV trigger reassessment ⏳

Reassess the deliberately deferred NRV subsystem against actual materiality and business triggers. This checkpoint is a decision record first; no new accounting formula is authorized by Phase 11 alone.

### 11.6 — Full regression and handoff ⏳

Run host, bridge, UI/package, focused device, and complete isolated-device gates; verify immutable hashes; record limitations; and hand off to Phase 12 only after the hardening matrix closes.

## 4. Verification at this checkpoint

- Bridge drift gate: v4, 86 operations, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Frozen logic specification: `45E5BDDABE8861A1B3DF7C5666AC7673078439A89FA5628061AD293A865F9DEA` (unchanged).
- Frozen SQL blueprint: `FB701C90D2DD97A1E6D962154692C48EA0F4E77E9300F9DFAC653D4AB67BBC51` (unchanged).
- Kotlin and Android-test Kotlin compilation: pass.
- Phase 11 focused durability/concurrency device tests: 4/4 pass.
- Host-side debug unit tests: 7/7 pass.
- Complete isolated-QA Samsung instrumentation regression: 228/228 pass, 0 skipped, 0 failed.

## 5. Known external gates

Production OAuth consent, live Drive upload/revocation/quota testing, and replacement-device recovery require a configured Google OAuth client and remain final acceptance work for Phases 14–16. The optional owner-phone viewer remains deferred and, if approved, must be read-only.

## 6. Current next action

Continue checkpoint 11.2 with deterministic cancellation and missed-run/offline retry tests, then cover account/Keystore/retention failure races. Preserve the same pending snapshot identity and encrypted bytes until remote verification succeeds.

## Plain English

Phase 11 has started. Its first checkpoint closes two crash windows in cloud backup and prevents overlapping workers from uploading duplicate recovery points. The next work is the broader cancellation, offline, account, key, and retention failure matrix.
