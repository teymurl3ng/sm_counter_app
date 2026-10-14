# 9b1c repaired; physical online exchange accepted

The scoped opening-detail and remote-author losses are repaired. The unchanged fresh-fixture A06 Owner/S24 Cashier online story passed all three full-record checkpoints: base, the exact 2 L cash sale and duplicate-free repeat. Each compares all 18 normalized business tables, not just balances or sequence counters. Earlier failed attempts are retained separately and are not relabelled as successes.

## Verified boundaries and implementation

Before production edits, three selected A06 reproductions confirmed the exact boundaries: user ID 0 fails the actor foreign key; remote author snapshots are missing after replaying an older header; and an opening correction chain receives no detail batch. The first is a constraint proof and the other two were expected failing assertions.

SyncEnvelopeFactory now embeds version-1 opening_state full restated inventory/account entries using product/account UIDs. The receiver remaps those references, preserves correction deltas in the ledger, validates full details against cumulative opening movements and rejects repeated/negative/malformed or inconsistent entries atomically. An older identical header or matching detail subset can be repaired without another business movement; conflicting existing detail is not overwritten. Legacy envelopes lacking details remain readable; full detail repair requires a current complete envelope. Existing hosted history was not rewritten.

Remote author name/role/time snapshots now persist with actor_user_id=NULL. Integer user IDs from another device are never assigned to a local login. Existing local actor references remain intact; local Activity ownership rules are unchanged. Snapshot insertion errors are no longer silently swallowed, and replay reaches missing snapshots even when the header/ledger already exist. Differing immutable author snapshots are refused.

Schema 17 makes only event_actor_links.actor_user_id nullable. Migration 16→17 preserves existing rows, local links and both foreign keys. The encrypted backup codec explicitly preserves NULL for remote snapshots and still requires the reference field to be present. Fifteen historical exported schemas match Git HEAD, and all other entity/view definitions match schema 16. Six production files plus generated schema17 changed within the enumerated scope; local identity/authentication commands, role gates, historical migrations, UI and provider/server definitions did not change.

## Focused checks

Thirteen distinct selected methods are accepted: four new event/detail cases, migration preservation, mixed local/remote-author encrypted restore, two existing opening controls, two existing backup controls, local Activity ownership and two existing receiver controls. First run passed 11 while the new persistence class could not initialize because its migration method returned a Boolean. After that test declaration was corrected, encrypted checks passed; the migration fixture then exposed an omitted required schema-16 column. That fixture alone was corrected and its one method rerun successfully. The backup parser was tightened to distinguish explicit NULL from a missing field, with the affected backup methods rerun. No expectation was changed to match app output, and no full suite ran. Exact selected-method evidence and source/APK hashes are retained in integrity.json and source-final.json.

## Physical hosted acceptance

Run 8474bf480197; DEV shop b4d5f170-6c0f-41b7-91b1-496fb665b994. A06 used USB; S24 used its observed wireless ADB endpoint. Distinct device identities and deliberately different local product/account/event/planning IDs were verified. Both devices use private run-specific databases in com.saaf.milkshop.qa.tests and real DEV Auth/HTTPS transport.

Owner published 30 initial records. Fresh Counter reconciled its 19 real captures with zero forbidden master upload and explicitly took the server's six differing settings through the existing review path. Base: Cash PKR 10000, supplier PKR 17576.92, customer 0, Milk 80 L and Yogurt 17 kg. Counter then recorded and published one authenticated Cashier sale for 2 L at PKR190/L. Both devices reached Cash PKR10380 and Milk78 L; other expected balances/stock stayed unchanged. All 18 normalized tables match at base, sale and repeat; the repeat has identical hashes, four event headers and three author snapshots, with no extra movements. Every exchange ends with zero queue/holds/parks and no skipped/conflicted records. These are actual two-phone results, not a fake HTTP/simulator substitute.

Cross-device local actor integers are excluded from comparison because users are device-local; author name/role/time snapshots are compared. This does not establish central actor identity or revocation, shared costing-policy convergence, offline/close/reopen or interrupted recovery. Those existing gates remain separately scoped.

## Containment and release limits

Both disposable sessions logged out (204) and their run-specific transport files cleared. Both created Auth accounts were retired via the existing supported termination command; the exact fixture shop is inactive. A field-only post-check confirms two terminated/inactive staff rows with cleared Auth references, 31 retained synthetic sync records, three server author snapshots and one opening snapshot. Existing DEV staff51/shops4/sync-records66 have identical before/after hashes. Non-test ordinary/preview/.qa package versions/install timestamps on both phones match before/after; only isolated QA APKs were installed. No PROD, real staff credentials, ordinary data, provider schema/RPC, commit/push or signing/publication changed.

Matched upgraded builds are required for complete new detail transport. New backups containing unlinked snapshots should be restored with the updated app; older-reader/downgrade acceptance is not claimed. Schema17 upgrade preservation is verified within the migration test; recovered signing-key and actual ordinary release upgrade/rollback remain phase6.

## Whole plan

1. ~~Money, rounding and withdrawal remedies~~ — bounded QA complete.
2. ~~Profile/planning receiver and catch-up, including app-generated dates~~ — bounded QA and current online profile/planning comparisons accepted.
3. ~~Mapped menu/list presentation~~ — bounded QA complete.
4. ~~Backup automation, local app restore, access wording/checks and fresh Counter seed reconciliation~~; hosted identity recovery, reset browser/production and actual shop-device handover remain.
5. ~~Independent day oracle, automated shop day, seven representative phone checks and physical online two-phone exchange (9b1)~~; offline/closing/reopening (9b2), interrupted/retried recovery (9b3), uncoached staff and remaining human phone checks remain.
6. Freeze, commit/push, recovered signing-key proof, signed ordinary upgrade/install and record-preservation check remain.

Next only narrow 9b2's offline-write and closing scenario against the existing role contract. Keep 9b3, hosted gates, human acceptance and release preparation separate; no new audit or design work.
