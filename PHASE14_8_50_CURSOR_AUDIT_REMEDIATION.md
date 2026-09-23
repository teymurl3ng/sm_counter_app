# Phase 14.8.50 — Cursor-work audit remediation

Date: 2026-09-23
Branch: `fix/native-shell-vs-content-owner-work`

## Outcome

The audited native-UI branch is buildable and its security, theme, adaptive-shell, lint, and evidence findings are remediated. One maintainability item remains intentionally open: `NativeOwnerMoreViewModel` still coordinates too many owner feature domains. Pending user/backup actions and backup-secret ownership have been extracted, but a complete feature-by-feature split should continue as a controlled refactor rather than an unverified rewrite.

## Remediations

- Debug and native-preview builds always use a non-production application ID (`.preview`, `.qa`, or `.qa.tests`). Preview fixture credentials are compiled only into debug; release fixtures are disabled and the release passphrase field is empty.
- Fixture creation and fixture login recognition are guarded by `ENABLE_NATIVE_PREVIEW_FIXTURES`.
- All authenticated Compose hosts use the system theme. Authentication remains deliberately light to preserve its approved access design.
- Passphrases, Quick PINs, confirmations, reopen credentials, and recovery passphrases use non-saveable Compose state and are cleared immediately on submission.
- ViewModels wipe caller-owned character arrays on early return and in asynchronous `finally` paths. Owner backup retry secrets use separately owned copies and are wiped when replaced, cancelled, expired, or cleared.
- Cashier, Operations, Owner, Manager, and Admin shells use bottom navigation below 600dp and a navigation rail at 600dp and wider.
- The Android 30 keystore call is guarded and isolated, navigation-bar XML attributes declare their API target, literal percent strings are escaped, and the local SDK path uses the expected escaped-colon syntax.
- The frozen-baseline QA script hashes through the .NET cryptography API, making it portable across Windows PowerShell 5.1 and PowerShell 7.

## Verification

| Gate | Result |
|---|---|
| `gradlew qaFast` | PASS; inventory `947`, untested `0`, orphans `0` |
| `gradlew :app:lintDebug` | PASS; `0 errors`, `181 warnings` |
| `gradlew :app:assembleDebug :app:assembleRelease` | PASS, including release lint-vital |
| Kotlin + Android-test Kotlin compilation | PASS |
| `AdaptiveNavigationScaffoldContractTest` on SM-A065F | PASS, 2/2 |
| Affected Cashier/Operations/Owner/Manager/Admin UI contract matrix on SM-A065F | PASS, 51/51; 0 skipped, 0 failed |
| `git diff --check` | PASS; only line-ending conversion notices |
| Secret-state source scan | No passphrase/PIN/confirmation stored with `rememberSaveable` |
| Forced-theme source scan | Only the approved authentication host remains explicitly light |

The first connected-test invocation correctly failed closed because the required instrumentation sandbox flag was absent. The rerun used `-Psaaf.instrumentationSandbox=true`, installed the isolated `com.saaf.milkshop.qa.tests` identity, and passed 2/2.

## Remaining engineering debt

- Split `NativeOwnerMoreViewModel` into feature coordinators (catalog, people, costing, shop settings, users/security, and backup). The current extraction establishes the secret-ownership boundary but does not by itself make the 4,570-line coordinator small.
- The 181 lint warnings are non-blocking inherited/deprecation and hygiene items. They no longer hide any lint errors and should be reduced incrementally.
- Product-depth deferrals remain unchanged: approval queue requires a domain API; opening cost basis, Cashier Deliveries-from-More, custom report ranges, and additional record groups are separate roadmap work rather than audit regressions.
