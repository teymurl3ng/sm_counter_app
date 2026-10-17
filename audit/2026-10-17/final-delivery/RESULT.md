# Signed 0.6.0 delivery — A06 complete, S24/private handover pending

17 October 2026. Frozen [scope](SCOPE.md). [Plain-language handover](START_HERE.md).

## Completed

- ✅ ~~Release version 0.6.0/code9 and signed ordinary APK.~~ Production source commit `109bc9d15f1059a7bc98c7fc1b7c89056b8bcabb` includes the accepted features through `d82e28e`, with only the version increment in app code for this delivery. [Release manifest](RELEASE_MANIFEST.json) records tracked production-input hashes, package/version, schema19 and unchanged signing certificate.
- ✅ ~~Three affected upgrade/recovery methods~~ pass on A06's isolated QA package. [Selection](UPGRADE_SELECTION.json), [results](upgrade-results.json). The schema18→19 migration retains the seeded financial amount and starts new follow-up tables empty. Encrypted restore holds original unfinished commands without restoring stale cache or authorizing a message launch. Malformed restore rolls back and the supported legacy backup path remains usable. No whole-app suite or repeated cloud/two-phone feature test ran.
- ✅ ~~Release/package/signature checks.~~ `apksigner verify` passes; certificate is `9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1`. The production package is non-debuggable, preview fixtures are off, test passphrase is blank and embedded Git SHA matches the source commit. All recorded production input hashes remained unchanged during build.
- ✅ ~~A06 ordinary update, exact APK hash and retained installation identity.~~ [Install evidence](RELEASE_INSTALLS.json). Existing `com.saaf.milkshop` changed from0.5.0/code8 to0.6.0/code9 using `install -r --user 0`. UID11498 and first-install time2026-10-10 19:31:03 remain unchanged. Installed APK bytes match the release; native process stays alive.
- ✅ ~~A06 ordinary startup/account-list observation.~~ [Non-sensitive facts](ORDINARY_STARTUP.json): the ordinary app is visible, Choose account opens after Continue, and an existing Owner/full-passphrase option is present. No account was selected and no credential was entered. Account labels, balances and customer names were not recorded in the evidence. Account-list presence is not proof of retained financial rows.
- ✅ ~~Current handover guide.~~ It distinguishes ordinary app from test packages, manual sending from owner alerts, local/server/backup credentials, deliberate opt-in, known-record/sync acceptance, staff practice and independent backup/signing custody.

Release artifact: `Saaf-Milk-0.6.0.apk`, SHA-256 `5cf0f0b8d1f3635137e3118e7b21ec8517d6937cd191ceace3b651440b4aa860`. APK and raw captures/logs remain local and excluded from Git. No signing material, private credential or shop backup was exported to the repository.

## Still pending — exact human/device boundary

1. **S24 reconnect/update.** Both phones were connected and on0.5.0 at preflight. S24 was locked/off, then disappeared from ADB during the build. One discovery check after the failed S24 install lookup found only A06; the later inventory still showed only A06. No S24 replacement operation ran. Reconnect/unlock it, then run `install-release.py S24` against this exact artifact; do not repeat A06's installation.
2. **Private ordinary-app acceptance.** Owner signs in privately on A06, checks a familiar Overview value and record/customer against existing records, and leaves Overview or Sync open for the bounded observation. Confirm intended PROD shop and an ordinary sync. No fabricated business transactions, credential changes, contract activation or real customer messages are authorized by this check. Financial retention/live sync are not yet claimed.
3. **Human staff check.** Actual phone-security cancel/success, readability/motion/vibration, uncoached practice and the next actual supported-date observation remain the user's/staff's outcomes. Use separate Shop Day QA for invented transactions, not the real shop. Existing technical acceptance stays credited.
4. **Independent custody/recovery.** The owner's answer about independent encrypted shop backup, correct recovery secret and separate signing-material custody is pending. No independent backup was copied or privately recovered during this run. Current signing success does not certify the independent backup. Select/verify a private destination/custodian and supported isolated recovery before marking this complete; never replace the sole live database as a test.

## Build interruption and stop line

The first QA build and its exact retry failed because Windows held generated `classes.jar` open. No source fix or broad cleanup was made. Stopping the Gradle daemon and retrying with in-process compilation/no persistent daemon completed successfully; then the three named tests passed. The ordinary release built successfully in2m50s, including required release lint/package checks. Existing toolchain/deprecation/native-access warnings were nonfatal.

No server mutation, account reset, shop opt-in, automatic SMS sender, provider purchase or customer contact occurred. No ordinary app was uninstalled or cleared. Phase1 is partially complete: A06 delivery and the guide are done; S24/private/staff/custody remain as above. Phase6's separate automatic-SMS route decision is still open.
