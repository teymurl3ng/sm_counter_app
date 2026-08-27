# Saaf Milk — Real Hybrid Rebuild

Status: RECONCILED 2026-08-27 — hybrid Phase 3 remains open only for authoritative costing/COGS; substantial Phase 4/7/9/11 native prerequisites were completed early and are credited without declaring those hybrid phases complete.

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
- `PHASE3_COGS_PROFITABILITY_DECISION_PROPOSAL.md` — proposed standards-based inventory-costing, spoilage-valuation, gross-profit, and honest operating-result contract; awaiting approval.

## Immutable baselines

Logic specification SHA-256:
`45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea`

SQL blueprint SHA-256:
`fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51`

## Current phase

**Phase 3 — Native Phase 18 Capability Expansion: IN PROGRESS**

The approved Tier 1 native expansion now includes product metadata/archive state, customer/vendor/employee profiles, attendance, deliveries, spoilage, physical cash counts, dynamic expense categories, account-specific ledgers, editable store identity, one-time auditable opening state, controlled daily close/reopen/reclose history, and schema-version-7 native security. The native application now owns bootstrap/login/logout/recovery sessions, user administration, role-aware navigation, and permission checks at protected application-command boundaries. React/bridge wiring has not started.

## Approved security track

- Application users are security principals and are distinct from employee/salary profiles. A user may optionally link to one employee, but an employee record does not grant application access.
- Authentication and authorization must be native-backed. React may present login and permission-aware controls, but it must never decide or self-assert a user's role.
- The approved model defines native users, Argon2id credentials, fixed role/permission bundles, memory-only sessions, first-owner bootstrap, deactivation, owner recovery, audit actors, and migration behavior.
- Once any application user exists, daily close/reopen rejects typed actor claims and requires a verified native session/user ID. Schema-6 history remains explicitly legacy/unverified.
- Portable encrypted backup format 2 is implemented with Argon2id-derived AES-256-GCM protection, security-state portability, tamper/wrong-passphrase rejection, legacy format-1 import compatibility, and post-restore session invalidation. The authenticated native shell and user-administration UI are implemented; React/bridge integration remains later work. The numeric quick-PIN shortcut remains deliberately deferred.
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

Phase 3 — Native Phase 18 Capability Expansion 🔧 (only authoritative COGS/profitability remains)

Phase 4 — Native Automated Tests ◐ (substantial early credit; formal coverage closure follows COGS)

Phase 5 — Native Read-Model Expansion ◐ (native foundation exists; hybrid DTO contract is not frozen)

Phase 6 — Bridge Contract (`bridge/CONTRACT.md`) ⏳

Phase 7 — Hybrid Shell / Vertical Slice ⏳ (native authentication prerequisite complete; actual hybrid slice absent)

Phase 8 — UI Wiring ⏳

Phase 9 — Runtime / Navigation / Device Integration ◐ (native lifecycle credit only; hybrid runtime absent)

Phase 10 — Functional Product Completion ⏳

Phase 11 — Integrity / Recovery / Concurrency ◐ (security/backup credit; concurrency and hybrid recovery remain)

Phase 12 — UI Fidelity ⏳

Phase 13 — Dead/Mock/Misleading UI Cleanup ⏳

Phase 14 — Final Device QA ⏳

Phase 15 — Final Shopkeeper QA ⏳

Phase 16 — Release Readiness ⏳

## Rule for phase records

Every completed or active hybrid-rebuild phase gets a root `PHASEN.md` file with its status, scope, changes, verification, and relevant hashes. Do not overwrite prior phase records; corrections create an explicitly named patch note in the current phase while the phase record records the final state.

## Latest Phase 3 verification

- Frozen logic-spec and SQL hashes still match the Phase 0 values.
- Room schema version 7 and migrations through `MIGRATION_6_7` are implemented.
- Kotlin and Android-test Kotlin compilation pass.
- Room schema 3→4, 4→5, 5→6, and 6→7 migration validation passes on the connected device; migration 6→7 creates no artificial user, credential, session, or attribution.
- Phase 3 ledger/capability tests pass.
- Legacy format-1 business backup/restore round trips still pass, including chronological replay of daily close/reopen/reclose history.
- Encrypted format-2 export/restore preserves user identities, roles/status, Argon2id credential verifiers, recovery verifiers, security audit, event attribution, and verified daily-close actor IDs; active sessions are never serialized and all supplied in-memory sessions are invalidated after restore.
- Native first-owner setup, login, logout, owner recovery, passphrase change, private staff enrollment/reset, user/role/status administration, lifecycle session handling, role-aware navigation, and protected-command authorization are implemented.
- Focused user-security plus authenticated-shell suite: 8/8 passed.
- Focused backup/restore suite: 8/8 passed, including portable and local confidentiality checks, wrong-passphrase rejection, tamper rejection, security round-trips, session invalidation, and transient lockout-state exclusion.
- Complete connected-device instrumentation regression: 103/103 passed, 0 skipped, 0 failed.

## Next Phase 3 decision

The sole remaining Phase 3 product decision is authoritative costing and COGS/profitability semantics. A researched recommendation is now proposed in `PHASE3_COGS_PROFITABILITY_DECISION_PROPOSAL.md` and awaits explicit approval before schema or code changes. The bridge and React/hybrid UI remain later delivery phases, not unfinished Phase 3 work. See `ROADMAP_RECONCILIATION_2026-08-27.md` for the exact credited and remaining gates.
