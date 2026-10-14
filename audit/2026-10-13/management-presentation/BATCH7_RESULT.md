# Phase 3 batch 7 — management presentation

Completed the mapped presentation in five implementation files. Costing policy is hosted in OwnerCostingPlansScreen; there is no separate OwnerCostingPolicyScreen in this source.

| Family | Delivered |
|---|---|
| Backup | Grouped schedule/account/status facts and local/cloud inspection facts; outlined cloud recovery-point rows; default hub action says Done through its existing callback |
| Sync | Grouped status with distinct responsive labels/values; review entries have their own surface and stacked full-width decision buttons; row dividers removed |
| Costing / policy | Grouped policy facts and component facts; conversion cost separate from status and review reason; component amount and kg basis remain complete |
| Reports | Grouped period totals; product headings and coverage have separate lines; money facts emphasized; redundant dividers outside product surfaces removed |
| Security history | Grouped read-only details; action/result/actor/time/preview have separate reading roles |

Cold failure panes in Backup, Costing, Reports and Security history now use the existing NativeLoadFailurePane with their error/retry tags and callbacks. Sync already used that pane. Loading, empty, warm-error, not-linked/not-available, superseded, busy and recovery controls retain their existing eligibility. No service/model/schema, backup/restore execution, sync transport, report calculation, approval or form-validation logic changed.

Verification used the isolated QA app on S24 (SM_S921W). Both APKs built successfully. Twenty selected existing contracts passed across the five families, including retry in every family, Backup inspection/Drive eligibility, Sync review callback, policy detail/superseded state and report empty state. Three actual Sync presentation fixtures passed at light 100%, light 150% and dark 200%, checking complete long shop/account/cursor values, both review decisions and disabled review/Sync-now buttons while busy. The test scaffold's initial positional-argument compilation error was corrected to named arguments before any fixture ran. `git diff --check` passed.

Four Sync status/review captures were pulled and visually inspected at light 100% and dark 200%. They remain local under `output/batch7/`; complete facts wrap and decision controls remain reachable. The shared header date abbreviation at 200% remains the earlier phase-5 observation. This verifies representative screens and callbacks, not every theme/font/state combination or real server backup/recovery.

Deliberate scope decisions: the Overview balances, Operator Today long-text and optional blind-count receipt tails remain phase-5 capture/acceptance items. Overview still has horizontal BalanceRow presentation; this run does not establish a defect there. No redesign was justified from this batch's evidence. Failure panes outside the five mapped families stay in the retained M20 queue. Other Costing editors and sections were not restyled merely because they share the host file.

Phase 3 is complete at its bounded implementation/QA scope; representative phone/day acceptance remains phase 5. Batches 6 and 7 are local, following the pushed d3f962f checkpoint. The isolated QA build is installed on S24. No full suite, ordinary release install, commit or push was performed in batch 7.

Next bounded unit: phase 4 batch 8a, protected credential-reset policy. Scope its caller/target matrix and supported administration paths before editing; deployment remains a separate concrete action.
