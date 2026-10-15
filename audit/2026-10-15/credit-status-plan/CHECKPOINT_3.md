# Phase 4: DEV server protocol

15 October 2026. Source baseline `88e392af2f938feae1e74dc343e75a83c3d387d0`. This is server-side acceptance only. Phase 4 and its server/client transport checkpoint remain open.

- [x] ✅ ~~DEV contract capability/explicit activation and separate metadata feed.~~
- [x] ✅ ~~Owner-only revisions and review actions, shop isolation, immutable identity/retry and revision conflicts.~~
- [x] ✅ ~~Atomic original-sale agreement validation and old-client refusal after explicit activation.~~
- [x] ✅ ~~39 focused SQL checks before application and against installed functions; cleanup/access review.~~
- [ ] Android metadata transport, restore reconciliation and contracted sale capture/envelope/apply.
- [ ] Existing customer/Sale/history screens and affected two-phone acceptance.

## Delivered boundary

DEV `tghzmofkkdsaasvnixps` has migration `20261015120815_credit_metadata_v1`. The CLI-created local migration was renamed to match the verified remote migration version. No existing shop was activated. No PROD query/migration, ordinary-app installation or Android source edit occurred.

Private contract, append-only metadata and accepted-head tables are accessible through four authenticated RPCs only. Every RPC checks the actual active server staff/shop; editing and explicit activation require Owner. Term revisions use compare-and-swap; identical retries are accepted and changed bodies/stale heads refused. SET/CLEAR history retains unseen concurrent SETs. The independent metadata cursor keeps these record types out of the old global feed.

Activation requires an explicit posting-phone upgrade confirmation and classifies existing original credit sales as legacy weekly evidence. The reviewed global `sync_push` now validates new original credit-sale agreements atomically, preserving old inactive-shop behavior, paid-now sales and identical known legacy retries. An activated shop refuses a new old-style credit sale. Historical accepted revisions remain valid for offline agreements. This server gate cannot prevent an offline old binary recording locally: intended posting phones must be upgraded before activation.

## Verification and limits

[Verification](PROTOCOL_VERIFICATION.json) records **39 checks**, first in a rolled-back migration dry run and again against installed functions. [Fixtures](protocol-fixtures.sql) exercise the authenticated database role and synthetic JWT subject, not HTTP token validation. Checks cover Owner/Cashier/Admin/inactive/cross-shop boundaries; activation; CAS/idempotency; flags/dependencies; legacy catch-up; atomic metadata/global-batch refusal; valid monthly/offline agreements; changed agreement/missing revision; extra financial destination; and paid-now behavior. They do not certify Android retries, client cursor/apply, restored-device convergence or UI.

All synthetic businesses/shops/staff/Auth/sync data were rolled back. Existing counts match: 2 businesses, 10 shops, 63 staff, 17 Auth users and 262 sync rows. Private contracts/records/heads remain empty. Counts are a cleanup check, not a hash comparison of every existing row; sequences may retain gaps after rollback.

Supabase advisors were reviewed. INFO for the three RLS tables without policies is intentional deny-all private storage, with no anon/authenticated schema/table access. WARN for authenticated SECURITY DEFINER RPC execution is the intentional API boundary; actual server authorization, empty search paths and inaccessible private helpers are verified. Findings are retained in the evidence, not suppressed or described as a clean advisor report. Unrelated existing findings were not expanded into this run.

Early fixture type/parenthesis failures were corrected. The dry run exposed an actual PL/pgSQL financial-row alias ambiguity, which was fixed before application. The inactive-Owner fixture was adjusted to respect the existing last-active-Owner guard. The final 39 checks passed before and after application.

Research used [Supabase functions](https://supabase.com/docs/guides/database/functions) and the current [official changelog](https://supabase.com/changelog); the markdown changelog endpoint failed, so the official HTML fallback was used. The [fixed judgment hook](protocol-judgment.json) is a reminder, not external code validation.

## Rollback and next bounded work

[The original global function](sync-push-before.sql) and [installed definition](sync-push-after.sql) are retained for comparison/recovery. With no activated shops and no metadata rows, a reviewed rollback can restore that exact definition and revoke/remove the four new public RPCs and private schema in one transaction. Do not run a destructive rollback automatically, use CASCADE against unrelated dependencies, rewrite remote migration history, or remove metadata after real activation. A populated/activated deployment requires a separate preservation plan; downgrading the posting app is unsupported.

**Next: Android transport and contracted posting only.** Wire capability and the separate metadata cursor/pending acknowledgments into existing authenticated retry behavior; apply dependencies atomically and retain rejected/held work. Reconcile restore against server history. Capture accepted terms inside the sale transaction, include agreement/version in root envelopes, and apply agreement/legacy evidence on pull. Include term/revision in the existing warning snapshot. Keep controls/real activation off until this path is verified. Then implement existing screens and the named two-phone acceptance. No new identity system, generic sync rewrite or broad audit.
