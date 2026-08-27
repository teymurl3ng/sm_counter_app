# Phase 10 Architecture Audit

- App Preferences are presentation/default-state concerns only.
- No core accounting code or canonical SQL meaning was changed.
- Preferences reuse the existing `milk_shop_preferences` store already handled by BackupManager.
- No new database tables are introduced.
- Theme and display changes are applied at Activity recreation using persisted preference values.
- Default analytics period is selected before the initial analytics refresh.
- Default opening section scrolls the existing main ScrollView to the selected section.
- Urdu is selectable but translation resources are intentionally deferred.
