# Saaf Milk Hybrid Rebuild — Roadmap Reconciliation

**Status: ACTIVE — approved for adoption on 2026-08-27; implementation ledger updated 2026-09-01**

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

## Approved periodic cloud-backup direction — details deferred 2026-08-31

Application users, cloud backup, and live multi-device access are separate capabilities:

- the implemented schema-7 user system supports multiple named owner/admin/manager/operator/cashier identities on one installation, with native permissions and durable actor audit;
- the current product remains offline-first and device-local: each installation owns its own Room database, there is no Internet permission or synchronization service, and encrypted backup/restore is migration and recovery rather than live sharing;
- the approved next direction is periodic encrypted off-device backup, with Google Drive as the example provider; cadence, network/charging conditions, retention, account ownership, and recovery UX will be decided at its design gate;
- the shop device must continue accepting authorized work without Internet, queue a missed backup safely, and retry without creating ambiguous or corrupt copies;
- the encrypted backup must preserve clerk identities and actor attribution so a recovered or explicitly viewed snapshot still shows who recorded each operation;
- periodic backup alone does not provide a live shared database. If owner-phone visibility is still desired, the later design must explicitly choose between a safe read-only view of the latest verified snapshot and true synchronization;
- restoring successive backups into two independently writable installations is not synchronization and must not be presented as such;
- React must not become a second accounting engine.

Before implementation, Phase 10 must produce and obtain approval for a concrete periodic-cloud-backup design covering provider/account authorization, Android background scheduling, encryption before upload, backup naming/versioning, cadence, Wi-Fi/mobile-data and charging policy, retention, missed-run retry, integrity verification, owner notification, restore selection, privacy, quota/cost, and sign-out/revocation behavior. The same gate may evaluate an optional read-only owner-phone snapshot viewer, but live multi-device writing remains a separate future decision. The current local credential, bridge, and backup contracts remain authoritative until that design is approved.

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
| 6 — Bridge Contract | **Complete; additive v2 correction recorded** | Preserve contract version 2, its manifest hash, lossless/authenticated envelopes, generated TypeScript/Kotlin projections, and mandatory pre-build drift checker. V2 adds only role-filtered active expense categories to transaction references. |
| 7 — Hybrid Vertical Slice | **Complete** | Preserve the separately built packaged assets, restricted app-assets origin, contract handshake, memory-only session flow, protected product command/audit, and 3-test physical-device proof. |
| 8 — Screen Wiring | **Complete** | All retained screen families and all 78 bridge operations are native-backed and device-proven, including encrypted export, inspection, and full-replacement restore through Android-owned picker/ticket boundaries. Preserve awaited native queries/commands, private credentials/passphrases, and the backup-versus-sync distinction. |
| 9 — Runtime Integration | **Complete** | Grouped navigation, isolated `.qa` storage, Android Back, useful fragmentation, secure recreation, keyboard/insets, measured targeted refresh, and renderer/error recovery are device-proven. Preserve fresh-WebView recovery and native session revalidation before protected content. |
| 10 — Functional Completion | **Not started; periodic cloud-backup design gate added** | Close the local capability matrix with every retained capability genuinely native-backed and wired. Replace broad post-mutation reloads with contract-declared, screen-scoped refresh and caching; add transport batching only if measurement justifies it. Produce and approve the periodic encrypted Google Drive/cloud-backup design, implement the approved minimum, and separately decide whether the owner phone needs only a read-only latest-snapshot view or any later synchronization. |
| 11 — Integrity/Recovery/Concurrency | **Substantial security/backup early credit; still open** | Complete concurrent-write, backup-during-write, large-history, repeated recovery, and remaining session/concurrency tests. For periodic cloud backup, prove scheduling, missed-run retry, upload idempotency, encryption/integrity, retention, provider sign-out/revocation, wrong-account behavior, restore selection, actor preservation, and honest backup-versus-sync boundaries. Reassess the deliberately deferred lower-of-cost/NRV subsystem against the approved materiality and business triggers before hardening closes. |
| 12 — UI Fidelity | **Not started for the hybrid product** | Compare the packaged physical-device React UI with the approved visual target without changing business semantics, and verify that accounting precision is preserved without requiring accounting literacy. |
| 13 — Dead/Mock/Misleading Cleanup | **Not started** | Remove localStorage business state, hardcoded business data, UI-side accounting, synthetic reports, dead controls, mock users, and misleading success paths. |
| 14 — Final Device QA | **Native instrumentation credit only; hybrid acceptance not started** | Run the full hybrid device and role/permission bypass matrix. If cloud backup ships, test real periodic upload, offline/missed scheduling, retry, provider sign-out/revocation, retention, corrupt/wrong-passphrase rejection, and restore on a second device. Test owner-phone viewing separately if that optional scope is approved. |
| 15 — Shopkeeper QA | **Not started** | Have non-accountant shop users simulate a complete business day, mistakes/corrections, periodic cloud backup, restore, account administration, and independently checked totals without coaching in accounting terminology. If snapshot viewing is approved, include a clerk entering work and the owner confirming the latest available snapshot from the owner's phone. |
| 16 — Release Readiness | **Not started** | Produce a reproducible release build, migration/security review, final report, archive, and release package. Any cloud-backup release also requires documented provider authorization, encrypted-secret handling, privacy/retention rules, quota/cost behavior, recovery procedure, and safe offline/degraded operation. |

## Current verification credit

The Phase 7 checkpoint passed **127/127** connected-device tests. Phase 8 brought the complete suite to **211/211**. Phase 9's navigation, lifecycle, keyboard/inset, targeted-refresh, and renderer-recovery coverage brings the current complete isolated-QA suite to **218/218**, with zero skipped, failed, or errored tests; the host-side debug unit suite passes **7/7**. Phase 8 proves native Daily Work, delivery lifecycle, customer/supplier ledgers, History/Corrections, Cash Count/Daily Close, Dashboard/Reports, Costing/Planning, Staff/Security, and Encrypted Backup/Restore. Phase 9 proves the single-WebView navigation path, secure recreation, deliberate phone-screen behavior, reduced bridge transfer, and secure recovery after real renderer termination. Contract version 2 still defines the same 78 operations and retains its documented additive expense-reference field.

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
8. Complete Phase 10 local capability closure, then pass the explicit periodic encrypted cloud-backup approval gate; decide owner-phone snapshot viewing separately from live synchronization.
9. Apply the early Phase 11 credit, finish local and cloud-backup integrity/recovery gates, and perform the deferred NRV trigger review before hardening closes.
10. Complete hybrid UI fidelity and mock/dead-state cleanup in Phases 12–13.
11. Run device, shopkeeper, and release gates in Phases 14–16, including real periodic-cloud-backup and second-device restore evidence plus owner-phone snapshot viewing if approved.

## Immediate next gate

Phases 3–9 are complete and all 78 contract operations are implemented. Phase 9 closed after real renderer-termination recovery tests passed 2/2 and the full isolated-QA Samsung regression passed 218/218; host tests pass 7/7. Next begin Phase 10's functional-product-completion review and the separate periodic encrypted Google Drive/cloud-backup design gate, including an explicit decision on owner-phone read-only snapshot viewing versus live synchronization.
