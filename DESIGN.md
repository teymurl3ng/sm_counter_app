# Saaf Milk Presentation System

Status: **AUTHORITATIVE PHASE 12 TARGET — 2026-09-04**

This document governs the packaged React/WebView interface. It derives a coherent target from the real Samsung rendering rather than introducing a new visual identity. `PRODUCT_SCOPE.md` remains authoritative for product, function, formula, and reporting boundaries.

## Product character

The interface should feel:

- calm, dependable, practical, and easy to learn;
- suitable for an owner or staff member doing daily shop work;
- clear without requiring accounting vocabulary;
- focused on the immediate task rather than enterprise dashboard density.

It should not imitate a corporate ERP, add features to make screenshots look comprehensive, or become decorative at the expense of speed and clarity. Saaf Milk is the current brand and Milk/Yogurt are the current validated products; neither is a permanent market boundary.

## Sources of truth

Use evidence in this order:

1. Native business records, permissions, and established product wording.
2. This presentation system and the physical Android render.
3. Android platform and WCAG guidance for system behavior and accessibility.
4. Observed owner/staff task completion.
5. Comparable products as pattern references only.
6. Aesthetic preference.

The physical packaged app is the acceptance surface. Browser previews are development aids, not proof of Android behavior. AI design tools can identify inconsistencies but cannot authorize accounting, legal, or domain changes.

## Visual roles

The current light theme is the approved Phase 12 theme. Do not add a partial dark theme during polish.

| Role | Token | Intended use |
| --- | --- | --- |
| Brand header | `#004F3B` | Screen header and Android status-bar protection |
| Primary action | `#007A55` | The main safe action in a task or section |
| Brand soft | `#ECFDF5` | Selected and positive supporting states |
| Canvas | `#F7F6F0` | App background |
| Surface | `#FFFFFF` | Cards, sheets, fields, and bottom navigation |
| Primary text | `#0F172A` | Headings, values, and important body copy |
| Muted text | `#475569` | Supporting copy that still passes contrast requirements |
| Outline | `#CBD5E1` | Field and surface boundaries |

Emerald communicates brand, selection, safe completion, and success. Amber is caution; rose is destructive/error; sky is neutral information. Do not use color as the only carrier of meaning. New colors require a semantic role, not a screen-specific preference.

## Typography

Use the Android/system sans-serif stack. Prefer plain language and sentence case.

| Role | Size / line height | Weight |
| --- | --- | --- |
| Screen title | 24 / 32 px | 700 |
| Section title | 20 / 28 px | 700 |
| Subsection title | 18 / 26 px | 700 |
| Body, input, material value | 16 / 24 px | 400–600 |
| Supporting text | 14 / 20 px | 400–600 |
| Metadata or navigation | 12 / 16 px minimum | 600–700 |

Use tabular numerals for aligned quantities and money. Keep `PKR` with its value. Avoid uppercase except short eyebrows or compact status labels. Existing 11 px navigation labels are a recorded migration gap for checkpoint 12.4, not an approved exception.

## Spacing and shape

- Base spacing scale: 4, 8, 12, 16, 24, 32, and 48 px.
- Compact-screen horizontal gutter: 16 px; expanded layouts may use 24 px.
- Standard card padding: 16–20 px.
- Related label/control spacing: 6–8 px; field-group spacing: 16 px.
- Control radius: 12 px; surface radius: 16 px; bottom-sheet top radius: 24 px.
- Shadows remain subtle and support separation; borders carry most structure.
- Interactive targets must be at least 48 × 48 CSS px unless the platform control already provides an equivalent hit area.

## Hierarchy and density

Each screen should answer, in order:

1. Where am I?
2. What needs attention or what am I doing?
3. What facts must I enter or review?
4. What is the next safe action?
5. What happened after I acted?

Frequent work should not be buried under explanations. Put the primary action after its required inputs and make it visually strongest. Use one primary emphasis per task region. Put explanations, history, and assumptions below the immediate task or behind progressive disclosure when that does not hide a material warning.

Do not increase density to resemble large accounting products. Add information only when it helps the supported user make the current decision accurately.

## Controls and feedback

- Labels remain visible above inputs; placeholders do not replace labels.
- Inputs use a clear boundary, 16 px value text, and an obvious focus indicator.
- Primary buttons use the primary-action color and explicit verbs such as `Record sale` or `Save customer`.
- Secondary actions use a lower-emphasis outline or neutral surface.
- Destructive actions use rose only when the outcome is destructive or replacing; confirmations state what remains recoverable.
- Disabled controls remain legible and explain unavailable actions when the reason is not obvious.
- Validation appears beside the affected field; task-level failures and success notices appear close to the action.
- Loading states preserve context and prevent duplicate submission without blanking unrelated content.

## Navigation and task changes

- The bottom bar is opaque and stable on compact Android screens. It must respect the navigation inset and must not rely on blur.
- Top-level destination changes jump immediately to the top; they do not smooth-scroll through unrelated content.
- The active destination must be identifiable by label, icon, and visual state.
- Secondary destinations must retain their real location rather than appearing only as generic `More`; the structural correction belongs to checkpoint 12.3.
- Task tabs must show the selected task and must not clip labels. The seven-item Daily work strip is a known 12.3 migration gap.

## System bars, scrolling, and motion

- Android edge-to-edge remains enabled because target SDK 36 requires it.
- The branded green header extends behind the transparent status bar. A matching top gradient protects white status icons after the header scrolls away and on light authentication screens.
- Critical content and controls use the native-published top, bottom, and IME insets.
- Compact touch devices use native automatic document scrolling; reserved scrollbar gutters are limited to larger fine-pointer previews.
- Motion is short and functional. Honor reduced-motion preferences. Do not animate top-level page changes or use persistent backdrop filters for basic navigation.

## Dialogs and sheets

- Use a bottom sheet on compact screens and a centered modal where space permits.
- Keep title, close action, warning, and final action understandable without relying on background context.
- Long sheets may scroll internally, but the current section and final action must remain discoverable.
- Closing with Android Back, the explicit close action, or a safe outside tap must be consistent.
- Never make destructive confirmation easier to trigger than cancellation.

## Language and displayed figures

- Use shop language first; introduce accounting terms only when they clarify a real consequence.
- Use generic words such as `product`, `shop`, or `customer` where the behavior is genuinely generic.
- Keep `Milk`, `Yogurt`, and dairy-specific wording where the current workflow or formula is specifically validated for them.
- Every material number keeps its established native meaning, input source, unit, precision, sign, cutoff, and disclosure.
- UI polish must not rename a management estimate as statutory profit, complete financial results, or regulatory compliance.

## Responsive target

The current product is an Android app, so review effort is proportional:

- primary: compact phones, including the 360 CSS-pixel / 720 physical-pixel Samsung baseline;
- required: keyboard open, large text, landscape, system bars, gesture/three-button navigation, and long translated or entered text;
- secondary: medium and expanded Android widths when the app is resizable;
- browser-only 1440 px desktop parity is not a release requirement unless a web product is separately approved.

Layouts should reflow rather than shrink. Horizontal scrolling is acceptable only for content whose structure genuinely requires it and must expose the selected/current item.

## Accessibility target

- Meet WCAG 2.2 AA contrast for text and meaningful controls.
- Preserve logical heading order, accessible names, label associations, and error relationships.
- Keep keyboard focus visible and reading/focus order consistent with visual order.
- Do not place touch targets under status, cutout, gesture, or keyboard insets.
- Verify at increased text size; do not truncate a material amount, warning, action, or location.

Primary references: [Android core app quality](https://developer.android.com/develop/adaptive-apps/quality-guidelines/core-app-quality), [Android edge-to-edge Views guidance](https://developer.android.com/develop/ui/views/layout/edge-to-edge), [Android system bars](https://developer.android.com/design/ui/mobile/guides/foundations/system-bars), and [WCAG 2.2](https://www.w3.org/TR/WCAG22/).

## Phase 12.2 source traceability

Phase 12.2 used a deliberately small set of sources. The table distinguishes evidence actually applied from references reserved for later checkpoints.

| Source or evidence | How it affected 12.2 | Status |
| --- | --- | --- |
| `PRODUCT_SCOPE.md` and the established native contracts | Kept the target proportional to an owner-managed business, rejected enterprise-density and feature-parity pressure, and prohibited presentation work from changing formulas or native meaning. | Directly applied |
| Phase 12.1 Samsung screenshots and packaged UI | Preserved the existing green/cream identity and supplied the actual compact-phone constraints from which the tokens and Android priorities were derived. | Directly applied |
| Android core app quality | Set the Android-first quality frame, representative physical-device verification, 48 dp touch-target target, readable contrast, consistent rendering, and adaptive behavior. | Directly applied to the target; full screen-by-screen verification remains 12.4 |
| Android edge-to-edge and system-bar guidance | Led directly to the branded top-inset gradient protection, retained edge-to-edge layout, and explicit status/navigation icon treatment. | Directly implemented and device-tested |
| WCAG 2.2 | Set the AA contrast, visible focus, label/error relationship, non-color-only meaning, and reduced-motion expectations. | Directly applied to the target; comprehensive accessibility audit remains 12.4 |
| ISO 9241-11 and Nielsen usability heuristics | Support the broader effectiveness, efficiency, feedback, consistency, error-prevention, and plain-language review framework. | Policy reference only in 12.2; task-flow application begins in 12.3 |
| IAS 2, IFRS for SMEs, FAO, and Codex dairy guidance | Continue to govern applicable function/formula/domain decisions through existing decision records. | Not re-evaluated in 12.2 because no business function or formula changed |
| Impeccable, UI/UX Pro Max, and ui-check | Their suggested approach—establish a design authority, capture real screens, and report evidence—helped frame the discussion. | Not installed or executed in 12.2 |
| axe, Lighthouse, Playwright, competitor-product reviews, and formal owner/staff usability sessions | Potential evidence for later accessibility, responsive, or task-flow work. | Not run in 12.2; no pass or conformity claim is made |

Therefore, completion of 12.2 means the visual target and shared Android presentation foundation are evidence-based and device-tested. It does not mean every screen already conforms to the target, that an external design skill certified the UI, or that formal user research has been completed.

## Change gate

For every proposed screen change, record:

- the user and task it helps;
- the observed screenshot, accessibility, platform, or task-flow finding;
- whether it is required, conventional, a user hypothesis, or aesthetic;
- any function or displayed-number meaning it touches;
- the smallest presentation change that resolves the finding;
- physical-device and regression evidence.

Reject a visual change when it adds unsupported behavior, changes a formula or native record, hides a material limitation, exists only to match a large competitor, or creates more cognitive work than it removes.

## Current migration boundary

Checkpoint 12.2 establishes the target and fixes the shared Android rendering foundation. Checkpoint 12.3 applies it to navigation and task flow. Checkpoint 12.4 applies it screen by screen, including text size, touch targets, forms, dialogs, responsive behavior, and accessibility. Existing nonconforming screens remain findings until those checkpoints verify them.

## Plain English

Keep the app calm, clear, and practical for a small shop. Use one consistent green-and-cream system, show the current task and next action clearly, and make every claimed number understandable and honest. We are not trying to copy a giant accounting system; we are making the smaller set of things this app supports dependable and easy to use.
