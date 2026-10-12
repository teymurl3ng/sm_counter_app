# B3b account-correction execution

Implementation and selected-check evidence for [the fixed run scope](../withdrawal-account-return-plan/B3B_RUN_SCOPE.md).

This run changes account truth and its required migration/backup/local replay behavior only. No new UI, returned-money event, ordinary-app installation or remote operation.

Current execution status: [B3b is verified](RESULT.md). The saved verification manifest maps each latest passing case to its run. Earlier failed run logs remain available for review.

Necessary test-boundary addition: `CashRoundingCompatibilityTest` opens its migrated fixture using an explicit migration list. Add 14 -> 15 to that builder so the already selected legacy-close preservation check can open the current database. No rounding rules or assertions change. This is required by B3b's additive schema upgrade, not a new rounding investigation.

Necessary Records read-boundary addition: `NativeOwnerRecordsFacade` still shows the original account for every later withdrawal correction. Once B3b's typed correction or sync replay changes accounts, the existing amount-only editor would display the wrong account. Resolve its prefill from the latest declared truth and disable correction on incomplete legacy history. No account-selector UI is included.
