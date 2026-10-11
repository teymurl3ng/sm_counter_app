# Repair evidence — 2026-10-10

The current implementation/status record is REPAIR_PLAN_2026-10-10.md at the repository root. PROJECT_AUDIT_2026-10-09.md preserves the original audit findings, severities and evidence.

`evidence/` contains both successful and failed repair runs. Do not treat earlier failed, locked-device, incorrect-selector or duplicate-transport runs as passes. The 33-test, final 21-test and receipt/facade 38-test USB batches overlap and must not be summed as unique test coverage. Static inventory coverage is separate from device execution and owner visual acceptance. The pre-repair receipt fault batch deliberately reproduced four product failures; the corrected receipt/facade batch passes all 38 checks.

`sql-check/` is a reproducible offline database harness. `server_offline_test.py` verifies pagination/scoping and local repeated backup-branch behavior without credential files or live endpoints.

Preparation scripts and patch text files record how repairs were developed. They are not a deployment installer or a replayable patch bundle for the current trees: subsequent verified refinements may supersede their intermediate content. Review the actual source in the Android, Viewer and account-server repositories. The historical UI diagnostic patch was not applied; report custom-range evidence uses ordinary touch.

No DEV/PROD mutation or production-app installation occurred. Temporary PostgreSQL clusters were shut down and removed; the ignored binary cache remains available for repeat verification. Product source contains no blanket service-role grants from the rejected proposal.

A06_VERIFICATION_2026-10-10.md records the continuation on unlocked A06: final UI contracts, bundled-driver regressions, isolated before/after-commit process termination and debug profiles. Preserve its failed fixture/infrastructure runs and first-unverified motion artifacts. The strengthened PixelCopy/guard-identity probe supplies the accepted component evidence; owner visual acceptance and release performance remain open. verification-a06-final.json supplements the earlier source/evidence snapshot.

SUPABASE_VERIFICATION_2026-10-10.md records subsequent read-only MCP checks of DEV/PROD and the new HIGH S01 direct-sync-INSERT bypass. SUPABASE_LIVE_REPAIR_2026-10-10.sql is a reviewed, locally verified proposal, not a deployed migration. The two final native batches each pass eleven checks; they have different scopes and are not a unique-coverage total. Real Auth/session and deployed reconciliation checks remain pending. verification-supabase-final.json supplements the older manifests; their dated report hashes are preserved rather than regenerated.

DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md supersedes that deployment status: DEV's reviewed SQL is now applied and verified, including rolled-back DB reconciliation/push/pull/owner checks. No real Auth-session PASS is claimed. PROD's exact deployment was rejected by automatic approval review and remains pending explicit approval; no alternate path was attempted. The release-derived benchmark uses a separate QA package and synthetic persistent data. verification-deployment-final.json supplements the earlier snapshots, including the intentionally preserved failures and rejection.

## Scoped final handoff — 2026-10-10

DEV deployment and rolled-back DB checks are complete; PROD remains unchanged after automatic approval review rejection and needs explicit authorization. Persistent A06 mixed-costing profiling and two instrumented motion traces are complete. A01 (MEDIUM repeated history reads) is locally repaired and verified; production smoothness is not certified. No further profiling is included in this pass. See DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md for current results, deployment limits and the four owner acceptance/decision checks. Older snapshots and failed attempts remain preserved.
