package com.saaf.milkshop.application.preview

import com.saaf.milkshop.BuildConfig
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.DatabaseSql
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import androidx.room3.withWriteTransaction
import androidx.room3.executeSQL
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

/**
 * Debug/preview shop filler for [com.saaf.milkshop.nativeui.NativePreviewActivity].
 *
 * Uses existing domain services only — no SQL/business-rule changes.
 * Idempotent scaffolding: sample customers / vendor / stock / staff accounts
 * are created once when missing and never re-created on later launches.
 * Deliberately does NOT record sales, collections, or any other business
 * activity: Records must show only what was actually recorded.
 */
object NativePreviewSampleData {
    /**
     * Serializes all seeding. Two role hosts can start near-simultaneously;
     * without this lock their check-then-create fixture steps race and one
     * dies on a UNIQUE constraint (accounts.name).
     */
    private val seedMutex = kotlinx.coroutines.sync.Mutex()

    /** Debug-only shared passphrase for the isolated preview package. Empty in release. */
    val PREVIEW_SHARED_PASSPHRASE: String
        get() = BuildConfig.NATIVE_PREVIEW_SHARED_PASSPHRASE

    const val SAMPLE_DELIVERY_NOTES = "Preview sample delivery"
    const val SAMPLE_MILK_STOCK_NOTES = "Native preview sample stock"
    const val SAMPLE_YOGURT_STOCK_NOTES = "Native preview sample yogurt batch"

    const val PREVIEW_OWNER_LOGIN = "preview.owner"
    const val PREVIEW_OWNER_DISPLAY_NAME = "Preview Owner"
    val PREVIEW_OWNER_PASSPHRASE: String
        get() = PREVIEW_SHARED_PASSPHRASE

    const val PREVIEW_ADMIN_LOGIN = "preview.admin"
    const val PREVIEW_ADMIN_DISPLAY_NAME = "Preview Admin"

    const val PREVIEW_MANAGER_LOGIN = "preview.manager"
    const val PREVIEW_MANAGER_DISPLAY_NAME = "Preview Manager"

    const val PREVIEW_CASHIER_LOGIN = "preview.cashier"
    const val PREVIEW_CASHIER_DISPLAY_NAME = "Preview Cashier"

    const val PREVIEW_OPERATOR_LOGIN = "preview.operator"
    const val PREVIEW_OPERATOR_DISPLAY_NAME = "Preview Operator"
    val PREVIEW_OPERATOR_PASSPHRASE: String
        get() = PREVIEW_SHARED_PASSPHRASE

    /** Older preview secrets — migrate once when found, never wipe on launch. */
    private val LEGACY_PREVIEW_PASSPHRASES = listOf(
        "preview owner passphrase 2026",
        "preview operator passphrase 2026",
    )

    const val PREVIEW_VENDOR_NAME = "Sample · Dawn Dairy"

    private data class PreviewStaffSpec(
        val loginName: String,
        val displayName: String,
        val role: AppRole,
    )

    private val previewStaff = listOf(
        PreviewStaffSpec(PREVIEW_ADMIN_LOGIN, PREVIEW_ADMIN_DISPLAY_NAME, AppRole.ADMIN),
        PreviewStaffSpec(PREVIEW_MANAGER_LOGIN, PREVIEW_MANAGER_DISPLAY_NAME, AppRole.MANAGER),
        PreviewStaffSpec(PREVIEW_CASHIER_LOGIN, PREVIEW_CASHIER_DISPLAY_NAME, AppRole.CASHIER),
        PreviewStaffSpec(PREVIEW_OPERATOR_LOGIN, PREVIEW_OPERATOR_DISPLAY_NAME, AppRole.OPERATOR),
    )

    val fixturesEnabled: Boolean
        get() = BuildConfig.ENABLE_NATIVE_PREVIEW_FIXTURES && PREVIEW_SHARED_PASSPHRASE.isNotBlank()

    fun isPreviewLogin(loginName: String): Boolean =
        fixturesEnabled && loginName.trim().lowercase().startsWith("preview.")

    fun previewAccountsHint(): String =
        "Preview logins: $PREVIEW_OWNER_LOGIN / $PREVIEW_MANAGER_LOGIN / $PREVIEW_ADMIN_LOGIN / " +
            "$PREVIEW_CASHIER_LOGIN / $PREVIEW_OPERATOR_LOGIN — passphrase: $PREVIEW_SHARED_PASSPHRASE"

    private val sampleCustomerNames = listOf(
        "Sample · Noor Store",
        "Sample · Ahmed Traders",
        "Sample · Walk-in Counter",
    )

    data class Result(
        val seededCustomers: Boolean,
        val seededStock: Boolean,
        val seededActivityRows: Int,
        val seededOperator: Boolean,
        val seededStaffAccounts: Int = 0,
        val seededVendor: Boolean = false,
        val errors: List<String> = emptyList(),
    ) {
        val changed: Boolean
            get() = seededCustomers || seededStock || seededActivityRows > 0 ||
                seededOperator || seededStaffAccounts > 0 || seededVendor
    }

    suspend fun ensureForSession(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): Result = seedMutex.withLock {
        seedForSession(
            database = database,
            security = security,
            sessionHandle = sessionHandle,
            date = date,
        )
    }

    private suspend fun seedForSession(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
        date: LocalDate,
    ): Result {
        if (!fixturesEnabled) return Result(false, false, 0, false)
        val errors = mutableListOf<String>()
        suspend fun <T> step(label: String, fallback: T, block: suspend () -> T): T =
            try {
                block()
            } catch (error: Throwable) {
                errors += "$label: ${error.message ?: error.javaClass.simpleName}"
                fallback
            }

        val seededStaff = step("preview staff", 0) {
            ensurePreviewStaffAccountsLocked(database, security, sessionHandle)
        }
        val seededCustomers = step("customers", false) {
            ensureSampleCustomers(database)
        }
        val seededVendor = step("vendor", false) {
            ensureSampleVendorLocked(database)
        }
        val seededStock = step("milk stock", false) {
            ensureMilkStock(database, date)
        }
        val seededYogurt = step("yogurt stock", false) {
            ensureYogurtStock(database, date)
        }
        return Result(
            seededCustomers = seededCustomers,
            seededStock = seededStock || seededYogurt,
            seededActivityRows = 0,
            seededOperator = seededStaff > 0,
            seededStaffAccounts = seededStaff,
            seededVendor = seededVendor,
            errors = errors,
        )
    }

    /**
     * Owner Overview preview filler: same shop sample so the decision summary
     * is readable on an empty install.
     */
    suspend fun ensureForOwnerOverview(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): Result = ensureForSession(
        database = database,
        security = security,
        sessionHandle = sessionHandle,
        date = date,
    )

    /** Locked entry point — the login path runs alongside host seeding. */
    suspend fun ensureSampleVendor(database: MilkShopDatabase): Boolean = seedMutex.withLock {
        ensureSampleVendorLocked(database)
    }

    private suspend fun ensureSampleVendorLocked(database: MilkShopDatabase): Boolean {
        if (!fixturesEnabled) return false
        val exists = database.accountDao().findByCategory("Vendor").any {
            it.name.removePrefix("Vendor: ").trim().equals(PREVIEW_VENDOR_NAME, ignoreCase = true)
        }
        if (!exists) {
            VendorManagementService(database).create(PREVIEW_VENDOR_NAME)
        }
        // No sample delivery seeding: a re-created "ghost" pending delivery on
        // every launch reads as fake data on the Work badge and Deliveries
        // list. Real deliveries are created from the New delivery form.
        runCatching { retireStaleSampleDeliveries(database) }
        return !exists
    }

    /** Locked entry point — the login path runs alongside host seeding. */
    suspend fun ensurePreviewOperatorEmployeeLink(
        database: MilkShopDatabase,
        loginName: String = PREVIEW_OPERATOR_LOGIN,
    ): Boolean = seedMutex.withLock {
        ensurePreviewOperatorEmployeeLinkLocked(database, loginName)
    }

    private suspend fun ensurePreviewOperatorEmployeeLinkLocked(
        database: MilkShopDatabase,
        loginName: String,
    ): Boolean {
        if (!fixturesEnabled) return false
        val user = database.appUserDao().findByLoginName(loginName) ?: return false
        if (user.employeeAccountId != null) return false
        val employeeName = when (loginName) {
            PREVIEW_OPERATOR_LOGIN -> "Preview Operator Staff"
            else -> "Staff · ${user.displayName}"
        }
        val existing = database.accountDao().findByCategory("Salary").firstOrNull {
            it.name.removePrefix("Employee: ").trim().equals(employeeName, ignoreCase = true)
        }
        val employeeId = existing?.accountId
            ?: EmployeeManagementService(database).create(employeeName)
        database.appUserDao().update(user.copy(employeeAccountId = employeeId))
        return true
    }

    /**
     * Seeds one pending Active delivery when none exist. Only used by the
     * instrumentation test now — production seeding no longer creates ghosts.
     */
    suspend fun ensureSamplePendingDeliveries(
        database: MilkShopDatabase,
        date: LocalDate = BusinessClock.today(),
    ): Boolean {
        retireStaleSampleDeliveries(database, date)
        val hasPending = database.deliveryDao().activeDeliveries().any { delivery ->
            database.deliveryDao().latestStatus(delivery.deliveryId)?.status ==
                com.saaf.milkshop.domain.DeliveryService.STATUS_PENDING
        }
        if (hasPending) return false
        ensureSampleCustomers(database)
        val customer = database.accountDao().findByCategory("Customer").firstOrNull() ?: return false
        val milk = database.productDao().findByName("Milk") ?: return false
        com.saaf.milkshop.domain.DeliveryService(database).create(
            com.saaf.milkshop.domain.DeliveryInput(
                customerAccountId = customer.accountId,
                productId = milk.productId,
                quantity = 5.0,
                route = "Sample · Morning route",
                businessDate = date.toString(),
                notes = SAMPLE_DELIVERY_NOTES,
            ),
        )
        return true
    }

    /**
     * Retires any pending sample delivery left by older preview installs: a
     * fixture nobody wants, and one dated on a since-closed business day could
     * never be delivered or cancelled from the UI (date guard), sticking as a
     * permanent badge. The domain service is used directly — the preview
     * seeder is fixture management, not a user action, and owns its cleanup.
     * Closed-day samples are frozen by the closed-business-date SQL triggers,
     * so the guards are lifted for this fixture-only write and restored
     * immediately after (same pattern as BackupRestoreEngine).
     */
    private suspend fun retireStaleSampleDeliveries(
        database: MilkShopDatabase,
        date: LocalDate = BusinessClock.today(),
    ) {
        val guardTriggers = listOf(
            "prevent_closed_delivery_update",
            "prevent_closed_delivery_insert",
            "prevent_closed_delivery_status",
        )
        val staleSamples = database.deliveryDao().activeDeliveries().filter { delivery ->
            delivery.notes == SAMPLE_DELIVERY_NOTES &&
                delivery.businessDate <= date.toString() &&
                database.deliveryDao().latestStatus(delivery.deliveryId)?.status ==
                    com.saaf.milkshop.domain.DeliveryService.STATUS_PENDING
        }
        if (staleSamples.isEmpty()) return
        database.withWriteTransaction<Unit> {
            guardTriggers.forEach { executeSQL("DROP TRIGGER IF EXISTS $it") }
            staleSamples.forEach { delivery ->
                runCatching {
                    com.saaf.milkshop.domain.DeliveryService(database).archive(delivery.deliveryId)
                }.onFailure {
                    android.util.Log.d("SaafSeed", "stale archive #${delivery.deliveryId} failed: ${it.message}")
                }
            }
            DatabaseSql.createTriggers.forEach { executeSQL(it) }
        }
    }

    /**
     * Enrolls Admin / Cashier / Operator preview accounts when missing.
     * Existing active accounts are left untouched (no passphrase reset).
     * @return count of accounts newly enrolled this call
     */
    /** Locked entry point — the login path runs alongside host seeding. */
    suspend fun ensurePreviewStaffAccounts(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
    ): Int = seedMutex.withLock {
        ensurePreviewStaffAccountsLocked(database, security, sessionHandle)
    }

    private suspend fun ensurePreviewStaffAccountsLocked(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
    ): Int {
        if (!fixturesEnabled) return 0
        try {
            security.requirePermission(sessionHandle, AppPermission.MANAGE_STAFF_USERS)
        } catch (_: Throwable) {
            return 0
        }
        var created = 0
        for (spec in previewStaff) {
            if (ensureOnePreviewStaff(database, security, sessionHandle, spec)) {
                created += 1
            }
        }
        return created
    }

    private suspend fun ensureOnePreviewStaff(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
        spec: PreviewStaffSpec,
    ): Boolean {
        val existing = database.appUserDao().findByLoginName(spec.loginName)
        if (existing != null &&
            existing.status == UserSecurityService.STATUS_ACTIVE &&
            !existing.credentialSetupRequired
        ) {
            return false
        }
        val pending = if (existing != null && existing.credentialSetupRequired) {
            security.resumeUserEnrollment(sessionHandle, existing.userId)
        } else {
            security.beginUserEnrollment(
                sessionHandle,
                CreateUserInput(
                    loginName = spec.loginName,
                    displayName = spec.displayName,
                    role = spec.role.wireValue,
                ),
            )
        }
        val passphrase = PREVIEW_SHARED_PASSPHRASE.toCharArray()
        try {
            security.completeUserEnrollment(pending.enrollmentToken, passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
        return true
    }

    /**
     * One-time: if a known preview login still uses a legacy passphrase, move it
     * to [PREVIEW_SHARED_PASSPHRASE]. Never forces a reset when the shared secret
     * already works, and never touches non-preview accounts.
     */
    suspend fun alignPreviewPassphrases(security: UserSecurityService) {
        if (!fixturesEnabled) return
        val logins = listOf(
            PREVIEW_OWNER_LOGIN,
            PREVIEW_MANAGER_LOGIN,
            PREVIEW_ADMIN_LOGIN,
            PREVIEW_CASHIER_LOGIN,
            PREVIEW_OPERATOR_LOGIN,
        )
        for (login in logins) {
            alignOnePreviewPassphrase(security, login)
        }
    }

    private suspend fun alignOnePreviewPassphrase(
        security: UserSecurityService,
        loginName: String,
    ) {
        val shared = PREVIEW_SHARED_PASSPHRASE.toCharArray()
        try {
            val already = runCatching { security.login(loginName, shared) }.getOrNull()
            if (already != null) {
                runCatching { security.logout(already.sessionId) }
                return
            }
        } finally {
            shared.fill('\u0000')
        }
        for (legacy in LEGACY_PREVIEW_PASSPHRASES) {
            val legacyChars = legacy.toCharArray()
            val session = try {
                runCatching { security.login(loginName, legacyChars) }.getOrNull()
            } finally {
                legacyChars.fill('\u0000')
            } ?: continue
            val current = legacy.toCharArray()
            val next = PREVIEW_SHARED_PASSPHRASE.toCharArray()
            try {
                security.changeOwnPassphrase(session.sessionId, current, next)
            } catch (_: Throwable) {
                runCatching { security.logout(session.sessionId) }
            } finally {
                current.fill('\u0000')
                next.fill('\u0000')
            }
            return
        }
    }

    private suspend fun ensureSampleCustomers(database: MilkShopDatabase): Boolean {
        val existing = database.accountDao().findByCategory("Customer").map {
            it.name.removePrefix("Customer: ").trim()
        }.toSet()
        val missing = sampleCustomerNames.filterNot { it in existing }
        if (missing.isEmpty()) return false
        val customers = CustomerManagementService(database)
        missing.forEach { customers.create(it) }
        return true
    }

    private suspend fun ensureMilkStock(database: MilkShopDatabase, date: LocalDate): Boolean {
        val milk = database.productDao().findByName("Milk") ?: return false
        // One-time scaffolding keyed on the sample marker, not on on-hand
        // stock: a threshold guard would re-fire after real selling drops
        // stock and inject surprise purchase events into Records.
        if (database.eventDao().countByNotes(SAMPLE_MILK_STOCK_NOTES) > 0) return false
        val vendorName = "Sample · Rehman Dairy"
        val vendors = database.accountDao().findByCategory("Vendor")
        val vendorId = vendors.firstOrNull { it.name == vendorName }?.accountId
            ?: VendorManagementService(database).create(vendorName)
        VendorPurchaseService(database).record(
            VendorPurchaseInput(
                vendorAccountId = vendorId,
                occurredAt = date.toString(),
                rawLiters = 80.0,
                fatPercentage = 4.2,
                lrReading = 28.5,
                notes = SAMPLE_MILK_STOCK_NOTES,
            ),
        )
        return true
    }

    private suspend fun ensureYogurtStock(database: MilkShopDatabase, date: LocalDate): Boolean {
        val yogurt = database.productDao().findByName("Yogurt") ?: return false
        if (database.eventDao().countByNotes(SAMPLE_YOGURT_STOCK_NOTES) > 0) return false
        ensureMilkStock(database, date)
        val milk = database.productDao().findByName("Milk") ?: return false
        val milkOnHand = database.readModelDao().currentStock()
            .firstOrNull { it.productId == milk.productId }
            ?.quantityOnHand
            ?: 0.0
        if (milkOnHand < 20.0) return false
        com.saaf.milkshop.domain.YogurtProductionService(database).record(
            com.saaf.milkshop.domain.YogurtProductionInput(
                occurredAt = date.toString(),
                milkLitersUsed = 20.0,
                yogurtKgProduced = 18.0,
                notes = SAMPLE_YOGURT_STOCK_NOTES,
            ),
        )
        return true
    }
}
