package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.ProductProfitabilityDto
import com.saaf.milkshop.application.readmodels.ProfitabilityReportDto
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CostCoverageCode
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale
import com.saaf.milkshop.sync.BusinessClock

enum class NativeOwnerReportsCoverageKind {
    Complete,
    Incomplete,
    Empty,
}

enum class NativeOwnerReportsPreset {
    ThisMonth,
    LastMonth,
    Custom,
}

data class NativeOwnerReportsProductRow(
    val productId: Long,
    val productName: String,
    val salesRevenueLabel: String,
    val costLabel: String?,
    val profitLabel: String?,
    val coverageLabel: String,
)

data class NativeOwnerReportsSnapshot(
    val shopName: String,
    val businessDate: String,
    val postingOpen: Boolean,
    val preset: NativeOwnerReportsPreset,
    val periodLabel: String,
    val startInclusive: String,
    val endExclusive: String,
    val coverageKind: NativeOwnerReportsCoverageKind,
    val coverageLabel: String,
    val coverageDetail: String,
    val deliverySurchargeLabel: String,
    val operatingExpensesLabel: String,
    val salaryPaymentsLabel: String,
    val estimatedResultLabel: String?,
    val products: List<NativeOwnerReportsProductRow>,
    val customRangeDeferredNotice: String = "",
)

/**
 * Owner Month reports boundary: maps [ProfitabilityReportDto] to presentation
 * labels. Does not invent profitability formulas or call bridge JSON.
 */
class NativeOwnerReportsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
) {
    suspend fun load(
        sessionHandle: String,
        startInclusive: String,
        endExclusive: String,
        preset: NativeOwnerReportsPreset,
        today: LocalDate = BusinessClock.today(),
    ): NativeOwnerReportsSnapshot {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_FULL_FINANCIAL)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Month reports is only available to Owner."
        }
        val settings = reads.businessSettings(sessionHandle)
        val report = reads.profitabilityReport(sessionHandle, startInclusive, endExclusive)
        return mapReport(
            report = report,
            preset = preset,
            shopName = settings.store?.storeName?.takeIf { it.isNotBlank() } ?: "Saaf Milk",
            businessDate = today.toString(),
            postingOpen = !database.dailyCloseDao().isPostingClosed(today.toString()),
        )
    }

    fun rangeForPreset(preset: NativeOwnerReportsPreset, today: LocalDate = BusinessClock.today()): Pair<String, String> {
        val month = java.time.YearMonth.from(today)
        return when (preset) {
            NativeOwnerReportsPreset.ThisMonth ->
                month.atDay(1).toString() to month.plusMonths(1).atDay(1).toString()
            NativeOwnerReportsPreset.LastMonth -> {
                val last = month.minusMonths(1)
                last.atDay(1).toString() to month.atDay(1).toString()
            }
            NativeOwnerReportsPreset.Custom -> error("Custom ranges must be supplied explicitly")
        }
    }

    fun validateRange(startInclusive: String, endInclusive: String): Pair<String, String> {
        val start = runCatching { LocalDate.parse(startInclusive.trim()) }
            .getOrElse { throw IllegalArgumentException("Enter the start date as YYYY-MM-DD") }
        val end = runCatching { LocalDate.parse(endInclusive.trim()) }
            .getOrElse { throw IllegalArgumentException("Enter the end date as YYYY-MM-DD") }
        require(!end.isBefore(start)) { "End date must be on or after the start date" }
        require(!end.isAfter(start.plusYears(1))) { "Choose a range of one year or less" }
        return start.toString() to end.plusDays(1).toString()
    }

    private fun mapReport(
        report: ProfitabilityReportDto,
        preset: NativeOwnerReportsPreset,
        shopName: String,
        businessDate: String,
        postingOpen: Boolean,
    ): NativeOwnerReportsSnapshot {
        val coverage = coveragePresentation(report.coverageCode, report.products.isEmpty())
        return NativeOwnerReportsSnapshot(
            shopName = shopName,
            businessDate = businessDate,
            postingOpen = postingOpen,
            preset = preset,
            periodLabel = periodLabel(report.startInclusive, report.endExclusive),
            startInclusive = report.startInclusive,
            endExclusive = report.endExclusive,
            coverageKind = coverage.kind,
            coverageLabel = coverage.label,
            coverageDetail = coverage.detail,
            deliverySurchargeLabel = formatPaisa(report.deliverySurchargePaisa),
            operatingExpensesLabel = formatPaisa(report.recordedOperatingExpensesPaisa),
            salaryPaymentsLabel = formatPaisa(report.recordedSalaryPaymentsPaisa),
            estimatedResultLabel = report.estimatedRecordedResultPaisa?.let(::formatPaisa),
            products = report.products.map(::mapProduct),
        )
    }

    private fun mapProduct(product: ProductProfitabilityDto): NativeOwnerReportsProductRow {
        val productCoverage = coveragePresentation(product.coverageCode, emptyProducts = false)
        return NativeOwnerReportsProductRow(
            productId = product.productId,
            productName = product.productName,
            salesRevenueLabel = formatPaisa(product.salesRevenuePaisa),
            costLabel = product.costOfProductsSoldPaisa?.let(::formatPaisa),
            profitLabel = product.profitBeforeShopExpensesPaisa?.let(::formatPaisa),
            coverageLabel = productCoverage.label,
        )
    }

    private data class CoveragePresentation(
        val kind: NativeOwnerReportsCoverageKind,
        val label: String,
        val detail: String,
    )

    private fun coveragePresentation(code: String, emptyProducts: Boolean): CoveragePresentation {
        if (emptyProducts) {
            return CoveragePresentation(
                kind = NativeOwnerReportsCoverageKind.Empty,
                label = "No product sales in this period",
                detail = "No product sales were recorded in this period.",
            )
        }
        return when (code) {
            CostCoverageCode.COMPLETE -> CoveragePresentation(
                kind = NativeOwnerReportsCoverageKind.Complete,
                label = "Cost history complete",
                detail = "The app has saved cost information for the stock sold in this period.",
            )
            CostCoverageCode.MISSING_BASIS -> CoveragePresentation(
                kind = NativeOwnerReportsCoverageKind.Incomplete,
                label = "Starting cost needed",
                detail = "Some sold stock came from opening stock without a saved starting cost. " +
                    "Add that cost before relying on the result.",
            )
            CostCoverageCode.STALE_CONVERSION_POLICY -> CoveragePresentation(
                kind = NativeOwnerReportsCoverageKind.Incomplete,
                label = "Yogurt cost policy needs review",
                detail = "The saved yogurt-making cost is missing or out of date for part of this period.",
            )
            CostCoverageCode.UNSUPPORTED_FLOW -> CoveragePresentation(
                kind = NativeOwnerReportsCoverageKind.Incomplete,
                label = "Some stock cost is not supported yet",
                detail = "At least one recorded stock movement cannot yet be assigned a dependable " +
                    "cost by this app version.",
            )
            else -> CoveragePresentation(
                kind = NativeOwnerReportsCoverageKind.Incomplete,
                label = "Cost information incomplete",
                detail = "The app has a mixture of complete and incomplete cost information for this period.",
            )
        }
    }

    private fun periodLabel(startInclusive: String, endExclusive: String): String {
        val through = runCatching {
            LocalDate.parse(endExclusive).minusDays(1).toString()
        }.getOrDefault(endExclusive)
        return if (startInclusive == through) startInclusive else "$startInclusive through $through"
    }

    private fun formatPaisa(paisa: Long): String {
        val pkr = BigDecimal.valueOf(paisa).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "PKR %,.2f", pkr)
    }
}
