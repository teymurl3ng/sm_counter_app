# Screen and state source map

62 screen-source files, including role wrappers and embedded dialogs/detail/edit/receipt states. CSV contains component names and routing details. This is source coverage, not completed device visual coverage. The shared NativeListItem helper is separately audited in PLAN.md. All 135 nativeui Kotlin files are frozen in source-baseline.json.

|Priority|Screen source|Variant|Existing helper calls|Decision|
|---|---|---|---:|---|
|P0|cashclose\CashCloseEntryScreen.kt|Section + Status|0|Expected/variance/receipt panels use Section variant. Audit long monetary values and reflow label/value pairs; preserve close semantics.|
|P0|more\CashierMoreScreen.kt|Navigation + Section|1|Reapply menu rows with uniform Navigation variant; personal security uses grouped sections. Cashier blind count is P0.|
|P1|activity\CashierActivityScreen.kt|Record + Section|1|Grouped record list or Record variant; amounts align in flexible trailing slot; detail facts grouped, not individually boxed.|
|P1|admin\AdminExpenseEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|admin\AdminMoreScreen.kt|Navigation + Section|1|Reapply menu rows with uniform Navigation variant; personal security uses grouped sections. Cashier blind count is P0.|
|P1|admin\AdminPeopleScreen.kt|Navigation|1|Hub destinations use Navigation variant; inherited detail/list screens use entity/record variants.|
|P1|admin\AdminRecordsScreen.kt|Navigation|1|Hub destinations use Navigation variant; inherited detail/list screens use entity/record variants.|
|P1|admin\AdminWorkScreen.kt|Navigation + Section|1|Secondary commands use Navigation variant; retain primary task buttons and attention hierarchy.|
|P1|auth\NativeAuthenticationScreens.kt|Entity + Selection + Section|1|Account picker gets Entity variant; methods use selection/action controls; identity/recovery grouped sections. Do not capsule every input.|
|P1|collection\CashierCollectionScreen.kt|Selection + Section + Status|2|Dialog rows use Selection variant; selected field remains selector; review and receipt use Section; status keeps semantic tone.|
|P1|expense\ExpenseEntryScreen.kt|Selection + Section + Status|2|Dialog rows use Selection variant; selected field remains selector; review and receipt use Section; status keeps semantic tone.|
|P1|manager\ManagerCashCloseEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerCollectionEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerDeliveriesEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerExpenseEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerMakeYogurtEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerMoreScreen.kt|Navigation + Section|1|Reapply menu rows with uniform Navigation variant; personal security uses grouped sections. Cashier blind count is P0.|
|P1|manager\ManagerPaySupplierEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerReceiveMilkEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerRecordSpoilageEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerSaleEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|manager\ManagerWorkScreen.kt|Navigation + Section|1|Secondary commands use Navigation variant; retain primary task buttons and attention hierarchy.|
|P1|more\CashierExpenseEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|operations\DeliveriesScreen.kt|Record + Section + Selection|0|Delivery records use expanded Record variant with stacked action area; new/link form Section; picker Selection.|
|P1|operations\OperationsMoreScreen.kt|Navigation + Section|1|Reapply menu rows with uniform Navigation variant; personal security uses grouped sections. Cashier blind count is P0.|
|P1|operations\OperationsWorkScreen.kt|Navigation + Section|0|Secondary commands use Navigation variant; retain primary task buttons and attention hierarchy.|
|P1|operations\ReceiveMilkScreen.kt|Selection + Section + Status|1|Dialog rows use Selection variant; selected field remains selector; review and receipt use Section; status keeps semantic tone.|
|P1|operations\RecordSpoilageScreen.kt|Selection + Section + Status|2|Dialog rows use Selection variant; selected field remains selector; review and receipt use Section; status keeps semantic tone.|
|P1|owner\OwnerCashCloseEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerCollectionEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerCustomersScreen.kt|Entity + Section|1|All list rows use Entity variant; details grouped sections; form fields remain native. Role choices use selection state.|
|P1|owner\OwnerDeliveriesEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerEmployeesScreen.kt|Entity + Section|1|All list rows use Entity variant; details grouped sections; form fields remain native. Role choices use selection state.|
|P1|owner\OwnerExpenseEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerMakeYogurtEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerMoreScreen.kt|Navigation + Section|1|Reapply menu rows with uniform Navigation variant; personal security uses grouped sections. Cashier blind count is P0.|
|P1|owner\OwnerPaySalaryEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerPaySupplierEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerProductsScreen.kt|Entity + Section|1|All list rows use Entity variant; details grouped sections; form fields remain native. Role choices use selection state.|
|P1|owner\OwnerReceiveMilkEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerRecordSpoilageEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerRecordsScreen.kt|Record + Section|2|Grouped record list or Record variant; amounts align in flexible trailing slot; detail facts grouped, not individually boxed.|
|P1|owner\OwnerSaleEntryScreen.kt|inherit|0|Apply shared form/selector/receipt design at underlying screen; do not double-wrap.|
|P1|owner\OwnerSuppliersScreen.kt|Entity + Section|1|All list rows use Entity variant; details grouped sections; form fields remain native. Role choices use selection state.|
|P1|owner\OwnerUsersRolesScreen.kt|Entity + Section|1|All list rows use Entity variant; details grouped sections; form fields remain native. Role choices use selection state.|
|P1|owner\OwnerWorkScreen.kt|Navigation + Section|1|Secondary commands use Navigation variant; retain primary task buttons and attention hierarchy.|
|P1|salarypayment\SalaryPaymentEntryScreen.kt|Selection + Section + Status|2|Dialog rows use Selection variant; selected field remains selector; review and receipt use Section; status keeps semantic tone.|
|P1|sale\CashierSaleScreen.kt|Selection + Section|1|Product choices use responsive product tile variant; customer/account dialogs Selection; calculation/receipt Section; do not capsule each calculator line.|
|P1|vendorpayment\VendorPaymentEntryScreen.kt|Selection + Section + Status|2|Dialog rows use Selection variant; selected field remains selector; review and receipt use Section; status keeps semantic tone.|
|P2|drawing\DrawingEntryScreen.kt|Section + Status|0|Attendance facts/form/review/receipt grouped sections; no list capsule needed unless repeated selectable items appear.|
|P2|operations\AttendanceScreen.kt|Section + Status|0|Attendance facts/form/review/receipt grouped sections; no list capsule needed unless repeated selectable items appear.|
|P2|operations\MakeYogurtScreen.kt|Section + Status|0|Attendance facts/form/review/receipt grouped sections; no list capsule needed unless repeated selectable items appear.|
|P2|owner\OwnerBackupScreen.kt|Section + Status|0|Backup/export/restore/connect/cloud facts grouped by task; retain action buttons and native form fields.|
|P2|owner\OwnerCostingPlanningScreens.kt|Record + Section|1|Policies/plans/opening cost rows use Record; editor grids/facts use Section; preserve numerical alignment.|
|P2|owner\OwnerCostingPlansScreen.kt|Record + Section|1|Policies/plans/opening cost rows use Record; editor grids/facts use Section; preserve numerical alignment.|
|P2|owner\OwnerOpeningCostBasisScreen.kt|Record + Section|0|Policies/plans/opening cost rows use Record; editor grids/facts use Section; preserve numerical alignment.|
|P2|owner\OwnerOverviewScreen.kt|Section + Record|0|Group totals/coverage and related balances; compact report rows with separators inside a section, not floating capsule per metric.|
|P2|owner\OwnerReportsScreen.kt|Section + Record|0|Group totals/coverage and related balances; compact report rows with separators inside a section, not floating capsule per metric.|
|P2|owner\OwnerSecurityHistoryScreen.kt|Record + Section|1|Grouped record list or Record variant; amounts align in flexible trailing slot; detail facts grouped, not individually boxed.|
|P2|owner\OwnerShopSettingsScreen.kt|Navigation + Selection + Section|1|Settings destinations/category list need consistent rows; rates/opening forms grouped sections; no capsules per field.|
|P2|owner\OwnerSyncScreen.kt|Record + Section + Status|1|Sync status/history rows inherit record rules; enrollment form Section; error/success semantic status.|
|P3|welcome\WelcomeScreen.kt|none|0|No capsule requirement identified; preserve hierarchy and assurance spacing.|