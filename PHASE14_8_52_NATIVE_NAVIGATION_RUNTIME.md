# Phase 14.8.52 — native navigation runtime

Date: 2026-09-23  
Branch: `codex/native-navigation-runtime`

## Outcome

Primary navigation is now session-retained across Cashier, Operations, Owner, Manager, and Administrator shells. A warm tab switch renders the retained destination and data instead of tearing down its ViewModel, showing a blocking loading screen, and querying Room again.

Owner More destinations now use explicit cold/warm readiness. Products, Customers, Suppliers, Employees, Costing & plans, Shop settings, Users & roles, Security history, and Backup may block on their first load; after a successful first load they render cached state immediately, including legitimately empty results, while explicit refresh remains available.

## Runtime contract

- Root ViewModels close only when the authenticated session changes or ends.
- Reselecting an already selected root keeps the established “return to root” behavior where the shell contract requires it.
- Work entry ViewModels still close when leaving Work, so transaction drafts and write flows retain their existing leave guards.
- Owner/Manager/Admin Records details, Owner Reports, and Owner More destinations survive ordinary primary-tab switches.
- Sensitive transient state never becomes a warm cache: passphrases, restore tickets, re-auth prompts, confirmation dialogs, and editors are cleared when returning to the More menu.
- Safe read snapshots and user-selected filters remain cached for the active session.
- Each shell has a saveable state bucket per primary destination, preserving Compose-owned list scroll and local saveable UI state when a tab leaves composition.

## Concurrency and loading

- `SessionRequestGate` gives each session-bound read a monotonically increasing request token. An older refresh, a prior session, or a completed teardown cannot publish over the current screen.
- Reopening an already Ready or Loading ViewModel for the same session is a no-op.
- Owner Overview no longer performs the previous open-then-refresh duplicate load.
- Independent More context/security reads execute concurrently.
- Owner Costing policy, planning, opening-cost, and yield reads execute concurrently.
- Warm navigation does not show a full-screen loading state. Explicit user refresh can retain existing content while work proceeds.

## Before and after

| Scenario | Before | After |
|---|---|---|
| Warm primary-tab switch | Close → Idle → open → Loading → Room query | Retained Ready state; zero reopen/query cycle |
| Return to an empty Owner More list | Blocking spinner again because empty was treated as “not loaded” | Empty result is marked loaded and reopens immediately |
| Late read from an old refresh/session | Could overwrite newer UI state | Rejected by `SessionRequestGate` |
| Tab leaves Compose composition | Scroll/local saveable state discarded | Restored from the tab’s saveable state bucket |

The pre-change installed preview could not provide a clean timing capture because its existing account-access record fails before role entry. The deterministic baseline above comes from the former host lifecycle (`close` on every switch) and its mandatory reload path. The post-change behavior was verified on the physical device with an isolated seeded database.

## Verification

- `qaFast`: PASS (`966` inventory rows, `0` untested/orphans)
- `:app:testDebugUnitTest`: PASS
- `:app:compileDebugKotlin`: PASS
- `:app:compileDebugAndroidTestKotlin`: PASS
- `:app:lintDebug`: PASS (`0` errors; existing warnings remain report-only)
- `:app:assembleDebug`: PASS
- SM-A065F isolated device regression: PASS
  - opened Owner Products
  - changed the archived filter
  - switched Products → Work → More
  - returned directly to Products
  - observed no Products loading node
  - retained the selected archived filter
- Latest user-facing `.preview` APK installed successfully on SM-A065F. The device’s pre-existing preview account-access failure was preserved; QA used the separate instrumentation identity and did not erase user preview data.

## Regression coverage

- Unit coverage rejects stale refresh, replaced-session, and post-teardown results.
- Owner More unit coverage verifies menu reset clears sensitive/transient state while preserving warm-destination readiness.
- `ownerProductsRemainWarmAcrossPrimaryNavigation` is registered in the Owner More device gate.

