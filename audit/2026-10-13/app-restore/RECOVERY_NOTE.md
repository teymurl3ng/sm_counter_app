# Shop recovery — separate what must be recovered

## App shop records

Use an encrypted portable `.milkbackup` plus its backup passphrase to recover records onto a fresh isolated installation first. Keep the local account passphrases/recovery codes available too: the backup password unlocks the file; local account credentials unlock the restored app. App restore replaces selected local groups, not a merge with newer records. Stop writes, establish the intended source/date and inspect the file before accepting replacement. Keep the pre-restore safety snapshot.

The current app supports format 3 and later; current exports are format 5, schema 12. Wrong file passphrase and tampering must refuse; no guessed passphrase or ordinary-device reset belongs to a rehearsal. A wrong backup passphrase is different from a forgotten local Owner passphrase.

Local automatic snapshots are encrypted using the originating Android Keystore. They protect that installation but are not substitutes for a portable passphrase-protected file stored off-device. Losing the originating device/key can prevent opening its local snapshots. Store a usable portable/Drive copy separately and protect its passphrase. Current isolated device checks do not create or change any Drive object.

Restored server links preserve stable business/shop/staff references but become `restored_unverified`. Saved remote tokens are cleared. Reconnect/reverify the server identity before relying on sync, and review held restored work against server state before permitting pushes. Preserve unsent work; a successful local restore does not establish current remote access or two-phone convergence.

## Account-server identity

The private identity snapshot preserves identity/audit state, not the app's ledger. Server credentials are newly issued through accepted recovery; they are not recoverable from old Auth dump IDs. The prepared safe server procedure is in the external server `RECOVERY.md` and the [proposal](../identity-recovery-proposal/DEPLOYMENT_PROPOSAL.md). Hosted Auth/profile acceptance remains pending. Do not run a server wipe or confuse new server credentials with local app passphrases.

## Android release signing

The signing keystore and its passwords are a third recovery requirement. Restoring ledger records or server identity does not recover the signing key needed to update the same ordinary Android package. Historical Owner off-machine Drive copy/hash evidence is credited from 29 September. Current Owner custody/password availability and signing from that recovered key are phase-6 release checks; no key retrieval or password disclosure ran in this app rehearsal.

## Evidence boundary

Earlier actual Drive recovery and before/after-SQL-commit interruption probes remain credited as historical evidence. This batch exercises current encrypted local/file restore and file-backed database reopen on one A06 test package, with synthetic rows and independent comparisons. It does not certify a current Drive download, new-phone Keystore transfer, forced process death, ordinary app upgrade or signing recovery. Intended shop-day/actual phone and release acceptance remain separate.
