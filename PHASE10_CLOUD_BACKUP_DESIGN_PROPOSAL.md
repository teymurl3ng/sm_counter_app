# Phase 10.3 — Periodic Encrypted Cloud-Backup Design Proposal

**Status: PROPOSAL COMPLETE — OWNER APPROVAL REQUIRED BEFORE IMPLEMENTATION**

## 1. Decision requested

Approve the following minimum direction for Phase 10.4:

1. Google Drive is the first cloud provider.
2. Automatic backups use Drive's hidden `appDataFolder` and request only the non-sensitive `drive.appdata` scope.
3. The owner connects an owner-controlled business Google account after recent Saaf Milk authentication.
4. WorkManager creates one approximate daily schedule. Wi-Fi is the default network policy, mobile data is an owner option, low-battery/low-storage runs wait, and charging is not required.
5. A new self-contained cloud envelope protects each snapshot with AES-256-GCM. Scheduled work uses a random cloud-vault key wrapped locally by Android Keystore and portably by a separate owner backup passphrase derived through Argon2id.
6. Retention keeps 14 daily, 8 weekly, and 12 monthly verified recovery points, while always preserving at least the three newest verified snapshots.
7. Phase 10.4 implements automatic backup, Back up now, status/history, and safe cloud restore. It does not implement live synchronization.
8. A read-only latest-snapshot owner-phone mode is approved as the only acceptable future multi-device direction, but its UI is deferred until the backup pipeline and Phase 11 recovery/concurrency gates pass.

No Internet permission, OAuth client, Drive dependency, WorkManager dependency, bridge endpoint, or production code is added until this proposal is approved.

## 2. Existing foundation being reused

The current product already provides:

- complete schema-8 backup payload generation;
- portable format-2 Argon2id + AES-256-GCM encryption;
- Android-Keystore AES-256-GCM local snapshots;
- backup compatibility and integrity validation;
- Android-owned file selection;
- one-use, five-minute restore tickets;
- a pre-restore local safety snapshot;
- full-replacement restore and post-restore session invalidation;
- owner permissions, recent authentication, and durable security audit.

Cloud backup must reuse the payload and restore engine. It must not introduce another database, merge algorithm, accounting model, or React-owned file/credential path.

## 3. Recommended product decisions

| Decision | Recommendation | Reason |
|---|---|---|
| Provider | Google Drive API v3 | Matches the approved direction and avoids operating a custom server. |
| Drive location | Hidden `appDataFolder` | It is intended for app-specific data, is accessible only to the authorizing app, and requires the narrow `drive.appdata` scope. Manual visible exports remain available through Android's existing document picker. |
| Google account | Dedicated business account controlled by the owner | Avoids tying recovery to a cashier's or temporary employee's personal account. |
| Saaf Milk authority | Only an enabled owner with recent authentication may connect, reconfigure, disable, disconnect, or restore | Cloud access and full replacement are security-sensitive infrastructure actions. |
| Schedule | Approximately every 24 hours, plus Back up now | WorkManager periodic work is deliberately inexact; the UI must show last success rather than promise an exact clock time. |
| Default constraints | Wi-Fi/unmetered, battery not low, storage not low; charging not required | Protects mobile data and avoids indefinite waiting for a charging window. The owner may explicitly allow metered data. |
| Stale threshold | Warning after 48 hours without a verified cloud backup | Allows one delayed daily run while making a real protection gap visible. |
| Retention | 14 daily + 8 weekly + 12 monthly; minimum three newest verified | Provides short-term mistake recovery and longer disaster-recovery coverage without unlimited growth. |
| Restore | Full replacement only, using the existing inspect/confirm/safety-snapshot/session-invalidation rules | Merge/import would create unresolved identity, ledger, correction, and close-history conflicts. |
| Owner phone | Read-only latest verified snapshot after hardening; never a second writer | Meets remote visibility intent without pretending backup is synchronization. |

## 4. System boundary

```text
Owner setup screen
    -> native Google account selection and Drive authorization
    -> native cloud-vault setup
    -> one unique WorkManager schedule

Worker
    -> acquire one coherent Room snapshot
    -> build the existing complete backup payload
    -> encrypt into a self-contained cloud envelope
    -> persist exact bytes in a private local outbox
    -> obtain a short-lived Drive access token
    -> upload or resume
    -> verify remote size and checksum
    -> mark success and apply retention

Restore screen
    -> list encrypted snapshot metadata
    -> download selected bytes
    -> verify envelope and backup compatibility
    -> issue one-use restore ticket
    -> explicit confirmation
    -> local safety snapshot
    -> full replacement
    -> invalidate all sessions
```

React receives status, non-secret metadata, and user-facing errors only. Backup bytes, Google access tokens, encryption keys, passphrases, Drive upload sessions, and filesystem paths remain native-owned.

## 5. Google authorization and account ownership

1. Google authentication and Drive authorization remain separate from Saaf Milk application users.
2. The owner deliberately selects the Google account through Credential Manager, then grants only `https://www.googleapis.com/auth/drive.appdata` through `AuthorizationClient`.
3. The first request must be interactive and initiated by the owner. Later workers call the same authorization API to obtain a short-lived access token without interaction while the grant remains valid.
4. No OAuth refresh token, server authorization code, client secret, or access token is stored by Saaf Milk. A backend server is not required for this design.
5. If authorization needs user resolution, a background worker must stop cleanly, record `authorization_required`, and notify the owner. It must never launch consent UI from the background.
6. The selected account's stable identity and masked display email may be stored in a dedicated app-private cloud-settings store. This store is excluded from business backups and Android managed backup so a restored phone never silently assumes that it is still authorized.
7. Disabling the schedule cancels work but does not delete cloud recovery points. Disconnecting also revokes Google authorization and removes local wrapped key/account state. Remote deletion is a separate destructive action and is outside the Phase 10.4 minimum.

Google documents that revoking access removes all scopes previously granted to the app for that user, not only the named Drive scope. The disconnect screen must state this before proceeding.

## 6. Storage and privacy

Automatic snapshots use `appDataFolder` rather than a visible My Drive folder.

- Files are hidden from the Drive UI and from other Drive apps.
- They cannot be shared or moved to another Drive space.
- They still count toward the account's storage quota.
- The user can remove the app's Drive data, so the app must treat missing remote files as a recoverable external-state condition.
- Manual portable `.milkbackup` export remains available for an owner who wants a visible independent copy.

Remote filenames and Drive metadata must not reveal the shop name, customer names, balances, or staff identities. Recommended name:

`saaf-milk_<UTC timestamp>_<snapshot UUID>.milkbackup`

Private `appProperties` may contain only operational metadata such as cloud-envelope version, snapshot UUID, vault ID, installation ID, schema version, encrypted-byte size, and content checksum.

## 7. Cloud encryption and recovery key

### 7.1 Why the current two formats cannot be uploaded unchanged

- Device-local snapshots are encrypted by a non-exportable Android Keystore key and cannot be restored after the original phone is lost.
- Portable format-2 snapshots require the passphrase for every encryption operation. Keeping that passphrase available to a background worker would require storing the user's secret.

The cloud design therefore needs a small outer envelope while reusing the existing complete inner backup payload.

### 7.2 Cloud envelope version 1

At cloud setup:

1. generate a random 256-bit cloud-vault key;
2. ask the owner for a distinct cloud-backup recovery passphrase and validate it with the existing backup-passphrase policy;
3. derive a key-encryption key with the approved Argon2id parameters and a fresh salt;
4. wrap the cloud-vault key with AES-256-GCM under the derived key;
5. separately wrap the cloud-vault key with a non-exportable Android Keystore AES key for scheduled local use;
6. erase temporary plaintext key and passphrase buffers where supported.

For every snapshot:

1. generate a new snapshot UUID and fresh 96-bit AES-GCM nonce;
2. build one coherent existing backup payload including security and costing source truth;
3. encrypt it with the cloud-vault key;
4. authenticate an immutable header containing envelope version, snapshot UUID, vault ID, creation instant, inner backup format, database schema, KDF/wrapped-key fields, and ciphertext length;
5. include the passphrase-wrapped vault key in the file so each snapshot remains independently restorable on a replacement device.

The cloud recovery passphrase, unwrapped vault key, and Google token must never enter React, Room, logs, crash reports, WorkManager input/output data, Drive metadata, or another backup.

### 7.3 Passphrase loss and rotation

- If the phone is lost and the owner forgets the cloud recovery passphrase, the cloud files are intentionally unrecoverable.
- The setup UI must require confirmation and clearly explain this consequence.
- Phase 10.4 does not silently reset or recover this passphrase through the Saaf Milk login recovery code.
- A future passphrase rotation creates a new vault/retention chain unless a separately tested bulk-rewrap operation is approved. Old recovery points continue to require their old passphrase until explicitly removed.

## 8. Coherent snapshot requirement

The existing `buildBackupBytes` implementation reads multiple tables in sequence. That is acceptable only when no concurrent write changes related tables between reads. A periodic worker can overlap normal shop activity, so uploading that sequence unchanged is not approved.

Phase 10.4 must add one native coherent-snapshot boundary before cloud upload. The preferred implementation is a Room/SQLite read transaction or an equivalent short application-level gate that gives the serializer one consistent database view without blocking network upload. The transaction ends before encryption and upload begin.

Only the immutable encrypted outbox file is uploaded. Phase 11 then stress-tests backup-during-write, cancellation, process death, large histories, and repeated recovery; it does not postpone the minimum consistency guarantee.

## 9. Scheduling, retries, and idempotency

- Enqueue exactly one uniquely named periodic WorkManager request.
- A settings change updates/replaces that unique schedule rather than creating another worker.
- Run one immediate one-time backup after setup so the owner can verify the connection.
- Periodic timing is approximate and may be delayed by Android power management or unmet constraints.
- Transient network, `429`, and Drive `5xx` failures use exponential backoff.
- Authorization revocation, wrong account, invalid local key, and configuration errors do not retry indefinitely; they enter an action-required state.
- A private local outbox holds the exact encrypted bytes, snapshot UUID, content SHA-256, length, and upload state until remote verification succeeds.
- Retries reuse the same snapshot UUID and bytes. Before creating a file after an uncertain response, query `appDataFolder` for that UUID in private `appProperties`.
- Resumable upload state may be reused while valid; an expired upload session is restarted with the same snapshot identity.
- A completed upload is accepted only after remote binary size and Drive checksum match the local encrypted file. If Drive does not return the expected checksum, download and compare the encrypted bytes before marking success.
- The worker never modifies business data. Its security audit actor is `system/scheduled_backup`, with a non-authoritative snapshot of the owner who enabled the schedule.

## 10. Retention and quota behavior

Retention runs only after a new snapshot is remotely verified.

1. Preserve the newest 14 daily recovery points.
2. Preserve the newest verified point from each of the previous 8 ISO weeks.
3. Preserve the newest verified point from each of the previous 12 calendar months.
4. Never reduce the set below the three newest verified snapshots.
5. Never delete the last known-good snapshot because a new upload failed.
6. Delete only files carrying the expected Saaf Milk private properties and vault identity.
7. Treat deletion failures as cleanup warnings, not backup failures.

Drive permanently deletes `appDataFolder` files rather than moving them to trash. Retention selection and the minimum-three rule therefore require focused tests before implementation is accepted.

If quota is exhausted, keep the pending encrypted outbox file, preserve all existing remote backups, show a quota-specific recovery action, and retry only after the owner resolves storage or explicitly requests another attempt.

## 11. Status and notification policy

The Backup workspace should show:

- connected account;
- enabled/disabled state;
- schedule and network policy;
- last attempt and last verified success;
- age of latest verified backup;
- latest failure category and recovery action;
- remote recovery-point count and newest/oldest dates;
- Back up now, disable, disconnect, and restore controls according to native permission.

Do not notify after every normal success. Notify when:

- authorization or owner action is required;
- no verified backup exists 48 hours after setup;
- the latest verified backup becomes more than 48 hours old;
- repeated transient failures persist for 24 hours;
- quota prevents protection;
- a manual Back up now or restore succeeds/fails while the user is waiting.

If notification permission is unavailable, the same condition remains visible as a persistent in-app warning.

## 12. Cloud restore

Cloud restore preserves the existing destructive-operation boundary:

1. owner signs into Saaf Milk and recently reauthenticates;
2. app obtains Drive authorization and lists only recognized cloud snapshots;
3. owner selects a recovery point and enters its cloud recovery passphrase;
4. native code downloads, decrypts, authenticates, and checks app/schema compatibility;
5. native code returns only non-secret inspection metadata and a one-use in-memory restore ticket;
6. owner explicitly confirms full replacement;
7. app creates and verifies a local safety snapshot;
8. restore engine replaces business, planning, preferences, and security state;
9. all sessions are invalidated and the app returns to bootstrap/sign-in;
10. cloud scheduling remains disabled on the restored device until the owner deliberately reconnects it.

No merge, partial cloud import, automatic restore, or background restore is allowed.

## 13. Owner-phone decision

Recommended decision: approve a future **read-only latest verified snapshot** mode, but exclude it from Phase 10.4.

The later viewer must:

- use the same owner-controlled Google account and explicit cloud recovery passphrase;
- download only a verified snapshot;
- expose no transaction, correction, settings, user-administration, or restore command;
- never schedule uploads or claim that displayed data is live;
- prominently show the snapshot creation time and stale state;
- keep decrypted data memory-only or in a separately encrypted read-only cache;
- require a fresh deliberate switch before the device can become a writable restored installation.

True two-writer synchronization, conflict resolution, and successive restore between active phones remain unapproved.

## 14. Proposed implementation boundaries after approval

- `CloudBackupProvider` — provider-neutral native list/upload/download/delete contract.
- `GoogleDriveCloudBackupProvider` — Drive `appDataFolder` implementation only.
- `CloudBackupCryptography` — cloud envelope and local/passphrase key wrapping.
- `CloudBackupOutbox` — private immutable pending/verified files and state.
- `CloudBackupCoordinator` — coherent snapshot, upload, verification, retention, and audit orchestration.
- `CloudBackupWorker` / `CloudBackupScheduler` — WorkManager integration.
- `CloudBackupSettingsStore` — device-local provider/schedule/status state excluded from backups.
- focused bridge handler and DTOs — status/configuration only; no token, key, passphrase, bytes, or raw path.
- focused React Backup workspace additions — rendering and user intent only.

The bridge contract would require an additive version amendment after approval. The current contract v3 and its 78 operations remain frozen until then.

## 15. Verification gates

### Phase 10.4 minimum

- cloud-envelope encryption, wrong-passphrase, tamper, nonce, and self-contained replacement-device recovery tests;
- coherent snapshot proof while a business write is attempted;
- unique scheduling and settings-update tests;
- retry reuses exact bytes and snapshot UUID;
- uncertain-response duplicate prevention;
- upload checksum/size verification;
- retention boundary and minimum-three tests;
- authorization-required, revoked-account, quota, offline, and low-storage states;
- cloud restore safety snapshot, full replacement, and session invalidation;
- bridge secrecy/permission tests and packaged UI test;
- complete host and isolated-device regression.

### Phase 11 hardening

- sustained concurrent writes and backup-during-write;
- cancellation/process death at each outbox/upload/verification stage;
- repeated recovery and repeated wrong-passphrase behavior;
- large-history memory, duration, and network measurements;
- missed schedules, reboot, clock/time-zone changes, prolonged offline use, and retention races;
- account switch, revocation, reinstall, Keystore loss, and stale local state;
- actor preservation and full restore verification across multiple generations.

### Phases 14–16

- real production OAuth configuration and consent-screen review;
- real Drive periodic run and offline retry on physical devices;
- second-device restore with the release signing identity;
- optional owner-phone read-only acceptance if separately implemented;
- privacy, quota, support, recovery, and release-operational documentation.

## 16. Explicit exclusions

- live synchronization;
- two writable phones;
- automatic restore;
- merge restore;
- React-managed OAuth, keys, bytes, or filesystem paths;
- broad Drive access;
- service-account ownership;
- storing Google refresh tokens on the device;
- uploading Android-Keystore-only local snapshots;
- deleting remote backups when merely disabling the schedule;
- claiming an exact daily execution time.

## 17. Approval options

The recommended package can be approved as written, or amended on these product choices:

1. hidden `appDataFolder` versus a visible app-created Drive folder;
2. Wi-Fi-only default versus any-network default;
3. daily cadence and the 48-hour stale warning;
4. 14-daily / 8-weekly / 12-monthly retention;
5. separate cloud recovery passphrase and cloud-vault envelope;
6. future read-only owner-phone snapshot mode versus no owner-phone mode.

Implementation begins only after the owner approves these decisions.

## 18. Standards and API basis

- [Android authorization guidance](https://developer.android.com/identity/authorization) — user-initiated authorization, `AuthorizationClient`, short-lived access tokens, revocation, and account identity behavior.
- [Android WorkManager task scheduling](https://developer.android.com/develop/background-work/background-tasks/persistent) — persistent, deferrable work across app restarts and device reboots.
- [Defining WorkManager requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) — inexact periodic work, constraints, retries, and backoff.
- [Managing unique WorkManager work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work) — duplicate-safe unique periodic scheduling.
- [Google Drive application data folder](https://developers.google.com/workspace/drive/api/guides/appdata) — hidden app-specific storage and the `drive.appdata` scope.
- [Google Drive scope guidance](https://developers.google.com/workspace/drive/api/guides/api-specific-auth) — least-privilege scope selection and token security.
- [Google Drive upload guidance](https://developers.google.com/workspace/drive/api/guides/manage-uploads) — resumable uploads and retry behavior.
- [Google Drive file resource](https://developers.google.com/workspace/drive/api/reference/rest/v3/files) — private app properties, content length, and binary checksums.
- [Google Drive delete semantics](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/delete) — permanent deletion behavior used by retention.

## Plain English

The shop phone will make an encrypted recovery copy roughly once per day and place it in a private part of the owner's Google Drive. The backup can still be recovered on a replacement phone with the owner's separate backup passphrase. A failed or delayed upload never stops shop work and never deletes the previous good copy. This remains backup—not live sharing—and a second owner's phone may later view the latest saved copy only after the recovery system has been thoroughly tested.
