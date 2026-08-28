package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.domain.AnalyticsPeriodSummary
import java.math.BigDecimal
import java.math.RoundingMode

internal fun AnalyticsPeriodSummary.toFinancialTotalsDto() = DailyTotalsDto(
    startDateInclusive = startDateInclusive,
    endDateExclusive = endDateExclusive,
    eventCount = eventCount,
    salesValuePaisa = money(salesValue),
    saleQuantity = decimal(saleQuantity),
    purchaseAmountPaisa = money(purchaseAmount),
    purchasedMilkLiters = decimal(purchasedMilkLiters),
    producedYogurtKg = decimal(producedYogurtKg),
    productionMilkUsedLiters = decimal(productionMilkUsedLiters),
    vendorPaymentsPaisa = money(vendorPayments),
    creditCollectionsPaisa = money(creditCollections),
    operatingExpensesPaisa = money(operatingExpenses),
    salaryPaymentsPaisa = money(salaryPayments),
)

internal fun AnalyticsPeriodSummary.toOperationalTotalsDto() = OperationalTotalsDto(
    startDateInclusive = startDateInclusive,
    endDateExclusive = endDateExclusive,
    eventCount = eventCount,
    saleQuantity = decimal(saleQuantity),
    purchasedMilkLiters = decimal(purchasedMilkLiters),
    producedYogurtKg = decimal(producedYogurtKg),
    productionMilkUsedLiters = decimal(productionMilkUsedLiters),
)

internal fun AppUserEntity.toAdministrationDto() = UserAdministrationDto(
    userId = userId,
    loginName = loginName,
    displayName = displayName,
    roleCode = role,
    employeeAccountId = employeeAccountId,
    statusCode = status,
    setupRequired = credentialSetupRequired,
    createdAt = createdAt,
    createdByUserId = createdByUserId,
    updatedAt = updatedAt,
    disabledAt = disabledAt,
    disabledByUserId = disabledByUserId,
)

internal fun money(value: Double): Long {
    require(value.isFinite()) { "Money value must be finite" }
    return BigDecimal.valueOf(value).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

internal fun decimal(value: Double): String {
    require(value.isFinite()) { "Decimal value must be finite" }
    return decimal(BigDecimal.valueOf(value))
}

internal fun decimal(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
