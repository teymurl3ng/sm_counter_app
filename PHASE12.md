# Phase 12 — UI Fidelity

Status: **IN PROGRESS — checkpoints 12.1–12.4 complete; checkpoint 12.5 next**

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

### 12.5 — Measured presentation performance

- Measure first draw, React-ready time, and representative navigation on a profileable release-equivalent build.
- Make only evidence-supported presentation optimizations that preserve security, freshness, and one-WebView architecture.

### 12.6 — Regression and handoff

- Rebuild and verify TypeScript, Vite assets, Android packaging, bridge drift, focused UI/device checks, and the complete regression suite.
- Publish final before/after evidence, limitations, and guarded Phase 13 handoff.

## Current next action

Begin checkpoint 12.5: measure first draw, React-ready time, and representative navigation on a profileable release-equivalent build, then make only evidence-supported presentation optimizations that preserve session revalidation, data freshness, and the one-WebView architecture.

## Checkpoint 12.2 verification

- UI lint and production web build — passed; packaged Android assets refreshed.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Android/Kotlin and instrumentation compilation — passed.
- `Phase12PresentationFoundationTest` — 4/4 passed on Samsung SM-A065F / Android 16, including 60 continuous destination changes.
- Combined system-inset and presentation device check — 5/5 passed.
- `Phase12VisualBaselineCaptureTest` — 1/1 passed; 81/81 PNGs are readable at 720 × 1600 and the 83-line manifest SHA-256 is `1C81401D6B8E25904A788B23B506CADDE8D052B0B0597D89894014FB86C715AC`.
- The last complete isolated-QA regression remains the Phase 11 gate at 247/247; checkpoint 12.6 owns the next complete-suite rerun.

## Checkpoint 12.4 verification

- UI lint and production web build — passed; packaged Android assets refreshed.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Android/Kotlin and instrumentation compilation — passed.
- Responsive/accessibility, navigation-flow, and system-inset checks — 5/5 passed on Samsung SM-A065F / Android 16.
- `Phase12VisualBaselineCaptureTest` — 1/1 passed after validating all 58 states; 83/83 PNGs are readable at 720 × 1600.
- The 85-line final manifest SHA-256 is `8E9937FC972B2EE36D8E31D5FB58C0C7C86189D4C8A8F116AFA91C727B45F1BE`.
- The last complete isolated-QA regression remains the Phase 11 gate at 247/247; checkpoint 12.6 owns the next complete-suite rerun.

## Plain English

Navigation and every retained screen are now clearer on the real phone. Controls are easier to tap, long dialogs guide users to their actions, tabs reflow, money stays together, messages have more than color alone, and the capture test checks size, labels, overflow, readable text, and contrast. Next we measure presentation performance. The shop's records, calculations, permissions, and security have not been changed.
