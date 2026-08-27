package com.saaf.milkshop.data

/**
 * Application-facing read boundary for History & Corrections.
 * The repository performs only existing reads; formatting and presentation
 * semantics remain in the history presentation action layer.
 */
class HistoryDataRepository(
    private val database: MilkShopDatabase,
) {
    data class InventoryLine(
        val productName: String,
        val quantity: Double,
        val unit: String,
    )

    data class FinancialLine(
        val accountName: String,
        val amount: Double,
    )

    data class DetailData(
        val inventoryLines: List<InventoryLine>,
        val financialLines: List<FinancialLine>,
    )

    suspend fun allEvents(): List<EventEntity> = database.eventDao().findAllDescending()

    suspend fun eventById(eventId: Long): EventEntity? = database.eventDao().findById(eventId)

    suspend fun detailData(eventId: Long): DetailData {
        val inventoryLogs = database.inventoryLogDao().findByEventId(eventId)
        val financialLogs = database.financialLogDao().findByEventId(eventId)

        val inventoryLines = inventoryLogs.map { log ->
            val product = database.productDao().findById(log.productId)
            InventoryLine(
                productName = product?.name ?: "Product #${log.productId}",
                quantity = log.quantity,
                unit = product?.unit ?: "",
            )
        }

        val financialLines = financialLogs.map { log ->
            val account = database.accountDao().findById(log.accountId)
            FinancialLine(
                accountName = account?.name ?: "Account #${log.accountId}",
                amount = log.amount,
            )
        }

        return DetailData(
            inventoryLines = inventoryLines,
            financialLines = financialLines,
        )
    }
}
