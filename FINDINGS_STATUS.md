# AUDIT FINDINGS STATUS — living ledger (started 2026-10-05)

Source: `PROJECT_AUDIT_2026-10-02.md` (2 Critical · 12 High · 27 Medium · 20 Low/Info + appendices).
This file tracks what happened to each finding. Rule: update the row; never delete it.
Context: C1 fix program merged to main (852a38b → bd03f2e, tag c1-go8-merged); tier list in `BEFORE_PILOT_DECISIONS.md`.

## CRITICAL

- **C1 · sync silently erases unpushed edits — FIXED & PRODUCTION-VERIFIED (2026-10-03).** A′ migration (§6+§7, canonical sync_push hash cc681db1) live on PROD+DEV; client got parking, pending-edit guard, structural ack-equality, push-before-pull; A06 pulled updated=16 rows S24 pushed after it had pulled them; quiescence 0/0. Merge 852a38b; design doc `SYNC_GLOBAL_SEQ_C1_MIGRATION.md`.
- **C2 · no CI, zero JVM tests on money logic — OPEN (partially advanced).** Now: sync/ has real JVM tests (JsonStructural, SyncRebaselineDecisions, SyncHoldLabels, SyncOutboxBackupCodec, SeedDeterminism); qaFast host gate green. Still: no CI config anywhere; Costing/DailyClose/Correction/Sale remain device-only. Tier: before-pilot (GitHub Actions + Room3-JVM is feasible per audit).

## HIGH

- **H1 · refresh wipes session on transient Unknown — OPEN (frozen-spec violation, §5 "UNKNOWN never revoke").** `SyncRuntime.refreshSession()` else-branch still `tokenStore.clear()` on ANY non-Allowed result. Fix shape: clear only on definitive results; Unknown keeps the session and returns false (retry later). Pre-pilot gate.
- **H2 · no mutex in sync/ — OPEN.** Concurrent drains race (start-up kick + worker + owner Sync-now + facade instances); losing refresher can delete the winner's fresh tokens (compounds H1). Fix shape: process-wide serialization of drain/pull (+ refresh under the same lock). Pre-pilot gate.
- **H3 · half-applied event commits — FIXED (go 2).** Per-record savepoints (93a877d), parse-first applyEvent (55c1a8c), CancellationException rethrow (f0ee68e).
- **H4 · BusinessDateGuard wired only on the hybrid path — OPEN.** Native facades take unguarded `BusinessClock.today()`; a wrong clock posts any date and it syncs. Pre-pilot.
- **H5 · offline-grace blocks reads AND sign-in — OPEN (frozen-spec violation, §6 "writes blocked, reads allowed").** `Security.kt` grace gate hits every permission; login then requires VIEW_OPERATIONAL. Needs a read/write split at the permission gate. Pre-pilot gate.
- **H6 · Room plaintext at rest — OPEN, OWNER DECISION.** Accept-and-write-down vs SQLCipher; nothing records it as accepted risk either way.
- **H7 · Delivery Surcharge unreachable in native — OPEN.** Domain implements it; native sale facade + UI have no surcharge fields. The capability silently dies at 14.8.6 cutover unless built. Pre-pilot.
- **H8 · skipped-close sealing (earlier never-closed days) — OPEN, OWNER DECISION.** Same decision cluster as the tier list's "offline-authored events on closed days".
- **H9 · close with pending deliveries freezes them — OPEN.** Close-blocker warning; same decision cluster as H8.
- **H10 · close_actions enqueued before cash_counts — FIXED (via M11 fix, 5b7a8c1).** Apply now parks on unresolved refs instead of nulling count_uid, so links resolve on a later pass; the enqueue order itself is still inverted (cosmetic once parking handles it).
- **H11 · dark-mode contrast on money banners — OPEN.** Two call sites (OwnerOverview attention banner, Reports complete banner ≈1.4:1). Small; pre-pilot gate.
- **H12 · demo-crossfade parallel line — RESOLVED.** ui-touchups-anims-dark merged to main (b0c2d7c); go-8 merged (bd03f2e); demo-crossfade verified superseded (its 6 commits are intermediate steps of work that landed on main) — deletable on owner word.

## MEDIUM

**Sync & data:** M1 qaFast inventory drift — FIXED (0e4e370; go-1 retracted the finding). M2 lint 4 errors — OPEN (only host-gate blocker). M3 inventory-gate nondeterminism — OPEN. M4 apply UPDATE bypasses CHECK-class validation — OPEN. M5 planning-only restore re-enqueues — OPEN (impact reshaped by the classified re-baseline; re-verify). M6 planning pushed but never applied — OPEN (roadmap decision: when does planning sync land). M7 push-halt rate-limit dead code — OPEN. M8 malformed record aborts pull + no auto-retry — OPEN. M9 enrollment role check — FIXED (58517e6, owner-only) and the admin enrollment surface silenced (go-8 leg 2b). M10 deletes never captured — OPEN (upsert-only invariant unenforced). M11 unresolvable-reference posture — FIXED (5b7a8c1: park-on-unresolved + PRESENT semantics). M12 render-timezone vs business-date — OPEN (also the roadmap's optional "timezone abroad"). M13 audit inserts runCatching-wrapped — OPEN. M14 no crash reporting — OPEN.

**Money & domain:** M15 Double rounding inconsistencies — OPEN (one converter + one tolerance policy needed). M16 "Today's sales" excludes surcharge — OPEN (gated by H7). M17 cashier holds CASH_COUNT_AND_CLOSE at service layer — OPEN (clusters with the revocation/credential-tier tier item). M18 no cash-transfer/owner-drawing event — OPEN, OWNER DECISION (product scope). M19 cashier activity omits own expenses — OPEN (small).

**UI & interaction:** M20 divergent load-failure panes (~21 screens) — OPEN (polish pass #2). M21 rails lose selected style — OPEN. M22 Manager/Admin More drop the refreshing state — OPEN. M23 Admin dead Sync route — OPEN (surface silenced in leg 2b; route/params still dead). M24 deliveries drafts no leave-guard — OPEN. M25 cashier blind cash-count unguarded + no haptic — OPEN. M26 Sync screen cold-fail has no retry — OPEN (status refresh exists; the failure pane lacks Try-again).

**Cutover & structural:** M27 notification taps open the retired WebView shell — OPEN (blocks 14.8.6 retirement gate; fold into cutover). M28 MainActivity + presentation/ dead weight — OPEN (scoping decision). M29 bridge/ not uniformly dead (8 live files) — OPEN (retirement is a refactor, not a delete; cutover cleanup). M30 879-line mega-state — OPEN (post-pilot). M31 manual DI cascade — OPEN (post-pilot). M32 toolchain no reproducibility guard — OPEN (cheap: verification metadata + pinned versions note). M33 keystore off-machine backup — FIXED IN PRACTICE (owner's Drive "My Drive/SaafMilk-backup", SHA-256 verified 2026-09-29) — residual: record it in the README the folder already has; disclosure risk note owed. M34 server session survives sign-out — OPEN (one doc divergence line + optional background-session throttle). M35 preview machinery behind runtime flag — OPEN (accepted-risk note; release-unreachable verified).

## LOW / INFO (20)

Mostly hygiene, open. Fixed as a side effect: `settings.updated_at` seed-stamp format (leg 4, 5b24a4a — deterministic epoch). The remaining items (unused imports, detekt signals, unreferenced tags/strings, stale APK/log cleanup, unindexed FKs, etc.) ride with lint cleanup (M2) and the polish passes.

## APPENDICES

- **A · Deliberate-design confirmations** — no action, ever-changing nothing.
- **B · Documentation discrepancies** — each is one decision; M34 is the live one.
- **C · Owner decision menu** — open: H6 (SQLCipher), H8/H9 (close semantics), M18 (cash-transfer event), plus tier-list items (revocation tier, ledger spec decisions).
- **D · Fix menu** — folded into the sequencing below.

## SEQUENCING (proposed 2026-10-05)

1. **Pre-pilot gate (small, spec violations and races first):** H1 + H2 (sync session safety) → H5 (grace read/write split) → H11 (contrast) → H7 (surcharge) → H8/H9 owner decisions then build → M34 doc line. 
2. **C2 (CI + JVM domain tests)** — standing background task, starts alongside.
3. **Polish pass #2:** M20–M26 cluster (+ H4 guard wiring can ride here or with the gate).
4. **Folded into 14.8.6 cutover:** M27, M29 (+ H7 if not done before).
5. **Post-pilot / deliberate:** M28, M30, M31, M32, M35 notes, Low/Info hygiene.
