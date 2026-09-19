# Phase 14.8.3 — Compose foundation

Status: **STARTED — native visual foundation runs on A06; service-bound vertical slice is next**

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
- left `HybridActivity` as the only launcher and left all database, security, bridge, and business behavior unchanged.

## Compatibility decision

The current stable Activity 1.13/Lifecycle 2.11 Compose path requires Android API 37 and AGP 9.1. This verified application is on Android API 36 and AGP 9.0.1. The foundation therefore uses Activity 1.12.4, Lifecycle 2.10.0, Compose BOM 2025.12.00, and Compose compiler 2.2.10. This avoids a toolchain migration inside the first presentation checkpoint.

## Verification

- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -Psaaf.instrumentationSandbox=true` — passed;
- `:app:assembleDebug -Psaaf.isolatedQa=true` — passed;
- `NativeFoundationUiTest` on wired A06 `SM-A065F` — 8/8 passed, covering every current Welcome/Cashier button, selection, input, dialog action, disclosure, commit action, and navigation destination;
- isolated package installed as `com.saaf.milkshop.qa`; ordinary Saaf package was not touched;
- real-device Welcome evidence: `phase14-evidence/saaf-native-welcome-a06.png`;
- real-device Cashier evidence: `phase14-evidence/saaf-native-cashier-a06.png`;
- the A06 render shows the full normal Cashier path, review, commit action, and four-item navigation in one viewport.

## Current boundary

This is a native implementation checkpoint, not a completed vertical slice. Sample identity, date, products, rates, customer accounts, and effects are presentation fixtures. The preview controls now form a coherent local state loop, but they do not read reference data or write a sale. Continue goes directly from Welcome to Cashier in the debug harness. Record sale deliberately writes nothing. The native activity is not reachable from the production launcher.

## Next checkpoint

Build the service-bound authentication-to-sale slice in this order:

1. expose a presentation-neutral typed authentication facade around `UserSecurityService` and the existing Quick unlock service;
2. add startup/bootstrap state, account selection, enrolled-method selection, and full-passphrase/Quick-unlock screens;
3. carry the memory-only native session into a role-calibrated native shell without placing secrets in saved state or navigation arguments;
4. bind Cashier reference data and Sale commands through the existing typed application/domain boundary;
5. replace fixture totals and effects with authoritative preview state, then implement saving, success, error, refresh, Back/draft, and restoration behavior;
6. prove the Room event, stock/cash effect, audit actor, session expiry, large text, IME, TalkBack, and process recreation before changing launcher ownership.
