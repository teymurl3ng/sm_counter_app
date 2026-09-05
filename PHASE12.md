# Phase 12 — UI Fidelity

Status: **COMPLETE — checkpoints 12.1–12.6**

## Purpose

Improve the clarity, consistency, responsiveness, accessibility, and perceived performance of the packaged React/WebView experience without changing native business, accounting, security, backup, or recovery semantics. Apply the proportional product and benchmark policy in `PRODUCT_SCOPE.md`: strong correctness within a deliberately small-business scope, without enterprise feature-parity pressure.

## Locked boundaries

- Room remains the only writable business truth.
- Kotlin remains responsible for validation, authorization, accounting, precision, encryption, audit, backup, and recovery.
- Bridge contract v4 and its 86 operations remain frozen unless a separately approved additive capability passes its own gate.
- Phase 12 changes presentation and presentation-only state; it does not create a second client-side business store.
- Performance changes require profileable release-equivalent evidence and must preserve session revalidation and data freshness.
- Saaf Milk and Milk/Yogurt describe the current implementation and validated domain; Phase 12 must not harden them into permanent product limits or imply that unsupported future products already have authoritative formulas.
- UI review uses applicable standards as guardrails, comparable products as references, and real owner/staff tasks as acceptance evidence. Aesthetic preference alone does not authorize a change.

## Ordered checkpoints

### 12.1 — Physical-device screen and state baseline ✅

- Capture every retained main area and meaningful subview on the isolated Samsung QA installation.
- Include onboarding, authentication, navigation, populated records, empty/inactive tabs, dialogs, and the principal settings/security/backup states.
- Redact one-time recovery material in saved evidence.
- Record the capture matrix and screen-by-screen visual findings before changing UI code.

Completed with 81 renderer-synchronized screenshots covering 57 named states on the Samsung SM-A065F / Android 16. See `PHASE12_SCREEN_AUDIT.md` and `phase12-evidence/sm-a065f-android16-render-synced/`.

### 12.2 — Visual target and presentation system ✅

- Reconcile the approved UI target with actual Android rendering.
- Lock typography, spacing, color, hierarchy, controls, dialog, density, theme, and language decisions.
- Score proposed changes against the fit-for-purpose function, formula, and screen framework in `PRODUCT_SCOPE.md`; do not pursue corporate-ERP density or feature parity.

Completed with `DESIGN.md` as the authoritative presentation target. Its **Phase 12.2 source traceability** table records which sources were directly applied, which remain policy references, and which suggested tools or audits were not run. The packaged app now uses an exact branded header role with Android top-inset gradient protection, opaque compact navigation, immediate top-level destination changes, touch-appropriate document scrolling, visible keyboard focus, and reduced-motion handling. These are shared presentation foundations only; no native record, formula, permission, accounting, security, backup, or recovery behavior changed.

The physical-device review also separated source evidence from preview behavior. PixelCopy plus WebView visual-state synchronization produced a second 81-image set in `phase12-evidence/sm-a065f-android16-phase12-2-stable/`. Apparent missing glyphs in some scaled tall-image previews are present in the PNG pixel data and in exact crops; reviewers must inspect the source file at original size or use smaller crops before recording a paint defect. Repeated normal-use navigation remains clean under a 60-transition device stress check.

### 12.3 — Navigation and natural task flows ✅

- Improve discoverability and common task sequences while preserving permissions and native-backed outcomes.
- Reconcile default opening area, report period, and presentation preferences.

Completed with a compact current-task control and a complete permission-aware Daily work chooser, real secondary-area identity in the bottom navigation, role-appropriate opening areas, an explicit `Today` report default, and an honest decision not to expose unsupported saved presentation preferences. Android Back, existing element IDs, the one-WebView path, permissions, and native outcomes remain intact. See `PHASE12_3_NAVIGATION_REVIEW.md` and `phase12-evidence/sm-a065f-android16-phase12-3-navigation/`.

### 12.4 — Screen-by-screen responsive and accessibility pass ✅

- Apply the approved system across every retained screen and meaningful state.
- Verify keyboard, inset, focus, touch-target, readable-text, loading, error, and empty-state behavior.
- Define receipt/image/PDF presentation from authoritative native records; Android sharing acceptance remains Phase 14.

Completed across all 58 retained states. Shared form controls, dialogs, notices, tabs, dense figures, supporting copy, and compact layouts now follow `DESIGN.md`; two missing delivery-field labels and measured contrast weaknesses were corrected. The physical-device capture gate now rejects horizontal overflow, undersized targets, sub-12 px visible text, unlabelled visible controls/dialogs, and WCAG-AA text-contrast failures before accepting a screenshot. See `PHASE12_4_SCREEN_REVIEW.md` and `phase12-evidence/sm-a065f-android16-phase12-4-wcag-screen-pass/`.

No retained native/bridge receipt, image-export, or PDF-export capability exists, so presentation code did not invent one. Any future output must render authoritative native records, with Android sharing acceptance retained for Phase 14.

### 12.5 — Measured presentation performance ✅

- Measure first draw, React-ready time, and representative navigation on a profileable release-equivalent build.
- Make only evidence-supported presentation optimizations that preserve security, freshness, and one-WebView architecture.

Completed with a dedicated non-debuggable, shell-profileable benchmark build and a repeatable physical-device gate. On the Samsung baseline, Android cold-process start measured 822 ms median / 871 ms p95, React ready measured 327.9 ms median / 378.3 ms p95, and painted preloaded navigation measured 24.0 ms median / 29.6 ms p95 with zero native bridge calls. These results do not justify code splitting, deferred native reads, or another architectural optimization. See `PHASE12_5_PERFORMANCE_REVIEW.md` and `phase12-evidence/sm-a065f-android16-phase12-5-performance/metrics.tsv`.

### 12.6 — Regression and handoff ✅

- Rebuild and verify TypeScript, Vite assets, Android packaging, bridge drift, focused UI/device checks, and the complete regression suite.
- Publish final before/after evidence, limitations, and guarded Phase 13 handoff.

Completed from a fresh production web build and Android package. Bridge v4/86, TypeScript, Vite, packaged-asset parity, Android compilation/packaging, 7/7 host tests, 11/11 focused Phase 12 device tests, and the complete 258/258 Samsung regression all pass with no skips or failures. The final clean-state visual partition reproduced 83 screenshots across 58 states at 720 × 1600. Two legacy Products UI tests were updated to follow the approved Dashboard-first **More → Products** flow; production behavior did not change. See `PHASE12_6_HANDOFF.md` and `phase12-evidence/sm-a065f-android16-phase12-6-final/`.

## Current next action

Begin Phase 13 with a source-traced inventory of dead, mock, duplicated, and misleading UI. Remove unsupported presentation paths without adding local business state, UI-side formulas, synthetic reports, or false success messages.

## Checkpoint 12.2 verification

- UI lint and production web build — passed; packaged Android assets refreshed.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Android/Kotlin and instrumentation compilation — passed.
- `Phase12PresentationFoundationTest` — 4/4 passed on Samsung SM-A065F / Android 16, including 60 continuous destination changes.
- Combined system-inset and presentation device check — 5/5 passed.
- `Phase12VisualBaselineCaptureTest` — 1/1 passed; 81/81 PNGs are readable at 720 × 1600 and the 83-line manifest SHA-256 is `1C81401D6B8E25904A788B23B506CADDE8D052B0B0597D89894014FB86C715AC`.
- At checkpoint 12.2, the last complete isolated-QA regression was the Phase 11 gate at 247/247; the final Phase 12.6 result is recorded below.

## Checkpoint 12.4 verification

- UI lint and production web build — passed; packaged Android assets refreshed.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Android/Kotlin and instrumentation compilation — passed.
- Responsive/accessibility, navigation-flow, and system-inset checks — 5/5 passed on Samsung SM-A065F / Android 16.
- `Phase12VisualBaselineCaptureTest` — 1/1 passed after validating all 58 states; 83/83 PNGs are readable at 720 × 1600.
- The 85-line final manifest SHA-256 is `8E9937FC972B2EE36D8E31D5FB58C0C7C86189D4C8A8F116AFA91C727B45F1BE`.
- At checkpoint 12.4, the last complete isolated-QA regression was the Phase 11 gate at 247/247; the final Phase 12.6 result is recorded below.

## Checkpoint 12.5 verification

- UI lint and production web build — passed; CSS 85.71 kB (14.11 kB gzip) and JavaScript 433.76 kB (111.18 kB gzip).
- Normal debug and dedicated benchmark Android/instrumentation compilation — passed.
- Benchmark manifest — release-initialized, non-debuggable, signed for local QA, and shell-profileable.
- `Phase12PresentationPerformanceTest` — 2/2 passed on Samsung SM-A065F / Android 16.
- Android cold-process start — 10/10 successful; 822 ms median / 871 ms p95.
- WebView first contentful paint — 136 ms median / 204 ms p95.
- React ready — 327.9 ms median / 378.3 ms p95.
- Painted preloaded navigation — 24.0 ms median / 29.6 ms p95 / 30.6 ms max, with zero bridge calls.
- Raw seven-line performance evidence SHA-256 — `A34327B3EFADEC0A476F5F60B6836082DB47AA31669534C7014E619EC7F7BAAC`.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- No performance optimization was made because every measured result was comfortably within the locked guardrails.
- At checkpoint 12.5, the last complete isolated-QA regression was the Phase 11 gate at 247/247; the final Phase 12.6 result is recorded below.

## Checkpoint 12.6 verification

- UI type check and production build — passed; Vite transformed 1,897 modules and reproduced the accepted 433.76 kB JavaScript / 85.71 kB CSS bundle.
- Packaged asset parity, Android debug packaging, production Kotlin compilation, and Android-test Kotlin compilation — passed.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Immutable logic specification and SQL blueprint hashes — unchanged.
- Host debug unit tests — 7/7 passed, 0 skipped, 0 failed.
- Focused Phase 12 device gate — 11/11 passed, 0 skipped, 0 failed.
- Main isolated-QA Samsung partition — 257/257 passed, 0 skipped, 0 failed.
- Empty-identity visual/accessibility Samsung partition — 1/1 passed, 0 skipped, 0 failed; 83 screenshots / 58 states.
- Complete Phase 12 device regression — 258/258 across the two required clean-state partitions.
- Final evidence manifest — 85 lines, SHA-256 `8E9937FC972B2EE36D8E31D5FB58C0C7C86189D4C8A8F116AFA91C727B45F1BE`.
- Debug APK SHA-256 — `55CB0EF5ED76795AEE207A465C0A5C40CA1FD2FE74A168DC3843A21C6AE03091`.

## Plain English

Phase 12 is finished. Navigation and every retained screen are clearer on the real phone, the release-like build is comfortably within its speed budgets, and all 258 phone tests pass. The only regression fix taught two old tests to follow the new Products route; the shop's records, calculations, permissions, and security were not changed. Phase 13 is next and will remove dead, mock, duplicated, or misleading UI.
