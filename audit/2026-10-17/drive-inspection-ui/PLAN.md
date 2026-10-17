# Drive inspection feedback repair — bounded scope

17 October 2026. Baseline: signed0.6.1 on both ordinary phones; A06 upload/local-key inspection accepted. Owner reports S24 passphrase inspection displayed format5/security Included, then ordinary Inspect removed it and returned the local-key error. This reported transient success is not independently captured and is not yet portable recovery acceptance.

Confirmed source causes: Inspect passes null (device-key path), controller invalidates prior inspection before every attempt, and passphrase/result controls render after the entire remote list. A new selection also failed to reset the restore checkbox. No evidence currently establishes a cryptography or backup corruption defect.

## Exact change

1. Keep recovery-passphrase input, errors and result inside the selected backup card, using existing native section/text-field components. State clearly that checking does not replace shop data.
2. Successful selected backup displays Backup checked and readable security status; remove its ordinary Inspect action so tapping cannot clear success and retry the wrong key. Key-unavailable fallback asks for the existing backup passphrase rather than another local-key attempt. Other errors retain an explicit retry.
3. Retain selected backup across the existing Owner reauthentication screen; clear draft passphrase and restore confirmation when a different backup is selected; retain existing recent-auth, ticket, checksum, passphrase wiping and restore confirmation machinery. No restore is performed.
4. Run only selected cloud-screen regressions plus the existing local inspection confirmation test in the isolated instrumentation package on S24. Package signed0.6.2/code11, verify signing/provenance/production flags and install on connected S24 retaining its installation. A06 update waits for connection. No schema, provider, vault format, retention, cloud publication or broad test run.

## Research and judgment

Android's [field feedback guidance](https://developer.android.com/develop/ui/compose/quick-guides/content/validate-input) places corrective feedback with its input. Applying that to a selected backup is our UI decision, not a prescribed Drive layout. Existing Compose state ownership/controller remains authoritative; no decrypted payload or secret is persisted in UI state. Independent judgment hook run: reported metadata is consistent with success but does not by itself prove the selected remote identity or independent restoration. Preserve this distinction in the result.

## Finish line

Selected fallback/result stay in the same card even with a long list, successful check cannot launch a null-passphrase retry, changing backup resets restore consent, and Restore remains disabled until deliberate new confirmation. No independent restore claim from UI tests. Record unrelated findings for later.
