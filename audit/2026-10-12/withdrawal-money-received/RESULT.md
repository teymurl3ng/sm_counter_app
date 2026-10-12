# B3c — owner money received: local model verified

The bounded B3c implementation is complete. An Owner/Admin command can record actual money received into an existing Cash/Bank account on its receiving date, linked to an original owner withdrawal. A mistaken receipt can be corrected for amount or account, or cancelled with a reason. The original entry and every ledger movement remain in history. Receipt and withdrawal corrections use the current effective amounts; neither direction affects sales, expenses, business profit or stock.

The review rule allows a voluntary contribution above the corrected withdrawal only after acknowledging the current withdrawal and received totals. A stale review refuses the write. Correcting or cancelling the withdrawal does not erase real receipts. New receipts cannot be linked to a cancelled withdrawal. A closed receiving date refuses new entries and correction, while a closed withdrawal date alone does not prevent recording a later real receipt. A retry with the same receipt UID and identical facts returns the saved entry; conflicting facts are refused.

Room 15 → 16 adds review context and receipt guards to the immutable owner-funds detail. Portable backup schema 12 retains it; older supported backups still restore under their bounded inference rules. Event-envelope contract v2 carries receipt account/root UIDs and review context. Local replay remaps differing database IDs, validates the complete ledger shape, parks missing or unsupported dependencies as whole records, and does not duplicate a repeated receipt.

## Focused verification

- `:app:compileDebugKotlin`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest -Psaaf.instrumentationSandbox=true` passed.
- [B3c device log](../withdrawal-account-return-plan/b3c-device-second.log): **8/8** receipt/remedy cases passed on the A06 QA package. These covered exact balances, amount/account corrections and cancellation, excess and stale review, role/date/UID refusal, Room migration and SQL guard, ordinary/encrypted backup, and replay into a second database with different numeric IDs.
- [B3b regression log](../withdrawal-account-return-plan/b3c-b3b-regression.log): **12/12** existing account-correction cases passed on the same QA package, including legacy migration, backup and replay.
- `git diff --check` exited successfully. It reported only existing Windows line-ending conversion warnings.

The first six-case receipt run exposed a pending-original-root validation error in replay; that validator was corrected. The focused replay rerun passed, followed by the complete eight-case run above. Failed and repair logs are retained beside the final logs. The current QA test APK was installed on A06 at `192.168.0.64:46645`; the ordinary app was not installed or changed.

The [server preflight](../withdrawal-account-return-plan/B3C_SERVER_PREFLIGHT.md) found generic event JSON transport, with no event-type allowlist. Its proposed pusher-role gate was applied only on development and then reverted after the shared-offline-outbox mismatch was found. Development and production RPC definitions again match their original fingerprint; production was never changed. This result proves local role checks and local replay, not hostile-client actor authorization or live two-device convergence.

B3 remains **partial**. B3d must expose the account correction, received-money form, review, linked history and saved/failure states in Records, then run the affected phone checks. No full suite, ordinary-app install, commit/push or release packaging occurred in B3c.
