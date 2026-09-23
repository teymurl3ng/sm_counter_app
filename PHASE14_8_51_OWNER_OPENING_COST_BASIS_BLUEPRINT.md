# Phase 14.8.51 — Owner opening stock cost basis

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-23

## Scope

Connect the existing authoritative `OpeningCostBasisService` to the native Owner and Administrator Costing & plans surface. This slice changes no Room schema, costing rule, permission, reporting formula, or backup format.

## User workflow

1. Add an **Opening** section to Costing & plans.
2. Read the current effective opening state and show only positive Milk/Yogurt quantities supported by the costing domain.
3. Each row shows the product, immutable opening quantity, and either:
   - the active total cost, evidence kind, and basis identifier; or
   - a clear missing-cost warning explaining that opening stock cannot be fully valued.
4. **Add cost** or **Correct cost** opens a focused editor under the current Owner/Admin shell.
5. The editor keeps opening quantity read-only, accepts total PKR cost, selects documented historical evidence or owner estimate, and requires an explanation for estimates.
6. Save uses typed `OpeningCostBasisInput`. A stale recent-auth window opens the existing non-saveable passphrase confirmation and retries the same input after successful reauthentication.
7. Success closes the editor and refreshes the visible row. Corrections retain the superseded basis in history.
8. Android Back and visible Back protect a dirty draft through the standard discard guard.

## Material states

- No opening state: direct the user to Shop settings.
- Opening state without positive Milk/Yogurt stock: explain that no supported opening quantity needs a basis.
- Missing basis: visible valuation-coverage warning.
- Existing basis: active amount, evidence source, and correction affordance.
- Saving, validation error, recent-auth challenge, successful refresh, and safe cancellation.

## Safety boundaries

- Never create a purchase or synthetic business event.
- Quantity comes from the effective opening state and is not editable here.
- Only Milk and Yogurt are supported, matching the existing domain service.
- Owner estimate requires evidence notes.
- Owner and Administrator use the same typed facade with native permission enforcement.
- Secrets remain non-saveable and are wiped by the existing reauthentication boundary.

## Acceptance gate

- Facade tests prove read, initial record, correction history, permission rejection, and recent-auth enforcement.
- Compose contract tests prove the Opening section, missing/existing states, editor validation, typed save input, and leave guard.
- Native control inventory has no untested or orphaned opening-basis controls.
- `qaFast`, Kotlin compilation, focused device tests, debug/release assembly, and installed native-preview verification pass.

## Verification

- `qaFast`: **PASS** — inventory `966`, untested `0`, orphans `0`.
- Kotlin debug and Android-test compilation: **PASS**.
- Focused facade + Compose contract suite: **34/34 PASS** on SM-A065F, including 1.3× font-scale tab reachability, direct Shop settings recovery, explicit invalid-amount/evidence states, initial save, correction history, authorization, recent-auth retry, and dirty-draft protection.
- Forced-fresh debug assembly and release assembly: **PASS**.
- Fresh debug/native-preview APK installed successfully as `com.saaf.milkshop.preview`; on-device Admin verification reached **More → Costing & plans → Opening** and showed the direct **Open Shop settings** recovery action.
- Independent Impeccable finish review: **APPROVED**.
