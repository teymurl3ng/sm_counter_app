# Phase 14.4 — Second-device recovery

Status: **IN PROGRESS — safe checkpoint after successful live replacement; cross-device source and remaining preservation evidence pending**

- Date: 2026-09-06
- QA device: Samsung SM-S921W / Android 16 / API 36
- QA package: `com.saaf.milkshop.qa` / `0.1.0-qa`

## Outcome so far

A real Google Drive recovery point was downloaded, authenticated, decrypted, inspected, and used for a full replacement of the isolated QA installation. The app reported a safe restore, invalidated the active session, accepted the restored owner account, and correctly left cloud backup paused while retaining the six remote recovery-point listings.

The restored point was created on the SM-S921W before the Phase 14.3 revocation. Its device-local vault key had been removed by revocation, so the successful restore proves recovery through the separately held cloud-recovery passphrase rather than reuse of that local key. It does not yet prove that a point created on the original SM-A065F can be restored on different hardware: the three original points require their original private recovery passphrase, which was not available during this run.

## Live evidence

- The newest point from the original phone was not restored because its original cloud-recovery passphrase was not available. No bypass or reset was attempted; the encrypted remote points remain untouched.
- The second-newest point, created before revocation on the replacement phone, passed its cloud security inspection and reached the deliberate full-replacement warning.
- Confirming restore produced `Backup restored safely`, signed the active user out, and did not affect the phone's ordinary Saaf installation/profile.
- The restored owner credentials worked. Cloud still listed six remote points and the header correctly reported `Google Drive paused`; visibility of retained points is independent from scheduling state.
- An incorrect Saaf owner passphrase was rejected before the native cloud-recovery dialog, but the UI used the generic and contextually misleading `name or passphrase is incorrect` wording.
- A follow-up attempt used the correct owner passphrase and a deliberately incorrect cloud-recovery passphrase. The cloud inspection was rejected and no restore was initiated. The exact error copy and an explicit post-failure continuity check were not recorded before pausing.
- While preparing recognizable recovery data, the correction dialog required a complete corrected transaction but presented blank editable fields and no in-dialog original-value reference.

### Finding F14.4-01 — owner reauthentication error refers to an absent login name

The Cloud inspection form asks only for `Your current owner passphrase`. When that value is wrong, the app reports that the `name or passphrase is incorrect`, even though no login-name field or login action exists in this context. Replace it with specific owner-reauthentication guidance such as `Your Saaf owner passphrase is incorrect` while preserving generic credential wording on the actual sign-in screen.

### Finding F14.4-02 — correction form hides the values being corrected

Severity: **P1 major**. The correction dialog says to enter the complete corrected transaction, but every material editable value starts blank and the original record is no longer visible inside the dialog. This forces the user to remember or re-copy multiple figures and increases the risk of changing a correct field accidentally. Show a compact read-only `Original values` summary and prefill each corrected field from the authoritative original transaction so the user changes only what is wrong while still submitting a complete replacement. If the current history DTO does not expose the required raw inputs, add them through the native read model rather than reconstructing them from display totals.

## Safety state

- The isolated QA installation remains restored and signed in with cloud backup paused.
- Six encrypted recovery points remain listed; none was deleted.
- The ordinary `com.saaf.milkshop` installation/profile remains untouched.
- No passphrase, recovery code, Google password, account identifier, token, or OAuth secret was added to project evidence.

## Resume point

First confirm that the failed cloud-passphrase inspection left the restored QA installation signed in and `Google Drive paused`. Then finish the remaining live preservation checks and decide how to supply honest cross-device/data-rich proof without weakening encryption: either make a known-data point on one QA device and restore it on the other isolated QA installation, or document the physical-source limitation and rely only where appropriate on the already-passing destructive-recovery fixture matrix. Never treat a forgotten recovery passphrase as recoverable.

## Plain English

The real restore worked: it replaced only the test app, logged everyone out, restored the owner, kept all six cloud copies, and paused future backups. A wrong recovery secret was rejected. Two UI problems are now recorded: unclear owner-passphrase errors and a correction form that hides the original values. The remaining work is to prove a data-rich backup moving between two separate devices; the oldest three copies cannot be opened without their original secret.
