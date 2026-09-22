package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.CostingPolicyComponentEntity
import com.saaf.milkshop.data.CostingPolicyVersionEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CostingPolicyInput
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

/**
 * Owner Costing & plans boundary: read approved policy versions + components
 * and approve new/superseding versions via [CostingPolicyService]. Does not
 * invent rates or call bridge JSON.
 */
class NativeOwnerCostingPlansFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val policies: CostingPolicyService = CostingPolicyService(database, security),
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

    /**
     * Full passphrase re-authentication for costing policy approve.
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

    private fun formatPaisaPerKg(rate: BigDecimal): String {
        val scaled = rate.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return "$scaled paisa/kg"
    }

    private fun formatPaisa(paisa: Long): String {
        val pkr = BigDecimal.valueOf(paisa).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "PKR %,.2f", pkr)
    }
}
