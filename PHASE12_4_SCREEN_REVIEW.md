# Phase 12.4 — Screen-by-Screen Responsive and Accessibility Review

Status: **COMPLETE — 2026-09-05**

## Outcome

The approved Phase 12 presentation system is now applied across all 58 retained meaningful application states. The work improves compact-phone readability, touch use, dialogs, forms, figures, tabs, feedback, and responsive behavior without changing a native record, calculation, permission, accounting rule, security rule, backup rule, or bridge operation.

## How the standards were used

| Source or evidence | Decision made in 12.4 |
| --- | --- |
| `PRODUCT_SCOPE.md`, native contracts, and existing domain decisions | Kept the pass proportional to an owner-managed shop app. No enterprise feature parity, new formula, or unsupported accounting claim was added. |
| Phase 12 Samsung screenshots | Supplied the actual compact-phone evidence for cramped tabs, tall forms, wrapped figures, weak supporting text, and long dialogs. |
| `DESIGN.md` | Supplied the approved typography, spacing, color, hierarchy, control, dialog, and language rules applied consistently across the retained UI. |
| [Android core app quality](https://developer.android.com/develop/adaptive-apps/quality-guidelines/core-app-quality) and adaptive-layout guidance | Drove the 48 px WebView touch target, large-text and landscape checks, inset preservation, and reflow instead of horizontal scrolling. |
| [WCAG 2.2 AA](https://www.w3.org/TR/WCAG22/) | Drove visible labels and accessible names, focus behavior, non-color-only notices, 12 px visible-text floor, and measured text contrast against effective backgrounds. |
| [Nielsen's ten usability heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/) | Supported consistent controls, visible system feedback, safer dialogs, plain language, and discoverable actions in long forms. |

Comparable products remain pattern references, not authorities for the app's calculations or scope. This pass judged the supported product on whether its real users can understand and complete its supported work; it did not try to imitate a corporate ERP.

## Presentation changes

- Standard inputs and buttons now have 48 px minimum touch height; form values use 16 px text and visible labels.
- Muted text and warning actions use contrast-safe tokens. Success and error notices add icons and semantic live-region behavior so meaning is not carried by color alone.
- Long dialogs now have an accessible title, initial and restored focus, Escape and contained Tab handling, a sticky header, a 48 px close action, and a discoverable route to the final form actions.
- Compact tab groups reflow into readable grids instead of horizontal strips. Dense account, report, planning, and opening-state layouts stack where required.
- Material monetary figures keep `PKR` with the value and use tabular numerals; cramped ledger amounts no longer compete with headings.
- Supporting explanations were raised where needed, and the two previously unlabelled delivery controls now have explicit visible labels.
- Existing element IDs, native actions, permissions, form outcomes, and Android Back behavior remain intact.

During verification, the automated contrast gate found and prompted correction of two real issues: muted headings/choice labels on tinted surfaces and white text on amber correction/deactivation actions.

## Executable screen gate

`Phase12VisualBaselineCaptureTest` now checks each visible captured surface before saving it:

- no root-level horizontal overflow;
- visible interactive targets meet the 48 px target, with a 1 px measurement tolerance;
- directly visible text is at least 12 px;
- visible form controls and dialogs have accessible labels;
- visible enabled text meets the WCAG AA normal- or large-text contrast target against its composited background, including modern WebView OKLCH, OKLab, and `color(srgb)` colors.

The capture test then saved 83 screenshots covering 58 states on the physical Samsung SM-A065F / Android 16. All PNGs are 720 × 1600. Evidence is in `phase12-evidence/sm-a065f-android16-phase12-4-wcag-screen-pass/`.

## Receipt, image, and PDF boundary

The retained v4 UI and bridge do not expose a receipt, image export, or PDF export path. Phase 12.4 does not invent one in presentation code. If such output is approved later, its values and wording must be rendered from authoritative native records; Android share-sheet and receiving-app acceptance remains a Phase 14 concern.

## Verification

- TypeScript lint and production Vite build — passed; packaged Android assets refreshed.
- Bridge contract — unchanged at v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Android application and instrumentation compilation — passed.
- `Phase12ResponsiveAccessibilityTest`, `Phase12NavigationFlowTest`, and `Phase9WindowInsetsTest` — 5/5 passed on Samsung SM-A065F / Android 16.
- `Phase12VisualBaselineCaptureTest` — 1/1 passed on the same device after traversing all 58 states.
- Evidence integrity — 83 PNGs, 10,129,506 PNG bytes, 58 distinct states, one completion row, and 85 manifest lines. Manifest SHA-256: `8E9937FC972B2EE36D8E31D5FB58C0C7C86189D4C8A8F116AFA91C727B45F1BE`.
- The last complete isolated-QA regression remains Phase 11 at 247/247; Phase 12.6 owns the next complete-suite rerun.

## Limitations

This is a source-reviewed, automated, and physical-device screen pass—not an external accessibility certification. A formal screen-reader audit, multilingual localization study, and owner/staff usability study have not been run. Disabled controls are covered by the presentation treatment but are excluded from the executable WCAG text-contrast assertion. Browser-only desktop parity is not a release target.

## Plain English

Every important app screen was made easier to read and tap on the real phone. Long forms and dialogs are clearer, tabs fit without sideways scrolling, money stays readable, and messages no longer rely only on color. The test now checks these rules before accepting each screenshot. Nothing about the shop's records, formulas, permissions, or security was changed.
