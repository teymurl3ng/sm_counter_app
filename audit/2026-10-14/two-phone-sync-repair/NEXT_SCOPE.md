# Next bounded unit: 9b1c event details, then the unchanged online exchange

The two previously scoped repairs are finished. The repeat progressed to its first full-table comparison and stopped on opening_batches, opening_account_entries and event_actor_links. Do not restart a general audit or weaken that comparison.

## Evidence and boundaries

Opening: both devices have the same OPENING_STATE event and financial movement, but the receiving phone lacks its opening batch and account-entry detail. SyncEnvelopeFactory does not embed opening details; the saved server payload contains none. Scope the OPENING_STATE envelope/receiver and its opening detail DAO queries only. Include inventory entries as part of this same event format, without claiming this cash-only fixture reproduced missing inventory details.

Actor snapshots: the server retains actor_link for both purchase and production, while the receiver has no event_actor_links rows. SyncApplyEngine attempts actor_user_id=0 and catches insertion failures; the schema requires that ID to reference a real device-local app user. This is a strong explanation from implementation, not an observed exception log. Reproduce that failure in one focused test before selecting the smallest valid representation for a remote, unlinked actor. Also inspect the existing-event early return which precedes actor repair. Keep name/role/time snapshots, avoid assigning another phone's integer user ID to a local person, and preserve the existing local actor/authentication rules. If a schema/codec change is required, enumerate those exact files and migration/backup checks before editing; do not create fake privileged users or disable foreign keys.

## Finish condition and selected checks

One focused cross-device OPENING_STATE round-trip/replay case with different local IDs, cash and inventory detail, parent-first dependency refusal, and unchanged ledger totals. One focused unlinked remote actor insert/replay/repair case preserving name/role/time, without a fabricated local login or changing local actor policy. Include only existing opening/actor/migration/backup checks touched by the selected implementation.

Then repeat the same fresh DEV A06 Owner/S24 Cashier exchange. Require all 18 normalized raw business tables equal at base, after the exact 2 L cash sale, and after repeat; retain literal Cash 10000 → 10380 and Milk 80 → 78, Yogurt 17. Preserve genuine pending work, server role gates and explicit seed review. Stop on any further mismatch and record it before expansion.

No offline/close/reopen/recovery extension, hosted identity/reset rollout, new staff identity system, full suite, ordinary phone installation, PROD change or release publication in this unit. 9b2/9b3, human acceptance and phases 4/6 remain separate.
