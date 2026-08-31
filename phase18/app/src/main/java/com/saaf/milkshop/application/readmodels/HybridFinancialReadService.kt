package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AccountLedgerService
import com.saaf.milkshop.domain.AnalyticsService
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.ProfitabilityReadService
import com.saaf.milkshop.domain.UserSecurityService

internal class HybridFinancialReadService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun financialOverview(sessionHandle: String, startDateInclusive: String, endDateExclusive: String): FinancialOverviewDto {
        security.requirePermission(sessionHandle, AppPermission.VIEW_FULL_FINANCIAL)
        val position = AnalyticsService(database).currentPosition()
        return FinancialOverviewDto(
            customers = position.accountBalances.filter { it.category == "Customer" }.map {
                AccountPositionDto(it.accountId, it.name.removePrefix("Customer: ").trim(), it.category, money(it.balance))
            },
            vendors = position.accountBalances.filter { it.category == "Vendor" }.map {
                AccountPositionDto(it.accountId, it.name.removePrefix("Vendor: ").trim(), it.category, money(it.balance))
            },
            totals = AnalyticsService(database).periodSummary(startDateInclusive, endDateExclusive).toFinancialTotalsDto(),
        )
    }

    suspend fun accountLedger(sessionHandle: String, accountId: Long): AccountLedgerDto {
        security.requirePermission(sessionHandle, AppPermission.VIEW_FULL_FINANCIAL)
        val ledger = AccountLedgerService(database).get(accountId)
        return AccountLedgerDto(
            accountId = ledger.accountId,
            accountName = ledger.accountName,
            displayName = ledger.displayName,
            categoryCode = ledger.category,
            currentBalancePaisa = money(ledger.currentBalance),
            entries = ledger.entries.map {
                AccountLedgerEntryDto(
                    logId = it.logId,
                    eventId = it.eventId,
                    occurredAt = it.occurredAt,
                    createdAt = it.createdAt,
                    eventTypeCode = it.eventType,
                    channelCode = it.channel,
                    movementPaisa = money(it.movementAmount),
                    balanceAfterPaisa = money(it.balanceAfter),
                    correctsEventId = it.correctsEventId,
                    isCorrection = it.isCorrection,
                    isSuperseded = it.isSuperseded,
                    notes = it.notes,
                )
            },
        )
    }

    suspend fun profitabilityReport(sessionHandle: String, startInclusive: String, endExclusive: String): ProfitabilityReportDto {
        security.requirePermission(sessionHandle, AppPermission.VIEW_FULL_FINANCIAL)
        val report = ProfitabilityReadService(database).report(startInclusive, endExclusive)
        return ProfitabilityReportDto(
            startInclusive = report.startInclusive,
            endExclusive = report.endExclusive,
            products = report.products.map {
                ProductProfitabilityDto(
                    productId = it.productId,
                    productName = it.productName,
                    quantitySold = decimal(it.quantitySold),
                    salesRevenuePaisa = it.salesRevenuePaisa,
                    costOfProductsSoldPaisa = it.costOfProductsSoldPaisa,
                    profitBeforeShopExpensesPaisa = it.profitBeforeShopExpensesPaisa,
                    grossMarginPercent = it.grossMarginPercent?.let(::decimal),
                    spoilageCostPaisa = it.spoilageCostPaisa,
                    abnormalLossPaisa = it.abnormalLossPaisa,
                    coverageCode = it.coverageCode,
                )
            },
            deliverySurchargePaisa = report.deliverySurchargePaisa,
            recordedOperatingExpensesPaisa = report.recordedOperatingExpensesPaisa,
            recordedSalaryPaymentsPaisa = report.recordedSalaryPaymentsPaisa,
            estimatedRecordedResultPaisa = report.estimatedRecordedResultPaisa,
            coverageCode = report.coverageCode,
            reportingBasisCode = report.reportingBasis,
            valuationScopeCode = report.valuationScope,
        )
    }
}
