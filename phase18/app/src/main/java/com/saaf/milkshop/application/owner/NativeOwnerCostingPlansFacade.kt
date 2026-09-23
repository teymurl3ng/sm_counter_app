package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.CollectionsTargetEntity
import com.saaf.milkshop.data.CostingPolicyComponentEntity
import com.saaf.milkshop.data.CostingPolicyVersionEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.YieldPolicyVersionEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.BudgetManagementService
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.BudgetPlanResult
import com.saaf.milkshop.domain.CollectionsTargetResult
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.OPERATING_BUDGET_CATEGORIES
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.OpeningCostBasisService
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.domain.YieldPolicyService
import com.saaf.milkshop.domain.YogurtProductionService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class NativeOwnerCostingPolicyRow(
    val policyId: Long,
    val calendarMonth: String,
    val statusLabel: String,
    val conversionCostLabel: String,
    val reviewReason: String,
)

data class NativeOwnerCostingComponentRow(
    val componentCode: String,
    val sourceAmountPaisa: Long,
    val sourceAmountLabel: String,
    val basisQuantityKg: String,
)

data class NativeOwnerCostingPolicyDetail(
    val row: NativeOwnerCostingPolicyRow,
    val normalCapacityKg: String,
    val notes: String?,
    val approvedAt: String,
    val components: List<NativeOwnerCostingComponentRow>,
)

data class NativeOwnerCostingPlansSnapshot(
    val policies: List<NativeOwnerCostingPolicyRow>,
    val businessMonth: String,
    val currentMonthSummary: String,
)

data class NativeOwnerBudgetRow(
    val budgetId: Long,
    val category: String,
    val periodStart: String,
    val periodEndExclusive: String,
    val plannedLitersLabel: String?,
    val plannedAmountLabel: String,
    val salesTargetLabel: String?,
    val archived: Boolean,
)

data class NativeOwnerCollectionsTargetRow(
    val targetId: Long,
    val weekStart: String,
    val weekEndExclusive: String,
    val targetAmountLabel: String,
    val archived: Boolean,
)

data class NativeOwnerPlanningSnapshot(
    val budgets: List<NativeOwnerBudgetRow>,
    val collectionTargets: List<NativeOwnerCollectionsTargetRow>,
    val budgetCategories: List<String>,
    val defaultMilkWeekStart: String,
    val defaultOperatingMonthStart: String,
)

data class NativeOwnerOpeningCostRow(
    val openingEventId: Long,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: String,
    val basisId: Long?,
    val totalCostPaisa: Long?,
    val totalCostLabel: String?,
    val totalCostInput: String,
    val basisKindCode: String,
    val basisKindLabel: String,
    val evidenceNotes: String?,
)

data class NativeOwnerOpeningCostSnapshot(
    val rows: List<NativeOwnerOpeningCostRow>,
    val summary: String,
    val basisKinds: List<String>,
)

data class NativeOwnerYieldPolicyRow(
    val yieldPolicyId: Long,
    val sampleStartDate: String,
    val sampleEndDate: String,
    val acceptedBatchCount: Int,
    val medianYieldRatio: String,
    val lowerAlertLimit: String,
    val upperAlertLimit: String,
    val effectiveFrom: String,
    val approvedAt: String,
    val statusLabel: String,
)

data class NativeOwnerYieldBatchRow(
    val productionEventId: Long,
    val yieldRatioLabel: String,
    val statusCode: String,
    val statusLabel: String,
)

data class NativeOwnerYieldContext(
    val policies: List<NativeOwnerYieldPolicyRow>,
    val batches: List<NativeOwnerYieldBatchRow>,
    val activePolicyId: Long?,
    val activeSummary: String,
)

/**
 * Owner Costing & plans boundary: read approved policy versions + components,
 * approve costing/yield policies, and upsert budgets / collections targets via
 * typed domain services. Does not invent rates or call bridge JSON.
 */
class NativeOwnerCostingPlansFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val policies: CostingPolicyService = CostingPolicyService(database, security),
    private val budgets: BudgetManagementService = BudgetManagementService(database),
    private val yields: YieldPolicyService = YieldPolicyService(database, security),
    private val openingState: OpeningStateService = OpeningStateService(database),
    private val openingCosts: OpeningCostBasisService = OpeningCostBasisService(database, security),
) {
    fun reviewReasons(): List<String> = CostingPolicyService.REVIEW_REASONS.sorted()

    fun componentCodes(): List<String> =
        CostingPolicyService.COMPONENT_CODES.sortedWith(
            compareBy { code ->
                when (code) {
                    "culture" -> 0
                    "packaging" -> 1
                    "variable_energy" -> 2
                    "direct_labor" -> 3
                    CostingPolicyService.COMPONENT_FIXED_OVERHEAD -> 4
                    else -> 99
                }
            },
        )

    fun budgetCategories(): List<String> =
        listOf(BUDGET_CATEGORY_MILK_PURCHASES) + OPERATING_BUDGET_CATEGORIES

    fun periodEndExclusive(category: String, periodStart: LocalDate): LocalDate =
        if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
            periodStart.plusDays(7)
        } else {
            periodStart.plusMonths(1)
        }

    suspend fun loadPolicies(
        sessionHandle: String,
        businessDate: String,
    ): NativeOwnerCostingPlansSnapshot {
        requireOwnerBusinessSettings(sessionHandle)
        val versions = database.costingPolicyDao().allVersions()
        val supersededIds = versions.mapNotNull { it.supersedesPolicyId }.toSet()
        val rows = versions
            .sortedWith(
                compareByDescending<CostingPolicyVersionEntity> { it.calendarMonth }
                    .thenByDescending { it.policyId },
            )
            .map { version ->
                val components = database.costingPolicyDao().components(version.policyId)
                version.toRow(
                    superseded = version.policyId in supersededIds,
                    components = components,
                )
            }
        val businessMonth = YearMonth.from(LocalDate.parse(businessDate)).toString()
        val current = policies.active(businessMonth)
        val currentMonthSummary = when {
            current != null -> {
                val superseded = current.version.policyId in supersededIds
                "Current ${businessMonth}: ${if (superseded) "Superseded" else "Active"} · " +
                    formatPaisaPerKg(current.conversionCostPaisaPerKg)
            }
            else -> "No costing policy for $businessMonth yet."
        }
        return NativeOwnerCostingPlansSnapshot(
            policies = rows,
            businessMonth = businessMonth,
            currentMonthSummary = currentMonthSummary,
        )
    }

    suspend fun loadDetail(
        sessionHandle: String,
        policyId: Long,
    ): NativeOwnerCostingPolicyDetail {
        requireOwnerBusinessSettings(sessionHandle)
        val version = database.costingPolicyDao().findVersion(policyId)
            ?: error("This costing policy is not in the policy list.")
        val all = database.costingPolicyDao().allVersions()
        val supersededIds = all.mapNotNull { it.supersedesPolicyId }.toSet()
        val components = database.costingPolicyDao().components(policyId)
        val row = version.toRow(
            superseded = version.policyId in supersededIds,
            components = components,
        )
        return NativeOwnerCostingPolicyDetail(
            row = row,
            normalCapacityKg = version.normalCapacityKg,
            notes = version.notes,
            approvedAt = version.approvedAt,
            components = components
                .sortedBy { it.componentCode }
                .map { it.toRow() },
        )
    }

    /**
     * Approves a new or superseding costing policy via typed domain input.
     * Requires recent authentication (caller handles reauth + retry).
     */
    suspend fun approvePolicy(
        sessionHandle: String,
        input: CostingPolicyInput,
    ): NativeOwnerCostingPolicyDetail {
        requireOwnerBusinessSettings(sessionHandle)
        val snapshot = policies.approve(sessionHandle, input)
        return NativeOwnerCostingPolicyDetail(
            row = snapshot.version.toRow(
                superseded = false,
                components = snapshot.components,
            ),
            normalCapacityKg = snapshot.version.normalCapacityKg,
            notes = snapshot.version.notes,
            approvedAt = snapshot.version.approvedAt,
            components = snapshot.components
                .sortedBy { it.componentCode }
                .map { it.toRow() },
        )
    }

    suspend fun loadPlanning(sessionHandle: String): NativeOwnerPlanningSnapshot {
        requireOwnerBusinessSettings(sessionHandle)
        val categories = budgetCategories()
        val budgetRows = buildList {
            categories.forEach { category ->
                addAll(database.budgetPlanDao().findAllForCategory(category))
            }
        }
            .sortedWith(
                compareByDescending<BudgetPlanEntity> { it.periodStart }
                    .thenByDescending { it.budgetId },
            )
            .map { it.toBudgetRow() }
        val targets = database.collectionsTargetDao().findAll()
            .sortedWith(
                compareByDescending<CollectionsTargetEntity> { it.weekStart }
                    .thenByDescending { it.targetId },
            )
            .map { it.toTargetRow() }
        return NativeOwnerPlanningSnapshot(
            budgets = budgetRows,
            collectionTargets = targets,
            budgetCategories = categories,
            defaultMilkWeekStart = budgets.currentWeekStart().toString(),
            defaultOperatingMonthStart = budgets.currentMonthStart().toString(),
        )
    }

    suspend fun upsertBudget(
        sessionHandle: String,
        input: BudgetPlanInput,
    ): BudgetPlanResult {
        requireOwnerBusinessSettings(sessionHandle)
        return budgets.createOrRevise(input)
    }

    suspend fun copyPreviousBudget(
        sessionHandle: String,
        category: String,
        periodStart: LocalDate,
        periodEndExclusive: LocalDate,
    ): BudgetPlanResult {
        requireOwnerBusinessSettings(sessionHandle)
        if (database.budgetPlanDao().findLatestBefore(category, periodStart.toString()) == null) {
            error("No previous budget exists for this category")
        }
        return budgets.copyPreviousPeriod(category, periodStart, periodEndExclusive)
    }

    suspend fun createOrReviseCollectionsTarget(
        sessionHandle: String,
        weekStart: LocalDate,
        amount: Double,
    ): CollectionsTargetResult {
        requireOwnerBusinessSettings(sessionHandle)
        return budgets.createOrReviseCollectionsTarget(weekStart, amount)
    }

    suspend fun loadOpeningCosts(sessionHandle: String): NativeOwnerOpeningCostSnapshot {
        requireOwnerBusinessSettings(sessionHandle)
        val opening = openingState.current()
            ?: return NativeOwnerOpeningCostSnapshot(
                rows = emptyList(),
                summary = "Record opening balances in Shop settings before adding opening stock costs.",
                basisKinds = openingBasisKinds(),
            )
        val rows = opening.inventoryBalances.mapNotNull { balance ->
            val product = database.productDao().findById(balance.productId) ?: return@mapNotNull null
            if (balance.quantity <= 0.0 || product.name !in setOf("Milk", "Yogurt")) {
                return@mapNotNull null
            }
            val active = database.inventoryCostBasisDao().active(opening.eventId, product.productId)
            NativeOwnerOpeningCostRow(
                openingEventId = opening.eventId,
                productId = product.productId,
                productName = product.name,
                unit = product.unit,
                quantity = trimNumber(balance.quantity),
                basisId = active?.basisId,
                totalCostPaisa = active?.totalCostPaisa,
                totalCostLabel = active?.totalCostPaisa?.let(::formatPaisa),
                totalCostInput = active?.totalCostPaisa?.let(::paisaInput).orEmpty(),
                basisKindCode = active?.basisKind ?: "documented_historical",
                basisKindLabel = openingBasisKindLabel(active?.basisKind ?: "documented_historical"),
                evidenceNotes = active?.evidenceNotes,
            )
        }.sortedBy { it.productName }
        val covered = rows.count { it.basisId != null }
        val summary = when {
            rows.isEmpty() -> "The current opening record has no positive Milk or Yogurt stock that needs a cost basis."
            covered == rows.size -> "All supported opening stock has an active cost basis."
            else -> "$covered of ${rows.size} opening stock items have a cost basis. Missing costs limit valuation coverage."
        }
        return NativeOwnerOpeningCostSnapshot(
            rows = rows,
            summary = summary,
            basisKinds = openingBasisKinds(),
        )
    }

    suspend fun recordOpeningCostBasis(
        sessionHandle: String,
        input: OpeningCostBasisInput,
    ): NativeOwnerOpeningCostSnapshot {
        requireOwnerBusinessSettings(sessionHandle)
        openingCosts.record(sessionHandle, input)
        return loadOpeningCosts(sessionHandle)
    }

    suspend fun loadYieldContext(sessionHandle: String): NativeOwnerYieldContext {
        requireOwnerBusinessSettings(sessionHandle)
        val versions = database.yieldPolicyDao().all()
        val supersededIds = versions.mapNotNull { it.supersedesYieldPolicyId }.toSet()
        val policyRows = versions
            .sortedWith(
                compareByDescending<YieldPolicyVersionEntity> { it.effectiveFrom }
                    .thenByDescending { it.yieldPolicyId },
            )
            .map { version ->
                version.toYieldRow(superseded = version.yieldPolicyId in supersededIds)
            }
        val active = policyRows.firstOrNull { it.statusLabel == "Active" }
        val productionEvents = database.inventoryCostProjectionDao().effectiveEvents()
            .filter { it.eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION }
            .sortedByDescending { it.eventId }
        val batches = productionEvents.map { event ->
            val assessment = yields.assess(event.eventId)
            NativeOwnerYieldBatchRow(
                productionEventId = event.eventId,
                yieldRatioLabel = assessment.yieldRatio
                    .setScale(4, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString() + " kg/L",
                statusCode = assessment.status,
                statusLabel = yieldStatusLabel(assessment.status),
            )
        }
        val activeSummary = when {
            active != null ->
                "Current baseline: ${active.lowerAlertLimit}–${active.upperAlertLimit} kg/L · " +
                    "${active.acceptedBatchCount} batches · from ${active.effectiveFrom}"
            else -> "No yield baseline approved yet. Select at least 30 batches spanning 28 days."
        }
        return NativeOwnerYieldContext(
            policies = policyRows,
            batches = batches,
            activePolicyId = active?.yieldPolicyId,
            activeSummary = activeSummary,
        )
    }

    /**
     * Approves a new or superseding yield policy via typed domain input.
     * Requires recent authentication (caller handles reauth + retry).
     */
    suspend fun approveYieldPolicy(
        sessionHandle: String,
        input: YieldPolicyInput,
    ): NativeOwnerYieldPolicyRow {
        requireOwnerBusinessSettings(sessionHandle)
        val version = yields.approve(sessionHandle, input)
        return version.toYieldRow(superseded = false)
    }

    /**
     * Full passphrase re-authentication for costing / yield policy approve.
     * Wipes [passphrase] before returning.
     */
    suspend fun reauthenticate(
        sessionHandle: String,
        passphrase: CharArray,
    ) {
        try {
            requireOwnerBusinessSettings(sessionHandle)
            security.reauthenticate(sessionHandle, passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun requireOwnerBusinessSettings(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val role = AppRole.parse(actor.role)
        require(role == AppRole.OWNER || role == AppRole.ADMIN) {
            "Costing & plans is only available to Owner or Admin."
        }
    }

    private fun CostingPolicyVersionEntity.toRow(
        superseded: Boolean,
        components: List<CostingPolicyComponentEntity>,
    ): NativeOwnerCostingPolicyRow = NativeOwnerCostingPolicyRow(
        policyId = policyId,
        calendarMonth = calendarMonth,
        statusLabel = if (superseded) "Superseded" else "Active",
        conversionCostLabel = formatPaisaPerKg(CostingPolicyService.ratePerKg(components)),
        reviewReason = reviewReason,
    )

    private fun CostingPolicyComponentEntity.toRow(): NativeOwnerCostingComponentRow =
        NativeOwnerCostingComponentRow(
            componentCode = componentCode,
            sourceAmountPaisa = sourceAmountPaisa,
            sourceAmountLabel = formatPaisa(sourceAmountPaisa),
            basisQuantityKg = basisQuantityKg,
        )

    private fun BudgetPlanEntity.toBudgetRow(): NativeOwnerBudgetRow {
        val salesTarget = if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
            val liters = plannedLiters
            val rate = retailRateSnapshot
            if (liters != null && rate != null) formatPkr(liters * rate) else null
        } else {
            null
        }
        return NativeOwnerBudgetRow(
            budgetId = budgetId,
            category = category,
            periodStart = periodStart,
            periodEndExclusive = periodEndExclusive,
            plannedLitersLabel = plannedLiters?.let { trimNumber(it) + " L" },
            plannedAmountLabel = plannedAmount?.let { formatPkr(it) } ?: "—",
            salesTargetLabel = salesTarget,
            archived = archived,
        )
    }

    private fun CollectionsTargetEntity.toTargetRow(): NativeOwnerCollectionsTargetRow =
        NativeOwnerCollectionsTargetRow(
            targetId = targetId,
            weekStart = weekStart,
            weekEndExclusive = weekEndExclusive,
            targetAmountLabel = formatPkr(targetAmount),
            archived = archived,
        )

    private fun YieldPolicyVersionEntity.toYieldRow(superseded: Boolean): NativeOwnerYieldPolicyRow =
        NativeOwnerYieldPolicyRow(
            yieldPolicyId = yieldPolicyId,
            sampleStartDate = sampleStartDate,
            sampleEndDate = sampleEndDate,
            acceptedBatchCount = acceptedBatchCount,
            medianYieldRatio = medianYieldRatio,
            lowerAlertLimit = lowerAlertLimit,
            upperAlertLimit = upperAlertLimit,
            effectiveFrom = effectiveFrom,
            approvedAt = approvedAt,
            statusLabel = if (superseded) "Superseded" else "Active",
        )

    private fun yieldStatusLabel(status: String): String = when (status) {
        "within_expected_range" -> "Within expected range"
        "review_required" -> "Review this batch"
        else -> "Calibration not ready"
    }

    private fun formatPaisaPerKg(rate: BigDecimal): String {
        val scaled = rate.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return "$scaled paisa/kg"
    }

    private fun formatPaisa(paisa: Long): String {
        val pkr = BigDecimal.valueOf(paisa).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "PKR %,.2f", pkr)
    }

    private fun formatPkr(amount: Double): String =
        String.format(Locale.US, "PKR %,.2f", amount)

    private fun trimNumber(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    private fun paisaInput(paisa: Long): String =
        BigDecimal.valueOf(paisa).movePointLeft(2).stripTrailingZeros().toPlainString()

    private fun openingBasisKinds(): List<String> =
        listOf("documented_historical", "owner_estimate")

    private fun openingBasisKindLabel(code: String): String = when (code) {
        "documented_historical" -> "Documented bill or historical record"
        "owner_estimate" -> "Owner estimate"
        else -> code.replace('_', ' ')
    }
}
