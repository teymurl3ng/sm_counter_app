# Phase 14 — Final Device QA

Status: **IN PROGRESS — checkpoints 14.1–14.6 complete; 14.7 defect code and complete regression pass, with final hands-on confirmation remaining**

## Purpose

Validate the finished hybrid application under real Android, role, accessibility, network, provider, and recovery conditions. Phase 14 is an acceptance and defect-finding phase, not another speculative UI overhaul or a place to add unapproved product scope.

## Locked boundaries

- Use the isolated `com.saaf.milkshop.qa` installation and dedicated QA records for destructive or failure-path testing. Do not overwrite an ordinary shop installation.
- Room and Kotlin remain the only business, validation, authorization, accounting, encryption, audit, backup, and recovery authority.
- React remains presentation-only and may not gain browser-local business state, duplicate formulas, synthetic reports, fake users, or self-authorized actions.
- Preserve bridge contract v5 and its 91 native-backed operations. V5 is the approved additive Quick unlock capability; later changes must pass the same manifest, generation, and regression gate.
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

### 14.4 — Second-device recovery ✅

- Restore a real encrypted cloud recovery point on a second physical Android device using dedicated QA data.
- Verify users, roles, actor history, records, corrections, balances, stock, costing coverage, and post-restore session invalidation.
- Prove wrong-passphrase, damaged-backup, incompatible-backup, expired-ticket, repeated-attempt, and cancellation paths preserve the target installation.
- Confirm that two restored writable installations are not presented or operated as synchronized copies.

Completed from Samsung SM-S921W source to a freshly reset isolated Samsung SM-A065F target. The exact 20:14 data-rich point passed inspection and full replacement; restore forced logout, recovered the source owner, retained ten remote listings, restored the fake product/supplier/customer/purchase/sale/correction plus expected stock and balances, preserved correction linkage and actor attribution, and left target Drive correctly paused. Wrong owner/cloud secrets preserved the installation, while the automated matrix retains damaged/incompatible/ticket/cancellation/repeated-attempt coverage. Two UX defects are recorded for 14.7, including a P1 correction form that hides the original values. See `PHASE14_4_SECOND_DEVICE_RECOVERY.md`.

### 14.5 — Accessibility, adaptivity, and Android behavior ✅

- Perform the native technical audit across accessibility, performance, appearance/theming, platform conformance, and adaptivity.
- Exercise TalkBack reading/focus order, labels, live feedback, error recovery, increased text, keyboard/IME, landscape, system bars, Android Back, lifecycle recreation, document pickers/sharing, and at least one additional device class where available.
- Score verified findings on the 0–4 audit scale and classify issues P0 blocking, P1 major, P2 minor, or P3 polish.
- Capture targeted evidence for findings and fixes; do not repeat all 83 screens without a material reason.

Completed on the isolated A06 and S24 QA installations. The targeted native audit scores 11/20: no P0 findings, five P1 findings, and one P2 finding. TalkBack generally announces controls clearly; app switching/locking retains drafts; S24 split-screen is usable; and Android document-picker cancellation returns safely, reports that no file changed, and clears passphrase fields. Major failures remain in large-text reflow, keyboard avoidance, safe Android Back/draft handling, TalkBack dialog-close activation, and rotation draft retention. A06 landscape is usable but needs broad refinement. The locked light theme remains intentionally unchanged when system dark mode changes. See `PHASE14_5_ACCESSIBILITY_ANDROID_QA.md`.

### 14.6 — Mandatory Quick unlock implementation and device QA

- Add opt-in, native, device-bound Quick unlock after one full-passphrase enrollment.
- Prefer strong biometric or Android device credential; provide a rate-limited per-user four-digit-minimum PIN fallback, recommending six digits.
- Use Android Keystore protection and preserve native user/role authority, shared-device actor attribution, invalidation, lockout, cancellation, and full-passphrase fallback.
- Let routine sign-in, recent reauthentication, and existing-device cloud inspection use Quick unlock without exposing or repeatedly requesting long secrets.
- Prove that a short PIN cannot decrypt a portable cloud recovery point on a cleared or replacement device.
- Exercise all enrollment, unavailable, success, failure, lifecycle, accessibility, role/security-change, and recovery states defined in `QUICK_UNLOCK_REQUIREMENT.md` on physical devices.

Implementation completed on 2026-09-09. The native device/Keystore boundary, per-user rate-limited PIN, shared-phone identity choice, strong-versus-PIN authorization split, existing-device cloud-key path, React presentation, bridge v5/91 projections, production UI build, packaged cleanup, Kotlin compilation, and debug APK assembly pass their host/build gates. The six focused tests pass 6/6 on the isolated S24, and combined enrollment plus separate Quick PIN and fingerprint/phone-security logins pass. Two blocking Android-thread/Keystore-order defects found during that smoke run were fixed. Extended misuse, recovery, lifecycle, and accessibility acceptance remains open. See `PHASE14_6_QUICK_UNLOCK_QA.md`.

### 14.7 — Defect closure, complete regression, and Phase 15 handoff

- Resolve verified in-scope defects in bounded batches, preserving the frozen product and business boundaries.
- Close all Quick unlock security/usability findings and rerun affected tests, build/contract/cleanup/hash gates, 7/7 host tests, and the expanded complete physical-device regression.
- Publish device/provider/recovery evidence, remaining limitations, and the guarded Phase 15 shopkeeper-QA handoff.

Implementation and automated regression completed on 2026-09-12. The eleven Phase 14.3–14.5 findings and the later F14.6-03 feedback finding have bounded fixes. The S24 passes 11/11 focused Android behavior tests, 271/271 ordinary instrumentation tests in the new `com.saaf.milkshop.qa.tests` sandbox, the separately configured 1/1 visual-capture audit, and 7/7 host tests. Contract v5/91, TypeScript, production Vite packaging, cleanup, Kotlin compilation, package identity, and OAuth-presence gates pass. The installed OAuth-enabled `com.saaf.milkshop.qa` APK is SHA-256 `4BEAD85632975B253E870463E271D781944FC6BE1BDE05F64C2BF26F5915E491`. See `PHASE14_7_DEFECT_CLOSURE.md`.

## Evidence policy

- Record device model, Android version, dimensions, configuration, exact action, expected result, observed result, severity, and artifact path.
- Never record Google passwords, access tokens, OAuth secrets, backup passphrases, recovery codes, real customer/shop data, or device account identifiers.
- A screenshot supports visual state; it does not prove native authorization, persistence, encryption, retry, or restore correctness by itself.
- Automated provider doubles remain useful regression evidence but cannot substitute for live Google Drive acceptance.
- Every destructive restore must use dedicated QA data and a known recoverable source copy.

## Current next action

The final S24 QA APK is installed and opens on a clean first-owner screen. Initialize only this disposable `.qa` identity with private credentials, save its recovery code, then perform the remaining physical TalkBack close-action check and any explicitly approved phone-security invalidation check. The new instrumentation target is `com.saaf.milkshop.qa.tests`, so future connected regressions no longer install over or remove the user-facing QA package.

## Plain English

The recorded cloud, recovery, Android behavior, and Quick unlock feedback defects are fixed, and all 272 Android checks plus seven host tests pass. The final QA build is installed on the S24. It is a fresh disposable QA database, so the next step is private first-owner setup followed by the last physical accessibility/security confirmations.
