# Phase 13.2 — Unreachable Prototype Removal

Status: **COMPLETE**

Completed: 2026-09-05

## 1. Removed source surface

The exact removal list locked in `PHASE13_1_INVENTORY.md` was applied:

- 23 unreachable TypeScript/TSX files;
- 8,370 lines of obsolete prototype source;
- the browser-local `useMilkShopViewModel` business store and prototype types;
- 13 obsolete screen modules;
- six obsolete common/receipt helpers;
- the duplicated early `Phase7HybridShell`.

The removed tree contained localStorage business records, hardcoded costs/stock, UI-side stock/balance/profit logic, synthetic report curves, arbitrary JSON import/export, casual clear-all data, browser-generated backup success, and unapproved receipt/image/PDF generation. None was imported by the production entry. Git history retains it if historical inspection is ever required.

## 2. Removed dependencies

The following six direct production dependencies were removed from `package.json`, `package-lock.json`, and the installed dependency tree:

- `clsx`;
- `html2canvas`;
- `jspdf`;
- `motion`;
- `recharts`;
- `tailwind-merge`.

Direct production dependencies fell from nine to three: React, React DOM, and Lucide React. The uninstall removed 68 installed packages. No active source imports any removed package.

## 3. Measured effect

| Surface | Before | After | Change |
|---|---:|---:|---:|
| TypeScript/TSX files | 108 | 85 | −23 |
| Unreachable TypeScript/TSX files | 23 | 0 | −100% |
| Locked prototype lines | 8,370 | 0 | −8,370 |
| Direct production dependencies | 9 | 3 | −6 |
| `package-lock.json` | 110,800 bytes | 83,538 bytes | −27,262 bytes / 24.6% |
| Production CSS | 85,709 bytes / 14.11 kB gzip | 34,031 bytes / 7.48 kB gzip | −51,678 bytes / 60.3% raw |
| Production JavaScript | 433,759 bytes / 111.18 kB gzip | 433,759 bytes / 111.18 kB gzip | no byte-size change |

The JavaScript content SHA-256 remains exactly `79F10653447D3E47931222C79A2F01F04C7CCC236A4995232E7668E4B9EFAEEF`. This independently confirms that the removed files were absent from the active JavaScript bundle. The CSS reduction occurs because Tailwind no longer sees thousands of class names in unreachable source.

## 4. Final source/package state

- Import graph: 85 reachable files / 85 total / 0 unreachable.
- Browser-state and prototype negative scan: no `localStorage`, `sessionStorage`, IndexedDB, synthetic `Math.sin` reporting, arbitrary import/clear-all functions, old view model, or old shell remains under `ui/src`.
- Production dependency negative scan: no active or lockfile package entries for the six removed direct dependencies.
- Bridge: v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Packaged `index.html`: SHA-256 `6F9133BF9FDE69461654768EBECF45811F7347D70AD3565A876C0D70802F2F3C`.
- Packaged CSS `index-zvzBHC5F.css`: SHA-256 `DC9DD86BBD7EA0D5B6D5AC69CCFDD9556B5D1C2CB94188F97074A4BF78D87DFE`.
- Packaged JavaScript `index-B9IhaZhB.js`: SHA-256 `79F10653447D3E47931222C79A2F01F04C7CCC236A4995232E7668E4B9EFAEEF`.
- Debug APK: SHA-256 `25094F7832C780AF2DB8FB060EE5D4C0E144697484E40F7D3D8BE958DFBD4661`.

## 5. Verification

- TypeScript no-emit check — passed.
- Production Vite build — passed; 1,897 active modules transformed.
- Packaged UI/dist hash parity — passed for index, CSS, and JavaScript.
- Android debug packaging and production/Android-test Kotlin compilation — passed.
- Host debug unit tests — 7/7 passed, 0 skipped, 0 failed.
- Focused Samsung SM-A065F / Android 16 proof — 12/12 passed, 0 skipped, 0 failed. It covers authenticated hybrid flow, Product management, Phase 12 presentation/navigation, and responsive/accessibility behavior.

Checkpoint 13.2 intentionally did not alter the one active heterogeneous opening-balance total found in 13.1. That source-visible behavior remains checkpoint 13.3 work so prototype deletion and active semantic changes stay independently reviewable.

## Plain English

The old demo UI is gone from the working source, along with the packages only that demo used. The real app's JavaScript is byte-for-byte the same size and has the same content hash, while its CSS is about 60% smaller because unused demo styles are no longer generated. Login, Products, navigation, and responsive screens still pass on the Samsung. Everything removed remains recoverable from Git history.
