# Observation outside the current vertical-layout acceptance

At 200% text, changing the Costing section can recreate its horizontally scrolling tab strip. The selected Yield chip's clipped semantics bounds became empty while the action remained visible. Source: `OwnerCostingPlansHubContent` calls its header from section-specific branches, each using the tab strip's `rememberScrollState`. The vertical regression check must measure `OwnerCostingPlansHub`, not an individual horizontally clipped chip.

This is a retained tab-scroll/selection-visibility observation for a later bounded review, not proof that the toolbar moves vertically and not authorization to change production UI in this finish run. No new task is appended to the seven finish batches.
