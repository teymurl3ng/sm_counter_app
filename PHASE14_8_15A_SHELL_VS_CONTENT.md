# Phase 14.8.15A — Native shell vs content correction

Status: **CORE METHOD — absolute, unskippable for all multi-role native entries**

Date: 2026-09-21

Cursor enforcement: `.cursor/rules/native-shell-vs-content.mdc` (`alwaysApply: true`)

## Problem

Owner Work currently opens complete Cashier and Operations screens for shared
commands. Those screens include their original role bottom navigation, so Owner
can see Cashier or Operations chrome and the host Activity must translate or
discard foreign navigation events.

This is form reuse with the wrong boundary. It also splits Back ownership
between the Activity and child screens and lets callback-only launcher tests
claim coverage without proving the hosted result.

## Decision

An entry surface used by two or more roles must be separated into:

1. role-neutral entry content and presentation state;
2. a role-owned shell that supplies navigation, snackbar hosting, and exit
   behavior;
3. a host-level integration test that proves the expected role shell and Back
   path.

Role-neutral content must not depend on `OwnerSection`, `CashierSection`, or
`OperationsSection`. Feature dependency points from the role wrapper to the
shared entry feature, never from the shared feature to a role package.

Dirty entry drafts must guard both hardware Back and bottom-navigation exits.
An exit runs immediately when clean; when dirty, it runs only after explicit
discard confirmation.

## Testing and inventory rule

- A launcher callback contract proves intent only.
- A hosted entry is not covered until a Foundation test opens it inside the
  host role, verifies the host role chrome, rejects foreign chrome, and proves
  Back behavior.
- A shipped control marked `COVERED` in
  `phase18/qa/native_control_inventory.tsv` must name a real workflow test.
  `workflow_test=-` is not acceptable for controls completed by this phase.

## Pilot

Sale is the first correction:

- `CashierSaleScreen` keeps its public contract and Cashier shell.
- role-neutral Sale loading, failure, form, and leave-guard presentation are
  reused by an Owner wrapper.
- Owner Record sale renders Owner navigation and returns to Owner Work.
- Sale facade, ViewModel, domain rules, and write behavior remain unchanged.

## Follow-up sequence

- 14.8.15A.2: Collect under Owner shell. **DONE**
- 14.8.15A.3: Receive Milk, Make Yogurt, Record Spoilage, and Deliveries under
  Owner shell. **DONE**
- 14.8.15A.4: extract role hosts from `NativePreviewActivity`. **DONE**
  (`OwnerPreviewHost`, `CashierPreviewHost`, `OperationsPreviewHost`; Activity wires
  ViewModels/facades/session and selects the host.)
- Later: destination-scoped ViewModels and a stronger local-unit test base.

## Non-goals

- No Navigation Compose migration in this correction.
- No role flattening: Owner, Cashier, and Operations homes stay distinct.
- No changes to business logic, accounting, permissions, or domain models.
- Phase 14.8.16 Owner Records blueprint is next / in draft (`PHASE14_8_16_OWNER_RECORDS_BLUEPRINT.md`) — do not implement Records Compose until that draft is accepted.
