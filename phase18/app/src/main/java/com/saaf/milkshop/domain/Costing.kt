package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.CostProjectionStateEntity
import com.saaf.milkshop.data.CostingPolicyComponentEntity
import com.saaf.milkshop.data.CostingPolicyVersionEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.InventoryCostBasisEntity
import com.saaf.milkshop.data.InventoryCostMovementEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductionLossAssessmentEntity
import com.saaf.milkshop.data.YieldPolicyVersionEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object CostCoverageCode {
    const val COMPLETE = "complete"
    const val PARTIAL = "partial"
    const val MISSING_BASIS = "missing_basis"
    const val UNSUPPORTED_FLOW = "unsupported_flow"
    const val STALE_CONVERSION_POLICY = "stale_conversion_policy"
}

object CostReportingScope {
    const val REPORTING_BASIS = "cost_based_management"
    const val VALUATION_SCOPE = "cost_only_no_nrv"
}

data class ConversionCostComponentInput(
    val code: String,
    val sourceAmountPaisa: Long,
    val basisQuantityKg: BigDecimal,
)

data class CostingPolicyInput(
    val calendarMonth: String,
    val normalCapacityKg: BigDecimal,
    val reviewReason: String,
    val components: List<ConversionCostComponentInput>,
    val notes: String? = null,
    val supersedesPolicyId: Long? = null,
)

data class CostingPolicySnapshot(
    val version: CostingPolicyVersionEntity,
    val components: List<CostingPolicyComponentEntity>,
    val conversionCostPaisaPerKg: BigDecimal,
)

class CostingPolicyService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun approve(sessionId: String, input: CostingPolicyInput): CostingPolicySnapshot {
        val actor = security.requirePermission(sessionId, AppPermission.MANAGE_BUSINESS_SETTINGS)
        security.sessionManager().requireRecentAuthentication(sessionId)
        val month = YearMonth.parse(input.calendarMonth).toString()
        val capacity = positive(input.normalCapacityKg, "Normal capacity")
        require(input.reviewReason in REVIEW_REASONS) { "Choose a valid costing review reason" }
        require(input.components.map { it.code }.toSet() == COMPONENT_CODES) {
            "Enter culture, packaging, variable energy, direct labor, and fixed production overhead"
        }
        require(input.components.size == COMPONENT_CODES.size) { "Each conversion-cost component can appear only once" }
        input.components.forEach { component ->
            require(component.sourceAmountPaisa >= 0) { "Component cost cannot be negative" }
            positive(component.basisQuantityKg, "Component basis quantity")
            if (component.code == COMPONENT_FIXED_OVERHEAD) {
                require(component.basisQuantityKg.compareTo(capacity) == 0) {
                    "Fixed production overhead must use normal capacity as its basis"
                }
            }
        }

        return database.withWriteTransaction {
            val active = database.costingPolicyDao().activeForMonth(month)
            require(active?.policyId == input.supersedesPolicyId) {
                if (active == null) "This month has no policy to supersede" else "Correct the current active policy version"
            }
            val comparison = active ?: database.costingPolicyDao().latestBeforeMonth(month)
            if (comparison != null) {
                val capacityChanged = materialChange(BigDecimal(comparison.normalCapacityKg), capacity)
                val oldRates = database.costingPolicyDao().components(comparison.policyId).associate {
                    it.componentCode to BigDecimal.valueOf(it.sourceAmountPaisa)
                        .divide(BigDecimal(it.basisQuantityKg), 12, RoundingMode.HALF_UP)
                }
                val newRates = input.components.associate {
                    it.code to BigDecimal.valueOf(it.sourceAmountPaisa)
                        .divide(it.basisQuantityKg, 12, RoundingMode.HALF_UP)
                }
                val componentChanged = COMPONENT_CODES.any { materialChange(oldRates.getValue(it), newRates.getValue(it)) }
                if (capacityChanged && componentChanged) {
                    require(input.reviewReason == "process_change") {
                        "A material component and capacity change requires process-change review"
                    }
                } else if (capacityChanged) {
                    require(input.reviewReason == "capacity_change" || input.reviewReason == "process_change") {
                        "A 10% capacity change requires capacity-change review"
                    }
                } else if (componentChanged) {
                    require(input.reviewReason == "component_change" || input.reviewReason == "process_change") {
                        "A 10% component-cost change requires component-change review"
                    }
                }
            }
            val now = Instant.now().toString()
            val policyId = database.costingPolicyDao().insertVersion(
                CostingPolicyVersionEntity(
                    calendarMonth = month,
                    normalCapacityKg = decimal(capacity),
                    reviewReason = input.reviewReason,
                    notes = input.notes.cleanOptional(),
                    approvedAt = now,
                    approvedByUserId = actor.userId,
                    supersedesPolicyId = input.supersedesPolicyId,
                )
            )
            val components = input.components.sortedBy { it.code }.map {
                CostingPolicyComponentEntity(
                    policyId = policyId,
                    componentCode = it.code,
                    sourceAmountPaisa = it.sourceAmountPaisa,
                    basisQuantityKg = decimal(it.basisQuantityKg),
                )
            }
            database.costingPolicyDao().insertComponents(components)
            val version = database.costingPolicyDao().findVersion(policyId)!!
            CostingPolicySnapshot(version, components, ratePerKg(components))
        }
    }

    suspend fun active(calendarMonth: String): CostingPolicySnapshot? {
        val version = database.costingPolicyDao().activeForMonth(YearMonth.parse(calendarMonth).toString()) ?: return null
        val components = database.costingPolicyDao().components(version.policyId)
        return CostingPolicySnapshot(version, components, ratePerKg(components))
    }

    companion object {
        const val COMPONENT_FIXED_OVERHEAD = "fixed_production_overhead"
        val COMPONENT_CODES = setOf("culture", "packaging", "variable_energy", "direct_labor", COMPONENT_FIXED_OVERHEAD)
        val REVIEW_REASONS = setOf("monthly", "component_change", "capacity_change", "process_change", "quarterly_variance")

        fun ratePerKg(components: List<CostingPolicyComponentEntity>): BigDecimal =
            components.fold(BigDecimal.ZERO) { total, component ->
                total + BigDecimal.valueOf(component.sourceAmountPaisa)
                    .divide(BigDecimal(component.basisQuantityKg), 12, RoundingMode.HALF_UP)
            }

        private fun materialChange(old: BigDecimal, new: BigDecimal): Boolean {
            if (old.signum() == 0) return new.signum() != 0
            return (new - old).abs().divide(old.abs(), 12, RoundingMode.HALF_UP) >= BigDecimal("0.10")
        }
    }
}

data class OpeningCostBasisInput(
    val openingEventId: Long,
    val productId: Long,
    val quantity: BigDecimal,
    val totalCostPaisa: Long,
    val basisKind: String,
    val evidenceNotes: String? = null,
    val supersedesBasisId: Long? = null,
)

class OpeningCostBasisService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun record(sessionId: String, input: OpeningCostBasisInput): InventoryCostBasisEntity {
        val actor = security.requirePermission(sessionId, AppPermission.MANAGE_BUSINESS_SETTINGS)
        security.sessionManager().requireRecentAuthentication(sessionId)
        val quantity = positive(input.quantity, "Opening quantity")
        require(input.totalCostPaisa > 0) { "Opening stock cost must be greater than zero" }
        require(input.basisKind in BASIS_KINDS) { "Choose documented historical cost or owner estimate" }
        if (input.basisKind == "owner_estimate") {
            require(!input.evidenceNotes.isNullOrBlank()) { "Explain how the opening stock estimate was obtained" }
        }

        return database.withWriteTransaction {
            val event = database.eventDao().findById(input.openingEventId)
                ?: error("Opening event ${input.openingEventId} does not exist")
            require(event.eventType == OpeningStateService.EVENT_TYPE_OPENING_STATE) { "Cost basis must reference opening state" }
            require(database.inventoryCostProjectionDao().effectiveEvents().any { it.eventId == event.eventId }) {
                "Cost basis must reference the current effective opening state"
            }
            val product = database.productDao().findById(input.productId)
                ?: error("Product ${input.productId} does not exist")
            require(product.name == "Milk" || product.name == "Yogurt") {
                "Initial authoritative costing supports only Milk and Yogurt"
            }
            val openingQuantity = database.openingStateDao().inventoryEntries(event.eventId)
                .singleOrNull { it.productId == input.productId }?.quantity
                ?: error("The opening state has no positive quantity for ${product.name}")
            require(BigDecimal.valueOf(openingQuantity).compareTo(quantity) == 0) {
                "Opening cost quantity must exactly match the opening stock quantity"
            }
            val active = database.inventoryCostBasisDao().active(event.eventId, product.productId)
            require(active?.basisId == input.supersedesBasisId) {
                if (active == null) "This opening stock has no cost basis to supersede" else "Correct the current active opening cost basis"
            }
            val id = database.inventoryCostBasisDao().insert(
                InventoryCostBasisEntity(
                    openingEventId = event.eventId,
                    productId = product.productId,
                    quantity = decimal(quantity),
                    totalCostPaisa = input.totalCostPaisa,
                    basisKind = input.basisKind,
                    evidenceNotes = input.evidenceNotes.cleanOptional(),
                    createdAt = Instant.now().toString(),
                    createdByUserId = actor.userId,
                    supersedesBasisId = input.supersedesBasisId,
                )
            )
            database.inventoryCostBasisDao().find(id)!!
        }
    }

    companion object {
        val BASIS_KINDS = setOf("documented_historical", "owner_estimate")
    }
}

data class YieldPolicyInput(
    val acceptedProductionEventIds: List<Long>,
    val effectiveFrom: String,
    val supersedesYieldPolicyId: Long? = null,
)

data class YieldAlertAssessment(
    val productionEventId: Long,
    val yieldRatio: BigDecimal,
    val policyId: Long?,
    val status: String,
)

class YieldPolicyService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun assess(productionEventId: Long): YieldAlertAssessment {
        val event = database.inventoryCostProjectionDao().effectiveEvents()
            .singleOrNull { it.eventId == productionEventId }
            ?: error("Yield assessment must reference an effective production event")
        require(event.eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION) { "Yield assessment must reference production" }
        val milk = BigDecimal.valueOf(requireNotNull(event.milkLitersUsed))
        val yogurt = BigDecimal.valueOf(requireNotNull(event.yogurtKgProduced))
        val ratio = yogurt.divide(milk, 12, RoundingMode.HALF_UP)
        val policy = database.yieldPolicyDao().activeForDate(event.occurredAt)
            ?: return YieldAlertAssessment(event.eventId, ratio, null, "calibration_pending")
        val status = if (ratio < BigDecimal(policy.lowerAlertLimit) || ratio > BigDecimal(policy.upperAlertLimit)) {
            "review_required"
        } else {
            "within_expected_range"
        }
        return YieldAlertAssessment(event.eventId, ratio, policy.yieldPolicyId, status)
    }

    suspend fun approve(sessionId: String, input: YieldPolicyInput): YieldPolicyVersionEntity {
        val actor = security.requirePermission(sessionId, AppPermission.MANAGE_BUSINESS_SETTINGS)
        security.sessionManager().requireRecentAuthentication(sessionId)
        require(input.acceptedProductionEventIds.distinct().size == input.acceptedProductionEventIds.size) {
            "Each accepted batch can appear only once"
        }
        require(input.acceptedProductionEventIds.size >= 30) { "Yield calibration needs at least 30 accepted batches" }
        val effectiveFrom = LocalDate.parse(input.effectiveFrom)

        return database.withWriteTransaction {
            val leaves = database.inventoryCostProjectionDao().effectiveEvents().associateBy { it.eventId }
            val batches = input.acceptedProductionEventIds.map { id ->
                val event = leaves[id] ?: error("Accepted batch $id is not an effective event")
                require(event.eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION) { "Accepted yield samples must be production batches" }
                val milk = requireNotNull(event.milkLitersUsed) { "Production batch has no Milk input" }
                val yogurt = requireNotNull(event.yogurtKgProduced) { "Production batch has no Yogurt output" }
                require(milk > 0 && yogurt > 0) { "Accepted production quantities must be positive" }
                event to BigDecimal.valueOf(yogurt).divide(BigDecimal.valueOf(milk), 12, RoundingMode.HALF_UP)
            }
            val start = batches.minOf { LocalDate.parse(it.first.occurredAt) }
            val end = batches.maxOf { LocalDate.parse(it.first.occurredAt) }
            require(ChronoUnit.DAYS.between(start, end) + 1 >= 28) {
                "Yield calibration must span at least 28 calendar days"
            }
            require(effectiveFrom > end) { "Yield policy can take effect only after its observation period" }
            val ratios = batches.map { it.second }.sorted()
            val median = median(ratios)
            val deviations = ratios.map { (it - median).abs() }.sorted()
            val mad = median(deviations)
            val band = (mad * BigDecimal("1.4826") * BigDecimal(3)).max(BigDecimal("0.03"))
            val lower = (median - band).max(BigDecimal.ZERO)
            val upper = median + band
            val id = database.yieldPolicyDao().insert(
                YieldPolicyVersionEntity(
                    sampleStartDate = start.toString(),
                    sampleEndDate = end.toString(),
                    acceptedBatchCount = batches.size,
                    medianYieldRatio = decimal(median),
                    madYieldRatio = decimal(mad),
                    lowerAlertLimit = decimal(lower),
                    upperAlertLimit = decimal(upper),
                    effectiveFrom = effectiveFrom.toString(),
                    approvedAt = Instant.now().toString(),
                    approvedByUserId = actor.userId,
                    supersedesYieldPolicyId = input.supersedesYieldPolicyId,
                )
            )
            database.yieldPolicyDao().find(id)!!
        }
    }

    private fun median(values: List<BigDecimal>): BigDecimal {
        require(values.isNotEmpty())
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle]
        else (values[middle - 1] + values[middle]).divide(BigDecimal(2), 12, RoundingMode.HALF_UP)
    }
}

data class ProductionLossAssessmentInput(
    val productionEventId: Long,
    val classification: String,
    val abnormalMilkLiters: BigDecimal,
    val reasonCode: String,
    val notes: String? = null,
    val supersedesAssessmentId: Long? = null,
)

class ProductionLossAssessmentService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun record(sessionId: String, input: ProductionLossAssessmentInput): ProductionLossAssessmentEntity {
        val actor = security.requirePermission(sessionId, AppPermission.MANAGE_BUSINESS_SETTINGS)
        security.sessionManager().requireRecentAuthentication(sessionId)
        require(input.classification in CLASSIFICATIONS) { "Choose ordinary or abnormal loss" }
        require(input.reasonCode in REASON_CODES) { "Choose what physically happened" }
        val abnormal = nonNegative(input.abnormalMilkLiters, "Abnormal Milk quantity")
        if (input.classification == "ordinary") require(abnormal.signum() == 0) { "Ordinary assessment cannot record abnormal loss" }
        if (input.classification == "abnormal") require(abnormal.signum() > 0) { "Enter the identified abnormal Milk quantity" }
        if (input.reasonCode == "other") require(!input.notes.isNullOrBlank()) { "Explain the production incident" }

        return database.withWriteTransaction {
            val event = database.inventoryCostProjectionDao().effectiveEvents()
                .singleOrNull { it.eventId == input.productionEventId }
                ?: error("Assessment must reference an effective production event")
            require(event.eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION) { "Assessment must reference production" }
            require(abnormal <= BigDecimal.valueOf(requireNotNull(event.milkLitersUsed))) {
                "Abnormal Milk quantity cannot exceed the batch input"
            }
            val active = database.productionLossAssessmentDao().activeForEvent(event.eventId)
            require(active?.assessmentId == input.supersedesAssessmentId) {
                if (active == null) "This batch has no assessment to supersede" else "Correct the current active assessment"
            }
            val id = database.productionLossAssessmentDao().insert(
                ProductionLossAssessmentEntity(
                    productionEventId = event.eventId,
                    classification = input.classification,
                    abnormalMilkLiters = decimal(abnormal),
                    reasonCode = input.reasonCode,
                    notes = input.notes.cleanOptional(),
                    assessedAt = Instant.now().toString(),
                    assessedByUserId = actor.userId,
                    supersedesAssessmentId = input.supersedesAssessmentId,
                )
            )
            database.productionLossAssessmentDao().find(id)!!
        }
    }

    companion object {
        val CLASSIFICATIONS = setOf("ordinary", "abnormal")
        val REASON_CODES = setOf("spill", "contamination", "power_failure", "equipment_failure", "measurement_correction", "other")
    }
}

data class CostProjectionResult(
    val movementCount: Int,
    val coverageCode: String,
    val rebuiltAt: String,
)

class InventoryCostProjectionService(private val database: MilkShopDatabase) {
    suspend fun ensureCurrent(): CostProjectionResult = database.withWriteTransaction {
        val source = database.inventoryCostProjectionDao().sourceWatermarks()
        val state = database.inventoryCostProjectionDao().state()
        if (state != null && state.engineVersion == ENGINE_VERSION &&
            state.sourceEventCount == source.eventCount && state.sourceMaxEventId == source.maxEventId &&
            state.sourceMaxPolicyId == source.maxPolicyId && state.sourceMaxBasisId == source.maxBasisId &&
            state.sourceMaxYieldPolicyId == source.maxYieldPolicyId && state.sourceMaxAssessmentId == source.maxAssessmentId
        ) {
            return@withWriteTransaction CostProjectionResult(
                database.inventoryCostProjectionDao().allMovements().size,
                state.coverageCode,
                state.rebuiltAt,
            )
        }
        rebuildInternal()
    }

    suspend fun rebuild(): CostProjectionResult = database.withWriteTransaction { rebuildInternal() }

    private suspend fun rebuildInternal(): CostProjectionResult {
        val projectionDao = database.inventoryCostProjectionDao()
        projectionDao.clearMovements()
        val events = projectionDao.effectiveEvents()
        val milk = database.productDao().findByName("Milk") ?: error("Seeded Milk product is missing")
        val yogurt = database.productDao().findByName("Yogurt") ?: error("Seeded Yogurt product is missing")
        val pools = mutableMapOf<Long, CostPool>()
        val encounteredCoverage = linkedSetOf<String>()
        var sequence = 0L
        var movementCount = 0

        suspend fun persist(
            event: EventEntity,
            productId: Long,
            role: String,
            kind: String,
            quantityIn: BigDecimal? = null,
            quantityOut: BigDecimal? = null,
            costIn: Long? = null,
            costOut: Long? = null,
            policyId: Long? = null,
            yieldPolicyId: Long? = null,
            assessmentId: Long? = null,
            basisId: Long? = null,
            coverageOverride: String? = null,
        ) {
            val pool = pools.getValue(productId)
            val missingMovementCost = (quantityIn?.signum() == 1 && costIn == null) ||
                (quantityOut?.signum() == 1 && costOut == null)
            val coverage = coverageOverride ?: pool.reason ?: if (missingMovementCost) {
                CostCoverageCode.PARTIAL
            } else {
                CostCoverageCode.COMPLETE
            }
            if (coverage != CostCoverageCode.COMPLETE) encounteredCoverage += coverage
            projectionDao.insertMovement(
                InventoryCostMovementEntity(
                    sourceEventId = event.eventId,
                    productId = productId,
                    movementRole = role,
                    movementKind = kind,
                    sequenceNumber = ++sequence,
                    occurredAt = event.occurredAt,
                    createdAt = event.createdAt,
                    quantityIn = quantityIn?.let(::decimal),
                    quantityOut = quantityOut?.let(::decimal),
                    costInPaisa = costIn,
                    costOutPaisa = costOut,
                    runningQuantity = decimal(pool.quantity),
                    runningCostPaisa = pool.costPaisa,
                    coverageCode = coverage,
                    appliedPolicyId = policyId,
                    appliedYieldPolicyId = yieldPolicyId,
                    appliedAssessmentId = assessmentId,
                    appliedBasisId = basisId,
                )
            )
            movementCount += 1
        }

        for (event in events) {
            when (event.eventType) {
                OpeningStateService.EVENT_TYPE_OPENING_STATE -> {
                    for (entry in database.openingStateDao().inventoryEntries(event.eventId)) {
                        val quantity = BigDecimal.valueOf(entry.quantity)
                        if (quantity.signum() <= 0) continue
                        val supported = entry.productId == milk.productId || entry.productId == yogurt.productId
                        val basis = if (supported) database.inventoryCostBasisDao().active(event.eventId, entry.productId) else null
                        val matches = basis != null && BigDecimal(basis.quantity).compareTo(quantity) == 0
                        val pool = pools.getOrPut(entry.productId) { CostPool() }
                        val reason = when {
                            !supported -> CostCoverageCode.UNSUPPORTED_FLOW
                            !matches -> CostCoverageCode.MISSING_BASIS
                            else -> null
                        }
                        pool.add(quantity, basis?.takeIf { matches }?.totalCostPaisa, reason)
                        persist(event, entry.productId, "opening", "opening", quantityIn = quantity,
                            costIn = basis?.takeIf { matches }?.totalCostPaisa, basisId = basis?.takeIf { matches }?.basisId)
                    }
                }
                VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> {
                    val quantity = BigDecimal.valueOf(requireNotNull(event.rawLiters) { "Purchase ${event.eventId} has no raw litres" })
                    val cost = moneyToPaisa(requireNotNull(event.amountOwed) { "Purchase ${event.eventId} has no amount owed" })
                    val pool = pools.getOrPut(milk.productId) { CostPool() }
                    pool.add(quantity, cost, null)
                    persist(event, milk.productId, "purchase", "purchase", quantityIn = quantity, costIn = cost)
                }
                YogurtProductionService.EVENT_TYPE_PRODUCTION -> {
                    val milkUsed = BigDecimal.valueOf(requireNotNull(event.milkLitersUsed) { "Production ${event.eventId} has no Milk input" })
                    val yogurtMade = BigDecimal.valueOf(requireNotNull(event.yogurtKgProduced) { "Production ${event.eventId} has no Yogurt output" })
                    val assessment = database.productionLossAssessmentDao().activeForEvent(event.eventId)
                    val abnormal = assessment?.takeIf { it.classification == "abnormal" }
                        ?.let { BigDecimal(it.abnormalMilkLiters) } ?: BigDecimal.ZERO
                    val ordinaryInput = milkUsed - abnormal
                    val milkPool = pools.getOrPut(milk.productId) { CostPool.unavailable(CostCoverageCode.MISSING_BASIS) }
                    var abnormalCost: Long? = null
                    if (abnormal.signum() > 0) {
                        val removalCoverage = milkPool.reason
                        abnormalCost = milkPool.remove(abnormal)
                        persist(event, milk.productId, "abnormal_loss", "abnormal_loss", quantityOut = abnormal,
                            costOut = abnormalCost, assessmentId = assessment?.assessmentId,
                            coverageOverride = if (abnormalCost == null) removalCoverage else null)
                    }
                    val transferCoverage = milkPool.reason
                    val transferredMilkCost = milkPool.remove(ordinaryInput)
                    persist(event, milk.productId, "production_input", "production_input", quantityOut = ordinaryInput,
                        costOut = transferredMilkCost, assessmentId = assessment?.assessmentId,
                        coverageOverride = if (transferredMilkCost == null) transferCoverage else null)

                    val policy = database.costingPolicyDao().activeForMonth(YearMonth.from(LocalDate.parse(event.occurredAt)).toString())
                    val conversionCost = policy?.let {
                        val components = database.costingPolicyDao().components(it.policyId)
                        (CostingPolicyService.ratePerKg(components) * yogurtMade)
                            .setScale(0, RoundingMode.HALF_UP).longValueExact()
                    }
                    val totalYogurtCost = if (transferredMilkCost != null && conversionCost != null) {
                        Math.addExact(transferredMilkCost, conversionCost)
                    } else null
                    val reason = when {
                        policy == null -> CostCoverageCode.STALE_CONVERSION_POLICY
                        transferredMilkCost == null -> transferCoverage ?: CostCoverageCode.MISSING_BASIS
                        else -> null
                    }
                    val yogurtPool = pools.getOrPut(yogurt.productId) { CostPool() }
                    yogurtPool.add(yogurtMade, totalYogurtCost, reason)
                    val yieldPolicy = database.yieldPolicyDao().activeForDate(event.occurredAt)
                    persist(event, yogurt.productId, "production_output", "production_output", quantityIn = yogurtMade,
                        costIn = totalYogurtCost, policyId = policy?.policyId,
                        yieldPolicyId = yieldPolicy?.yieldPolicyId, assessmentId = assessment?.assessmentId)
                }
                SaleService.EVENT_TYPE_SALE -> {
                    val quantity = BigDecimal.valueOf(requireNotNull(event.saleQuantity) { "Sale ${event.eventId} has no quantity" })
                    val productId = resolveInventoryProduct(event)
                        ?: error("Sale ${event.eventId} has no product movement")
                    val pool = pools.getOrPut(productId) { CostPool.unavailable(CostCoverageCode.UNSUPPORTED_FLOW) }
                    val removalCoverage = pool.reason
                    val cost = pool.remove(quantity)
                    persist(event, productId, "sale", "sale", quantityOut = quantity, costOut = cost,
                        coverageOverride = if (cost == null) removalCoverage else null)
                }
                SpoilageService.EVENT_TYPE_SPOILAGE -> {
                    val detail = database.spoilageDao().findByEventId(event.eventId)
                        ?: error("Spoilage ${event.eventId} has no detail")
                    val quantity = BigDecimal.valueOf(detail.quantity)
                    val pool = pools.getOrPut(detail.productId) { CostPool.unavailable(CostCoverageCode.UNSUPPORTED_FLOW) }
                    val removalCoverage = pool.reason
                    val cost = pool.remove(quantity)
                    persist(event, detail.productId, "spoilage", "spoilage", quantityOut = quantity, costOut = cost,
                        coverageOverride = if (cost == null) removalCoverage else null)
                }
            }
        }

        val coverage = when (encounteredCoverage.size) {
            0 -> CostCoverageCode.COMPLETE
            1 -> encounteredCoverage.single()
            else -> CostCoverageCode.PARTIAL
        }
        val source = projectionDao.sourceWatermarks()
        val rebuiltAt = Instant.now().toString()
        projectionDao.saveState(
            CostProjectionStateEntity(
                engineVersion = ENGINE_VERSION,
                sourceEventCount = source.eventCount,
                sourceMaxEventId = source.maxEventId,
                sourceMaxPolicyId = source.maxPolicyId,
                sourceMaxBasisId = source.maxBasisId,
                sourceMaxYieldPolicyId = source.maxYieldPolicyId,
                sourceMaxAssessmentId = source.maxAssessmentId,
                rebuiltAt = rebuiltAt,
                coverageCode = coverage,
            )
        )
        return CostProjectionResult(movementCount, coverage, rebuiltAt)
    }

    private suspend fun resolveInventoryProduct(event: EventEntity): Long? {
        var current: EventEntity? = event
        while (current != null) {
            database.inventoryLogDao().findByEventId(current.eventId).firstOrNull()?.let { return it.productId }
            current = current.correctsEventId?.let { database.eventDao().findById(it) }
        }
        return null
    }

    private class CostPool(
        var quantity: BigDecimal = BigDecimal.ZERO,
        var costPaisa: Long? = 0L,
        var reason: String? = null,
    ) {
        fun add(incomingQuantity: BigDecimal, incomingCostPaisa: Long?, incomingReason: String?) {
            require(incomingQuantity.signum() > 0) { "Cost-pool addition must be positive" }
            val wasEmpty = quantity.signum() == 0
            quantity += incomingQuantity
            if (wasEmpty && incomingCostPaisa != null && incomingReason == null) {
                costPaisa = incomingCostPaisa
                reason = null
                return
            }
            if (costPaisa != null && incomingCostPaisa != null && reason == null && incomingReason == null) {
                costPaisa = Math.addExact(costPaisa!!, incomingCostPaisa)
            } else {
                costPaisa = null
                reason = combineReason(reason, incomingReason)
            }
        }

        fun remove(outgoingQuantity: BigDecimal): Long? {
            require(outgoingQuantity.signum() >= 0) { "Cost-pool removal cannot be negative" }
            if (outgoingQuantity.signum() == 0) return 0L
            require(quantity.compareTo(outgoingQuantity) >= 0) { "Cost replay would make inventory negative" }
            val released = costPaisa?.let { carrying ->
                if (quantity.compareTo(outgoingQuantity) == 0) carrying
                else (BigDecimal.valueOf(carrying) * outgoingQuantity)
                    .divide(quantity, 0, RoundingMode.HALF_UP).longValueExact()
            }
            quantity -= outgoingQuantity
            if (quantity.signum() == 0) {
                quantity = BigDecimal.ZERO
                costPaisa = 0L
                reason = null
            } else if (released != null) {
                costPaisa = Math.subtractExact(costPaisa!!, released)
            }
            return released
        }

        companion object {
            fun unavailable(reason: String) = CostPool(BigDecimal.ZERO, null, reason)

            private fun combineReason(first: String?, second: String?): String = when {
                first == null -> second ?: CostCoverageCode.PARTIAL
                second == null -> first
                first == second -> first
                else -> CostCoverageCode.PARTIAL
            }
        }
    }

    companion object {
        const val ENGINE_VERSION = 1
    }
}

data class ProductProfitability(
    val productId: Long,
    val productName: String,
    val quantitySold: BigDecimal,
    val salesRevenuePaisa: Long,
    val costOfProductsSoldPaisa: Long?,
    val profitBeforeShopExpensesPaisa: Long?,
    val grossMarginPercent: BigDecimal?,
    val spoilageCostPaisa: Long?,
    val abnormalLossPaisa: Long?,
    val coverageCode: String,
)

data class ProfitabilityReport(
    val startInclusive: String,
    val endExclusive: String,
    val products: List<ProductProfitability>,
    val deliverySurchargePaisa: Long,
    val recordedOperatingExpensesPaisa: Long,
    val recordedSalaryPaymentsPaisa: Long,
    val estimatedRecordedResultPaisa: Long?,
    val coverageCode: String,
    val reportingBasis: String = CostReportingScope.REPORTING_BASIS,
    val valuationScope: String = CostReportingScope.VALUATION_SCOPE,
)

class ProfitabilityReadService(private val database: MilkShopDatabase) {
    private val projection = InventoryCostProjectionService(database)

    suspend fun report(startInclusive: String, endExclusive: String): ProfitabilityReport {
        val start = LocalDate.parse(startInclusive)
        val end = LocalDate.parse(endExclusive)
        require(end > start) { "Report end must be after start" }
        projection.ensureCurrent()
        val movements = database.inventoryCostProjectionDao().movementsForPeriod(start.toString(), end.toString())
        val events = database.inventoryCostProjectionDao().effectiveEvents().associateBy { it.eventId }
        val products = database.productDao().all().associateBy { it.productId }
        val grouped = movements.groupBy { it.productId }
        val productRows = grouped.mapNotNull { (productId, rows) ->
            val saleRows = rows.filter { it.movementKind == "sale" }
            val spoilageRows = rows.filter { it.movementKind == "spoilage" }
            val abnormalRows = rows.filter { it.movementKind == "abnormal_loss" }
            if (saleRows.isEmpty() && spoilageRows.isEmpty() && abnormalRows.isEmpty()) return@mapNotNull null
            val quantitySold = saleRows.fold(BigDecimal.ZERO) { sum, row -> sum + BigDecimal(requireNotNull(row.quantityOut)) }
            val revenue = saleRows.sumLong { row -> moneyToPaisa(requireNotNull(events[row.sourceEventId]?.saleValue)) }
            val cogs = saleRows.sumNullable { it.costOutPaisa }
            val spoilage = spoilageRows.sumNullable { it.costOutPaisa }
            val abnormal = abnormalRows.sumNullable { it.costOutPaisa }
            val coverage = (saleRows + spoilageRows + abnormalRows).map { it.coverageCode }
                .filter { it != CostCoverageCode.COMPLETE }.distinct().let {
                when (it.size) { 0 -> CostCoverageCode.COMPLETE; 1 -> it.single(); else -> CostCoverageCode.PARTIAL }
            }
            val grossProfit = cogs?.let { Math.subtractExact(revenue, it) }
            ProductProfitability(
                productId = productId,
                productName = products[productId]?.name ?: "Product $productId",
                quantitySold = quantitySold,
                salesRevenuePaisa = revenue,
                costOfProductsSoldPaisa = cogs,
                profitBeforeShopExpensesPaisa = grossProfit,
                grossMarginPercent = grossProfit?.takeIf { revenue != 0L }?.let {
                    BigDecimal.valueOf(it).multiply(BigDecimal(100))
                        .divide(BigDecimal.valueOf(revenue), 4, RoundingMode.HALF_UP)
                },
                spoilageCostPaisa = spoilage,
                abnormalLossPaisa = abnormal,
                coverageCode = coverage,
            )
        }.sortedBy { it.productName }

        val period = database.analyticsReadDao().periodSummary(start.toString(), end.toString())
        val surcharge = events.values.asSequence()
            .filter { it.eventType == SaleService.EVENT_TYPE_SALE && it.occurredAt >= start.toString() && it.occurredAt < end.toString() }
            .asIterable().sumLong { moneyToPaisa(it.surchargeApplied) }
        val expenses = moneyToPaisa(period.operatingExpenses)
        val salaries = moneyToPaisa(period.salaryPayments)
        val allKnown = productRows.all {
            it.costOfProductsSoldPaisa != null && it.spoilageCostPaisa != null && it.abnormalLossPaisa != null
        }
        val result = if (allKnown) {
            val sales = productRows.sumLong { it.salesRevenuePaisa }
            val cogs = productRows.sumLong { requireNotNull(it.costOfProductsSoldPaisa) }
            val spoilage = productRows.sumLong { requireNotNull(it.spoilageCostPaisa) }
            val abnormal = productRows.sumLong { requireNotNull(it.abnormalLossPaisa) }
            Math.subtractExact(
                Math.addExact(sales, surcharge),
                Math.addExact(Math.addExact(cogs, spoilage), Math.addExact(abnormal, Math.addExact(expenses, salaries))),
            )
        } else null
        val coverage = productRows.map { it.coverageCode }.filter { it != CostCoverageCode.COMPLETE }.distinct().let {
            when (it.size) { 0 -> CostCoverageCode.COMPLETE; 1 -> it.single(); else -> CostCoverageCode.PARTIAL }
        }
        return ProfitabilityReport(
            startInclusive = start.toString(),
            endExclusive = end.toString(),
            products = productRows,
            deliverySurchargePaisa = surcharge,
            recordedOperatingExpensesPaisa = expenses,
            recordedSalaryPaymentsPaisa = salaries,
            estimatedRecordedResultPaisa = result,
            coverageCode = coverage,
        )
    }
}

private fun positive(value: BigDecimal, label: String): BigDecimal {
    require(value.signum() > 0) { "$label must be greater than zero" }
    return value
}

private fun nonNegative(value: BigDecimal, label: String): BigDecimal {
    require(value.signum() >= 0) { "$label cannot be negative" }
    return value
}

private fun decimal(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

private fun moneyToPaisa(value: Double): Long {
    require(value.isFinite()) { "Money value must be finite" }
    return BigDecimal.valueOf(value).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

private inline fun <T> Iterable<T>.sumLong(value: (T) -> Long): Long =
    fold(0L) { total, item -> Math.addExact(total, value(item)) }

private inline fun <T> Iterable<T>.sumNullable(value: (T) -> Long?): Long? {
    var total = 0L
    for (item in this) {
        val amount = value(item) ?: return null
        total = Math.addExact(total, amount)
    }
    return total
}
