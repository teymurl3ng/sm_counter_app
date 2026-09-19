# Phase 14.8.3 — Compose foundation

Status: **IN PROGRESS — native authentication and the authoritative native Sale vertical slice are verified on A06**

Date started: 2026-09-19

## Authorized checkpoint

The owner accepted the refined Counter Ledger preview after the final quantity-unit and entry-label corrections and authorized implementation. This checkpoint introduces Compose beside the verified hybrid fallback. It does not change launcher ownership.

## Implemented

- enabled the Kotlin Compose compiler and an Android API 36-compatible stable Compose dependency set;
- retained AGP 9.0.1, `compileSdk 36`, `targetSdk 36`, the existing Room/security layers, and the packaged hybrid fallback;
- added Counter Ledger light and dark color roles, Material typography roles, spacing through Compose layout primitives, and 48 dp-or-larger interactive targets;
- bundled explicit Manrope Regular, Medium, Semibold, Bold, and ExtraBold files plus the OFL license so Android does not fall back to a thin variable-font instance;
- added `NativePreviewActivity` as a non-exported main-manifest activity, exposed only in debug builds for device review;
- recreated the accepted branded Welcome and Cashier New sale surfaces in Compose;
- made the quantity editable with 0.5-unit steppers, used `Amount` for value entry, aligned the unit to the numeral baseline, kept compact paid-now/customer selectors, and kept the review plus Record action visible above navigation;
- completed the interaction loop: switching between Quantity and Amount converts the field at the stored retail rate, direct numeric edits immediately recalculate quantity/total/stock/money effects, and Customer account opens the real active-customer picker before showing a customer-balance consequence;
- adopted `NATIVE_INTERACTION_CONTRACT.md` as a required gate: every visible control needs a described workflow, stable test tag, real action, state assertion, and visible-result assertion;
- removed remaining silent preview taps: secondary details now expands/collapses, customer selection has defined Cancel/change behavior, and unfinished navigation destinations show explicit temporary notices;
- implemented Android Back from an untouched Cashier screen to Welcome and an explicit keep/discard checkpoint for an edited sale draft;
- added a typed native Sale facade and ViewModel over the existing permission-aware read models and authoritative transaction command; Compose does not pass JSON;
- replaced the fixture identity, date, open/closed state, products, retail rates, stock, customers, and cash account with Room-backed values;
- connected Record sale to the real atomic SALE write, including saving lockout, numbered success receipt, refreshed stock/read models, draft reset after success, persistent error feedback, and draft preservation after rejection;
- added a presentation-neutral typed authentication facade over Room and `UserSecurityService`; Compose does not call the JSON bridge;
- added native startup loading/retry, returning-account selection, selected-identity passphrase sign-in, first-Owner creation, one-time recovery-code acknowledgment, and explicit validation/error states;
- kept passphrases out of navigation and saved state, cleared field state on submission, zeroed submitted `CharArray` values, and carried only a memory-only session handle into the authenticated preview;
- made system Back and visible Back behavior explicit at every access stage; leaving Cashier invalidates the native session before returning to Welcome;
- added account-specific native method selection backed by the existing encrypted Quick unlock records, with Quick PIN, Android phone security, and the always-available full-passphrase route shown only when applicable;
- connected Quick PIN to the real per-account security service, including cross-account rejection, immediate corrected-PIN retry, delay/full-passphrase recovery, and full-passphrase reset behavior;
- connected Android phone security through the Activity-scoped `BiometricPrompt` device gate, using `Phone security` unless a strong biometric is actually available and never claiming a fingerprint that Android has not enrolled;
- left `HybridActivity` as the only launcher and reused the existing database, security, permission, audit, and business rules unchanged.

## Compatibility decision

The current stable Activity 1.13/Lifecycle 2.11 Compose path requires Android API 37 and AGP 9.1. This verified application is on Android API 36 and AGP 9.0.1. The foundation therefore uses Activity 1.12.4, Lifecycle 2.10.0, Compose BOM 2025.12.00, and Compose compiler 2.2.10. This avoids a toolchain migration inside the first presentation checkpoint.

## Verification

- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -Psaaf.instrumentationSandbox=true` — passed;
- `:app:assembleDebug -Psaaf.isolatedQa=true` — passed;
- `NativeOwnerBootstrapUiTest` on wired A06 `SM-A065F` — 1/1 passed on a fresh isolated installation, including disabled submit, visible/system Back, mismatch rejection, Owner creation, recovery-code retention, acknowledgment, and entry to Cashier;
- `NativeFoundationUiTest` on wired A06 `SM-A065F` — 16/16 passed, covering Welcome, account and method selection, blank/invalid/retried/valid passphrase and Quick PIN states, cross-account PIN rejection, real-rate calculations, customer selection, disabled Record, paid and credit Room writes, actor attribution, insufficient-stock rollback, refreshed success, and untouched/edited Back routes;
- `NativeSaleScreenContractTest` — 1/1 passed, covering visible retry from an authoritative Sale load failure;
- `Phase8SaleOperationsTest` — 4/4 passed, preserving paid/credit calculations, summaries, stock/day/permission rejection, audit attribution, and packaged-app Room behavior;
- `NativeAuthenticationFacadeTest` and `NativeAuthenticationScreenContractTest` — 5/5 passed, covering typed sessions, secret zeroing, active-account filtering, startup retry, enrolled-method visibility, phone-security cancellation, and rate-limited full-passphrase recovery;
- `Phase14QuickUnlockTest` — 10/10 passed, including encrypted persistence, immediate corrected-PIN retry, PIN delay/full-passphrase reset, phone-security strength, invalidation, and concurrent-attempt/removal races;
- combined native authentication checkpoint on the wired A06 — **28/28 passed, 0 failed, 0 skipped**;
- authoritative native Sale checkpoint on the wired A06 — **21/21 passed, 0 failed, 0 skipped** across the native screen, Sale load-state contract, and existing Sale service regressions;
- manual native Phone security on the A06 — existing Owner enrollment appeared as `Owner · Phone security, full passphrase`; device-PIN approval opened `New sale`; cancelling a second prompt retained the selected Owner and showed `Quick unlock was cancelled`;
- isolated package installed as `com.saaf.milkshop.qa`; ordinary Saaf package was not touched;
- real-device Welcome evidence: `phase14-evidence/saaf-native-welcome-a06.png`;
- real-device Cashier evidence: `phase14-evidence/saaf-native-cashier-a06.png`;
- the A06 render shows the full normal Cashier path, review, commit action, and four-item navigation in one viewport.

## Current boundary

This is a service-connected authentication and Sale slice inside the debug-native harness, not a completed application migration. Account discovery, first-Owner creation, full-passphrase verification, recovery acknowledgment, Quick PIN, Android phone-security invocation, session boundaries, Sale reference reads, and Sale writes use the real native security and Room authority. Quick PIN is automated on the A06, and the actual Android phone-security prompt has passed both approval and cancellation on the native screen. Record sale now writes one permission-checked, actor-attributed transaction and refreshes the native screen. Secondary sale details and the Collect, Activity, and More destinations still expose their explicit preview contracts. The native activity is not reachable from the production launcher.

## Next checkpoint

Close the remaining foundation risks in this order:

1. prove Sale restoration and session-expiry behavior during process/lifecycle recreation;
2. run large-text, IME, TalkBack, dark-theme, and rotation checks on the now-authoritative Sale screen;
3. choose and implement the next role-specific native destination before changing launcher ownership.
