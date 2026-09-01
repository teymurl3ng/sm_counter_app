# Phase 9 — Runtime, Navigation, and Device Integration

**Status: IN PROGRESS — responsive navigation milestone complete 2026-09-01**

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

## 3. Remaining milestone order

1. **Complete:** responsive grouped navigation;
2. isolated QA/test application data, then Android Back and in-app overlay/navigation coordination;
3. WebView lifecycle, recreation, and protected-session restoration;
4. keyboard, system bars, and display insets;
5. refresh/transfer measurement and targeted-state improvements;
6. renderer/error recovery and complete physical-device regression.

The order may combine closely related tests, but Phase 9 does not absorb the Phase 10 periodic cloud-backup design gate or the Phase 12 visual-fidelity phase.

## 4. Verification baseline

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

## 5. Device-test isolation requirement

The existing debug instrumentation target shares the installed debug app's package data. When the Samsung contained a different real/demo owner, the legacy test runner correctly rejected its instrumentation password before running any test. The test was changed only for the exact read-only Phase 9 navigation class: it may start a memory-only session for an existing active owner, without changing credentials or saved records.

Do not run the mutating complete instrumentation suite against a phone copy containing data the user wants to keep. Before the next complete regression, provide a separate QA application ID/data sandbox or a dedicated clean emulator/device workflow. Release code contains no authentication bypass.

## Plain English

Phase 8 made every shop screen work with the real saved data. Phase 9 makes the app behave properly as a phone app. The long menu strip is now replaced by four everyday buttons and a grouped More panel. Next we will separate the automated testing copy from the copy you inspect, then make Android's Back button close menus/dialogs or return through app areas safely instead of accidentally leaving a protected screen.
