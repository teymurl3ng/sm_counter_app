package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.CreditCollectionResult
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseResult
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleResult
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SalaryPaymentInput
import com.saaf.milkshop.domain.SalaryPaymentResult
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.SpoilageInput
import com.saaf.milkshop.domain.SpoilageResult
import com.saaf.milkshop.domain.SpoilageService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseResult
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.VendorPaymentResult
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionResult
import com.saaf.milkshop.domain.YogurtProductionService
import com.saaf.milkshop.sync.BusinessDateGuard
import com.saaf.milkshop.sync.SyncScheduler
import java.time.Instant

internal class OperationsBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun recordPurchase(sessionHandle: String, input: VendorPurchaseInput): VendorPurchaseResult =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.PURCHASE)
            val vendor = database.accountDao().findById(input.vendorAccountId)
            val profile = database.vendorProfileDao().findByAccountId(input.vendorAccountId)
            if (vendor?.category != "Vendor" || profile?.active != true) {
                throw BridgeCommandNotFound("Choose an active supplier")
            }
            if (database.dailyCloseDao().isPostingClosed(input.occurredAt)) {
                throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording the purchase.")
            }
            BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt)
            val result = VendorPurchaseService(database).record(input)
            database.eventActorLinkDao().insert(
                EventActorLinkEntity(
                    eventId = result.eventId,
                    actorUserId = actor.userId,
                    actorNameSnapshot = actor.displayName,
                    actorRoleSnapshot = actor.role,
                    attributedAt = Instant.now().toString(),
                )
            )
            database.securityAuditDao().insert(
                SecurityAuditEntryEntity(
                    occurredAt = Instant.now().toString(),
                    action = ACTION_RECORD_PURCHASE,
                    result = RESULT_SUCCESS,
                    actorUserId = actor.userId,
                    actorNameSnapshot = actor.displayName,
                    actorRoleSnapshot = actor.role,
                    details = "event_id=${result.eventId};vendor_account_id=${input.vendorAccountId}",
                )
            )
            result
        }.also { SyncScheduler.requestPush() }

    suspend fun recordSale(sessionHandle: String, input: SaleInput): SaleResult =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.SALE_AND_COLLECTION)
            val product = database.productDao().findById(input.productId)
            val productProfile = database.productProfileDao().findByProductId(input.productId)
            if (product == null || productProfile?.archived == true) {
                throw BridgeCommandNotFound("Choose an active product")
            }
            val destinationId = when (val payment = input.payment) {
                is SalePayment.Account -> {
                    val account = database.accountDao().findById(payment.accountId)
                    if (account?.category != "Cash/Bank") throw BridgeCommandNotFound("Choose a valid payment account")
                    payment.accountId
                }
                is SalePayment.OnCredit -> {
                    val account = database.accountDao().findById(payment.customerAccountId)
                    val profile = database.customerProfileDao().findByAccountId(payment.customerAccountId)
                    if (account?.category != "Customer" || profile?.active == false) {
                        throw BridgeCommandNotFound("Choose an active customer")
                    }
                    payment.customerAccountId
                }
            }
            if (database.dailyCloseDao().isPostingClosed(input.occurredAt)) {
                throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording the sale.")
            }
            BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt)
            val result = try {
                SaleService(database).record(input)
            } catch (error: Throwable) {
                if (error.hasMessage("Insufficient stock for this product")) {
                    throw BridgeCommandInsufficientStock("There is not enough ${product.name} in stock for this sale.")
                }
                throw error
            }
            attribute(result.eventId, actor.userId, actor.displayName, actor.role)
            audit(
                ACTION_RECORD_SALE,
                "event_id=${result.eventId};product_id=${input.productId};destination_account_id=$destinationId",
                actor.userId,
                actor.displayName,
                actor.role,
            )
            result
        }.also { SyncScheduler.requestPush() }

    suspend fun recordProduction(
        sessionHandle: String,
        input: YogurtProductionInput,
    ): YogurtProductionResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.PRODUCTION_AND_SPOILAGE)
        if (database.dailyCloseDao().isPostingClosed(input.occurredAt)) {
            throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording production.")
        }
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt)
        val result = try {
            YogurtProductionService(database).record(input)
        } catch (error: Throwable) {
            if (error.hasMessage("Insufficient stock for this product")) {
                throw BridgeCommandInsufficientStock("There is not enough Milk in stock for this production batch.")
            }
            throw error
        }
        attribute(result.eventId, actor.userId, actor.displayName, actor.role)
        audit(
            ACTION_RECORD_PRODUCTION,
            "event_id=${result.eventId}",
            actor.userId,
            actor.displayName,
            actor.role,
        )
        result
    }.also { SyncScheduler.requestPush() }

    suspend fun recordExpense(
        sessionHandle: String,
        expenseCategoryAccountId: Long,
        paymentAccountId: Long,
        amount: Double,
        occurredAt: String,
        notes: String?,
    ): ExpenseResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.EXPENSE)
        val category = database.expenseCategoryProfileDao().findByAccountId(expenseCategoryAccountId)
        val categoryAccount = database.accountDao().findById(expenseCategoryAccountId)
        if (category?.active != true || categoryAccount?.category != "Operating Expense") {
            throw BridgeCommandNotFound("Choose an active expense category")
        }
        val paymentAccount = database.accountDao().findById(paymentAccountId)
        if (paymentAccount?.category != "Cash/Bank") {
            throw BridgeCommandNotFound("Choose a valid cash or bank account")
        }
        if (database.dailyCloseDao().isPostingClosed(occurredAt)) {
            throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording the expense.")
        }
        BusinessDateGuard.requireHappenedWithinHorizon(occurredAt)
        val result = ExpenseService(database).record(
            ExpenseInput(category.displayName, paymentAccountId, amount, occurredAt, notes)
        )
        attribute(result.eventId, actor.userId, actor.displayName, actor.role)
        audit(
            ACTION_RECORD_EXPENSE,
            "event_id=${result.eventId};expense_category_account_id=$expenseCategoryAccountId;payment_account_id=$paymentAccountId",
            actor.userId,
            actor.displayName,
            actor.role,
        )
        result
    }.also { SyncScheduler.requestPush() }

    suspend fun recordVendorPayment(
        sessionHandle: String,
        input: VendorPaymentInput,
    ): VendorPaymentResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.SUPPLIER_PAYMENT)
        val vendor = database.accountDao().findById(input.vendorAccountId)
        val vendorProfile = database.vendorProfileDao().findByAccountId(input.vendorAccountId)
        if (vendor?.category != "Vendor" || vendorProfile?.active != true) {
            throw BridgeCommandNotFound("Choose an active supplier")
        }
        requirePaymentAccount(input.paymentAccountId)
        requireOpenDate(input.occurredAt, "supplier payment")
        val result = VendorPaymentService(database).record(input)
        attribute(result.eventId, actor.userId, actor.displayName, actor.role)
        audit(
            ACTION_RECORD_VENDOR_PAYMENT,
            "event_id=${result.eventId};vendor_account_id=${input.vendorAccountId};payment_account_id=${input.paymentAccountId}",
            actor.userId,
            actor.displayName,
            actor.role,
        )
        result
    }.also { SyncScheduler.requestPush() }

    suspend fun recordCreditCollection(
        sessionHandle: String,
        input: CreditCollectionInput,
    ): CreditCollectionResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.SALE_AND_COLLECTION)
        val customer = database.accountDao().findById(input.customerAccountId)
        val customerProfile = database.customerProfileDao().findByAccountId(input.customerAccountId)
        if (customer?.category != "Customer" || customerProfile?.active != true) {
            throw BridgeCommandNotFound("Choose an active customer")
        }
        requirePaymentAccount(input.paymentAccountId)
        requireOpenDate(input.occurredAt, "customer collection")
        val result = CreditCollectionService(database).record(input)
        attribute(result.eventId, actor.userId, actor.displayName, actor.role)
        audit(
            ACTION_RECORD_CREDIT_COLLECTION,
            "event_id=${result.eventId};customer_account_id=${input.customerAccountId};payment_account_id=${input.paymentAccountId}",
            actor.userId,
            actor.displayName,
            actor.role,
        )
        result
    }.also { SyncScheduler.requestPush() }

    suspend fun recordSalaryPayment(
        sessionHandle: String,
        input: SalaryPaymentInput,
    ): SalaryPaymentResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.SALARY_PAYMENT)
        val employee = database.accountDao().findById(input.employeeAccountId)
        val employeeProfile = database.employeeProfileDao().findByAccountId(input.employeeAccountId)
        if (employee?.category != "Salary" || employeeProfile?.active != true) {
            throw BridgeCommandNotFound("Choose an active employee")
        }
        requirePaymentAccount(input.paymentAccountId)
        requireOpenDate(input.occurredAt, "salary payment")
        val result = SalaryPaymentService(database).record(input)
        attribute(result.eventId, actor.userId, actor.displayName, actor.role)
        audit(
            ACTION_RECORD_SALARY_PAYMENT,
            "event_id=${result.eventId};employee_account_id=${input.employeeAccountId};payment_account_id=${input.paymentAccountId}",
            actor.userId,
            actor.displayName,
            actor.role,
        )
        result
    }.also { SyncScheduler.requestPush() }

    suspend fun recordSpoilage(
        sessionHandle: String,
        input: SpoilageInput,
    ): SpoilageResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.PRODUCTION_AND_SPOILAGE)
        val product = database.productDao().findById(input.productId)
        val profile = database.productProfileDao().findByProductId(input.productId)
        if (product == null || profile?.archived == true || profile?.inventoryTracking == false) {
            throw BridgeCommandNotFound("Choose an active inventory-tracked product")
        }
        requireOpenDate(input.occurredAt, "spoilage")
        val result = try {
            SpoilageService(database).record(input)
        } catch (error: Throwable) {
            if (error.hasMessage("Insufficient stock for this product")) {
                throw BridgeCommandInsufficientStock("There is not enough ${product.name} in stock for this spoilage entry.")
            }
            throw error
        }
        attribute(result.eventId, actor.userId, actor.displayName, actor.role)
        audit(
            ACTION_RECORD_SPOILAGE,
            "event_id=${result.eventId};product_id=${input.productId}",
            actor.userId,
            actor.displayName,
            actor.role,
        )
        result
    }.also { SyncScheduler.requestPush() }

    private suspend fun requirePaymentAccount(accountId: Long) {
        if (database.accountDao().findById(accountId)?.category != "Cash/Bank") {
            throw BridgeCommandNotFound("Choose a valid cash or bank account")
        }
    }

    private suspend fun requireOpenDate(occurredAt: String, operation: String) {
        BusinessDateGuard.requireHappenedWithinHorizon(occurredAt)
        if (database.dailyCloseDao().isPostingClosed(occurredAt)) {
            throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording the $operation.")
        }
    }

    private suspend fun attribute(eventId: Long, actorUserId: Long, actorName: String, actorRole: String) {
        database.eventActorLinkDao().insert(
            EventActorLinkEntity(
                eventId = eventId,
                actorUserId = actorUserId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                attributedAt = Instant.now().toString(),
            )
        )
    }

    private suspend fun audit(
        action: String,
        details: String,
        actorUserId: Long,
        actorName: String,
        actorRole: String,
    ) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.now().toString(),
                action = action,
                result = RESULT_SUCCESS,
                actorUserId = actorUserId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                details = details,
            )
        )
    }

    companion object {
        const val ACTION_RECORD_PURCHASE = "bridge_record_purchase"
        const val ACTION_RECORD_SALE = "bridge_record_sale"
        const val ACTION_RECORD_PRODUCTION = "bridge_record_production"
        const val ACTION_RECORD_EXPENSE = "bridge_record_expense"
        const val ACTION_RECORD_VENDOR_PAYMENT = "bridge_record_vendor_payment"
        const val ACTION_RECORD_CREDIT_COLLECTION = "bridge_record_credit_collection"
        const val ACTION_RECORD_SALARY_PAYMENT = "bridge_record_salary_payment"
        const val ACTION_RECORD_SPOILAGE = "bridge_record_spoilage"
        const val RESULT_SUCCESS = "success"
    }
}

private fun Throwable.hasMessage(expected: String): Boolean =
    generateSequence(this) { it.cause }.any { it.message?.contains(expected, ignoreCase = true) == true }
