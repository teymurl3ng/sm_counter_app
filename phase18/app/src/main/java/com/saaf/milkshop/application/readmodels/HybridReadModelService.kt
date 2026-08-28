package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AccountLedgerService
import com.saaf.milkshop.domain.AnalyticsPeriodSummary
import com.saaf.milkshop.domain.AnalyticsService
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BusinessSettingsService
import com.saaf.milkshop.domain.CostCoverageCode
import com.saaf.milkshop.domain.CostReportingScope
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.OpeningCostBasisService
import com.saaf.milkshop.domain.ProfitabilityReadService
import com.saaf.milkshop.domain.ProductionLossAssessmentService
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.YieldPolicyService
import com.saaf.milkshop.domain.YogurtProductionService
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Permission-aware, read-only boundary for the future hybrid UI.
 * This class composes existing authoritative queries; it owns no accounting rules.
 */
class HybridReadModelService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun bootstrapStatus(): BootstrapStatusDto =
        BootstrapStatusDto(security.needsOwnerBootstrap())

    suspend fun currentSession(sessionHandle: String): CurrentSessionDto {
        val user = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(user.role)
        return CurrentSessionDto(
            authenticationStatusCode = "authenticated",
            userId = user.userId,
            loginName = user.loginName,
            displayName = user.displayName,
            roleCode = role.wireValue,
            employeeAccountId = user.employeeAccountId,
            effectivePermissionCodes = RolePermissions.permissions(role).map { it.name.lowercase() }.sorted(),
        )
    }

    suspend fun dashboard(
        sessionHandle: String,
        startDateInclusive: String,
        endDateExclusive: String,
    ): DashboardDto {
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val position = AnalyticsService(database).currentPosition()
        val totals = AnalyticsService(database).periodSummary(startDateInclusive, endDateExclusive)
        return DashboardDto(
            stock = position.stock.map {
                StockPositionDto(it.productId, it.name, it.unit, decimal(it.quantityOnHand))
            },
            totals = totals.toOperationalDto(),
        )
    }

    suspend fun cashierSummary(
        sessionHandle: String,
        startDateInclusive: String,
        endDateExclusive: String,
    ): CashierSummaryDto {
        security.requirePermission(sessionHandle, AppPermission.SALE_AND_COLLECTION)
        val position = AnalyticsService(database).currentPosition()
        val totals = AnalyticsService(database).periodSummary(startDateInclusive, endDateExclusive)
        return CashierSummaryDto(
            paymentAccounts = position.accountBalances.filter { it.category == "Cash/Bank" }.map {
                AccountPositionDto(it.accountId, it.name, it.category, money(it.balance))
            },
            startDateInclusive = totals.startDateInclusive,
            endDateExclusive = totals.endDateExclusive,
            salesValuePaisa = money(totals.salesValue),
            creditCollectionsPaisa = money(totals.creditCollections),
        )
    }

    suspend fun financialOverview(
        sessionHandle: String,
        startDateInclusive: String,
        endDateExclusive: String,
    ): FinancialOverviewDto {
        security.requirePermission(sessionHandle, AppPermission.VIEW_FULL_FINANCIAL)
        val position = AnalyticsService(database).currentPosition()
        return FinancialOverviewDto(
            customers = position.accountBalances.filter { it.category == "Customer" }.map {
                AccountPositionDto(it.accountId, it.name, it.category, money(it.balance))
            },
            vendors = position.accountBalances.filter { it.category == "Vendor" }.map {
                AccountPositionDto(it.accountId, it.name, it.category, money(it.balance))
            },
            totals = AnalyticsService(database).periodSummary(startDateInclusive, endDateExclusive).toDto(),
        )
    }

    suspend fun businessSettings(sessionHandle: String): BusinessSettingsDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val settings = BusinessSettingsService(database).get()
        val store = database.storeProfileDao().get()?.let {
            StoreIdentityDto(it.storeName, it.currencyCode, it.phone, it.address, it.updatedAt)
        }
        val opening = OpeningStateService(database).current()?.let { snapshot ->
            OpeningStateDto(
                eventId = snapshot.eventId,
                openingDate = snapshot.openingDate,
                inventoryBalances = snapshot.inventoryBalances.map {
                    OpeningInventoryBalanceDto(it.productId, decimal(it.quantity))
                },
                accountBalances = snapshot.accountBalances.map {
                    OpeningAccountBalanceDto(it.accountId, money(it.balance))
                },
                notes = snapshot.notes,
            )
        }
        return BusinessSettingsDto(
            store = store,
            vendorMilkRatePaisaPerUnit = money(settings.milkRateVendor),
            vendorDeliveryFeePaisaPerUnit = money(settings.deliveryFeeVendor),
            tsStandardPercent = decimal(settings.tsStandard),
            retailMilkRatePaisaPerUnit = money(settings.retailRateMilk),
            retailYogurtRatePaisaPerUnit = money(settings.retailRateYogurt),
            defaultDeliverySurchargePaisa = money(settings.defaultDeliverySurcharge),
            openingState = opening,
            expenseCategories = database.expenseCategoryProfileDao().all().map {
                ExpenseCategoryDto(it.accountId, it.displayName, it.active)
            },
        )
    }

    suspend fun masterData(sessionHandle: String): MasterDataDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        val stock = database.readModelDao().currentStock().associateBy { it.productId }
        val balances = database.readModelDao().accountBalances().associateBy { it.accountId }
        val productProfiles = database.productProfileDao().all().associateBy { it.productId }
        val customerProfiles = database.customerProfileDao().all().associateBy { it.accountId }
        val vendorProfiles = database.vendorProfileDao().all().associateBy { it.accountId }
        val employeeProfiles = database.employeeProfileDao().all().associateBy { it.accountId }
        val products = database.productDao().all().map { product ->
            val profile = productProfiles[product.productId]
            ProductDto(
                productId = product.productId,
                name = product.name,
                unit = product.unit,
                quantityOnHand = decimal(stock[product.productId]?.quantityOnHand ?: 0.0),
                defaultSaleRatePaisaPerUnit = profile?.defaultSaleRate?.let(::money),
                inventoryTracking = profile?.inventoryTracking ?: true,
                archived = profile?.archived ?: false,
                notes = profile?.notes,
            )
        }
        return MasterDataDto(
            products = products,
            customers = database.accountDao().findByCategory("Customer").map { account ->
                val profile = customerProfiles[account.accountId]
                CustomerDto(
                    accountId = account.accountId,
                    name = account.name,
                    canonicalBalancePaisa = money(balances[account.accountId]?.balance ?: 0.0),
                    phone = profile?.phone,
                    areaRoute = profile?.areaRoute,
                    address = profile?.address,
                    notes = profile?.notes,
                    creditLimitPaisa = profile?.creditLimit?.let(::money),
                    dailyQuota = profile?.dailyQuota?.let(::decimal),
                    active = profile?.active ?: true,
                )
            },
            vendors = database.accountDao().findByCategory("Vendor").map { account ->
                val profile = vendorProfiles[account.accountId]
                VendorDto(
                    accountId = account.accountId,
                    name = account.name,
                    canonicalBalancePaisa = money(balances[account.accountId]?.balance ?: 0.0),
                    phone = profile?.phone,
                    location = profile?.location,
                    notes = profile?.notes,
                    active = profile?.active ?: true,
                )
            },
            employees = database.accountDao().findByCategory("Salary").map { account ->
                val profile = employeeProfiles[account.accountId]
                EmployeeDto(
                    accountId = account.accountId,
                    name = account.name,
                    canonicalBalancePaisa = money(balances[account.accountId]?.balance ?: 0.0),
                    roleLabel = profile?.role,
                    phone = profile?.phone,
                    referenceSalaryPaisa = profile?.referenceSalary?.let(::money),
                    salaryPeriodCode = profile?.salaryPeriod,
                    joinDate = profile?.joinDate,
                    active = profile?.active ?: true,
                )
            },
            expenseCategories = database.expenseCategoryProfileDao().all().map {
                ExpenseCategoryDto(it.accountId, it.displayName, it.active)
            },
        )
    }

    suspend fun transactionReferences(sessionHandle: String): TransactionReferencesDto {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(actor.role)
        fun allows(permission: AppPermission) = RolePermissions.allows(role, permission)
        val canSell = allows(AppPermission.SALE_AND_COLLECTION)
        val canPurchase = allows(AppPermission.PURCHASE)
        val canPaySupplier = allows(AppPermission.SUPPLIER_PAYMENT)
        val canPaySalary = allows(AppPermission.SALARY_PAYMENT)
        val canProduce = allows(AppPermission.PRODUCTION_AND_SPOILAGE)
        val canDeliver = allows(AppPermission.DELIVERY)
        val canAttend = allows(AppPermission.ATTENDANCE)
        val needsPaymentAccounts = canSell || canPaySupplier || canPaySalary ||
            allows(AppPermission.EXPENSE)
        val settings = database.settingsDao().getAll().associate { it.key to it.value }
        val productProfiles = database.productProfileDao().all().associateBy { it.productId }
        suspend fun refs(category: String) = database.accountDao().findByCategory(category)
            .map { AccountReferenceDto(it.accountId, it.name) }
        val settingKeys = com.saaf.milkshop.domain.SettingKeys
        return TransactionReferencesDto(
            products = database.productDao().all().filter { product ->
                (canSell || canPurchase || canProduce || canDeliver) && productProfiles[product.productId]?.archived != true
            }.map { ProductReferenceDto(it.productId, it.name, it.unit) },
            customers = if (canSell || canDeliver) refs("Customer") else emptyList(),
            vendors = if (canPurchase || canPaySupplier) refs("Vendor") else emptyList(),
            employees = if (canPaySalary || canAttend) refs("Salary") else emptyList(),
            paymentAccounts = if (needsPaymentAccounts) refs("Cash/Bank") else emptyList(),
            vendorMilkRatePaisaPerUnit = settings[settingKeys.MILK_RATE_VENDOR]?.takeIf { canPurchase }?.let(::money),
            vendorDeliveryFeePaisaPerUnit = settings[settingKeys.DELIVERY_FEE_VENDOR]?.takeIf { canPurchase }?.let(::money),
            tsStandardPercent = settings[settingKeys.TS_STANDARD]?.takeIf { canPurchase }?.let(::decimal),
            retailMilkRatePaisaPerUnit = settings[settingKeys.RETAIL_RATE_MILK]?.takeIf { canSell }?.let(::money),
            retailYogurtRatePaisaPerUnit = settings[settingKeys.RETAIL_RATE_YOGURT]?.takeIf { canSell }?.let(::money),
            defaultDeliverySurchargePaisa = settings[settingKeys.DEFAULT_DELIVERY_SURCHARGE]?.takeIf { canSell }?.let(::money),
        )
    }

    suspend fun eventHistory(sessionHandle: String): List<EventHistoryDto> {
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val events = database.eventDao().findAllDescending()
        val supersededIds = events.mapNotNull { it.correctsEventId }.toSet()
        return events.map { it.toHistoryDto(supersededIds) }
    }

    suspend fun eventDetail(sessionHandle: String, eventId: Long): EventDetailDto {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val event = database.eventDao().findById(eventId) ?: error("Event $eventId does not exist")
        val events = database.eventDao().findAllDescending()
        val inventory = database.inventoryLogDao().findByEventId(eventId).map { movement ->
            val product = database.productDao().findById(movement.productId)
            InventoryMovementDto(
                productName = product?.name ?: "Product #${movement.productId}",
                quantity = decimal(movement.quantity),
                unit = product?.unit ?: "",
            )
        }
        val financial = if (RolePermissions.allows(AppRole.parse(actor.role), AppPermission.VIEW_FULL_FINANCIAL)) {
            database.financialLogDao().findByEventId(eventId).map { movement ->
                val account = database.accountDao().findById(movement.accountId)
                FinancialMovementDto(account?.name ?: "Account #${movement.accountId}", money(movement.amount))
            }
        } else null
        return EventDetailDto(
            event = event.toHistoryDto(events.mapNotNull { it.correctsEventId }.toSet()),
            inventoryMovements = inventory,
            financialMovements = financial,
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

    suspend fun deliveries(sessionHandle: String): List<DeliveryDto> {
        security.requirePermission(sessionHandle, AppPermission.DELIVERY)
        return database.deliveryDao().activeDeliveries().map { delivery ->
            val customer = database.accountDao().findById(delivery.customerAccountId)
            val product = database.productDao().findById(delivery.productId)
            val status = database.deliveryDao().latestStatus(delivery.deliveryId)
            DeliveryDto(
                deliveryId = delivery.deliveryId,
                customerAccountId = delivery.customerAccountId,
                customerName = customer?.name ?: "Customer #${delivery.customerAccountId}",
                productId = delivery.productId,
                productName = product?.name ?: "Product #${delivery.productId}",
                unit = product?.unit ?: "",
                quantity = decimal(delivery.quantity),
                route = delivery.route,
                businessDate = delivery.businessDate,
                saleEventId = delivery.saleEventId,
                statusCode = status?.status,
                statusOccurredAt = status?.occurredAt,
                notes = delivery.notes,
            )
        }
    }

    suspend fun attendance(sessionHandle: String, businessDate: String): List<AttendanceDto> {
        security.requirePermission(sessionHandle, AppPermission.ATTENDANCE)
        return database.accountDao().findByCategory("Salary").mapNotNull { employee ->
            database.attendanceDao().current(employee.accountId, businessDate)?.let {
                AttendanceDto(
                    attendanceId = it.attendanceId,
                    employeeAccountId = employee.accountId,
                    employeeName = employee.name,
                    businessDate = it.businessDate,
                    statusCode = it.status,
                    notes = it.notes,
                    createdAt = it.createdAt,
                )
            }
        }
    }

    suspend fun spoilageHistory(sessionHandle: String): List<SpoilageDto> {
        security.requirePermission(sessionHandle, AppPermission.PRODUCTION_AND_SPOILAGE)
        return database.spoilageDao().history().map {
            SpoilageDto(
                eventId = it.eventId,
                occurredAt = it.occurredAt,
                productId = it.productId,
                productName = it.productName,
                unit = it.unit,
                quantity = decimal(it.quantity),
                reasonCode = it.reason,
                correctsEventId = it.correctsEventId,
                isSuperseded = it.superseded,
                notes = it.notes,
            )
        }
    }

    suspend fun reconciliationArchive(sessionHandle: String): ReconciliationArchiveDto {
        security.requirePermission(sessionHandle, AppPermission.CASH_COUNT_AND_CLOSE)
        val counts = database.physicalCashCountDao().all().map { count ->
            val expected = database.physicalCashCountDao().canonicalBalanceAt(count.accountId, count.countedAt)
            CashCountDto(
                countId = count.countId,
                accountId = count.accountId,
                businessDate = count.businessDate,
                countedCashPaisa = money(count.countedAmount),
                expectedCashPaisa = money(expected),
                variancePaisa = money(count.countedAmount - expected),
                countedAt = count.countedAt,
                notes = count.notes,
            )
        }
        val actions = database.dailyCloseDao().all().map {
            DailyCloseActionDto(
                actionId = it.actionId,
                businessDate = it.businessDate,
                actionCode = it.action,
                physicalCashCountId = it.physicalCashCountId,
                actor = AuditActorDto(it.actorUserId, it.actorName, it.actorRole),
                reason = it.reason,
                createdAt = it.createdAt,
                supersedesActionId = it.supersedesActionId,
            )
        }
        return ReconciliationArchiveDto(counts, actions)
    }

    suspend fun profitabilityReport(
        sessionHandle: String,
        startInclusive: String,
        endExclusive: String,
    ): ProfitabilityReportDto {
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

    suspend fun costingManagement(sessionHandle: String): CostingManagementDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val policies = database.costingPolicyDao().allVersions()
        val supersededPolicyIds = policies.mapNotNull { it.supersedesPolicyId }.toSet()
        val bases = database.inventoryCostBasisDao().all()
        val supersededBasisIds = bases.mapNotNull { it.supersedesBasisId }.toSet()
        val yieldPolicies = database.yieldPolicyDao().all()
        val supersededYieldPolicyIds = yieldPolicies.mapNotNull { it.supersedesYieldPolicyId }.toSet()
        val assessments = database.productionLossAssessmentDao().all()
        val supersededAssessmentIds = assessments.mapNotNull { it.supersedesAssessmentId }.toSet()
        val productionEvents = database.inventoryCostProjectionDao().effectiveEvents()
            .filter { it.eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION }
        val yieldService = YieldPolicyService(database, security)

        return CostingManagementDto(
            codes = CostingCodeCatalogDto(
                coverageCodes = listOf(
                    CostCoverageCode.COMPLETE,
                    CostCoverageCode.PARTIAL,
                    CostCoverageCode.MISSING_BASIS,
                    CostCoverageCode.UNSUPPORTED_FLOW,
                    CostCoverageCode.STALE_CONVERSION_POLICY,
                ),
                reportingBasisCodes = listOf(CostReportingScope.REPORTING_BASIS),
                valuationScopeCodes = listOf(CostReportingScope.VALUATION_SCOPE),
                componentCodes = CostingPolicyService.COMPONENT_CODES.sorted(),
                reviewReasonCodes = CostingPolicyService.REVIEW_REASONS.sorted(),
                openingBasisKindCodes = OpeningCostBasisService.BASIS_KINDS.sorted(),
                yieldStatusCodes = listOf("calibration_pending", "review_required", "within_expected_range"),
                lossClassificationCodes = ProductionLossAssessmentService.CLASSIFICATIONS.sorted(),
                lossReasonCodes = ProductionLossAssessmentService.REASON_CODES.sorted(),
            ),
            policies = policies.map { policy ->
                val components = database.costingPolicyDao().components(policy.policyId)
                CostingPolicyDto(
                    policyId = policy.policyId,
                    calendarMonth = policy.calendarMonth,
                    normalCapacityKg = policy.normalCapacityKg,
                    reviewReasonCode = policy.reviewReason,
                    notes = policy.notes,
                    approvedAt = policy.approvedAt,
                    approvedByUserId = policy.approvedByUserId,
                    supersedesPolicyId = policy.supersedesPolicyId,
                    isSuperseded = policy.policyId in supersededPolicyIds,
                    conversionCostPaisaPerKg = decimal(CostingPolicyService.ratePerKg(components)),
                    components = components.map {
                        CostingPolicyComponentDto(it.componentCode, it.sourceAmountPaisa, it.basisQuantityKg)
                    },
                )
            },
            openingCostBases = bases.map {
                OpeningCostBasisDto(
                    basisId = it.basisId,
                    openingEventId = it.openingEventId,
                    productId = it.productId,
                    quantity = it.quantity,
                    totalCostPaisa = it.totalCostPaisa,
                    basisKindCode = it.basisKind,
                    evidenceNotes = it.evidenceNotes,
                    createdAt = it.createdAt,
                    createdByUserId = it.createdByUserId,
                    supersedesBasisId = it.supersedesBasisId,
                    isSuperseded = it.basisId in supersededBasisIds,
                )
            },
            yieldPolicies = yieldPolicies.map {
                YieldPolicyDto(
                    yieldPolicyId = it.yieldPolicyId,
                    sampleStartDate = it.sampleStartDate,
                    sampleEndDate = it.sampleEndDate,
                    acceptedBatchCount = it.acceptedBatchCount,
                    medianYieldRatio = it.medianYieldRatio,
                    madYieldRatio = it.madYieldRatio,
                    lowerAlertLimit = it.lowerAlertLimit,
                    upperAlertLimit = it.upperAlertLimit,
                    effectiveFrom = it.effectiveFrom,
                    approvedAt = it.approvedAt,
                    approvedByUserId = it.approvedByUserId,
                    supersedesYieldPolicyId = it.supersedesYieldPolicyId,
                    isSuperseded = it.yieldPolicyId in supersededYieldPolicyIds,
                )
            },
            yieldAlerts = productionEvents.map { event ->
                val assessment = yieldService.assess(event.eventId)
                YieldAlertDto(
                    productionEventId = assessment.productionEventId,
                    yieldRatio = decimal(assessment.yieldRatio),
                    appliedYieldPolicyId = assessment.policyId,
                    statusCode = assessment.status,
                )
            },
            productionLossAssessments = assessments.map {
                ProductionLossAssessmentDto(
                    assessmentId = it.assessmentId,
                    productionEventId = it.productionEventId,
                    classificationCode = it.classification,
                    abnormalMilkLiters = it.abnormalMilkLiters,
                    reasonCode = it.reasonCode,
                    notes = it.notes,
                    assessedAt = it.assessedAt,
                    assessedByUserId = it.assessedByUserId,
                    supersedesAssessmentId = it.supersedesAssessmentId,
                    isSuperseded = it.assessmentId in supersededAssessmentIds,
                )
            },
        )
    }

    suspend fun planning(sessionHandle: String): PlanningDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val budgets = buildList {
            val categories = listOf(com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES) +
                com.saaf.milkshop.domain.OPERATING_BUDGET_CATEGORIES
            categories.forEach { category -> addAll(database.budgetPlanDao().findAllForCategory(category)) }
        }.sortedWith(compareByDescending<com.saaf.milkshop.data.BudgetPlanEntity> { it.periodStart }.thenByDescending { it.budgetId })
        return PlanningDto(
            budgets = budgets.map {
                BudgetPlanDto(
                    budgetId = it.budgetId,
                    categoryCode = it.category,
                    periodStart = it.periodStart,
                    periodEndExclusive = it.periodEndExclusive,
                    plannedLiters = it.plannedLiters?.let(::decimal),
                    plannedAmountPaisa = it.plannedAmount?.let(::money),
                    vendorRateSnapshotPaisaPerUnit = it.vendorRateSnapshot?.let(::money),
                    retailRateSnapshotPaisaPerUnit = it.retailRateSnapshot?.let(::money),
                    createdAt = it.createdAt,
                    supersedesId = it.supersedesId,
                    archived = it.archived,
                )
            },
            collectionTargets = database.collectionsTargetDao().findAll().map {
                CollectionsTargetDto(
                    targetId = it.targetId,
                    weekStart = it.weekStart,
                    weekEndExclusive = it.weekEndExclusive,
                    targetAmountPaisa = money(it.targetAmount),
                    createdAt = it.createdAt,
                    supersedesId = it.supersedesId,
                    archived = it.archived,
                )
            },
        )
    }

    suspend fun users(sessionHandle: String): List<UserAdministrationDto> =
        security.listUsers(sessionHandle).map(AppUserEntity::toAdministrationDto)

    suspend fun securityAudit(sessionHandle: String): List<SecurityAuditDto> {
        security.requirePermission(sessionHandle, AppPermission.VIEW_SECURITY_AUDIT)
        return database.securityAuditDao().all().map {
            SecurityAuditDto(
                auditId = it.auditId,
                occurredAt = it.occurredAt,
                actionCode = it.action,
                resultCode = it.result,
                reasonCode = it.reasonCode,
                actor = AuditActorDto(it.actorUserId, it.actorNameSnapshot, it.actorRoleSnapshot),
                targetUserId = it.targetUserId,
                details = it.details,
            )
        }
    }

    private suspend fun EventEntity.toHistoryDto(supersededIds: Set<Long>): EventHistoryDto {
        val actor = database.eventActorLinkDao().find(eventId)?.let {
            AuditActorDto(it.actorUserId, it.actorNameSnapshot, it.actorRoleSnapshot)
        }
        return EventHistoryDto(
            eventId = eventId,
            createdAt = createdAt,
            occurredAt = occurredAt,
            eventTypeCode = eventType,
            channelCode = channel,
            correctsEventId = correctsEventId,
            isCorrection = correctsEventId != null,
            isSuperseded = eventId in supersededIds,
            actor = actor,
            notes = notes,
        )
    }
}

private fun AnalyticsPeriodSummary.toDto() = DailyTotalsDto(
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

private fun AnalyticsPeriodSummary.toOperationalDto() = OperationalTotalsDto(
    startDateInclusive = startDateInclusive,
    endDateExclusive = endDateExclusive,
    eventCount = eventCount,
    saleQuantity = decimal(saleQuantity),
    purchasedMilkLiters = decimal(purchasedMilkLiters),
    producedYogurtKg = decimal(producedYogurtKg),
    productionMilkUsedLiters = decimal(productionMilkUsedLiters),
)

private fun AppUserEntity.toAdministrationDto() = UserAdministrationDto(
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

private fun money(value: Double): Long {
    require(value.isFinite()) { "Money value must be finite" }
    return BigDecimal.valueOf(value).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

private fun decimal(value: Double): String {
    require(value.isFinite()) { "Decimal value must be finite" }
    return decimal(BigDecimal.valueOf(value))
}

private fun decimal(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
