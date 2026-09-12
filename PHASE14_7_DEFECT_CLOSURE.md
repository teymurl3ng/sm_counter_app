# Phase 14.7 — Defect closure and complete regression

Status: **COMPLETE — defects, complete regression, and hands-on Quick unlock boundaries closed**

Date: 2026-09-12

Devices: Samsung SM-S921W / Android 16 / API 36 / 1080 × 2340 / density 480; Samsung SM-A065F / Android 16 / 720 × 1600 evidence capture

User-facing QA package: `com.saaf.milkshop.qa`

Instrumentation target: `com.saaf.milkshop.qa.tests`

## Closed implementation findings

- F14.3-01: five-minute windows for legitimate picker/provider interactions plus cloud-status reconciliation on return to the app.
- F14.3-02: safe offline and queued-retry guidance instead of raw DNS errors.
- F14.3-03: explicit boundaries between the Saaf owner passphrase, Google authentication, and the separate cloud recovery passphrase, including reconnect/new-vault behavior.
- F14.4-01: precise owner-reauthentication failure text while preserving generic login errors.
- F14.4-02: native, permission-filtered original transaction inputs and a prefilled correction form with an original-values summary.
- F14.5-01 through F14.5-06: 200% text reflow, compact landscape, usable IME viewport, history-aware Back with dirty-form confirmation, visible semantic dialog Close, and rotation retention without WebView replacement.
- F14.6-03: Access mutations reveal their success notice in the retained viewport.
- F14.7-01: replacement-phone cloud inspection now treats a different local vault as a missing local key and offers the portable recovery-passphrase path instead of stopping with `The cloud backup belongs to a different backup vault`.
- F14.7-02: Quick unlock enrollment starts with both methods off and identifies the available Android capability accurately. A phone with a screen PIN but no enrolled biometric now says `Phone security`; it no longer preselects that method or claims a fingerprint exists.

The fixes preserve Room/Kotlin authority. The UI does not calculate or reconstruct correction values, authorize itself, store secrets, or duplicate business state.

## Verification

- Bridge contract: v5, 91 endpoints, SHA-256 `50CBB9E0DD21B703EBEB400DBF415BFD259A3A14DC1C0A5EF73D9EC056ECCBBA`.
- TypeScript check, Vite production packaging, source cleanup, and packaged cleanup: pass.
- Kotlin production and Android-test compilation: pass.
- Focused Phase 14 Android behavior matrix: 12/12 pass.
- Ordinary connected Android suite: 273/273 pass, zero skipped/failures.
- Blank-install full visual capture/audit: 1/1 pass.
- Host unit tests: 7/7 pass.
- The backup WebView regression now supplies the required second strong approval on final replacement. All five backup bridge tests pass.
- The focused cloud-cryptography suite passes 6/6, including a regression that proves a clean replacement vault falls back to portable recovery and decrypts only with the correct recovery passphrase.

Instrumentation now has its own `.qa.tests` application identity. Gradle may install and remove that test target without clearing the user-facing `.qa` database on later runs.

## Final QA artifact

- Application ID: `com.saaf.milkshop.qa`
- OAuth client presence gate: pass; the configured `saaf.googleWebClientId` was recovered from the existing installed OAuth-enabled QA package in memory and was never printed or written into tracked source.
- APK: `phase18/app/build/outputs/apk/debug/app-debug.apk`
- Size: 16,174,564 bytes
- SHA-256: `5581218C0761ED3C33CA9D09877D19E4D2654B5B6DF5AC2790C80B364F223A24`
- Install/launch: pass as a data-preserving update on the S24 and A06; the corrected Quick unlock flow was exercised on the A06.
- Final screen: clean first-owner setup, recorded as `phase14-evidence/sm-s921w-android16-phase14-7-closure/01-final-qa-first-owner.png`.

The earlier connected-test route installed over and then removed the disposable `.qa` target before the dedicated test identity existed. That clean first-owner state is preserved in the evidence, but subsequent acceptance intentionally created QA data and restored the S24 recovery point on the A06. Later connected tests use `.qa.tests` and did not clear either user-facing `.qa` database. Remote encrypted Drive recovery points were not removed. The ordinary `com.saaf.milkshop` identity was not targeted.

The first final-package attempt selected a different Google OAuth client from three historical identifiers instead of the Web client property. Both fingerprint and full-passphrase approval therefore reached the same generic Google authorization failure. The package was rebuilt with the single exact historical Web client property and installed as a data-preserving update. The user then confirmed that initialization/data-path selection, Quick unlock re-enrollment, Drive use, and the TalkBack dialog Close check all pass. `02-steps1-4-complete.png` records the resulting signed-in QA state without credentials or account identifiers.

Shared-phone account selection also passes. Two Saaf identities were enrolled with separate Quick PINs, each PIN opened only its own identity, and deliberately entering each user's PIN for the other identity produced the expected generic verification error. This confirms both positive account switching and negative cross-account PIN isolation by direct user report; no PIN or passphrase was recorded.

## Final hands-on acceptance

The A06 was cleared only for `com.saaf.milkshop.qa`, relaunched as an independent first-owner installation, and showed no S24 Quick unlock identity or method. After its own owner connected the same Google test account, the S24 recovery point was visible. Its first security inspection exposed F14.7-01: the app stopped at a local-vault mismatch instead of asking for the portable cloud recovery passphrase. The typed failure was fixed, the 6/6 regression passed, and the corrected OAuth-enabled APK was installed without clearing either QA installation.

The user then repeated the A06 inspection. It required the separate cloud recovery passphrase, passed the recovery point's security check, and restored the S24 7:05 recovery point. The restored S24 owner account opened on the A06, while the S24 Quick unlock enrollment remained absent because that state is device-local.

The A06 initially had no secure Android screen lock, so Quick unlock was correctly unavailable after restore. The user added an Android screen PIN. This exposed F14.7-02: phone security was preselected and the generic label implied a fingerprint even though the A06 had no fingerprint enrolled. After the fix, both choices start off, the capability is called `Phone security`, Quick PIN-only setup/login passes, and Phone-security-only setup/login passes. The two screenshots under `phase14-evidence/sm-a065f-android16-phase14-7-quick-unlock-choice/` record the corrected copy and phone-security-only state; the PIN-only and both matching login results are direct user reports. No credential was recorded.

Removing or changing the secure screen lock after enrollment and changing biometric enrollment were not performed. The corresponding Keystore invalidation paths pass automated coverage and this remains the sole non-invasive physical-testing limitation. It does not block Phase 14 closure.

Phase 15 shopkeeper QA may begin.
