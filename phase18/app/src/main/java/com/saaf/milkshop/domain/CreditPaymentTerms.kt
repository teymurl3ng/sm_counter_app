package com.saaf.milkshop.domain

import java.time.LocalDate

enum class CreditTermKind { NET_DAYS, CALENDAR_MONTHS }

/** Version 1 agreement: no editable deadline, installments or bill-sending schedule. */
data class CreditPaymentTerm(val kind: CreditTermKind, val value: Int) {
    init {
        require(when (kind) {
            CreditTermKind.NET_DAYS -> value in 1..365
            CreditTermKind.CALENDAR_MONTHS -> value == 1
        }) { "Choose 1–365 days or one calendar month." }
    }

    val label: String get() = when (kind) {
        CreditTermKind.NET_DAYS -> "$value ${if (value == 1) "day" else "days"}"
        CreditTermKind.CALENDAR_MONTHS -> "1 calendar month"
    }

    fun dueDate(saleDate: LocalDate): LocalDate = when (kind) {
        CreditTermKind.NET_DAYS -> saleDate.plusDays(value.toLong())
        CreditTermKind.CALENDAR_MONTHS -> saleDate.plusMonths(value.toLong())
    }

    companion object {
        val Weekly = CreditPaymentTerm(CreditTermKind.NET_DAYS, 7)
        val Monthly = CreditPaymentTerm(CreditTermKind.CALENDAR_MONTHS, 1)
    }
}

/** A root sale's historical agreement. A future customer preference cannot replace it. */
data class CreditSaleAgreement(
    val rootSaleUid: String,
    val customerUid: String,
    val term: CreditPaymentTerm,
    val preferenceRevisionUid: String,
    val contractVersion: Int = 1,
) {
    init {
        require(rootSaleUid.isNotBlank() && customerUid.isNotBlank() && preferenceRevisionUid.isNotBlank())
        require(contractVersion == 1) { "Unsupported credit agreement version." }
    }
}

/** Absence is not proof of legacy. The persistence boundary must classify it explicitly. */
sealed interface CreditTermEvidence {
    data class Recorded(val agreement: CreditSaleAgreement) : CreditTermEvidence
    data object LegacyWeekly : CreditTermEvidence
    data object Unavailable : CreditTermEvidence
}
