# Phase 14.8.2 — Counter Ledger refinement after Stitch

Status: **ACCEPTED — final preview corrections applied; native implementation authorized**

Date: 2026-09-19

## Decision

The six Stitch proposals are useful visual studies, but neither Treatment A nor Treatment B is a sufficiently large improvement over the accepted Counter Ledger blueprints to replace them.

The accepted Counter Ledger layouts remain the design foundation. The next design pass will remake those original previews with the strongest lessons from the Stitch study, especially its typography and readability. This is a refinement of the accepted direction, not a new visual direction and not a blend of two finished products.

The Stitch project remains evidence and inspiration. Its generated HTML and screenshots are not native Android implementation artifacts and are not implementation authority.

## What the Stitch study improved

### 1. Typography feels functional

The original previews used type that was too thin in several working areas. It looked elegant at preview scale but did not feel dependable for rapid shop work.

The refined system will use:

- Manrope with Roboto/system fallback in the design references;
- medium or semibold weights for screen titles, field values, selected controls, totals, actions, and navigation labels;
- regular or medium body text rather than hairline/light weights;
- tabular numerals for money and quantities;
- a clear difference between primary text and secondary explanation without making secondary text faint;
- type sizes measured in Android sp during implementation and verified at larger font scales.

Working targets for the next preview:

| Use | Starting target |
|---|---:|
| Main screen title | 26–30 sp, semibold/bold |
| Primary amount or quantity | 30–40 sp, bold, tabular |
| Body and input text | 16–18 sp, regular/medium |
| Field and section labels | 14–16 sp, medium/semibold |
| Bottom-navigation labels | 12–14 sp, medium/semibold |
| Primary action label | 16–18 sp, semibold |

These are starting tokens for the native render, not proof that every label will fit unchanged.

### 2. Contrast and selection are clearer

- Keep the approved ivory, deep green, yellow, and near-black palette.
- Use near-black green #17201B for normal text.
- Muted text begins around #68726C; avoid pale grey for information a worker must read.
- Selected products, entry modes, accounts, and navigation must be obvious without depending on a thin outline alone.
- Icons use a consistent, readable stroke and must not carry meaning without a visible label.

### 3. The transaction result belongs beside the action

For Cashier and Operations entry:

- the review block repeats what will be recorded;
- the total or quantity consequence is visible with the receiving account or stock effect;
- the primary Record action stays in the same viewport at normal text size;
- fixed navigation never covers the action or result;
- saving, success, and correctable failure replace or update that same region, so the worker never scrolls upward to discover what happened.

### 4. Controls must feel easy under pressure

- All touch targets remain at least 48 dp.
- Quantity steppers, toggles, radio rows, Change actions, and expandable rows receive a whole-row target where appropriate.
- Compact does not mean cramped: related controls stay grouped, while decorative padding is reduced before text or touch targets.
- The Cashier path remains product → entry method/value → payment destination → review → record.

### 5. Role hierarchy remains the original plan

The Stitch study does not change the role architecture:

- Owner remains data- and exception-led.
- Cashier remains rapid transaction-led.
- Operations staff remain physical-work-led without restricted financial information.
- Store manager remains focused on today's work, approvals, and close.
- Administrator remains focused on people, setup, access, records, and system health.

Navigation labels, permissions, security boundaries, and task order continue to come from the accepted Counter Ledger blueprints.

## What not to carry forward

- Do not adopt either Stitch treatment wholesale.
- Do not turn every group into a large rounded card.
- Do not use large empty areas on working screens where they push the action below the fold.
- Do not repeat numbered section headings when the control and label already explain the step.
- Do not use thin fonts, faint helper text, tiny navigation labels, or outline-only selected states.
- Do not introduce generated icon-font names, inconsistent system bars, artificial phone frames, or browser-specific layout behavior.
- Do not copy generated HTML/CSS into the Android application or place it in a WebView.

## Revised preview checkpoint

The first refined set is available at [design/refined-counter-ledger/index.html](design/refined-counter-ledger/index.html).

Before production Compose implementation, remake the three original representative previews:

1. branded returning-user Welcome;
2. Cashier New sale, ready to record;
3. Owner Overview, day open with an action needed.

The remake should preserve the original information architecture and role calibration while applying the typography, contrast, touch, and action-visibility rules above. Review the three screens together so typography and density form one system.

After those three are accepted, apply the same tokens and component rules to the already specified connected journeys and material states. Only then begin the native vertical slice.

## Native implementation boundary

Stitch produced screenshots and HTML references. The product target is Kotlin with Jetpack Compose, so the approved design must be recreated as native components:

- Compose theme tokens for color, typography, shape, spacing, elevation, and motion;
- native layouts, text fields, selectors, dialogs/sheets, navigation, system bars, and IME handling;
- existing ViewModels, services, Room data, authorization, offline behavior, backup rules, and security flows;
- Android Back, state restoration, large-text reflow, TalkBack semantics, reduced motion, and device-specific insets;
- physical-device verification before replacing each corresponding WebView route.

This translation is deliberate. The generated HTML is a visual reference, not a shortcut around the native migration.

## Gate

The owner accepted the refined Counter Ledger direction on 2026-09-19 and authorized native implementation. The last requested corrections were applied before implementation began:

- the Cashier paid-now choice became a compact selector instead of a large empty card;
- the quantity and unit share a deliberate baseline;
- `PKR value` became the clearer entry label `Amount`.

Neither Stitch A nor Stitch B is selected. The accepted preview, original role-calibrated blueprints, and the material-state rules in the workflow documents jointly govern the Compose implementation.
