package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogDao
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductDao
import com.saaf.milkshop.data.SettingsDao
import java.time.Instant

sealed interface SalePayment {
    data class Account(val accountId: Long) : SalePayment
    data class OnCredit(val customerAccountId: Long) : SalePayment
}

data class SaleInput(
    val productId: Long,
    val occurredAt: String,
    val channel: String?,
    val unitRate: Double,
    val quantity: Double? = null,
    val value: Double? = null,
    val payment: SalePayment,
    val surchargeEnabled: Boolean = false,
    val surchargeAmount: Double? = null,
    val notes: String? = null,
)

data class SaleResult(
    val eventId: Long,
    val quantity: Double,
    val value: Double,
    val surcharge: Double,
    val total: Double,
)

class SaleService(
    private val database: MilkShopDatabase,
    private val productDao: ProductDao = database.productDao(),
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val inventoryLogDao: InventoryLogDao = database.inventoryLogDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
    private val settingsDao: SettingsDao = database.settingsDao(),
) {
    suspend fun record(input: SaleInput): SaleResult =
        database.withWriteTransaction {
            val hasQuantity = input.quantity != null
            val hasValue = input.value != null
            require(hasQuantity.xor(hasValue)) {
                "Enter exactly one of quantity or value"
            }
            require(input.unitRate > 0.0) { "Rate must be greater than zero" }

            val product = productDao.findById(input.productId)
                ?: error("Product ${input.productId} does not exist")
            requireActiveProduct(database, product.productId)

            val quantity: Double
            val value: Double
            if (input.quantity != null) {
                require(input.quantity > 0.0) { "Quantity must be greater than zero" }
                quantity = input.quantity
                value = quantity * input.unitRate
            } else {
                require(input.value!! > 0.0) { "Value must be greater than zero" }
                value = input.value
                quantity = value / input.unitRate
            }

            val surcharge = if (!input.surchargeEnabled) {
                0.0
            } else {
                input.surchargeAmount ?: settingsDao.getValue(SettingKeys.DEFAULT_DELIVERY_SURCHARGE)
                ?: error("Missing setting: ${SettingKeys.DEFAULT_DELIVERY_SURCHARGE}")
            }
            require(surcharge >= 0.0) { "Surcharge cannot be negative" }

            val total = value + surcharge

            when (val payment = input.payment) {
                is SalePayment.Account -> {
                    val account = accountDao.findById(payment.accountId)
                        ?: error("Payment account ${payment.accountId} does not exist")
                    require(account.category == "Cash/Bank") {
                        "Sale payment must use a Cash/Bank account"
                    }
                }
                is SalePayment.OnCredit -> {
                    requireActiveProfile(database, payment.customerAccountId, "Customer")
                }
            }

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_SALE,
                    channel = input.channel,
                    saleQuantity = quantity,
                    saleValue = value,
                    unitRate = input.unitRate,
                    surchargeApplied = surcharge,
                    notes = input.notes,
                )
            )

            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = eventId,
                    productId = product.productId,
                    quantity = -quantity,
                )
            )

            val destinationAccountId = when (val payment = input.payment) {
                is SalePayment.Account -> payment.accountId
                is SalePayment.OnCredit -> payment.customerAccountId
            }
            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = destinationAccountId,
                    amount = total,
                )
            )

            SaleResult(
                eventId = eventId,
                quantity = quantity,
                value = value,
                surcharge = surcharge,
                total = total,
            )
        }

    companion object {
        const val EVENT_TYPE_SALE = "SALE"
    }
}
