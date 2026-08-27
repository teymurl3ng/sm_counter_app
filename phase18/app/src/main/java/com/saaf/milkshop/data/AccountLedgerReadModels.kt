package com.saaf.milkshop.data

/**
 * Canonical financial movement row for one account.
 *
 * Ledger truth is movement-based: original movements remain visible and
 * correction events contribute their delta movements. `superseded` marks
 * an original event that has a child correction so presentation code does
 * not have to infer correction relationships itself.
 */
data class AccountLedgerMovementRow(
    val logId: Long,
    val eventId: Long,
    val createdAt: String,
    val occurredAt: String,
    val eventType: String,
    val channel: String?,
    val correctsEventId: Long?,
    val superseded: Boolean,
    val movementAmount: Double,
    val notes: String?,
)
