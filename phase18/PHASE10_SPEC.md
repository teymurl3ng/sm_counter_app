# Phase 10 Specification — App Preferences

## 1. Purpose
Provide a small user-facing Preferences area under Settings without turning business rules or integrity protections into configurable options.

## 2. Preference definitions

| Preference | Values | Effect |
|---|---|---|
| Theme | System / Light / Dark | App presentation theme |
| Language | English / Urdu | Persisted selection; Urdu translation deferred |
| Display density | Comfortable / Compact | Shared UI spacing density |
| Default reporting period | Today / This Week / This Month / This Year | Initial analytics dashboard period |
| Default opening section | Dashboard / Budgets & Targets / Backup & Restore / Settings | Initial scroll destination |

## 3. Fixed behavior
Number formatting, date formatting, currency display, week start (Monday), automatic analytics refresh, and integrity warnings are not user-configurable preferences.

## 4. Persistence
All preference values are stored in `milk_shop_preferences` and therefore travel with the Phase 9 full application-state backup/restore.

## 5. Safety
Changing preferences must not mutate transactions, business settings, analytics truth, budgets, targets, or other accounting state.
