# Phase 14.8.10 — Operations Record spoilage blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-20

## Where this checkpoint starts

Operations Work home, Receive milk, and Make yogurt are connected. Record spoilage was still a family preview notice.

This checkpoint owns **Record spoilage** entry only: product + quantity + reason + physical review + record. It does not implement Deliveries, Attendance, Operations More, or supervisor unusual-loss approval.

## What the existing product already guarantees

- Operator has `PRODUCTION_AND_SPOILAGE`.
- `SpoilageService.record` / bridge `recordSpoilage` require an active inventory-tracked product, positive quantity, non-empty reason, and an open business date. Room stock triggers reject insufficient stock.
- Actor attribution is written through the existing bridge command path.
- Financial loss values must not reach Operator UI — strip at the native facade.

## Normal path

1. From Work home, open **Record spoilage**.
2. Shared green header shows Record spoilage, identity, role, business date, Open/Closed.
3. Choose product via the same AlertDialog picker pattern as Collect/Sale (not a full-screen sheet).
4. Enter quantity; current stock is visible beside the field.
5. Choose a concrete reason: spoiled/sour/curdled, spill, contamination, power failure, equipment failure, or other. **Other** requires a note.
6. Review dock shows quantity removed and remaining stock. Never loss money.
7. **Record spoilage** locks as Recording… while saving.
8. Success shows record number, product, quantity, reason, and remaining stock. **Record another** clears the draft (may keep last product).
9. Insufficient stock / closed date keep the draft with guidance.

## Out of scope

- Unusual-loss supervisor approval (bridge/policy not connected yet)
- Deliveries, Attendance, Operations More
- Launcher migration

## Verification

Facade + Compose contract tests; `qaScreen -Pqa.screen=spoilage`; foundation open + record. Prove on A06.
