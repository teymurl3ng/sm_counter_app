# Phase 14 — Final Device QA

Status: **IN PROGRESS — checkpoints 14.1–14.2 complete; checkpoint 14.3 next (external OAuth/account prerequisite)**

## Purpose

Validate the finished hybrid application under real Android, role, accessibility, network, provider, and recovery conditions. Phase 14 is an acceptance and defect-finding phase, not another speculative UI overhaul or a place to add unapproved product scope.

## Locked boundaries

- Use the isolated `com.saaf.milkshop.qa` installation and dedicated QA records for destructive or failure-path testing. Do not overwrite an ordinary shop installation.
- Room and Kotlin remain the only business, validation, authorization, accounting, encryption, audit, backup, and recovery authority.
- React remains presentation-only and may not gain browser-local business state, duplicate formulas, synthetic reports, fake users, or self-authorized actions.
- Preserve bridge contract v4 and its 86 native-backed operations unless a separately approved additive capability passes its own design and regression gate.
- Backup is encrypted recovery, not synchronization between writable phones.
- Milk/Yogurt are the current validated workflows, not a permanent product limit and not evidence that unsupported products already have authoritative treatment.
- Judge the product proportionally as an owner-managed small-business tool. Do not claim enterprise ERP parity, statutory-accounting completeness, or external accessibility certification.
- Record verified defects with severity and evidence before fixing them. A missing capability or changed product meaning requires a separate decision rather than a silent QA-phase expansion.

## Ordered checkpoints

### 14.1 — Device-QA plan and clean baseline ✅

- Inventory connected hardware, display/accessibility state, isolated-package safety, OAuth configuration, second-device availability, and existing automated coverage.
- Rebuild and install only the isolated QA APK.
- Capture a clean first-run physical-device screenshot and accessibility-tree artifact.
- Lock the acceptance matrix, evidence method, severity scale, and prerequisites for later checkpoints.

Completed on Samsung SM-A065F / Android 16. The exact `com.saaf.milkshop.qa` APK rebuilt and installed after the bridge and cleanup gates passed; its SHA-256 remains `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C`. The clean first-owner screen is unobscured at 720 × 1600 with system dark mode enabled and normal text size. The Phase 13.5 258/258 device and 7/7 host regression is accepted as the unchanged entry baseline. Real Drive testing still needs a build-time Google OAuth web client ID and owner-controlled test account. A second Samsung SM-S921W / Android 16 is now connected, but it already contains the ordinary `com.saaf.milkshop` package and is not an approved destructive-QA target; nothing was installed or changed on it. See `PHASE14_1_DEVICE_QA_BASELINE.md`.

### 14.2 — Roles, permissions, sessions, and bypass attempts ✅

- Exercise owner, admin, manager, cashier, and operator journeys through the packaged hybrid UI.
- Verify hidden/disabled presentation states and independently attempt forbidden native bridge actions so UI hiding is never treated as authorization.
- Cover privileged-user administration, backup/restore restrictions, recent reauthentication, account disablement/role change, logout, session expiry, lifecycle recreation, and audit evidence.
- Add only targeted regression coverage when a real untested boundary is found.

Completed on the isolated Samsung SM-A065F / Android 16. Two new tests exercise all five roles through the packaged UI and valid native-bypass probes across every bridge-contract permission, plus the separate owner-only privileged-user and cloud boundaries. The focused matrix passes 2/2, the related security/session/backup/lifecycle/recovery regression passes 36/36, and host tests pass 7/7. No production defect was found, so the app UI, functions, formulas, and business behavior are unchanged. The full device-suite inventory is now 260 tests; its complete two-part rerun remains the 14.6 closure gate. See `PHASE14_2_ROLE_PERMISSION_QA.md`.

### 14.3 — Live Google Drive and resilience

- Build the isolated QA application with an approved Google OAuth web client ID supplied outside source control.
- Use an owner-controlled test Google account to prove consent, `drive.appdata` authorization, initial and manual encrypted upload, listing, inspection, retention, and disconnect/revocation.
- Exercise offline and metered-network behavior, missed scheduling, WorkManager retry, provider expiry, wrong-account handling, and safe degraded operation without blocking shop work.
- Keep credentials, tokens, recovery passphrases, and account-private data out of project evidence and logs.

### 14.4 — Second-device recovery

- Restore a real encrypted cloud recovery point on a second physical Android device using dedicated QA data.
- Verify users, roles, actor history, records, corrections, balances, stock, costing coverage, and post-restore session invalidation.
- Prove wrong-passphrase, damaged-backup, incompatible-backup, expired-ticket, repeated-attempt, and cancellation paths preserve the target installation.
- Confirm that two restored writable installations are not presented or operated as synchronized copies.

### 14.5 — Accessibility, adaptivity, and Android behavior

- Perform the native technical audit across accessibility, performance, appearance/theming, platform conformance, and adaptivity.
- Exercise TalkBack reading/focus order, labels, live feedback, error recovery, increased text, keyboard/IME, landscape, system bars, Android Back, lifecycle recreation, document pickers/sharing, and at least one additional device class where available.
- Score verified findings on the 0–4 audit scale and classify issues P0 blocking, P1 major, P2 minor, or P3 polish.
- Capture targeted evidence for findings and fixes; do not repeat all 83 screens without a material reason.

### 14.6 — Defect closure, complete regression, and Phase 15 handoff

- Resolve verified in-scope defects in bounded batches, preserving the frozen product and business boundaries.
- Rerun affected tests, build/contract/cleanup/hash gates, 7/7 host tests, and the complete 260-test two-part physical-device regression.
- Publish device/provider/recovery evidence, remaining limitations, and the guarded Phase 15 shopkeeper-QA handoff.

## Evidence policy

- Record device model, Android version, dimensions, configuration, exact action, expected result, observed result, severity, and artifact path.
- Never record Google passwords, access tokens, OAuth secrets, backup passphrases, recovery codes, real customer/shop data, or device account identifiers.
- A screenshot supports visual state; it does not prove native authorization, persistence, encryption, retry, or restore correctness by itself.
- Automated provider doubles remain useful regression evidence but cannot substitute for live Google Drive acceptance.
- Every destructive restore must use dedicated QA data and a known recoverable source copy.

## Current next action

Prepare checkpoint 14.3 without claiming live-provider acceptance: supply an approved Google OAuth web client ID outside source control and an owner-controlled test Google account, then run consent, upload, listing, retry/degraded-operation, and revocation tests on the isolated-QA Samsung. Checkpoint 14.4 has a connected candidate phone, but installing the separate QA package or performing recovery there requires an explicit safe-use decision because its ordinary Saaf installation must remain untouched.

## Plain English

The safe test app works on the primary Samsung, and every user role now sees only its approved work. Direct attempts to bypass the screens were also blocked by Android/native code. No app defect was found in this checkpoint. Next is real Google Drive testing, which needs a test OAuth setup and owner-controlled Google account. The second phone still has its ordinary Saaf app and will not be changed until separate QA use is confirmed.
