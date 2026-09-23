# Saaf Milk — Real Hybrid Rebuild

Status: RECONCILED 2026-09-23 — hybrid Phases 0–14 are complete; Phase 14.8 native UI redesign has reached 14.8.50 audit remediation before Phase 15 uncoached shopkeeper QA. Contract v5 exposes 91 native-backed operations. Quick unlock, live Drive, cross-device recovery, Android behavior, accessibility closure, and every recorded Phase 14 defect pass their final gates. The verified hybrid application is retained as the behavioral baseline and temporary fallback while the final presentation moves to Jetpack Compose.

Latest complete physical-device regression: Phase 14.7 passes 273/273 ordinary tests in the isolated `com.saaf.milkshop.qa.tests` instrumentation package and the separately configured 1/1 blank-install visual audit, with zero skips or failures. Host-side tests pass 7/7. The final OAuth-enabled user-facing `.qa` APK is 16,174,564 bytes with SHA-256 `5581218C0761ED3C33CA9D09877D19E4D2654B5B6DF5AC2790C80B364F223A24`.

Phase 14.1–14.5 established the isolated Samsung baseline, all-role UI/native permission checks, live Drive acceptance, data-rich S24-to-A06 restore, and the Android/accessibility audit. Phase 14.6 added secure device-local Quick unlock and closed its security and usability findings. Phase 14.7 fixed every recorded finding, including the replacement-vault fallback and misleading Quick unlock method selection found on the A06. Shared-phone PIN isolation, TalkBack Close activation, portable recovery/restore, and explicit PIN-only and Phone-security-only setup/login pass physically. Ordinary `com.saaf.milkshop` installations/profiles remain untouched.

Product scope: Saaf Milk is the current working product identity, and Milk/Yogurt are the current validated domain—not permanent limits. The product is intended to expand carefully to other products and suitable owner-managed shop contexts without pretending to be a corporate ERP or complete statutory accounting system. See `PRODUCT_SCOPE.md`.

## What this repository contains

- `phase18/` — Phase 18-derived native project. Its logic specification and SQL blueprint remain frozen; approved hybrid native extensions are implemented in the Kotlin/Room source.
- `ui/` — the latest AI Studio UI source supplied for the new product layer.
- `PRODUCT_SCOPE.md` — authoritative target-user, product-breadth, proportional-quality, benchmark, and expansion policy.
- `DESIGN.md` — historical Phase 12 visual authority for the runnable hybrid fallback; the accepted Counter Ledger blueprints govern the native redesign.
- `QUICK_UNLOCK_REQUIREMENT.md` — approved release-blocking biometric/device-credential and per-user short-PIN contract, security boundaries, phase placement, and test states.
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
- `bridge/CONTRACT.md` and `bridge/contract.json` — frozen human-readable and machine-readable bridge contract version 5.
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
- `PHASE13_3_ACTIVE_UI_AUDIT.md` — active-control wiring, permission/disabled/message/calculation audit, opening-balance correction, hashes, and physical-device proof.
- `PHASE13_4_NEGATIVE_GATE.md` — automated cleanup guard, npm/Android enforcement, focused 31-test device matrix, hashes, and next gate.
- `PHASE13_5_HANDOFF.md` — final rebuild/regression matrix, removed-surface proof, 83-screen evidence, immutable hashes, limitations, and guarded Phase 14 entry.
- `PHASE14.md` — active final-device-QA checkpoints, locked boundaries, evidence policy, prerequisites, and current next action.
- `PHASE14_1_DEVICE_QA_BASELINE.md` — connected-device inventory, clean QA installation proof, provider/second-device prerequisites, existing test credit, and evidence hashes.
- `PHASE14_2_ROLE_PERMISSION_QA.md` — complete five-role packaged-UI matrix, native bypass probes, sensitive backup/cloud boundaries, and focused regression evidence.
- `PHASE14_3_LIVE_DRIVE_PREFLIGHT.md` — completed live consent/upload/hidden-listing/offline-retry/inspection/revocation/reconnect evidence and verified defects.
- `PHASE14_4_SECOND_DEVICE_RECOVERY.md` — completed live replacement evidence, forgotten-secret boundary, failure-path proof, and cross-device data verification.
- `PHASE14_5_ACCESSIBILITY_ANDROID_QA.md` — completed large-text, landscape, keyboard, Back, TalkBack, lifecycle, split-screen, theme, and document-picker audit with scored findings.
- `PHASE14_6_QUICK_UNLOCK_QA.md` — completed device-bound Quick unlock implementation, security regression, shared-phone isolation, and physical acceptance.
- `PHASE14_7_DEFECT_CLOSURE.md` — completed Phase 14 defect closure, final regression, APK record, replacement-phone recovery proof, and Phase 15 handoff.
- `PHASE14_8.md` — active native UI redesign/migration plan, protected architecture, incremental coexistence strategy, checkpoints, and retirement gate.
- `PHASE14_8_1_NATIVE_MIGRATION_INVENTORY.md` — complete source-derived map of authentication states, fourteen application areas, 91 bridge actions, native owners, parity gates, and fallback boundaries.
- `PHASE14_8_2_JOURNEY_DIRECTIONS.md` — current UI audit, external pattern evidence, three native information-architecture directions, and the recommended `Today / Work / Records / More` structure.
- `PHASE14_8_2_ROLE_CALIBRATION.md` — earlier five-role shell comparison, now superseded for review until the role policy is accepted.
- `PHASE14_8_2_ROLE_ARCHITECTURE_RESEARCH.md` — researched role definitions, necessity decision, complete screen/action access policy, current permission gaps, and the future scoped-permission model.
- `PHASE14_8_2_CASHIER_VISUAL_DIRECTIONS.md` — three high-fidelity Cashier worlds and the recorded Counter Ledger selection.
- `PHASE14_8_2_COUNTER_LEDGER_WORKFLOWS.md` — accepted Counter Ledger Cashier and Operations implementation blueprint.
- `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md` — accepted Owner, Store manager, and Administrator screen/state blueprint.
- `PHASE14_8_2_COUNTER_LEDGER_ACCESS.md` — accepted branded welcome, account selection, authentication, first-Owner, staff-enrollment, and Quick-unlock blueprint.
- `PHASE14_8_2_COUNTER_LEDGER_REFINEMENT.md` — owner decision to retain the original layouts and apply the typography/readability lessons from the Stitch study.
- `PHASE14_8_3_COMPOSE_FOUNDATION.md` — active Compose foundation, authoritative authentication-to-Sale/Collect slice, lifecycle/accessibility hardening, and real-device evidence.
- `NATIVE_INTERACTION_CONTRACT.md` — required workflow description, stable-tag, and device-test gate for every visible native control.
- `NATIVE_SCREEN_ACCEPTANCE.md` — permanent `qaFast` / `qaScreen` / `qaCheckpoint` gate, risk tiers, inventory, and package/device safety.
- `design/refined-counter-ledger/index.html` — accepted refined original Welcome, Cashier sale, and Owner overview visual reference.
- `STITCH_DESIGN_PREPARATION.md` — verified Stitch connection, approved brief, six-screen comparison evidence, and recorded owner review.
- `design/stitch/comparison.html` and `design/stitch/REVIEW.md` — generated A/B Welcome, Cashier New sale, and Owner Overview proposals retained as comparative evidence; neither treatment is selected wholesale.

## Immutable baselines

Logic specification SHA-256:
`45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`

SQL blueprint SHA-256:
`fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

## Current phase

**Phase 14.8 — Native UI Redesign and Migration: ACTIVE (14.8.50 Cursor-work audit remediation passes host gates, debug/release packaging, lint, and focused A06 adaptive-shell tests; hybrid launcher retained)**

Phase 14 completed the safe `.qa` device baseline, all-five-role authorization matrix, live Drive and real cross-device recovery, Android/accessibility audit, device-bound Quick unlock, defect closure, and complete regression. The final result remains 273/273 ordinary Android tests, the 1/1 visual audit, and 7/7 host tests. The owner then rejected the hybrid interface as the release UX and confirmed native UI as the intended destination. Phase 14.8 redesigns and incrementally migrates the presentation to Compose while retaining the working hybrid build as a behavioral oracle and fallback. Phase 15 will test the native release candidate without coaching.

Earlier native-backed shop screens remain intact. Phase 9's grouped role-aware navigation, separate QA installation, one non-reloading React/WebView presentation, secure Activity recreation, targeted state refresh, and renderer recovery remain protected. A recovery reload creates a fresh WebView and revalidates the memory-only session before protected information appears. The keyboard/inset, Back/draft, rotation, large-text, landscape, and TalkBack findings recorded in Phase 14.5 are closed by Phase 14.7.

The application remains offline-first and each writable installation owns one Room database. Phase 10 added periodic encrypted off-device recovery through Google Drive `appDataFolder`. Phase 11 recovers authenticated outbox files after process death, coalesces overlapping runs, preserves retry identity through cancellation/offline/quota paths, hardens repeated replacement, and proves coherent backup around writes/corrections. A measured 5,000-event history remains within query, memory, backup, and repeated-restore bounds. The NRV trigger reassessment found no supplied evidence authorizing a formal write-down/reversal subsystem, so honest `cost_only_no_nrv` disclosure remains. The final source/package/device matrix passes, and backup remains recovery—not synchronization between writable phones.

## Approved security track

- Application users are security principals and are distinct from employee/salary profiles. A user may optionally link to one employee, but an employee record does not grant application access.
- Authentication and authorization must be native-backed. React may present login and permission-aware controls, but it must never decide or self-assert a user's role.
- The implemented security baseline defines native users, Argon2id credentials, fixed role/permission bundles, memory-only sessions, first-owner bootstrap, deactivation, owner recovery, audit actors, and migration behavior. Phase 14.8 is reviewing a finer permission model before native screen implementation; any accepted change must preserve native deny-by-default enforcement and migration safety.
- Once any application user exists, daily close/reopen rejects typed actor claims and requires a verified native session/user ID. Schema-6 history remains explicitly legacy/unverified.
- Portable encrypted backup format 2 is implemented with Argon2id-derived AES-256-GCM protection, security-state portability, tamper/wrong-passphrase rejection, legacy format-1 import compatibility, and post-restore session invalidation. Its authenticated export/inspection/restore UI is wired through Android-owned document pickers and short-lived one-use restore tickets. The earlier numeric quick-PIN deferral is now superseded for forward work: native device-bound Quick unlock is mandatory in Phase 14.6, while portable cloud recovery remains protected by a high-entropy secret.
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

Phase 13 — Dead/Mock/Misleading UI Cleanup ✅ (all five checkpoints complete; 258/258 final device regression and Phase 14 handoff verified)

Phase 14 — Final Device QA ✅ (all seven checkpoints complete; 273/273 ordinary Android tests, 1/1 visual audit, 7/7 host tests)

Phase 14.8 — Native UI Redesign and Migration 🚧 (screen-family migration substantially implemented across Cashier, Operations, Owner, Manager, and Administrator; audit remediation and Owner More modularization complete; remaining depth, release cutover, bridge retirement, and full native regression pending)

Phase 15 — Final Shopkeeper QA ⏳ (starts after the native migration/regression gate; includes uncoached Quick unlock enrollment/use/recovery and periodic backup/recovery)

Phase 16 — Release Readiness ⏳ (secure Quick unlock and its recovery boundary are release blockers alongside cloud-provider authorization, privacy, quota, and recovery readiness)

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
- Bridge contract: version 5, 91 endpoints, generated Kotlin/TypeScript projections and both pre-build drift gates pass; v5 retains v4 and adds five native-owned Quick unlock operations.
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
- Phase 13.4 cleanup guard passes from its self-test, npm, post-package, and Android `preBuild` entry points; the broader focused isolated-QA Samsung suite passes 31/31 with 0 skipped and 0 failed.
- Phase 13.5 final cleanup guard, bridge, web/package parity, Android build, Kotlin compilation, immutable-hash, and 7/7 host-test gates pass.
- Phase 13.5 complete isolated-QA device regression: 258/258 passed across the 257-test authenticated and 1-test empty-identity visual partitions, 0 skipped, 0 failed.
- Phase 13.5 visual evidence: 83/83 readable PNGs at 720 × 1600 across 58 retained states in `phase13-evidence/sm-a065f-android16-phase13-5-final/`; manifest SHA-256 `8E9937FC972B2EE36D8E31D5FB58C0C7C86189D4C8A8F116AFA91C727B45F1BE`.
- Phase 14.1 isolated-QA build/install and clean first-run Samsung baseline pass; APK SHA-256 remains `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C`.
- Phase 14.1 confirms two connected Samsung phones and no configured Google OAuth web client ID. The primary phone holds the isolated QA app; the candidate recovery phone already holds the ordinary Saaf package and was deliberately left untouched pending safe-use confirmation.
- Phase 14.2 adds two role/permission tests: the all-five-role packaged UI and native-bypass matrix passes 2/2; the related security/session/backup/lifecycle/recovery regression passes 36/36; host tests pass 7/7. The complete device-suite inventory is now 260 tests; Phase 14.6 adds Quick unlock coverage before the expanded full rerun in Phase 14.7.
- Phase 14.3 passes 25/25 automated cloud tests and the real provider passes consent, encrypted upload, hidden listing, offline queue/retry, successful no-restore inspection, revocation, retained remote points, and fresh-consent reconnect. Six verified recovery points are visible; three live UX defects are recorded for Phase 14.7.
- Phase 14.4 passes real S24-to-A06 recovery of a recognizable product/supplier/customer/purchase/sale/correction dataset with matching stock and balances, correction lineage, actor identity, forced logout, retained remote points, and target cloud pause. Wrong secrets preserve the installation; two UX defects are recorded, including a P1 correction form that hides original values.
- Phase 14.5 completes the targeted A06/S24 Android audit at 11/20. Five P1 issues cover large-text reflow, keyboard obstruction, unsafe Back/draft loss, TalkBack dialog closing, and rotation draft loss; one P2 covers landscape refinement. TalkBack announcements, switch/lock lifecycle, S24 split-screen, light-theme stability, and document-picker cancellation pass.
- Phase 14.6 implements device-bound Android phone-security/PIN Quick unlock, Keystore protection and invalidation, PIN delay/full-passphrase recovery, shared-phone identity selection, routine backup reauthentication, trusted-device cloud-key inspection, and the v5/91 bridge. Nine focused tests pass 9/9 after closing two native security races. Physical S24 checks cover both fast sign-in methods, delays, restart, removal/re-enrollment, shared accounts, cross-account rejection, cloud inspection, and TalkBack.
- Phase 14.7 closes every recorded Phase 14 finding. A clean A06 offered no S24 Quick unlock method, required the portable recovery passphrase, and restored the S24 7:05 point. The same phone then exposed and verified the fix for automatic Phone security selection and false fingerprint wording. PIN-only and Phone-security-only setup/login pass separately. The final regression passes 273/273 ordinary Android tests, 1/1 visual audit, and 7/7 host tests.

## Next gate

The owner accepted the [refined original Counter Ledger preview](design/refined-counter-ledger/index.html), including the final compact payment selector, quantity-unit alignment, and `Amount` entry label. Neither generated treatment is selected wholesale. The [comparison](design/stitch/comparison.html), [review](design/stitch/REVIEW.md), [private Stitch canvas](https://stitch.withgoogle.com/projects/17568665013202933334), and `design/stitch/generation/` remain evidence.

Phase 14.8.3 now includes the Compose foundation, real native authentication, and an authoritative native Sale vertical slice. Welcome enters Room-backed account discovery or first-Owner setup; enrolled identities get account-specific Quick PIN/phone-security/full-passphrase choices; native security performs Owner creation and login; the one-time recovery code must be acknowledged; and leaving Cashier invalidates the memory-only native session. The authentication checkpoint passes 28/28 tests on the wired A06. The hardened Sale checkpoint passes 28/28 across current native screen workflows, hardening contracts, and authoritative Sale service regressions, with the separate load-retry contract also passing 1/1. New sale reads the signed-in identity, current date and day status, stored products/rates/stock/customers/payment accounts, writes the real permission-checked and actor-attributed Room transaction, refreshes after success, preserves rejected drafts, protects edited drafts on Back and activity recreation, revalidates the session on foreground return, and explains closed-day or missing-reference blocks. Large text reflows the dense rows, the IME Done action clears focus, long customer lists scroll lazily, dynamic status has a TalkBack live region, and system dark theme plus landscape remain usable. The real Android phone-security prompt also passes native approval and cancellation with the existing Owner enrollment. The hybrid launcher remains the fallback, and ordinary Saaf installations/profiles remain untouched. See `NATIVE_INTERACTION_CONTRACT.md`, `PHASE14_8_3_COMPOSE_FOUNDATION.md`, `PHASE14_8.md`, the four `PHASE14_8_2_COUNTER_LEDGER_*` documents, and `ROADMAP_RECONCILIATION_2026-08-27.md`.

Current update (2026-09-23): native screen-family migration now covers Cashier, Operations, Owner, Manager M0–M3, and Administrator M0–A2 at their recorded permission scope. Corrections, Owner planning, Shop opening, credential reset, backup/cloud restore, adaptive navigation, audit remediation, and Owner More feature modularization are connected and verified through targeted gates. The `.preview` APK launches native Compose; release deliberately remains on the hybrid launcher until remaining product depth and endpoint ownership close. Opening cost basis is the next unblocked implementation slice, followed by remaining Records groups, Cashier Deliveries-from-More, custom report dates, and production-loss assessment. Approval queue work still requires a domain API.
