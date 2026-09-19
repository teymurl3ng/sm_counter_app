# Phase 14.8.3 — Compose foundation

Status: **IN PROGRESS — native authentication including Quick PIN runs on A06; phone-security prompt review and service-bound Sale are next**

Date started: 2026-09-19

## Authorized checkpoint

The owner accepted the refined Counter Ledger preview after the final quantity-unit and entry-label corrections and authorized implementation. This checkpoint introduces Compose beside the verified hybrid fallback. It does not change the launcher or claim that authentication and Sale writes are native-owned.

## Implemented

- enabled the Kotlin Compose compiler and an Android API 36-compatible stable Compose dependency set;
- retained AGP 9.0.1, `compileSdk 36`, `targetSdk 36`, the existing Room/security layers, and the packaged hybrid fallback;
- added Counter Ledger light and dark color roles, Material typography roles, spacing through Compose layout primitives, and 48 dp-or-larger interactive targets;
- bundled explicit Manrope Regular, Medium, Semibold, Bold, and ExtraBold files plus the OFL license so Android does not fall back to a thin variable-font instance;
- added `NativePreviewActivity` as a non-exported main-manifest activity, exposed only in debug builds for device review;
- recreated the accepted branded Welcome and Cashier New sale surfaces in Compose;
- made the quantity editable with 0.5-unit steppers, used `Amount` for value entry, aligned the unit to the numeral baseline, kept compact paid-now/customer selectors, and kept the review plus Record action visible above navigation;
- completed the preview interaction loop: switching between Quantity and Amount converts the field at the fixture rate, direct numeric edits immediately recalculate quantity/total/stock/money effects, and Customer account opens an account picker before showing a customer-balance consequence;
- adopted `NATIVE_INTERACTION_CONTRACT.md` as a required gate: every visible control needs a described workflow, stable test tag, real action, state assertion, and visible-result assertion;
- removed remaining silent preview taps: secondary details now expands/collapses, customer selection has defined Cancel/change behavior, and unfinished navigation destinations show explicit temporary notices;
- implemented Android Back from the Cashier preview to Welcome and a clear non-writing snackbar when the preview Record action is pressed;
- added a presentation-neutral typed authentication facade over Room and `UserSecurityService`; Compose does not call the JSON bridge;
- added native startup loading/retry, returning-account selection, selected-identity passphrase sign-in, first-Owner creation, one-time recovery-code acknowledgment, and explicit validation/error states;
- kept passphrases out of navigation and saved state, cleared field state on submission, zeroed submitted `CharArray` values, and carried only a memory-only session handle into the authenticated preview;
- made system Back and visible Back behavior explicit at every access stage; leaving Cashier invalidates the native session before returning to Welcome;
- added account-specific native method selection backed by the existing encrypted Quick unlock records, with Quick PIN, Android phone security, and the always-available full-passphrase route shown only when applicable;
- connected Quick PIN to the real per-account security service, including cross-account rejection, immediate corrected-PIN retry, delay/full-passphrase recovery, and full-passphrase reset behavior;
- connected Android phone security through the Activity-scoped `BiometricPrompt` device gate, using `Phone security` unless a strong biometric is actually available and never claiming a fingerprint that Android has not enrolled;
- left `HybridActivity` as the only launcher and left all database, security, bridge, and business behavior unchanged.

## Compatibility decision

The current stable Activity 1.13/Lifecycle 2.11 Compose path requires Android API 37 and AGP 9.1. This verified application is on Android API 36 and AGP 9.0.1. The foundation therefore uses Activity 1.12.4, Lifecycle 2.10.0, Compose BOM 2025.12.00, and Compose compiler 2.2.10. This avoids a toolchain migration inside the first presentation checkpoint.

## Verification

- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -Psaaf.instrumentationSandbox=true` — passed;
- `:app:assembleDebug -Psaaf.isolatedQa=true` — passed;
- `NativeOwnerBootstrapUiTest` on wired A06 `SM-A065F` — 1/1 passed on a fresh isolated installation, including disabled submit, visible/system Back, mismatch rejection, Owner creation, recovery-code retention, acknowledgment, and entry to Cashier;
- `NativeFoundationUiTest` on wired A06 `SM-A065F` — 12/12 passed, covering Welcome, account and method selection, blank/invalid/retried/valid passphrase and Quick PIN states, cross-account PIN rejection, every Back route, session invalidation, and every current Cashier interaction;
- `NativeAuthenticationFacadeTest` and `NativeAuthenticationScreenContractTest` — 5/5 passed, covering typed sessions, secret zeroing, active-account filtering, startup retry, enrolled-method visibility, phone-security cancellation, and rate-limited full-passphrase recovery;
- `Phase14QuickUnlockTest` — 10/10 passed, including encrypted persistence, immediate corrected-PIN retry, PIN delay/full-passphrase reset, phone-security strength, invalidation, and concurrent-attempt/removal races;
- combined native authentication checkpoint on the wired A06 — **28/28 passed, 0 failed, 0 skipped**;
- isolated package installed as `com.saaf.milkshop.qa`; ordinary Saaf package was not touched;
- real-device Welcome evidence: `phase14-evidence/saaf-native-welcome-a06.png`;
- real-device Cashier evidence: `phase14-evidence/saaf-native-cashier-a06.png`;
- the A06 render shows the full normal Cashier path, review, commit action, and four-item navigation in one viewport.

## Current boundary

This is a service-connected authentication slice inside the debug-native harness, not a completed application migration. Account discovery, first-Owner creation, full-passphrase verification, recovery acknowledgment, Quick PIN, Android phone-security invocation, and session boundaries use the real native security and Room authority. Quick PIN is automated on the A06; the actual Android phone-security prompt still needs one manual native-screen review. Date, products, rates, customer accounts, and sale effects remain presentation fixtures; Record sale deliberately writes nothing. The native activity is not reachable from the production launcher.

## Next checkpoint

Continue the authentication-to-sale slice in this order:

1. manually review the real Android phone-security prompt, cancellation, successful return, and selected-account identity in the isolated native QA activity;
2. bind Cashier reference data and Sale commands through the existing typed application/domain boundary;
3. replace fixture totals and effects with authoritative preview state, then implement saving, success, error, refresh, Back/draft, and restoration behavior;
4. prove the Room event, stock/cash effect, audit actor, session expiry, large text, IME, TalkBack, and process recreation before changing launcher ownership.
