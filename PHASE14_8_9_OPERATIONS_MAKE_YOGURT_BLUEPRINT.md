# Phase 14.8.9 — Operations Make yogurt blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-20

## Where this checkpoint starts

Operations Work home and Receive milk are connected in the debug `NativePreviewActivity` harness. Make yogurt was still a family preview notice.

This checkpoint owns **Make yogurt** entry only: milk used + yogurt produced + physical review + record. It does not implement Record spoilage, Deliveries, Attendance, or Operations More.

## What the existing product already guarantees

- Operator (and other production-capable roles) have `PRODUCTION_AND_SPOILAGE`.
- `YogurtProductionService.record` / bridge `recordProduction` require positive milk liters used and positive yogurt kg, Milk + Yogurt products present, and an open business date. Room stock triggers reject insufficient milk.
- Actor attribution is written through the existing bridge command path.
- Financial rates, margins, and loss values must not reach Operator UI — strip at the native facade.

## Normal path

1. From Work home, open **Make yogurt**.
2. Shared green header shows Make yogurt, identity, role, business date, Open/Closed.
3. Context shows milk on hand and yogurt on hand (quantities only).
4. Enter milk liters used and yogurt kg produced. Optional note.
5. Review dock shows the physical conversion (`20 L → 15 kg`) and remaining milk after the batch. Never rates or cost.
6. Primary action label includes the conversion, e.g. **Record 20 L → 15 kg**; locks as Recording… while saving.
7. Success replaces the form with record number, conversion, and updated stock labels. **Make another** clears measurements.
8. Insufficient milk keeps the draft and shows available quantity. Closed date disables record.
9. Hardware / UI Back on a dirty form asks before discarding; clean Back returns to Work home.

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Make yogurt task | Open entry from Work | Connected form (not family preview) | Destination + tags |
| Milk used / Yogurt produced | Accept finite decimals | Review dock updates | Valid/empty/zero/malformed |
| Review dock | Physical conversion | Liters → kg + remaining milk | No money strings |
| Record conversion | One production command | Saving then success | One write; actor linked |
| Make another | New draft | Measurements clear | Reset asserted |
| Insufficient milk | Preserve draft | Available quantity guidance | Error asserted |
| Back (dirty) | Confirm discard | Draft kept on cancel | Discard contract |
| Back (clean) | Return to Work home | Work context visible | Nav asserted |
| Closed date | Disable record | Closed guidance | Record disabled |

## Native data boundary

`NativeMakeYogurtFacade`:

- require `PRODUCTION_AND_SPOILAGE`;
- load posting-open + milk/yogurt presence + on-hand quantity labels (and milk liters numeric for insufficient checks);
- record through `OperationsBridgeCommandService.recordProduction`;
- project receipt without rates, cost, or margins.

## Out of scope

- Record spoilage form
- Deliveries, Attendance, Operations More
- Yield-policy / loss-assessment financial surfaces
- Launcher migration

## Verification

Facade + Compose contract tests; harness `qaScreen -Pqa.screen=yogurt`; foundation open + record tests. Prove on A06. Keep Cashier `qaCheckpoint` green.
