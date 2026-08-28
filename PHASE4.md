# Phase 4 — Native Automated Test Closure

**Status: COMPLETE — 2026-08-28**

## 1. Purpose and boundary

This is root hybrid-rebuild Phase 4, not the inherited native application's historical Phase 4 UI checkpoint.

The locked requirement in `POSTMORTEM.md` is to prove every native capability independently before hybrid UI wiring, including happy paths, validation/failure, persistence, correction and ledger effects, backup/restore, restart, security bypass, and applicable concurrency behavior.

Phase 4 audits and closes the native executable-evidence gate. It does not claim that a bridge, WebView, React screen, or hybrid integration test exists.

## 2. Audit result

The pre-existing 111 connected-device tests already provided broad native evidence. The audit found no missing business formula or schema requirement. It found concentrated security evidence gaps:

- only sample permissions, rather than the complete role matrix, were asserted;
- logout, background timeout, overall timeout, process restart, and multi-session revocation were not all directly tested;
- progressive password lockout and successful-login reset were not directly tested;
- owner-recovery throttling and independence from password lockout were not directly tested;
- recent-passphrase confirmation and representative direct-service bypass attempts were not directly tested;
- login-name normalization, duplicate rejection, Unicode passphrase normalization, blocklist/length policy, and generic unavailable-account errors needed explicit coverage;
- future credential-work-factor upgrade behavior was specified but not implemented;
- Android managed-backup exclusions were configured but lacked an executable assertion.

Nine tests in `Phase4SecurityCoverageTest.kt` close these gaps. The complete connected-device suite is now 120 tests.

## 3. Security improvements made because of the audit

`Security.kt` now:

1. runs unavailable, disabled, setup-required, and malformed-login attempts through a prebuilt Argon2id comparison credential before returning the same generic authentication failure;
2. recognizes an older still-valid Argon2id verifier and replaces it with the configured stronger work factors after a successful login;
3. preserves the approved minimum database constraints—tests cannot store a verifier below the minimum;
4. continues to clear failed-attempt/lockout state only after successful full login.

No schema migration was required.

## 4. Capability evidence matrix

| Native capability group | Primary executable evidence | Phase 4 dimensions covered |
|---|---|---|
| Canonical purchase, production, sale, collections, payments, expenses, salary | `Phase1RoomTest`, `Phase2TransactionTest`, transaction UI tests | Happy path, calculation, validation, atomic rollback, account/inventory effects, UI-to-domain save behavior |
| Corrections and effective truth | `Phase3CorrectionTest`, `Phase5cCorrectionUiTest` | Every supported correction type, delta effects, chained corrections, wrong type, negative-stock rollback |
| Customer/vendor account ledgers | `Phase3NativeExpansionTest` | Canonical movement reuse, correction relationships, running balances, invalid account-category rejection |
| Products and customer/vendor/employee profiles | `Phase3NativeCapabilitiesTest`, management UI tests | Canonical identity, archive/inactive behavior, validation, history preservation |
| Attendance and delivery | `Phase3NativeCapabilitiesTest`, `Phase3DailyCloseTest` | Append-only revision, sale linkage without duplicate revenue, lifecycle, closed-day rejection |
| Spoilage | `Phase3NativeCapabilitiesTest`, `Phase3CostingTest` | Inventory event/delta correction, negative-stock protection, cost valuation/report effects |
| Physical cash count and dynamic expense categories | `Phase3NativeCapabilitiesTest`, expense UI tests | Append-only counts, canonical variance, account-backed category, archive rejection, ledger effects |
| Store identity and opening state | `Phase3OpeningStateTest`, `Phase3MigrationTest`, `Phase9BackupRestoreTest` | Editable identity, PKR constraint, one-time opening, corrections, pre-existing-business rejection, no fabricated migration rows, backup round trip |
| Daily close/reopen/reclose | `Phase3DailyCloseTest`, `Phase3UserSecurityTest`, `Phase9BackupRestoreTest` | Cash-count prerequisite, lock/reopen/reclose, role/session actor, variance explanation, correction flow, attribution, backup history |
| Users, credentials, sessions, roles and audit | `Phase3UserSecurityTest`, `Phase3SecurityShellUiTest`, `Phase4SecurityCoverageTest` | Bootstrap, enrollment/reset, normalization, password policy, Argon2id storage/upgrade, full role matrix, generic failures, progressive lockout, recovery, inactivity/overall/background/logout/restart, recent authentication, concurrent-session revocation, direct bypass, audit snapshots, authenticated shell |
| Secure backup/restore | `Phase9BackupRestoreTest`, `Phase4SecurityCoverageTest` | Portable/local confidentiality, wrong passphrase, tamper rejection, legacy import, source-state round trip, session invalidation, transient lockout exclusion, Android managed-backup exclusions |
| Costing and profitability | `Phase3CostingTest`, `Phase3MigrationTest`, `Phase9BackupRestoreTest`, `Phase4SecurityCoverageTest` | Moving average, conversion policy, opening basis, incomplete coverage, correction replay, yield alert, abnormal loss, material-change validation, immutable source/migration, backup, permission and recent-auth bypass |
| Room migration/persistence boundary | `Phase3MigrationTest`, `Phase9BackupRestoreTest`, application-database UI tests | Schema 3→8 validation, preservation/no fabricated truth, separate-database restore verification, application database reads after activity lifecycle |

## 5. Locked Phase 4 criterion disposition

| Criterion | Disposition |
|---|---|
| Happy path | Covered for every delivered native capability group. |
| Validation and failure | Covered through domain, database-trigger, security, and UI-boundary rejection tests. |
| Persistence and restart | Durable data is covered by Room migration, application-database, and separate-database backup/restore tests. Memory-only sessions are explicitly rejected by a newly constructed session owner, representing process restart. |
| Correction | Covered for all correctable financial/inventory events and append-only Phase 3 source records where applicable. |
| Account/inventory effects | Covered by canonical read-model assertions and rollback tests. |
| Backup/restore | Covered for legacy, encrypted portable, and device-local formats, including costing and security source truth. |
| Concurrency where applicable | Concurrent active sessions are revoked together. Transactional atomicity and rollback are covered. Parallel database-write load/race testing remains the explicitly locked Phase 11 hardening gate. |
| Credential/security checklist | Covered by the Phase 3 security tests, Phase 4 additions, migration tests, backup tests, and Android exclusion assertion. |

## 6. Verification

Connected device: `SM-A065F`

- Kotlin and Android-test Kotlin compilation — PASS;
- new Phase 4 security coverage — 9/9 PASS;
- host-side debug unit tests — 7/7 PASS;
- complete connected-device instrumentation regression — 120/120 PASS, 0 skipped, 0 failed;
- frozen Phase 18 logic specification hash — unchanged;
- frozen Phase 18 SQL blueprint hash — unchanged.

## 7. Deliberately later work

Phase 4 does not pull later-phase work forward:

- Phase 5 must define and freeze the hybrid-facing DTO/read-model catalog;
- Phase 6 must define the canonical bridge contract and drift checker;
- hybrid bridge/UI bypass testing begins only after a bridge exists;
- Phase 11 retains parallel-write, backup-during-write, large-history, repeated-recovery, and deeper device-clock/reboot resistance hardening;
- Phases 14–16 retain the final physical-device role matrix, shopkeeper acceptance, and release gates.

The optional financial info-button/explainer layer remains deferred. Later UI must still translate native reason/action codes into plain shop language.

## Plain English

Phase 4 proves that the app's existing business and security rules are not just written down—they are exercised automatically. Sales, stock, corrections, backups, costing, user roles, account disabling, password lockouts, recovery, logout, and owner-only actions now have direct checks. If a future change breaks one of these protections, the test suite should catch it before the app reaches the shop.
