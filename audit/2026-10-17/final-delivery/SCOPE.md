# Final signed delivery and handover

17 October 2026. Baseline `d82e28e`. The owner selected final delivery/handover; automatic customer SMS remains unconfigured and outside this release. No provider purchase, customer-message dispatch, new feature, cloud migration or whole-app audit.

## Frozen work

1. Deliver completed phase 5 statements/PDF/images/manual drafts, phase 6 owner follow-ups/shared coordination and the accepted clerk-language pass as ordinary `com.saaf.milkshop` 0.6.0/code9. Existing ordinary A06 and S24 both report 0.5.0/code8. Preserve signing continuity and use an ordinary replacement update, never uninstall or clear shop data.
2. Before ordinary installation, run only the three upgrade/recovery checks: `FollowUpDataTest#migrationPreservesLedgerAndStartsEmpty`, `restoredJournalIsHeldAndCacheIsNotRestored`, and `malformedRestoreRollsBackAndLegacyBackupStillWorks`. Use the disposable QA package. Existing feature/UI/two-phone/protocol evidence remains credited; version change does not justify repeating it all.
3. Verify release signature against existing certificate, production package/version/non-debuggable build, preview fixtures off, blank test passphrase, source commit/hash provenance and unchanged inputs. Confirm exact APK hash on each installed phone, same installation identity/UID and process launch. Schema 18→19 adds only follow-up tables; inspect migration registration and run the named acceptance before update.
4. Private ordinary-app login and retained records/sync require actual observation. Never manufacture production transactions, reveal credentials, silently activate reminders or claim a synthetic restore proves the owner's private backup. Ask the owner to sign in privately when the update restarts the app.
5. Prepare one current plain-language handover. Credit technical delivery separately from staff readability/motion/vibration/uncoached tasks, next-date observation and independent backup/recovery/signing custody. Finish the parts supported by evidence; leave exact human actions explicitly pending until supplied.

## Research and judgment

[Android ADB](https://developer.android.com/tools/adb) documents `install -r` as replacement retaining data. [Android signing](https://developer.android.com/studio/publish/app-signing) explains update/signing continuity. Existing normal replacement evidence for both named phones is in phase 4's release manifests. These are prerequisites, not proof of the private business rows after migration; ordinary login/records still need observation. The independent-judgment reminder hook was run; it is not external approval.

No destructive downgrade is a rollback plan after schema19. Preserve the functioning installation, use supported encrypted recovery only with an independently verified backup, and prefer a scoped forward fix. Do not infer staff success or custody from an installation report.

## Stop line

Exactly three existing migration/restore methods, release/package/signature checks and ordinary-app handover. No broad regression suite, new QA accounts, server changes or automatic SMS work. Source baseline, release artifacts and install results are recorded separately. Raw device captures/logs and signing material remain excluded from Git.
