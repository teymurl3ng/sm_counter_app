package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface BackupDataDao {
    @Query("SELECT * FROM products ORDER BY product_id")
    suspend fun products(): List<ProductEntity>
    @Query("SELECT * FROM accounts ORDER BY account_id")
    suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM events ORDER BY event_id")
    suspend fun events(): List<EventEntity>
    @Query("SELECT * FROM inventory_log ORDER BY log_id")
    suspend fun inventoryLogs(): List<InventoryLogEntity>
    @Query("SELECT * FROM financial_log ORDER BY log_id")
    suspend fun financialLogs(): List<FinancialLogEntity>
    @Query("SELECT * FROM settings ORDER BY setting_key")
    suspend fun settings(): List<SettingEntity>
    @Query("SELECT * FROM budget_plans ORDER BY budget_id")
    suspend fun budgets(): List<BudgetPlanEntity>
    @Query("SELECT * FROM collections_targets ORDER BY target_id")
    suspend fun collectionsTargets(): List<CollectionsTargetEntity>
    @Query("SELECT * FROM product_profiles ORDER BY product_id")
    suspend fun productProfiles(): List<ProductProfileEntity>
    @Query("SELECT * FROM customer_profiles ORDER BY account_id")
    suspend fun customerProfiles(): List<CustomerProfileEntity>
    @Query("SELECT * FROM vendor_profiles ORDER BY account_id")
    suspend fun vendorProfiles(): List<VendorProfileEntity>
    @Query("SELECT * FROM employee_profiles ORDER BY account_id")
    suspend fun employeeProfiles(): List<EmployeeProfileEntity>
    @Query("SELECT * FROM attendance_records ORDER BY attendance_id")
    suspend fun attendanceRecords(): List<AttendanceRecordEntity>
    @Query("SELECT * FROM deliveries ORDER BY delivery_id")
    suspend fun deliveries(): List<DeliveryEntity>
    @Query("SELECT * FROM delivery_status_history ORDER BY status_id")
    suspend fun deliveryStatuses(): List<DeliveryStatusEntity>
    @Query("SELECT * FROM spoilage_details ORDER BY event_id")
    suspend fun spoilageDetails(): List<SpoilageDetailEntity>
    @Query("SELECT * FROM physical_cash_counts ORDER BY count_id")
    suspend fun physicalCashCounts(): List<PhysicalCashCountEntity>
    @Query("SELECT * FROM expense_category_profiles ORDER BY account_id")
    suspend fun expenseCategoryProfiles(): List<ExpenseCategoryProfileEntity>
    @Query("SELECT * FROM store_profile ORDER BY store_profile_id")
    suspend fun storeProfiles(): List<StoreProfileEntity>
    @Query("SELECT * FROM opening_batches ORDER BY event_id")
    suspend fun openingBatches(): List<OpeningBatchEntity>
    @Query("SELECT * FROM opening_inventory_entries ORDER BY event_id, product_id")
    suspend fun openingInventoryEntries(): List<OpeningInventoryEntryEntity>
    @Query("SELECT * FROM opening_account_entries ORDER BY event_id, account_id")
    suspend fun openingAccountEntries(): List<OpeningAccountEntryEntity>
    @Query("SELECT * FROM daily_close_actions ORDER BY action_id")
    suspend fun dailyCloseActions(): List<DailyCloseActionEntity>
    @Query("SELECT * FROM app_users ORDER BY user_id")
    suspend fun appUsers(): List<AppUserEntity>
    @Query("SELECT * FROM user_credentials ORDER BY user_id")
    suspend fun userCredentials(): List<UserCredentialEntity>
    @Query("SELECT * FROM saved_recovery_codes ORDER BY recovery_code_id")
    suspend fun savedRecoveryCodes(): List<SavedRecoveryCodeEntity>
    @Query("SELECT * FROM security_audit_entries ORDER BY audit_id")
    suspend fun securityAuditEntries(): List<SecurityAuditEntryEntity>
    @Query("SELECT * FROM event_actor_links ORDER BY event_id")
    suspend fun eventActorLinks(): List<EventActorLinkEntity>

    @Query("DELETE FROM daily_close_actions") suspend fun deleteDailyCloseActions()
    @Query("DELETE FROM opening_account_entries") suspend fun deleteOpeningAccountEntries()
    @Query("DELETE FROM opening_inventory_entries") suspend fun deleteOpeningInventoryEntries()
    @Query("DELETE FROM opening_batches") suspend fun deleteOpeningBatches()
    @Query("DELETE FROM store_profile") suspend fun deleteStoreProfiles()
    @Query("DELETE FROM delivery_status_history") suspend fun deleteDeliveryStatuses()
    @Query("DELETE FROM attendance_records") suspend fun deleteAttendanceRecords()
    @Query("DELETE FROM deliveries") suspend fun deleteDeliveries()
    @Query("DELETE FROM spoilage_details") suspend fun deleteSpoilageDetails()
    @Query("DELETE FROM physical_cash_counts") suspend fun deletePhysicalCashCounts()
    @Query("DELETE FROM customer_profiles") suspend fun deleteCustomerProfiles()
    @Query("DELETE FROM vendor_profiles") suspend fun deleteVendorProfiles()
    @Query("DELETE FROM employee_profiles") suspend fun deleteEmployeeProfiles()
    @Query("DELETE FROM product_profiles") suspend fun deleteProductProfiles()
    @Query("DELETE FROM expense_category_profiles") suspend fun deleteExpenseCategoryProfiles()
    @Query("DELETE FROM financial_log") suspend fun deleteFinancialLogs()
    @Query("DELETE FROM inventory_log") suspend fun deleteInventoryLogs()
    @Query("DELETE FROM events") suspend fun deleteEvents()
    @Query("DELETE FROM accounts") suspend fun deleteAccounts()
    @Query("DELETE FROM products") suspend fun deleteProducts()
    @Query("DELETE FROM settings") suspend fun deleteSettings()
    @Query("DELETE FROM budget_plans") suspend fun deleteBudgets()
    @Query("DELETE FROM collections_targets") suspend fun deleteCollectionsTargets()

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProducts(rows: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAccounts(rows: List<AccountEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEvents(rows: List<EventEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertInventoryLogs(rows: List<InventoryLogEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertFinancialLogs(rows: List<FinancialLogEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSettings(rows: List<SettingEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertBudgets(rows: List<BudgetPlanEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCollectionsTargets(rows: List<CollectionsTargetEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProductProfiles(rows: List<ProductProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCustomerProfiles(rows: List<CustomerProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertVendorProfiles(rows: List<VendorProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEmployeeProfiles(rows: List<EmployeeProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAttendanceRecords(rows: List<AttendanceRecordEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertDeliveries(rows: List<DeliveryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertDeliveryStatuses(rows: List<DeliveryStatusEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSpoilageDetails(rows: List<SpoilageDetailEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPhysicalCashCounts(rows: List<PhysicalCashCountEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertExpenseCategoryProfiles(rows: List<ExpenseCategoryProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertStoreProfiles(rows: List<StoreProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertOpeningBatches(rows: List<OpeningBatchEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertOpeningInventoryEntries(rows: List<OpeningInventoryEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertOpeningAccountEntries(rows: List<OpeningAccountEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertDailyCloseActions(rows: List<DailyCloseActionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPhysicalCashCount(row: PhysicalCashCountEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertDailyCloseAction(row: DailyCloseActionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAppUsers(rows: List<AppUserEntity>)
    @Update suspend fun updateAppUsers(rows: List<AppUserEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertUserCredentials(rows: List<UserCredentialEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSavedRecoveryCodes(rows: List<SavedRecoveryCodeEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSecurityAuditEntries(rows: List<SecurityAuditEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEventActorLinks(rows: List<EventActorLinkEntity>)

    @Query("INSERT OR IGNORE INTO product_profiles(product_id, inventory_tracking, archived, created_at) SELECT product_id, 1, 0, CURRENT_TIMESTAMP FROM products")
    suspend fun backfillProductProfiles()
    @Query("INSERT OR IGNORE INTO customer_profiles(account_id, active, created_at) SELECT account_id, 1, CURRENT_TIMESTAMP FROM accounts WHERE category = 'Customer'")
    suspend fun backfillCustomerProfiles()
    @Query("INSERT OR IGNORE INTO vendor_profiles(account_id, active, created_at) SELECT account_id, 1, CURRENT_TIMESTAMP FROM accounts WHERE category = 'Vendor'")
    suspend fun backfillVendorProfiles()
    @Query("INSERT OR IGNORE INTO employee_profiles(account_id, active, created_at) SELECT account_id, 1, CURRENT_TIMESTAMP FROM accounts WHERE category = 'Salary'")
    suspend fun backfillEmployeeProfiles()
    @Query("INSERT OR IGNORE INTO expense_category_profiles(account_id, display_name, active, created_at) SELECT account_id, substr(name, length('Expense: ') + 1), 1, CURRENT_TIMESTAMP FROM accounts WHERE category = 'Operating Expense'")
    suspend fun backfillExpenseCategoryProfiles()
}
