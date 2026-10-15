package com.saaf.milkshop.data

data class CustomerStatementMovementRow(
    val eventId: Long, val eventUid: String, val occurredAt: String, val eventType: String,
    val correctsEventId: Long?, val movementAmount: Double, val balanceAfter: Double,
)

data class CustomerStatementProductRow(val eventId: Long, val productName: String, val unit: String, val quantity: Double)
