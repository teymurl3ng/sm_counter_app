# Phase 13.3 — Active Control and Meaning Audit

Status: **COMPLETE**

Completed: 2026-09-05

## 1. Audit scope

The audit covered the complete 85-file production TypeScript/TSX graph and cross-checked its controls, permission and disabled states, success/error presentation, placeholders, and display arithmetic against the native bridge and native-supplied DTOs.

The core, database, formulas, bridge contract, and native authorization behavior remained read-only.

## 2. Active-control result

A TypeScript AST pass found 120 literal `<button>` elements in the active hybrid component tree:

- 83 dispatch a direct `onClick` interaction;
- 34 submit a containing form with an `onSubmit` handler;
- the remaining three authentication buttons are children of the shared `AuthCard` form and submit that form at runtime;
- zero active buttons are disconnected;
- 86 buttons carry an explicit busy, prerequisite, selection, closed-date, or confirmation guard where disabling is meaningful.

Navigation and complete screen availability derive from the native `effectivePermissionCodes`. Native command authorization remains authoritative even when the UI has already hidden an unavailable action.

## 3. Messages, placeholders, and presentation arithmetic

- Shared mutation notices are shown only after the native command and its authorized refresh complete successfully.
- Backup, security, costing, planning, delivery, history, attendance, and reconciliation paths likewise wait for their native result before presenting completion.
- Errors and user cancellation do not claim that a business mutation completed.
- Eight active placeholders are search guidance, a sale-record-number hint, or a visible zero example. None inserts a business value or creates mock data.
- Remaining client arithmetic is presentation-only: PKR/file-size formatting, sign-to-language mapping for native balances and cash variance, viewport sizing, counts, and input-shape validation. Business totals, stock, balances, profit, and costing results continue to come from native DTOs.

No active no-op, self-authorized mutation, synthetic result, or duplicated business store was found.

## 4. Corrected misleading opening-balance total

`OpeningStatePanel.tsx` previously added cash/bank balances, customer receivables, and supplier payables into one unsigned **Recorded money total**. Those values have different meanings, and the native opening-state DTO defines no combined accounting total.

The UI now:

- reports only the source-faithful number of recorded account balances;
- keeps the three meanings separate;
- explicitly explains that they are not combined into one total;
- performs no opening-balance accounting calculation in React.

The packaged UI and focused device test both assert that **Recorded money total** is absent.

## 5. Verification

- TypeScript no-emit check — passed.
- Bridge contract — v4, 86 endpoints, SHA-256 `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.
- Production Vite build — passed; 1,897 modules transformed.
- Android production and instrumentation Kotlin compilation — passed.
- Host debug unit tests — 7/7 passed, 0 failed, 0 skipped.
- Focused Samsung SM-A065F / Android 16 opening-state suite — 4/4 passed, 0 failed, 0 skipped.
- Complete Android test inventory remains 258 tests; the existing opening-state device test was strengthened rather than adding a duplicate test.
- Packaged CSS — 34,031 bytes, SHA-256 `DC9DD86BBD7EA0D5B6D5AC69CCFDD9556B5D1C2CB94188F97074A4BF78D87DFE`.
- Packaged JavaScript — 433,808 bytes, SHA-256 `B3587C236B522636046ECBFD50AEF6194CB5F5409C4BC9882F6F803F97E63C41`.
- Packaged `index.html` — SHA-256 `7520F73F9C557C1B15206F93DDA6094C5637A26F9C38ED161E3D35723A0B7F71`.
- Debug APK — SHA-256 `ED06E0D2122E320B2BB8086C3B8B6AB9C5DACFB0ADEB57BD3279C4A54DD4D0BF`.

## Plain English

Every live button still leads somewhere real, and permissions and waiting states match the native app. Messages only say something succeeded after Android confirms it. We removed one bad number that mixed cash, money customers owe, and money owed to suppliers; the screen now keeps those meanings separate and explains why there is no grand total.
