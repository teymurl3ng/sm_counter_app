# Phase 13.1 — Reachability and Risk Inventory

Status: **COMPLETE**

Completed: 2026-09-05

## 1. Method

The inventory used four independent views:

1. recursively resolve every relative TypeScript/TSX import from the production `ui/src/main.tsx` entry;
2. search the full UI source for browser storage, mock/fake/demo markers, hardcoded business values, synthetic calculations, arbitrary import/export, clear-all paths, disabled/no-op controls, and success language;
3. cross-check retained screen families and all native-backed operations against `PHASE10_CAPABILITY_AUDIT.md`, contract v4, and the Phase 12 handoff;
4. inspect arithmetic and derived text in the active tree to separate display formatting from business meaning.

This is a source-reachability inventory, not a claim that deleted history never existed. Git remains the archive.

## 2. Import-graph result

| Classification | Files | Source lines | Decision |
|---|---:|---:|---|
| Reachable from `main.tsx` | 85 | Not a deletion target | Retain and audit |
| Unreachable prototype | 23 | 8,370 | Remove in 13.2 |
| Total TypeScript/TSX | 108 | — | Fully classified |

The live entry is `main.tsx → App.tsx → HybridAppShell.tsx`. No reachable source imports the legacy `useMilkShopViewModel`, legacy screen tree, or `Phase7HybridShell`.

## 3. Locked 13.2 removal list

### Browser-local business engine and prototype types

- `ui/src/viewmodels/useMilkShopViewModel.ts`
- `ui/src/types/milkShop.ts`
- `ui/src/types.ts`

### Obsolete prototype screens

- `ui/src/components/accounts/CustomerLedgerView.tsx`
- `ui/src/components/accounts/LedgerHubView.tsx`
- `ui/src/components/expenses/ExpensesView.tsx`
- `ui/src/components/inventory/InventoryView.tsx`
- `ui/src/components/layout/Navbar.tsx`
- `ui/src/components/management/EmployeesView.tsx`
- `ui/src/components/management/ManagementHubView.tsx`
- `ui/src/components/operations/DailyOperationsView.tsx`
- `ui/src/components/reports/DailyCloseSummaryModal.tsx`
- `ui/src/components/reports/ReportsView.tsx`
- `ui/src/components/settings/SettingsView.tsx`
- `ui/src/components/suppliers/SupplierLedgerView.tsx`
- `ui/src/components/wholesale/WholesaleView.tsx`

### Obsolete prototype helpers and early shell

- `ui/src/components/common/Badge.tsx`
- `ui/src/components/common/ConfirmModal.tsx`
- `ui/src/components/common/MetricCard.tsx`
- `ui/src/components/common/Modal.tsx`
- `ui/src/components/common/NoProductErrorModal.tsx`
- `ui/src/components/common/PaymentTransactionView.tsx`
- `ui/src/components/hybrid/Phase7HybridShell.tsx`

### Dependencies used only by the dead tree or by no source

- `clsx`
- `html2canvas`
- `jspdf`
- `motion`
- `recharts`
- `tailwind-merge`

These dependencies are not present in the active import graph. Removing them will reduce install/maintenance surface. The current Vite JavaScript bundle already excludes the unreachable modules; any CSS or package-size change must be measured after deletion.

## 4. Why the unreachable tree must go

The locked tree contains exactly the risks Phase 13 was created to remove:

- extensive `localStorage` persistence for products, parties, staff, transactions, settings, closing state, cash counts, and audit logs;
- hardcoded product costs and stock plus UI-side stock, balance, expense, cash, spoilage, and profit calculations;
- arbitrary JSON export/import and a casual clear-all-data action;
- browser-generated backup and restore-success presentation that bypasses the native encrypted backup boundary;
- reports that scrape browser keys and create a synthetic trend using `Math.sin`;
- browser-side receipt/image/PDF generation that is not tied to an approved native export capability;
- an obsolete early hybrid shell duplicated by the active session/workspace architecture.

The code is unreachable in the installed app, so this is removal of misleading source surface rather than removal of a current user capability.

## 5. Active-tree findings

### Confirmed clean or intentional

- No reachable code reads or writes `localStorage`, `sessionStorage`, IndexedDB, cookies, or a browser business database.
- Active business reads and mutations use the canonical native bridge and await native responses before showing success.
- Form drafts, dialog state, selected tabs, and in-memory native responses are presentation state, not a second business store.
- Input placeholders are examples only and do not seed records.
- `sampleStartDate` / `sampleEndDate` are native yield-calibration interval fields, not sample data.
- Text such as “does not create a fake purchase/balancing entry” is an honest restriction, not a fake implementation.
- A JSX button inventory found 122 active buttons. The only three without an inline click handler are the owner-setup, recovery, and login submit buttons; each submits through the shared `AuthCard` form and reaches the native authentication actions.
- Current disabled states reviewed in this pass are tied to busy state, missing prerequisites, selection/calibration thresholds, closed dates, or permission-aware availability. Native permission enforcement remains authoritative.
- File-size conversion, currency formatting, sign labels, date defaults, and display-only status mapping do not create business truth.

### Active issue carried to 13.3

`OpeningStatePanel.tsx` displays **Recorded money total** by adding every native opening account balance in React. Cash/bank, customer receivable, and supplier payable balances have different meanings, so one unsigned combined number can mislead. The native DTO does not define this as an accounting total.

Checkpoint 13.3 must remove or replace that presentation with source-faithful information. It must not invent net-assets, equity, or another formula in React.

## 6. Decision

Checkpoint 13.1 is complete. The 23-file removal list is fully unreachable and safe to delete as one coherent prototype tree. Active hybrid source is retained. The opening-balance total is deliberately deferred to the active semantic audit rather than mixed into the deletion step.

## Plain English

There are two UI codebases in the folder, but only one is actually used. The installed app uses the secure Android-backed version. The other is an old demo with browser-saved records, made-up report behavior, and unsafe-looking backup controls. We mapped every file so we can delete only that unused demo next. We also found one real screen label/total in the active app that needs a careful fix afterward.
