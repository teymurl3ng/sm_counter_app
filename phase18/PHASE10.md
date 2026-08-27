# Phase 10 — App Preferences

## Status
Implementation checkpoint: first build.

## Scope
App Preferences are user personalization settings only. Business rules and integrity safeguards are not user-configurable preferences.

### Included
- Theme: System default / Light / Dark.
- Language selector: English / Urdu. Urdu translation is deferred; the selection is persisted for future localization work.
- Display density: Comfortable / Compact.
- Default reporting period: Today / This Week / This Month / This Year.
- Default opening section: Dashboard / Budgets & Targets / Backup & Restore / Settings.

### Excluded
- Number-format preference: formatting is automatic and appropriate to the data type.
- Date-format preference: standard readable date presentation is fixed.
- Currency-format preference: Pakistani rupee presentation is fixed.
- Week-start preference: Monday is a fixed business rule.
- Dashboard refresh preference: automatic refresh is built into app behavior.
- Integrity/safety preferences: safeguards are mandatory and cannot be disabled.
- Backup/restore controls: owned by Phase 9 unless a genuine capability gap is found.

## Storage
Preferences are stored in the existing `milk_shop_preferences` SharedPreferences store so they are included by the existing Phase 9 full-backup preference snapshot.

## Architectural rule
Preferences affect presentation and defaults only. They must not alter accounting truth, business calculations, or the immutable core logic/SQL blueprint.
