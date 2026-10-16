# Exact reminder contract publication — prepared, awaiting approval

17 October 2026; source baseline `cc60170`. Destination: **saafmilk-prod (`tlsuxbghuhlqntkgtrxt`)**. This package prepares publication only. No migration, synthetic fixture or ordinary app update has been applied to PROD. [Exact source/check manifest](PROD_PUBLICATION_MANIFEST.json).

## Change

Apply the existing two DEV migration sources, unchanged and in this order:

1. `customer_followups_v1` — `supabase/migrations/20261016110904_customer_followups_v1.sql`: six new private tables for policy, customer schedule/review state, message preparations/handoffs, accepted actions, original-command results and daily alert reservations; five private helpers; two authenticated RPCs; two explicit lookup indexes and primary/unique indexes.
2. `followup_actor_indexes` — `supabase/migrations/20261016112511_followup_actor_indexes.sql`: six indexes covering the new staff foreign keys.

The private tables enable RLS and deny direct anonymous/authenticated access. The two public RPCs deny anonymous execution, fix their search path and check actual active leadership/shop membership; only Owner may change policy/primary assignment. They retain the already verified sync/credit advisory-lock and request-recovery semantics. No existing financial table/function is replaced. No existing record, balance, term or customer message is rewritten. No real shop is opted in; an Owner must later enable the policy after compatible posting phones are updated/synced. No paid service, SMS provider, customer sending or phone installation is included in this approval.

## Verified baseline

PROD is healthy on Postgres17.6 and has no conflicting reminder schema/RPC. Its required shop/staff/sync/Credit column signatures and existing `sync_push` / `sync_staff_shop_ids` definitions exactly match DEV. The seven prepared function bodies match the deployed DEV bodies. The DEV inventory has six RLS-protected private tables, sixteen indexes in total and the intended two RPC grants. Source hashes are frozen in the manifest.

Read-only PROD counts: one business, two shops, eight staff, three Auth users,36 sync records and zero Credit contracts. Protected row fingerprints are saved for the exact publication window. A read-only check observed no transaction older than30seconds; this is not a promise that publication can never wait for a concurrent lock. New tables are initially empty, so their indexes require no existing-business-row rewrite. Recheck connection, prerequisites, history, protected baseline and long transactions immediately before applying; stop on an unexpected schema conflict rather than force or repair history.

## Scoped verification after approval

Use the existing72 contract checks inside `BEGIN`/`ROLLBACK`, with only a pre-fixture shop-midnight/quiet-window guard added. They cover actual-actor permission/isolation, explicit opt-in, types/schedules/CAS, fresh heads, claims/leases, uncertain and replayed handoffs, exact review, primary transfer and one daily reservation. All synthetic users/businesses/shops and records roll back; sequence gaps may remain because Postgres sequences are not transactional. No synthetic account persists and no real shop is enabled. No Android suite or repeated two-phone run belongs to this SQL-only publication.

Then compare the deployed reminder inventory/function bodies to DEV; verify grants/RLS/search paths, empty reminder state and protected existing row hashes; review only advisor deltas relevant to these new objects. The already accepted live two-phone story remains the client evidence. Supabase may assign PROD migration timestamps different from DEV; retain the actual versions and do not rename or repair history to make them match.

## Failure and rollback plan

Each source is a separate additive migration. If a call fails or times out, inspect actual migration history and object state before retrying; do not assume it failed or blindly rerun CREATE. If only the contract is installed and the actor-index step fails, keep shop activation/delivery closed and finish the exact reviewed indexes after diagnosis. No financial behavior is changed by the incomplete index step.

If verification fails after commit, do not activate a shop or deliver the new app. Prefer correcting the additive publication under a new reviewed change. Before any real reminder use, removal of the two new RPCs/private schema is a possible separately reviewed rollback after proving all six new tables empty and checking dependencies. Once metadata has been accepted, retain it; do not automatically DROP/CASCADE or silently resolve handoffs. Emergency RPC access revocation is also a separate explicit containment decision. This package authorizes neither destructive rollback nor deletion of business data.

## Judgment and retained scope

Current Supabase functions/API/RLS guidance and changelog were reviewed in the preceding accepted checkpoints; no SDK/TLS change is proposed. The Supabase and Postgres-best-practices skills support current grants, FK index coverage and short publication transactions. The hook is a reminder, not independent approval. Existing intentional private-RLS/no-policy and authenticated definer notices must be assessed against actual permissions and passing actor checks, not treated as proof of insecurity or ignored solely because tests pass.

The earlier production approval covered **Credit metadata**, not these new reminder migrations. This exact new PROD schema publication is the pending approval boundary already recorded in the plan. After it is approved and verified, signed ordinary delivery remains phase1; C's automatic-SMS decision, staff/private acceptance, independent key/backup custody and deferred clerk wording remain open.
