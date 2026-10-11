# Saaf Milk audit — 2026-10-09

**Audit snapshot status: audit complete; production sources unchanged at that stage.** The S24 audit checks used only the isolated QA package with temporary test-only compatibility adapters, which were removed after evidence collection. No production fixes or server mutations were made during the audit.

**Repair continuation, 2026-10-10:** the owner subsequently authorized fixes. Current source changes and their separate verification are tracked in [REPAIR_PLAN_2026-10-10.md](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/REPAIR_PLAN_2026-10-10.md>). The findings below preserve their original severity and snapshot evidence; their old source line numbers refer to the audited revisions. Repair work has not been deployed to DEV/PROD.

**Final scoped continuation, 2026-10-10:** persistent A06 costing and instrumented motion traces are collected. New A01 (MEDIUM repeated history reads in yield context) is repaired locally and verified; ordinary Android/Viewer releases remain separate. DEV C1.6/S01 is deployed and verified; PROD remains unchanged pending explicit approval, with S01 HIGH/open. Additional frame investigation is deferred, and real-session/visual/policy checks remain explicit in audit/2026-10-09/repairs/DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md. This later status supersedes the deployment/profiling statements in earlier dated snapshots without changing their findings or evidence.

## Scope and evidence

**A06 verification continuation, 2026-10-10:** the previously blocked touch batch and two restore process-death boundaries are now verified on the isolated A06 QA package. Debug dataset/motion measurements are collected; frame overruns, release profiling and owner visual acceptance remain open. See audit/2026-10-09/repairs/A06_VERIFICATION_2026-10-10.md and the current repair tracker. The dated findings and evidence below remain unchanged.

**Supabase continuation, 2026-10-10:** MCP read-only checks now cover DEV and PROD. A separate new **S01 — HIGH** finding confirms direct authenticated sync INSERT bypassing RPC controls in both projects. C1.6 is absent live. Local reproductions and the reviewed repair SQL are recorded in audit/2026-10-09/repairs/SUPABASE_VERIFICATION_2026-10-10.md. No live migration or Auth mutation occurred; real-session/reconciliation rehearsal and deployment remain pending. This addition preserves all original findings and severity classifications.

**Subsequent deployment continuation, 2026-10-10:** DEV now has the reviewed C1.6/S01 repair, with deployed DB behavior verified using rolled-back fixtures. Real Auth-session checks remain pending. PROD deployment was rejected by automatic approval review pending explicit authorization and remains unchanged, so S01 stays HIGH/open in PROD. The current checklist is audit/2026-10-09/repairs/DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md. Earlier sections describe their dated snapshots; no historical severity/evidence is rewritten.

Reviewed Android HEAD `4bee92b5d6c9145d9f658a0d0eb0c85cb74f3ea3`, Admin Viewer HEAD `d99510e` at `C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer`, and account-server HEAD `0cf106e` at `C:/Users/Teymur/Desktop/saaf-anchor-tmp`. The sync SQL says it was extracted from PROD on 2026-10-08; this audit did not query production to verify deployment parity. No secrets were read.

This is a broad risk-driven source and execution audit, not a claim that every line or device state has been proven correct. “Source-confirmed” means the trigger and consequence follow from the current implementation; it does not imply a device reproduction. P1 means high-priority integrity/security/recovery risk, P2 means actionable correctness/workflow risk, P3 means lower-priority quality issue. Intentional choices are listed separately.

The current production launcher is native Compose. The retained hybrid UI and bridge remain packaged but dormant. Native facades still share the authoritative domain and command services. Server enrollment is separate from local PIN/session authentication. Sync schema 12, backup format 5 and rebaseline/hold recovery are the most consequential recent additions.

## Findings

### F01 · P1 · Recovery can upload stale restored data before comparison finishes

**Evidence: Source-confirmed.** A failed restore pull returns from `runRestoredRebaselineIfPending()`, but its caller immediately runs the push engine anyway. Restored outbox entries are unheld. A crash after any cursor advancement also clears the recovery flag and resumes ordinary pushes. Separately, a pull that applied some rows before a later network error returns `Done`; live/restore classifiers consequently treat unseen server rows as absent and release them. These paths violate the recorded pull-to-exhaustion requirement and can overwrite newer server master data.

**Repair direction:** Fail the drain closed until a complete, durable classification succeeds. Distinguish progress from exhaustion; restart/recover the seen-row set after interruption. Pre-hold restored entries.

**Sources:** [SyncRuntime.kt:85](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:85>), [SyncRuntime.kt:129](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:129>), [SyncRuntime.kt:362](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:362>).

### F02 · P1 · Offline and failed syncs renew the server-verification grace clock

**Evidence: Source-confirmed.** `drainOnceLocked` refreshes the binding for every outcome except NotConfigured, including an empty outbox (no HTTP call), RetryLater, and Halted. Reopening an offline installation can therefore renew its 14-day entitlement indefinitely. A successful empty pull is also insufficient proof of entitlement: the anchored SQL returns ok/empty for disabled or unassigned staff, rather than a denial. Existing H13 positive-path evidence does not test these negative cases.

**Repair direction:** Renew only after explicit successful active-profile/shop authorization; preserve the last verified time for offline, denied, malformed, and no-network outcomes.

**Sources:** [SyncRuntime.kt:94](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:94>), [SyncRuntime.kt:213](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:213>), [SyncRuntime.kt:229](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:229>), [sync_pull.sql:11](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/sync-rpc-anchor/sync_pull.sql:11>).

### F03 · P1 · Acknowledgement comparison and outbox deletion still have a lost-edit race

**Evidence: Source-confirmed interleaving.** The engine rebuilds the current envelope, decides that it equals the upload, then deletes the outbox ID in a separate database operation. A local edit between those steps finds an already queued UID and does not create a new slot. The subsequent delete removes that edit's only pending marker. A later pull can then replace the unsent change. Structural equality improved C1 but does not make acknowledgement atomic.

**Repair direction:** Perform revision comparison and conditional deletion in one write transaction, or acknowledge a durable revision number while preserving a newer queued revision.

**Sources:** [SyncPushEngine.kt:67](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncPushEngine.kt:67>), [SyncPushEngine.kt:164](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncPushEngine.kt:164>), [SyncMigrationSql.kt:135](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/data/SyncMigrationSql.kt:135>).

### F04 · P1 · Valid parked records stop recovering after five unrelated batches

**Evidence: Reproduced on S24 with the production BundledSQLiteDriver.** Parked records are permanently skipped once retryCount reaches five, even if their missing dependency subsequently arrives. The probe parks a valid expense, applies five unrelated product batches, then delivers its missing account: the expense remains absent. Baseline enqueue places all events before products/accounts, and close actions before cash counts, so a sufficiently large history can consume the budget before required parents are pulled. Bounded retry is intentional, but exhausting it before a dependency arrives without a recovery path breaks eventual recovery. No dependency-arrival reset or owner retry action is provided.

**Repair direction:** Use dependency-aware retries or retry when the dependency changes. A bounded pass may prevent loops, but must not permanently abandon valid records merely because other batches arrived first.

**Sources:** [SyncApplyEngine.kt:203](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:203>), [SyncApplyEngine.kt:660](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:660>), [SyncBaseline.kt:19](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncBaseline.kt:19>).

### F05 · P1 · Re-enrolling in another shop republishes the installation's existing ledger

**Evidence: Source-confirmed.** Stop syncing a populated shop-A installation, then enroll the same device into another authorized shop B. Enrollment replaces the binding and baselines all local tables without comparing the new shop to the existing database identity. Rows absent in B are classified as new and uploaded there. Backup restore has a shop-compatibility check, but ordinary enrollment bypasses that boundary.

**Repair direction:** Require matching environment/business/shop for a populated database, with a deliberate reset or separately scoped database for migration to another shop.

**Sources:** [SyncEnrollment.kt:90](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncEnrollment.kt:90>), [SyncEnrollment.kt:111](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncEnrollment.kt:111>), [SyncEnrollment.kt:138](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncEnrollment.kt:138>), [SyncBaseline.kt:19](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncBaseline.kt:19>).

### F06 · P1 · Viewer logout can revoke the phone's sync sessions

**Evidence: Source-confirmed against provider contract.** Viewer logout calls /auth/v1/logout without scope=local. The same helper runs when an otherwise valid staff login is denied access to the Viewer. Supabase's default is global, so signing out of the website—or a cashier attempting to enter the website—can revoke refresh sessions on Android devices sharing that account. Those devices fail later when their access tokens expire.

**Repair direction:** Use local scope for routine Viewer logout and denied-login cleanup. Reserve global revocation for an explicit account-wide action.

**Sources:** [account-server.ts:156](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/lib/account-server.ts:156>), [account-server.ts:170](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/lib/account-server.ts:170>). Provider behavior: [Supabase sign-out documentation](https://supabase.com/docs/guides/auth/signout).

### F07 · P1 · Ambiguous create-staff failure can delete a successfully created identity

**Evidence: Source-confirmed distributed failure path.** If admin_create_staff_db commits but its response is lost or malformed, callWriteRpc reports unreachable. The Viewer treats every non-ok result as a definite database failure and deletes the newly created Auth user. The staff row can remain committed, with auth_user_id cleared by ON DELETE SET NULL. Its username is reserved and the credential-reset route rejects it as having no Auth identity.

**Repair direction:** Reconcile by operation ID/staff/auth ID before compensation. Delete Auth only after proving the database operation did not commit; preserve ambiguous operations for safe retry.

**Sources:** [route.ts:146](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/app/api/admin/create-staff/route.ts:146>), [account-server.ts:386](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/lib/account-server.ts:386>), [migration.sql:30](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/migration.sql:30>).

### F08 · P1 · Identity disaster-recovery automation fails after its first successful run

**Evidence: Reproduced offline.** The workflow creates an untracked .snapshot_dir, checks out identity-backups, then uses git add -A, committing that marker into the backup branch. On the next run the freshly created untracked marker blocks checkout of the branch's tracked copy. The offline Git fixture reproduced: “untracked working tree files would be overwritten by checkout: .snapshot_dir”. This affects ordinary weekly runs, not only same-day reruns.

**Repair direction:** Checkout the backup branch before generating files, or stage only snapshot data and keep temporary markers outside the checkout. Also support repeat runs on the same date.

**Sources:** [identity-dr.yml:45](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/.github/workflows/identity-dr.yml:45>), [identity-dr.yml:54](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/.github/workflows/identity-dr.yml:54>). Probe: [audit/2026-10-09/probes/identity-dr.cjs](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/probes/identity-dr.cjs:1>).

### F09 · P1 · Restore does not serialize against in-flight sync

**Evidence: Source-confirmed interleaving.** The process-wide sync mutex protects drain/pull, but restore never acquires it. Tokens are cleared only after the restore commits. A pull started at the pre-restore cursor can return after the database is replaced, apply into the restored state and persist its old high cursor over the reset-to-zero cursor. A token clear cannot cancel that already authorized response. Enrollment, unenrollment and hold resolution also mutate sync state outside this gate.

**Repair direction:** Make restore and binding changes exclusive with the whole sync operation, cancel/await in-flight network work, and invalidate stale responses with a database/binding generation token.

**Sources:** [BackupRestoreEngine.kt:32](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/BackupRestoreEngine.kt:32>), [BackupRestoreEngine.kt:207](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/BackupRestoreEngine.kt:207>), [SyncRuntime.kt:68](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:68>), [SyncRuntime.kt:355](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:355>).

### F10 · P2 · A transient push failure after progress is reported as success

**Evidence: Reproduced on host.** With two queued rows and batchSize=1, the first upload succeeds and the second returns a network failure. One row remains, but drainOnce returns Drained(pushed=1). SyncPushWorker maps Drained to WorkManager success, so the remaining work has no retry from that run and waits for another kick. Both batch and single-row fallback branches have this behavior.

**Repair direction:** Return RetryLater(pushedTotal) whenever a transient failure leaves pending work.

**Sources:** [SyncPushEngine.kt:76](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncPushEngine.kt:76>), [SyncPushEngine.kt:98](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncPushEngine.kt:98>). Evidence: [audit/2026-10-09/evidence/TEST-com.saaf.milkshop.data.AuditSyncIntegrityTest.xml](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/evidence/TEST-com.saaf.milkshop.data.AuditSyncIntegrityTest.xml:1>).

### F11 · P2 · Malformed 2xx push responses acknowledge unsent rows

**Evidence: Source-confirmed.** An empty body, invalid JSON, or an object without ok is accepted as SyncPushResult.Success. The engine then deletes unchanged outbox rows even though the server never supplied a valid acknowledgement. The pull client already treats malformed responses as retryable, making push behavior inconsistent.

**Repair direction:** Require the expected response schema and explicit ok=true; retain the queue on missing or malformed acknowledgement.

**Sources:** [SyncServerClient.kt:86](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncServerClient.kt:86>).

### F12 · P2 · Take server can remove the conflict without applying the server version

**Evidence: Source-confirmed.** Resolving a hold with take-server deletes its outbox entry and review row but does not apply the parked payload. Recovery runs only inside SyncApplyEngine.apply. Once the cursor is at the end, the next pull returns immediately on an empty batch, so repeated Sync now operations can leave the old local version indefinitely while the conflict disappears from review.

**Repair direction:** Apply/retry that parked UID as part of resolution, or run a recovery pass on empty pulls. Keep the review item until resolution actually succeeds.

**Sources:** [NativeOwnerSyncFacade.kt:80](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/application/owner/NativeOwnerSyncFacade.kt:80>), [SyncRuntime.kt:336](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt:336>), [SyncApplyEngine.kt:91](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:91>).

### F13 · P2 · Wholesale amount sales create the wrong delivery quantity

**Evidence: Reproduced on host.** The sale facade uses the requested wholesale rate, but deliveryFollowUpFor divides the entered amount by the product's retail rate. The probe used Rs1,200, retail Rs400/L and wholesale Rs300/L: the ledger sale is 4 L, while the follow-up offers 3 L. The service checks product/customer identity but not equality with the sale quantity. The helper also rounds fractional quantities to two decimals.

**Repair direction:** Build the follow-up from submission.receipt.quantity, which already contains the authoritative recorded quantity.

**Sources:** [NativeSaleViewModel.kt:223](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/NativeSaleViewModel.kt:223>), [NativeSaleFacade.kt:176](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/application/sale/NativeSaleFacade.kt:176>). Evidence: [audit/2026-10-09/evidence/TEST-com.saaf.milkshop.data.AuditSaleDeliveryTest.xml](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/evidence/TEST-com.saaf.milkshop.data.AuditSaleDeliveryTest.xml:1>).

### F14 · P2 · Owner reports remain stale after new transactions

**Evidence: Source-confirmed.** Open Reports, switch to Work, record a transaction, and return to Reports during the same session. open() immediately returns for an already Ready state. This ViewModel observes DayStateBus, not DataChangeBus, and selecting the already selected preset also returns. The displayed financial totals can therefore remain from before the transaction until a different preset/range or day-state change forces loading.

**Repair direction:** Revalidate on warm entry and/or subscribe to transaction invalidation while preserving the selected range.

**Sources:** [NativeOwnerReportsViewModel.kt:44](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/NativeOwnerReportsViewModel.kt:44>), [NativeOwnerReportsViewModel.kt:52](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/NativeOwnerReportsViewModel.kt:52>), [NativeOwnerReportsViewModel.kt:72](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/NativeOwnerReportsViewModel.kt:72>).

### F15 · P2 · A finishing refresh can undo sale feedback or reopen the save gate

**Evidence: Source-confirmed interleaving.** refresh() captures previous before loading and later publishes previous.copy(context=...). If record() or sendAsDelivery() changes state meanwhile, the older snapshot replaces its receipt/follow-up/saving flags. A refresh started before submission can restore saving=false while that submission is still running; one started during saving can restore saving=true after completion.

**Repair direction:** Merge fresh context into the current state at publication time and coordinate load/save generations. The existing silentRefresh implementation already demonstrates the safer latest-state merge.

**Sources:** [NativeSaleViewModel.kt:134](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/NativeSaleViewModel.kt:134>), [NativeSaleViewModel.kt:148](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/NativeSaleViewModel.kt:148>).

### F16 · P2 · Old sale operations can publish into a replacement session

**Evidence: Source-confirmed interleaving.** close() invalidates only the load gate. record() and sendAsDelivery() do not compare their captured session/generation after suspension, so a completion can repopulate a closed ViewModel or inject the prior account's receipt into a newly opened one. An old SessionExpired exception can also clear the newer session. This is especially relevant because the hosts keep ViewModels warm.

**Repair direction:** Fence every mutation completion and error handler by session generation; cancellation must not masquerade as a user-visible failure.

**Sources:** [NativeSaleViewModel.kt:100](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/NativeSaleViewModel.kt:100>), [NativeSaleViewModel.kt:198](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/NativeSaleViewModel.kt:198>), [NativeSaleViewModel.kt:282](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/NativeSaleViewModel.kt:282>).

### F17 · P2 · The pre-close sync warning uses unsent work as evidence of successful sync

**Evidence: Source-confirmed.** lastSyncedLabel takes the maximum outbox.enqueuedAt. An offline sale immediately produces “Last synced moments ago” even if nothing uploaded. Successful deletion of the outbox removes that purported evidence. Its refusal counter counts sync_push_halted, whereas closed-day incoming refusals are recorded as sync_apply_skipped, so the counter does not represent the refusal condition described in its comment.

**Repair direction:** Persist successful contact/ack timestamps separately and count the relevant apply-refusal reason. Present pending writes and last verified contact as distinct facts.

**Sources:** [NativeCashCloseFacade.kt:290](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/application/cashclose/NativeCashCloseFacade.kt:290>), [Phase3SecurityDaos.kt:75](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/data/Phase3SecurityDaos.kt:75>), [SyncApplyEngine.kt:628](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:628>).

### F18 · P2 · Cashier Sell/Collect are disconnected from the host tab leave guard

**Evidence: Source-confirmed.** The host's navigation consults activeGuard, but its CashierSaleScreen and CashierCollectionScreen calls omit onRegisterGuard. Both screens default that callback to a no-op. Expense/Deliveries pass the callback correctly. Editing Sell/Collect and tapping another bottom tab therefore bypasses the host's discard confirmation.

**Repair direction:** Register both root forms and test bottom-navigation exits independently from hardware Back.

**Sources:** [CashierPreviewHost.kt:230](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierPreviewHost.kt:230>), [CashierPreviewHost.kt:296](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierPreviewHost.kt:296>), [CashierSaleScreen.kt:146](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierSaleScreen.kt:146>).

### F19 · P2 · Discard and leave does not discard the saved form state

**Evidence: Source-confirmed.** The cashier host confirms that entry data will be cleared, but calls only EntryLeaveGuard.clear() and navigates. The same host keeps the pane in SaveableStateProvider, and clear() changes only guard flags, not remembered form values. Returning to a guarded form can restore the supposedly discarded draft.

**Repair direction:** Give discard an explicit form reset and remove/reset its saveable-state entry. Preserve drafts only on navigation paths whose UI promises preservation.

**Sources:** [CashierPreviewHost.kt:294](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierPreviewHost.kt:294>), [CashierPreviewHost.kt:430](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierPreviewHost.kt:430>), [EntryLeaveGuard.kt:29](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/common/EntryLeaveGuard.kt:29>).

### F20 · P2 · Reports and planning still use the phone's day instead of the business day

**Evidence: Source-confirmed; extends existing M12.** Transaction facades default to BusinessClock.today(), but report presets/defaults and planning cutoffs use LocalDate.now(). With a phone in another zone near midnight/month-end, “Today/This month” can exclude newly recorded shop transactions, and plan revision eligibility can disagree with the shop date. This is more than timestamp rendering.

**Repair direction:** Use the business clock for business date selection and period boundaries; retain device-local formatting only where explicitly desired.

**Sources:** [NativeOwnerReportsViewModel.kt:52](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/NativeOwnerReportsViewModel.kt:52>), [BudgetManagement.kt:55](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/domain/BudgetManagement.kt:55>), [BudgetManagement.kt:130](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/domain/BudgetManagement.kt:130>).

### F21 · P2 · A business restore leaves old conflict-review rows behind

**Evidence: Source-confirmed.** Restore clears the outbox, cursor and parked rows but not sync_rebaseline_holds. That table is neither restored from the snapshot nor cleared. Holds for UIDs absent from the restored snapshot survive classification because classifiers iterate current outbox entries, so the new database can show unrelated old review items with no resolvable park/queue entry.

**Repair direction:** Clear or restore all conflict metadata atomically with the business snapshot, then rebuild it from the new baseline.

**Sources:** [BackupRestoreEngine.kt:171](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/BackupRestoreEngine.kt:171>), [SyncEntities.kt:106](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/data/SyncEntities.kt:106>).

### F22 · P2 · A new pulled attendance row loses its archived state

**Evidence: Source-confirmed.** The existing-row update forwards payload.archived, but the insert constructs AttendanceRecordEntity without archived, using its false default. A new or restored receiving installation can therefore resurrect attendance that was already archived on the source.

**Repair direction:** Copy archived on insert and add a fresh-device round-trip test for archived attendance.

**Sources:** [SyncApplyEngine.kt:495](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:495>), [SyncApplyEngine.kt:516](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:516>).

### F23 · P2 · A pulled delivery silently drops an unresolved sale link

**Evidence: Source-confirmed.** When sale_event_uid is present but its sale is not local yet, applyDelivery resolves it to null and inserts/updates the delivery. The cursor advances, and later recovery of the sale does not repair that delivery. Other dependency paths already park instead. This is reachable when a sale is itself parked awaiting product/account history.

**Repair direction:** Distinguish an absent optional link from a present unresolved UID and park the latter until the sale arrives.

**Sources:** [SyncApplyEngine.kt:447](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:447>).

### F24 · P2 · Local database IDs create false rebaseline conflicts

**Evidence: Reproduced on host.** The wire codec retains local primary/foreign IDs even though receivers deliberately remap them. livePayloadsEqual strips only sync_updated_at. Equal business data with the same UID but different local product_id values therefore compares unequal and becomes HOLD_DIFFER on reconnect. The probe changed only product_id from 7 to 41. Child IDs and arrays can introduce further false differences.

**Repair direction:** Compare canonical business payloads keyed by UIDs, omitting installation-local identifiers and normalizing child ordering.

**Sources:** [BackupSnapshotCodec.kt:21](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/BackupSnapshotCodec.kt:21>), [SyncRebaseline.kt:215](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRebaseline.kt:215>). Evidence: [audit/2026-10-09/evidence/TEST-com.saaf.milkshop.data.AuditSyncIntegrityTest.xml](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/evidence/TEST-com.saaf.milkshop.data.AuditSyncIntegrityTest.xml:1>).

### F25 · P2 · Identity backups/exports silently stop at one API page

**Evidence: Source-confirmed.** The scheduled job requests Auth users with per_page=100 and no page loop. Every REST table and the CLI export also use one unrestricted select with no range traversal or completeness check. Above the configured PostgREST cap, snapshots can omit identities, assignments or audit rows while still being committed as successful. Frequent sync_push audit records make the audit table the first likely victim.

**Repair direction:** Paginate with deterministic ordering, verify counts, and label/export a consistent snapshot boundary. Test more than 100 Auth users and more than one REST page using fixtures.

**Sources:** [identity-dr.yml:36](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/.github/workflows/identity-dr.yml:36>), [identity-dr.yml:41](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/.github/workflows/identity-dr.yml:41>), [admin.py:224](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/admin.py:224>), [admin.py:748](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/admin.py:748>).

### F26 · P2 · The admin CLI bypasses the RPC last-owner concurrency guard

**Evidence: Source-confirmed.** CLI disable/demote/terminate reads a count and later performs a privileged REST mutation. The count is not even business-scoped. Concurrent CLI/Viewer actions can both pass their check and remove the final two owners; an owner in another business also makes a one-owner business appear safe to disable. RPC-side business locks do not protect these direct service-key updates.

**Repair direction:** Route the CLI through the same atomic invariant or enforce it at the database mutation boundary. Scope the invariant to the target business.

**Sources:** [admin.py:339](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/admin.py:339>), [admin.py:505](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/admin.py:505>), [migration_c1_3_hardening.sql:152](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/migration_c1_3_hardening.sql:152>).

### F27 · P2 · The required native QA gate is currently broken

**Evidence: Reproduced.** qaFast fails checkNativeInventory: ten orphan tags and pending workflow-test references for drawing and owner-sync additions. It reports inventory rows=982, untested=32, orphans=10. Frozen baselines and the golden host contract pass. The existing ledger says M1 inventory drift is fixed, which is no longer true at this snapshot.

**Repair direction:** Add real tests and accurate inventory entries for the new controls; do not replace pending evidence with a fabricated PASS.

**Sources:** [scripts/qa/check-native-inventory.ps1](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/scripts/qa/check-native-inventory.ps1:1>), [FINDINGS_STATUS.md](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/FINDINGS_STATUS.md:32>).

### F28 · P2 · Viewer mutation invalidation never matches its cache keys

**Evidence: Reproduced offline against the real module.** Entries are stored as sessionId::/api/..., but viewerCacheInvalidate compares them with raw prefixes such as /api/staff. The mutation callers all supply raw prefixes. The probe edited its fake server response, invalidated /api/staff and got the old cached response with only one total network call. Role/status/shop changes can consequently look unsaved until TTL/revalidation.

**Repair direction:** Invalidate the path portion of session-prefixed keys and coordinate in-flight responses so an old request cannot immediately reinsert invalidated data.

**Sources:** [viewer-cache.ts:50](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/lib/viewer-cache.ts:50>), [viewer-cache.ts:75](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/lib/viewer-cache.ts:75>). Probe: [audit/2026-10-09/probes/viewer-cache.cjs](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/probes/viewer-cache.cjs:1>).

### F29 · P3 · Viewer lint is red despite passing unit tests and type-checking

**Evidence: Reproduced.** npm run lint reports 13 errors and five warnings, primarily the enabled react-hooks/set-state-in-effect rule across list/detail/dashboard components. These are not thirteen separately demonstrated runtime defects. They are a failed quality gate and a mismatch between the accepted implementation and configured lint policy.

**Repair direction:** Refactor the affected state/effect flows or deliberately configure justified exceptions, then make this check part of the required validation.

**Sources:** [package.json:6](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/package.json:6>), [staff-list-panel.tsx:82](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/app/(app)/staff/staff-list-panel.tsx:82>), [overview-panel.tsx:105](<C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/src/app/(app)/overview/overview-panel.tsx:105>).

### F30 · P2 · The account-server checkout cannot independently rebuild the sync schema

**Evidence: Source-confirmed repository coverage gap.** The new sync-rpc-anchor files record deployed functions, but there is no CREATE TABLE sync_records or complete associated grants/RLS/sequence-trigger bootstrap in this checkout. The global-sequence migration exists as SQL in an Android markdown document and assumes an existing table. Restoring the account-server repository alone therefore does not recreate the deployed sync service; the identity JSON backup does not supply this DDL.

**Repair direction:** Create an ordered, executable migration set for the complete server schema and validate a clean-database restore. Keep live deployment parity evidence separate from source anchoring.

**Sources:** [sync_push.sql:23](<C:/Users/Teymur/Desktop/saaf-anchor-tmp/sync-rpc-anchor/sync_push.sql:23>), [SYNC_GLOBAL_SEQ_C1_MIGRATION.md](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/SYNC_GLOBAL_SEQ_C1_MIGRATION.md:86>).

### F31 · P1 · Instrumentation no longer compiles after recent API moves

**Evidence: reproduced on unmodified tracked sources.** The S24 run failed at compileDebugAndroidTestKotlin before installing/running tests: eleven Phase8 test files still import moved command services from application.bridge; NativeOwnerMoreScreenContractTest supplies old enrollment callback arities; SyncEnrollmentTest calls the now-suspending unenroll from non-suspending setup/teardown. A class filter does not avoid compilation of the other tests.

**Repair direction:** update test callers with the same commit that changes the APIs and compile androidTest in CI. Audit execution used a temporary test-only compatibility patch, recorded separately, and restores the original tracked files afterward. Device results obtained with that patch must not be represented as a passing baseline checkout.

**Sources:** [Phase8SaleOperationsTest.kt](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/androidTest/java/com/saaf/milkshop/Phase8SaleOperationsTest.kt:10>), [NativeOwnerMoreScreenContractTest.kt](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/androidTest/java/com/saaf/milkshop/NativeOwnerMoreScreenContractTest.kt:306>), [SyncEnrollmentTest.kt](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/androidTest/java/com/saaf/milkshop/SyncEnrollmentTest.kt:48>).

### F32 · P1 · Failed ledger children commit an incomplete event that re-pull cannot repair

**Evidence: Reproduced on S24 with the production BundledSQLiteDriver.** The probe submits an expense whose financial child violates the zero-amount constraint. The audit confirms that exact child failure, but the event header remains committed. `applyOne` catches the exception and returns SKIPPED, so the outer handler releases its savepoint instead of rolling it back. On a later pull the existing event UID returns PRESENT before its missing children can be inserted. Depending on which child fails, an event can retain no financial rows or only a prefix of its financial/inventory rows. This contradicts the claimed H3 fix and can leave balances and stock inconsistent with history.

**Reproduction:** Apply the archived `failedLedgerChildMustRollBackIncomingEvent` probe against the bundled driver. Expected: one recorded refusal and no event/children. Observed: refusal recorded, header still present. The first host probe was inconclusive because the framework driver did not reach the intended persistent constraint evidence; the device result is the confirmation.

**Repair direction:** Let failures escape to the savepoint boundary, then audit after rollback. Apply the same atomic boundary to parked-record retries. Add a repair strategy for already committed partial UIDs; ordinary idempotent pull currently cannot repair them.

**Sources:** [SyncApplyEngine.kt:69](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:69>), [SyncApplyEngine.kt:146](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:146>), [SyncApplyEngine.kt:213](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:213>), [SyncApplyEngine.kt:294](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/sync/SyncApplyEngine.kt:294>). Evidence: [S24-selected-device-tests.xml:8](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/evidence/S24-selected-device-tests.xml:8>), [AuditDeviceIntegrityTest.kt:17](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/probes/AuditDeviceIntegrityTest.kt:17>).

### F33 · P2 · Owner reports custom-range control is physically covered by the report body

**Evidence: Reproduced on S24 and source-confirmed.** In the Ready branch, the custom-range TextButton row and the full-size scrollable report Column are emitted as sibling roots inside AnimatedContent. AnimatedContent stacks those roots in its content container; the report column therefore occupies the same upper area as the button and intercepts the normal touch. The device test's ordinary performClick() left the callback false. The same test passed only after a direct semantics OnClick invocation, which bypasses hit testing. The follow-on date-entry test could not find the dialog after the ordinary click. This is a real interaction regression, not a stale test expectation.

**Repair direction:** Put the custom action, error text, and report body in one vertical layout (or give the content pane an explicit top offset/weight) so their bounds cannot overlap. Restore the refreshing indicator's testable/accessible surface if that contract is still required; the current nativeRefreshOverlay has no OwnerReportsRefreshing tag, so the old warm-refresh assertion is stale until the UI contract is intentionally redefined.

**Sources:** [OwnerReportsScreen.kt:120](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerReportsScreen.kt:120>), [OwnerReportsScreen.kt:122](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerReportsScreen.kt:122>), [OwnerReportsScreen.kt:150](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerReportsScreen.kt:150>), [NativeAsyncUi.kt:169](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/phase18/app/src/main/java/com/saaf/milkshop/nativeui/common/NativeAsyncUi.kt:169>). Evidence: [S24-focused-ui-diagnostics.xml:8](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/evidence/S24-focused-ui-diagnostics.xml:8>).

## Intentional decisions and incomplete features to revisit

These are **not included as accidental bugs in the numbered findings**.

| Decision or limitation | Current evidence and consequence | Recommended decision |
|---|---|---|
| Admins may reset owner credentials | Account-server CONTRACT.md C1 explicitly permits admin reset. The Viewer does not fetch/check the target's role before replacing its credential. An admin can reset an owner and then sign in as that owner, bypassing the spirit of owner-only promotion/termination. | Restrict resets of owners/admins to owners, or explicitly accept that every admin can become an owner. This is a high-risk policy inconsistency. |
| Owner drawings are deliberately noncorrectable | OwnerDrawingService accepts only positive amounts and writes only a negative cash leg. Correction excludes OWNER_DRAWING. The documented “reverse drawing” remedy has no implementation and fails the database shape check. | Keep the append-only model, but design a supported reversal/return-of-capital operation. Until then, a mistyped drawing has no honest in-app repair. |
| Product/account profile sync is incomplete | applyProfileChild only inserts a subset of new customer fields; it never updates an existing customer and does nothing for vendor/salary/expense profiles. Product profiles are not applied. Inactive/archive flags, limits and contact data can disagree across phones. Comments explicitly defer satellites. | Decide and publish the supported sync surface before pilot; archive/reactivation is operationally significant, not merely cosmetic. |
| Planning rows are pushed but intentionally not applied | budget_plans/collections_targets are explicitly skipped by SyncApplyEngine. Existing M6 remains a roadmap decision. | Either implement pull application or make the one-way limitation visible. Do not describe these tables as convergent sync. |
| Late offline events for already closed days are refused | Reopen/recount/reclose on the closing device is the recorded remedy. Automatic correction is deferred. | Keep the chosen close semantics, but verify the recovery path and display truthful refusal/contact information (F17). |
| Absorb-and-seal close semantics | An earlier open date can be covered by a later close. This is deliberate. The added explanatory seal-audit rows are currently written after the close transaction, so a crash can lose those disclosures. | Preserve the decision; put explanatory records in the same transaction or derive them reliably. |
| Shared device/server identity and local clerk identity differ | Local PIN switching intentionally leaves the installation's server session alive. Per-clerk verified server attribution/device-specific revocation is deferred; a counter identity can upload operational history more broadly than a local cashier screen exposes. | Document the trust boundary. Do not claim server-verified clerk attribution or local-role enforcement on hostile client requests. |
| Plaintext Room | Explicitly accepted, with platform storage encryption, disabled Android backup/transfer and encrypted exports/tokens. | Keep the written acceptance; this audit does not relabel it a new defect. |
| Draft preservation and the 200 ms fade/92%-to-100% scale | Source identifies the motion as owner-preferred and warm state as deliberate. | Do not “fix” the aesthetic by preference. Fix guard/state bugs separately and validate motion on hardware. |
| Product identity derived from Milk/Yogurt name prefixes | Latest patch deliberately supports suffix/probe renames. Arbitrary catalog naming and new product families remain constrained. | Eventually use stable product/rate identity. Do not claim this interim name-based rule is an accidental new regression. |
| Stop syncing retains installation entitlement state | Unenroll removes transport files/tokens but retains the Room binding. Privileged work will eventually degrade with that clock. | Clarify whether Stop syncing means temporary disconnect or return to standalone operation. No silent policy change recommended here. |

## Existing findings rechecked, not silently declared fixed

- **C1 lost edits:** still has the acknowledgement interleaving in F03 and recovery hazards F01/F04/F12/F24. Earlier successful two-device demonstrations do not cover these interleavings.
- **H3 partial event writes:** reopened and device-confirmed (F32). The inner applyOne catch converts a child error to SKIPPED before the outer savepoint handler sees it. retryParksPass also calls applyOne without per-record savepoints. The host framework-driver result was inconclusive; the S24 reproduced the persistent partial header.
- **H13 entitlement recovery:** positive recovery works, but negative authorization/contact conditions remain in F02.
- **M1 native inventory:** regressed at current HEAD (F27). **M2 lint:** Android lint has zero errors; no claim of a new Android lint error.
- **M4 update validation:** product/account apply uses direct UPDATE while many domain shape triggers are INSERT-only. Still source-visible; test adversarial changed unit/category against the bundled driver before expanding its severity.
- **M7 halt-audit throttle:** lastHaltAuditEpochMs lives on each SyncPushEngine instance, and the runtime creates a new engine each drain. The one-hour throttle therefore does not survive successive kicks.
- **M8 malformed pull record:** getLong/getString/getJSONObject inside the record map can throw outside the response-parse catch; one malformed object aborts the pull. No per-record quarantine or pull retry worker is provided.
- **M10 delete capture:** sync is upsert-only. Flag unsupported hard deletion as an invariant requiring enforcement; do not propose broad deletion sync without an explicit product decision.
- **M12 business timezone:** remains and affects date ranges/eligibility, not only formatting (F20).
- **M13 audit runCatching:** audit insertion can be swallowed. The partial-apply investigation specifically requires checking persistent audit evidence, not merely a returned skipped count.
- **M17 blind cashier close versus shared CASH_COUNT_AND_CLOSE permission:** still a policy/surface review item.
- **M27/M29 migration status:** recent command cutover is implemented; old “open” paragraphs are stale. The missed instrumentation callers are a new concrete regression (F31).
- **H10 dependency-order acceptance:** calling the inverted close/count enqueue order harmless depends on successful parking; the permanent retry cap in F04 undermines that premise.

## Concerns requiring more evidence

1. **Animation leave-guard lifetime:** outgoing and incoming panes both register a guard; outgoing onDispose unconditionally calls onRegisterGuard(null). AnimatedContent temporarily retains both panes. Check whether disposal clears the new pane's guard. Do not count it as an observed visual defect until reproduced.
2. **Post-commit UI failure:** several facades commit a transaction and then reload/reauthorize before returning its receipt. A failure in that second phase can show an error for a saved transaction. Cash-close also writes explanatory audit rows afterward. Add a forced-failure regression and distinguish “saved, refresh failed” from “not saved”.
3. **Viewer cache generation:** old in-flight responses can repopulate a cleared cache; old 401 responses can clear a new active session's cache. Session-prefixed keys reduce cross-identity reads, but generation checks are still absent. F28's prefix bug is independently reproduced.
4. **Restore failure cleanup and sync state persistence:** restore flags and tokens are file-backed side effects after the SQL commit. Crash-consistency across these stores needs a deliberately interrupted-device test beyond F09's source interleaving.
5. **Performance:** the source contains sequential reference/history reads and per-batch cost projection rebuilds. This audit has not produced a production-sized dataset benchmark or jank profile; no invented performance measurements are reported.

## Coverage and verification

| Surface | Work performed | Limit |
|---|---|---|
| Architecture and additions | Launcher/cutover, command services, Room schema, bridge inventory, sync/identity/restore and decision-ledger tracing | Broad risk-driven review, not every line independently proven |
| Android money/domain | Existing host suite and golden ledger contracts; sale, delivery, count/close, drawing, correction and profile paths inspected | Costing remains principally device-tested; one existing host engine test is ignored |
| Sync/recovery | Push/pull/apply/baseline/holds/acknowledgement/auth/session/backup source review and targeted probes | No production server mutation or multi-device live convergence run |
| Native UI/workflow | Role hosts, saved state, load/save generations, reports, entry guards, close warnings, shared motion, accessibility tests selected | No complete device/viewport/font/IME/TalkBack/animation matrix yet |
| Admin Viewer | Authentication, mutations, cross-service compensation, cache, 104 unit tests, TypeScript and ESLint | No real staff-account changes; no production browser session exercised |
| Account server | Anchored SQL, role contract, identity CLI/recovery and GitHub DR automation | Local source snapshot only; no live RLS/grant parity certification |
| Dormant hybrid UI | 92-endpoint bridge contract, cleanup gate, 88-file TypeScript check and WebView boundary review | Not a fresh full legacy-UI workflow run |

Recorded checks:

- Baseline Android host suite: **92 tests; 91 pass, one ignored**, no failures. Bridge v5/92 endpoints and UI cleanup pass.
- Android lint: **0 errors, 236 warnings**. API-constant warnings in haptics/quick-unlock have downstream version guards; opening-state maps are replaced on edit. These were not blindly counted as runtime bugs.
- Native qaFast: **FAIL** at inventory; rows=982, untested=32, orphans=10.
- Viewer unit tests: **104 pass**. TypeScript no-emit: **PASS**. ESLint: **13 errors, five warnings**.
- Retained hybrid UI `npm run lint` (TypeScript): **PASS**.
- Offline Viewer cache probe: **reproduced F28**.
- Offline identity-DR Git probe: **reproduced F08**.
- Host audit probes: **F10, F13 and F24 reproduced**. The separate rollback diagnostic is **inconclusive on the framework driver**, not a fourth confirmed bug.
- Initial connected-test command: **FAIL at compileDebugAndroidTestKotlin**, before device execution (F31).
- Selected S24 run with temporary test-only API adapters: **34 tests; 25 passed, nine failed, none skipped**. The two new audit probes confirmed F32 and F04. Two restore failures are stale expectations for intentionally preserved format-5 outbox rows. Reports custom-range failures contributed F33; the warm-refresh and cashier-nav failures are stale contracts. The overpayment test still times out on its second dialog attempt and remains an unconfirmed validation failure, not a numbered product defect. The focused follow-up had the reports custom-range test pass only after semantics invocation and the overpayment test fail again.
- Ordinary production app/data: not installed over or reset by audit commands. Instrumentation target is `com.saaf.milkshop.qa.tests` on S24 `RFCX116CYYX`.
- No visual-acceptance PASS is implied by a functional test pass. A06/device-of-record and owner-reviewed screenshot acceptance remain distinct.

## Repair order

1. Recovery/ledger safety: F01–F05, F09 and F32.
2. Operational continuity: Viewer session scope and identity creation (F06/F07), DR automation/completeness (F08/F25/F30), pending-work retries (F10–F12).
3. Restore trustworthy validation: F31 compilation, F27 inventory and the missing negative/concurrency tests; update the old findings ledger with new evidence.
4. Correct visible workflows: sale-to-delivery quantity, reports, refresh/session races, close warnings and leave/discard behavior.
5. Resolve the explicit product/security decisions above before advertising full multi-device parity.

Audit probes and raw results are retained under [WORKING_NOTES.md](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/WORKING_NOTES.md:1>) and [audit/2026-10-09/probes](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/audit/2026-10-09/probes>). Temporary probes and compatibility adapters were removed from default test source sets; product sources stay unchanged.


## Functional acceptance supplement — 2026-10-10

The original audit above remains the historical findings/evidence snapshot; repair status is in REPAIR_PLAN_2026-10-10.md. Bounded real DEV acceptance is now recorded in audit/2026-10-10/acceptance/FUNCTIONAL_ACCEPTANCE_2026-10-10.md. The owner's 3 skipped / 24 updated sync is verified as existing-event echoes with identical headers/ledger children, with no parked, held or queued records. Repeat sync and real session/staff-revocation checks pass.

New A02 (MEDIUM): Viewer successful SQL void mutations were reported as unavailable because empty HTTP 204 acknowledgements were parsed as JSON. A real committed-disable/503 reproduction is preserved; the response decoder is repaired locally. Final validation is 114 Viewer tests, types, lint/build and 17 real DEV workflow assertions. Original severities are unchanged. S01 remains HIGH and open on PROD pending explicit deployment authorization. Intentional deferred features, visual/design choices, physical power-loss durability, historical recovery authority and production DR acceptance are not silently declared fixed by this supplement.


## PROD server repair supplement — 2026-10-10, 14:15 UTC

The owner explicitly authorized PROD deployment. Exact migration 20261010141548_saaf_c1_6_sync_boundary is applied and verified in saafmilk-prod. S01's HIGH severity is preserved; its status is now repaired in DEV and PROD. Metadata, owner profile/overview/pull, effective access boundaries and unchanged production row digests/counts are verified without inserting test history. See audit/2026-10-10/production/PRODUCTION_SERVER_DEPLOYMENT_2026-10-10.md. Earlier blocked/rejected status statements remain historical evidence. App/Viewer source fixes are not yet published, and no new production end-to-end Auth or disaster-recovery acceptance is claimed.

## Production client release supplement — 2026-10-10

Viewer repair source 115a63634a2010eda0f3e47d96f678ba6d15495f is now committed, pushed and live on the existing production domain. It preserves the newer remote create-staff diagnostics. The exact final GitHub deployment is READY and verified against that commit; 114 tests, lint, cloud build/type checks and final unauthenticated PROD route guards pass. Signed ordinary Android 0.4.1 / versionCode 5 is installed on A06, with matching previous release certificate and matching installed/distributable APK hash; native welcome and Create Owner open successfully. No production credentials, accounts or test history were changed by the client release.

See audit/2026-10-10/releases/RELEASE_2026-10-10.md and verification-release-final.json for current artifacts, publication provenance and owner steps. Actual PROD login/enrollment/sync and authenticated Viewer behavior remain owner acceptance, not passing tests. The existing cloud contract display label is stale metadata, not a write gate. Android source and prepared server CLI/DR changes remain local; remote DR publication/restore and the previously scoped durability, convergence, motion and intentional-policy limits are not claimed complete. Original finding severities and historical failures remain unchanged.

## Actual production phone acceptance supplement — 2026-10-10, 14:48 UTC

The owner's existing PROD account now logs in and enrolls the ordinary signed A06 release. Captured actual UI shows verified owner identity, active server-anchored session, zero upload queue and cursor 107 matching PROD's latest record. The reported updated 16 / other counters zero is confirmed. A normal repeat sync returns push drained and all pull counters zero; read-only MCP checks confirm unchanged 36-row/107 sequence totals, empty Auth reconciliation, valid payload/shop/table metadata and the still-denied direct authenticated INSERT boundary. No new production account or fixture was created. This is real phone Auth/session/workflow evidence, not SQL claims alone.

Evidence and precise limits are in audit/2026-10-10/production-acceptance/PROD_PHONE_ACCEPTANCE_2026-10-10.md. The immediate remaining manual check is actual published PROD Viewer login and expected shop/staff display. Production admin-key mutations, full private-ledger comparison, older persistent parks and previously scoped DR/durability/convergence/design choices remain unverified or deferred. Earlier pending statuses remain historical snapshots; no finding severity or historical failure was changed.


## Owner-confirmed PROD Viewer acceptance — 2026-10-10

The owner responded “all looks good” to the final PROD Viewer login and expected shop/staff check. Record this as owner-confirmed manual login/read acceptance, not an automated browser or production admin-mutation pass. The immediate release checklist is complete alongside the verified real PROD phone enrollment/sync and published clients. See audit/2026-10-10/production-acceptance/PROD_ACCEPTANCE_COMPLETE_2026-10-10.md for the current closure and separately unfinished source-publication, DR, mutation, convergence, durability, motion and intentional-policy work. Historical evidence and all severity classifications remain preserved.
