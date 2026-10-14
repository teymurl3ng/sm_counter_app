# 9b2a: accept remote close history without weakening local verification

This is a direct dependency of the planned two-phone closing test. No production change has been made in this baseline run.

## Exact defect

A real authenticated local Cashier close is serialized with its count UID and actor name/role. SyncApplyEngine.applyCloseAction deliberately leaves actor_user_id=NULL because another phone's integer ID is not a local identity. validate_verified_daily_close_actor refuses that insert whenever the receiving app has users. The tested receiver has its own Owner at a deliberately different ID; copying a foreign ID or matching a display name would be an invalid repair. The empty-user receiver control and two native role controls pass.

## Proposed smallest boundary

Use the existing trusted-import distinction already present in BackupRestoreEngine: a verified same-shop remote close record needs its snapshot preserved, not a fabricated local login. Isolate the local-user actor-validation exception to the remote close insertion inside the existing write transaction/savepoint. Preserve and restore the actor guard on success, malformed input, failure and cancellation. Keep all other close/count/date/role/history/foreign-key validation active. Never relax ordinary authenticated commands or allow snapshot text to authorize a local reopen.

Planned production boundary: SyncApplyEngine.kt's close insertion/refusal handling only; SyncEntities.kt only if a small metadata query is required to preserve the existing actor trigger definition. No schema version, provenance flag, fake user, foreign-user mapping, new identity system, server RPC/RLS/role change or global trigger disabling. If that isolation cannot preserve the guard exactly, stop and record the specific alternative before expanding.

Ordinary pull currently advances its cursor after skipped records. Retain refused close bodies in the existing parked-row mechanism rather than silently losing them; clear only a successfully applied/current body. Prove reapplication when its cash count already exists. Do not introduce a general historical crawler. Previously skipped history, if needed, must use a separately verified bounded replay procedure; new acceptance must not claim old installations have been repaired automatically.

## Focused finish checks

Make the initialized-receiver regression pass while preserving the empty-receiver control. Preserve remote actor name/role/time, UID count/supersedes remapping, local user rows and original cash/stock movements. Replaying the close must not duplicate count/action records. Prove ordinary local NULL-user/fake-user closing and Cashier reopening remain refused; Owner reopen still requires reason/recent authentication and recount. Prove malformed/refused imports are retained, cannot partially write, and do not leave local actor validation disabled. Run only the new boundary cases and affected existing native closing/receiver controls. No full suite or physical radio change in 9b2a.

After acceptance, scope/execute the fixed 9b2 physical scenario with both phones. Add physical_cash_counts and daily_close_actions to the accepted 18-table comparison, remapping count/action references by UID and treating only device-local actor integers as local identity metadata. Compare all other close fields and count/history semantics. Leave 9b3 interrupted recovery, human acceptance, hosted gates and signed delivery separate.
