# Phase 14.7 — Defect closure and complete regression

Status: **IMPLEMENTATION AND AUTOMATED REGRESSION COMPLETE — final hands-on confirmation remains**

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

The fixes preserve Room/Kotlin authority. The UI does not calculate or reconstruct correction values, authorize itself, store secrets, or duplicate business state.

## Verification

- Bridge contract: v5, 91 endpoints, SHA-256 `50CBB9E0DD21B703EBEB400DBF415BFD259A3A14DC1C0A5EF73D9EC056ECCBBA`.
- TypeScript check, Vite production packaging, source cleanup, and packaged cleanup: pass.
- Kotlin production and Android-test compilation: pass.
- Focused Phase 14 Android behavior matrix: 11/11 pass.
- Ordinary connected Android suite: 271/271 pass, zero skipped/failures.
- Blank-install full visual capture/audit: 1/1 pass.
- Host unit tests: 7/7 pass.
- The backup WebView regression now supplies the required second strong approval on final replacement. All five backup bridge tests pass.

Instrumentation now has its own `.qa.tests` application identity. Gradle may install and remove that test target without clearing the user-facing `.qa` database on later runs.

## Final QA artifact

- Application ID: `com.saaf.milkshop.qa`
- OAuth client presence gate: pass; the identifier was recovered from local traces and never printed or written into tracked source.
- APK: `phase18/app/build/outputs/apk/debug/app-debug.apk`
- Size: 16,174,596 bytes
- SHA-256: `4BEAD85632975B253E870463E271D781944FC6BE1BDE05F64C2BF26F5915E491`
- Install/launch: pass on wired S24; cold launch completed in 1.74 seconds.
- Final screen: clean first-owner setup, recorded as `phase14-evidence/sm-s921w-android16-phase14-7-closure/01-final-qa-first-owner.png`.

The earlier connected-test route installed over and then removed the disposable `.qa` target before the dedicated test identity existed. The final `.qa` installation is therefore clean and has no prior local owner, data, cloud authorization, or Quick unlock enrollment. Remote encrypted Drive recovery points were not removed. The ordinary `com.saaf.milkshop` identity was not targeted.

## Remaining hands-on acceptance

1. Privately create the first owner in the clean QA app and save the one-time recovery code.
2. Recheck the corrected dialog Close button using TalkBack swipe focus and double-tap.
3. If explicitly approved, exercise secure-lock/biometric-change invalidation; this changes phone security settings and is never performed automatically.
4. Confirm that a cleared/replacement QA identity cannot use another phone's Quick PIN to open a cloud recovery point. The native and automated cryptographic boundary already passes; this item is the remaining physical corroboration.

Phase 15 shopkeeper QA should start only after those physical confirmations are either completed or explicitly accepted as documented limitations.
