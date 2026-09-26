package com.saaf.milkshop.application.preview

import com.saaf.milkshop.BuildConfig
import com.saaf.milkshop.application.activity.NativeActivityFacade
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.application.sale.NativeSaleEntryMode
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.application.sale.NativeSalePaymentKind
import com.saaf.milkshop.application.sale.NativeSaleRequest
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Debug/preview shop filler for [com.saaf.milkshop.nativeui.NativePreviewActivity].
 *
 * Uses existing domain and native facades only — no SQL/business-rule changes.
 * Idempotent: sample customers / stock / Activity rows are topped up when thin;
 * preview role accounts are enrolled **once** when missing and are never
 * re-created or passphrase-reset on later launches.
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
        saleFacade: NativeSaleFacade,
        collectionFacade: NativeCollectionFacade,
        activityFacade: NativeActivityFacade,
        date: LocalDate = LocalDate.now(),
    ): Result = seedMutex.withLock {
        seedForSession(
            database = database,
            security = security,
            sessionHandle = sessionHandle,
            saleFacade = saleFacade,
            collectionFacade = collectionFacade,
            activityFacade = activityFacade,
            date = date,
        )
    }

    private suspend fun seedForSession(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
        saleFacade: NativeSaleFacade,
        collectionFacade: NativeCollectionFacade,
        activityFacade: NativeActivityFacade,
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
            ensurePreviewStaffAccounts(database, security, sessionHandle)
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
        val seededActivityRows = step("today activity", 0) {
            ensureTodayActivity(
                sessionHandle = sessionHandle,
                saleFacade = saleFacade,
                collectionFacade = collectionFacade,
                activityFacade = activityFacade,
                date = date,
            )
        }
        return Result(
            seededCustomers = seededCustomers,
            seededStock = seededStock || seededYogurt,
            seededActivityRows = seededActivityRows,
            seededOperator = seededStaff > 0,
            seededStaffAccounts = seededStaff,
            seededVendor = seededVendor,
            errors = errors,
        )
    }

    /**
     * Owner Overview preview filler: same shop sample plus yogurt stock so the
     * decision summary is readable on an empty install.
     */
    suspend fun ensureForOwnerOverview(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
        saleFacade: NativeSaleFacade,
        collectionFacade: NativeCollectionFacade,
        activityFacade: NativeActivityFacade,
        date: LocalDate = LocalDate.now(),
    ): Result = ensureForSession(
        database = database,
        security = security,
        sessionHandle = sessionHandle,
        saleFacade = saleFacade,
        collectionFacade = collectionFacade,
        activityFacade = activityFacade,
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
        // Deliveries are optional preview chrome; never block stock/sales seeding.
        runCatching { ensureSamplePendingDeliveries(database) }
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
     * Seeds one pending Active delivery when none exist, so Operators can try Mark delivered.
     */
    suspend fun ensureSamplePendingDeliveries(
        database: MilkShopDatabase,
        date: LocalDate = LocalDate.now(),
    ): Boolean {
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
                notes = "Preview sample delivery",
            ),
        )
        return true
    }

    /**
     * Enrolls Admin / Cashier / Operator preview accounts when missing.
     * Existing active accounts are left untouched (no passphrase reset).
     * @return count of accounts newly enrolled this call
     */
    suspend fun ensurePreviewStaffAccounts(
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
        val onHand = database.readModelDao().currentStock()
            .firstOrNull { it.productId == milk.productId }
            ?.quantityOnHand
            ?: 0.0
        if (onHand >= 40.0) return false
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
                notes = "Native preview sample stock",
            ),
        )
        return true
    }

    private suspend fun ensureYogurtStock(database: MilkShopDatabase, date: LocalDate): Boolean {
        val yogurt = database.productDao().findByName("Yogurt") ?: return false
        val onHand = database.readModelDao().currentStock()
            .firstOrNull { it.productId == yogurt.productId }
            ?.quantityOnHand
            ?: 0.0
        if (onHand >= 10.0) return false
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
                notes = "Native preview sample yogurt batch",
            ),
        )
        return true
    }

    private suspend fun ensureTodayActivity(
        sessionHandle: String,
        saleFacade: NativeSaleFacade,
        collectionFacade: NativeCollectionFacade,
        activityFacade: NativeActivityFacade,
        date: LocalDate,
    ): Int {
        val today = activityFacade.loadToday(sessionHandle, date)
        if (today.records.size >= 3) return 0

        val saleContext = saleFacade.load(sessionHandle, date)
        val milk = saleContext.products.firstOrNull { it.name.equals("Milk", ignoreCase = true) }
            ?: return 0
        val cashId = saleContext.defaultPaymentAccountId
            ?: saleContext.paymentAccounts.firstOrNull()?.accountId
            ?: return 0
        val customers = saleContext.customers.filter { it.name.startsWith("Sample ·") }
            .ifEmpty { saleContext.customers }
        if (customers.isEmpty()) return 0

        val creditCustomer = customers.first()
        var created = 0

        saleFacade.record(
            sessionHandle,
            NativeSaleRequest(
                productId = milk.productId,
                entryMode = NativeSaleEntryMode.QUANTITY,
                enteredValue = 2.0,
                paymentKind = NativeSalePaymentKind.PAID_NOW,
                paymentAccountId = cashId,
                customerAccountId = null,
            ),
            date,
        )
        created += 1

        saleFacade.record(
            sessionHandle,
            NativeSaleRequest(
                productId = milk.productId,
                entryMode = NativeSaleEntryMode.QUANTITY,
                enteredValue = 5.0,
                paymentKind = NativeSalePaymentKind.CUSTOMER_ACCOUNT,
                paymentAccountId = null,
                customerAccountId = creditCustomer.accountId,
            ),
            date,
        )
        created += 1

        val collectionContext = collectionFacade.load(sessionHandle, date)
        val paymentId = collectionContext.defaultPaymentAccountId ?: cashId
        collectionFacade.record(
            sessionHandle,
            NativeCollectionRequest(
                customerAccountId = creditCustomer.accountId,
                paymentAccountId = paymentId,
                amountPaisa = 50_000L,
                notes = "Sample Friday route collection",
            ),
            date,
        )
        created += 1
        return created
    }
}
