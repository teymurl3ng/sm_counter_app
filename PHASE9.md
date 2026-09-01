# Phase 9 — Runtime, Navigation, and Device Integration

**Status: COMPLETE — all runtime, navigation, device-integration, recovery, and final physical-device gates passed 2026-09-01**

## 1. Reconciled scope

This root phase follows the completed hybrid Phase 8 ledger. The historical `phase18/PHASE9.md` describes earlier native backup work and does not define the current hybrid phase number.

Phase 9 hardens the completed 78-action product as a real Android/WebView application without changing accounting, persistence, roles, or bridge contracts. Its required outcomes are:

1. compact, role-aware navigation that does not rely on one endlessly scrolling horizontal or vertical menu;
2. one React navigation path inside one active WebView presentation, with no page reload for ordinary screen changes;
3. safe Android Back behavior for dialogs, secondary navigation, signed-in screens, and logout boundaries;
4. deliberate keyboard, status-bar, navigation-bar, and display-inset behavior;
5. safe configuration/process recreation and renderer-failure behavior without exposing a protected screen after session loss;
6. measured, screen-scoped refresh behavior that avoids unnecessary repeated bridge/state transfer.

The AndroidX WebKit, restricted `WebViewAssetLoader` HTTPS origin, and origin-checked `WebMessageListener` foundations already exist and remain mandatory.

## 2. Milestone 1 — Responsive Application Navigation

Status: **COMPLETE — physical-device verified 2026-09-01**.

Acceptance criteria:

- show no more than four everyday destinations directly, plus one More control when needed;
- organize remaining authorized destinations into named groups rather than a single long strip;
- keep every destination permission-aware for usability while retaining native Kotlin authorization as the real guard;
- provide a fixed phone navigation surface and a compact sticky wider-screen surface;
- keep the current-area title clear and make secondary-area selection visible through the More control;
- avoid horizontal page/menu scrolling and reserve space for the phone navigation surface;
- preserve existing navigation element IDs so the completed Phase 8 packaged tests remain valid;
- prove that switching areas retains the same WebView and packaged URL;
- change no Room schema, business formula, permission bundle, or bridge endpoint.

Implemented:

- a fixed phone navigation bar with at most four direct role-authorized destinations and one More control;
- a compact sticky wider-screen version of the same navigation model;
- grouped secondary areas under Records & closing, People & stock, and Shop controls, while everyday destinations remain direct when permitted;
- active secondary-area feedback through the More control;
- automatic return to the top of content after an area change;
- Android safe-area-aware top and bottom spacing without changing the current non-edge-to-edge window policy;
- preservation of every Phase 8 navigation element ID for existing packaged integration coverage;
- a separate `HybridNavigation.tsx` presentation component, keeping navigation layout out of the already broad workspace coordinator.

Packaged Samsung evidence proves four direct destinations, grouped secondary destinations, no document-width overflow, sheet close/selection behavior, one unchanged WebView instance, one unchanged packaged URL, and no additional page navigation entry while changing areas.

## 3. Milestone 2 — Isolated QA, Android Back, and Structural Hardening

Status: **COMPLETE — physical-device verified 2026-09-01**.

Implemented:

- opt-in `-Psaaf.isolatedQa=true` debug application ID suffix `.qa`, giving instrumentation a separate installation and data directory while leaving the normal debug app untouched;
- one shared packaged-WebView instrumentation driver instead of repeated login/input/evaluation helpers;
- Android Back coordination that closes an active dialog/More sheet, returns a secondary area to the first authorized primary area, and backgrounds the task at the root;
- same-WebView and same-packaged-URL proof across navigation and Back actions;
- the active React shell renamed from its historical Phase 8 name and split into a 155-line composition root, a session/authentication hook, and an authorized-data hook;
- `HybridActivity` reduced to lifecycle/composition responsibilities, with separate secure WebView hosting and Android document-picker classes;
- `BackupManager` reduced from 1,115 lines to a 476-line facade/coordinator, with separate cryptography, restore, and JSON codec collaborators and no backup-format or schema change.

## 4. Milestone 3 — Secure Activity and WebView Recreation

Status: **COMPLETE — physical-device verified 2026-09-01**.

Implemented:

- genuine Android Activity recreation may hand a still-live memory-only native session to the newly created packaged WebView;
- React validates that handle through `query.currentSession` before setting any protected stage or loading authorized shop data;
- the selected authorized management area is restored after recreation, while dialogs, form drafts, credentials, and session handles are not written to saved-instance state;
- the handle is never stored in preferences, WebView storage, a URL, or Android saved-instance state; only the non-sensitive area code is saved;
- AndroidX WebKit document-start injection is used when supported, with a packaged-origin page-finished event fallback;
- fresh Activity launches never consume an existing native handle and continue through normal owner setup/login;
- process death destroys `MilkShopApplication` and `NativeSessionManager`, so reopening after process death requires login;
- invalid/expired restoration is rejected before protected navigation renders and clears the stale application-level reference;
- each recreated Activity owns exactly one new WebView and destroys the previous Activity's WebView cleanly.

## 5. Milestone 4 — Keyboard, System Bars, and Display Insets

Status: **COMPLETE — physical-device verified 2026-09-01**.

Implemented:

- intentional edge-to-edge window ownership with transparent system bars and readable light status-bar/dark navigation-bar icon contrast;
- explicit `adjustResize` keyboard policy in both the Activity manifest and runtime window configuration;
- native `WindowInsetsCompat` observation that publishes status-bar, navigation-bar, and IME measurements to the packaged page without adding a general-purpose bridge capability;
- `viewport-fit=cover`, `interactive-widget=resizes-content`, and live `visualViewport` sizing for the single long-lived React presentation;
- safe top/bottom spacing for the header, fixed application navigation, dialogs, and the grouped More sheet;
- focused-field visibility correction after keyboard/viewport movement, while the phone navigation temporarily hides during typing and returns after the keyboard closes;
- a focused physical-device test that opens a real product dialog, invokes the Samsung keyboard, and proves the lower field and dialog remain inside the usable viewport.

## 6. Milestone 5 — Targeted Refresh and Transfer Measurement

Status: **COMPLETE — physical-device verified 2026-09-01**.

Implemented:

- one explicit authorized-data slice catalog covering the ten existing workspace query families without changing the frozen bridge contract;
- per-operation refresh plans so product/customer/supplier/employee changes refresh master data and transaction references rather than every authorized data set;
- operation-specific refresh plans for Daily Work, deliveries, Settings, corrections, and cash closing, preserving every screen value affected by each native command;
- Attendance continues to reload its selected date locally, while its save no longer triggers an unrelated workspace reload;
- Planning mutations reload only Planning, while costing/yield/loss mutations reload only Costing after their initial two-query screen load;
- anonymous in-memory bridge metrics for call count, failures, request/response character counts, and total duration by action; payload content and session handles are never retained in metrics;
- a Samsung proof that ordinary React navigation transfers no bridge data and a product save uses exactly one command plus two relevant queries instead of one command plus up to ten queries;
- preservation of the full parallel authorized load at login/recreation, when the complete permission-appropriate workspace state is genuinely required.

## 7. Milestone 6 — Renderer/Error Recovery and Phase Closure

Status: **COMPLETE — physical-device verified 2026-09-01**.

Implemented:

- native handling for main-frame load errors, main-frame HTTP failures, and Android WebView renderer termination;
- immediate removal and destruction of a failed WebView so damaged or stale protected content cannot remain visible;
- a native safe-area-aware recovery screen that explains the failure without exposing session or business data;
- an explicit secure reload action that creates a fresh Activity and WebView and revalidates any still-live memory-only session before restoring protected navigation;
- forced return to Login when the session is missing or invalid during recovery, with no protected navigation rendered first;
- safe Android Back behavior from the recovery screen;
- physical Samsung tests that terminate the real WebView renderer for both valid-session recovery and invalid-session rejection.

## 8. Completed milestone order

1. **Complete:** responsive grouped navigation;
2. **Complete:** isolated QA/test data, Android Back/overlay coordination, and useful Phase 8/9 file fragmentation;
3. **Complete:** WebView lifecycle, Activity recreation, and protected-session restoration;
4. **Complete:** keyboard, system bars, and display insets;
5. **Complete:** refresh/transfer measurement and targeted-state improvements;
6. **Complete:** renderer/error recovery and final Phase 9 physical-device regression.

The order may combine closely related tests, but Phase 9 does not absorb the Phase 10 periodic cloud-backup design gate or the Phase 12 visual-fidelity phase.

## 9. Verification baseline

Inherited from completed Phase 8:

- connected physical-device suite — **211/211 PASS**;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript check and production Vite build — PASS;
- bridge contract — version 2, all 78 operations implemented, SHA-256 `AB374869603B9D773AD86B56EF801DA8C4493816C7301B05214AE596E33D0920`;
- frozen logic-spec SHA-256 — `45E5BDDABE8861A1B3DF7C5666AC7673078439A89FA5628061AD293A865F9DEA`;
- frozen SQL blueprint SHA-256 — `FB701C90D2DD97A1E6D962154692C48EA0F4E77E9300F9DFAC653D4AB67BBC51`.

Phase 9 Milestone 1 verification:

- focused packaged navigation/runtime test on `SM-A065F` — **1/1 PASS**;
- TypeScript no-emit check — PASS;
- production Vite build and Android asset packaging — PASS;
- Kotlin and Android-test Kotlin compilation — PASS;
- bridge drift/asset gates — PASS.

Phase 9 Milestone 2 verification:

- focused packaged navigation/Android Back runtime test on `SM-A065F` — **1/1 PASS**;
- focused encrypted/local backup and restore suites after fragmentation — **13/13 PASS**;
- complete isolated-QA connected-device regression — **212/212 PASS**, 0 skipped, 0 failed;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript no-emit check, production Vite build, Android asset packaging, Kotlin compilation, Android-test compilation, bridge drift gate, and packaged-asset gate — PASS;
- frozen logic-spec and SQL blueprint SHA-256 values — unchanged and verified.

Phase 9 Milestone 3 verification:

- focused valid-session and invalid-session Activity recreation suite on `SM-A065F` — **2/2 PASS**;
- combined Phase 9 navigation, Android Back, and recreation suite — **3/3 PASS**;
- fresh-launch compatibility regression covering the vertical slice, Accounts, Backup, and lifecycle paths — **14/14 PASS**;
- complete isolated-QA connected-device regression — **214/214 PASS**, 0 skipped, 0 failed;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript no-emit check, production Vite build, Android asset packaging, Kotlin compilation, Android-test compilation, bridge drift gate, and packaged-asset gate — PASS.

Phase 9 Milestone 4 verification:

- focused live keyboard/system-inset suite on `SM-A065F` — **1/1 PASS**;
- combined Phase 9 navigation, recreation, and keyboard/inset suite — **4/4 PASS**;
- complete isolated-QA connected-device regression — **215/215 PASS**, 0 skipped, 0 failed;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript no-emit check, production Vite build, Android asset packaging, Kotlin compilation, Android-test compilation, bridge drift gate, and packaged-asset gate — PASS.

Phase 9 Milestone 5 verification:

- focused navigation/targeted-refresh/transfer-measurement suite on `SM-A065F` — **1/1 PASS**;
- combined Phase 9 navigation, recreation, keyboard/inset, and targeted-refresh suite — **5/5 PASS**;
- complete isolated-QA connected-device regression — **216/216 PASS**, 0 skipped, 0 failed;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript no-emit check, production Vite build, Android asset packaging, Kotlin compilation, Android-test compilation, bridge drift gate, and packaged-asset gate — PASS.

Phase 9 Milestone 6 verification:

- focused real-renderer termination and secure recovery suite on `SM-A065F` — **2/2 PASS**;
- complete isolated-QA connected-device regression — **218/218 PASS**, 0 skipped, 0 failed;
- host-side debug unit suite — **7/7 PASS**;
- TypeScript no-emit check, production Vite build, Android asset packaging, Kotlin compilation, Android-test compilation, bridge drift gate, and packaged-asset gate — PASS;
- bridge contract remains version 2 with all 78 operations and SHA-256 `AB374869603B9D773AD86B56EF801DA8C4493816C7301B05214AE596E33D0920`.

## 10. Device-test isolation requirement

Instrumentation must be run with `-Psaaf.isolatedQa=true`. That property adds the `.qa` application-ID suffix to the debug target, so the QA app has independent Android storage and may safely bootstrap its instrumentation owner. The normal `com.saaf.milkshop` debug installation is not cleared, borrowed, or authenticated by the test runner. Release code contains no authentication bypass.

Do not run the mutating complete instrumentation suite without the isolated-QA property against a phone copy containing data the user wants to keep.

## Plain English

Phase 8 made every shop screen work with real saved data. Phase 9 now makes it behave like a proper phone app. The menu is grouped, Back behaves normally, screen recreation is secure, forms stay usable with the keyboard, and the app rereads only the shop information affected by an action. If Android's screen-rendering engine stops, the broken screen is removed and replaced by a clear Reload screen; the app checks the login again before showing private shop information. Phase 9 is closed. The next gate is Phase 10's product-completion and periodic encrypted cloud-backup design work.
