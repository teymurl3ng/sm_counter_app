# Phase 14.8.55 — endpoint-ownership closure

Date: 2026-09-26
Branch: `main` (follows 14.8.54c, commit `70c6db0`)

## Method

Mechanical, from source: `bridge/contract.json` (91 actions) -> `BridgeProtocol.kt` constants -> per-handler
`when`-branch slices (with one-level private-helper expansion, since handlers route through helpers like
`login(request)`) -> identifier set -> search of the native-reachable corpus (`nativeui/**` + `application/**`
excluding `application/bridge/**`). Actions whose identifiers appear in native-reachable code are NATIVE;
the remainder were reviewed manually at facade level. Raw per-action data:
`phase18/qa/ownership_matrix_raw.json`.

## Result

- **NATIVE (with evidence): 91 of 91** — the two gaps found by this audit were closed in checkpoint 14.8.55b
- **BY-DESIGN hybrid-only (no native caller required): 1** (`system.bridgeInfo`, retires with the bridge)


| Verdict | Count |
|---|---|
| NATIVE | 88 |
| GAP | 2 |
| BY-DESIGN | 1 |

## Gap 1 — `command.linkDeliverySale`

The hybrid deliveries workspace (DeliveryList.tsx) offered a per-row "Link a recorded sale" numeric input
for every active delivery without a linked sale, calling `DeliveryService.linkSale`. The native Deliveries
workflow lists pending rows only (mark delivered / cancel) and never displays delivered/cancelled rows or
linking. Native closure requires: facade extension (list linkable rows + `linkSale` mirroring bridge
validation), ViewModel state, screen UI with tags, inventory rows, and contract tests.

## Gap 2 — `command.auth.resumeUserEnrollment`

Re-opens staff credential setup for a user stuck in setup-required state. The native Users & Roles facade
exposes enroll/disable/role-change/credential-reset but no resume action; the only native-side caller is the
preview data seeder. Native closure: add a resume-setup action to `NativeOwnerUsersRolesFacade` + user-row
UI when `credentialSetupRequired`, with tags/inventory/tests.

## By-design — `system.bridgeInfo`

This is the WebView handshake itself (contract version + supported actions for the JS runtime). It has no
native caller by definition and is deleted together with the bridge at 14.8.6.

## Gate — CLOSED 2026-09-26

Both gap items received native callers in checkpoint 14.8.55b (deliveries link-sale flow; Users & roles
resume-setup action) with device contract tests. The 14.8.6 retirement precondition is satisfied.

## Verification

- Analysis script output committed as `phase18/qa/ownership_matrix_raw.json`
- All 91 actions accounted for; no action left unclassified

### command.*

| Action | Verdict | Evidence |
|---|---|---|
| `command.approveCostingPolicy` | NATIVE | mechanical: ids [approveCostingPolicy, optionalString, requiredDecimal, requiredObjectArray, requiredPaisa] -> nativeui/owner/NativeOwnerMoreViewModel.kt +1 more |
| `command.approveYieldPolicy` | NATIVE | mechanical: ids [approveYieldPolicy, get, matches, requiredArray, requiredDate] -> application/admin/NativeAdminWorkFacade.kt +24 more |
| `command.archiveDelivery` | NATIVE | mechanical: ids [archive, create, linkSale, optionalString, requiredDate] -> application/operations/NativeDeliveriesFacade.kt +31 more |
| `command.auth.beginCredentialReset` | NATIVE | mechanical: ids [audit, beginCredentialReset, beginEnrollment, beginReset, changeRole] -> application/authentication/NativeAuthenticationFacade.kt +38 more |
| `command.auth.beginUserEnrollment` | NATIVE | NativeOwnerUsersRolesFacade.kt:98 -> security.beginUserEnrollment |
| `command.auth.bootstrapOwner` | NATIVE | mechanical: ids [bootstrapOwner, clearAll, currentSession, fill, needsOwnerBootstrap] -> application/authentication/NativeAuthenticationFacade.kt +33 more |
| `command.auth.changeOwnPassphrase` | NATIVE | mechanical: ids [changeOwnPassphrase, fill, invalidateUser, require, sessionManager] -> application/admin/NativeAdminWorkFacade.kt +71 more |
| `command.auth.changeUserRole` | NATIVE | mechanical: ids [changeRole] -> application/owner/NativeOwnerUsersRolesFacade.kt +1 more |
| `command.auth.completeEnrollment` | NATIVE | mechanical: ids [completeEnrollment, fill, toCharArray] -> application/authentication/NativeAuthenticationFacade.kt +31 more |
| `command.auth.enrollQuickUnlock` | NATIVE | mechanical: ids [enroll, fill, toCharArray] -> application/authentication/NativeAuthenticationFacade.kt +31 more |
| `command.auth.login` | NATIVE | mechanical: ids [currentSession, fill, fullPassphraseAccepted, login, toCharArray] -> application/authentication/NativeAuthenticationFacade.kt +33 more |
| `command.auth.logout` | NATIVE | mechanical: ids [logout] -> application/authentication/NativeAuthenticationFacade.kt +1 more |
| `command.auth.quickLogin` | NATIVE | mechanical: ids [currentSession, fill, login, toCharArray] -> application/authentication/NativeAuthenticationFacade.kt +33 more |
| `command.auth.quickReauthenticate` | NATIVE | mechanical: ids [currentSession, enroll, fill, login, reauthenticate] -> application/authentication/NativeAuthenticationFacade.kt +33 more |
| `command.auth.reauthenticate` | NATIVE | mechanical: ids [fill, fullPassphraseAccepted, reauthenticate, require, sessionManager] -> application/admin/NativeAdminWorkFacade.kt +71 more |
| `command.auth.recoverOwner` | NATIVE | mechanical: ids [currentSession, fill, invalidateUser, recoverOwner, toCharArray] -> application/authentication/NativeAuthenticationFacade.kt +33 more |
| `command.auth.removeQuickUnlock` | NATIVE | mechanical: ids [fill, remove, toCharArray] -> application/authentication/NativeAuthenticationFacade.kt +31 more |
| `command.auth.resumeUserEnrollment` | NATIVE (14.8.55b) | only native caller is preview seeding (NativePreviewSampleData.kt:285); Users & Roles needs a resume-setup flow |
| `command.auth.setUserEnabled` | NATIVE | mechanical: ids [setEnabled] -> application/owner/NativeOwnerUsersRolesFacade.kt +1 more |
| `command.closeBusinessDate` | NATIVE | mechanical: ids [closeDate, optionalString, requiredDate] -> application/cashclose/NativeCashCloseFacade.kt |
| `command.copyPreviousBudget` | NATIVE | mechanical: ids [copyPreviousBudget, requiredDate] -> application/owner/NativeOwnerCostingPlansFacade.kt +1 more |
| `command.correctEvent` | NATIVE | mechanical: ids [correct, correctEvent, eventDetail, eventHistory, movePointLeft] -> application/cashclose/NativeCashCloseFacade.kt +14 more |
| `command.correctOpeningState` | NATIVE | mechanical: ids [correctOpeningState, isFinite, mapObjects, movePointLeft, openingState] -> application/cashclose/NativeCashCloseFacade.kt +19 more |
| `command.createCustomer` | NATIVE | mechanical: ids [createCustomer, strictPayload] -> application/owner/NativeOwnerCustomersFacade.kt +2 more |
| `command.createDelivery` | NATIVE (14.8.55c) | NATIVE | mechanical: ids [create, optionalString, requiredDate, requiredDecimal, requiredNullableId] -> application/owner/NativeOwnerCustomersFacade.kt +30 more |
| `command.createEmployee` | NATIVE | mechanical: ids [createEmployee, strictPayload] -> application/owner/NativeOwnerEmployeesFacade.kt +2 more |
| `command.createExpenseCategory` | NATIVE | mechanical: ids [createExpenseCategory, strictPayload] -> application/owner/NativeOwnerShopSettingsFacade.kt +1 more |
| `command.createProduct` | NATIVE | mechanical: ids [createProduct] -> application/owner/NativeOwnerProductsFacade.kt +2 more |
| `command.createVendor` | NATIVE | NativeOwnerSuppliersFacade.createSupplier -> vendors.create (= VendorManagementService.create, same domain call as HybridCommandService.createVendor) |
| `command.linkDeliverySale` | NATIVE (14.8.55b) | bridge-only; native Deliveries workflow lists pending rows only and never links sales (hybrid DeliveryList.tsx had per-row link input) |
| `command.recordAttendance` | NATIVE | mechanical: ids [movePointLeft, nullableDate, nullablePaisa, optionalString, recordAttendance] -> application/cashclose/NativeCashCloseFacade.kt +11 more |
| `command.recordCreditCollection` | NATIVE | mechanical: ids [movePointLeft, optionalString, paymentInput, recordCreditCollection, requiredDate] -> application/cashclose/NativeCashCloseFacade.kt +11 more |
| `command.recordExpense` | NATIVE | mechanical: ids [movePointLeft, optionalString, recordExpense, requiredDate, requiredPaisa] -> application/cashclose/NativeCashCloseFacade.kt +11 more |
| `command.recordOpeningCostBasis` | NATIVE | mechanical: ids [optionalString, recordOpeningCostBasis, requiredDecimal, requiredPaisa] -> application/owner/NativeOwnerCostingPlansFacade.kt +1 more |
| `command.recordOpeningState` | NATIVE | mechanical: ids [correctOpeningState, mapObjects, movePointLeft, openingState, optionalString] -> application/cashclose/NativeCashCloseFacade.kt +10 more |
| `command.recordPhysicalCashCount` | NATIVE | mechanical: ids [movePointLeft, optionalString, recordCount, requiredDate, requiredPaisa] -> application/cashclose/NativeCashCloseFacade.kt +11 more |
| `command.recordProduction` | NATIVE | mechanical: ids [optionalString, recordProduction, requiredDate] -> application/operations/NativeMakeYogurtFacade.kt +1 more |
| `command.recordProductionLossAssessment` | NATIVE | mechanical: ids [approveCostingPolicy, copyPreviousBudget, costingManagement, movePointLeft, nullableDecimal] -> application/cashclose/NativeCashCloseFacade.kt +14 more |
| `command.recordPurchase` | NATIVE | mechanical: ids [optionalString, positiveDecimal, recordPurchase, requiredDate, requiredDecimal] -> application/operations/NativeReceiveMilkFacade.kt +1 more |
| `command.recordSalaryPayment` | NATIVE | mechanical: ids [movePointLeft, optionalString, paymentInput, recordSalaryPayment, requiredDate] -> application/cashclose/NativeCashCloseFacade.kt +11 more |
| `command.recordSale` | NATIVE | mechanical: ids [movePointLeft, nullableDecimal, nullableId, nullablePaisa, optionalString] -> application/cashclose/NativeCashCloseFacade.kt +12 more |
| `command.recordSpoilage` | NATIVE | mechanical: ids [cashierSummary, dashboard, dateRange, financialOverview, isAfter] -> application/cashclose/NativeCashCloseFacade.kt +18 more |
| `command.recordVendorPayment` | NATIVE | mechanical: ids [movePointLeft, optionalString, paymentInput, recordVendorPayment, requiredDate] -> application/cashclose/NativeCashCloseFacade.kt +11 more |
| `command.renameCustomer` | NATIVE | NativeOwnerCustomersFacade.updateCustomer -> customers.rename + profiles.save |
| `command.renameEmployee` | NATIVE | NativeOwnerEmployeesFacade.updateEmployee -> employees.rename (+ profiles) |
| `command.renameVendor` | NATIVE | NativeOwnerSuppliersFacade.updateSupplier -> vendors.rename + profiles.save |
| `command.reopenBusinessDate` | NATIVE | mechanical: ids [archive, closeDate, movePointLeft, optionalString, reconciliationArchive] -> application/cashclose/NativeCashCloseFacade.kt +17 more |
| `command.saveCustomerProfile` | NATIVE | mechanical: ids [movePointLeft, nullableDecimal, nullablePaisa, saveCustomerProfile] -> application/cashclose/NativeCashCloseFacade.kt +10 more |
| `command.saveEmployeeProfile` | NATIVE | mechanical: ids [movePointLeft, nullableDate, nullablePaisa, saveEmployeeProfile] -> application/cashclose/NativeCashCloseFacade.kt +10 more |
| `command.saveStoreIdentity` | NATIVE | NativeOwnerShopSettingsFacade.saveIdentity -> storeIdentity.save (StoreIdentityService.save) |
| `command.saveVendorProfile` | NATIVE | NativeOwnerSuppliersFacade.updateSupplier -> profiles.save (VendorProfileService.save) |
| `command.setExpenseCategoryArchived` | NATIVE | mechanical: ids [setExpenseCategoryArchived, strictPayload] -> application/owner/NativeOwnerShopSettingsFacade.kt +1 more |
| `command.setProductArchived` | NATIVE | NativeOwnerProductsFacade.archiveProduct -> catalog.setArchived |
| `command.transitionDelivery` | NATIVE | mechanical: ids [optionalString, transition] -> application/operations/NativeDeliveriesFacade.kt |
| `command.updateBusinessSettings` | NATIVE | mechanical: ids [isFinite, movePointLeft, requiredDecimal, requiredPaisa, updateBusinessSettings] -> application/cashclose/NativeCashCloseFacade.kt +19 more |
| `command.updateProduct` | NATIVE | mechanical: ids [updateProduct] -> application/owner/NativeOwnerProductsFacade.kt +2 more |
| `command.upsertBudget` | NATIVE | mechanical: ids [movePointLeft, nullableDecimal, nullablePaisa, requiredDate, upsertBudget] -> application/cashclose/NativeCashCloseFacade.kt +11 more |
| `command.upsertCollectionsTarget` | NATIVE | mechanical: ids [movePointLeft, requiredDate, requiredPaisa, upsertCollectionsTarget] -> application/cashclose/NativeCashCloseFacade.kt +10 more |

### query.*

| Action | Verdict | Evidence |
|---|---|---|
| `query.accountLedger` | NATIVE | mechanical: ids [accountLedger, strictPayload] -> application/owner/NativeOwnerRecordsFacade.kt +2 more |
| `query.attendance` | NATIVE | mechanical: ids [attendance, requiredDate, strictPayload] -> application/readmodels/HybridOperationalReadService.kt +1 more |
| `query.bootstrapStatus` | NATIVE | mechanical: ids [bootstrapStatus] -> application/readmodels/HybridIdentityReadService.kt +1 more |
| `query.businessSettings` | NATIVE | mechanical: ids [businessSettings, strictPayload] -> application/owner/NativeOwnerOverviewFacade.kt +5 more |
| `query.cashierSummary` | NATIVE | mechanical: ids [cashierSummary, dateRange] -> application/readmodels/HybridOperationalReadService.kt +2 more |
| `query.costingManagement` | NATIVE | mechanical: ids [costingManagement] -> application/readmodels/HybridCostingPlanningReadService.kt +1 more |
| `query.currentSession` | NATIVE | mechanical: ids [bootstrapOwner, changeOwnPassphrase, clearAll, currentSession, fill] -> application/admin/NativeAdminWorkFacade.kt +73 more |
| `query.dashboard` | NATIVE | mechanical: ids [dashboard, dateRange] -> application/owner/NativeOwnerOverviewFacade.kt +3 more |
| `query.deliveries` | NATIVE | mechanical: ids [deliveries, strictPayload] -> application/readmodels/HybridOperationalReadService.kt +1 more |
| `query.eventDetail` | NATIVE | mechanical: ids [eventDetail] -> application/owner/NativeOwnerRecordsFacade.kt +2 more |
| `query.eventHistory` | NATIVE | mechanical: ids [eventHistory] -> application/owner/NativeOwnerRecordsFacade.kt +2 more |
| `query.financialOverview` | NATIVE | mechanical: ids [dateRange, financialOverview] -> application/owner/NativeOwnerOverviewFacade.kt +2 more |
| `query.masterData` | NATIVE | mechanical: ids [masterData, strictPayload] -> application/owner/NativeOwnerShopSettingsFacade.kt +2 more |
| `query.planning` | NATIVE | mechanical: ids [planning] -> application/readmodels/HybridCostingPlanningReadService.kt +1 more |
| `query.profitabilityReport` | NATIVE | mechanical: ids [isAfter, profitabilityReport, requiredDate] -> application/owner/NativeOwnerOverviewFacade.kt +3 more |
| `query.reconciliationArchive` | NATIVE | mechanical: ids [archive, reconciliationArchive] -> application/owner/NativeOwnerOverviewFacade.kt +5 more |
| `query.securityAudit` | NATIVE | mechanical: ids [audit, securityAudit] -> application/owner/NativeOwnerBackupFacade.kt +2 more |
| `query.spoilageHistory` | NATIVE | mechanical: ids [spoilageHistory] -> application/readmodels/HybridOperationalReadService.kt +1 more |
| `query.transactionReferences` | NATIVE | mechanical: ids [strictPayload, transactionReferences] -> application/collection/NativeCollectionFacade.kt +7 more |
| `query.users` | NATIVE | mechanical: ids [users] -> application/readmodels/HybridReadModelService.kt +1 more |

### system.*

| Action | Verdict | Evidence |
|---|---|---|
| `system.bridgeInfo` | BY-DESIGN | bridge handshake itself; no native caller required; removed with the bridge at 14.8.6 |
| `system.cloudBackupStatus` | NATIVE | mechanical: ids [authorizeOwner, requireRecentAuthentication, status] -> application/authentication/NativeAuthenticationFacade.kt +5 more |
| `system.configureCloudBackup` | NATIVE | mechanical: ids [apply, authorizeOwner, configure, requireRecentAuthentication, status] -> application/authentication/NativeAuthenticationFacade.kt +6 more |
| `system.connectCloudBackup` | NATIVE | mechanical: ids [apply, authorizeOwner, configure, connect, equals] -> application/admin/NativeAdminWorkFacade.kt +54 more |
| `system.disconnectCloudBackup` | NATIVE | mechanical: ids [apply, authorizeOwner, cancelAll, configure, connect] -> application/admin/NativeAdminWorkFacade.kt +54 more |
| `system.exportEncryptedBackup` | NATIVE | mechanical: ids [also, createDocument, export, exportEncryptedSnapshot, fill] -> application/activity/NativeActivityFacade.kt +58 more |
| `system.inspectCloudBackup` | NATIVE | mechanical: ids [downloadAndDecrypt, downloadAndDecryptWithLocalKey, fill, inspect, inspectCompleteSnapshotBytes] -> application/authentication/NativeAuthenticationFacade.kt +21 more |
| `system.inspectEncryptedBackup` | NATIVE | mechanical: ids [fill, getOrElse, inspect, inspectEncryptedSnapshot, openDocument] -> application/authentication/NativeAuthenticationFacade.kt +31 more |
| `system.quickUnlockStatus` | NATIVE | mechanical: ids [status] -> application/authentication/NativeAuthenticationFacade.kt +3 more |
| `system.refreshCloudBackups` | NATIVE | mechanical: ids [authorizeOwner, refresh, refreshSnapshots, requireRecentAuthentication, status] -> application/authentication/NativeAuthenticationFacade.kt +29 more |
| `system.restoreInspectedBackup` | NATIVE | mechanical: ids [also, clear, createDocument, export, exportEncryptedSnapshot] -> application/activity/NativeActivityFacade.kt +86 more |
| `system.restoreInspectedCloudBackup` | NATIVE | mechanical: ids [cancelAll, clear, disableAfterRestore, downloadAndDecrypt, downloadAndDecryptWithLocalKey] -> application/authentication/NativeAuthenticationFacade.kt +60 more |
| `system.runCloudBackupNow` | NATIVE | mechanical: ids [authorizeOwner, read, requireRecentAuthentication, runNow] -> application/owner/NativeOwnerBackupFacade.kt +3 more |
