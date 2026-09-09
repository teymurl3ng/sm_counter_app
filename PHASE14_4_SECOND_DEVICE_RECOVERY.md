# Phase 14.4 — Second-device recovery

Status: **COMPLETE — data-rich S24-to-A06 replacement, identity/history verification, and failure preservation proven**

- Started: 2026-09-06
- Completed: 2026-09-09
- Source QA device: Samsung SM-S921W / Android 16 / API 36
- Target QA device: Samsung SM-A065F / Android 16 / API 36
- QA package: `com.saaf.milkshop.qa` / `0.1.0-qa`

## Outcome

A real data-rich Google Drive recovery point created on the S24 was downloaded, authenticated, decrypted, inspected, and used for a full replacement of a freshly reset isolated QA installation on the A06. The app reported a safe restore, invalidated the active session, accepted the restored S24 owner account, restored the recognizable business records and expected totals, and correctly left cloud backup paused.

An earlier same-device replacement used a point whose device-local vault key had been removed by revocation, proving recovery through the separately held cloud-recovery passphrase rather than reuse of that local key. The final run then proved the required physical transfer in the opposite direction: S24 source to A06 target. The three oldest A06-origin points still require their forgotten original private recovery passphrase; leaving them unreadable is the intended encryption boundary and no longer blocks the separately proven cross-device path.

## Live evidence

- The newest point from the original phone was not restored because its original cloud-recovery passphrase was not available. No bypass or reset was attempted; the encrypted remote points remain untouched.
- The second-newest point, created before revocation on the replacement phone, passed its cloud security inspection and reached the deliberate full-replacement warning.
- Confirming restore produced `Backup restored safely`, signed the active user out, and did not affect the phone's ordinary Saaf installation/profile.
- The restored owner credentials worked. Cloud still listed six remote points and the header correctly reported `Google Drive paused`; visibility of retained points is independent from scheduling state.
- An incorrect Saaf owner passphrase was rejected before the native cloud-recovery dialog, but the UI used the generic and contextually misleading `name or passphrase is incorrect` wording.
- A follow-up attempt used the correct owner passphrase and a deliberately incorrect cloud-recovery passphrase. The cloud inspection was rejected, no restore was initiated, and the app remained signed in with its records intact and Google Drive paused.
- While preparing recognizable recovery data, the correction dialog required a complete corrected transaction but presented blank editable fields and no in-dialog original-value reference.
- On the S24, the owner created a recognizable fake product, supplier, customer, purchase, sale, and correction and noted the expected stock and balance results. Re-enabling cloud backup plus one manual run produced two verified points; the data-rich source point was identified by its 20:14 timestamp and the remote total reached eight.
- On the connected A06, the target identity was verified as `SM-A065F`. Only `com.saaf.milkshop.qa` in owner profile 0 was cleared with explicit user authorization; no ordinary `com.saaf.milkshop` package was present in that profile and no remote point was removed.
- A temporary A06 owner and cloud connection created two additional points, bringing the visible total to ten. The exact 20:14 S24 recovery point passed inspection and full-replacement confirmation, then logged the temporary A06 owner out.
- The S24 owner credentials from the backup worked on the restored A06. The product, supplier, customer, purchase, sale, correction, expected stock, and expected balances all matched the source.
- History preserved the append-only correction chain: the original showed `Replaced by correction`, the new event showed `Correction of #…`, and `Recorded by` identified the restored owner. Google Drive was paused on the target, so the two writable installations were not presented as synchronized copies.

### Finding F14.4-01 — owner reauthentication error refers to an absent login name

The Cloud inspection form asks only for `Your current owner passphrase`. When that value is wrong, the app reports that the `name or passphrase is incorrect`, even though no login-name field or login action exists in this context. Replace it with specific owner-reauthentication guidance such as `Your Saaf owner passphrase is incorrect` while preserving generic credential wording on the actual sign-in screen.

### Finding F14.4-02 — correction form hides the values being corrected

Severity: **P1 major**. The correction dialog says to enter the complete corrected transaction, but every material editable value starts blank and the original record is no longer visible inside the dialog. This forces the user to remember or re-copy multiple figures and increases the risk of changing a correct field accidentally. Show a compact read-only `Original values` summary and prefill each corrected field from the authoritative original transaction so the user changes only what is wrong while still submitting a complete replacement. If the current history DTO does not expose the required raw inputs, add them through the native read model rather than reconstructing them from display totals.

## Safety state

- The A06 isolated QA installation remains restored and signed in with cloud backup paused.
- Ten encrypted recovery points remain listed; none was deleted.
- The S24 source QA installation remains separate. The app did not merge or synchronize later writes between devices.
- Ordinary `com.saaf.milkshop` installations/profiles remain untouched.
- No passphrase, recovery code, Google password, account identifier, token, or OAuth secret was added to project evidence.

## Automated preservation evidence

The unchanged production implementation retains the green Phase 8/11 recovery matrix for cases that should not be manufactured against live Drive data: tampered backup, expired or wrong-session ticket, cancellation/compensation, repeated replacement, repeated wrong passphrase followed by success, and wrong-account preservation. These tests are included in the previously accepted Phase 14.3 25/25 focused cloud suite and Phase 11/13 complete regression evidence. The live run adds the provider and two-physical-device proof those doubles cannot supply.

## Phase 14.5 handoff

Proceed to accessibility, adaptivity, and Android-behavior QA. Preserve the two isolated installations and ten remote recovery points until later defect closure no longer needs them. Phase 14.7 owns F14.4-01 and F14.4-02 alongside the three Phase 14.3 findings.

## Plain English

The recovery system worked across two real phones. A recognizable S24 dataset appeared correctly on the freshly reset A06, including its correction history, owner attribution, stock, and balances. Restore logged everyone out and paused the target's backups instead of pretending the phones were synchronized. Wrong secrets changed nothing. The three oldest backups remain unreadable without their forgotten secret, exactly as strong encryption requires.
