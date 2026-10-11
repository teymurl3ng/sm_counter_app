package com.saaf.milkshop.application.vendorpayment

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPaymentInput
import java.math.BigDecimal
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

data class NativeVendorPaymentVendor(
    val accountId: Long,
    val name: String,
)

data class NativeVendorPaymentAccount(
    val accountId: Long,
    val name: String,
)

data class NativeVendorPaymentContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val vendors: List<NativeVendorPaymentVendor>,
    val paymentAccounts: List<NativeVendorPaymentAccount>,
    val defaultPaymentAccountId: Long?,
)

data class NativeVendorPaymentRequest(
    val vendorAccountId: Long,
    val paymentAccountId: Long,
    val amountPaisa: Long,
    val notes: String?,
)

data class NativeVendorPaymentReceipt(
    val eventId: Long,
    val vendor: NativeVendorPaymentVendor,
    val paymentAccount: NativeVendorPaymentAccount,
    val amountPaisa: Long,
)

data class NativeVendorPaymentSubmission(
    val receipt: NativeVendorPaymentReceipt,
    val refreshedContext: NativeVendorPaymentContext,
)

/**
 * Typed native boundary for Pay supplier.
 *
 * Vendors and payment accounts come from transaction references.
 * Posting goes through [OperationsBridgeCommandService.recordVendorPayment].
 */
class NativeVendorPaymentFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val reads = HybridReadModelService(database, security)
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeVendorPaymentContext {
        security.requirePermission(sessionHandle, AppPermission.SUPPLIER_PAYMENT)
        val references = reads.transactionReferences(sessionHandle)
        val vendors = references.vendors.map { reference ->
            NativeVendorPaymentVendor(reference.accountId, reference.name)
        }
        val paymentAccounts = references.paymentAccounts.map { reference ->
            NativeVendorPaymentAccount(reference.accountId, reference.name)
        }
        val defaultAccount = paymentAccounts.firstOrNull { it.name.equals("Cash", ignoreCase = true) }
            ?: paymentAccounts.firstOrNull()
        val businessDate = date.toString()
        return NativeVendorPaymentContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            vendors = vendors,
            paymentAccounts = paymentAccounts,
            defaultPaymentAccountId = defaultAccount?.accountId,
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeVendorPaymentRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeVendorPaymentSubmission {
        require(request.amountPaisa > 0L) { "Enter an amount greater than zero." }
        val context = load(sessionHandle, date)
        val vendor = context.vendors.singleOrNull { it.accountId == request.vendorAccountId }
            ?: throw IllegalArgumentException("Choose an active supplier.")
        val paymentAccount = context.paymentAccounts.singleOrNull { it.accountId == request.paymentAccountId }
            ?: throw IllegalArgumentException("Choose a valid Cash/Bank account.")
        // The mutation, required evidence and receipt reads commit together.
        // A read/authorization failure cannot report failure after saving.
        return database.withWriteTransaction {
            val result = commands.recordVendorPayment(
                sessionHandle = sessionHandle,
                input = VendorPaymentInput(
                    vendorAccountId = vendor.accountId,
                    paymentAccountId = paymentAccount.accountId,
                    amount = BigDecimal(request.amountPaisa).movePointLeft(2).toDouble(),
                    occurredAt = context.businessDate,
                    notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            val refreshedContext = load(sessionHandle, date)
            val refreshedVendor = refreshedContext.vendors.singleOrNull {
                it.accountId == vendor.accountId
            } ?: vendor
            val refreshedPayment = refreshedContext.paymentAccounts.singleOrNull {
                it.accountId == paymentAccount.accountId
            } ?: paymentAccount
            NativeVendorPaymentSubmission(
                receipt = NativeVendorPaymentReceipt(
                    eventId = result.eventId,
                    vendor = refreshedVendor,
                    paymentAccount = refreshedPayment,
                    amountPaisa = request.amountPaisa,
                ),
                refreshedContext = refreshedContext,
            )
        }
    }
}
