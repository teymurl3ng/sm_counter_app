# Phase 13.5 — Complete Regression and Phase 14 Handoff

Status: **COMPLETE**

Completed: 2026-09-06

## 1. Phase 13 closure

Phase 13 is complete. The unreachable browser-local prototype is removed, the active UI and all literal controls are audited, the misleading combined opening-balance total is replaced with a truthful explanation, and mandatory web/Android build guards now reject reintroduction of the removed or misleading surface.

Checkpoint 13.5 made no product, schema, bridge, formula, permission, security, or workflow change. It rebuilt the exact final source, reran the complete regression, preserved the every-screen physical-device evidence, and defined the guarded Phase 14 entry.

## 2. Final verification matrix

All results below are from the final source and freshly rebuilt packaged React assets.

| Gate | Final result |
|---|---|
| Cleanup guard self-test and source/package scan | PASS — 85 active TypeScript/TSX files; 23 prototype paths and six dependencies absent; three packaged files scanned |
| Canonical bridge verifier | PASS — contract v4, 86 endpoints, manifest SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136` |
| React type check and production build | PASS — `tsc`; Vite transformed 1,897 modules |
| Android packaged web assets | PASS — packaged index, CSS, and JavaScript exactly match `ui/dist` |
| Android debug packaging | PASS |
| Kotlin production and Android-test compilation | PASS |
| Host debug unit tests | PASS — 7/7, 0 skipped, 0 failed |
| Android instrumentation inventory | 258 `@Test` methods across 76 source files |
| Main isolated-QA Samsung regression | PASS — 257/257, 0 skipped, 0 failed; 6m37s Gradle run |
| Empty-identity visual/a11y Samsung partition | PASS — 1/1, 0 skipped, 0 failed; 83 screenshots / 58 states |
| Complete Phase 13 Samsung regression | PASS — 258/258 across the two required clean-state partitions |
| Final debug APK | 16,086,533 bytes; SHA-256 `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C` |

The two phone partitions remain deliberately separate. The 257-test partition uses the authenticated isolated-QA owner, while the visual test must begin with an empty identity at first-owner setup. This preserves real onboarding coverage and avoids state-dependent skips.

## 3. Removed-surface and active-surface evidence

- The locked 23-file, 8,370-line prototype tree remains absent. It contained the unused browser-local store, hardcoded business data, UI-side stock/balance/profit logic, synthetic reports, arbitrary JSON import/export, casual clear-all data, fake backup success, and unapproved receipt/image/PDF generation.
- The six prototype-only direct dependencies remain absent: `clsx`, `html2canvas`, `jspdf`, `motion`, `recharts`, and `tailwind-merge`.
- The production graph remains 85 reachable files / 85 total / zero unreachable.
- The active audit still accounts for 120 literal buttons with zero disconnected controls.
- The active opening-state UI no longer combines cash/bank, customer, and supplier amounts into one unsigned “money total”; it explains that these meanings stay separate.
- `scripts/verify-ui-cleanup.py` enforces the negative rules through npm/Vite, post-package verification, and Android `preBuild`, so the cleanup is reproducible rather than a one-time manual observation.

The detailed candidate list, removal record, active-control audit, and guard design remain in `PHASE13_1_INVENTORY.md`, `PHASE13_2_PROTOTYPE_REMOVAL.md`, `PHASE13_3_ACTIVE_UI_AUDIT.md`, and `PHASE13_4_NEGATIVE_GATE.md`.

## 4. Final visual evidence

- Device: Samsung SM-A065F, Android 16.
- Evidence: `phase13-evidence/sm-a065f-android16-phase13-5-final/`.
- Coverage: 83/83 readable PNGs across 58 retained states, all 720 × 1600.
- Manifest: 85 lines, ending in `complete\t83-screenshots\tcomplete\tsamsung SM-A065F\t16`.
- Manifest SHA-256: `8E9937FC972B2EE36D8E31D5FB58C0C7C86189D4C8A8F116AFA91C727B45F1BE`.

This rerun preserves the same screen/state inventory and device metadata as the accepted Phase 12 evidence. Expected date- and seeded-record-dependent pixels are not treated as design regressions.

Screen judgment continues to follow the proportional benchmark recorded in `PRODUCT_SCOPE.md` and `DESIGN.md`: Android guidance, WCAG 2.2, ISO 9241-11, Nielsen usability heuristics, honest small-business/accounting presentation, and the real Samsung evidence. This is not a claim that Saaf Milk matches an enterprise ERP or has external accessibility certification.

## 5. Immutable and packaged hashes

- Logic specification SHA-256: `45E5BDDABE8861A1B3DF7C5666AC7673078439A89FA5628061AD293A865F9DEA`.
- SQL blueprint SHA-256: `FB701C90D2DD97A1E6D962154692C48EA0F4E77E9300F9DFAC653D4AB67BBC51`.
- Cleanup guard SHA-256: `34DD2D770500DAB5403328F743F166BF7C18C4952FE728B00818D4485E902E69`.
- Packaged `index.html`: 906 bytes; SHA-256 `7520F73F9C557C1B15206F93DDA6094C5637A26F9C38ED161E3D35723A0B7F71`.
- Packaged JavaScript: 433,808 bytes; SHA-256 `B3587C236B522636046ECBFD50AEF6194CB5F5409C4BC9882F6F803F97E63C41`.
- Packaged CSS: 34,031 bytes; SHA-256 `DC9DD86BBD7EA0D5B6D5AC69CCFDD9556B5D1C2CB94188F97074A4BF78D87DFE`.

## 6. Restrictions that Phase 14 must preserve

- Room remains the only writable business truth. Kotlin owns validation, authorization, accounting, precision, encryption, audit, backup, and recovery.
- React renders native state and dispatches existing native actions. It must not gain browser-local business storage, duplicate formulas, synthetic reports, fake users, or unconditional success.
- Bridge contract v4 and all 86 operations remain frozen unless a separately approved additive capability passes its own design and regression gate.
- Memory-only sessions, native permission enforcement, restricted app-assets origin, fresh-WebView recovery, and session revalidation before protected data appears remain mandatory.
- Backup remains recovery, not synchronization. Restoring backups into independently writable phones must never be presented as live shared data.
- Milk/Yogurt are the current validated workflows, not the permanent product limit. Unsupported product formulas must be designed and validated before being claimed.
- UI judgment remains proportional to an owner-managed small-business product. The current evidence is not a claim of enterprise ERP parity, statutory-accounting completeness, or external accessibility certification.

## 7. Known limitations and Phase 14 device-QA handoff

- Automated layout/accessibility checks and screenshot review are guardrails. Formal TalkBack, large-font, Urdu/multilingual, and uncoached owner/staff usability testing remain acceptance work.
- Final automated evidence still comes from one Samsung model on Android 16. Broader phone/device coverage, Android sharing behavior, release signing, Play distribution, tablet/foldable behavior, and production configuration are not proven.
- Live production Google consent/Drive upload and download, scheduled retry while offline, missed scheduling, provider sign-out/revocation, quota/retention behavior, corrupt/wrong-passphrase rejection, and restoration on a second physical device remain unproven.
- Formal NRV valuation remains conditionally deferred under `PHASE11_5_NRV_TRIGGER_REASSESSMENT.md`; the app makes no statutory or IFRS-compliance claim.
- Real shopkeeper acceptance remains Phase 15. Release optimization, migration/security review, production configuration, signing, and packaging remain Phase 16.

Phase 14 should therefore validate the finished hybrid app on real devices rather than redesign it:

1. exercise the complete role/permission and attempted-bypass matrix on the packaged app;
2. prove real Google account connection, manual and scheduled encrypted Drive backup, offline/missed-run retry, sign-out/revocation, quota/retention, and safe degraded operation;
3. prove wrong-passphrase/corrupt-backup rejection and full replacement restore on a second physical device, including users, actor history, session invalidation, and post-restore totals;
4. complete TalkBack, large-font, lifecycle, Android Back, document-picker/sharing, network-transition, and broader device checks;
5. record failures as targeted issues without moving business authority into React or presenting backup as synchronization.

Any destructive restore exercise must use dedicated QA data and a recoverable backup, never an unprotected live shop database.

## Plain English

Phase 13 is finished. We removed the disconnected demo code, checked every live control, corrected the one misleading total, and added build rules that stop the old behavior from returning. Then we rebuilt everything and passed all 258 phone tests plus 7 computer-side tests. We also captured and validated all 83 review screenshots. Next is Phase 14: test the finished app in harder real-device situations, especially real Google Drive backup, offline/retry behavior, accessibility, permissions, and restoring safely on another phone.
