package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.UserSecurityService

/**
 * Stable permission-aware facade for hybrid reads. Domain-focused readers keep
 * each information area independently maintainable while preserving this API.
 */
class HybridReadModelService(database: MilkShopDatabase, security: UserSecurityService) {
    private val identity = HybridIdentityReadService(database, security)
    private val operations = HybridOperationalReadService(database, security)
    private val finance = HybridFinancialReadService(database, security)
    private val costing = HybridCostingPlanningReadService(database, security)
    private val securityReads = HybridSecurityReadService(database, security)

    suspend fun bootstrapStatus(): BootstrapStatusDto = identity.bootstrapStatus()
    suspend fun currentSession(sessionHandle: String): CurrentSessionDto = identity.currentSession(sessionHandle)
    suspend fun businessSettings(sessionHandle: String): BusinessSettingsDto = identity.businessSettings(sessionHandle)
    suspend fun masterData(sessionHandle: String): MasterDataDto = identity.masterData(sessionHandle)

    suspend fun dashboard(sessionHandle: String, startDateInclusive: String, endDateExclusive: String): DashboardDto =
        operations.dashboard(sessionHandle, startDateInclusive, endDateExclusive)

    suspend fun cashierSummary(sessionHandle: String, startDateInclusive: String, endDateExclusive: String): CashierSummaryDto =
        operations.cashierSummary(sessionHandle, startDateInclusive, endDateExclusive)

    suspend fun transactionReferences(sessionHandle: String): TransactionReferencesDto = operations.transactionReferences(sessionHandle)
    suspend fun eventHistory(sessionHandle: String): List<EventHistoryDto> = operations.eventHistory(sessionHandle)
    suspend fun eventDetail(sessionHandle: String, eventId: Long): EventDetailDto = operations.eventDetail(sessionHandle, eventId)
    suspend fun deliveries(sessionHandle: String): List<DeliveryDto> = operations.deliveries(sessionHandle)
    suspend fun attendance(sessionHandle: String, businessDate: String): List<AttendanceDto> = operations.attendance(sessionHandle, businessDate)
    suspend fun spoilageHistory(sessionHandle: String): List<SpoilageDto> = operations.spoilageHistory(sessionHandle)
    suspend fun reconciliationArchive(sessionHandle: String): ReconciliationArchiveDto = operations.reconciliationArchive(sessionHandle)

    suspend fun financialOverview(sessionHandle: String, startDateInclusive: String, endDateExclusive: String): FinancialOverviewDto =
        finance.financialOverview(sessionHandle, startDateInclusive, endDateExclusive)

    suspend fun accountLedger(sessionHandle: String, accountId: Long): AccountLedgerDto = finance.accountLedger(sessionHandle, accountId)
    suspend fun profitabilityReport(sessionHandle: String, startInclusive: String, endExclusive: String): ProfitabilityReportDto =
        finance.profitabilityReport(sessionHandle, startInclusive, endExclusive)

    suspend fun costingManagement(sessionHandle: String): CostingManagementDto = costing.costingManagement(sessionHandle)
    suspend fun planning(sessionHandle: String): PlanningDto = costing.planning(sessionHandle)
    suspend fun users(sessionHandle: String): List<UserAdministrationDto> = securityReads.users(sessionHandle)
    suspend fun securityAudit(sessionHandle: String): List<SecurityAuditDto> = securityReads.securityAudit(sessionHandle)
}
