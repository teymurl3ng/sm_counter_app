# Project audit repair plan — 2026-10-10

The audit remains the historical evidence record. This tracker records implementation and new verification. Intentional decisions in the audit require an explicit product decision before changing their policy.

Source repairs are implemented for all 33 numbered findings, M4/M7/M8/M13 and the receipt/close atomicity concern. DEV's reviewed Supabase C1.6/S01 repair is now deployed and verified. Android/Viewer source release, PROD database deployment, real-session acceptance and historical-data recovery remain open; see the dated deployment continuation below.

## Sequence and completion criteria

1. Restore instrumentation compilation, then repair ledger atomicity, dependency recovery, acknowledgements, transport validation, rebaseline, grace verification, shop binding and restore coordination. Use host tests for pure logic and isolated bundled-driver device tests for transaction behavior.
2. Repair Viewer session scope, ambiguous identity creation, cache generations and quality gates; repair server DR pagination, branch workflow, last-owner invariant and reproducible schema bootstrap. Verify locally before any deployment.
3. Repair sale quantity/state/session handling, reports refresh/date/layout, close warning evidence, navigation guards and discard behavior. Retain the accepted animation and business policies.
4. Repair native inventory and stale tests with genuine contract coverage. Investigate the collection repeat-confirmation timeout and animation guard lifetime before classifying/fixing them.
5. Run the required host, compile, lint, inventory, Viewer and server checks; run targeted tests on the explicitly named isolated S24 package if available. A06 and owner visual acceptance remain distinct.

## Findings

| IDs | Work | Status |
|---|---|---|
| F31 | Instrumentation API callers and compile gate | Repaired; all instrumentation sources compile and qaFast passes |
| F32 | Atomic incoming event and parked retry application; existing partial-row repair | Implemented; bundled-driver rollback and partial-event repair tests pass. Historical rows require re-pull |
| F04, F22, F23 | Dependency retry, attendance archive, delivery sale links | Implemented; late-parent recovery, first-pull archived attendance and preserved sale-link checks pass |
| F03, F10, F11, F24 | Atomic acknowledgement, retry outcomes, strict acknowledgements, canonical payloads | Implemented; edit-during-upload, retry and malformed acknowledgement device checks pass; canonical host check passes |
| F01, F02, F05, F09, F12, F21 | Rebaseline, verified grace, shop boundary, sync/restore gate, conflict resolution, metadata cleanup | Implemented; partial recovery, negative/identity-matched proof, identity fence, invalid shop selection, cross-shop binding, serialized teardown, conflict rollback and hold cleanup pass |
| F06, F07, F28, F29 | Viewer session/identity/cache/quality | Implemented; final 109 tests, types, lint and production build pass |
| F08, F25, F26, F30 | Server DR, pagination, owner invariant, schema bootstrap | Offline/WASM/native checks pass. DEV C1.6 deployed; final-owner and reconciliation DB checks pass. PROD remains unmodified pending explicit deployment approval. Fresh timezone/profile parity corrected; real Auth and DR acceptance remain pending |
| F13, F15, F16 | Sale delivery quantity and state/session generations | Implemented; three controlled asynchronous device tests pass |
| F14, F20, F33 | Reports refresh, business dates and touch layout | Implemented; reports ordinary-touch tests pass; shop-zone month-boundary check passes |
| F17, F18, F19 | Truthful close warning, root navigation guards and actual discard | Implemented; contact/refusal evidence and same-frame guard disposal checks pass; sale/correction/backup/costing discard touch checks now pass on unlocked A06 |
| F27 | Native inventory and meaningful workflow evidence | 992 rows, inventory untested=0 and orphans=0; static gate passes. A06 final batch passes 102 UI contracts plus two observational probes; owner visual acceptance remains separate |

## Unconfirmed checks and policy decisions

- Collection overpayment second confirmation: passes after closing the test IME; no product change was needed for that observation.
- Animation guard disposal: both cross-frame and strengthened same-frame live-slot ownership checks pass.
- Receipt loading: the four forced-failure probes reproduced saves surviving a failed receipt/context read, including a close surviving failed seal-audit persistence. Nine native entry facades and cash-close count/close/reopen/resolve now keep required local receipt reads inside the mutation transaction. The corrected 38-test device batch passes, including an additional real-sale rollback probe. This resolves the concern through rollback on failure, without returning stale context as a successful receipt.
- Restore file crash consistency: source review confirms the fresh-identity fence is persisted before Room replacement, every rebaseline preference commit is checked, and token-cleanup failure cannot clear that fence. A06 before-commit and after-commit process-death checks now pass in fresh processes; the stale token cannot trigger network access. Power-loss/filesystem durability remains unproven.
- Keep the audit's intentional choices (drawing policy, shared identity, plaintext Room, close semantics, profile/planning deferrals, accepted motion) separate.
- Deployment is a separate final action after reviewable code, migrations and local verification.

## Additional existing findings repaired

- M4: incoming product/account updates validate the same shape rules as inserts; invalid unit/type/category update fixture passes. This does not add a new product policy restricting otherwise valid edits.
- M7: the halt audit throttle reads durable audit state inside a transaction, so new runtime instances and process restarts do not reset the hour window; repeated new-engine fixture passes.
- M8: malformed, unordered and foreign-shop pull batches return retryable failure without advancing the cursor; fixture passes.
- M13: refusal/conflict/parking audit persistence is required for the apply transaction. Failure rolls back the batch instead of silently acknowledging a record; worker exceptions request retry and cancellation remains cancellation. The injected-audit-failure fixture passes. Deliberate best-effort local actor attribution remains separate.
- Audit concern 2 / absorbed-day disclosures: required seal audit rows commit in the same outer transaction as the close and receipt reads. Absorb-and-seal policy is unchanged. The post-command fault probes exercise actual facades against the bundled Room driver, rather than a mock success response.
- Removed three identical unreachable OwnersAvailable branches in OwnerSyncController as stale code; behavior is unchanged.

## Verification log

Historical baseline evidence remains under audit/2026-10-09/. Repair evidence is separate under audit/2026-10-09/repairs/evidence/.

- sync-batch-fixed.xml: 27 selected device tests pass after enrollment fixtures were corrected.
- combined-second.xml: 97 selected device tests, 96 pass and one delivery-link fixture failure. The fixture lacked required sale header fields. Corrected and verified in sync-final-33.xml; this older run remains recorded as failed.
- sync-final-33.xml: all 33 selected device tests pass, including strengthened old-sale-session completion, exact wholesale delivery quantity, recovery and boundary fixtures.
- sync-audit-usb-final-21.xml: all 21 final USB-selected tests pass, including missing-refusal-audit rollback, failed-recovery-audit rollback, foreign-identity proof rejection and same-frame guard disposal. These overlap earlier batches; counts are not summed as unique coverage.
- viewer-final-tests.txt: all 109 Viewer tests pass. Final type check, lint and production build also passed (build output reported the existing middleware-convention deprecation).
- sql-native-roles.txt: PostgreSQL 18.6 bootstrap and eleven checks pass, including authenticated RLS and concurrent service-role disable/demote/delete at READ COMMITTED and REPEATABLE READ. Auth objects are local stand-ins. Clusters used localhost and were stopped/removed.
- sql-wasm-final.txt: five checks pass in the reproducible PostgreSQL WASM harness. server-offline.txt: seven Python/Git fixtures pass; three repeated local backup-branch runs preserve snapshots.
- native-inventory.txt: static inventory passes with 992 rows, untested=0 and orphans=0. COVERED means an actual contract test is mapped; it is not an execution PASS or owner visual sign-off.
- qaFast-final.txt and the later sync-audit-final-log.txt show QA_FAST: PASS. Host suite: 93 tests, 92 pass, one existing skipped case. android-lint-final.txt confirms lint passes with zero errors and 236 warnings. android-release-compile.txt and android-final-compile-qa.txt confirm release compilation passes, including the later duplicate-branch cleanup. android-post-receipt-final.txt confirms the final post-receipt qaFast, debug/androidTest/host gates, release compilation and lint all pass (BUILD SUCCESSFUL, exit 0). Host results remain 93 tests, zero failures/errors, one skipped; final lint remains zero errors and 236 warnings.
- receipt-boundary-before.xml: five targeted checks, one pass and four failures, before the additional receipt/close atomicity repair. These are deliberately injected after-command faults, preserved as product regression evidence.
- receipt-boundary-selection-error.xml/.txt: the first post-fix selection completed 28 results, 27 pass and one failure loading a nonexistent NativeDrawingTest class. All five receipt-boundary probes passed. The incorrect selector was removed, affected payment contracts and two actual-sale checks added, and the corrected run is recorded separately. The selection failure is validation infrastructure, not a product failure.
- receipt-boundary-final-38.xml and receipt-boundary-final-log.txt: 38 tests, zero failures/errors/skips. Seven boundary tests cover failed drawing/collection/count/sale receipt reads, failed seal-audit persistence, successful sale totals and successful absorbed-close disclosures. Existing affected facade contracts and asynchronous sale-state checks pass in the same batch. These overlap earlier batches and are not summed as unique coverage.
- Earlier failed runs (sync-batch-first.xml, ui-first.xml, combined-first.xml) are preserved. Device checks target the isolated QA package on USB S24 RFCX116CYYX, not the production application.
- The newest guard/control batch initially failed compilation due to a mistaken test tag and unavailable Espresso dependency; corrected. controls-locked-device.xml then recorded 23 completed tests, five pass and eighteen UI failures while dumpsys showed the secure lock screen. The run was stopped; its process-crashed termination is not presented as a product crash.
- sync-audit-failure-final-log.txt and sync-audit-compile-race.txt preserve intermediate compilation failures corrected before the latest QA_FAST pass. A later run omitted the USB selector and attempted both aliases of the same phone, producing zero results and instrumentation failure; logs/XML are preserved with duplicate-transport labels. The corrected USB-only rerun passes all 21 tests.
- At this earlier verification stage, server changes were prepared source and A06 hardware/motion remained outstanding. The dated continuations below supersede those access/test blockers; deployment and owner visual acceptance remain pending.
- verification-final.json reconciles all 33 numbered IDs with this tracker, preserves the actual XML test counts (including failures/skips), and records SHA-256 hashes for the 79 changed/new Android Kotlin source and test files. All three repository diff checks pass with their existing Windows Git newline configuration.

## A06 continuation — 2026-10-10

Full details and artifact names are in audit/2026-10-09/repairs/A06_VERIFICATION_2026-10-10.md. A06 SM-A065F / Android 16 / USB R8VXA02SNKH was unlocked. Tests used only com.saaf.milkshop.qa.tests. No production source changed during this test continuation, and no DEV/PROD or ordinary application data was changed.

- a06-ui-first-102.xml: 102 tests, five failures. a06-ui-profile-second-104.xml: 104 tests, three failures. These preserve incorrect merged-text/placeholder assertions, a nonunique Bank selector, both unsupported IME actions, a fixture dropping navigation callbacks and a boxed Int/Long comparison. The initial progress message understated the first failure count; these counts match the saved XML. The actual values and callback wiring were checked before correcting tests.
- a06-ui-profile-final-104.xml: 104 tests, zero failures/errors/skips (102 UI contracts and two observational probes). The motion probe was subsequently strengthened after identical UiAutomation screenshots made visual coverage inconclusive. Its original test PASS does not imply visual acceptance.
- a06-data-final.xml: 44 tests, 42 pass, zero failures/errors, two intentional skips. Covers actual bundled-driver sync/rebaseline/acknowledgement/audit/receipt rollback and asynchronous sale regressions. The skipped methods are the opt-in destructive probes; they must not run during default discovery. The console's final 46 count includes extra skip notifications; the XML contains 44 distinct cases.
- a06-restore-process-results.json and both fresh-process-verification logs: before-commit and after-commit recovery each pass. The preparatory runs were deliberately killed after readiness markers, and are recorded as interruptions, never passing tests. Synthetic databases, preferences and tokens were isolated behind a ContextWrapper; the physical Room fixture file was asserted. Safety-snapshot creation and power loss were outside this probe.
- a06-profile-first-manual-run.txt: two probes pass; a06-native-data-profile.json preserves the full 10,000-collection dataset measurements. Collection/sale/month-report medians were 44/273/461 ms; p95 79/323/531 ms. A separate successful Gradle run had 73/327/525 ms medians and is preserved, showing run-to-run variation. These are debug, in-memory, synthetic collection-heavy measurements, not production disk or costing-workload budgets.
- a06-motion-strengthened-run.txt: one strengthened probe passes, checking the actual incoming pane's guard identity across 30 real-time transitions. PixelCopy window captures show distinct sale/report panes. a06-native-motion-profile.json contains 754 frame observations, median total duration 27.6 ms, p95 263.5 ms, maximum 328.7 ms and 36 observations over Android's per-frame deadline. Keep this as an open debug performance concern; no smoothness or release-budget PASS is claimed. The requested mid capture shows an already settled report, so it is not certified evidence of an intermediate animation frame.
- Earlier missing-package collection, wrong-runner launch, unverified screenshot/frame artifacts and missing-allow.logic QA errors remain preserved as validation infrastructure evidence. Gradle uninstalls the QA packages after connected tests. The actual registered runner is SaafMilkTestRunner. QA_FAST and instrumentation assembly pass with the already-authorized logic exception.
- verification-a06-final.json records authoritative XML counts, evidence/report hashes and 81 current Android source hashes. All three final read-only repository diff checks pass; external-checkout Git checks needed execution outside the sandbox. verification-final.json remains the prior snapshot. Existing numbered finding severities and historical evidence are preserved.

## Remaining completion checks

1. Owner visual acceptance of the A06 layouts and accepted motion remains pending. The previously blocked touch cases now pass on A06; a complete viewport/font/IME/TalkBack matrix is still outside this selected batch. Earlier S24 secure-lock failures remain historical evidence.
2. Persistent mixed-costing profiling and two release-derived instrumented motion traces are complete. A01 (MEDIUM repeated history reads) is repaired locally; seven costing tests and post-repair qaFast/host/lint gates pass. Frame overruns remain an observational concern, without proof of production/AOT behavior; additional frame investigation is deferred as a separately scoped task. Accepted animation policy is unchanged.
3. DEV C1.6/S01 is applied and verified through MCP. PROD's exact deployment was rejected by automatic approval review for insufficient explicit production authorization; the approval question is pending. Rehearse real Supabase Auth/session and reconciliation with an authorized DEV test account. SQL role/claim simulations and Auth stand-ins do not prove real sessions. Android/Viewer source release remains separate.
4. Review affected historical partial event UIDs. Owner-approved same-shop re-enrollment can trigger full classification; use Take server for held partial events whose immutable header matches. The code fills missing children on re-pull and refuses divergent history. No deployed cursor or business history has been changed by this repair task.
5. If power-loss durability is required, plan a separate filesystem/device failure check. The completed A06 process-death tests prove their two specific commit boundaries only.

The automatic approval review rejected a proposed blanket service-role grant in bootstrap. That proposal was dropped and no blanket grants were applied or left in product source. Only limited staff permissions were added inside the disposable SQL fixtures to model existing CLI/service-role access.

## Supabase continuation — 2026-10-10

See audit/2026-10-09/repairs/SUPABASE_VERIFICATION_2026-10-10.md for the captured metadata, advisor classifications and limits. saafmilk-dev and saafmilk-prod were checked through MCP using read-only SQL; neither project was mutated.

- **S01 — HIGH, newly verified:** both live environments permit authenticated direct INSERT into sync_records. Its RLS membership check bypasses sync_push's table whitelist, correction-role check, business header binding and per-shop lock. Read-only EXPLAIN accepts the plan; a native replay reproduces the bypass. Live aggregate checks show no observed malformed payloads, unknown tables or business/shop mismatches; no live attack was performed. This is separate from F01–F33 and does not change their severities.
- C1.6's identity-verification RPC and final-owner trigger are absent in both projects; the existing recorder rejects create_verify. Deployed C1.5 functions otherwise match the prepared source. The early SQLSTATE drift suspicion was resolved, not promoted to a finding.
- Local bootstrap now includes the observed nullable shop_timezone field and matching profile output; no hardcoded server timezone default was added. The internal redactor has a fixed search_path locally. Intentional RPC-only tables and authorized SECURITY DEFINER APIs remain separate from S01. Fresh/live sync FK deletion semantics differ and require an explicit policy decision.
- supabase-bootstrap-parity-final.txt: eleven native bootstrap/concurrency checks pass. supabase-native-boundary-repair.txt: eleven native replay/repair checks pass, including atomic abort on incompatible history and preserved ordinary RPC behavior. These use disposable PostgreSQL 18.6 and Auth stand-ins, not deployed migrations or real GoTrue sessions.
- SUPABASE_LIVE_REPAIR_2026-10-10.sql is the exact reviewable proposal: C1.6, ordinary-role direct-sync-write/sequence revocation, payload-object validation and internal redactor hardening. No Auth deletion, blanket grants, sequence reset or history rewrite. It has not been applied.
- verification-supabase-final.json supplements the historical manifests with current report, SQL, source and evidence hashes. Older report hashes represent their earlier snapshots and are preserved. All 81 A06 Android source hashes still match; no Android retest is implied by these server checks.

## Deployment continuation — 2026-10-10

The preceding Supabase section records the read-only/pre-deployment snapshot. Subsequent authorization allowed DEV deployment: migration 20261010102351_saaf_c1_6_sync_boundary is applied, ordinary-role direct sync writes are denied, C1.6 owner/identity checks pass and the redactor warning is cleared. An expired connector request was verified unapplied before retry. The DEV write fixture rolled back all rows; sequence allocations remain consumed. An initial verifier's ambiguous variable and a catalog-order assumption were corrected; failed evidence is retained.

Automatic approval review rejected the exact PROD deployment because explicit production authorization was missing. PROD is unchanged and S01 remains HIGH/open there. No workaround was attempted. The agent can complete PROD deployment and automated verification after the outstanding approval.

The release-derived A06 profiling continuation uses a separate QA benchmark target, a persistent mixed costing fixture and a Perfetto trace; its actual completion status is in audit/2026-10-09/repairs/DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md. Earlier source/evidence snapshots remain preserved. verification-deployment-final.json records this later state.

The owner-facing checklist in that document separates real-session and visual acceptance, authoritative historical recovery choices and provider/product policy decisions from agent work. No approval rejection or missing real Auth session is represented as a successful check.

## Narrowed handoff and A01 repair — 2026-10-10

The owner requested a shorter, bounded completion. Stop new profiling and close with the verified DEV deployment, local repairs and current evidence. A01 is a separate MEDIUM product performance finding: loading yield context read complete effective history once per production batch. The bulk assessment repair preserves policy/validation semantics and reduces the A06 40-batch fixture median from 4932.6 to 356.7 ms (three measured samples each). Seven costing cases, the persistent probe and post-repair gates pass. No signed ordinary Android release or Viewer publication is claimed.

The final checklist and scope boundary are in audit/2026-10-09/repairs/DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md. PROD deployment needs explicit authorization after automatic approval review rejection. Remaining owner checks are real-session acceptance, visual acceptance, recovery authority and provider/product policy choices. Optional direct-Activity profiling was abandoned before device execution; its unused host source was removed and compile evidence retained. Original F01–F33 and S01 classifications are preserved.


## Functional DEV acceptance continuation — 2026-10-10

The owner supplied the new DEV login/enrollment and reported 3 skipped / 24 updated. Those three events are already-present immutable records: server payloads match local headers and all ledger children. The acceptance shop has 27 server records through sequence 222; local outbox, parks and holds are zero. Two repeat A06 drains are quiescent, including after Viewer logout/session checks.

A02 (MEDIUM) was discovered and repaired: Viewer write RPCs parsed successful empty HTTP 204 responses as JSON and returned false 503 errors after committed void staff mutations. The decoder now accepts empty 204 only for verified SQL void RPCs, preserving strict identity/shop response handling. Final Viewer validation: 114 tests pass; types, lint and fresh build pass. Actual DEV creation/login passed before the reproduced disable failure; the same fixture was reused after repair. Final real-session workflow has 17 passing checks, including independent refresh survival after logout, existing disabled-session read/mutation denial, new login denial, final-owner protection and an empty reconciliation queue.

See audit/2026-10-10/acceptance/FUNCTIONAL_ACCEPTANCE_2026-10-10.md for evidence, unsuccessful attempts, current package/build and limits. Previous reports/manifests are dated snapshots. This finishes the bounded DEV phone/account checks; it does not claim an injected ambiguous Auth failure, another phone's convergence, full deferred-feature sync parity or production DR restoration. Intentional behavior and deferred design/profile work remain separate. PROD S01 (HIGH) and publication remain open pending explicit production authorization after the earlier automatic approval rejection. No production data or publication changed.


## PROD server deployment — 2026-10-10, 14:15 UTC

After the owner's explicit approval, the exact DEV-verified C1.6/S01 migration was applied once through Supabase MCP to saafmilk-prod (tlsuxbghuhlqntkgtrxt), recorded as 20261010141548_saaf_c1_6_sync_boundary. S01 remains HIGH in severity and is now repaired/verified on both DEV and PROD. Postflight grants, enabled owner guard, validated payload CHECK, internal-function access and owner profile/overview/sync reads pass. All 36 production sync rows and business/shop/staff data digests are unchanged; counts of identities/audit/reconciliation are unchanged. No production fixtures or account resets were used. The redactor search_path warning is cleared; existing RPC-only advisories and disabled leaked-password protection retain their prior classifications.

Evidence and exact limits: audit/2026-10-10/production/PRODUCTION_SERVER_DEPLOYMENT_2026-10-10.md. Earlier pending/rejected-PROD snapshots remain historical. Android and Viewer publication are still separate release work; the local Viewer and A06 preview remain on DEV. No production end-to-end Auth/client or DR restore pass is implied.

## Production client release — 2026-10-10

The repaired Viewer is now published at https://sm-server-view.vercel.app/login from committed/pushed source 115a63634a2010eda0f3e47d96f678ba6d15495f, preserving the newer remote diagnostics change. Final stable-domain GitHub deployment is READY and its metadata matches that commit. All 114 Viewer tests pass, ESLint exits 0, cloud production build/type checks pass, and staged/final unauthenticated PROD login/API guards pass. Existing production secrets were retained. The cloud contract display label remains stale v1.1.0-read-admin; source confirms it is not a write gate.

Signed ordinary Android release 0.4.1 / versionCode 5 / com.saaf.milkshop builds and is freshly installed on A06. Its certificate matches the previous release and installed bytes match the saved APK. Native welcome and Create Owner render successfully. The DEV acceptance preview is hidden with disable-user, preserving data/session; no production owner or server account was created. Android source remains an uncommitted repaired workspace, identified by source/APK hashes rather than a claimed clean Git release.

Current report, distributable APK, source snapshot and evidence verifier: audit/2026-10-10/releases/RELEASE_2026-10-10.md. Immediate owner work is private local-owner setup, existing PROD server enrollment/sync and PROD Viewer login. Genuine production authentication/mutations remain unverified until those results. The server CLI/DR repository changes remain local; production DR restore, physical power-loss durability, another phone's convergence and prior design/feature/policy choices remain separately scoped. Earlier failures, severities and dated snapshots are preserved.

## Actual PROD phone acceptance — 2026-10-10, 14:48 UTC

The owner completed production local-owner setup and existing-account enrollment. Actual signed-release A06 UI confirms PROD, verified owner identity, active session, trusted server time, zero waiting uploads and cursor 107. Reported pull updated 16 / all other counters zero matches the captured UI. MCP reads show 36 server rows through sequence 107 and 16 changed mutable rows since deployment baseline 72 (eight accounts, two products, six settings); no unknown tables or business/shop mismatches. One normal repeat sync is quiescent with every pull counter zero; server sequence/count remain unchanged. Real GoTrue sign-in time corroborates the phone's production identity/session. No new account or SQL fixture was created, and the direct-sync-insert security boundary remains closed.

Immediate owner acceptance now needs only published PROD Viewer login and confirmation of expected shop/staff information. See audit/2026-10-10/production-acceptance/PROD_PHONE_ACCEPTANCE_2026-10-10.md. Production admin-key mutations, private ledger-child comparison, older persistent parks, multi-phone convergence, DR and prior optional/intentional choices remain explicitly outside this bounded pass. Earlier reports/manifests retain their historical pending statuses and hashes; the new acceptance manifest identifies the later tracker snapshot.


## Bounded PROD acceptance complete — 2026-10-10

The owner replied “all looks good” after the final request to sign in to the published PROD Viewer and confirm expected shop/staff information. PROD Viewer login/read screens are now manually accepted by the owner; this is distinct from an automated browser test or production admin-key mutation check. Together with the actual signed-release phone login/enrollment and quiescent repeat sync, this completes the immediate DEV/PROD release acceptance checklist. No immediate owner action remains.

Current closing note: audit/2026-10-10/production-acceptance/PROD_ACCEPTANCE_COMPLETE_2026-10-10.md. Android/server CLI source publication, live DR restoration, production admin mutations, multi-phone/full-ledger convergence, durability and production motion remain separately unfinished or deferred. Existing stale contract metadata and intentional product/policy decisions remain recorded. Earlier manifests and pending statuses retain their historical meaning; verification-owner-acceptance-final.json identifies the later owner confirmation and tracker hashes.
