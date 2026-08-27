# Phase 6A — Business Settings

## Scope
Expose the six manually maintained business settings already defined by the frozen logic core and SQL blueprint:

- Vendor milk rate
- Vendor delivery fee
- TS standard
- Retail milk rate
- Retail yogurt rate
- Default delivery surcharge

This phase does **not** add application preferences such as theme, language, notifications, or backup.

## Design
The UI reads and writes the existing `settings` table through a small domain service, `BusinessSettingsService`.

The domain service validates the existing SQL rule (`setting_value > 0`) and updates all six settings inside one Room write transaction. Existing transaction services continue to read the same setting keys, so changing a setting changes future calculations without rewriting historical event snapshots.

## Tests
`Phase6aBusinessSettingsUiTest` verifies:

- all six values can be saved;
- persisted values match exactly;
- the updated retail milk rate feeds the existing Sales UI;
- invalid values are rejected;
- invalid saves do not alter the persisted settings.

The successful-save test restores the original settings in `finally`, so the Phase 6A tests do not contaminate the regression suite.

## Verification
The project was statically checked for XML/Kotlin resource-ID consistency and source-level integrity. Android/Gradle compilation was not executable in this environment because the Gradle distribution was unavailable offline.

The authoritative verification remains the user's Android Studio instrumentation run.
