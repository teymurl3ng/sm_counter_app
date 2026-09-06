# Phase 14 — Final Device QA

Status: **IN PROGRESS — checkpoints 14.1–14.3 complete; checkpoint 14.4 replacement recovery is safely in progress**

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
- Secure device-local Quick unlock is now mandatory. It may replace repeated long-passphrase entry on a trusted phone, but a short PIN may never become the portable cloud-backup decryption secret. See `QUICK_UNLOCK_REQUIREMENT.md`.
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

Completed on the isolated Samsung SM-A065F / Android 16. Two new tests exercise all five roles through the packaged UI and valid native-bypass probes across every bridge-contract permission, plus the separate owner-only privileged-user and cloud boundaries. The focused matrix passes 2/2, the related security/session/backup/lifecycle/recovery regression passes 36/36, and host tests pass 7/7. No production defect was found, so the app UI, functions, formulas, and business behavior are unchanged. The full device-suite inventory is now 260 tests; its expanded complete two-part rerun remains the 14.7 closure gate. See `PHASE14_2_ROLE_PERMISSION_QA.md`.

### 14.3 — Live Google Drive and resilience ✅

- Build the isolated QA application with an approved Google OAuth web client ID supplied outside source control.
- Use an owner-controlled test Google account to prove consent, `drive.appdata` authorization, initial and manual encrypted upload, listing, inspection, retention, and disconnect/revocation.
- Exercise offline and metered-network behavior, missed scheduling, WorkManager retry, provider expiry, wrong-account handling, and safe degraded operation without blocking shop work.
- Keep credentials, tokens, recovery passphrases, and account-private data out of project evidence and logs.

Completed across the isolated Samsung SM-A065F and replacement Samsung SM-S921W, both on Android 16. The explicit `.qa` cloud resilience suite passes 25/25, including metered constraints, retention, wrong-account, authorization-required, retry, and recovery boundaries. The real provider passes consent, initial/manual encrypted upload, hidden app-data listing, offline queue/retry, successful no-restore inspection, account revocation, retained remote ciphertext, and fresh-consent reconnect. The replacement QA installation remains connected with six verified recovery points and no cloud error. Three UX defects are recorded for 14.7: interactive connect/disconnect can outlive the fixed bridge timer while succeeding, offline refresh exposes a raw DNS error, and credential/new-vault reconnect wording is ambiguous. See `PHASE14_3_LIVE_DRIVE_PREFLIGHT.md`.

### 14.4 — Second-device recovery

- Restore a real encrypted cloud recovery point on a second physical Android device using dedicated QA data.
- Verify users, roles, actor history, records, corrections, balances, stock, costing coverage, and post-restore session invalidation.
- Prove wrong-passphrase, damaged-backup, incompatible-backup, expired-ticket, repeated-attempt, and cancellation paths preserve the target installation.
- Confirm that two restored writable installations are not presented or operated as synchronized copies.

Live replacement now passes on the isolated Samsung SM-S921W: a remote point whose local vault key had been removed by revocation passed inspection, fully replaced the QA installation, forced logout, restored the owner account, retained six remote listings, and left Google Drive correctly paused. Wrong owner and cloud-recovery secrets were rejected without invoking restore. The original phone's three points remain encrypted because their original private recovery passphrase was unavailable, so honest cross-device source/data-rich proof and final post-failure continuity checks remain. One new copy defect is recorded for 14.7. See `PHASE14_4_SECOND_DEVICE_RECOVERY.md`.

### 14.5 — Accessibility, adaptivity, and Android behavior

- Perform the native technical audit across accessibility, performance, appearance/theming, platform conformance, and adaptivity.
- Exercise TalkBack reading/focus order, labels, live feedback, error recovery, increased text, keyboard/IME, landscape, system bars, Android Back, lifecycle recreation, document pickers/sharing, and at least one additional device class where available.
- Score verified findings on the 0–4 audit scale and classify issues P0 blocking, P1 major, P2 minor, or P3 polish.
- Capture targeted evidence for findings and fixes; do not repeat all 83 screens without a material reason.

### 14.6 — Mandatory Quick unlock implementation and device QA

- Add opt-in, native, device-bound Quick unlock after one full-passphrase enrollment.
- Prefer strong biometric or Android device credential; provide a rate-limited per-user four-digit-minimum PIN fallback, recommending six digits.
- Use Android Keystore protection and preserve native user/role authority, shared-device actor attribution, invalidation, lockout, cancellation, and full-passphrase fallback.
- Let routine sign-in, recent reauthentication, and existing-device cloud inspection use Quick unlock without exposing or repeatedly requesting long secrets.
- Prove that a short PIN cannot decrypt a portable cloud recovery point on a cleared or replacement device.
- Exercise all enrollment, unavailable, success, failure, lifecycle, accessibility, role/security-change, and recovery states defined in `QUICK_UNLOCK_REQUIREMENT.md` on physical devices.

### 14.7 — Defect closure, complete regression, and Phase 15 handoff

- Resolve verified in-scope defects in bounded batches, preserving the frozen product and business boundaries.
- Close all Quick unlock security/usability findings and rerun affected tests, build/contract/cleanup/hash gates, 7/7 host tests, and the expanded complete physical-device regression.
- Publish device/provider/recovery evidence, remaining limitations, and the guarded Phase 15 shopkeeper-QA handoff.

## Evidence policy

- Record device model, Android version, dimensions, configuration, exact action, expected result, observed result, severity, and artifact path.
- Never record Google passwords, access tokens, OAuth secrets, backup passphrases, recovery codes, real customer/shop data, or device account identifiers.
- A screenshot supports visual state; it does not prove native authorization, persistence, encryption, retry, or restore correctness by itself.
- Automated provider doubles remain useful regression evidence but cannot substitute for live Google Drive acceptance.
- Every destructive restore must use dedicated QA data and a known recoverable source copy.

## Current next action

Resume checkpoint 14.4 by confirming the failed wrong-cloud-passphrase inspection left the restored QA installation signed in with Google Drive paused. Then finish the remaining preservation checks and honest two-device/data-rich recovery evidence without touching either ordinary Saaf installation or weakening the forgotten-secret boundary. After 14.5, implement and physically verify mandatory Quick unlock in 14.6, then close every recorded defect and run the expanded complete regression in 14.7.

## Plain English

The real cloud restore fully replaced only the test app, logged the user out, restored the owner account, retained six encrypted points, and paused future backups. Wrong secrets were rejected, and another unclear error message was recorded for later repair. The oldest three backups remain securely unreadable without their forgotten original recovery secret. We paused safely before completing the remaining cross-device/data-rich proof.
