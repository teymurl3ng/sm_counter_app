package com.saaf.milkshop.application.sale

import com.saaf.milkshop.domain.CreditAge

data class NativeSaleCreditSummary(
    val customerId: Long,
    val customerName: String,
    val balancePaisa: Long?,
    val limitPaisa: Long?,
    val projectedBalancePaisa: Long?,
    val saleTotalPaisa: Long,
    val age: CreditAge,
    val checkUnavailable: Boolean,
    val warningKey: String?,
)

data class NativeSaleCreditWarning(val request: NativeSaleRequest, val summary: NativeSaleCreditSummary)
class CreditWarningRequired(val warning: NativeSaleCreditWarning) : IllegalStateException("Review the credit warning before saving.")
