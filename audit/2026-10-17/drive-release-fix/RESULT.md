# Drive release repair — A06 connection, upload and inspection accepted

17 October 2026. [Finite scope/progress](PLAN.md).

## Accepted

- Confirmed installed0.6.0 omitted its Web client ID. The only existing Android registration used the QA package/debug certificate; this was not a production-app registration.
- Exact user-approved ordinary Android client created and read back in saaf-milk-qa. Current release package/signing SHA-1 match. Existing Web/QA clients and backups retained. Drive API already enabled; declared only the app-data scope already requested in app code.
- Verified Web identifier persisted in ignored local configuration. Actual release preflight rejects missing/malformed values and accepts the verified local-file configuration (3/3). No client secret is used.
- Signed0.6.1/code10 from native production source b125417 passes release build/lint, expected package/version, unchanged certificate, production flags, embedded ID and tracked input hash checks. [Manifest](RELEASE_MANIFEST.json). APK SHA-256: 1aca09f069ea337c4dccf9b4c561c8f91f39b42067e5ed91c38a5695ba9083e0. Database schema19 unchanged; no migration/provider/cryptography change or whole-app suite.
- A06 received the exact APK with install -r --user0. Installed bytes, retained UID and first-install values verified. [Installation](RELEASE_INSTALLS.json). No uninstall, data clear, fabricated financial posting or account reset. S24 is absent and still has the prior0.6.0 delivery.
- User selected the existing admin site for public Google app information. Only the existing landing page and new /privacy changed in sm-server-view source7a00363. Touched-file ESLint, TypeScript/production build, five local and five live anonymous route checks pass. Existing middleware/session/protected-layout/API source unchanged. Git integration published exact7a00363 to the existing production project; no duplicate deployment, new project or paid service. [Checks](ADMIN_PAGES_CHECKS.json), [publication](ADMIN_PUBLICATION.json).
- Public URLs: https://sm-server-view.vercel.app/ and https://sm-server-view.vercel.app/privacy. The privacy page is a factual description of the current app/backup/hosting behavior, not a new customer-data collection flow or a claim of formal legal certification.

## Live acceptance

- User approved and Google saved/read back the app name, existing public URLs/domain and External **In production** mode. Verification Center explicitly says data-access verification is not required for the non-sensitive app-data permission. Branding is not yet shown to users; cosmetic branding review is deferred. This is not a claim that Google verified the brand.
- Owner privately supplied the recovery passphrase and completed account selection/consent on A06. No secret was entered by the agent or recorded in evidence.
- Ordinary A06 reports **Drive vault protected**, enabled/configured, successful upload at `2026-10-17T13:39:35.733827Z`, no stale state and no pending upload. The coordinator verifies the remote upload before recording success.
- Refreshed cloud list contains the new **17 October 2026, 6:39 pm** recovery point. Non-destructive inspection returns format **5** and security state **Included**. The checkbox remains unchecked and the actual Restore button ancestor is disabled. No restore occurred. Returned to the read-only backup home with **Drive vault protected** and no editable field. [Non-secret phone evidence](PHONE_DRIVE_CHECK.json).
- Inspection used the current device's local vault key. It does not prove independent recovery-passphrase restoration on another installation or long-duration unattended operation.

## Exact remaining boundary

1. S24 is absent and retains0.6.0. Install the frozen0.6.1 artifact when reconnected; no repeat A06 install or rebuild is needed.
2. The encrypted shop recovery point is in Drive's **hidden app-specific backup storage**, accessible through the app's Cloud recovery points. It is separate from the visible **SaafMilk-backup** folder holding the independently verified signing files. No manual shop export to that visible folder is claimed.
3. Independent passphrase custody/supported isolated recovery, known-record/live-sync confirmation and actual human staff outcomes remain the existing phase1 handover boundary. Never restore over the sole live shop for a test. No repeated whole-app test run is scheduled here.

Current Drive repair is accepted on A06. Google publication is complete; cosmetic brand review is a later note, not a new repair phase. Browser-control restriction resolved after the owner's permission change. Vercel connector MKRC access remained403; established same-team CLI fallback verified the existing project/deployed commit without changing credentials. Existing Next middleware-deprecation notice was not expanded into an unrelated migration. APK packaging-time Google fields remain historical; no app rebuild is required for the subsequent Google-side publication.
