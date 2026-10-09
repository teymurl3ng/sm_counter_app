# HYBRID PARITY AUDIT — WebView (retiring) vs Native UI

Audited 2026-10-09. Scope: prove **zero retained hybrid-only capabilities** before the 14.8.6
production cutover (ROADMAP_RECONCILIATION gate; 14.8.1 bridge-retirement gate, conditions 1–2).

**VERDICT: ZERO retained hybrid-only capabilities. The native app is a strict superset of the
WebView UI.** Every one of the 91 bridge actions the WebView invokes is implemented natively
(same command services, reached through typed facades); the native app adds ~13 capability
areas the WebView never had. The WebView's only exclusive is the startup handshake
`system.bridgeInfo`, which is by-design bridge-only and retires with the bridge.

## 1 · WebView capability inventory (what the retiring UI can do)

The web UI (`ui/src`, 88 active TS/TSX files) calls native over the injected WebMessage bridge
(`saafMilkNative`, contract v5 pinned). It invokes **91 distinct actions** (identical to its
`RUNTIME_ACTIONS` handshake list). Screen families and their actions:

| Family | User capabilities | Bridge actions |
|---|---|---|
| Auth (setup / sign-in / recovery) | Bootstrap owner; passphrase login; Quick unlock (biometric / 4–12 digit PIN); owner recovery code | `command.auth.bootstrapOwner / login / quickLogin / recoverOwner` + `system.quickUnlockStatus`, `query.bootstrapStatus/currentSession` |
| Dashboard / Reports | Date-range report; operational, cashier, financial sections; per-product profitability | `query.dashboard / cashierSummary / financialOverview / profitabilityReport` |
| Daily work (7 task tabs) | Purchase, Sale (channels, credit, surcharge), Production, Spoilage, Expense, Payments (vendor + collection), Salary | `command.recordPurchase / recordSale / recordProduction / recordSpoilage / recordExpense / recordVendorPayment / recordCreditCollection / recordSalaryPayment` |
| Deliveries | Create, deliver, cancel-with-note, link sale, archive | `command.createDelivery / transitionDelivery / linkDeliverySale / archiveDelivery`, `query.deliveries` |
| Accounts | Customer/supplier balances, ledgers, record payment/collection | `query.accountLedger / financialOverview`, `command.recordVendorPayment / recordCreditCollection` |
| History + corrections | Search, detail, per-type corrections | `query.eventHistory / eventDetail`, `command.correctEvent` |
| Cash close | Count, close with variance reason, reopen with re-auth, archive | `command.recordPhysicalCashCount / closeBusinessDate / reopenBusinessDate`, `command.auth.reauthenticate`, `query.reconciliationArchive` |
| Products / Customers / Suppliers / Employees | Full CRUD + profiles + attendance | `command.create/update/setProductArchived`, `command.create/renameCustomer`, `command.create/renameVendor`, `command.create/renameEmployee`, `command.saveCustomerProfile / saveVendorProfile / saveEmployeeProfile`, `command.recordAttendance`, `query.masterData / transactionReferences / attendance` |
| Costing & plans | Budgets, copy previous, collections targets, costing policy approval, opening cost basis, yield policy + loss assessment | `query.costingManagement / planning`, `command.upsertBudget / copyPreviousBudget / upsertCollectionsTarget / approveCostingPolicy / recordOpeningCostBasis / approveYieldPolicy / recordProductionLossAssessment` |
| Settings | Store identity, rates, opening record/correct, expense categories | `query.businessSettings`, `command.saveStoreIdentity / updateBusinessSettings / recordOpeningState / correctOpeningState / createExpenseCategory / setExpenseCategoryArchived` |
| Access (account / staff / audit) | Passphrase change, Quick unlock enroll/remove, staff enrollment + resume + reset, role change, enable/disable, security audit | `query.users / securityAudit`, `command.auth.changeOwnPassphrase / reauthenticate / beginUserEnrollment / resumeUserEnrollment / completeEnrollment / changeUserRole / setUserEnabled / beginCredentialReset / enrollQuickUnlock / removeQuickUnlock / quickReauthenticate` |
| Backup / cloud | Encrypted export/inspect/restore; Drive connect/configure/run/refresh/inspect/restore/disconnect | `system.exportEncryptedBackup / inspectEncryptedBackup / restoreInspectedBackup / cloudBackupStatus / connectCloudBackup / configureCloudBackup / runCloudBackupNow / refreshCloudBackups / inspectCloudBackup / restoreInspectedCloudBackup / disconnectCloudBackup` |
| Header | Sign out | `command.auth.logout` |

Pure-web (no bridge): client-side date windows, paisa/PKR formatting + validation, search/filter,
pending counters, presentation-only derivations (Phase 13.3 confinement), web-history navigation
with popstate, dirty-form discard handshake, refresh-on-visible, IME viewport CSS. **No print,
export, CSV, or charting exists** (removed with the Phase 13.2 prototypes; verified absent).

## 2 · Native coverage verdict — all 91 natively implemented

The native screens never call the JSON bridge; they call typed facades which reuse the SAME
command services the bridge handlers dispatch to (`OperationsBridgeCommandService`,
`ReconciliationBridgeCommandService`, `HistoryBridgeCommandService`,
`EmployeeBridgeCommandService`, `DeliveryBridgeCommandService`, master-data/settings/costing/
security facades). Endpoint-ownership closure was declared at 14.8.55b (91 endpoints);
`command.recordOwnerDrawing` (92nd, added 2026-10-08) is native-first with its own Work screen.
`system.bridgeInfo` is the sole by-design exception (handshake only).

## 3 · Native-only capabilities (WebView never had these)

1. **Sync** — enrollment (owner + counter-device), enroll-in-shop choice, sync now, unenroll,
   rebaseline hold resolution ("keep mine" / "take server"), refused-push counter, last-synced
   signal, identity card with grace countdown (`sync/`, `OwnerSyncScreen`).
2. **Owner Overview + Month reports** — attention detection, stock, estimated result.
3. **Role work desks** — attention strips and setup-health counters (admin private-enrollment
   deep links, missing-rate detection, unlinked-staff detection).
4. **Cashier Activity screen** — own records + detail.
5. **Operations "Today's work"** — own receive/production/spoilage records, actor-linked.
6. **Cash-close depth** — absorbed-days enumeration + seal rows, pending-delivery one-tap
   resolve, refused-push 24 h count, reopen note, degraded-mode copy.
7. **Blind cashier count** with haptics.
8. **Full native authentication UX** — bootstrap, account picker, method picker, Quick PIN,
   phone security, recovery-code ack, Lock/switch user.
9. **Personal-security enrollment** in every role's More menu.
10. **Backup/restore UI depth** — Drive OAuth via Credential Manager, policy controls, cloud
    recovery-point inspect/restore, post-restore quick-unlock wipe, "replaces installation"
    confirm.
11. **Role-preview host shortcuts** + day-status chip action injection.
12. **Sample-data seeding** for preview/first-run.
13. **UI-safety machinery** — entry leave guards with discard dialogs, session
    foreground/background tracking, portrait lock.

## 4 · WebView-only (and why it's fine)

- `system.bridgeInfo` — startup handshake; by-design bridge-only; retires with the bridge.
- Nothing else. Grep-verified: no print/export/CSV/chart/download in `ui/src`.

## 5 · Standing items (tracked, not parity gaps)

- **M27** — sync + cloud-backup notification taps still open the dormant `HybridActivity`
  (the only production routes into the WebView; fix = repoint to the native activity).
- **M29** — 5 files in `application/bridge/` are live write-transaction logic called by 9 native
  facades (refactor/rename, not delete); the other 24 bridge files are dormant.
- **Stale proof artifacts** — ownership matrix JSON (91, Sep 26) and CONTRACT.md prose (91) lag
  the 92-endpoint contract; refreshed alongside this audit.
- **M28 post-pilot** — MainActivity + presentation/ removal, then the WebView/bridge deletion
  phase.

## 6 · Evidence anchors

- WebView action list: `ui/src/bridge/nativeBridge.ts` `RUNTIME_ACTIONS` (91) + per-family
  `*BridgeActions.ts` files.
- Native handling: `application/bridge/BridgeContractSnapshot.kt` (92 rows, contract-gated) +
  handler `when` tables.
- Ownership matrix: `phase18/qa/ownership_matrix_raw.json` (refreshed to 92, see Step 2).
- Gates live on every build: `verifyBridgeContract` (JSON ↔ Kotlin ↔ TS alignment),
  `verifyUiCleanup` (prototype/misleading-UI regression), `verifyHybridWebAssets` (packaged
  bundle currency).
