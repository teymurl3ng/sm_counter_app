# B Android data checkpoint result

16 October 2026. Source baseline `36da8f0`. Completes only the [frozen data scope](B_ANDROID_DATA_SCOPE.md) inside existing B. No new phase. No ordinary UI entry, scheduler, notification, composer, cloud/schema change, release install or customer contact.

## Accepted implementation

Room18→19 adds two empty metadata-only tables and their exported schema. The explicit migration preserves an existing ledger amount. The accepted snapshot is keyed by environment/server/shop/server staff/installation; no balance/contact copy is introduced. Strict codecs check the actual v1 contract, schedule versus edit revisions, heads, time/date, duplicate identities, claims and receipts. Failed refreshes leave stale data; explicit refusal removes the cache. Malformed snapshot time cannot anchor the shared clock.

The request journal commits the exact ID, body and binding before network access. Cancellation, malformed acknowledgment and unknown result retain it. A subsequent invocation is recovery-only, even if its first request never reached the server. Completion is persisted before a possible live grant returns; restored/recovered/replayed results expose neither a launch nor a notification grant. Grants are never serialized. These rules may lose an interrupted side effect; they do not retry one automatically. Actual handoff/fresh-financial gating remains the next existing B wiring.

Read-only SyncRuntime methods share the existing process-wide gate and active application UserSecurityService, require an enrolled current local leadership session, and refresh a 401 once. Unknown refresh preserves tokens and stale cache; definitive invalid refresh clears credentials and cache. The actual server independently checks current membership/role. Ordinary drain/pull has no new reminder request or automatic command drain.

Backup schema13→14 is separate from Room19. Only unfinished request identity is exported, requiring the existing encrypted/security-bearing path when nonempty. Derived cache and side-effect grants are omitted. Business replacement clears both tables transactionally. Imported commands are validated and recovery-only; older schema backups remain supported but cannot hide a new journal section. Restored binding/install identity is never silently retargeted. Older app versions reject newer backup schema14.

## Focused evidence

Build succeeds for the isolated main/test APKs. Thirteen unique methods have latest passing results across [initial](b-data-first-results.json) and [three-method corrected](b-data-corrected-results.json) runs; [combined record](B_ANDROID_DATA_VERIFICATION.json). Eleven new checks cover codec/cache/migration/journal/recovery/session behavior; two existing encrypted Credit-backup regressions verify roundtrip, rollback and legacy compatibility. Only `com.saaf.milkshop.qa.tests` was installed/reset on A06. Ordinary shop package and cloud data were untouched. No full suite or two-phone concurrency claim.

The first run passed10/13. One fixture omitted required `events.surcharge_applied`; corrected to0. Two failures exposed a real loader defect: constructing a fresh security service created an unrelated session manager. The loader now uses the application's active service, with an explicit injected service for the isolated in-memory test. The three affected methods then pass3/3; the refresh test also verifies definitive revocation. No source shared by the other ten passing methods changed afterward.

## Independent judgment after hook

The hook is a fixed reminder, not approval. Inspected the new transport/store/codec, actual server receipt shapes, application session ownership, registered migration and transactional restore placement. The first tests falsified the original session-loading implementation; the corrected session ownership is now verified directly and by role/logout checks. Passing storage tests do not prove worker behavior, financial freshness, physical concurrency or delivery. A raw cache would be smaller but cannot preserve uncertain request identity; an automatic offline outbox could repeat an external effect and is unsuitable here. The bounded read cache plus explicit recovery journal is supported at this checkpoint. Device assignment strings remain coordination, not device attestation.

Existing research applies: [Room migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions) and [Supabase functions](https://supabase.com/docs/guides/database/functions). No platform update or remote migration was needed. Clerk wording stays explicitly deferred in LATER.md.

## Remaining B and C

Connect the accepted panes to current-session loading/saves, Owner enable/primary controls, persistent in-app attention and quiet WorkManager detection. Put every supported coordinated message entry through the shared handoff gate and fresh-facts checks; expose held/restored requests for explicit recovery. Then run the pinned affected DEV and actual two-phone acceptance, obtain exact PROD migration approval and deliver through phase1. C remains the separately bounded SMS route/cost/activation decision. No customer sending is enabled.
