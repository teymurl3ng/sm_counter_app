# Current technical finish — 15 October 2026

**Later ordinary-app check:** The user confirmed the already-open ordinary A06 installation is the real configured shop. Authenticated Owner, PROD binding and historical records/details are observed. A live zero-outbox sync then exposed a nullable-timezone refresh defect. [Bounded repair](../prod-timezone-fix/RESULT.md) reproduces it, passes six affected checks and delivers signed0.4.3/code7 to A06 without data clearing. Post-patch private sign-in/live acceptance and S24 patch update remain open. Earlier305a91d freeze and “no production source change” statements below describe the initial finish run only.

Completed in this bounded run: production reset/browser acceptance, latest signed A06 update, seventeen affected phone checks, current signing-key proof and the revised handover. No production app feature or schema change was needed. Staff/private ordinary-app acceptance and independent custody are not inferred from these results.

## Production reset acceptance: batch 6 complete

The installed Playwright/Chromium browser exercised the published production Viewer `d2c94e1`, with disposable Owner/Admin sessions that reported PROD and an online account server. Those identities were created only on exact PROD `tlsuxbghuhlqntkgtrxt`; existing staff credentials were not reset.

- Admin was denied Owner/Admin/disabled Cashier resets; the browser disabled the protected-target control and explained the policy.
- Cancelling the real browser confirmation left the original fixture credential and reset audit unchanged.
- Owner's browser reset of a disabled Cashier showed a working one-time credential, rejected the old credential, kept the target disabled, and wrote one completion audit. The screenshot masks the credential; Dismiss removed it.
- Admin's permitted active-Cashier reset produced a working replacement with one completion audit.
- Service-only claim rejected ordinary staff tokens; participant access changes and a second claim were blocked while unresolved.
- The pending-only repeat used a trusted, actually successful Auth change deliberately marked `AUTH_UNKNOWN`. The browser showed its reference/warning, disabled reset, and Check reset status preserved it. A repeat API call returned pending without a credential or another Auth change.
- Completion-only retry was idempotent: one audit, no Auth replay, no held reservation; browser status released the controls.

The initial pending tail used an invented test uncertainty code and was correctly rejected by the installed function's allowlist. Its known-success request was safely completed, its three fixtures retired and original-state comparison passed. Only the pending tail was repeated with the supported code and two fresh fixtures. Do not report the failed first attempt as wholly passing: [initial results](prod-reset.json), [pending-only accepted results](pending-retry/prod-reset.json). Both cleanup records pass. Five deliberately created QA Auth identities were removed through supported termination; their inactive/unlinked/terminated staff and audit history remain. Original businesses, shops, full staff rows and Auth IDs matched checkpoints. No test request remains held.

The cached local production env file failed exact-target checks and was not used for fixture operations. Existing exact-PROD keys came from the authenticated supported CLI and remained in memory; no keys were created or saved. The built-in CUA and Node kernels failed before navigation with missing kernel assets. The installed Python browser worked directly against production; no hosting protection was disabled or bypassed.

## Phone acceptance and candidate

A06 (`R8VXA02SNKH`, ordinary profile 0) now has the same production-source `305a91d` APK previously verified on S24. The APK hash is `ec2cf295884fece77d951e8022e3270ab5e0ec8fd9c223943178a677eed64ba0`; package `com.saaf.milkshop`, 0.4.2/code 6, release certificate unchanged. It was a normal replacement update, with no uninstall or data clear. Exact installed bytes and launch are verified. The script initially used an unsupported monkey option after installation/hash checks had succeeded; the supported explicit activity launch succeeded without reinstalling.

Seventeen unique selected checks passed on A06's separate `com.saaf.milkshop.qa.tests` package:

- Six real host/IME checks: Owner, Cashier, Manager, Admin and Operations Personal security forms (first/last field switching, repeated real IME opening and keyboard Back, stationary outer title, focused field unobscured), plus Sale Done hiding the keyboard and clearing focus.
- Costing switching twice through four sections at 100% and 200% text: tab-strip and action/content baselines stay stable. Existing empty, failure/retry and reauthentication contracts pass.
- Normal and dark/200% shared work-header geometry pass.
- The actual PIN setup keyboard trace passes, including field visibility, no transient Auto-fill menu and no header panning.
- Dynamic sale catalogs at 0/1/2/3/12/50 products, payment-picker changes/cancellation and wholesale-rate/note editing pass.

Evidence: [six host results](hosts-results.json), [ten passing original contracts plus the first large-text measurement failure](contracts-results.json), [single accepted large-text repeat](costing-retry-results.json). First host failures are retained in `hosts-first-results.json`: three incorrect same-title assumptions, two uses of app Back instead of system keyboard Back, and one Sale input not scrolled into view. Corrections were to test selectors/actions, not production code. The large-text failure measured a horizontally clipped chip; the corrected check measures the intended tab strip. The separate tab-scroll observation is retained in [LATER.md](LATER.md) without expanding this run.

These are representative affected checks of the repaired shared boundaries, not an exhaustive physical certification of all 112 mapped fields, every device/font combination, real enrollment or subjective smoothness. No broad suite ran. Production source remains byte-for-byte at the frozen candidate's tracked tree; only affected Android tests and documentation were added. The existing signed candidate therefore did not need another rebuild.

## Signing and handover

The protected current signing key successfully signed and verified a fresh random challenge; its public certificate equals the candidate's recorded certificate. No private key or passwords were exported. [Signing proof](signing-proof.json). This proves current key usability, not independent-backup custody or a fresh recovery from that backup.

[HANDOVER.md](HANDOVER.md) replaces the old pre-repair candidate instructions. It pins the APK/source/certificate and server/Viewer versions, separates local and server credentials, explains cashier closing/reopen and lock/sync semantics, identifies private shop setup, and gives compatible app/server recovery and forward-repair/rollback boundaries. Old checklists are reconciled to credit completed publication, hosted recovery, production reset and both phone installations.

## Actual remaining boundary

The ordinary A06 app launches and its account chooser still shows the existing Owner and configured Quick PIN/full-passphrase options. No credential was entered and no real transaction was posted. This is evidence of retained local account presence, not proof of retained ledger contents or a successful private login. The owner has been asked to sign in privately and confirm the intended shop/PROD setup and independent key/recovery custody. Until those facts/actions are supplied, ordinary-app retained-record, genuine phone PROD session/sync and real business acceptance cannot be marked complete.

Uncoached staff rehearsal, actual enrolled phone-security cancel/success/account isolation, felt motion/vibration/readability and next supported date remain batch 4. Final private setup/records/login and custody/handover acceptance remain within batch 7. No new feature has been appended; Credit/reminders and other future work remain in the recovered later queue.
