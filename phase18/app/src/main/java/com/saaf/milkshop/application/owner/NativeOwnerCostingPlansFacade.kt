package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.CostingPolicyComponentEntity
import com.saaf.milkshop.data.CostingPolicyVersionEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.UserSecurityService
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

/**
 * Owner Costing & plans boundary: read approved policy versions + components
 * from [CostingPolicyDao] / [CostingPolicyService]. Does not invent rates or
 * call bridge JSON. Approve/edit stays out of this facade.
 */
class NativeOwnerCostingPlansFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val policies: CostingPolicyService = CostingPolicyService(database, security),
) {
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
                .map { component ->
                    NativeOwnerCostingComponentRow(
                        componentCode = component.componentCode,
                        sourceAmountLabel = formatPaisa(component.sourceAmountPaisa),
                        basisQuantityKg = component.basisQuantityKg,
                    )
                },
        )
    }

    private suspend fun requireOwnerBusinessSettings(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Costing & plans is only available to Owner."
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

    private fun formatPaisaPerKg(rate: BigDecimal): String {
        val scaled = rate.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return "$scaled paisa/kg"
    }

    private fun formatPaisa(paisa: Long): String {
        val pkr = BigDecimal.valueOf(paisa).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "PKR %,.2f", pkr)
    }
}
