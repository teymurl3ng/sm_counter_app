# Saaf Milk — Real Hybrid Rebuild

Status: RECONCILED 2026-09-05 — hybrid Phases 0–12 are complete; Phase 13 Dead/Mock/Misleading UI Cleanup is active with checkpoints 13.1–13.2 complete. Contract v4 exposes 86 native-backed operations. The unreachable 23-file, 8,370-line browser-local prototype and six unused dependencies are removed; the production import graph is now 85 reachable files / 85 total / zero unreachable.

Latest physical-device checkpoint: Phase 12.6 passes the complete 258/258 isolated-QA regression across its authenticated 257-test and empty-identity 1-test partitions on Samsung SM-A065F / Android 16, with zero skips or failures. The final visual audit reproduces 83 screenshots / 58 states at 720 × 1600. The Phase 12.5 release-like baseline remains 822 ms median / 871 ms p95 cold start, 327.9 ms median / 378.3 ms p95 React ready, and 24.0 ms median / 29.6 ms p95 painted navigation with zero bridge calls. Host-side tests pass 7/7.

Product scope: Saaf Milk is the current working product identity, and Milk/Yogurt are the current validated domain—not permanent limits. The product is intended to expand carefully to other products and suitable owner-managed shop contexts without pretending to be a corporate ERP or complete statutory accounting system. See `PRODUCT_SCOPE.md`.

## What this repository contains

- `phase18/` — Phase 18-derived native project. Its logic specification and SQL blueprint remain frozen; approved hybrid native extensions are implemented in the Kotlin/Room source.
- `ui/` — the latest AI Studio UI source supplied for the new product layer.
- `PRODUCT_SCOPE.md` — authoritative target-user, product-breadth, proportional-quality, benchmark, and expansion policy.
- `DESIGN.md` — authoritative Phase 12 visual target, presentation tokens, Android priorities, accessibility rules, and proportional change gate.
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
- `PHASE11.md` — completed integrity/recovery/concurrency ledger and final verification.
- `PHASE11_5_NRV_TRIGGER_REASSESSMENT.md` — completed lower-of-cost/NRV trigger decision, evidence gaps, reopen conditions, and minimum future design boundary.
- `PHASE11_6_HANDOFF.md` — final regression matrix, immutable hashes, known limitations, and guarded Phase 12 entry conditions.
- `PHASE12.md` — completed UI-fidelity checkpoints and locked presentation-only boundaries.
- `PHASE12_SCREEN_AUDIT.md` — completed physical-device screenshot matrix, visual findings, Phase 12.2 resolutions, and next gate.
- `PHASE12_3_NAVIGATION_REVIEW.md` — completed navigation decisions, source traceability, preference boundary, implementation, and physical-device evidence.
- `PHASE12_4_SCREEN_REVIEW.md` — completed screen-by-screen responsive/accessibility application, executable quality gate, physical-device evidence, and limitations.
- `PHASE12_5_PERFORMANCE_REVIEW.md` — completed release-like startup/navigation measurements, performance guardrails, evidence, and no-change optimization decision.
- `PHASE12_6_HANDOFF.md` — final regression matrix, before/after evidence, immutable hashes, known limitations, and guarded Phase 13 entry conditions.
- `PHASE13.md` — active dead/mock/misleading UI cleanup checkpoints and locked boundaries.
- `PHASE13_1_INVENTORY.md` — production import graph, locked prototype/dependency removal list, active-tree findings, and source classifications.
- `PHASE13_2_PROTOTYPE_REMOVAL.md` — removed source/dependency surface, measured CSS/package reduction, hashes, and physical-device proof.

## Immutable baselines

Logic specification SHA-256:
`45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`

SQL blueprint SHA-256:
`fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

## Current phase

**Phase 13 — Dead/Mock/Misleading UI Cleanup: IN PROGRESS (checkpoints 13.1–13.2 complete)**

Phase 8's native-backed shop screens and all 78 frozen operations remain intact. Phase 9 adds grouped role-aware navigation, predictable Android Back behavior, a separate QA installation, focused file boundaries, one non-reloading React/WebView presentation, secure Activity recreation, deliberate keyboard/system-inset behavior, targeted state refresh with bridge-transfer measurement, and native recovery from main-page or WebView-renderer failure. A recovery reload creates a fresh WebView and revalidates the memory-only session before protected information appears. The complete isolated-QA physical-device regression passes 218/218.

The application remains offline-first and each writable installation owns one Room database. Phase 10 added periodic encrypted off-device recovery through Google Drive `appDataFolder`. Phase 11 recovers authenticated outbox files after process death, coalesces overlapping runs, preserves retry identity through cancellation/offline/quota paths, hardens repeated replacement, and proves coherent backup around writes/corrections. A measured 5,000-event history remains within query, memory, backup, and repeated-restore bounds. The NRV trigger reassessment found no supplied evidence authorizing a formal write-down/reversal subsystem, so honest `cost_only_no_nrv` disclosure remains. The final source/package/device matrix passes, and backup remains recovery—not synchronization between writable phones.

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

Phase 11 — Integrity / Recovery / Concurrency ✅ (all six checkpoints complete; 247/247 final device regression)

Phase 12 — UI Fidelity ✅ (all six checkpoints complete; 83-screen final evidence, release-like performance, and 258/258 device regression verified)

Phase 13 — Dead/Mock/Misleading UI Cleanup ⏳ (13.1 inventory and 13.2 unreachable prototype removal complete; 13.3 active semantic audit next)

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
- Phase 12.4 responsive/accessibility, navigation-flow, and system-inset checks: 5/5 passed on the physical device.
- Phase 12.4 screen-quality capture: 1/1 passed across 83 screenshots and 58 retained states on the physical device.
- Phase 12.5 profileable release-like performance gate: 2/2 passed; cold start p95 871 ms, React-ready p95 378.3 ms, and painted navigation p95 29.6 ms.
- Phase 12.6 focused presentation/device gate: 11/11 passed, 0 skipped, 0 failed.
- Phase 12.6 complete isolated-QA device regression: 258/258 passed across the 257-test authenticated and 1-test empty-identity visual partitions, 0 skipped, 0 failed.
- Phase 12.6 final visual evidence: 83/83 PNGs readable at 720 × 1600 across 58 retained states; manifest SHA-256 `8E9937FC972B2EE36D8E31D5FB58C0C7C86189D4C8A8F116AFA91C727B45F1BE`.

## Next gate

Phases 3–12 are closed. Phase 13.1 classified the complete UI graph, and Phase 13.2 removed the locked 23-file, 8,370-line browser-local prototype plus six unused dependencies. The active JavaScript content hash is unchanged, generated CSS falls from 85.71 kB to 34.03 kB, and TypeScript/Vite, Android packaging, 7/7 host tests, and 12/12 focused Samsung tests pass. Checkpoint 13.3 now audits active controls, success/disabled states, labels, and presentation calculations; it will resolve one misleading heterogeneous opening-balance total without inventing a React accounting formula. Use Phases 14–16 for live Google consent/Drive, second-device, shopkeeper, and release acceptance. See `PHASE13.md`, `PHASE13_1_INVENTORY.md`, `PHASE13_2_PROTOTYPE_REMOVAL.md`, `PHASE12_6_HANDOFF.md`, and `ROADMAP_RECONCILIATION_2026-08-27.md`.
