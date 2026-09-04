# Saaf Milk — Real Hybrid Rebuild

Status: RECONCILED 2026-09-04 — hybrid Phases 0–10 are complete and Phase 11 integrity/recovery/concurrency hardening is in progress. Contract v4 exposes 86 native-backed operations. Phase 11 checkpoints 11.1–11.5 close cloud outbox/process-death, provider-failure, cancellation, repeated-restore, identity/session, concurrent-write/correction, measured large-history, and deferred-NRV decision paths.

Latest physical-device checkpoint: the complete isolated-QA connected-device suite passes 247/247; host-side tests pass 7/7.

## What this repository contains

- `phase18/` — Phase 18-derived native project. Its logic specification and SQL blueprint remain frozen; approved hybrid native extensions are implemented in the Kotlin/Room source.
- `ui/` — the latest AI Studio UI source supplied for the new product layer.
- `POSTMORTEM.md` — locked fresh-start playbook.
- `PHASE0.md` — immutable baseline record and hashes.
- `PHASE1.md` — complete source-level capability inventory and mapping.
- `PHASE2.md` — approved desired product capability set and Phase 3 implementation priorities.
- `PHASE3.md` — native reuse map, first native expansion, proposed persistence changes, and approval gate.
- `PHASE3_USER_ACCOUNTS_DESIGN_PROPOSAL.md` — approved native identity, credential, authorization, session, recovery, audit, migration, and secure-backup contract.
- `ROADMAP_RECONCILIATION_2026-08-27.md` — authoritative current phase ledger, early-work credits, remaining gates, and revised execution order.
- `PHASE3_COGS_PROFITABILITY_DECISION_PROPOSAL.md` — approved standards-based inventory-costing, spoilage-valuation, gross-profit, and honest operating-result contract.
- `PHASE3_COGS_IMPLEMENTATION_DESIGN.md` — implemented schema-8 costing source/projection, service, migration, backup, and test design.
- `PHASE4.md` — completed criterion-by-criterion native automated-test audit, gap closure, evidence matrix, and verification record.
- `PHASE5.md` — completed native read-model expansion, permission boundary, verification, and next gate.
- `PHASE5_DTO_CATALOG.md` — frozen hybrid-facing Kotlin query/DTO catalog, representation rules, code sets, and security exclusions.
- `PHASE6.md` — completed canonical bridge-contract boundary, generated projections, drift gate, verification, and Phase 7 handoff.
- `bridge/CONTRACT.md` and `bridge/contract.json` — frozen human-readable and machine-readable bridge contract version 4.
- `PHASE7.md` — completed packaged React/WebView/Android vertical slice, authenticated bridge proof, physical-device evidence, and Phase 8 handoff.
- `PHASE8.md` — completed screen capability ledger, all 21 native-backed milestones, verification, limitations, and Phase 9 handoff.
- `PHASE9.md` — completed runtime/navigation/device-integration ledger, including grouped navigation, lifecycle, keyboard/insets, targeted refresh, renderer recovery, and physical-device evidence.
- `PHASE10_CLOUD_BACKUP_DESIGN_PROPOSAL.md` — approved periodic encrypted Google Drive backup, recovery-key, scheduling, retention, restore, and owner-phone scope design.
- `PHASE10_4_IMPLEMENTATION_REPORT.md` — implemented cloud-backup boundary, verification evidence, build configuration, and remaining live-provider gates.
- `PHASE11.md` — active integrity/recovery/concurrency plan, completed cloud and destructive-restore checkpoints, evidence, and remaining hardening gates.
- `PHASE11_5_NRV_TRIGGER_REASSESSMENT.md` — completed lower-of-cost/NRV trigger decision, evidence gaps, reopen conditions, and minimum future design boundary.

## Immutable baselines

Logic specification SHA-256:
`45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`

SQL blueprint SHA-256:
`fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

## Current phase

**Phase 11 — Integrity / Recovery / Concurrency: IN PROGRESS (checkpoints 11.1–11.5 complete)**

Phase 8's native-backed shop screens and all 78 frozen operations remain intact. Phase 9 adds grouped role-aware navigation, predictable Android Back behavior, a separate QA installation, focused file boundaries, one non-reloading React/WebView presentation, secure Activity recreation, deliberate keyboard/system-inset behavior, targeted state refresh with bridge-transfer measurement, and native recovery from main-page or WebView-renderer failure. A recovery reload creates a fresh WebView and revalidates the memory-only session before protected information appears. The complete isolated-QA physical-device regression passes 218/218.

The application remains offline-first and each writable installation owns one Room database. Phase 10 added periodic encrypted off-device recovery through Google Drive `appDataFolder`. Phase 11 checkpoints 11.1–11.5 recover authenticated outbox files after process death, coalesce overlapping runs, preserve retry identity through cancellation/offline/quota paths, harden repeated replacement, and prove coherent backup around writes/corrections. A measured 5,000-event history remains within query, memory, backup, and repeated-restore bounds. The NRV trigger reassessment found no supplied evidence authorizing a formal write-down/reversal subsystem, so honest `cost_only_no_nrv` disclosure remains and no accounting formula was invented. Backup is still recovery—not synchronization between writable phones.

## Approved security track

- Application users are security principals and are distinct from employee/salary profiles. A user may optionally link to one employee, but an employee record does not grant application access.
- Authentication and authorization must be native-backed. React may present login and permission-aware controls, but it must never decide or self-assert a user's role.
- The approved model defines native users, Argon2id credentials, fixed role/permission bundles, memory-only sessions, first-owner bootstrap, deactivation, owner recovery, audit actors, and migration behavior.
- Once any application user exists, daily close/reopen rejects typed actor claims and requires a verified native session/user ID. Schema-6 history remains explicitly legacy/unverified.
- Portable encrypted backup format 2 is implemented with Argon2id-derived AES-256-GCM protection, security-state portability, tamper/wrong-passphrase rejection, legacy format-1 import compatibility, and post-restore session invalidation. Its authenticated export/inspection/restore UI is wired through Android-owned document pickers and short-lived one-use restore tickets. The numeric quick-PIN shortcut remains deliberately deferred.
- Staff accounts use private enrollment/reset: an authorized owner/admin creates the identity and role, while the staff member chooses the credential. A user marked as requiring setup cannot authenticate with any previous verifier.
- Automatic app-private snapshots now use AES-256-GCM under a non-exportable random Android Keystore key. Their authenticated payload includes security state but excludes active sessions and failed-login/lockout timing state. They are intentionally device-local; complete security restore is allowed only before owner setup on the same installation.

## Locked architecture

Business truth:
`Phase 18 logic/specification + approved native extensions`

Read truth:
`Kotlin/native read models`

Bridge:
`canonical bridge contract`

Presentation:
`React/AI Studio`

React must never become a second accounting engine.

## Reconciled workflow

The original locked phase definitions remain in `POSTMORTEM.md`. `ROADMAP_RECONCILIATION_2026-08-27.md` records current status and cross-phase credit without renumbering or silently rewriting that roadmap.

Phase 0 — Freeze & Baseline ✅

Phase 1 — Capability Inventory ✅

Phase 2 — Desired Product Capability Decisions ✅

Phase 3 — Native Phase 18 Capability Expansion ✅

Phase 4 — Native Automated Tests ✅

Phase 5 — Native Read-Model Expansion ✅

Phase 6 — Bridge Contract (`bridge/CONTRACT.md`) ✅

Phase 7 — Hybrid Shell / Vertical Slice ✅

Phase 8 — UI Wiring ✅ (all 21 milestones complete; all 78 bridge operations native-backed and device-proven)

Phase 9 — Runtime / Navigation / Device Integration ✅ (all six milestones complete, including secure renderer recovery; 218/218 device regression)

Phase 10 — Functional Product Completion ✅ (checkpoints 10.1–10.5 complete; encrypted Drive recovery implemented; 224/224 device regression)

Phase 11 — Integrity / Recovery / Concurrency ◐ (in progress; checkpoints 11.1–11.5 complete; final regression and handoff are next)

Phase 12 — UI Fidelity ⏳ (includes the deferred measured presentation-performance note; no broad optimization rewrite is approved)

Phase 13 — Dead/Mock/Misleading UI Cleanup ⏳

Phase 14 — Final Device QA ⏳ (includes real scheduled-cloud-backup, retry, and second-device restore testing)

Phase 15 — Final Shopkeeper QA ⏳ (includes periodic backup/recovery and owner-phone snapshot viewing if approved)

Phase 16 — Release Readiness ⏳ (includes cloud-provider authorization, privacy, quota, and recovery readiness if cloud backup ships)

## Rule for phase records

Every completed or active hybrid-rebuild phase gets a root `PHASEN.md` file with its status, scope, changes, verification, and relevant hashes. Do not overwrite prior phase records; corrections create an explicitly named patch note in the current phase while the phase record records the final state.

## Latest native verification

- Frozen logic-spec and SQL hashes still match the Phase 0 values.
- Room schema version 8 and migrations through `MIGRATION_7_8` are implemented.
- Kotlin and Android-test Kotlin compilation pass.
- Room schema 3→4 through 7→8 migration validation passes on the connected device; migrations create no artificial opening, security, or costing source truth.
- Phase 3 ledger/capability tests pass.
- Legacy format-1 business backup/restore round trips still pass, including chronological replay of daily close/reopen/reclose history.
- Encrypted format-2 export/restore preserves user identities, roles/status, Argon2id credential verifiers, recovery verifiers, security audit, event attribution, and verified daily-close actor IDs; active sessions are never serialized and all supplied in-memory sessions are invalidated after restore.
- Native first-owner setup, login, logout, owner recovery, passphrase change, private staff enrollment/reset, user/role/status administration, lifecycle session handling, role-aware navigation, and protected-command authorization are implemented.
- Authoritative Milk/Yogurt historical-cost management reporting is implemented with moving weighted average, versioned monthly Yogurt conversion cost, append-only opening cost bases, calibrated yield review, explicit abnormal-loss assessment, deterministic correction replay, honest incomplete-coverage states, and deferred NRV disclosure.
- Encrypted backups preserve costing source truth and rebuild derived cost projections; legacy backups remain supported. Plain security-free backup is rejected when protected costing sources require actor identity.
- Focused user-security plus authenticated-shell suite: 8/8 passed.
- Focused backup/restore suite: 8/8 passed, including portable and local confidentiality checks, wrong-passphrase rejection, tamper rejection, security round-trips, session invalidation, and transient lockout-state exclusion.
- Phase 4 security gap-closure suite: 9/9 passed.
- Phase 5 hybrid read-model suite: 4/4 passed.
- Bridge contract: version 4, 86 endpoints, generated Kotlin/TypeScript projections and both pre-build drift gates pass; v4 retains v3 and adds eight native-owned cloud-backup operations.
- Host-side debug unit tests: 7/7 passed.
- Phase 7 focused hybrid vertical-slice suite: 3/3 passed on the physical device.
- Phase 8 focused Product Management suite: 3/3 passed on the physical device.
- Phase 8 focused Customer Profiles suite: 3/3 passed on the physical device.
- Phase 8 focused Supplier Profiles suite: 3/3 passed on the physical device.
- Phase 8 focused Employees/Attendance suite: 4/4 passed on the physical device.
- Phase 8 focused Settings/Expense Categories suite: 4/4 passed on the physical device.
- Phase 8 focused Opening State suite: 4/4 passed on the physical device.
- Phase 8 focused Daily Operations purchase suite: 4/4 passed on the physical device.
- Phase 8 focused Daily Operations sales suite: 4/4 passed on the physical device.
- Phase 8 focused Daily Operations production suite: 4/4 passed on the physical device.
- Phase 8 focused Daily Operations expense suite: 4/4 passed on the physical device.
- Phase 8 focused Daily Operations payment suite: 4/4 passed on the physical device.
- Phase 8 focused Daily Operations salary-payment suite: 4/4 passed on the physical device.
- Phase 8 focused Daily Operations spoilage suite: 4/4 passed on the physical device.
- Phase 8 focused Deliveries/wholesale suite: 4/4 passed on the physical device.
- Phase 8 focused Customer/Supplier Ledgers suite: 4/4 passed on the physical device.
- Phase 8 focused History/Corrections suite: 4/4 passed on the physical device.
- Phase 8 focused Cash Count/Daily Close suite: 4/4 passed on the physical device.
- Phase 8 focused Dashboard/Reports suite: 4/4 passed on the physical device.
- Phase 8 focused Costing/Planning suite: 5/5 passed on the physical device.
- Phase 8 focused Staff Users/Security Audit suite: 5/5 passed on the physical device.
- Phase 8 focused Encrypted Backup/Restore suite: 5/5 passed on the physical device.
- Phase 10 focused periodic encrypted cloud-backup suite: 5/5 passed on the physical device.
- Phase 11 focused cloud durability/provider-failure suite: 15/15 passed on the physical device.
- Phase 11 focused restore/identity/session suite: 5/5 passed on the physical device.
- Phase 11 focused concurrent-write/large-history suite: 3/3 passed on the physical device.
- Phase 11.5 reused costing/reporting boundary suite: 16/16 passed on the physical device (included in the complete-suite total).
- Phase 9 focused real-renderer termination and secure recovery suite: 2/2 passed on the physical device.
- Phase 10 complete isolated-QA connected-device regression: 224/224 passed, 0 skipped, 0 failed.
- Phase 11 complete isolated-QA connected-device regression: 247/247 passed, 0 skipped, 0 failed.

## Next gate

Phases 3–10 are closed and Phase 11 is active. Checkpoints 11.1–11.5 are complete; 23/23 dedicated Phase 11 tests, the reused 16/16 costing/reporting boundary, and the complete 247/247 isolated Samsung suite pass with zero skipped or failed. Continue checkpoint 11.6 with final regression, limitations, and Phase 12 handoff. Use Phases 14–16 for live Google consent/Drive and second-device release acceptance. See `PHASE11.md`, `PHASE11_5_NRV_TRIGGER_REASSESSMENT.md`, `PHASE10_4_IMPLEMENTATION_REPORT.md`, and `ROADMAP_RECONCILIATION_2026-08-27.md`.
