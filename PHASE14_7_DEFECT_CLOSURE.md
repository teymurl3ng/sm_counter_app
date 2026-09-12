# Phase 14.7 — Defect closure and complete regression

Status: **COMPLETE — defects, complete regression, and hands-on Quick unlock boundaries closed**

Date: 2026-09-12

Device: Samsung SM-S921W / Android 16 / API 36 / 1080 × 2340 / density 480

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

The fixes preserve Room/Kotlin authority. The UI does not calculate or reconstruct correction values, authorize itself, store secrets, or duplicate business state.

## Verification

- Bridge contract: v5, 91 endpoints, SHA-256 `50CBB9E0DD21B703EBEB400DBF415BFD259A3A14DC1C0A5EF73D9EC056ECCBBA`.
- TypeScript check, Vite production packaging, source cleanup, and packaged cleanup: pass.
- Kotlin production and Android-test compilation: pass.
- Focused Phase 14 Android behavior matrix: 11/11 pass.
- Ordinary connected Android suite: 272/272 pass, zero skipped/failures.
- Blank-install full visual capture/audit: 1/1 pass.
- Host unit tests: 7/7 pass.
- The backup WebView regression now supplies the required second strong approval on final replacement. All five backup bridge tests pass.
- The focused cloud-cryptography suite passes 6/6, including a regression that proves a clean replacement vault falls back to portable recovery and decrypts only with the correct recovery passphrase.

Instrumentation now has its own `.qa.tests` application identity. Gradle may install and remove that test target without clearing the user-facing `.qa` database on later runs.

## Final QA artifact

- Application ID: `com.saaf.milkshop.qa`
- OAuth client presence gate: pass; the exact historical `saaf.googleWebClientId` build property was recovered from local traces and never printed or written into tracked source.
- APK: `phase18/app/build/outputs/apk/debug/app-debug.apk`
- Size: 16,174,596 bytes
- SHA-256: `9600CC68D5CB6E950DA137B51F7D53AB3FFF552ABAB55071AA9B8F330A8EA4DC`
- Install/launch: pass as a data-preserving update on the S24 and A06; the recorded clean S24 cold launch completed in 1.74 seconds.
- Final screen: clean first-owner setup, recorded as `phase14-evidence/sm-s921w-android16-phase14-7-closure/01-final-qa-first-owner.png`.

The earlier connected-test route installed over and then removed the disposable `.qa` target before the dedicated test identity existed. The final `.qa` installation is therefore clean and has no prior local owner, data, cloud authorization, or Quick unlock enrollment. Remote encrypted Drive recovery points were not removed. The ordinary `com.saaf.milkshop` identity was not targeted.

The first final-package attempt selected a different Google OAuth client from three historical identifiers instead of the Web client property. Both fingerprint and full-passphrase approval therefore reached the same generic Google authorization failure. The package was rebuilt with the single exact historical Web client property and installed as a data-preserving update. The user then confirmed that initialization/data-path selection, Quick unlock re-enrollment, Drive use, and the TalkBack dialog Close check all pass. `02-steps1-4-complete.png` records the resulting signed-in QA state without credentials or account identifiers.

Shared-phone account selection also passes. Two Saaf identities were enrolled with separate Quick PINs, each PIN opened only its own identity, and deliberately entering each user's PIN for the other identity produced the expected generic verification error. This confirms both positive account switching and negative cross-account PIN isolation by direct user report; no PIN or passphrase was recorded.

## Final hands-on acceptance

The A06 was cleared only for `com.saaf.milkshop.qa`, relaunched as an independent first-owner installation, and showed no S24 Quick unlock identity or method. After its own owner connected the same Google test account, the S24 recovery point was visible. Its first security inspection exposed F14.7-01: the app stopped at a local-vault mismatch instead of asking for the portable cloud recovery passphrase. The typed failure was fixed, the 6/6 regression passed, and the corrected OAuth-enabled APK was installed without clearing either QA installation.

The user then repeated the A06 inspection. It required the separate cloud recovery passphrase and passed the recovery point's security check. No restore was performed. Because the clean A06 offered no S24 Quick PIN route at all, entering an S24 PIN into the unrelated recovery-passphrase field would not add evidence; the device-local PIN boundary is physically closed.

Changing the secure screen lock or biometric enrollment on a personal phone was intentionally not performed. The corresponding Keystore invalidation paths pass automated coverage and this is accepted as the sole non-invasive physical-testing limitation. It does not block Phase 14 closure.

Phase 15 shopkeeper QA may begin.
