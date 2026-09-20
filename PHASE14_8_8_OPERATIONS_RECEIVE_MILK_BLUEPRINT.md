# Phase 14.8.8 — Operations Receive milk blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-20

## Where this checkpoint starts

Operations Work home is connected in the debug `NativePreviewActivity` harness. Receive milk / Make yogurt / Record spoilage were preview notices.

This checkpoint owns **Receive milk** entry only: supplier + measurements + physical review + record. It does not implement Make yogurt, Record spoilage, Deliveries, Attendance, or Operations More.

## What the existing product already guarantees

- Operator (and other purchase-capable roles) have `PURCHASE`.
- `VendorPurchaseService.record` / bridge `recordPurchase` require an active Vendor, open business date, positive raw liters, non-negative fat, positive LR; they compute SNF/TS/adjusted liters and amount owed, update Milk stock, and credit the vendor payable.
- Actor attribution is written through the existing bridge command path.
- Financial rates, payables, and amount owed must not reach Operator UI — strip at the native facade, not merely hide in Compose.

## Normal path

1. From Work home, open **Receive milk**.
2. Shared green header shows Receive milk, identity, role, business date, Open/Closed.
3. Choose an active supplier (name only — no balances).
4. Enter raw liters, fat %, and LR reading. Optional note.
5. Review dock shows physical consequences only: SNF %, TS %, adjusted liters (uses TS standard). Never rates, delivery fee, or amount owed.
6. Press **Record receipt** once; button locks as Recording….
7. Success replaces the form with record number, supplier, liters in, SNF/TS/adjusted, and milk stock effect. **Receive another** clears measurements (may keep last supplier).
8. Hardware / UI Back on a dirty form asks before discarding; clean Back returns to Work home.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Receive milk task | Open entry from Work | Connected form (not family preview) | Destination + tags |
| Supplier selector | Open active-supplier chooser | Names only | Opens; select updates |
| Liters / Fat / LR | Accept finite decimals | Review dock updates | Valid/empty/zero/malformed |
| Review dock | Physical preview | SNF, TS, adjusted L | No money strings |
| Record receipt | One purchase command | Saving then success | One write; actor linked |
| Receive another | New draft | Measurements clear; supplier may stay | Reset asserted |
| Back (dirty) | Confirm discard | Draft kept on cancel | Discard contract |
| Back (clean) | Return to Work home | Work context visible | Nav asserted |
| Closed date | Disable record | Closed guidance | Record disabled |

## Native data boundary

`NativeReceiveMilkFacade`:

- require `PURCHASE`;
- load active vendors (id + display name only) and posting-open + TS standard for preview;
- preview SNF/TS/adjusted without loading milk rate or delivery fee into DTOs;
- record through `OperationsBridgeCommandService.recordPurchase`;
- project receipt without `amountOwed`, rates, or payables; include milk quantity added.

## Out of scope

- Make yogurt / Record spoilage forms
- Deliveries, Attendance, Operations More
- Showing supplier balances or purchase amount
- Launcher migration

## Verification

Facade + Compose contract tests; harness `qaScreen -Pqa.screen=receive`; update Work foundation test that previously asserted the family preview. Prove on A06. Keep Cashier `qaCheckpoint` green.
