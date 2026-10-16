# Reminder contract published and verified on PROD

17 October 2026. The user explicitly approved the exact package from commit `7129418`. ✅ ~~The two reminder migrations and scoped production verification~~ are complete on **saafmilk-prod (`tlsuxbghuhlqntkgtrxt`)**. [Frozen manifest](PROD_PUBLICATION_MANIFEST.json), [approval brief](PROD_PUBLICATION_BRIEF.md), [saved verification](PROD_ROLLOUT.json).

Installed from the unchanged approved source bytes:

- `customer_followups_v1` → actual PROD version `20261016195451`.
- `followup_actor_indexes` → actual PROD version `20261016195459`.

The versions are server-assigned UTC history identifiers. They were retained as returned; DEV history was not renamed or repaired. Immediately before application, source/check hashes, absence of conflicting objects, required columns, existing sync functions and protected row fingerprints still matched the reviewed package. No long-running transaction was observed at that check. Both migration calls succeeded without retries or rollback work.

**All72 contract checks passed inside `BEGIN`/`ROLLBACK`.** These cover current-actor authorization/isolation, explicit opt-in, schedules/CAS, shared heads and preparation ownership, original/replayed uncertain handoffs, explicit review, primary transfer and the daily notification reservation. The original checks were unchanged apart from the approved pre-fixture date/time guard. Synthetic identities/business records rolled back; Postgres sequence gaps may remain, as disclosed in the brief. No Android tests were repeated for this SQL-only publication.

Post-check evidence independently confirms:

- All seven function definitions and the six-table/sixteen-index inventory match deployed DEV, including the intended RLS, grants and fixed search paths. Anonymous RPC execution and direct authenticated/anonymous private-table access remain denied; RPCs still check actual active leadership and shop membership.
- All six new reminder tables contain zero rows. No policy, customer schedule, claim, command, action or digest is left by QA, and no real shop is activated.
- Protected business/shop/staff/sync/Credit-contract row hashes are unchanged, and existing `sync_push` / `sync_staff_shop_ids` definitions are identical. Counts remain one business, two shops, eight staff, three Auth users,36 sync records and zero Credit contracts. The existing last-Owner trigger remains enabled.
- Security advisors have no new notice. The only performance delta is an informational unused-index group covering the six new actor FK indexes. They are initially empty-table indexes with no live reminder traffic; keep the FK coverage rather than delete it on that observation alone. [Supabase unused-index guidance](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index). Existing unrelated notices were not expanded into this checkpoint.

The Supabase and Postgres-best-practices skills and judgment hook were used for the reviewed publication/verification. The hook is a reminder, not approval; the user's exact response supplied approval. No service credential entered the Android client, and no customer app/message, paid service or provider was used. No ordinary APK was installed. The preflight manifest and brief remain historical preparation records; this result is the current publication status.

**Next:** signed ordinary delivery of the accepted phase5/6 source under phase1. Production availability does not opt in a shop; real enablement remains an authenticated Owner action after compatible posting devices are updated and synced. C's automatic-SMS route/cost/activation decision, staff/private acceptance, independent backup/signing custody and the explicitly deferred clerk-language cleanup remain open.
