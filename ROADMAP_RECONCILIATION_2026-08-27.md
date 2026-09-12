# Saaf Milk Hybrid Rebuild — Roadmap Reconciliation

**Status: ACTIVE — approved for adoption on 2026-08-27; native UI insertion recorded 2026-09-12**

## Purpose

This record reconciles the locked hybrid-rebuild roadmap with work that was validly completed earlier than its original phase assignment. It does not delete, renumber, or rewrite the roadmap in `POSTMORTEM.md`, and it does not change the frozen business specification or SQL blueprint.

The original phase order remains the delivery-control structure. This document adds an honest status ledger so completed work receives credit without falsely declaring later hybrid phases complete.

## Numbering boundary

Two independent histories exist in this repository:

1. `phase18/PHASE*.md` records the development history of the inherited native Kotlin/Room application.
2. Root `PHASE0.md` through the future hybrid phase records describe the new real-hybrid rebuild.

The phrase “Phase 3” in this document means root hybrid Phase 3, not the inherited application's historical Phase 3.

## Discovered sequencing issue

Authenticated application users were approved as a cross-cutting prerequisite after the initial hybrid roadmap was written. A safe bridge could not be designed while identity, credentials, permissions, sessions, recovery, actor attribution, migration, and secure backup behavior were undefined.

During root Phase 3, implementation therefore continued beyond the minimum schema foundation and completed several native prerequisites originally distributed across Phases 4, 7, 9, and 11:

- extensive native security and backup tests;
- first-owner/login/logout/recovery and user-administration UI;
- app-owned session lifecycle handling and role-aware native navigation;
- protected native application-command checks;
- encrypted portable backups and Android-Keystore local snapshots;
- tamper rejection, restore session invalidation, and transient lockout-state exclusion.

This work remains valid and reusable. It does not constitute completion of the corresponding hybrid phases because no canonical bridge, packaged React/WebView slice, or hybrid integration test exists yet.

## Protected behavior

The reconciliation preserves these non-negotiable contracts:

- the Phase 18 logic specification and SQL blueprint remain byte-for-byte frozen;
- Kotlin/Room remains the authority for accounting, inventory, identity, and authorization;
- React must not calculate or persist business truth;
- bridge callers must not assert roles or actor identities;
- no migrated installation receives an artificial opening event, owner, credential, session, or privileged audit record;
- no phase is considered complete merely because part of its work was delivered early.

## Approved periodic cloud-backup direction — implemented 2026-09-04

Application users, cloud backup, and live multi-device access are separate capabilities:

- the implemented schema-7 user system supports multiple named owner/admin/manager/operator/cashier identities on one installation, with native permissions and durable actor audit;
- the product remains offline-first: each writable installation owns its Room database, and encrypted backup/restore is migration and recovery rather than live sharing;
- periodic encrypted off-device backup is implemented through Google Drive `appDataFolder`, with the approved cadence, network/charging constraints, retention, account ownership, and recovery UX;
- the shop device must continue accepting authorized work without Internet, queue a missed backup safely, and retry without creating ambiguous or corrupt copies;
- the encrypted backup must preserve clerk identities and actor attribution so a recovered or explicitly viewed snapshot still shows who recorded each operation;
- periodic backup alone does not provide a live shared database. If owner-phone visibility is still desired, the later design must explicitly choose between a safe read-only view of the latest verified snapshot and true synchronization;
- restoring successive backups into two independently writable installations is not synchronization and must not be presented as such;
- React must not become a second accounting engine.

Phase 10 produced, obtained approval for, and implemented the design in `PHASE10_CLOUD_BACKUP_DESIGN_PROPOSAL.md`. Contract v4 and `PHASE10_4_IMPLEMENTATION_REPORT.md` record the native boundary and evidence. The optional read-only owner-phone snapshot viewer remains deferred, and live multi-device writing remains a separate unapproved future decision.

## Implemented product foundation

### Inherited native foundation

The native application already provides purchases, production, sales, collections, vendor payments, expenses, salary payments, corrections, canonical inventory/account reads, transaction history/detail, analytics, budgets/targets, settings, management screens, native XML presentation, and the original backup workflow.

### Root Phase 3 native expansion

Schema version 4 adds product metadata/archive state, customer/vendor/employee profiles, attendance, delivery lifecycle, spoilage events and corrections, physical cash counts, dynamic expense categories, and account-specific ledgers.

Schema version 5 adds editable store identity, database-constrained PKR currency, and optional one-time auditable opening state with restated-truth corrections. Upgrades create no artificial opening entries.

Schema version 6 adds append-only close/reopen/reclose history, physical-count requirements, variance explanation, and posting locks without creating a second ledger.

Schema version 7 adds users separate from employees, Argon2id credentials, fixed role/permission bundles, memory-only sessions, recovery, audit attribution, verified daily-close actors, migration safety, and secure backup boundaries. Upgrades create no artificial users.

Schema version 8 adds immutable costing-policy, opening-cost-basis, yield-policy, and production-loss source records plus rebuildable inventory-cost projections. It implements authoritative Milk/Yogurt historical-cost management reporting, moving weighted average, Yogurt conversion cost, spoilage/abnormal-loss valuation, correction replay, and explicit coverage disclosures. Upgrades create no artificial cost source or opening-cost truth.

The native authenticated shell now provides deliberate first-owner setup, login/logout/recovery, passphrase changes, recent reauthentication, private staff enrollment/reset, owner/admin user administration, role-aware navigation, protected-content lifecycle behavior, and permission checks at protected application-command boundaries.

Portable backup format 2 uses Argon2id-derived AES-256-GCM. Automatic local snapshots use AES-256-GCM under a non-exportable Android Keystore key. Security-bearing backups exclude active sessions and transient failed-login/lockout timing state.

## Reconciled hybrid phase ledger

| Hybrid phase | Reconciled status | Completion gate still required |
|---|---|---|
| 0 — Freeze & Baseline | **Complete** | Continue verifying the two frozen hashes at every material checkpoint. |
| 1 — Capability Inventory | **Complete** | Preserve the inventory as the UI/native comparison baseline. |
| 2 — Product Decisions | **Complete** | New business semantics still require explicit amendments. |
| 3 — Native Expansion | **Complete** | Preserve the approved schema-4 through schema-8 native contracts; no React/bridge completion is implied. |
| 4 — Native Automated Tests | **Complete** | Preserve the `PHASE4.md` evidence matrix and keep the complete regression green as later native/bridge capabilities are added. |
| 5 — Native Read Models | **Complete** | Preserve `PHASE5_DTO_CATALOG.md`: integer-PKR-paisa money, canonical decimal quantities, explicit correction/coverage state, permission-aware reads, stable costing codes, and no credential/session-handle material. |
| 6 — Bridge Contract | **Complete; additive v2–v4 amendments recorded** | Preserve contract version 4, its manifest hash, lossless/authenticated envelopes, generated TypeScript/Kotlin projections, and mandatory pre-build drift checker. V4 retains v3 and adds eight owner-only native cloud-backup operations, for 86 total. |
| 7 — Hybrid Vertical Slice | **Complete** | Preserve the separately built packaged assets, restricted app-assets origin, contract handshake, memory-only session flow, protected product command/audit, and 3-test physical-device proof. |
| 8 — Screen Wiring | **Complete** | All retained screen families and all 78 bridge operations are native-backed and device-proven, including encrypted export, inspection, and full-replacement restore through Android-owned picker/ticket boundaries. Preserve awaited native queries/commands, private credentials/passphrases, and the backup-versus-sync distinction. |
| 9 — Runtime Integration | **Complete** | Grouped navigation, isolated `.qa` storage, Android Back, useful fragmentation, secure recreation, keyboard/insets, measured targeted refresh, and renderer/error recovery are device-proven. Preserve fresh-WebView recovery and native session revalidation before protected content. |
| 10 — Functional Completion | **Complete — checkpoints 10.1–10.5** | All 16 retained workflow families remain native-backed. Contract v4 exposes 86 operations and implements the approved Drive `appDataFolder`, envelope encryption, coherent snapshot, idempotent upload, WorkManager, retention, status, and full-replacement restore boundary. The complete isolated suite passes 224/224. |
| 11 — Integrity/Recovery/Concurrency | **Complete — checkpoints 11.1–11.6** | Cloud outbox/process-death, overlapping-run, scheduling/provider failures, destructive replacement restore, and coherent backup around writes/corrections are hardened. A 5,000-event/10,000-financial-row history meets measured bounds. Formal NRV remains conditionally deferred with honest `cost_only_no_nrv` disclosure. Final bridge, UI/package, host, focused, full-device, hash, limitations, and Phase 12 handoff gates pass. |
| 12 — UI Fidelity | **Complete — checkpoints 12.1–12.6** | The Samsung baseline, proportional presentation system, navigation/task flow, 83-screen responsive/accessibility pass, profileable release-like performance gate, final rebuild, and 258/258 device regression are complete without changing business semantics. Cold-process start is 822 ms median / 871 ms p95, React ready is 327.9 ms median / 378.3 ms p95, and painted preloaded navigation is 24.0 ms median / 29.6 ms p95 with zero bridge calls. The final 83-screen manifest reproduces the accepted Phase 12.4 hash. |
| 13 — Dead/Mock/Misleading Cleanup | **Complete — checkpoints 13.1–13.5** | The fully unreachable 23-file, 8,370-line browser-local prototype and six unused direct dependencies are removed. All 85 active files and 120 literal buttons are audited; the misleading combined opening-balance total is gone. A mandatory cleanup gate protects source and packaged assets through npm/Vite and Android `preBuild`. Host tests pass 7/7; the final Samsung regression passes 258/258 across its authenticated and empty-identity partitions, with 83-screen evidence preserved for the Phase 14 handoff. |
| 14 — Final Device QA | **Complete — checkpoints 14.1–14.7** | Live Drive, data-rich S24-to-A06 restore, accessibility/Android behavior, and device-bound Quick unlock pass. All recorded Phase 14 defects are fixed, including replacement-phone vault fallback and misleading Quick unlock method selection. Shared-phone PIN isolation, A06 portable recovery/restore, and separate PIN-only and Phone-security-only setup/login pass physically. The final verification is 273/273 ordinary Android tests, 1/1 visual audit, and 7/7 host tests with bridge v5/91. Removing/changing phone security after enrollment and changing biometric enrollment were not performed; automated Keystore invalidation coverage is the accepted limitation. Ordinary Saaf installations/profiles remain untouched. |
| 14.8 — Native UI Redesign and Migration | **Active — checkpoint 14.8.1 complete** | Replace the unaccepted React/WebView release interface with a Compose presentation through an incremental, parity-gated migration. Preserve the Phase 14 hybrid build as the behavioral oracle and fallback, retain all Kotlin/Room/security/backup authority, and retire the bridge only after every retained capability has a native owner and the full native regression passes. |
| 15 — Shopkeeper QA | **Not started** | Have non-accountant shop users simulate a complete business day, mistakes/corrections, periodic cloud backup, restore, account administration, and independently checked totals without coaching in accounting terminology. Prove they can enroll, use, cancel, recover, and understand biometric/device-credential or short-PIN Quick unlock without confusing it with portable backup recovery. If snapshot viewing is approved, include a clerk entering work and the owner confirming the latest available snapshot from the owner's phone. |
| 16 — Release Readiness | **Not started** | Produce a reproducible release build, migration/security review, final report, archive, and release package. Establish a profileable benchmark baseline, then measure R8/resource shrinking and Baseline/Startup Profiles rather than assuming benefit; verify Room, bridge serialization, cryptography, backup/restore, permissions, Quick unlock, and physical-device startup after each release optimization. Release is blocked until Quick unlock's Keystore protection, invalidation, lockout, shared-device attribution, and portable-recovery boundary pass. Any cloud-backup release also requires documented provider authorization, encrypted-secret handling, privacy/retention rules, quota/cost behavior, recovery procedure, and safe offline/degraded operation. |

## Current verification credit

The Phase 7 checkpoint passed **127/127** connected-device tests. Phase 8 brought the suite to **211/211**, Phase 9 to **218/218**, Phase 10.2 to **219/219**, Phase 10.4 to **224/224**, and Phase 11 to **247/247**. Phase 12 added eleven presentation, navigation, responsive/accessibility, performance, and visual-audit tests. Phase 13.5 reran the complete isolated-QA device regression at **258/258** across the authenticated 257-test and empty-identity 1-test partitions, with zero skipped, failed, or errored tests. Phase 14 closes at **273/273** ordinary Android tests plus a separately configured **1/1** visual audit and **7/7** host tests. Contract version 5 exposes 91 operations and passes both generated-projection drift gates.

The two immutable hashes remain:

- logic specification: `45E5BDDABE8861A1B3DF7C5666AC7673078439A89FA5628061AD293A865F9DEA`;
- SQL blueprint: `FB701C90D2DD97A1E6D962154692C48EA0F4E77E9300F9DFAC653D4AB67BBC51`.

## Revised execution order

1. **Completed:** close Phase 3 by researching, approving, implementing, and documenting authoritative costing/profitability semantics.
2. **Completed:** formally close Phase 4 with a criterion-by-criterion native/security coverage audit and focused gap tests.
3. **Completed:** implement and freeze the Phase 5 hybrid-facing native read-model/DTO catalog.
4. **Completed:** create the Phase 6 canonical bridge contract and drift checker.
5. **Completed:** prove the Phase 7 authenticated physical-device hybrid vertical slice before scaling.
6. **Completed:** Phase 8 screen-by-screen wiring, including encrypted backup and restore; all 78 contract operations are implemented and device-proven.
7. **Completed:** Phase 9 runtime integration, including grouped navigation, isolated QA data, Android Back/overlay coordination, structural hardening, secure Activity/WebView recreation, keyboard/system-inset behavior, measured targeted refresh, and renderer/error recovery.
8. **Completed:** Phase 10 local capability closure, explicit encrypted cloud-backup approval gate, minimum implementation, and verification; owner-phone snapshot viewing remains separate from live synchronization.
9. **Completed:** Phase 11 integrity/recovery/concurrency hardening, NRV trigger reassessment, final regression, limitations ledger, and Phase 12 handoff.
10. **Completed:** Phase 12 UI fidelity, including physical baseline, proportional presentation system, navigation/task flow, 83-screen responsive/accessibility evidence, release-like performance, final regression, limitations ledger, and Phase 13 handoff.
11. **Completed:** Phase 13 inventory, prototype removal, active control/meaning audit, automatic cleanup guard, focused gate, complete 258-test regression, 83-screen evidence, and Phase 14 handoff.
12. **Completed:** Phase 14.1–14.7 established the safe device baseline, all-role permission matrix, live Drive behavior, data-rich cross-device restore, accessibility/Android audit, device-bound Quick unlock, defect closure, and the 273/273 ordinary plus 1/1 visual final regression. Physical checks include shared-account PIN isolation, clean-device portable recovery/restore, and explicit PIN-only and Phone-security-only setup/login.
13. **Active:** Phase 14.8 redesigns and migrates the presentation directly to native Compose. Checkpoint 14.8.1 freezes the hybrid baseline and maps all surfaces/native owners. Journey direction, Compose foundation, vertical slice, family migration, bridge retirement, and full native regression follow before Phase 15.

## Immediate next gate

Phases 3–14 are complete and Phase 14.8 is active. Begin checkpoint 14.8.2 journey evidence and native information architecture, then implement the Compose migration through the recorded parity gates. Phase 15 uncoached shopkeeper QA starts only after the full native regression. Never bypass an unavailable recovery secret or touch an ordinary Saaf profile. Phase 16 retains release acceptance, and `QUICK_UNLOCK_REQUIREMENT.md` remains a release-blocking contract.
