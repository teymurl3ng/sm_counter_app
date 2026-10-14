# Shop phone setup and access — verified boundary

## Intended shared counter setup

For a shop-owned counter phone, the existing cashier-scoped server transport is the appropriate least-privilege starting point. Each worker still has an individual **local** app account/passphrase/PIN and role. The server transport account belongs to this phone's sync connection, not to whichever worker is currently signed in locally. Owner performs setup/maintenance; workers use their own local sign-ins. Do not hand an Owner server credential to every clerk or use a shared local Owner login for daily entry.

The current enrollment supports server Owner or Cashier. Owner resolves a specific active local Owner identity link; Cashier is a counter-device binding without an Owner link. Shop choice must be one of the account's authorized shops; changing the installation to another environment/business/shop is refused while its recorded association exists. A valid connection anchors shop timezone. Invalid or uncertain enrollment does not save new connection state. The selected existing tests pass against fake HTTP in the isolated test package, not a new real PROD account setup.

Cashier transport has limited server authority: correction/master-data pushes require Owner/Admin on the server. A local Owner action on a cashier-connected phone does not acquire higher server transport authority automatically. Keep that existing constraint visible during the intended shop-day acceptance; no role bypass, credential-sharing workaround or sync rewrite was added. The actual pilot's owner-maintenance and writable-phone arrangement still must be recorded before ordinary setup.

The historical `counter` server-account check is credited from the saved findings, but this preflight did not retrieve its credentials, reauthenticate it, inspect the ordinary phone's enrollment or configure that phone. Do not present the physical shop device as ready from these tests alone. Do not read old credential text files to establish metadata.

## What the actions mean

| Action | Verified local/server effect | What remains |
|---|---|---|
| Local Sign out / lock session | Native authentication calls local `UserSecurityService.logout`; the local session ends and the Welcome flow is shown | Saved shop data, local accounts/quick-unlock enrollment and the installation's independent business-sync connection remain |
| Switch account | Choose/verify another local person's account; local role controls their actions | The same installation sync transport/configuration remains; the transport is not automatically switched to that person's server identity |
| Stop syncing this phone | Attempts server logout with `scope=local`, then clears configuration, file shop binding and saved server tokens, and disables scheduling | Local users/records, unsent rows and Room identity/binding history are not deleted; it is not a factory reset or remote account termination |
| Disable/terminate server account | Server authorization/profile/refresh can deny further remote operations; definitive refresh results clear saved session tokens and recorded link verdicts become suspended/revoked | Current offline local users are not centrally revoked by this action; `ENFORCE_LINK_STATES=false` |
| Disable a local app account | Separate local account lifecycle/permission operation | Does not automatically identify or revoke that person across every phone/server account |

`unenroll_clearsEverything` is an old test name: its actual assertions cover connection config/file binding/tokens, not deletion of all Room data. The source explicitly leaves those rows alone. A targeted next check must verify preserved rows and local sign-out/transport independence together; do not infer that from this test name.

The stop-sync server logout is best effort. If offline/unreachable, local connection files still clear. It targets only that session, and existing access JWTs can remain valid until expiry: this action is connection hygiene, not an immediate stolen-device kill switch. [Supabase sign-out behavior](https://supabase.com/docs/guides/auth/signout).

## Offline boundary

The current grace clock belongs to the Room installation binding. After 14 days without verified server contact, daily operational entry and reads remain available; privileged settings/master-data/restore/export/Owner-drawing/reopen operations are restricted. Stopping sync does not delete the saved binding history or its clock. The runtime test verifies offline/disabled/malformed/wrong-shop responses do not renew this clock; only an allowed matching profile does.

Server suspended/revoked link status is recorded/displayed but is not itself enforced against each person's offline local access. This is an intentional existing boundary, not a newly fixed central-revocation feature. Do not turn on the flag alone: its documented confirmation/refresh/recovery/offline guardrails and per-device/per-person identity lifecycle are a separate design/acceptance requirement (B7), particularly if workers control remote personal phones. Shop-owned physically supervised counter use has a different trust boundary, and neither deployment choice has been silently accepted by this note.

## Before ordinary device handover

1. Record intended phone(s), shop, local roles and responsible Owner. Use the accepted release installation rather than the instrumentation/preview package. Existing ordinary records are never replaced as a setup fixture.
2. Confirm a usable portable/off-device app backup and the recovery passphrases; server identity recovery and signing key are separate requirements.
3. Owner establishes the correct PROD shop connection with the intended cashier or Owner transport, verifies status/authorized shop/timezone and keeps the server credential private. Existing staff credentials are not changed for testing.
4. Each person verifies their own local sign-in/PIN and assigned role. Sign-out/account-switch tests must explain that business sync persists; stop-sync must explain that records remain and future uploads/pulls stop until reconnected.
5. Reconcile unsent/held work, server transport limits and offline behavior in the scoped shop-day rehearsal. Use two physical phones only if both will write. Record the remote-revocation limitation before relying on it.

This is the setup procedure and boundary baseline, not completed ordinary enrollment. The next 8c2 batch changes only misleading/missing explanation and verifies those two local-versus-transport consequences on synthetic state. No actual credential configuration is required to perform that narrow batch.
