# Phase 10 — Functional Product Completion

Status: **COMPLETE — checkpoints 10.1–10.5 complete; handed to Phase 11**

## 1. Purpose

Phase 10 closes the intended product capability matrix without reopening the approved accounting, persistence, security, user-role, costing, or bridge contracts. Every retained capability must end this phase in one of three honest states:

1. genuinely native-backed and wired through the production React/WebView path;
2. explicitly deferred to a named later phase with a concrete reason;
3. deliberately presentation-only and unable to create false business success.

The phase also owns the approved design gate for periodic encrypted off-device backup, using Google Drive as the example provider. Backup remains different from live synchronization. The owner-phone decision must separately distinguish a read-only latest-verified-snapshot view from any future multi-writer system.

## 2. Locked boundaries

- Preserve the 86-operation contract-v4 surface and its drift checker. Version 4 is the approved additive Phase 10.4 cloud-backup amendment; future changes still require the documented version/hash workflow.
- Preserve native Room data as the only business truth; React must not calculate or persist a second ledger.
- Preserve verified users, permissions, memory-only sessions, actor attribution, audit history, corrections, daily-close rules, costing rules, and encrypted backup boundaries.
- Existing installations must not receive artificial users, opening entries, costing sources, balances, or other invented business history.
- The shop device must remain useful offline.
- Do not present periodic backup, second-device restore, or a read-only snapshot as live synchronization.

## 3. Ordered completion plan

### 10.1 — Capability-matrix closure audit ✅

Reconcile the approved capability inventory, the 78 bridge operations, the native read/command services, and every retained production screen. Record any genuine missing, dead, duplicated, misleading, or UI-only path. This checkpoint is assessment-first and must not invent new product scope.

Completed 2026-09-01. `PHASE10_CAPABILITY_AUDIT.md` confirms all 78 operations and all 16 retained screen/workflow families are genuinely native-backed. It records two narrow local planning-presentation gaps for checkpoint 10.2, presentation preferences/receipt export for Phase 12, inactive prototype cleanup for Phase 13, and release optimization for Phase 16.

### 10.2 — Local capability closure ✅

Implement only confirmed Phase 10 gaps. Keep refreshes screen-scoped where the frozen contract supports them, and add bridge batching only when measurement shows that it is useful and correctness-neutral.

Completed 2026-09-01. Contract version 3 retains the same 78 operations and every version-2 request shape while extending `query.planning` with native-authoritative:

- recorded spending and actual-minus-plan variance for each saved budget;
- a derived sales target, actual sales, and sales-minus-target variance for Milk plans;
- actual collections and collected-minus-target variance for each collection target.

Kotlin derives and rounds every money value from canonical effective events and saved rate snapshots. React only formats the returned PKR-paisa strings. The display is isolated in `PlanningStatusCards.tsx`, so later UI fidelity work can rearrange the cards without touching calculations. Focused device verification passes 6/6 and the complete isolated Samsung suite passes 219/219.

### 10.3 — Periodic encrypted cloud-backup design gate ✅

**Completed and approved 2026-09-04.**

`PHASE10_CLOUD_BACKUP_DESIGN_PROPOSAL.md` approved Google Drive `appDataFolder`, owner-controlled authorization, approximate daily WorkManager scheduling, a recoverable cloud-vault encryption envelope, verified idempotent uploads, 14-daily/8-weekly/12-monthly retention, full-replacement restore, and a later read-only owner-phone snapshot mode. The owner's “Let's continue” instruction authorized checkpoint 10.4 implementation.

Produce an approval-ready design covering provider/account ownership, authorization and revocation, encryption before upload, file naming/versioning, cadence, retention, network/charging policy, missed-run retry, idempotency, integrity verification, notification, quota/cost behavior, restore selection, privacy, and safe offline operation.

The same gate must make a separate decision about owner-phone access:

- no owner-phone view;
- a read-only view of the latest verified snapshot; or
- defer any true synchronization to a separately approved future architecture.

The design gate is closed. Later changes to provider scope, retention, recovery-key semantics, or multi-device behavior still require an explicit amendment.

### 10.4 — Approved minimum implementation ✅

Completed 2026-09-04. The native app now provides owner-only Google Drive connection/revocation, a self-contained AES-256-GCM cloud envelope, Android-Keystore and Argon2id recovery-key wrapping, coherent Room snapshots, immutable retry outbox, verified idempotent Drive uploads, unique WorkManager scheduling, retention, status/history, action-required notifications, and full-replacement cloud restore through the existing safety-snapshot/session-invalidation engine. React renders status and sends intent only. Contract v4 adds eight cloud operations for a total of 86. The optional read-only owner-phone view remains deferred; live synchronization remains excluded.

See `PHASE10_4_IMPLEMENTATION_REPORT.md` for the implementation boundary and evidence.

### 10.5 — Phase verification and handoff ✅

Completed 2026-09-04. The bridge verifier passes at v4/86 operations, UI lint and packaged-asset build pass, host tests pass 7/7, focused cloud tests pass 5/5, and the complete isolated-QA Samsung suite passes 224/224 with zero skipped or failed. Real production OAuth consent, live Drive upload/revocation, and replacement-device acceptance require a configured OAuth client and remain assigned to Phases 14–16.

## 4. Deferred presentation-performance note

The 2026-09-01 assessment found no reason for a broad performance rewrite. Its focused presentation candidates are recorded under Phase 12 UI Fidelity: remove the unconditional restoration delay, measure asynchronous WebView startup, and stage first-workspace data while avoiding duplicate dashboard reads. Large-history query-shape hardening belongs to Phase 11; R8, resource shrinking, Baseline Profiles, Startup Profiles, and release-grade benchmark configuration belong to Phase 16.

## 5. Current checkpoint

Begin Phase 11 integrity/recovery/concurrency hardening. Priorities are cancellation/process-death recovery across outbox/upload stages, prolonged-offline and missed-schedule behavior, large-history measurement, repeated restore/wrong-passphrase behavior, account-switch/Keystore-loss cases, and retention races. The local Room database remains business truth and cloud backup remains recovery—not synchronization.

## Plain English

Phase 10 is complete. Every retained screen remains native-backed, and the approved encrypted Google Drive recovery pipeline is implemented without turning React or Drive into a second ledger. Phase 11 now stress-tests failure, concurrency, and repeated recovery; later device/release phases validate a real production Google account and second-device restore.
