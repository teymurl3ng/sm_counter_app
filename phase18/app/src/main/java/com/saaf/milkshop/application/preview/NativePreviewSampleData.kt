package com.saaf.milkshop.application.preview

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
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import java.time.LocalDate

/**
 * Debug/preview shop filler for [com.saaf.milkshop.nativeui.NativePreviewActivity].
 *
 * Uses existing domain and native facades only — no SQL/business-rule changes.
 * Idempotent: sample customers are created once; today's Activity rows are topped
 * up for the signed-in actor when the list is thin; a preview Operator account is
 * enrolled once when the session can manage staff users.
 */
object NativePreviewSampleData {
    const val PREVIEW_OWNER_LOGIN = "preview.owner"
    const val PREVIEW_OWNER_DISPLAY_NAME = "Preview Owner"
    const val PREVIEW_OWNER_PASSPHRASE = "preview owner passphrase 2026"

    const val PREVIEW_OPERATOR_LOGIN = "preview.operator"
    const val PREVIEW_OPERATOR_DISPLAY_NAME = "Preview Operator"
    const val PREVIEW_OPERATOR_PASSPHRASE = "preview operator passphrase 2026"
    const val PREVIEW_VENDOR_NAME = "Sample · Dawn Dairy"

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
        val seededVendor: Boolean = false,
    ) {
        val changed: Boolean
            get() = seededCustomers || seededStock || seededActivityRows > 0 ||
                seededOperator || seededVendor
    }

    suspend fun ensureForSession(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
        saleFacade: NativeSaleFacade,
        collectionFacade: NativeCollectionFacade,
        activityFacade: NativeActivityFacade,
        date: LocalDate = LocalDate.now(),
    ): Result {
        val seededOperator = ensurePreviewOperator(database, security, sessionHandle)
        val seededCustomers = ensureSampleCustomers(database)
        val seededVendor = ensureSampleVendor(database)
        val seededStock = ensureMilkStock(database, date)
        val seededActivityRows = ensureTodayActivity(
            sessionHandle = sessionHandle,
            saleFacade = saleFacade,
            collectionFacade = collectionFacade,
            activityFacade = activityFacade,
            date = date,
        )
        return Result(
            seededCustomers = seededCustomers,
            seededStock = seededStock,
            seededActivityRows = seededActivityRows,
            seededOperator = seededOperator,
            seededVendor = seededVendor,
        )
    }

    suspend fun ensureSampleVendor(database: MilkShopDatabase): Boolean {
        val exists = database.accountDao().findByCategory("Vendor").any {
            it.name.removePrefix("Vendor: ").trim().equals(PREVIEW_VENDOR_NAME, ignoreCase = true)
        }
        if (exists) return false
        VendorManagementService(database).create(PREVIEW_VENDOR_NAME)
        return true
    }

    private suspend fun ensurePreviewOperator(
        database: MilkShopDatabase,
        security: UserSecurityService,
        sessionHandle: String,
    ): Boolean {
        val existing = database.appUserDao().findByLoginName(PREVIEW_OPERATOR_LOGIN)
        if (existing != null &&
            existing.status == com.saaf.milkshop.domain.UserSecurityService.STATUS_ACTIVE &&
            !existing.credentialSetupRequired
        ) {
            return false
        }
        try {
            security.requirePermission(sessionHandle, AppPermission.MANAGE_STAFF_USERS)
        } catch (_: Throwable) {
            return false
        }
        val pending = if (existing != null && existing.credentialSetupRequired) {
            security.resumeUserEnrollment(sessionHandle, existing.userId)
        } else {
            security.beginUserEnrollment(
                sessionHandle,
                CreateUserInput(
                    loginName = PREVIEW_OPERATOR_LOGIN,
                    displayName = PREVIEW_OPERATOR_DISPLAY_NAME,
                    role = AppRole.OPERATOR.wireValue,
                ),
            )
        }
        val passphrase = PREVIEW_OPERATOR_PASSPHRASE.toCharArray()
        try {
            security.completeUserEnrollment(pending.enrollmentToken, passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
        return true
    }

    private suspend fun ensureSampleCustomers(database: MilkShopDatabase): Boolean {
        val existing = database.accountDao().findByCategory("Customer").map { it.name }.toSet()
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
