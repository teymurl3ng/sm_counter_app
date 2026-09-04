# Phase 12 — UI Fidelity

Status: **IN PROGRESS — checkpoint 12.1 complete; checkpoint 12.2 next**

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

### 12.2 — Visual target and presentation system

- Reconcile the approved UI target with actual Android rendering.
- Lock typography, spacing, color, hierarchy, controls, dialog, density, theme, and language decisions.
- Score proposed changes against the fit-for-purpose function, formula, and screen framework in `PRODUCT_SCOPE.md`; do not pursue corporate-ERP density or feature parity.

### 12.3 — Navigation and natural task flows

- Improve discoverability and common task sequences while preserving permissions and native-backed outcomes.
- Reconcile default opening area, report period, and presentation preferences.

### 12.4 — Screen-by-screen responsive and accessibility pass

- Apply the approved system across every retained screen and meaningful state.
- Verify keyboard, inset, focus, touch-target, readable-text, loading, error, and empty-state behavior.
- Define receipt/image/PDF presentation from authoritative native records; Android sharing acceptance remains Phase 14.

### 12.5 — Measured presentation performance

- Measure first draw, React-ready time, and representative navigation on a profileable release-equivalent build.
- Make only evidence-supported presentation optimizations that preserve security, freshness, and one-WebView architecture.

### 12.6 — Regression and handoff

- Rebuild and verify TypeScript, Vite assets, Android packaging, bridge drift, focused UI/device checks, and the complete regression suite.
- Publish final before/after evidence, limitations, and guarded Phase 13 handoff.

## Current next action

Begin checkpoint 12.2: isolate the physical-device paint/inset findings, reconcile the approved visual target, and lock the presentation system before changing production screens.

## Plain English

The before-picture is complete: 81 screenshots cover every important screen and meaningful state on the real phone. Next we will fix the device-level drawing and status-bar problems, then agree on one consistent visual system before polishing the screens. The shop’s records, calculations, and security have not been changed.
