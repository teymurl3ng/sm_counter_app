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

## Addendum — 2026-09-25 C1 ACCEPTED (gate closed)

All nine acceptance items now pass with committed, pushed, reproducible evidence:

1. **Clean clone builds and tests** — viewer: 104/104 tests + `next build` from the committed tree; server: contract suite runs from the committed tree (no ignored source dependencies; `credential.ts` is tracked source).
2. **Migrations committed and pushed** — server `main` `0207e27` carries `migration_c1_admin_write` … `c1_5_error_semantics` plus the CLI-linked `supabase/migrations/` layout.
3. **Last-owner invariant passes a real concurrent test** — Group 20.4 PASS on DEV (77/77 total, verdict PASS).
4. **Fresh authorization before privileged work** — `requireMutationContext` re-checks active/terminated/role per request (C1-04 fixed); verified in-suite.
5. **Credential create/reset works; old credential rejected** — verified in-suite (Group 5 + Group 20).
6. **Compensation and reconciliation durable and tested** — `c1_4` state machine + `reconcile-auth` route + Group 20.10 PASS.
7. **DEV destructive suite / PROD non-destructive smoke** — DEV: `test_contract.py` 77/77 PASS. PROD: `smoke_prod.py` **10/10 PASS** (ping, owner sign-in, `my_profile` shape, header boundary, invalid-credentials shape, anonymous write denial, 404/42P01 + 400/22023 semantics, reconciliation lockdown, logout; zero writes).
8. **Exact deployed commit + applied migration versions recorded** —
   - Viewer: production deployment `dpl_9t9cYUzE16RyGHa7WiqMpV5zBzPZ` **READY** from commit `5ebfd52` (merge of `fix/c1-hardening`), verified serving HTTP 200 on the production alias. Docs-only follow-up commit `d99510e` (not behavior-affecting).
   - Databases: DEV (`tghzmofkkdsaasvnixps`) and PROD (`tlsuxbghuhlqntkgtrxt`) both record the identical nine-version history — `20260920082151`/`20260920090710+21` phase_a, `2026092321xxxx`/`2026092319xxxx` c1_0–c1_2, and `20260925120001`–`20260925120005` c1_1–c1_5 — applied via `supabase db push` with `--include-all` history convergence (out-of-band versions aligned by annotated empty placeholders).
9. **Documentation matches the write-enabled system** — server `CONTRACT.md`/`README.md` updated (in-flight hardening commits); viewer `README.md`/`SECURITY-REVIEW.md` read-only claims replaced with the C1 mutation-boundary description (commit `d99510e`).

**Verdict: C1 is accepted.** The Account Server write-administration channel may now be used for staff provisioning, and the Android Account Bridge may treat C1 outcomes (staff sign-in → `my_profile` verification) as real — while still never calling C1 routes itself and never holding elevated keys, per `ACCOUNT_BRIDGE_PHASE1_SPEC.md`.

**Operational note (non-blocking):** Vercel↔GitHub auto-deploy for the private `sm-server-view` repo is not connected — pushes to `main` do not trigger builds. Production releases use the documented CLI path (`vercel --prod`). To restore auto-deploy, connect the Vercel GitHub App for `teymurl3ng/sm-server-view` in the dashboard (steps in the viewer `SECURITY-REVIEW.md`).

## Addendum — 2026-09-25 hardening completed on DEV (superseded same-day by the acceptance above; retained for evidence continuity)

The in-flight remediation was completed, applied to DEV, verified, and pushed. Current evidence:

- **Server** (`saaf-milk-account-server`, branch `fix/c1-hardening`, commit `4434136`): migrations `c1_1`–`c1_5` committed, including `c1_4` (durable auth-reconciliation state machine) and `c1_5` (error-semantics correction — the original `P0002`/`P0004` codes surface as HTTP 500 on this PostgREST deployment; probed mapping confirmed `42P01`→404 and `22023`→400, and the final write RPC definitions now raise those). All five original release-blocking findings have committed fixes; a CLI-linked `supabase/migrations/` layout records applied versions (20260925120001–120005 on DEV; earlier pre-CLI versions are aligned with annotated empty placeholders).
- **Server contract suite**: `test_contract.py` **77/77 PASS** on DEV (`saafmilk-dev`), verdict PASS — including the real concurrent last-owner test (20.4), invalid/anonymous denial (20.7), and cross-business isolation (20.8). Credential create/reset/old-credential-rejection verified in-suite.
- **Viewer** (`sm-server-view`, branch `fix/c1-hardening`, commit `7d78298`): fresh per-request authorization on every mutation route, safe RPC error mapping for both c1.5-standard and legacy codes, reconciliation route, tests **104/104 PASS**, `next build` passes.
- A DEV probe fixture `probe_phase1` (owner) was created for the Account Bridge probes; its credential is in the gitignored `dev probe credentials.txt` of the server working copy.

**Gate items as they stood at this point (all closed later the same day — see the ACCEPTED addendum):**

1. PROD application of `c1_1`–`c1_5` plus the non-destructive PROD smoke suite (owner-approved step; PROD has not been touched).
2. Deployment of the hardened Viewer (commit `7d78298`) and re-recording of the deployed commit + applied migration versions. The prior "latest UI deployed to Vercel PROD" claim is now **VERIFIED** via the Vercel MCP: account `teimurkasumov` (the owner's single account), project `sm-server-view` (`prj_x3nKY8gE7Qllsl5Dvf0oyoivjIsR`), current production deployment READY from GitHub commit `dd2708e` on `main` — i.e., the deployed tree is the pre-hardening `origin/main` head, and the C1 hardening is not yet live. (An earlier draft of this addendum wrongly inferred "no deployments / different account" from an empty Vercel `list_teams` response; personal-account projects appear under `list_projects`, not `list_teams`.)
3. `README.md`/`SECURITY-REVIEW.md` of both server projects still describe a read-only product and must be rewritten for the write-enabled system.

Until these close, Android continues to depend only on the public Auth + `my_profile` contract per `ACCOUNT_BRIDGE_PHASE1_SPEC.md` §5/§9.
