package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.OpeningAccountEntryEntity
import com.saaf.milkshop.data.OpeningBatchEntity
import com.saaf.milkshop.data.OpeningHistoryRow
import com.saaf.milkshop.data.OpeningInventoryEntryEntity
import java.time.Instant
import java.time.LocalDate

data class OpeningInventoryBalance(val productId: Long, val quantity: Double)

data class OpeningAccountBalance(val accountId: Long, val balance: Double)

data class OpeningStateInput(
    val openingDate: String,
    val inventoryBalances: List<OpeningInventoryBalance> = emptyList(),
    val accountBalances: List<OpeningAccountBalance> = emptyList(),
    val notes: String? = null,
)

data class OpeningStateSnapshot(
    val eventId: Long,
    val openingDate: String,
    val inventoryBalances: List<OpeningInventoryBalance>,
    val accountBalances: List<OpeningAccountBalance>,
    val notes: String?,
)

class OpeningStateService(private val database: MilkShopDatabase) {
    suspend fun record(input: OpeningStateInput): OpeningStateSnapshot = database.withWriteTransaction {
        require(database.storeProfileDao().get() != null) { "Save the store identity before opening state" }
        require(database.openingStateDao().initialCount() == 0) { "Opening state has already been recorded" }
        require(database.eventDao().count() == 0) {
            "Opening state can only be recorded before the first business event"
        }
        val validated = validate(input, requireActive = true)
        require(validated.inventory.isNotEmpty() || validated.accounts.isNotEmpty()) {
            "Skip opening state when every opening balance is zero"
        }
        append(input, validated, correctsEventId = null)
    }

    /**
     * The input is the complete corrected opening snapshot. Ledger rows contain
     * only differences from the current snapshot, while detail rows retain the
     * full restated truth for the next correction and for audit display.
     */
    suspend fun correct(input: OpeningStateInput): OpeningStateSnapshot = database.withWriteTransaction {
        val current = currentInternal() ?: error("Opening state has not been recorded")
        val validated = validate(input, requireActive = false)
        val previousInventory = current.inventoryBalances.associate { it.productId to it.quantity }
        val previousAccounts = current.accountBalances.associate { it.accountId to it.balance }
        append(
            input = input,
            validated = validated,
            correctsEventId = current.eventId,
            previousInventory = previousInventory,
            previousAccounts = previousAccounts,
        )
    }

    suspend fun current(): OpeningStateSnapshot? = currentInternal()

    suspend fun history(): List<OpeningHistoryRow> = database.openingStateDao().history()

    private suspend fun currentInternal(): OpeningStateSnapshot? {
        val batch = database.openingStateDao().currentBatch() ?: return null
        val event = database.eventDao().findById(batch.eventId)
            ?: error("Opening event ${batch.eventId} does not exist")
        return OpeningStateSnapshot(
            eventId = event.eventId,
            openingDate = event.occurredAt,
            inventoryBalances = database.openingStateDao().inventoryEntries(event.eventId)
                .map { OpeningInventoryBalance(it.productId, it.quantity) },
            accountBalances = database.openingStateDao().accountEntries(event.eventId)
                .map { OpeningAccountBalance(it.accountId, it.balance) },
            notes = event.notes,
        )
    }

    private suspend fun append(
        input: OpeningStateInput,
        validated: ValidatedOpeningState,
        correctsEventId: Long?,
        previousInventory: Map<Long, Double> = emptyMap(),
        previousAccounts: Map<Long, Double> = emptyMap(),
    ): OpeningStateSnapshot {
        val eventId = database.eventDao().insert(
            EventEntity(
                createdAt = Instant.now().toString(),
                occurredAt = input.openingDate,
                eventType = EVENT_TYPE_OPENING_STATE,
                correctsEventId = correctsEventId,
                notes = input.notes.cleanOptional(),
            )
        )
        database.openingStateDao().insertBatch(OpeningBatchEntity(eventId))

        val inventoryDetails = validated.inventory.map { (productId, quantity) ->
            OpeningInventoryEntryEntity(eventId, productId, quantity)
        }
        val accountDetails = validated.accounts.map { (accountId, balance) ->
            OpeningAccountEntryEntity(eventId, accountId, balance)
        }
        if (inventoryDetails.isNotEmpty()) database.openingStateDao().insertInventoryEntries(inventoryDetails)
        if (accountDetails.isNotEmpty()) database.openingStateDao().insertAccountEntries(accountDetails)

        (previousInventory.keys + validated.inventory.keys).forEach { productId ->
            val delta = validated.inventory.getOrDefault(productId, 0.0) - previousInventory.getOrDefault(productId, 0.0)
            if (delta != 0.0) {
                database.inventoryLogDao().insert(InventoryLogEntity(eventId = eventId, productId = productId, quantity = delta))
            }
        }
        (previousAccounts.keys + validated.accounts.keys).forEach { accountId ->
            val delta = validated.accounts.getOrDefault(accountId, 0.0) - previousAccounts.getOrDefault(accountId, 0.0)
            if (delta != 0.0) {
                database.financialLogDao().insert(FinancialLogEntity(eventId = eventId, accountId = accountId, amount = delta))
            }
        }

        return OpeningStateSnapshot(
            eventId = eventId,
            openingDate = input.openingDate,
            inventoryBalances = inventoryDetails.map { OpeningInventoryBalance(it.productId, it.quantity) },
            accountBalances = accountDetails.map { OpeningAccountBalance(it.accountId, it.balance) },
            notes = input.notes.cleanOptional(),
        )
    }

    private suspend fun validate(input: OpeningStateInput, requireActive: Boolean): ValidatedOpeningState {
        LocalDate.parse(input.openingDate)
        require(input.inventoryBalances.map { it.productId }.distinct().size == input.inventoryBalances.size) {
            "Each product can appear only once in opening state"
        }
        require(input.accountBalances.map { it.accountId }.distinct().size == input.accountBalances.size) {
            "Each account can appear only once in opening state"
        }

        val inventory = linkedMapOf<Long, Double>()
        input.inventoryBalances.forEach { entry ->
            require(entry.quantity.isFinite() && entry.quantity >= 0.0) { "Opening quantity cannot be negative" }
            val product = database.productDao().findById(entry.productId)
                ?: error("Product ${entry.productId} does not exist")
            val profile = database.productProfileDao().findByProductId(product.productId)
            require(profile?.inventoryTracking != false) { "Product ${entry.productId} is not inventory tracked" }
            if (requireActive) require(profile?.archived != true) { "Product ${entry.productId} is archived" }
            if (entry.quantity > 0.0) inventory[entry.productId] = entry.quantity
        }

        val accounts = linkedMapOf<Long, Double>()
        input.accountBalances.forEach { entry ->
            require(entry.balance.isFinite() && entry.balance >= 0.0) { "Opening account balance cannot be negative" }
            val account = database.accountDao().findById(entry.accountId)
                ?: error("Account ${entry.accountId} does not exist")
            require(account.category in ACCOUNT_CATEGORIES) {
                "Opening state supports only Cash/Bank, Customer, and Vendor accounts"
            }
            if (requireActive && account.category != "Cash/Bank") {
                requireActiveProfile(database, account.accountId, account.category)
            }
            if (entry.balance > 0.0) accounts[entry.accountId] = entry.balance
        }
        return ValidatedOpeningState(inventory, accounts)
    }

    private data class ValidatedOpeningState(
        val inventory: LinkedHashMap<Long, Double>,
        val accounts: LinkedHashMap<Long, Double>,
    )

    companion object {
        const val EVENT_TYPE_OPENING_STATE = "OPENING_STATE"
        private val ACCOUNT_CATEGORIES = setOf("Cash/Bank", "Customer", "Vendor")
    }
}
