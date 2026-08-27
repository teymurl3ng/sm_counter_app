# Phase 4 — First UI Vertical Slice

This phase adds the first usable app slice on top of the verified Phase 3 transaction engine.

## Scope

- dashboard: current Milk/Yogurt stock, Cash/JazzCash/Bank balances, vendor balances
- milk purchase form
- vendor selection and minimal vendor creation needed to record a purchase
- live purchase calculation preview using the existing VendorPurchaseCalculator
- save purchase through the existing VendorPurchaseService
- user-facing save errors
- dashboard refresh after a successful purchase
- UI instrumentation coverage for purchase success and invalid LR input

No sales, production, payments, expenses, salaries, corrections UI, reports, networking, or Compose were introduced.
