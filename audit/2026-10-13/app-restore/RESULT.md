# 8b4 complete at bounded QA scope — current app restore

Ten exact selected methods passed on the A06 `R8VXA02SNKH` / SM_A065F, in the separate `com.saaf.milkshop.qa.tests` instrumentation app. Build succeeded; the selected instrumented run completed in 16.321 seconds with `OK (10 tests)`. The runner skipped legacy UI account bootstrap; no broad suite ran. Installed APK hashes and relevant source hashes are recorded in `source-and-apk-hashes.json`.

## Verified behavior

- Existing encrypted-security tests verify credential/recovery state, sessions invalidated on successful restore, costing/close actor context, encryption opacity, wrong passphrase and tampering refusal.
- Existing profile/opening/close tests verify customer/vendor/employee/attendance/spoilage/category state, shop identity, opening corrections and balances, and close/reopen/reclose history.
- Existing bridge/planning/hardening tests verify stable server references become `restored_unverified`, different-shop refusal preserves enrollment, same-shop success clears remote tokens while retaining configuration/binding, and planning-only restore retains unrelated unsent work without blind push of restored planning.
- One new focused method, `CurrentAppRestoreAcceptanceTest.encryptedFullRestorePreservesRowsPreferencesAndReopenedState`, adds independent complete event/inventory/financial entity equality, product/account/profile/store/attendance/spoilage equality and typed preference equality. The synthetic purchase/loss fixture independently expects 9 liters remaining. Wrong-passphrase actual restore preserves destination products/events/preferences and its active session. Successful restore invalidates the stale session, restores all five preference types, creates a pre-restore safety snapshot, and preserves exact transaction rows, expected stock and working local Owner login after closing/reopening the isolated file-backed database.

The source is in-memory; the destination uses the real database factory with only its test context database path redirected to a unique private cache file. The test checks that this file exists before reopening. It never restores the instrumentation application's main database or ordinary app database. Credentials/passphrases and row content are synthetic; no plaintext real credentials or real backups were captured.

## Preservation and cleanup

Ordinary, preview and isolated preview-QA package versions and install timestamps match before/after exactly. Their data was not read or restored. The app and test APK target are independently confirmed by build metadata and installed instrumentation. No server, Drive object or signing key was accessed. The synthetic portable backup, database/WAL/SHM files were removed by the test. Room retained its synthetic `.db.lck` after close; the bounded cleanup helper removed only that filename after an exact prefix/type check. Final cache verification finds no `restore_acceptance_` artifacts. Existing test-package safety snapshots from prior selected methods are outside that unique acceptance-file cleanup; no general cache/backup delete ran.

No product behavior source changed. Only the new instrumentation test and evidence/operator note were added. Initial ADB start was sandbox-blocked; the normal local ADB server/device check succeeded with escalation. All ten methods passed on the first instrumented run. No rerun was needed for the harmless lock-file cleanup.

## Scope and remaining acceptance

[RECOVERY_NOTE.md](RECOVERY_NOTE.md) separates portable app backup passphrase/local account credentials, newly issued server credentials, and the Android signing key/passwords. It explains replacement/safety snapshots, device-local Keystore limits and restored sync revalidation. Historical actual Drive restore and SQL-commit interruption proofs remain credited from earlier reports; they were not repeated or relabeled current.

This completes A2/current isolated app recovery and the 8b4 operator-note portion. It does not certify current Drive access, forced process death, physical new-phone Keystore behavior, ordinary installation upgrade, signing from a recovered key, hosted server Auth recovery or two-phone convergence. O6 recovered-key signing remains phase 6; O1 hosted identity acceptance/publication remains pending on the exact proposal. Those limits do not reopen the selected successful app restore comparisons.

**Next existing bounded unit is 8c shop setup preflight:** inspect only the intended setup/access/lock/disconnect/revocation surfaces, select affected checks and document the actual supported trust boundary before execution. No central identity redesign or whole-app crawl. Phase 5 real shop-day/phone acceptance and phase 6 accepted release remain separate.
