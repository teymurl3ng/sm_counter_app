# Complete active native field map

P1 covers everyday work, access, the reported costing issue and missing/shared keyboard ownership. P2 covers management, correction, backup and dialog exceptions. Priorities are implementation/review order, not evidence that every mapped field visibly failed.

| Priority | Source / field sites | Screen or form coverage | Treatment |
|---|---|---|---|
| P1 | [NativeAuthenticationScreens.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/auth/NativeAuthenticationScreens.kt) / 5 | Owner creation, passphrase sign-in and recovery; keypad remains separate | Access toolbar/body already separates stationary chrome and scrolling content; documented activity resize mode |
| P1 | [CashCloseEntryScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/cashclose/CashCloseEntryScreen.kt) / 5 | Cash count, variance reason, reopen reason/passphrase | Shared keyboard owner; existing form scroll |
| P1 | [CashierCollectionScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/collection/CashierCollectionScreen.kt) / 3 | Payment amount/notes and customer search dialog | Standard consumed padding; customer results already scroll |
| P1 | [NativePersonalSecurityPanel.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/common/NativePersonalSecurityPanel.kt) / 2 | Personal security passphrase/PIN shared by eligible roles | Shared role keyboard owner and existing surrounding scroll |
| P1 | [DrawingEntryScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/drawing/DrawingEntryScreen.kt) / 2 | Owner drawing amount and notes | Shared keyboard owner; existing form scroll |
| P1 | [ExpenseEntryScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/expense/ExpenseEntryScreen.kt) / 2 | Expense amount and description | Shared keyboard owner; existing form scroll |
| P1 | [CashierMoreScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/more/CashierMoreScreen.kt) / 2 | Cashier blind cash count and security host | Shared keyboard owner; existing form scroll |
| P1 | [DeliveriesScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/operations/DeliveriesScreen.kt) / 6 | New delivery fields and linked sale reference | Standard consumed padding; existing form scroll |
| P1 | [MakeYogurtScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/operations/MakeYogurtScreen.kt) / 2 | Milk input and product measurement fields | Shared keyboard owner; existing form scroll |
| P1 | [ReceiveMilkScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/operations/ReceiveMilkScreen.kt) / 2 | Receiving date and milk measurement fields | Shared keyboard owner; existing form scroll |
| P1 | [RecordSpoilageScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/operations/RecordSpoilageScreen.kt) / 2 | Spoilage quantity and reason | Shared keyboard owner; existing form scroll |
| P1 | [OwnerCostingPlanningScreens.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerCostingPlanningScreens.kt) / 7 | Budgets, collection targets, yield/loss assessment and approval | Stable contextual action toolbar; shared keyboard owner; existing assessment dialog scroll |
| P1 | [OwnerCostingPlansScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerCostingPlansScreen.kt) / 6 | Policies hub/approval and passphrase dialog | Stable contextual action toolbar; add scroll to reauthentication dialog |
| P1 | [OwnerOpeningCostBasisScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerOpeningCostBasisScreen.kt) / 3 | Opening product quantity/cost/evidence | Previously lacked local keyboard padding; shared owner now covers it |
| P1 | [OwnerShopSettingsScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerShopSettingsScreen.kt) / 9 | Shop identity, rates, category, opening balances and reauthentication | Shared keyboard owner; add scroll to opening reauthentication body |
| P1 | [OwnerSyncScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerSyncScreen.kt) / 2 | Sync enrollment username/passphrase | Previously lacked local keyboard padding; shared owner now covers it |
| P1 | [SalaryPaymentEntryScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/salarypayment/SalaryPaymentEntryScreen.kt) / 2 | Salary payment amount and notes | Shared keyboard owner; existing form scroll |
| P1 | [CashierSaleScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierSaleScreen.kt) / 6 | Sale search/quantity/amount, notes and delivery follow-up | Standard consumed padding; existing product result list and form scroll |
| P1 | [VendorPaymentEntryScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/vendorpayment/VendorPaymentEntryScreen.kt) / 2 | Supplier payment amount and notes | Shared keyboard owner; existing form scroll |
| P2 | [OwnerBackupScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerBackupScreen.kt) / 7 | Local/cloud backup, inspect/restore and Owner reauthentication | Shared keyboard owner; add scroll to reauthentication body |
| P2 | [OwnerCustomersScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerCustomersScreen.kt) / 8 | Customer creation and edit/contact fields | Shared keyboard owner; existing form scroll |
| P2 | [OwnerEmployeesScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerEmployeesScreen.kt) / 7 | Employee creation and edit/contact/salary fields | Shared keyboard owner; existing form scroll |
| P2 | [OwnerFundsRecordsForms.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerFundsRecordsForms.kt) / 3 | Drawing correction and funds receipt forms | Shared keyboard owner; existing surrounding form scroll |
| P2 | [OwnerProductsScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerProductsScreen.kt) / 3 | Product create/edit name and rate fields | Shared keyboard owner; existing form scroll |
| P2 | [OwnerRecordsScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerRecordsScreen.kt) / 2 | Records search and business correction fields | Shared keyboard owner; pinned search and existing correction scroll |
| P2 | [OwnerReportsScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerReportsScreen.kt) / 2 | Custom report date-range dialog | Add scroll to the dialog body; platform owns its separate window |
| P2 | [OwnerSuppliersScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerSuppliersScreen.kt) / 5 | Supplier creation and edit/contact fields | Shared keyboard owner; existing form scroll |
| P2 | [OwnerUsersRolesScreen.kt](../../../phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/OwnerUsersRolesScreen.kt) / 5 | Local staff creation/edit and Owner reauthentication | Shared keyboard owner; add scroll to reauthentication dialog |

112 call sites in 28 files, including shared helpers, read-only fields and dialog search. INVENTORY.json preserves each source line and hash before changes.

## Menu/header coverage

The inventory indexes 54 native presentation files and 73 shared-header call sites in 36 files. Owner, Cashier, Manager, Administrator and Operator each pin one header in their role host and suppress in-pane duplicates. The shared scaffold is the repair boundary for all five; a field does not need a new one-off keyboard workaround.

The costing-only intrinsically sized extraActions row is the confirmed action-height defect. Its four sections now use a constant, font-aware action toolbar below the stable tabs. Other primary hubs, Products/Customers/Suppliers/Employees/Users, Shop settings groups, Records filters, Reports, Security history, Backup, personal security, Work/Attendance/Delivery, sale/payment/expense/drawing/count forms and receipts remain mapped through their host/header/scroll call sites. Conditional errors, permissions, close/reopen panels and saved receipts retain their real meaning; they are not all reclassified as bugs.

Native system biometric/device-security prompts are platform-owned. Welcome/keypad screens are not ordinary keyboard fields. Dormant HybridActivity/MainActivity and web controls are outside the active launcher path.

Source coverage is complete for the stated active native call-site scan. Device-visible smoothness, full field clearance and the exact dark-green gap are not yet accepted; see CHECK_LATER.md.
