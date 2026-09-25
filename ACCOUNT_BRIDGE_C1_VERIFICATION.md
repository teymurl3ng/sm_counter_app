# Account Bridge — C1 Implementation Verification

**Date:** 2026-09-24  
**Scope:** Read-only inspection of `SM Admin Server`, `saaf-milk-admin-viewer`, and the supplied C1 completion report.  
**Verdict:** **C1 is an implemented prototype, but it is not an accepted or reproducible release.**

This record distinguishes repository evidence from deployment claims. It does not modify either external project and does not claim access to the live Supabase or Vercel control planes.

## Repository state

| Project | Local branch state | Evidence-backed conclusion |
|---|---|---|
| Account Server | `main` at `94f7791`; `CONTRACT.md` modified; all three `migration_c1_*.sql` files untracked | C1 server work is not committed or present on `origin/main`. |
| Admin Viewer | `main` at `dd2708e`, matching `origin/main` | Most C1 UI/routes are pushed, but ignored source files make the pushed tree incomplete. |
| Live DEV/PROD | Not accessible from this read-only inspection | Migration application, environment variables, and the claimed production deployment remain unverified. |

## Completion-report reconciliation

| Report claim | Result | Repository evidence |
|---|---|---|
| All C1 code committed and pushed to `main` | **False** | Account-server migrations are untracked. Viewer support files are ignored and absent from Git. |
| Reset-credential route shipped | **False for the Git tree** | `src/app/api/admin/reset-credential/route.ts` exists locally but is ignored by `*credential*`. |
| Secure credential generator shipped | **False for the Git tree and broken locally** | `src/lib/credential.ts` is ignored. Its `randomInt(0, 10^15)` call exceeds Node's allowed range and throws `ERR_OUT_OF_RANGE`. |
| Concurrency-lock table added | **False** | No such table exists in the C1 migrations. |
| Last-owner race is serialized | **Not valid as written** | The helper applies `FOR UPDATE` to `SELECT count(...)`; PostgreSQL does not allow row locking on aggregate results, and the design does not establish a stable per-business lock. |
| C1 write contract tests added | **False** | `test_contract.py` has no Group 20+ implementation and no local C1 diff. |
| Strict role checks protect every route | **False** | The reset route accepts any stored Viewer session, then only checks that `my_profile` exists. It does not re-check current active status or owner/admin role before using the admin key. |
| PROD and DEV migrations applied | **Unverified** | Local files cannot prove remote database state. |
| Latest UI deployed to Vercel PROD | **Unverified** | Local Git proves the UI commit exists; it cannot prove which source bundle is live. |

## Release-blocking findings

### C1-01 — Ignored production source files

The Viewer `.gitignore` contains `*credential*`. That pattern ignores both:

- `src/lib/credential.ts`;
- `src/app/api/admin/reset-credential/route.ts`.

Committed routes import `@/lib/credential`, so a clean checkout of `origin/main` is not a reproducible build. The ignore policy must target secret artifacts, not legitimate source filenames.

### C1-02 — Credential generation throws at runtime

Node `crypto.randomInt` permits a range no greater than `2^48 - 1`. The implementation requests `10^15`, so staff creation and credential reset fail before reaching Supabase.

The replacement must generate 15 decimal digits without modulo bias, for example by generating digits with rejection sampling or by composing chunks whose individual ranges satisfy the API limit. Tests must cover length, leading zeroes, numeric-only output, and a large uniqueness sample without treating uniqueness as a cryptographic proof.

### C1-03 — Last-owner lock is not a valid concurrency boundary

`SELECT count(id) ... FOR UPDATE` cannot provide the claimed lock. All owner-reducing operations must first acquire the same stable transaction lock, then count and mutate within that transaction. The preferred database design is:

1. lock the single `businesses` row with `SELECT id ... FOR UPDATE`;
2. lock/read the target staff row;
3. count remaining active owners;
4. reject or mutate;
5. commit.

The same lock order must be used by disable, terminate, demote, and any owner-creation or owner-reactivation path to avoid deadlocks and write skew.

### C1-04 — Reset route can retain revoked privilege

The route uses an admin key after only proving that a Viewer cookie exists and that `my_profile` returned an object. A session whose staff account was disabled or demoted can therefore reach the privileged path until its cookie/token becomes unusable.

Every Auth-touching route must perform a fresh server-side authorization check immediately before privileged work. The check must require an active, non-terminated owner/admin and enforce the operation-specific role rule. Authorization must not depend on a profile cookie.

### C1-05 — C1 has no automated contract coverage

The Account Server contract says `test_contract.py` includes “Group 20+ write RPCs,” but the file ends its functional groups at Group 19 plus existing hygiene checks. Required coverage includes:

- every allowed and denied role per operation;
- disabled, terminated, anonymous, orphan-Auth, and cross-business callers;
- last-owner concurrency with two simultaneous transactions;
- create compensation, including cleanup failure;
- termination with Auth deletion failure and retry/reconciliation;
- reset invalidating the old credential;
- audit actor attribution and secret redaction;
- idempotent enable/disable/assign/unassign behavior;
- clean-clone Viewer build and route tests.

## Important hardening findings

- Create performs privileged Auth creation before a fresh owner/admin authorization check. A stale session can cause privileged work followed by best-effort cleanup.
- Failed create cleanup is ignored, so the “no orphan Auth user” invariant is not guaranteed.
- Termination returns success when Auth deletion fails. The database correctly denies that orphan identity, but the system needs a durable reconciliation job/status rather than a console warning.
- Username normalization/validation must happen before constructing the fake email; the DB currently normalizes later than Auth creation.
- C1 error mapping relies on substring searches and `any`, making stable client errors dependent on raw PostgREST text.
- `README.md` and `SECURITY-REVIEW.md` still describe a read-only product and explicitly forbid the mutations now present.
- The public/admin key header shapes need live contract tests for both legacy JWT service-role keys and current `sb_secret_...` keys; accepting both formats in configuration does not prove both work with the same `Authorization` header.
- Sensitive write responses should consistently set `Cache-Control: no-store`; credentials must never enter logs, analytics, exception payloads, or cache snapshots.

## Acceptance gate for C1

C1 may be called complete only when all of the following are true:

1. A clean clone builds and tests without ignored source dependencies.
2. Account Server migrations and contract updates are committed and pushed.
3. The last-owner invariant passes an actual concurrent test.
4. Auth-touching routes fresh-check authorization before privileged work.
5. Credential creation/reset works and the old credential is rejected.
6. Compensation and reconciliation paths are durable and tested.
7. DEV receives the full destructive suite; PROD receives a non-destructive smoke suite.
8. The exact deployed Viewer commit and exact applied migration versions are recorded.
9. Security, runbook, environment, and recovery documentation match the write-enabled system.

Until this gate passes, Android must depend only on the previously tested public Auth + `my_profile` contract, never on C1 write routes.

## Addendum — 2026-09-25 status note

The verdict above is unchanged: **C1 remains gated.** For honesty of record, remediation work is now visibly in flight in both local working copies, none of it merged or pushed to `main`:

- `SM Admin Server` on branch `fix/c1-hardening` (2 local commits ahead of `main`: C1 hardening migrations and Group 20 contract tests), with uncommitted modifications to `admin.py`, the three C1 migrations, `test_contract.py`, and a new untracked `migration_c1_4_reconciliation.sql`.
- `saaf-milk-admin-viewer` on branch `fix/c1-hardening` (2 local commits ahead of `main`: C1 hardening and build fixes), with 11 modified files plus new untracked `src/app/api/admin/reconcile-auth/`, `src/lib/admin-security.ts` (+test), and `src/lib/reconciliation.ts`.

The gate in this document still governs: until its nine acceptance items pass with committed, pushed, reproducible evidence, the Android Account Bridge treats C1 as unavailable.
