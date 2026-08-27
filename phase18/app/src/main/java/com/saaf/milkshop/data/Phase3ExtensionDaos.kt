package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ProductProfileDao {
    @Query("SELECT * FROM product_profiles WHERE product_id = :productId LIMIT 1")
    suspend fun findByProductId(productId: Long): ProductProfileEntity?

    @Query("SELECT * FROM product_profiles ORDER BY product_id")
    suspend fun all(): List<ProductProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: ProductProfileEntity)
}

@Dao
interface CustomerProfileDao {
    @Query("SELECT * FROM customer_profiles WHERE account_id = :accountId LIMIT 1")
    suspend fun findByAccountId(accountId: Long): CustomerProfileEntity?

    @Query("SELECT * FROM customer_profiles ORDER BY account_id")
    suspend fun all(): List<CustomerProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: CustomerProfileEntity)
}

@Dao
interface VendorProfileDao {
    @Query("SELECT * FROM vendor_profiles WHERE account_id = :accountId LIMIT 1")
    suspend fun findByAccountId(accountId: Long): VendorProfileEntity?

    @Query("SELECT * FROM vendor_profiles ORDER BY account_id")
    suspend fun all(): List<VendorProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: VendorProfileEntity)
}

@Dao
interface EmployeeProfileDao {
    @Query("SELECT * FROM employee_profiles WHERE account_id = :accountId LIMIT 1")
    suspend fun findByAccountId(accountId: Long): EmployeeProfileEntity?

    @Query("SELECT * FROM employee_profiles ORDER BY account_id")
    suspend fun all(): List<EmployeeProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: EmployeeProfileEntity)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE employee_account_id = :employeeAccountId AND business_date = :businessDate AND archived = 0 ORDER BY attendance_id DESC LIMIT 1")
    suspend fun current(employeeAccountId: Long, businessDate: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE employee_account_id = :employeeAccountId ORDER BY business_date DESC, attendance_id DESC")
    suspend fun history(employeeAccountId: Long): List<AttendanceRecordEntity>

    @Query("UPDATE attendance_records SET archived = 1 WHERE attendance_id = :attendanceId AND archived = 0")
    suspend fun archive(attendanceId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: AttendanceRecordEntity): Long
}

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries WHERE delivery_id = :deliveryId LIMIT 1")
    suspend fun findById(deliveryId: Long): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE archived = 0 ORDER BY business_date DESC, delivery_id DESC")
    suspend fun activeDeliveries(): List<DeliveryEntity>

    @Query("UPDATE deliveries SET sale_event_id = :saleEventId WHERE delivery_id = :deliveryId AND sale_event_id IS NULL")
    suspend fun linkSale(deliveryId: Long, saleEventId: Long): Int

    @Query("UPDATE deliveries SET archived = 1 WHERE delivery_id = :deliveryId AND archived = 0")
    suspend fun archive(deliveryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(delivery: DeliveryEntity): Long

    @Query("SELECT * FROM delivery_status_history WHERE delivery_id = :deliveryId ORDER BY status_id")
    suspend fun statusHistory(deliveryId: Long): List<DeliveryStatusEntity>

    @Query("SELECT * FROM delivery_status_history WHERE delivery_id = :deliveryId ORDER BY status_id DESC LIMIT 1")
    suspend fun latestStatus(deliveryId: Long): DeliveryStatusEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStatus(status: DeliveryStatusEntity): Long
}

@Dao
interface SpoilageDao {
    @Query("SELECT * FROM spoilage_details WHERE event_id = :eventId LIMIT 1")
    suspend fun findByEventId(eventId: Long): SpoilageDetailEntity?

    @Query("SELECT * FROM spoilage_details ORDER BY event_id")
    suspend fun all(): List<SpoilageDetailEntity>

    @Query(
        """
        SELECT
            e.event_id AS eventId,
            e.occurred_at AS occurredAt,
            e.created_at AS createdAt,
            sd.product_id AS productId,
            p.name AS productName,
            p.unit AS unit,
            sd.quantity AS quantity,
            sd.reason AS reason,
            e.corrects_event_id AS correctsEventId,
            CASE WHEN EXISTS (
                SELECT 1 FROM events child WHERE child.corrects_event_id = e.event_id
            ) THEN 1 ELSE 0 END AS superseded,
            e.notes AS notes
        FROM spoilage_details sd
        INNER JOIN events e ON e.event_id = sd.event_id
        INNER JOIN products p ON p.product_id = sd.product_id
        ORDER BY e.occurred_at DESC, e.event_id DESC
        """
    )
    suspend fun history(): List<SpoilageHistoryRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(detail: SpoilageDetailEntity)
}

@Dao
interface PhysicalCashCountDao {
    @Query("SELECT * FROM physical_cash_counts WHERE count_id = :countId LIMIT 1")
    suspend fun findById(countId: Long): PhysicalCashCountEntity?

    @Query("SELECT * FROM physical_cash_counts WHERE account_id = :accountId AND business_date = :businessDate ORDER BY counted_at DESC, count_id DESC LIMIT 1")
    suspend fun latest(accountId: Long, businessDate: String): PhysicalCashCountEntity?

    @Query("SELECT * FROM physical_cash_counts ORDER BY business_date DESC, counted_at DESC, count_id DESC")
    suspend fun all(): List<PhysicalCashCountEntity>

    @Query(
        """
        SELECT ROUND(COALESCE(SUM(fl.amount), 0), 2)
        FROM financial_log fl
        INNER JOIN events e ON e.event_id = fl.event_id
        WHERE fl.account_id = :accountId AND e.created_at <= :createdAtInclusive
        """
    )
    suspend fun canonicalBalanceAt(accountId: Long, createdAtInclusive: String): Double

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(count: PhysicalCashCountEntity): Long
}

@Dao
interface ExpenseCategoryProfileDao {
    @Query("SELECT * FROM expense_category_profiles WHERE display_name = :displayName LIMIT 1")
    suspend fun findByDisplayName(displayName: String): ExpenseCategoryProfileEntity?

    @Query("SELECT * FROM expense_category_profiles WHERE account_id = :accountId LIMIT 1")
    suspend fun findByAccountId(accountId: Long): ExpenseCategoryProfileEntity?

    @Query("SELECT * FROM expense_category_profiles ORDER BY display_name")
    suspend fun all(): List<ExpenseCategoryProfileEntity>

    @Query("UPDATE expense_category_profiles SET active = :active WHERE account_id = :accountId")
    suspend fun setActive(accountId: Long, active: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ExpenseCategoryProfileEntity)
}
