# BEFORE-PILOT DECISIONS — owner call list (updated 2026-10-03 after audit round 2)

Item 1 is now DECIDED in direction (reviewer b′, refined during verification) — it is a pilot gate, not a merge
gate. Everything below needs implementation gos before the real pilot devices ship.

## 1. Re-baseline policy — DECIDED: b′ + round-3 refinements; FULL SPEC AT `.zcode/audit-logs/go7-review/GO8-SPEC.md`
Round-3 additions all adopted: f3/f4 restores run the old push-first path with a fresh device_id (no new UI; f4
exposure is a days-wide bridge window — both phones take fresh f5 backups at the go-8 install); restored outbox
entries classified by server origin_device (own prev id → drop/server-wins; foreign → hold for owner decision);
epoch-mismatch is its own path (re-push missing rows, HOLD differing rows — server PITR/re-seed means devices hold
newer truth); device_id minted only at enrollment-after-restore/reset with prev_device_id carried in the f5 backup;
seeder determinism confirmed still required (capture triggers install BEFORE seeding — MilkShopDatabase :269→:270 —
so seed rows are genuinely pending entries). Only new UI in go 8: a minimal sync-review list on the owner Sync
screen (shared by restored-entry holds and epoch-mismatch holds).
→ Implementation package = go 8, on main, after the C1 merge, before any pilot phone leaves. Per-leg test: restore
an old backup after server-side edits; assert the server does not regress.

## 2. Seeder timestamps — AGREED, SCHEDULED
Fixed timestamps in `DatabaseSql.seedStatements` + a test that two fresh installs produce byte-identical seed rows.
Existing devices keep their stamps (one more self-converging churn each). Client-only; can ride with go 8.

## 3. Tiered deferred list (reviewer's tiering adopted)
**Before any pilot phone leaves:**
  - Item 1 package (b′ + backup-format bump + fresh device_id + restore-origin tag + epoch-with-cursor).
  - ~~The two ledger-correctness spec decisions~~ — **BOTH DECIDED (2026-10-08, owner):**
    - *Offline-authored events for a day another device closed:* **reopen-as-remedy is the spec.** A
      refused push halts at head (existing) and surfaces in the close-dock refusal count (built, H8/H9
      package); the remedy is to reopen the closed day on the closing device — which unblocks the
      refused rows so they drain — then re-close with a fresh count. NO auto-conversion to
      corrections; the "re-record as correction on an open day" item stays LATER/deferred.
    - *Unit/price changes vs historical events:* **events freeze their rates at write time — that is
      the spec.** `unit_rate`, `milk_rate_used`, `delivery_fee_used`, `ts_standard_used` are columns
      on the event row, so price edits never rewrite history and per-event reports/costing are
      already correct. A settings-change audit trail (who changed the price, when) is NOT built
      pre-pilot; deferred as a post-pilot nicety.
  - Per-device revocation + a lower-privilege credential tier (a lost counter phone currently carries owner
    credentials).
  - Build-embedded git SHA (cheap provenance).
**During the pilot:** the conflict-notification surface.
**Later:** sync_records_history sidecar (audit rows already keep losers) · parked rows in backup (moot while
restore resets the cursor to 0) · per-cashier attribution (events carry the actor locally but the server cannot
verify it — fine for a trusted pilot, worth knowing).

## 4. Accepted notes (documented, no action unless the owner wants one)
Seq burn on existing-row envelopes (benign, bigint) · append-only refusals not counted in 'applied' ·
S24's old preview identity disabled (pm enable restores) · DEV rows renumbered by the go-5 rehearsal.
(The former same-device-restore audit-gap note is now CLOSED by the fresh-device_id-at-enroll decision — kept
here only until go 8 lands.)
