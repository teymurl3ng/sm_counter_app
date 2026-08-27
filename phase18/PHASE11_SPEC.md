# Phase 11 UI / UX Specification

## Objective
Transform the existing functional/prototype-oriented UI into a coherent production-quality Android application without changing established business behavior.

## 11A — Design System Foundations
- Android/Material-style conventions are the baseline.
- Use a restrained, component-driven visual language inspired by the discipline of systems such as shadcn/ui, but implemented with the existing Android View/XML stack.
- Centralize semantic colors, spacing, typography, touch targets, radii, and reusable surface/control styles.

## 11B — Navigation & Information Architecture
- Provide a persistent top navigation entry point.
- Group screens by user intent rather than presenting one undifferentiated long page.
- Initial destination groups:
  - Dashboard
  - Budgets & Targets
  - Transactions
  - History & Corrections
  - People & Accounts
  - Settings
  - Backup & Restore
- The first navigation checkpoint may use section anchors in the existing ScrollView to minimize regression risk.
- Later checkpoints may extract true destinations once shared presentation-state ownership is defined.

## Phase-wide constraints
- No business logic changes merely for UI work.
- No SQL/schema changes without explicit approval.
- No alternate accounting formulas.
- Existing regression tests remain mandatory after every checkpoint.
- Visual correctness must eventually be verified from rendered emulator/device screenshots, not inferred solely from XML/Kotlin.


## 11D — Transaction Screens

Transaction screens are migrated using the shared 11A design tokens and the established 11C contrast rules. Each transaction checkpoint preserves all existing business logic, calculations, validation, IDs, persistence behavior, and event semantics.

### 11D-1 Purchase
- Dedicated purchase card/surface.
- Shared headline, muted subtitle, input, and primary/secondary button styles.
- Calculation preview presented as a visually distinct information surface.
- Existing purchase behavior unchanged.


## 11D-1 stronger visual redesign
The first Purchase styling checkpoint was judged too conservative because it mostly changed colors and surfaces without sufficiently improving composition. The current 11D-1 implementation therefore adds explicit field labels, semantic section grouping, deliberate spacing, aligned summary content, and stronger primary/secondary action hierarchy. This is still presentation-only.

## 11D transaction screen migration

The Purchase screen establishes the approved visual pattern for daily transaction entry. The same presentation system is applied to Yogurt Production, Vendor Payment, Sale, Credit Collection, Expense, and Salary Payment in a single visual migration checkpoint.

Required presentation qualities:
- surface/card grouping for each transaction form;
- clear title/subtitle hierarchy;
- consistent field sizing and vertical rhythm;
- aligned labels and controls;
- readable widget states;
- clear primary/secondary action hierarchy;
- calculation/summary surfaces where the existing screen has a calculation preview.

The migration must preserve all existing view IDs, event listeners, validation, calculations, services, SQL/database behavior, and accounting semantics.


## 11E Navigation contract
- Each top-level navigation item switches to a distinct screen container.
- Only the selected screen is visible.
- The app bar section label reflects the selected destination.
- No business or persistence behavior changes are implied by navigation.
- The existing default-opening preference remains compatible with its previously supported destinations.
