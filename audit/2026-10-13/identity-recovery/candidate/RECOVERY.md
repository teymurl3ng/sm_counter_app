# Identity recovery into an empty isolated destination

This restores account-server identities. App ledger/Drive backups and the Android signing key are separate recovery needs. The former emergency in-place wipe is disabled, including its old Python entrypoint and command flags. Shared DEV and PROD are not rehearsal destinations.

## Prepared state and acceptance

The recovery source is locally prepared and tested against native PostgreSQL with synthetic Auth responses. It is not published, deployed or accepted against real Supabase Auth. Prior historical DEV rehearsal is not proof for this revised source. A separately approved empty project and its current schema/permissions must be verified before the live rehearsal. The weekly identity snapshots remain sequential captures rather than atomic SQL backups.

## Portable source

Business/shop/staff UUIDs, staff status and termination, assignments, shop timezone, timestamps and semantic audit history (including nullable actor ID/name and detail) are retained. PostgreSQL audit bigint IDs are regenerated. Linking new Auth identities invokes the normal staff update trigger, so `updated_at` advances at linking; the portable source retains the original timestamp. No last-Owner or reset guard is disabled.

Portable exports exclude old Auth user IDs, passwords/credentials/hashes, tokens, API keys and env files. They still contain private staff names and audit content; protect them and keep them off the public repository. Credentials cannot be recovered from the identity snapshot; non-terminated active and disabled staff receive new credentials. Terminated staff retain their reserved usernames and have no Auth identities. Disabled users must still be denied by the actual profile/access policy; verify that in the live rehearsal.

Before export, stop identity administration and establish that no reset dispatch or reconciliation is unresolved. Keep maintenance in effect throughout capture. Install the matching recovery source-status RPC on the source only as part of an approved deployment. The CLI checks status before and after collection; these checks do not replace maintenance or establish a transactional capture:

```text
python admin.py --env dev export --source-quiesced
```

Legacy portable exports without this evidence are refused. Re-establish a source export or perform a separately reviewed conversion; do not add the flags based only on a successful backup job.

## Raw scheduled snapshot conversion

Use a complete format-1 manifested snapshot with matching source project, seven payload count/hash checks and consistent linked Auth IDs. The converter never restores its Auth payload. Private reset state is absent from scheduled snapshots: the operator must establish that maintenance and no unresolved resets applied **at capture time**. If that evidence is absent, capture a new portable source in maintenance.

```text
python convert_identity_snapshot.py --snapshot <private-folder> ^
  --expected-source-ref <source-project-ref> --source-quiesced ^
  --no-unresolved-resets --output <new-private-portable.json>
```

Any reconciliation rows cause refusal rather than silent omission. Resolve and recapture, or separately scope their recovery. Completed private reset request mechanics are not restored; completed reset audit history is retained. Manifest-free snapshots such as 9 October need a separate reviewed source-verification path and are not accepted by this converter. Output refuses overwriting an existing file.

## Destination preparation

Create a separately authorized fresh isolated Supabase project. This run did not create one or establish a cost allowance. Keep it in maintenance, inaccessible to staff and app writers, throughout import, Auth recreation and acceptance.

Use the new recovery bootstrap, which opts out of the sample business/shop seeds, loads the existing current bootstrap/C1.6/sync schema, then the protected-reset migration and the safe-empty-restore migration:

```text
psql -v ON_ERROR_STOP=1 -f bootstrap_recovery.sql
```

The normal bootstrap still creates the normal sample rows. Neither bootstrap clears an existing project. Before live use confirm the actual schema, RLS, service permissions and enabled guards against the prepared source; native tests use Supabase role/Auth stand-ins and are not remote catalog certification. Protect the privileged destination key in a separate env file with URL, project name, exact project reference and fake domain. Never paste it into chat or use it in an Android/browser client.

## Restore

```text
python recover_identity.py --env-file <isolated-destination.env> ^
  --from <private-portable.json> --confirm-project <destination-name> ^
  --confirm-project-ref <destination-ref> ^
  --destination-is-isolated-and-in-maintenance
```

The command requires the confirmed reference to match the exact HTTPS Supabase URL. It validates the portable source before opening a client. The service-only RPC checks the current schema/guards/catalog, locks the current public identity/app tables, private reset state and Auth users, and refuses any existing rows (apart from roles). It imports all identity rows in one transaction; a late constraint failure rolls back the entire import before any Auth creation. Concurrent restores serialize and the later import refuses populated state. READ COMMITTED is required to avoid stale emptiness checks.

After DB import, Auth creation and linking are separate network operations and cannot be atomic with that transaction. The command sends one create request per non-terminated staff member and never deletes Auth, wipes the destination or automatically retries an unknown response. New credentials are printed once in the operator's terminal and must be saved in a password manager. Known-created credentials remain available if linking fails. Credentials for an ambiguous Auth outcome are not declared successful.

Exit 0 means the create/link calls returned their expected results; it still requires live profile/sign-in checks. Exit 1 is refusal or unconfirmed DB outcome; inspect state before deciding whether any rerun is safe. Exit 2 is partial/uncertain Auth recovery. Keep maintenance enabled, retain all existing state, save any known-created credentials, and inspect the exact Auth/database outcome. Do not blindly rerun, issue another credential, delete accounts or weaken guards. Partial recovery resolution is an explicit operator action, not an automated retry feature.

## Live acceptance still required

Use only synthetic active Owner, disabled and terminated staff in the authorized empty project. Independently compare stable IDs, timezone, assignments and full audit semantics. Verify new Owner sign-in/profile, disabled denial, terminated absence of Auth, safe refusal against populated state and actual service-only permissions. Preserve existing DEV/PROD identities and never use the old wiper as cleanup. Record exact source/schema and outcomes; any teardown needs its own exact project boundary.

Supabase API assumptions checked against official [Auth user creation](https://supabase.com/docs/reference/python/auth-admin-createuser), [user listing](https://supabase.com/docs/reference/python/auth-admin-listusers) and [backup/restore](https://supabase.com/docs/guides/platform/migrating-within-supabase/backup-restore) documentation on 13 October. Relevant changelog entries were checked; no dependency or CLI upgrade was made.
