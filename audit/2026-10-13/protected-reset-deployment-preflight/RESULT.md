# Protected reset deployment preflight — complete

Read-only preflight confirmed the exact DEV/PROD database targets and current Viewer deployment. Neither database has the prepared reset guard installed. The current production Viewer points to committed source `b5122e8`; the four new local Viewer files are not in that deployment. Public `/api/system` reports PROD and that commit without a session. Both remote main hashes agree with the respective local HEADs.

Selected DEV/PROD reset catalog fields match. Seven normalized function definitions match the saved native catalog; ten C1 source hashes remain identical to the 8a1 fixture. All eight installed integration files still match the reviewed 8a2 manifest. Remote PostgreSQL is 17.6; local concurrency proof was 18.6, so no live version acceptance is claimed.

Production and Preview environment-name/type metadata contains the needed settings; values were not retrieved. No conclusion about live admin-key validity follows from presence. The Vercel connector returned 403 for MKRC scope, while the same explicit team/project CLI fallback succeeded. No login, relinking or connector configuration was attempted.

The scoped [deployment proposal](DEPLOYMENT_PROPOSAL.md) identifies exact projects, migration/source hashes, release order, six live acceptance groups, fixture cleanup, known-outcome completion and rollback limits. Local/remote migration timestamps differ, so it specifies one exact migration operation rather than a broad history push. No migration was applied, no source was pushed, no Viewer was published, no real password reset and no identity fixture creation occurred.

Verification used catalog SELECTs, migration listing, bounded deployment/project/env metadata, Git remote hashes, public metadata and file-hash comparisons. No broad suite or Android check ran. An initial evidence parser matched the prose reference to an untrusted-data tag rather than the actual JSON block; its newline-anchored extraction was corrected and `verify.py` passed. Those are evidence parsing steps, not database changes.

Next proposed bounded run: **DEV-only migration + protected Viewer preview + newly created disposable identity acceptance** using the exact payload. Authorization for those concrete external mutations is separate from this completed read-only/proposal run. Production migration/promotion and repository publication remain separately scoped after DEV acceptance. No existing staff is a reset fixture. Recovery 8b and setup 8c remain separate.

Whole plan: 1 money/withdrawals, 2 receiving sync and 3 mapped menus/lists complete at bounded QA scope; 4 access/recovery/setup in progress (local reset implementation and deployment preflight complete, rollout/recovery/setup pending); 5 shop-day/phone acceptance pending; 6 freeze/accepted release pending.
