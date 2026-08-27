# Saaf Milk Hybrid Rebuild — Roadmap Reconciliation

**Status: ACTIVE — approved for adoption on 2026-08-27**

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

## Implemented product foundation

### Inherited native foundation

The native application already provides purchases, production, sales, collections, vendor payments, expenses, salary payments, corrections, canonical inventory/account reads, transaction history/detail, analytics, budgets/targets, settings, management screens, native XML presentation, and the original backup workflow.

### Root Phase 3 native expansion

Schema version 4 adds product metadata/archive state, customer/vendor/employee profiles, attendance, delivery lifecycle, spoilage events and corrections, physical cash counts, dynamic expense categories, and account-specific ledgers.

Schema version 5 adds editable store identity, database-constrained PKR currency, and optional one-time auditable opening state with restated-truth corrections. Upgrades create no artificial opening entries.

Schema version 6 adds append-only close/reopen/reclose history, physical-count requirements, variance explanation, and posting locks without creating a second ledger.

Schema version 7 adds users separate from employees, Argon2id credentials, fixed role/permission bundles, memory-only sessions, recovery, audit attribution, verified daily-close actors, migration safety, and secure backup boundaries. Upgrades create no artificial users.

The native authenticated shell now provides deliberate first-owner setup, login/logout/recovery, passphrase changes, recent reauthentication, private staff enrollment/reset, owner/admin user administration, role-aware navigation, protected-content lifecycle behavior, and permission checks at protected application-command boundaries.

Portable backup format 2 uses Argon2id-derived AES-256-GCM. Automatic local snapshots use AES-256-GCM under a non-exportable Android Keystore key. Security-bearing backups exclude active sessions and transient failed-login/lockout timing state.

## Reconciled hybrid phase ledger

| Hybrid phase | Reconciled status | Completion gate still required |
|---|---|---|
| 0 — Freeze & Baseline | **Complete** | Continue verifying the two frozen hashes at every material checkpoint. |
| 1 — Capability Inventory | **Complete** | Preserve the inventory as the UI/native comparison baseline. |
| 2 — Product Decisions | **Complete** | New business semantics still require explicit amendments. |
| 3 — Native Expansion | **Open: one domain track remains** | Approve and implement authoritative COGS, spoilage valuation, gross profit, net profit, and per-product profitability. |
| 4 — Native Automated Tests | **Substantial early credit; not formally closed** | Add tests for the approved costing model, then audit every Phase 4 criterion, including direct permission bypass, recent reauthentication, lockout, restart, and revocation coverage. |
| 5 — Native Read Models | **Partial foundation exists** | Define, review, and freeze the hybrid-facing DTO catalog, including safe security/session/permission views and no credential material. |
| 6 — Bridge Contract | **Not started** | Create `bridge/CONTRACT.md` and a mandatory TypeScript/Kotlin drift checker before bridge implementation. |
| 7 — Hybrid Vertical Slice | **Native authentication prerequisites completed early; hybrid slice not started** | Prove packaged React → WebView → bridge → Kotlin → Room → React on a physical device with one authenticated query and one protected mutation. |
| 8 — Screen Wiring | **Not started** | Replace React mock/local state screen-by-screen with awaited native queries and commands plus hybrid integration tests. |
| 9 — Runtime Integration | **Partial native credit; hybrid work not started** | Prove WebView navigation, lifecycle, process recreation, keyboard, errors, logout/back-stack protection, and session expiry. |
| 10 — Functional Completion | **Not started** | Close the capability matrix with every retained capability genuinely native-backed and wired. |
| 11 — Integrity/Recovery/Concurrency | **Substantial security/backup early credit; still open** | Complete concurrent-write, backup-during-write, large-history, repeated recovery, and remaining session/concurrency tests. |
| 12 — UI Fidelity | **Not started for the hybrid product** | Compare the packaged physical-device React UI with the approved visual target without changing business semantics. |
| 13 — Dead/Mock/Misleading Cleanup | **Not started** | Remove localStorage business state, hardcoded business data, UI-side accounting, synthetic reports, dead controls, mock users, and misleading success paths. |
| 14 — Final Device QA | **Native instrumentation credit only; hybrid acceptance not started** | Run the full hybrid device and role/permission bypass matrix. |
| 15 — Shopkeeper QA | **Not started** | Simulate a complete business day, mistakes/corrections, backup/restore, account administration, and independently checked totals. |
| 16 — Release Readiness | **Not started** | Produce a reproducible release build, migration/security review, final report, archive, and release package. |

## Current verification credit

The connected-device native regression suite passes **103/103**, with zero skipped, failed, or errored tests. This is valid evidence for completed native work and early Phase 4/11 credit. It is not evidence for bridge or hybrid UI behavior.

The two immutable hashes remain:

- logic specification: `45E5BDDABE8861A1B3DF7C5666AC7673078439A89FA5628061AD293A865F9DEA`;
- SQL blueprint: `FB701C90D2DD97A1E6D962154692C48EA0F4E77E9300F9DFAC653D4AB67BBC51`.

## Revised execution order

1. Close Phase 3 by researching, approving, implementing, and documenting authoritative costing/profitability semantics.
2. Formally close Phase 4 with costing tests and a criterion-by-criterion native/security coverage audit.
3. Complete and freeze the Phase 5 hybrid-facing native read-model/DTO catalog.
4. Create the Phase 6 canonical bridge contract and drift checker.
5. Prove the Phase 7 authenticated physical-device hybrid vertical slice before scaling.
6. Perform Phase 8 screen-by-screen wiring, replacing all mock/local business behavior.
7. Complete Phases 9–10 runtime integration and capability closure.
8. Apply the early Phase 11 credit, then finish its remaining concurrency/recovery gates.
9. Complete hybrid UI fidelity and mock/dead-state cleanup in Phases 12–13.
10. Run device, shopkeeper, and release gates in Phases 14–16.

## Immediate next gate

The COGS/profitability decision record is now proposed in `PHASE3_COGS_PROFITABILITY_DECISION_PROPOSAL.md`. It establishes recommended treatment for purchased milk, transformed yogurt, other products, opening inventory, spoilage, valuation, corrections, coverage gaps, and historical reporting. The existing React prototype's hardcoded `costRate` values and client-side profit calculations are not authoritative and must not be copied into Kotlin.

No costing implementation begins until the proposed semantics and migration implications are explicitly approved.
