# Drive release repair — installed; final Google consent/publication pending

17 October 2026. [Finite scope/progress](PLAN.md).

## Accepted

- Confirmed installed0.6.0 omitted its Web client ID. The only existing Android registration used the QA package/debug certificate; this was not a production-app registration.
- Exact user-approved ordinary Android client created and read back in saaf-milk-qa. Current release package/signing SHA-1 match. Existing Web/QA clients and backups retained. Drive API already enabled; declared only the app-data scope already requested in app code.
- Verified Web identifier persisted in ignored local configuration. Actual release preflight rejects missing/malformed values and accepts the verified local-file configuration (3/3). No client secret is used.
- Signed0.6.1/code10 from native production source b125417 passes release build/lint, expected package/version, unchanged certificate, production flags, embedded ID and tracked input hash checks. [Manifest](RELEASE_MANIFEST.json). APK SHA-256: 1aca09f069ea337c4dccf9b4c561c8f91f39b42067e5ed91c38a5695ba9083e0. Database schema19 unchanged; no migration/provider/cryptography change or whole-app suite.
- A06 received the exact APK with install -r --user0. Installed bytes, retained UID and first-install values verified. [Installation](RELEASE_INSTALLS.json). No uninstall, data clear, fabricated financial posting or account reset. S24 is absent and still has the prior0.6.0 delivery.
- User selected the existing admin site for public Google app information. Only the existing landing page and new /privacy changed in sm-server-view source7a00363. Touched-file ESLint, TypeScript/production build, five local and five live anonymous route checks pass. Existing middleware/session/protected-layout/API source unchanged. Git integration published exact7a00363 to the existing production project; no duplicate deployment, new project or paid service. [Checks](ADMIN_PAGES_CHECKS.json), [publication](ADMIN_PUBLICATION.json).
- Public URLs: https://sm-server-view.vercel.app/ and https://sm-server-view.vercel.app/privacy. The privacy page is a factual description of the current app/backup/hosting behavior, not a new customer-data collection flow or a claim of formal legal certification.

## Exact remaining boundary

1. Google is still External/Testing. Publish is disabled and points to incomplete branding. The real app name/public homepage/privacy/domain are prepared in the Google form but not saved. Requested confirmation covers saving that exact draft and changing Testing to Production when Google allows it; no such approval/result is yet claimed. Existing testing-account restriction and testing refresh-token limits must not be represented as unattended production readiness.
2. A06 needs private existing-Owner sign-in, then Google account selection/consent and private recovery-passphrase input. Real encrypted upload/listing/non-destructive inspection are not verified by build/install success. No restore over the live shop is allowed.
3. Update S24 when available with the frozen same artifact; do not repeat A06 or unrelated tests.

The browser-control restriction resolved after the owner's permission change. Vercel connector MKRC access remained403; the established same-team CLI fallback verified the exact project and deployed commit without changing credentials. Local Next emitted its existing middleware-deprecation notice; no unrelated migration was made. Existing independent signing-copy recovery remains accepted; shop-data recovery/human staff acceptance remain in phase1.
