# Phase 8 — Budget & Targets

## Status
First implementation checkpoint prepared; Android Studio/device verification required. Phase 8 is not yet closed.

## Final business rules
- Budget = planned maximum spend per category and period.
- Sales Target = weekly, auto-derived from the active weekly Milk Purchases budget: planned liters × retail milk rate snapshot.
- Collections Target = weekly, independently entered.
- Sales counts credit sales immediately; Collections counts actual collection events.
- Milk Purchases budget stores planned liters only; planned spend = liters × vendor milk rate snapshot.
- No auto-repeat. Each period/version is explicitly created. Copy-previous is a convenience shortcut.
- Mid-period revision becomes current immediately; previous version remains viewable.
- Closed periods cannot be revised. A period closes automatically when its end date is reached.
- Old versions are archived/superseded, never deleted. Restoring an old version means creating a new version from its values.
- Business-rate changes do not mutate existing budget rows; new budgets/revisions snapshot current rates.
- Weekly periods crossing calendar months belong wholly to the month containing the majority of their days. Applies to weekly-native Milk/Sales/Collections reporting.
- Planned milk 0 is distinct from no target.
- Variance shows amount and percentage; zero-target percentage is shown as “—”.
- Actuals are obtained through the Phase 7A analytics layer; Phase 8 does not query raw events.
- For closed periods later affected by transaction corrections, the original comparison remains intact and a separate recalculated comparison is created using the budget version active at close.
- Milk actual, Yogurt actual, and combined Milk + Yogurt actual are shown separately in the weekly target view.

## Architectural boundary
Phase 8 is planning state above the frozen accounting core and Phase 7A read-only analytics. Its new writable tables contain planning intent only; they do not create ledger entries or alternate accounting truth.

## Cash planner exclusion
The cash/reserve planner is explicitly outside Phase 8 and remains a future separate phase.
