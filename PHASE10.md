# Phase 10 — Functional Product Completion

Status: **IN PROGRESS** — opened 2026-09-01

## 1. Purpose

Phase 10 closes the intended product capability matrix without reopening the approved accounting, persistence, security, user-role, costing, or bridge contracts. Every retained capability must end this phase in one of three honest states:

1. genuinely native-backed and wired through the production React/WebView path;
2. explicitly deferred to a named later phase with a concrete reason;
3. deliberately presentation-only and unable to create false business success.

The phase also owns the approved design gate for periodic encrypted off-device backup, using Google Drive as the example provider. Backup remains different from live synchronization. The owner-phone decision must separately distinguish a read-only latest-verified-snapshot view from any future multi-writer system.

## 2. Locked boundaries

- Preserve the 78-operation contract-v2 surface and its drift checker unless an explicitly approved additive capability is required.
- Preserve native Room data as the only business truth; React must not calculate or persist a second ledger.
- Preserve verified users, permissions, memory-only sessions, actor attribution, audit history, corrections, daily-close rules, costing rules, and encrypted backup boundaries.
- Existing installations must not receive artificial users, opening entries, costing sources, balances, or other invented business history.
- The shop device must remain useful offline.
- Do not present periodic backup, second-device restore, or a read-only snapshot as live synchronization.

## 3. Ordered completion plan

### 10.1 — Capability-matrix closure audit

Reconcile the approved capability inventory, the 78 bridge operations, the native read/command services, and every retained production screen. Record any genuine missing, dead, duplicated, misleading, or UI-only path. This checkpoint is assessment-first and must not invent new product scope.

### 10.2 — Local capability closure

Implement only confirmed Phase 10 gaps. Keep refreshes screen-scoped where the frozen contract supports them, and add bridge batching only when measurement shows that it is useful and correctness-neutral.

### 10.3 — Periodic encrypted cloud-backup design gate

Produce an approval-ready design covering provider/account ownership, authorization and revocation, encryption before upload, file naming/versioning, cadence, retention, network/charging policy, missed-run retry, idempotency, integrity verification, notification, quota/cost behavior, restore selection, privacy, and safe offline operation.

The same gate must make a separate decision about owner-phone access:

- no owner-phone view;
- a read-only view of the latest verified snapshot; or
- defer any true synchronization to a separately approved future architecture.

No provider integration begins before this design is approved.

### 10.4 — Approved minimum implementation

Implement only the cloud-backup and owner-phone scope approved at checkpoint 10.3. Phase 11 retains scheduling, retry, integrity, concurrency, repeated recovery, and large-history hardening responsibilities.

### 10.5 — Phase verification and handoff

Run the relevant host and isolated physical-device regression, update the capability ledger, document remaining limitations honestly, and hand the verified result to Phase 11.

## 4. Deferred presentation-performance note

The 2026-09-01 assessment found no reason for a broad performance rewrite. Its focused presentation candidates are recorded under Phase 12 UI Fidelity: remove the unconditional restoration delay, measure asynchronous WebView startup, and stage first-workspace data while avoiding duplicate dashboard reads. Large-history query-shape hardening belongs to Phase 11; R8, resource shrinking, Baseline Profiles, Startup Profiles, and release-grade benchmark configuration belong to Phase 16.

## 5. Current checkpoint

Begin **10.1 — Capability-matrix closure audit**. Application code has not yet changed for Phase 10.

## Plain English

Phase 10 is the final completeness check before deeper hardening. We will verify that every button and screen either performs a real saved shop action or is clearly documented as intentionally deferred. After that, we will design automatic encrypted Google Drive backup in plain terms and bring the design back for approval before connecting the app to any cloud account.
