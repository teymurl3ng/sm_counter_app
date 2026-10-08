# REMAINING WORK — consolidated 2026-10-08

Single-source sweep of every tracking surface: FINDINGS_STATUS.md, BEFORE_PILOT_DECISIONS.md,
the 2026-10-02 "what is truly left" inventory, the transition/static-chrome program memory,
the Phase 14.9 program memory, and the 2026-10-08 decision batch. Stale items removed; done
items (C1, C2, H1–H13, H7, M2, M9, M11, M16, M18, M25–M26, M33-main, build SHA, push/CI, the
four owner decisions) are NOT repeated here. This file is the one place to look; update rows
in place, never delete.

## 1 · BEFORE THE PILOT (engineering)

- **Per-device revocation + lower-privilege credential tier + staff linking** — a lost counter
  phone currently carries OWNER credentials, and clerks exist per-phone only. One combined identity
  decision after the Track 2 evidence memo (devices table, device-scoped keys, per-clerk server
  identities with by-person revocation; SUSPENDED/REVOKED display wiring already shipped, enforcement
  flag documented off). M17 folds in.
  **NEW (2026-10-08, owner plan): the shop tablet will be CASHIER-ONLY and SHARED** — everyone
  records daily entries on it. That makes the lower-privilege tier REQUIRED before the tablet
  ships: a shared, publicly-accessible device must not carry an owner-level sync credential. The
  tablet also raises the shared-device question for the local account model (single device PIN +
  cashier role vs per-person passcodes). See IDENTITY_DECISION_MEMO.md tablet note.
  **TIER BUILT + VERIFIED (2026-10-09)**: enrollment accepts a cashier server account as a
  counter device; server account `counter` created (cashier, Shop 1) and verified live (sign-in
  200, sync_pull 200 with records, admin API 403 = least privilege proven). Credential: Desktop
  counter_cred.txt (owner stores it). Tablet setup when it arrives: install build → Sync → sign
  in as counter → pick Shop 1. Per-person Quick PINs decided for the tablet's local logins.
- **14.8.6 cutover finish** (the gate to the pilot week):
  1. M27 — notification taps open the retired WebView shell (MainActivity); BLOCKS the
     retirement gate. Point notifications at the native activity.
  2. Enrollment screen folded into the release app — bootstrap-on-first-launch + minimal
     enrollment (owner signaled "retire the WebView now"; formal confirmation of the revised
     order still owed, see §4).
  3. M29 — bridge/ retirement refactor (8 live files; a refactor, not a delete).
  4. Release-build regression connected run (the cutover run was lost and never re-run).
  5. Signed rebuild from current main (v0.4.0 predates ~200 commits incl. all audit fixes).
  6. S24 walkthrough: bootstrap → Sync → enroll PROD → Shop 1 → Sync now (also retires the
     DEV enrollment; Private DNS must stay dns.google).
  7. Pilot week.
- **Staff linking** (Account Bridge §6) — per-clerk credential proof so server-side D8 checks
  the acting clerk; SUSPENDED/REVOKED link enforcement is unimplemented; needs the enrollment
  UI extended + C1 admin gate.
- **M20 remainder** — ~14 role screens still use the cosmetic failure-pane variant
  (icon/title/centered); function identical, each is a small manual swap onto
  NativeLoadFailurePane. One focused pass.
- **M34** — server session survives sign-out: one doc divergence line + optional
  background-session throttle (was on the pre-pilot gate list).
- **Owed-debug test** — engine-path sync refusal under Robolectric is @Ignore'd
  (timing-flaky persistence; debug entry point in SyncApplyClosedDayRefusalTest KDoc).
  Device suite backstops it; un-park after a debug session.
- **OwnerBackupScreen back button** — the last surviving "Back to More" (5 hub-label sites in
  one file); the rest of the old back-button debt was cleaned by the T9–T13 sweep. Fold into
  the next UI touch-up.

## 2 · DEVICE / OPS (owner hands, mostly quick)

- S24: reconnect → unenroll DEV → re-enroll PROD (folds into the walkthrough above); verify no
  leftover qa.tests packages (`adb shell pm list packages | grep saaf`).
- Shop tablet (NEW, nothing installed): target role = CASHIER, shared by everyone for daily
  entries. Requires the lower-privilege sync credential tier first (§1). Shared-device local
  account model needs deciding (single device PIN vs per-person).
- A06: push current preview build (on 0.3.0-preview, product code identical); battery →
  Unrestricted; reconnect USB for the authoritative full instrumented re-run (stay-awake or
  unlocked; S24 triage found no product bug — wide-device shape debt + sleep cascade).
- Owner device checks of the Owner/Manager/Admin shells (H11 dark-mode visual check rides here).
- Decision: does the shop tablet replace the A06 as pilot device (restore-snapshot + enroll is
  the designed path)?
- Ops: PROD free-tier pause/quota policy re-check; DEV retirement timing.
- Release gotcha (standing): after a sandbox connected run, rebuild `assembleDebug` WITHOUT
  `-Psaaf.instrumentationSandbox=true` before shipping previews.

## 3 · SYNC/DATA + MONEY MEDIUMS (after pilot prep, before/split across pilot)

- M3 inventory-gate nondeterminism · M4 apply-UPDATE bypasses CHECK-class validation ·
  M5 planning-only restore re-enqueues (re-verify post re-baseline) · M6 planning pushed but
  never applied (needs the §4 timing decision) · M7 push-halt rate-limit dead code ·
  M8 malformed record aborts pull + no auto-retry · M10 deletes never captured (upsert-only
  invariant unenforced) · M12 render-timezone vs business-date · M13 audit inserts
  runCatching-swallowed · M14 no crash reporting · M15 one rounding converter + one tolerance
  policy · M19 cashier activity omits own expenses (small).

## 4 · OWNER DECISIONS STILL OWED (small, unblock work)

- Confirm the revised 14.8.6-first order (bootstrap-on-first-launch release app + enrollment
  screen folded into the cutover).
- Planning sync timing (unblocks M6).
- Shop-tablet-vs-A06 pilot decision; PROD-only timing (DEV stays for development either way).

## 5 · DURING THE PILOT

- Conflict-notification surface (the reviewer's tiering put it here).

## 6 · PARKED FEATURES / POST-PILOT

- **Phase 14.9 Credit & Reminders** — owner: MUST-DO. Blueprint now IN the repo:
  `PHASE14_9_CREDIT_REMINDERS_BLUEPRINT.md` (restored 2026-10-08 from the preserved session
  record after the original write was lost; numbering is 14.9 because Phase 15 = reserved
  Shopkeeper QA). Task menu T1–T13 inside. Deferred sub-items: WA Business API,
  JazzCash/EasyPaisa links, delivery auto-pause at limit, bulk reminders.
- M28 MainActivity dead-weight removal (scoping decision; M27 must land first).
- M30 879-line mega-state · M31 manual DI cascade · M32 toolchain reproducibility guard
  (verification metadata + pinned versions — cheap if wanted early) · M35 preview-machinery
  accepted-risk note.
- Low/Info hygiene (~20: unused imports, detekt signals, unindexed FKs, stale artifacts) —
  ride with cleanup passes.
- H8/H9 deferred item (9): "re-record as correction on an open day" from the preserved loser
  payload.
- BEFORE_PILOT later-list: sync_records_history sidecar (audit rows keep losers) · parked rows
  in backup (moot while restore resets cursor) · per-cashier attribution (server can't verify —
  fine for trusted pilot).
- Transition spec leftover: sign-in success → app is a hard cut (animating = role-switch work).
- Optional/deferred: timezone display abroad · time-trust warn/alert thresholds as settings ·
  approval-queue UI (BLOCKED — no domain API, do not invent).
- Housekeeping: if the GitHub repo ever goes public — review the hardcoded Supabase
  publishable keys against RLS posture and the two internal docs' visibility.

## 7 · VERIFICATION DEBTS (standing)

- A06 full instrumented run (see §2) · the parked owed-debug test (see §1) · H11 dark-mode
  owner check (see §2) · 236 lint warnings (hygiene bucket, non-gating).
