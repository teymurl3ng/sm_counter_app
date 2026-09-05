# Phase 13.4 — Negative-Source and Focused Device Gate

Status: **COMPLETE**

Completed: 2026-09-06

## 1. Repeatable cleanup guard

`scripts/verify-ui-cleanup.py` now fails when the removed or misleading surface returns. It checks the complete active TypeScript/TSX tree and the packaged HTML/JavaScript/CSS for:

- any of the 23 locked prototype paths;
- `localStorage`, `sessionStorage`, or IndexedDB business state;
- the synthetic `Math.sin` report curve;
- the obsolete `useMilkShopViewModel` or `Phase7HybridShell`;
- browser-owned file import and old arbitrary import/export/clear-all action names;
- removed presentation-package markers;
- any `JSON.parse` outside the one allowed native bridge response boundary;
- the six removed package or lockfile entries;
- the misleading **Recorded money total** wording or an `accountBalances.reduce(...)` calculation;
- loss of the active `main.tsx → App.tsx → HybridAppShell.tsx` entry;
- loss of the opening-account explanation that unlike balances are not totaled.

Its `--self-test` mode proves representative forbidden patterns are rejected before it verifies the real tree.

## 2. Enforced build entry points

The cleanup gate is not an optional checklist:

- `npm run build` invokes it through `prebuild`;
- `scripts/build-web-ui.ps1` invokes it again after copying Vite output into Android assets;
- Android `preBuild` depends on the Gradle `verifyUiCleanup` task;
- it can also be run directly with `python scripts/verify-ui-cleanup.py --self-test`.

The bridge-contract gate remains independent and unchanged.

## 3. Focused physical-device gate

The isolated QA installation ran 31 tests on Samsung SM-A065F / Android 16 with zero failures and zero skips. The matrix covered:

- secure authentication and the hybrid bridge;
- Product management;
- Settings and expense categories;
- encrypted file Backup/Restore;
- Dashboard and reports;
- opening-state behavior and the corrected meaning presentation;
- Phase 12 navigation flow;
- Phase 12 presentation foundation;
- Phase 12 responsive and accessibility behavior.

## 4. Verification

- Cleanup guard self-test and real source/package scan — passed.
- Active UI inventory — 85 TypeScript/TSX files.
- Locked absence inventory — 23 prototype paths and six removed dependencies.
- Packaged inventory — one HTML, one JavaScript, and one CSS file scanned.
- Web `prebuild`, TypeScript, Vite, and post-package cleanup gates — passed; 1,897 modules transformed.
- Android `verifyUiCleanup`, bridge verification, production compilation, and instrumentation compilation — passed.
- Host debug unit tests — 7/7 passed, 0 failed, 0 skipped.
- Focused isolated-QA physical-device suite — 31/31 passed, 0 failed, 0 skipped.
- Complete Android test inventory remains 258; the broader complete regression is checkpoint 13.5.
- Cleanup guard SHA-256 — `34DD2D770500DAB5403328F743F166BF7C18C4952FE728B00818D4485E902E69`.
- Bridge contract SHA-256 — `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Packaged JavaScript — 433,808 bytes, SHA-256 `B3587C236B522636046ECBFD50AEF6194CB5F5409C4BC9882F6F803F97E63C41`.
- Packaged CSS — 34,031 bytes, SHA-256 `DC9DD86BBD7EA0D5B6D5AC69CCFDD9556B5D1C2CB94188F97074A4BF78D87DFE`.
- Packaged `index.html` SHA-256 — `7520F73F9C557C1B15206F93DDA6094C5637A26F9C38ED161E3D35723A0B7F71`.
- Isolated-QA debug APK SHA-256 — `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C`.

## Plain English

The cleanup rules are now automatic. If someone brings back the old browser database, demo reports, unsafe browser restore controls, deleted packages, old shell, or misleading opening total, both the web and Android builds stop. The important cleaned-up screens also passed 31 tests on the Samsung.
