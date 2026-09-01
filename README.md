# Saaf Milk — Real Hybrid Rebuild

Status: RECONCILED 2026-09-01 — hybrid Phases 0–8 are complete and Phase 9 Runtime / Navigation / Device Integration is in progress. Grouped navigation, isolated QA data, predictable Android Back behavior, structural hardening, and secure Activity/WebView recreation are physical-device verified. All 78 frozen bridge operations remain native-backed. Substantial Phase 9/11 native prerequisites completed early remain credited without declaring those later phases complete.

Latest physical-device checkpoint: the complete isolated-QA connected-device suite passes 214/214; host-side tests pass 7/7.

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
- `bridge/CONTRACT.md` and `bridge/contract.json` — frozen human-readable and machine-readable bridge contract version 2.
- `PHASE7.md` — completed packaged React/WebView/Android vertical slice, authenticated bridge proof, physical-device evidence, and Phase 8 handoff.
- `PHASE8.md` — completed screen capability ledger, all 21 native-backed milestones, verification, limitations, and Phase 9 handoff.
- `PHASE9.md` — active runtime/navigation/device-integration ledger, completed grouped-navigation, isolated-QA, Android Back, structural-hardening, and secure-recreation milestones, plus the next gate.

## Immutable baselines

Logic specification SHA-256:
`45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`

SQL blueprint SHA-256:
`fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

## Current phase

**Phase 8 — Screen-by-Screen Native UI Wiring: COMPLETE**

The Phase 7 shell remains intact. Phase 8 provides native-backed management screens, Attendance, editable store identity, core shop-wide rates, Expense Categories, optional Opening State setup/correction, Daily Work, deliveries/wholesale, customer/supplier ledgers, History/Corrections, Cash Count/Daily Close, Dashboard/Reports, Costing/Planning, Staff/Security, and Encrypted Backup/Restore. Currency remains fixed to PKR. Posting behavior remains native-only; deliveries never duplicate sale revenue; ledgers and reports display canonical native values without React-side accounting; corrections and close/reopen actions remain append-only; physical counts never create balancing money entries. The Access area provides private staff enrollment/reset, role/status control, personal passphrase changes, owner recovery, and durable security history without React-side authority. The Backup area keeps file selection, encryption, inspection, and restore inside Android-owned boundaries; a successful restore replaces the active copy and invalidates every session. The runtime implements all 78 frozen operations. The complete connected-device regression passes 211/211.

The current release architecture is multi-user but device-local: named roles share one installation and one Room database. The approved next direction is periodic encrypted off-device backup, with Google Drive as the example provider and the exact cadence/retention/network rules deferred to a Phase 10 design gate. Backup protects and transfers a verified snapshot; it is not live synchronization between two writable phones. That gate will separately decide whether the owner's phone should offer a safe read-only view of the latest snapshot.

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

Phase 9 — Runtime / Navigation / Device Integration ◐ (grouped navigation, isolated QA data, Android Back, structural hardening, and secure Activity/WebView recreation complete; targeted refresh, keyboard/insets, and renderer recovery remain)

Phase 10 — Functional Product Completion ⏳ (includes the periodic encrypted Google Drive/cloud-backup design gate, approved implementation, and a separate owner-phone snapshot-view decision)

Phase 11 — Integrity / Recovery / Concurrency ◐ (security/backup credit; local concurrency, hybrid recovery, and periodic-cloud-backup integrity remain)

Phase 12 — UI Fidelity ⏳

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
- Bridge contract: version 2, 78 endpoints, generated Kotlin/TypeScript projections and both pre-build drift gates pass; v2 adds least-privilege active expense references without changing endpoints.
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
- Complete isolated-QA connected-device instrumentation regression: 214/214 passed, 0 skipped, 0 failed.

## Next gate

Phases 3–8 are closed and Phase 9 is active. Every retained Phase 8 screen family and all 78 bridge operations remain native-backed and device-proven. Grouped navigation, separate QA storage, Android Back coordination, useful fragmentation, and secure Activity/WebView recreation are complete; the full isolated-QA Samsung suite passes 214/214. Next, implement deliberate keyboard, system-bar, and display-inset behavior. Google Drive scheduling remains deferred to the Phase 10 design gate. See `PHASE9.md` and `ROADMAP_RECONCILIATION_2026-08-27.md`.
