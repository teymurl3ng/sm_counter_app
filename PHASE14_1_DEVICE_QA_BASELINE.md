# Phase 14.1 — Device-QA Plan and Clean Baseline

Status: **COMPLETE**

Completed: 2026-09-06

## 1. Entry baseline

No source changed between the Phase 13.5 closure and this checkpoint. The accepted entry baseline therefore remains:

- complete isolated-QA Samsung regression — 258/258 passed across the authenticated 257-test and empty-identity 1-test partitions, with zero skips or failures;
- host debug unit suite — 7/7 passed;
- bridge contract — v4, 86 endpoints, manifest SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`;
- cleanup guard — 85 active TypeScript/TSX files, 23 retired prototype paths and six dependencies absent;
- immutable logic and SQL hashes — unchanged from Phase 0.

Checkpoint 14.1 rebuilt the isolated QA APK. Bridge, cleanup, packaged-asset, and Android build checks passed, and the APK hash remains `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C`.

## 2. Physical-device inventory

| Item | Observed baseline |
|---|---|
| Connected physical devices | Two Samsung phones detected by the end of the checkpoint |
| Primary device | Samsung SM-A065F |
| Android | 16 / API 36 |
| Physical display | 720 × 1600 at 300 dpi |
| System font scale | 1.0 |
| System night mode | Enabled |
| Touch exploration / TalkBack | Disabled / no accessibility service enabled |
| Installed test application | `com.saaf.milkshop.qa`, version `0.1.0-qa` |
| Ordinary Saaf package on primary | Not installed or modified during this checkpoint |
| Candidate recovery device | Samsung SM-S921W, Android 16 / API 36, 1080 × 2340 at 480 dpi, font scale 1.0 |
| Candidate-device safety state | Ordinary `com.saaf.milkshop` package detected; no install, clear, launch, or restore action performed |

The clean first-owner screen renders as the approved light interface even while the phone is in system dark mode. The status-bar protection, form, four fields, primary action, and three-button navigation inset are visible without clipping or overlap. This is a baseline observation, not a complete TalkBack or large-font acceptance result.

## 3. Provider and recovery prerequisites

The generated debug `BuildConfig` contains an empty `GOOGLE_WEB_CLIENT_ID`. This is intentional source-control behavior: the production/test Web OAuth client ID must be supplied at build time with `-Psaaf.googleWebClientId=<client-id>`.

Therefore:

- local authentication, role, lifecycle, file-backup, presentation, and current-device accessibility work can proceed immediately;
- live Google consent/Drive tests cannot honestly start until an approved OAuth client ID and owner-controlled test Google account are available;
- the account password, tokens, recovery passphrase, and client configuration value must not be written into project evidence;
- a second physical Android device is connected, but it already contains the ordinary Saaf package; installing the separate `.qa` package or using it for recovery requires confirmation that this is the intended test phone, and the ordinary package/data must remain untouched;
- automated provider/failure doubles already prove many failure semantics, but Phase 14 will not relabel them as live-provider evidence.

## 4. Existing automated credit and live gaps

Existing device tests already provide strong regression coverage:

| Area | Existing focused credit |
|---|---:|
| Native user/session/security rules | 7 Phase 3 tests + 9 Phase 4 tests |
| Packaged hybrid authentication/authorization | 3 vertical-slice tests + 5 staff-security tests |
| Cloud envelope, scheduling, retry, and retention | 5 Phase 10 tests + 4 durability tests + 11 failure-matrix tests |
| Restore identity/session safety | 5 recovery tests |
| Presentation and responsive/accessibility automation | 4 presentation tests + 2 responsive/accessibility tests |

The remaining acceptance gap is not a lack of mocked unit behavior. It is real packaged interaction: all five roles, deliberate bridge bypass attempts, actual Google consent/Drive behavior, actual offline/scheduling transitions, TalkBack, broader device configurations, and a second-device restore.

## 5. Audit and evidence method

The Phase 14 native UI audit follows the incumbent `PRODUCT_SCOPE.md` and `DESIGN.md` plus Android/Material and WCAG guidance. The Impeccable native audit framework supplies five technical dimensions—accessibility, performance, appearance/theming, platform conformance, and adaptivity—and the P0/P1/P2/P3 severity scale. It does not authorize a redesign or any business/formula change.

The raw UI Automator dump exposes the outer Android WebView container but not the inner semantic web tree. That tool limitation is not recorded as an accessibility defect. Actual TalkBack traversal and focused WebView accessibility tests are required in checkpoint 14.5.

Evidence captured in `phase14-evidence/sm-a065f-android16-phase14-1-baseline/`:

- `baseline.png` — 720 × 1600, SHA-256 `53C8A73889A9BD33F56FC752DD41F1F4A1C5F539EF3D59B25FFAA0BCB48E00D3`;
- `accessibility-tree.xml` — SHA-256 `1DE7E4C424CAF06026AE26203EBA2E8F1CC0C60BFA66AF902FF7C591D7DBE55F`.

## 6. Checkpoint decision

Checkpoint 14.1 passes. The safe isolated package, primary phone, unchanged regression baseline, evidence rules, and later prerequisites are explicit. Phase 14.2 may proceed immediately on the primary Samsung. Phase 14.3 requires real OAuth/account setup. A second phone is connected for possible Phase 14.4 use, but its existing ordinary Saaf installation makes explicit safe-use confirmation mandatory before the separate QA package is installed or any recovery test begins. No live requirement will be simulated or silently skipped.

## Plain English

The Phase 14 test environment is ready. The separate QA app installs and opens correctly on the primary Samsung, so the normal app and real shop data remain untouched. Google Drive is not configured in this build yet. A second Samsung is connected, but it already has the ordinary Saaf app, so we deliberately left it alone; it can become the recovery test phone only after confirming that the separate QA installation is wanted. We can start the full role and permission attack-style testing immediately on the primary phone.
