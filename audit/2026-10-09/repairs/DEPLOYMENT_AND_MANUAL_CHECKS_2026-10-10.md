# Deployment and remaining acceptance checks — 2026-10-10

This continues the existing audit and fixes. F01–F33 severities and all failed/partial runs remain preserved. Earlier A06 and read-only Supabase reports describe their dated snapshots; this document records the subsequent deployment state.

## Supabase work completed

saafmilk-dev (tghzmofkkdsaasvnixps) received the exact reviewed SUPABASE_LIVE_REPAIR_2026-10-10.sql through MCP apply_migration. Its recorded migration is 20261010102351_saaf_c1_6_sync_boundary. The initial connector call returned an expired request-state error; migration history and a new catalog confirmed no application before one successful retry. This was connector infrastructure, not a failed SQL migration.

DEV now has 36 public functions, the C1.6 identity-verification RPC and enabled final-owner guard. Authenticated sync SELECT and existing service-role access remain available. Anonymous table access, ordinary-role INSERT/UPDATE/DELETE and sync-sequence access are revoked; the permissive INSERT policy is gone. The object-payload check is installed, and the internal redactor's fixed search_path clears that advisor warning. Existing sync_push, sync_pull, profile and unrelated RPC bodies are unchanged.

Deployed DEV checks confirm:

- Authenticated RPC push/pull round trip and unknown-table rejection.
- Linked identity reconciliation resolves; an unlinked identity stays pending.
- Direct disable, demotion and deletion of a disposable business's final owner are denied.
- Direct authenticated sync INSERT planning fails with the intended SQLSTATE 42501.
- Owner profile/overview/pull still work; the no-identity context sees zero rows.
- The service-role recursive redactor removes synthetic password/token values and preserves harmless values.

The write checks used an explicit rollback transaction. No fixture business, staff, sync or reconciliation rows were committed, and no Auth accounts/passwords/tokens were changed. PostgreSQL sequence allocations are not rolled back: the two DEV fixture attempts consumed sequence values, without resetting them. Counts remain 39 sync rows, zero malformed payloads/unknown tables/business-shop mismatches, zero unlinked active staff, zero businesses without an active owner and zero pending reconciliations.

The first rollback verifier failed on an ambiguous variable/column name (42702). It was corrected and the complete rerun passes; the failed result remains saved. Logs for 10:20–10:34 UTC record that error, the expected permission denial (42501), and transaction wrapper warnings (25001/25P01). An initial log query used an absent severity key and returned empty severities; a corrected query uses parsed.error_severity. These summaries do not certify an end-to-end application session or all traffic.

These are deployed database calls with SQL role/claim contexts, not GoTrue login, JWT verification, refresh or revocation tests. Android and Viewer source repairs have not been installed/published to the ordinary production app/site by this continuation.

## PROD gate

saafmilk-prod (tlsuxbghuhlqntkgtrxt) remains unchanged. Automatic approval review rejected the exact PROD migration because it revokes live client writes and adds enforcement triggers/functions, and the review requires explicit authorization for that production deployment. The approval question was presented; no alternate execution path or retry followed the rejection. A subsequent catalog still shows 34 public functions, no C1.6 trigger, and the vulnerable direct INSERT grant/policy. **S01 remains HIGH and open in PROD.**

After explicit approval, the agent can apply the same SQL, run metadata/permissions/read checks, and rerun advisors. This is an agent deployment action, not a SQL task the owner needs to perform manually. No PROD business-history test rows should be inserted.

## A06 automated performance continuation

The separate com.saaf.milkshop.qa.benchmark target inherits the existing release build configuration, is profileable, and uses debug signing for instrumentation. It has a benchmark-only ComponentActivity declaration and a separate NativeReleaseProfileProbe. The earlier 81 repaired Kotlin source hashes remain intact; the newly measured A01 repair changes two additional production files and augments Phase3CostingTest. This is a release-derived instrumentation fixture, not the signed production package or a production traffic benchmark.

The completed fixture uses an isolated persistent bundled-Room database, 10,000 collections, 21 stock purchases, 40 yogurt batches, 20 yogurt sales and a costing policy. After the yield repair, collection receipt median/p95 is 38.1/66.8 ms, sale receipt 293.3/307.9 ms and monthly reports 406.9/424.5 ms (20 samples each, two warmups). These are observational measurements, not production budgets.

**A01 — MEDIUM, locally repaired: repeated history reads in yield context.** NativeOwnerCostingPlansFacade previously loaded effective history, then YieldPolicyService.assess reloaded it for each production batch: 41 full reads for 40 batches. YieldPolicyService.assessAll now assesses the requested batches from one history read, while the facade retains its separate context read. Historical policy lookup, validation, alert status and single-batch behavior remain preserved; no cross-call cache or animation policy change was added. Costing.kt and NativeOwnerCostingPlansFacade.kt contain the fix. The same physical A06 fixture measured yield-context median 4932.6 ms before and 356.7 ms after (p95 4960.6/405.6 ms; one warmup and three measured samples per run). This establishes the repeated-read defect and improvement for this fixture, not a universal latency promise.

All seven Phase3CostingTest device cases pass, including bulk/single assessment equivalence, historical and unusual-yield behavior, empty input and invalid ID rejection. Post-repair qaFast, host tests and lintBenchmark complete successfully: host XML records 93 tests, zero failures/errors and one existing skip; benchmark lint records zero errors/fatals and 235 warnings. The persistent dataset probe passes before and after the repair.

Motion collected two real Choreographer/guard-identity runs of 30 transitions, PixelCopy captures and bounded Perfetto traces. The first run records 888 FrameMetrics observations, median 22.5 ms, p95 127.0 ms and 26 over Android's frame deadline. The second, after successful package-level full-compilation requests, records 882 observations, median 20.1 ms, p95 129.4 ms, maximum 187.5 ms and 25 over deadline. Its FrameTimeline contains 515 app surface frames: 469 Buffer Stuffing, 38 App Deadline Missed, seven mixed and one with no jank. FrameTimeline covers startup/warmups/captures as well as transitions and has a different scope from FrameMetrics; these counts are not app FPS or a smoothness PASS. JIT thread CPU remains visible despite the successful compilation requests, so this does not establish production/AOT performance. The accepted animation policy remains unchanged, and the performance concern stays open for a separately scoped investigation.

The second trace initially yielded no app rows because the captured process name remained zygote64. Identification by the exact QA app surface layer and PID corrected the query; both outputs are retained. No nonzero error/data-loss stats were returned by the completed trace queries. The requested mid screenshot may already be settled and is not certified evidence of an intermediate frame.

The initial sandbox build could not open the Gradle cache lock. The first external build failed only because the new verifier referenced a private database-name constant; the fixture now verifies the actual isolated .db file without accessing that constant. Both logs are preserved. The existing ordinary app is not cleared, restored or replaced.

## Narrowed completion scope

At the owner's request to reduce elapsed time, this pass stops at verified repairs, evidence reconciliation and the acceptance checklist below. No additional benchmarks, animation retuning or provider changes are included. An optional direct-Activity profiling host compiled successfully but was never installed/executed; its unused source and manifest entry were removed, and its compile log remains infrastructure evidence only. Future frame investigation is a separate task. No broad test suite is repeated without a new failure or production change.

## Owner checks requiring real sessions or judgment

1. **Real DEV session/workflow acceptance:** use the repaired Viewer build pointed at DEV and an authorized owner test login. Sign in, create a disposable staff identity, sign out of the Viewer, then refresh/sync a phone sharing that account: website sign-out must leave the phone session usable. Disable the disposable staff identity and confirm its next protected request is refused; re-enable only if it is meant to remain available. Exercise create/reconciliation through the actual Auth endpoint. SQL simulations do not replace this check, and MCP exposes no Auth sign-in/session API here. The agent can automate more once an authorized real DEV session is available; no credentials were guessed or extracted.
2. **Visual acceptance on A06:** inspect sale/report navigation, payment picker, reports/date ranges, close disclosures and discard prompts using the repaired QA build. Check the accepted motion and the font size/keyboard/TalkBack settings you actually use. Functional contracts and screenshots cannot decide whether the layouts and motion meet your preference.
3. **Historical recovery selection:** identify any affected partial-event UIDs and choose which same-shop device/history is authoritative. The agent can then execute the recovery procedure; it must not guess business truth, advance deployed cursors or overwrite divergent records. Full reclassification/re-pull remains available through the repaired enrollment path.
4. **Policy choices:** decide whether to enable leaked-password protection and review provider PostgreSQL patch eligibility in the Dashboard; MCP has no update-Auth-config/upgrade operation. Resolve fresh NO ACTION versus live CASCADE sync deletion policy before standardizing it. Existing protected RPC-only tables and authorized SECURITY DEFINER APIs should not gain permissive RLS policies or lose application access merely to silence advisors. [Password-protection reference](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection), [provider patch notice](https://supabase.com/changelog/postgres-15-19-17-11-breaking-changes).

Physical power-loss durability is optional if required by the product. The completed before/after-commit force-stop probes cover process death only; this task does not justify a destructive power/storage experiment on a shop device.

Evidence is under evidence/deploy-* and evidence/a06-benchmark-*. verification-deployment-final.json supplements older snapshots and records actual current report/source/evidence hashes. Nothing in this document upgrades intentional behavior, stale test failures or infrastructure errors into product defects.


## Later authorized PROD deployment — 2026-10-10, 14:15 UTC

The owner's explicit approval supersedes the earlier PROD authorization gate. Migration 20261010141548_saaf_c1_6_sync_boundary is now applied through MCP and verified. S01 retains HIGH severity and is repaired in both environments. Read/access checks pass and production history/account data remains unchanged. Current evidence and remaining client-release limits are in audit/2026-10-10/production/PRODUCTION_SERVER_DEPLOYMENT_2026-10-10.md. Earlier rejection, DEV tests and dated manifests above are preserved; do not treat their old PROD-pending statements as current deployment status.
