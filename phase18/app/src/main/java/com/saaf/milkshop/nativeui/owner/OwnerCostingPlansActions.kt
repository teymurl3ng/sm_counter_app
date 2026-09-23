package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.YieldPolicyInput
import java.time.LocalDate

/**
 * Bundled Costing & plans actions so host More screens stay under the JVM
 * method-size limit when wiring [NativeOwnerMoreViewModel].
 */
data class OwnerCostingPlansActions(
    val onRetry: () -> Unit = {},
    val onSectionChange: (NativeOwnerCostingSection) -> Unit = {},
    val onOpenPolicy: (Long) -> Unit = {},
    val onBackFromDetail: () -> Unit = {},
    val onOpenApprove: () -> Unit = {},
    val onOpenApproveFromDetail: () -> Unit = {},
    val onBackFromApprove: () -> Unit = {},
    val onApprovePolicy: (CostingPolicyInput) -> Unit = {},
    val onOpenBudgetEditor: () -> Unit = {},
    val onBackFromBudgetEditor: () -> Unit = {},
    val onSaveBudget: (BudgetPlanInput) -> Unit = {},
    val onCopyPreviousBudget: (String, LocalDate, LocalDate) -> Unit = { _, _, _ -> },
    val onOpenCollectionsEditor: () -> Unit = {},
    val onBackFromCollectionsEditor: () -> Unit = {},
    val onSaveCollectionsTarget: (LocalDate, Double) -> Unit = { _, _ -> },
    val onOpenOpeningEditor: (Long) -> Unit = {},
    val onBackFromOpeningEditor: () -> Unit = {},
    val onSaveOpeningCost: (OpeningCostBasisInput) -> Unit = {},
    val onOpenYieldApprove: () -> Unit = {},
    val onBackFromYieldApprove: () -> Unit = {},
    val onApproveYieldPolicy: (YieldPolicyInput) -> Unit = {},
    val onSubmitReauth: (CharArray) -> Unit = { it.fill('\u0000') },
    val onCancelReauth: () -> Unit = {},
)

fun NativeOwnerMoreViewModel.costingPlansActions(): OwnerCostingPlansActions =
    OwnerCostingPlansActions(
        onRetry = ::refreshCostingPlans,
        onSectionChange = ::setCostingSection,
        onOpenPolicy = ::openCostingPolicyDetail,
        onBackFromDetail = ::clearCostingPolicyDetail,
        onOpenApprove = ::openCostingApprove,
        onOpenApproveFromDetail = ::openCostingApproveFromDetail,
        onBackFromApprove = ::clearCostingApprove,
        onApprovePolicy = ::approveCostingPolicy,
        onOpenBudgetEditor = ::openCostingBudgetEditor,
        onBackFromBudgetEditor = ::clearCostingBudgetEditor,
        onSaveBudget = ::upsertCostingBudget,
        onCopyPreviousBudget = ::copyPreviousCostingBudget,
        onOpenCollectionsEditor = ::openCostingCollectionsEditor,
        onBackFromCollectionsEditor = ::clearCostingCollectionsEditor,
        onSaveCollectionsTarget = ::upsertCostingCollectionsTarget,
        onOpenOpeningEditor = ::openCostingOpeningEditor,
        onBackFromOpeningEditor = ::clearCostingOpeningEditor,
        onSaveOpeningCost = ::saveCostingOpeningCost,
        onOpenYieldApprove = ::openCostingYieldApprove,
        onBackFromYieldApprove = ::clearCostingYieldApprove,
        onApproveYieldPolicy = ::approveCostingYieldPolicy,
        onSubmitReauth = ::submitCostingReauth,
        onCancelReauth = ::cancelCostingReauth,
    )
