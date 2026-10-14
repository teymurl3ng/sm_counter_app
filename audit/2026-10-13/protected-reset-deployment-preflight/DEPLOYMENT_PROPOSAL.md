# Protected reset — exact rollout proposal

Preflight is complete for the named surfaces on 13 October 2026. This is a proposal, not a deployed fix. No migration, push, deployment, login, real reset or identity fixture creation ran in this unit.

## Verified targets and current state

| Surface | Exact target | Observed state |
|---|---|---|
| Development database | `saafmilk-dev`, `tghzmofkkdsaasvnixps` | Healthy, PostgreSQL 17.6; reset guard/schema/functions absent |
| Production database | `saafmilk-prod`, `tlsuxbghuhlqntkgtrxt` | Healthy, PostgreSQL 17.6; reset guard/schema/functions absent |
| Viewer | MKRC / `team_4S1INLjqHa6cjTQCbHxuAt6l`, project `sm-server-view` / `prj_x3nKY8gE7Qllsl5Dvf0oyoivjIsR` | Current production deployment `dpl_C13bg6KbNN1mCsCCbKqbVc3MLHts`, READY; aliases include `sm-server-view.vercel.app` |
| Viewer source | `https://github.com/teymurl3ng/sm-server-view`, main | Local HEAD and remote main both `b5122e820ebb79f18c3288a5e4a49805911e7f02`; deployment metadata agrees |
| Account-server source | `https://github.com/teymurl3ng/saaf-milk-account-server`, main | Local HEAD and remote main both `0cf106e8422a6a04da340b025dbef5fa0018074e`; existing unrelated local changes preserved |

The public unauthenticated `/api/system` response is HTTP 401 with `account_server.env=prod`, Viewer commit `b5122e8` and `UNAUTHENTICATED`. No authenticated account or reset behavior was tested. Production and Preview both have the six required environment-variable names, including `ACCOUNT_SERVER_ADMIN_KEY`, recorded as Secret/Hidden. Their values were not retrieved. Presence alone does not prove a valid key, exact remote URL binding, or successful Auth access; runtime acceptance must verify those. The local `.env.local` identifies the known DEV URL; the cached local production file is not used as evidence of current hosted values.

The selected DEV/PROD catalog fields match: staff columns/types, seven existing reset-related functions/signatures/search paths and two existing staff triggers. The seven normalized definitions match the saved native catalog. All ten C1 source hashes still match the 8a1 fixture, and all eight installed integration files match the reviewed manifest. This is bounded dependency parity, not a full database audit. The SQL proof ran locally on PostgreSQL 18.6; remote PostgreSQL 17.6 acceptance is still required.

Migration histories are not identical: C1.6 has remote versions `20261010102351` (DEV) and `20261010141548` (PROD), while its local filename uses `20261010120000`. DEV also records the applied/reverted owner-funds experiment. Neither history contains this reset migration. Do not run a broad `supabase db push` or repair history in this rollout. Apply only the reviewed reset SQL via an exact single-migration operation and save the actual remote migration identity plus catalog hashes. Keep the CLI-generated source filename as provenance.

Vercel MCP reads returned 403 for MKRC scope. Installed CLI 59.16.0 with its existing credentials successfully inspected the same explicit project/team and current deployment. No reauthentication or target switching was performed. The CLI is the working route for the proposed rollout.

## Exact payload

Use the [8a2 manifest](../protected-reset-integration/manifest.json), [Viewer patch](../protected-reset-integration/viewer.patch) and [server patch](../protected-reset-integration/server.patch). The Viewer payload is four files: reset route, account-server allowlists, reset eligibility helper and staff-detail page. The server payload is CLI reset command, contract, operations guide and the new migration. Preserve earlier account-server modifications; do not publish all dirty files as part of this fix.

Migration source: `20261012194632_protected_credential_reset_guard.sql`, SHA-256 `282876836d1981a934c7a5e0ff31fe1be58c3f2ba64751d39b87636e4a08d204`. No dependency/version, Android or ledger change belongs to this release. Freeze/hash the reviewed candidate and record the resulting commit IDs before publication. Recheck branch/file/catalog drift immediately before each external write.

## Next bounded execution: DEV rollout and disposable acceptance

This is the next authorization boundary. It includes an exact DEV migration, a Viewer preview against DEV, and newly created disposable DEV identities only. It excludes production migration, production promotion, existing staff credential changes and unrelated source publication.

1. Apply only the exact SQL above to `tghzmofkkdsaasvnixps`. Verify the six public RPC signatures, private schema/table permissions and RLS, empty definer search paths, participant trigger and applicable security advisors. Do not reapply C1.6 or the reverted owner-funds gate.
2. Create an isolated candidate checkout from the verified Viewer base plus only its four manifest files. Publish a protected Preview to the exact MKRC project, with source hashes/commit recorded. Use the existing Preview environment and verify its `/api/system` reports DEV before any fixture action. Never promote that preview directly as the tested production build: Preview and Production configuration differ.
3. Create uniquely named disposable Owner, Admin and Cashier DEV identities and a synthetic shop/business context as required by the existing contract. Generate credentials in memory; retain no plaintext in evidence. Use a unique run prefix so no existing identity is reused. Verify the fixture IDs and target project before any Auth change. Existing real/test accounts outside this fixture remain untouched.
4. Run the focused live groups below. These are the deployment acceptance checks; earlier synthetic checks remain credited and are not repeated as a full suite.
5. Verify no pending reservation remains for fixture participants. Terminate the newly created disposable accounts using the supported lifecycle and retain their audit history. Do not delete or force-release uncertain rows. A real unknown outcome stops the run with its request reference and an Owner-review blocker.
6. Save only safe results, IDs/references, source/catalog hashes and deployment URL. Record the new DEV migration history identity. Stop with a production proposal based on the accepted candidate; do not continue into PROD, recovery or phone testing automatically.

### Focused live groups

| Group | Required result |
|---|---|
| Environment/transport | Protected preview binds to DEV; signed-in fixture receives fresh role/eligibility; service-only RPCs/private tables are inaccessible to ordinary staff tokens |
| Policy denial | Fixture Admin cannot reset fixture Owner/Admin or disabled Cashier. No Auth update or reset-success audit for denial; original fixture credential still works |
| Authorized success | Admin resets active Cashier; Owner resets an eligible disabled target without enabling it. Each valid claim produces one Auth update and one attributed completion audit; new credential works, old credential is rejected |
| Guard/single claim | With a deliberately reserved fixture request and no Auth dispatch, protected caller/target access changes fail; a second claim fails. Cancel only when known undispatched and no worker can race the cancellation. No arbitrary production reservation |
| Known-success completion recovery | Fixture-only gateway performs one known successful Auth update and deliberately defers completion. Pending reference is visible and duplicate reset is blocked; complete the original request without another Auth update, then verify one audit and released participants |
| UI feedback | Browser shows denial clearly, pending guidance/reference, one-time credential after known success and completion-only status; staff navigation does not show another account's credential |

Lost/unknown Auth outcomes remain covered by the 8a2 synthetic gateway tests. Do not deliberately create an unknowable live Auth request just for a green test; the guard has no automatic expiry. If an actual outage occurs, preserve the pending state, stop, and use the trusted outcome procedure. Security/permission rejection is not automatically assumed to prove no Auth mutation.

## Following boundary: production, after DEV acceptance

Production rollout is separate. Apply the same reviewed SQL to `tlsuxbghuhlqntkgtrxt`, stage the same reviewed Viewer source with Production configuration and `--skip-domain`, verify environment/session behavior without resetting existing staff, then promote the exact staged production deployment to the existing domains. Record source, build/deployment and actual database migration identities. The account-server CLI must use the same reviewed integration; distribute/publish only its scoped source changes through the separately authorized repository action.

During the transition, the old production Viewer still has its direct Auth reset path. SQL installation alone does not remove that bypass. Coordinate the release with no old-client reset use during that window; do not claim B4 complete before both components and the supported CLI path are accepted. If an enforceable reset pause is required, prepare that small release control before executing this transition rather than assuming the guard intercepts Auth API calls.

A staged production build is promoted without rebuilding; a Preview-to-Production promotion changes configuration/rebuilds. See [Vercel promotion documentation](https://vercel.com/docs/deployments/promoting-a-deployment). Supabase Auth admin updates are server-side operations and immediately change the Auth user; keep their keys out of client code. See [Supabase Auth update documentation](https://supabase.com/docs/reference/javascript/auth-admin-updateuserbyid).

Rollback: retain SQL/private requests/trigger while participants are held. Do not restore the old direct-reset route and still describe the policy as enforced. Prefer a fail-closed reset-disabled gateway with other Viewer reads unaffected; prepare and test that exact fallback before production publication. No automatic uncertain unlock, Auth replay, schema drop or secret retrieval is part of rollback.

B4 remains **local integration verified; deployment acceptance pending**. Recovery 8b, setup 8c, shop-day/phone acceptance and final release remain in their existing phases.
