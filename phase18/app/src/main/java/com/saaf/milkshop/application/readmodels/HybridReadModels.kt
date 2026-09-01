package com.saaf.milkshop.application.readmodels

/**
 * Phase 5 hybrid-facing read contract.
 *
 * Money is always an integer number of PKR paisa. Quantities and percentages
 * are canonical decimal strings so a JavaScript bridge never has to round
 * business values. Codes are stable machine values intended for UI translation.
 */
data class BootstrapStatusDto(val needsOwnerBootstrap: Boolean)

data class CurrentSessionDto(
    val authenticationStatusCode: String,
    val userId: Long,
    val loginName: String,
    val displayName: String,
    val roleCode: String,
    val employeeAccountId: Long?,
    val effectivePermissionCodes: List<String>,
)

data class StoreIdentityDto(
    val storeName: String,
    val currencyCode: String,
    val phone: String?,
    val address: String?,
    val updatedAt: String,
)

data class BusinessSettingsDto(
    val store: StoreIdentityDto?,
    val vendorMilkRatePaisaPerUnit: Long,
    val vendorDeliveryFeePaisaPerUnit: Long,
    val tsStandardPercent: String,
    val retailMilkRatePaisaPerUnit: Long,
    val retailYogurtRatePaisaPerUnit: Long,
    val defaultDeliverySurchargePaisa: Long,
    val openingState: OpeningStateDto?,
    val expenseCategories: List<ExpenseCategoryDto>,
)

data class OpeningStateDto(
    val eventId: Long,
    val openingDate: String,
    val inventoryBalances: List<OpeningInventoryBalanceDto>,
    val accountBalances: List<OpeningAccountBalanceDto>,
    val notes: String?,
)

data class OpeningInventoryBalanceDto(
    val productId: Long,
    val quantity: String,
)

data class OpeningAccountBalanceDto(
    val accountId: Long,
    val balancePaisa: Long,
)

data class ProductDto(
    val productId: Long,
    val name: String,
    val unit: String,
    val quantityOnHand: String,
    val defaultSaleRatePaisaPerUnit: Long?,
    val inventoryTracking: Boolean,
    val archived: Boolean,
    val notes: String?,
)

data class CustomerDto(
    val accountId: Long,
    val name: String,
    val canonicalBalancePaisa: Long,
    val phone: String?,
    val areaRoute: String?,
    val address: String?,
    val notes: String?,
    val creditLimitPaisa: Long?,
    val dailyQuota: String?,
    val active: Boolean,
)

data class VendorDto(
    val accountId: Long,
    val name: String,
    val canonicalBalancePaisa: Long,
    val phone: String?,
    val location: String?,
    val notes: String?,
    val active: Boolean,
)

data class EmployeeDto(
    val accountId: Long,
    val name: String,
    val canonicalBalancePaisa: Long,
    val roleLabel: String?,
    val phone: String?,
    val referenceSalaryPaisa: Long?,
    val salaryPeriodCode: String?,
    val joinDate: String?,
    val active: Boolean,
)

data class ExpenseCategoryDto(
    val accountId: Long,
    val displayName: String,
    val active: Boolean,
)

data class MasterDataDto(
    val products: List<ProductDto>,
    val customers: List<CustomerDto>,
    val vendors: List<VendorDto>,
    val employees: List<EmployeeDto>,
    val expenseCategories: List<ExpenseCategoryDto>,
)

data class AccountReferenceDto(
    val accountId: Long,
    val name: String,
)

data class ProductReferenceDto(
    val productId: Long,
    val name: String,
    val unit: String,
)

data class TransactionReferencesDto(
    val products: List<ProductReferenceDto>,
    val customers: List<AccountReferenceDto>,
    val vendors: List<AccountReferenceDto>,
    val employees: List<AccountReferenceDto>,
    val paymentAccounts: List<AccountReferenceDto>,
    val expenseCategories: List<ExpenseCategoryDto>,
    val vendorMilkRatePaisaPerUnit: Long?,
    val vendorDeliveryFeePaisaPerUnit: Long?,
    val tsStandardPercent: String?,
    val retailMilkRatePaisaPerUnit: Long?,
    val retailYogurtRatePaisaPerUnit: Long?,
    val defaultDeliverySurchargePaisa: Long?,
)

data class StockPositionDto(
    val productId: Long,
    val name: String,
    val unit: String,
    val quantityOnHand: String,
)

data class AccountPositionDto(
    val accountId: Long,
    val name: String,
    val categoryCode: String,
    val canonicalBalancePaisa: Long,
)

data class DailyTotalsDto(
    val startDateInclusive: String,
    val endDateExclusive: String,
    val eventCount: Long,
    val salesValuePaisa: Long,
    val saleQuantity: String,
    val purchaseAmountPaisa: Long,
    val purchasedMilkLiters: String,
    val producedYogurtKg: String,
    val productionMilkUsedLiters: String,
    val vendorPaymentsPaisa: Long,
    val creditCollectionsPaisa: Long,
    val operatingExpensesPaisa: Long,
    val salaryPaymentsPaisa: Long,
)

data class OperationalTotalsDto(
    val startDateInclusive: String,
    val endDateExclusive: String,
    val eventCount: Long,
    val saleQuantity: String,
    val purchasedMilkLiters: String,
    val producedYogurtKg: String,
    val productionMilkUsedLiters: String,
)

data class DashboardDto(
    val stock: List<StockPositionDto>,
    val totals: OperationalTotalsDto,
)

data class CashierSummaryDto(
    val paymentAccounts: List<AccountPositionDto>,
    val startDateInclusive: String,
    val endDateExclusive: String,
    val salesValuePaisa: Long,
    val creditCollectionsPaisa: Long,
)

data class FinancialOverviewDto(
    val customers: List<AccountPositionDto>,
    val vendors: List<AccountPositionDto>,
    val totals: DailyTotalsDto,
)

data class EventHistoryDto(
    val eventId: Long,
    val createdAt: String,
    val occurredAt: String,
    val eventTypeCode: String,
    val channelCode: String?,
    val correctsEventId: Long?,
    val isCorrection: Boolean,
    val isSuperseded: Boolean,
    val actor: AuditActorDto?,
    val notes: String?,
)

data class InventoryMovementDto(
    val productName: String,
    val quantity: String,
    val unit: String,
)

data class FinancialMovementDto(
    val accountName: String,
    val amountPaisa: Long,
)

data class EventDetailDto(
    val event: EventHistoryDto,
    val inventoryMovements: List<InventoryMovementDto>,
    val financialMovements: List<FinancialMovementDto>?,
)

data class AccountLedgerEntryDto(
    val logId: Long,
    val eventId: Long,
    val occurredAt: String,
    val createdAt: String,
    val eventTypeCode: String,
    val channelCode: String?,
    val movementPaisa: Long,
    val balanceAfterPaisa: Long,
    val correctsEventId: Long?,
    val isCorrection: Boolean,
    val isSuperseded: Boolean,
    val notes: String?,
)

data class AccountLedgerDto(
    val accountId: Long,
    val accountName: String,
    val displayName: String,
    val categoryCode: String,
    val currentBalancePaisa: Long,
    val entries: List<AccountLedgerEntryDto>,
)

data class DeliveryDto(
    val deliveryId: Long,
    val customerAccountId: Long,
    val customerName: String,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: String,
    val route: String?,
    val businessDate: String,
    val saleEventId: Long?,
    val statusCode: String?,
    val statusOccurredAt: String?,
    val notes: String?,
)

data class AttendanceDto(
    val attendanceId: Long,
    val employeeAccountId: Long,
    val employeeName: String,
    val businessDate: String,
    val statusCode: String,
    val notes: String?,
    val createdAt: String,
)

data class SpoilageDto(
    val eventId: Long,
    val occurredAt: String,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: String,
    val reasonCode: String,
    val correctsEventId: Long?,
    val isSuperseded: Boolean,
    val notes: String?,
)

data class CashCountDto(
    val countId: Long,
    val accountId: Long,
    val businessDate: String,
    val countedCashPaisa: Long,
    val expectedCashPaisa: Long,
    val variancePaisa: Long,
    val countedAt: String,
    val notes: String?,
)

data class DailyCloseActionDto(
    val actionId: Long,
    val businessDate: String,
    val actionCode: String,
    val physicalCashCountId: Long?,
    val actor: AuditActorDto,
    val reason: String?,
    val createdAt: String,
    val supersedesActionId: Long?,
)

data class ReconciliationArchiveDto(
    val cashCounts: List<CashCountDto>,
    val closeActions: List<DailyCloseActionDto>,
)

data class ProductProfitabilityDto(
    val productId: Long,
    val productName: String,
    val quantitySold: String,
    val salesRevenuePaisa: Long,
    val costOfProductsSoldPaisa: Long?,
    val profitBeforeShopExpensesPaisa: Long?,
    val grossMarginPercent: String?,
    val spoilageCostPaisa: Long?,
    val abnormalLossPaisa: Long?,
    val coverageCode: String,
)

data class ProfitabilityReportDto(
    val startInclusive: String,
    val endExclusive: String,
    val products: List<ProductProfitabilityDto>,
    val deliverySurchargePaisa: Long,
    val recordedOperatingExpensesPaisa: Long,
    val recordedSalaryPaymentsPaisa: Long,
    val estimatedRecordedResultPaisa: Long?,
    val coverageCode: String,
    val reportingBasisCode: String,
    val valuationScopeCode: String,
)

data class CostingCodeCatalogDto(
    val coverageCodes: List<String>,
    val reportingBasisCodes: List<String>,
    val valuationScopeCodes: List<String>,
    val componentCodes: List<String>,
    val reviewReasonCodes: List<String>,
    val openingBasisKindCodes: List<String>,
    val yieldStatusCodes: List<String>,
    val lossClassificationCodes: List<String>,
    val lossReasonCodes: List<String>,
)

data class CostingPolicyComponentDto(
    val componentCode: String,
    val sourceAmountPaisa: Long,
    val basisQuantityKg: String,
)

data class CostingPolicyDto(
    val policyId: Long,
    val calendarMonth: String,
    val normalCapacityKg: String,
    val reviewReasonCode: String,
    val notes: String?,
    val approvedAt: String,
    val approvedByUserId: Long,
    val supersedesPolicyId: Long?,
    val isSuperseded: Boolean,
    val conversionCostPaisaPerKg: String,
    val components: List<CostingPolicyComponentDto>,
)

data class OpeningCostBasisDto(
    val basisId: Long,
    val openingEventId: Long,
    val productId: Long,
    val quantity: String,
    val totalCostPaisa: Long,
    val basisKindCode: String,
    val evidenceNotes: String?,
    val createdAt: String,
    val createdByUserId: Long,
    val supersedesBasisId: Long?,
    val isSuperseded: Boolean,
)

data class YieldPolicyDto(
    val yieldPolicyId: Long,
    val sampleStartDate: String,
    val sampleEndDate: String,
    val acceptedBatchCount: Int,
    val medianYieldRatio: String,
    val madYieldRatio: String,
    val lowerAlertLimit: String,
    val upperAlertLimit: String,
    val effectiveFrom: String,
    val approvedAt: String,
    val approvedByUserId: Long,
    val supersedesYieldPolicyId: Long?,
    val isSuperseded: Boolean,
)

data class YieldAlertDto(
    val productionEventId: Long,
    val yieldRatio: String,
    val appliedYieldPolicyId: Long?,
    val statusCode: String,
)

data class ProductionLossAssessmentDto(
    val assessmentId: Long,
    val productionEventId: Long,
    val classificationCode: String,
    val abnormalMilkLiters: String,
    val reasonCode: String,
    val notes: String?,
    val assessedAt: String,
    val assessedByUserId: Long,
    val supersedesAssessmentId: Long?,
    val isSuperseded: Boolean,
)

data class CostingManagementDto(
    val codes: CostingCodeCatalogDto,
    val policies: List<CostingPolicyDto>,
    val openingCostBases: List<OpeningCostBasisDto>,
    val yieldPolicies: List<YieldPolicyDto>,
    val yieldAlerts: List<YieldAlertDto>,
    val productionLossAssessments: List<ProductionLossAssessmentDto>,
)

data class BudgetPlanDto(
    val budgetId: Long,
    val categoryCode: String,
    val periodStart: String,
    val periodEndExclusive: String,
    val plannedLiters: String?,
    val plannedAmountPaisa: Long?,
    val actualAmountPaisa: Long,
    val variancePaisa: Long?,
    val vendorRateSnapshotPaisaPerUnit: Long?,
    val retailRateSnapshotPaisaPerUnit: Long?,
    val salesTargetPaisa: Long?,
    val actualSalesPaisa: Long?,
    val salesVariancePaisa: Long?,
    val createdAt: String,
    val supersedesId: Long?,
    val archived: Boolean,
)

data class CollectionsTargetDto(
    val targetId: Long,
    val weekStart: String,
    val weekEndExclusive: String,
    val targetAmountPaisa: Long,
    val actualCollectedPaisa: Long,
    val variancePaisa: Long,
    val createdAt: String,
    val supersedesId: Long?,
    val archived: Boolean,
)

data class PlanningDto(
    val budgets: List<BudgetPlanDto>,
    val collectionTargets: List<CollectionsTargetDto>,
)

data class UserAdministrationDto(
    val userId: Long,
    val loginName: String,
    val displayName: String,
    val roleCode: String,
    val employeeAccountId: Long?,
    val statusCode: String,
    val setupRequired: Boolean,
    val createdAt: String,
    val createdByUserId: Long?,
    val updatedAt: String,
    val disabledAt: String?,
    val disabledByUserId: Long?,
)

data class AuditActorDto(
    val userId: Long?,
    val displayName: String?,
    val roleCode: String?,
)

data class SecurityAuditDto(
    val auditId: Long,
    val occurredAt: String,
    val actionCode: String,
    val resultCode: String,
    val reasonCode: String?,
    val actor: AuditActorDto,
    val targetUserId: Long?,
    val details: String?,
)
