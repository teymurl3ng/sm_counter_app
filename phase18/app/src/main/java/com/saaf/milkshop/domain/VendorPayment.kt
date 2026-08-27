package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant

/** Inputs for spec §5. A vendor must be identified so its payable can be reduced. */
data class VendorPaymentInput(
    val vendorAccountId: Long,
    val paymentAccountId: Long,
    val amount: Double,
    val occurredAt: String,
    val notes: String? = null,
)

data class VendorPaymentResult(
    val eventId: Long,
    val amount: Double,
)

class VendorPaymentService(
    private val database: MilkShopDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
) {
    suspend fun record(input: VendorPaymentInput): VendorPaymentResult =
        database.withWriteTransaction {
            require(input.amount > 0.0) { "Amount must be greater than zero" }

            val vendor = accountDao.findById(input.vendorAccountId)
                ?: error("Vendor account ${input.vendorAccountId} does not exist")
            require(vendor.category == "Vendor") {
                "Vendor payment must use a Vendor account"
            }

            val paymentAccount = accountDao.findById(input.paymentAccountId)
                ?: error("Payment account ${input.paymentAccountId} does not exist")
            require(paymentAccount.category == "Cash/Bank") {
                "Vendor payment must use a Cash/Bank account"
            }

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_VENDOR_PAYMENT,
                    notes = input.notes,
                    amount = input.amount,
                )
            )

            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = vendor.accountId,
                    amount = -input.amount,
                )
            )

            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = paymentAccount.accountId,
                    amount = -input.amount,
                )
            )

            VendorPaymentResult(eventId = eventId, amount = input.amount)
        }

    companion object {
        const val EVENT_TYPE_VENDOR_PAYMENT = "VENDOR_PAYMENT"
    }
}
