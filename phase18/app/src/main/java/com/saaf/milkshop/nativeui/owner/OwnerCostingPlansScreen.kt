package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import com.saaf.milkshop.nativeui.common.NativeInformationSection
import com.saaf.milkshop.nativeui.common.NativeInformationFact
import com.saaf.milkshop.nativeui.common.NativeInformationHeading
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.theme.nativeMetricTextStyle
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeVisualColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerBudgetRow
import com.saaf.milkshop.application.owner.NativeOwnerCollectionsTargetRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingComponentRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyDetail
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerOpeningCostRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerYieldBatchRow
import com.saaf.milkshop.application.owner.NativeOwnerYieldPolicyRow
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.ConversionCostComponentInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.ProductionLossAssessmentInput
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Owner Costing & plans — policy list, planning budgets/targets, yield approve,
 * and costing policy approve/supersede under OwnerScaffold. Nested from Owner More;
 * never mounts foreign chrome.
 */
private data class OwnerCostingPlansScreenSnapshot(
    val identity: NativeSignedInIdentity,
    val context: NativeOwnerMoreContext,
    val policies: List<NativeOwnerCostingPolicyRow>,
    val policiesLoading: Boolean,
    val policyDetail: NativeOwnerCostingPolicyDetail?,
    val policiesError: String?,
    val currentMonthSummary: String?,
    val section: NativeOwnerCostingSection,
    val budgets: List<NativeOwnerBudgetRow>,
    val targets: List<NativeOwnerCollectionsTargetRow>,
    val budgetCategories: List<String>,
    val defaultMilkWeekStart: String,
    val defaultOperatingMonthStart: String,
    val budgetEditorOpen: Boolean,
    val budgetSaving: Boolean,
    val collectionsEditorOpen: Boolean,
    val collectionsSaving: Boolean,
    val openingCosts: List<NativeOwnerOpeningCostRow>,
    val openingSummary: String?,
    val openingBasisKinds: List<String>,
    val openingEditor: NativeOwnerOpeningCostRow?,
    val openingSaving: Boolean,
    val yieldPolicies: List<NativeOwnerYieldPolicyRow>,
    val yieldBatches: List<NativeOwnerYieldBatchRow>,
    val yieldActivePolicyId: Long?,
    val yieldSummary: String?,
    val yieldApproveOpen: Boolean,
    val yieldApproving: Boolean,
    val lossSaving: Boolean,
    val lossClassifications: List<String>,
    val lossReasons: List<String>,
    val approveOpen: Boolean,
    val approveSeed: NativeOwnerCostingPolicyDetail?,
    val approving: Boolean,
    val reauthOpen: Boolean,
    val reauthBusy: Boolean,
    val reviewReasons: List<String>,
    val componentCodes: List<String>,
    val showHeader: Boolean,
)

@Composable
fun OwnerCostingPlansScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    policies: List<NativeOwnerCostingPolicyRow>,
    policiesLoading: Boolean,
    policyDetail: NativeOwnerCostingPolicyDetail?,
    policiesError: String?,
    currentMonthSummary: String?,
    section: NativeOwnerCostingSection = NativeOwnerCostingSection.Policies,
    budgets: List<NativeOwnerBudgetRow> = emptyList(),
    targets: List<NativeOwnerCollectionsTargetRow> = emptyList(),
    budgetCategories: List<String> = emptyList(),
    defaultMilkWeekStart: String = "",
    defaultOperatingMonthStart: String = "",
    budgetEditorOpen: Boolean = false,
    budgetSaving: Boolean = false,
    collectionsEditorOpen: Boolean = false,
    collectionsSaving: Boolean = false,
    openingCosts: List<NativeOwnerOpeningCostRow> = emptyList(),
    openingSummary: String? = null,
    openingBasisKinds: List<String> = emptyList(),
    openingEditor: NativeOwnerOpeningCostRow? = null,
    openingSaving: Boolean = false,
    yieldPolicies: List<NativeOwnerYieldPolicyRow> = emptyList(),
    yieldBatches: List<NativeOwnerYieldBatchRow> = emptyList(),
    yieldActivePolicyId: Long? = null,
    yieldSummary: String? = null,
    yieldApproveOpen: Boolean = false,
    yieldApproving: Boolean = false,
    lossSaving: Boolean = false,
    lossClassifications: List<String> = emptyList(),
    lossReasons: List<String> = emptyList(),
    approveOpen: Boolean = false,
    approveSeed: NativeOwnerCostingPolicyDetail? = null,
    approving: Boolean = false,
    reauthOpen: Boolean = false,
    reauthBusy: Boolean = false,
    reviewReasons: List<String> = emptyList(),
    componentCodes: List<String> = emptyList(),
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onSectionChange: (NativeOwnerCostingSection) -> Unit = {},
    onOpenPolicy: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenApprove: () -> Unit,
    onOpenApproveFromDetail: () -> Unit,
    onBackFromApprove: () -> Unit,
    onApprovePolicy: (CostingPolicyInput) -> Unit,
    onOpenBudgetEditor: () -> Unit = {},
    onBackFromBudgetEditor: () -> Unit = {},
    onSaveBudget: (BudgetPlanInput) -> Unit = {},
    onCopyPreviousBudget: (String, LocalDate, LocalDate) -> Unit = { _, _, _ -> },
    onOpenCollectionsEditor: () -> Unit = {},
    onBackFromCollectionsEditor: () -> Unit = {},
    onSaveCollectionsTarget: (LocalDate, Double) -> Unit = { _, _ -> },
    onOpenOpeningEditor: (Long) -> Unit = {},
    onOpenShopSettings: () -> Unit = {},
    onBackFromOpeningEditor: () -> Unit = {},
    onSaveOpeningCost: (OpeningCostBasisInput) -> Unit = {},
    onOpenYieldApprove: () -> Unit = {},
    onBackFromYieldApprove: () -> Unit = {},
    onApproveYieldPolicy: (YieldPolicyInput) -> Unit = {},
    onRecordProductionLoss: (ProductionLossAssessmentInput) -> Unit = {},
    onSubmitReauth: (CharArray) -> Unit,
    onCancelReauth: () -> Unit,
    periodEndExclusive: (String, LocalDate) -> LocalDate = { category, start ->
        if (category == BUDGET_CATEGORY_MILK_PURCHASES) start.plusDays(7) else start.plusMonths(1)
    },
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val paneSnapshot = OwnerCostingPlansScreenSnapshot(
        identity = identity,
        context = context,
        policies = policies,
        policiesLoading = policiesLoading,
        policyDetail = policyDetail,
        policiesError = policiesError,
        currentMonthSummary = currentMonthSummary,
        section = section,
        budgets = budgets,
        targets = targets,
        budgetCategories = budgetCategories,
        defaultMilkWeekStart = defaultMilkWeekStart,
        defaultOperatingMonthStart = defaultOperatingMonthStart,
        budgetEditorOpen = budgetEditorOpen,
        budgetSaving = budgetSaving,
        collectionsEditorOpen = collectionsEditorOpen,
        collectionsSaving = collectionsSaving,
        openingCosts = openingCosts,
        openingSummary = openingSummary,
        openingBasisKinds = openingBasisKinds,
        openingEditor = openingEditor,
        openingSaving = openingSaving,
        yieldPolicies = yieldPolicies,
        yieldBatches = yieldBatches,
        yieldActivePolicyId = yieldActivePolicyId,
        yieldSummary = yieldSummary,
        yieldApproveOpen = yieldApproveOpen,
        yieldApproving = yieldApproving,
        lossSaving = lossSaving,
        lossClassifications = lossClassifications,
        lossReasons = lossReasons,
        approveOpen = approveOpen,
        approveSeed = approveSeed,
        approving = approving,
        reauthOpen = reauthOpen,
        reauthBusy = reauthBusy,
        reviewReasons = reviewReasons,
        componentCodes = componentCodes,
        showHeader = showHeader,
    )
    com.saaf.milkshop.nativeui.common.NativeSectionTransition(
        targetState = paneSnapshot,
        contentKey = { pane -> when {
            pane.openingEditor != null -> "opening"
            pane.budgetEditorOpen -> "budget"
            pane.collectionsEditorOpen -> "collections"
            pane.policyDetail != null -> "policy-detail"
            else -> "home"
        } },
        modifier = modifier,
    ) { pane ->
        OwnerCostingPlansScreenPane(
            snackbarHostState = snackbarHostState,
            identity = pane.identity,
            context = pane.context,
            policies = pane.policies,
            policiesLoading = pane.policiesLoading,
            policyDetail = pane.policyDetail,
            policiesError = pane.policiesError,
            currentMonthSummary = pane.currentMonthSummary,
            section = pane.section,
            budgets = pane.budgets,
            targets = pane.targets,
            budgetCategories = pane.budgetCategories,
            defaultMilkWeekStart = pane.defaultMilkWeekStart,
            defaultOperatingMonthStart = pane.defaultOperatingMonthStart,
            budgetEditorOpen = pane.budgetEditorOpen,
            budgetSaving = pane.budgetSaving,
            collectionsEditorOpen = pane.collectionsEditorOpen,
            collectionsSaving = pane.collectionsSaving,
            openingCosts = pane.openingCosts,
            openingSummary = pane.openingSummary,
            openingBasisKinds = pane.openingBasisKinds,
            openingEditor = pane.openingEditor,
            openingSaving = pane.openingSaving,
            yieldPolicies = pane.yieldPolicies,
            yieldBatches = pane.yieldBatches,
            yieldActivePolicyId = pane.yieldActivePolicyId,
            yieldSummary = pane.yieldSummary,
            yieldApproveOpen = pane.yieldApproveOpen,
            yieldApproving = pane.yieldApproving,
            lossSaving = pane.lossSaving,
            lossClassifications = pane.lossClassifications,
            lossReasons = pane.lossReasons,
            approveOpen = pane.approveOpen,
            approveSeed = pane.approveSeed,
            approving = pane.approving,
            reauthOpen = pane.reauthOpen,
            reauthBusy = pane.reauthBusy,
            reviewReasons = pane.reviewReasons,
            componentCodes = pane.componentCodes,
            onBackToMenu = onBackToMenu,
            onRetry = onRetry,
            onSectionChange = onSectionChange,
            onOpenPolicy = onOpenPolicy,
            onBackFromDetail = onBackFromDetail,
            onOpenApprove = onOpenApprove,
            onOpenApproveFromDetail = onOpenApproveFromDetail,
            onBackFromApprove = onBackFromApprove,
            onApprovePolicy = onApprovePolicy,
            onOpenBudgetEditor = onOpenBudgetEditor,
            onBackFromBudgetEditor = onBackFromBudgetEditor,
            onSaveBudget = onSaveBudget,
            onCopyPreviousBudget = onCopyPreviousBudget,
            onOpenCollectionsEditor = onOpenCollectionsEditor,
            onBackFromCollectionsEditor = onBackFromCollectionsEditor,
            onSaveCollectionsTarget = onSaveCollectionsTarget,
            onOpenOpeningEditor = onOpenOpeningEditor,
            onOpenShopSettings = onOpenShopSettings,
            onBackFromOpeningEditor = onBackFromOpeningEditor,
            onSaveOpeningCost = onSaveOpeningCost,
            onOpenYieldApprove = onOpenYieldApprove,
            onBackFromYieldApprove = onBackFromYieldApprove,
            onApproveYieldPolicy = onApproveYieldPolicy,
            onRecordProductionLoss = onRecordProductionLoss,
            onSubmitReauth = onSubmitReauth,
            onCancelReauth = onCancelReauth,
            periodEndExclusive = periodEndExclusive,
            hostedScaffold = hostedScaffold,
            showHeader = pane.showHeader,
            modifier = modifier,
        )
    }
}

@Composable
private fun OwnerCostingPlansScreenPane(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    policies: List<NativeOwnerCostingPolicyRow>,
    policiesLoading: Boolean,
    policyDetail: NativeOwnerCostingPolicyDetail?,
    policiesError: String?,
    currentMonthSummary: String?,
    section: NativeOwnerCostingSection = NativeOwnerCostingSection.Policies,
    budgets: List<NativeOwnerBudgetRow> = emptyList(),
    targets: List<NativeOwnerCollectionsTargetRow> = emptyList(),
    budgetCategories: List<String> = emptyList(),
    defaultMilkWeekStart: String = "",
    defaultOperatingMonthStart: String = "",
    budgetEditorOpen: Boolean = false,
    budgetSaving: Boolean = false,
    collectionsEditorOpen: Boolean = false,
    collectionsSaving: Boolean = false,
    openingCosts: List<NativeOwnerOpeningCostRow> = emptyList(),
    openingSummary: String? = null,
    openingBasisKinds: List<String> = emptyList(),
    openingEditor: NativeOwnerOpeningCostRow? = null,
    openingSaving: Boolean = false,
    yieldPolicies: List<NativeOwnerYieldPolicyRow> = emptyList(),
    yieldBatches: List<NativeOwnerYieldBatchRow> = emptyList(),
    yieldActivePolicyId: Long? = null,
    yieldSummary: String? = null,
    yieldApproveOpen: Boolean = false,
    yieldApproving: Boolean = false,
    lossSaving: Boolean = false,
    lossClassifications: List<String> = emptyList(),
    lossReasons: List<String> = emptyList(),
    approveOpen: Boolean = false,
    approveSeed: NativeOwnerCostingPolicyDetail? = null,
    approving: Boolean = false,
    reauthOpen: Boolean = false,
    reauthBusy: Boolean = false,
    reviewReasons: List<String> = emptyList(),
    componentCodes: List<String> = emptyList(),
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onSectionChange: (NativeOwnerCostingSection) -> Unit = {},
    onOpenPolicy: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenApprove: () -> Unit,
    onOpenApproveFromDetail: () -> Unit,
    onBackFromApprove: () -> Unit,
    onApprovePolicy: (CostingPolicyInput) -> Unit,
    onOpenBudgetEditor: () -> Unit = {},
    onBackFromBudgetEditor: () -> Unit = {},
    onSaveBudget: (BudgetPlanInput) -> Unit = {},
    onCopyPreviousBudget: (String, LocalDate, LocalDate) -> Unit = { _, _, _ -> },
    onOpenCollectionsEditor: () -> Unit = {},
    onBackFromCollectionsEditor: () -> Unit = {},
    onSaveCollectionsTarget: (LocalDate, Double) -> Unit = { _, _ -> },
    onOpenOpeningEditor: (Long) -> Unit = {},
    onOpenShopSettings: () -> Unit = {},
    onBackFromOpeningEditor: () -> Unit = {},
    onSaveOpeningCost: (OpeningCostBasisInput) -> Unit = {},
    onOpenYieldApprove: () -> Unit = {},
    onBackFromYieldApprove: () -> Unit = {},
    onApproveYieldPolicy: (YieldPolicyInput) -> Unit = {},
    onRecordProductionLoss: (ProductionLossAssessmentInput) -> Unit = {},
    onSubmitReauth: (CharArray) -> Unit,
    onCancelReauth: () -> Unit,
    periodEndExclusive: (String, LocalDate) -> LocalDate = { category, start ->
        if (category == BUDGET_CATEGORY_MILK_PURCHASES) start.plusDays(7) else start.plusMonths(1)
    },
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    when {
        reauthOpen -> {
            OwnerCostingPlansReauthDialog(
                busy = reauthBusy,
                error = policiesError,
                onConfirm = onSubmitReauth,
                onCancel = onCancelReauth,
            )
            if (approveOpen) {
                OwnerCostingPlansApproveScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    context = context,
                    policies = policies,
                    seed = approveSeed,
                    reviewReasons = reviewReasons,
                    componentCodes = componentCodes,
                    saving = approving || reauthBusy,
                    error = null,
                    onBack = onBackFromApprove,
                    onSave = onApprovePolicy,
                    hostedScaffold = hostedScaffold,
                    modifier = modifier,
                )
            } else if (yieldApproveOpen) {
                OwnerCostingYieldApproveScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    context = context,
                    batches = yieldBatches,
                    activePolicyId = yieldActivePolicyId,
                    saving = yieldApproving || reauthBusy,
                    error = null,
                    onBack = onBackFromYieldApprove,
                    onSave = onApproveYieldPolicy,
                    hostedScaffold = hostedScaffold,
                    modifier = modifier,
                )
            } else if (openingEditor != null) {
                OwnerOpeningCostBasisEditorScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    context = context,
                    row = openingEditor,
                    basisKinds = openingBasisKinds,
                    saving = openingSaving || reauthBusy,
                    error = null,
                    onBack = onBackFromOpeningEditor,
                    onSave = onSaveOpeningCost,
                    hostedScaffold = hostedScaffold,
                    modifier = modifier,
                )
            }
        }
        approveOpen -> {
            OwnerCostingPlansApproveScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                policies = policies,
                seed = approveSeed,
                reviewReasons = reviewReasons,
                componentCodes = componentCodes,
                saving = approving,
                error = policiesError,
                onBack = onBackFromApprove,
                onSave = onApprovePolicy,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
        }
        yieldApproveOpen -> {
            OwnerCostingYieldApproveScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                batches = yieldBatches,
                activePolicyId = yieldActivePolicyId,
                saving = yieldApproving,
                error = policiesError,
                onBack = onBackFromYieldApprove,
                onSave = onApproveYieldPolicy,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
        }
        budgetEditorOpen -> {
            OwnerCostingBudgetEditorScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                categories = budgetCategories,
                defaultMilkWeekStart = defaultMilkWeekStart,
                defaultOperatingMonthStart = defaultOperatingMonthStart,
                periodEndExclusive = periodEndExclusive,
                saving = budgetSaving,
                error = policiesError,
                onBack = onBackFromBudgetEditor,
                onSave = onSaveBudget,
                onCopyPrevious = onCopyPreviousBudget,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
        }
        collectionsEditorOpen -> {
            OwnerCostingCollectionsEditorScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                defaultWeekStart = defaultMilkWeekStart,
                saving = collectionsSaving,
                error = policiesError,
                onBack = onBackFromCollectionsEditor,
                onSave = onSaveCollectionsTarget,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
        }
        openingEditor != null -> {
            OwnerOpeningCostBasisEditorScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                row = openingEditor,
                basisKinds = openingBasisKinds,
                saving = openingSaving,
                error = policiesError,
                onBack = onBackFromOpeningEditor,
                onSave = onSaveOpeningCost,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
        }
        policyDetail != null -> {
            OwnerCostingPlansDetailScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                detail = policyDetail,
                onBack = onBackFromDetail,
                onOpenApproveFromDetail = onOpenApproveFromDetail,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
        }
        else -> {
            OwnerCostingPlansListBody(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                section = section,
                policies = policies,
                policiesLoading = policiesLoading,
                policiesError = policiesError,
                currentMonthSummary = currentMonthSummary,
                budgets = budgets,
                targets = targets,
                openingCosts = openingCosts,
                openingSummary = openingSummary,
                yieldPolicies = yieldPolicies,
                yieldBatches = yieldBatches,
                yieldSummary = yieldSummary,
                lossSaving = lossSaving,
                lossClassifications = lossClassifications,
                lossReasons = lossReasons,
                onBackToMenu = onBackToMenu,
                onRetry = onRetry,
                onSectionChange = onSectionChange,
                onOpenPolicy = onOpenPolicy,
                onOpenApprove = onOpenApprove,
                onOpenBudgetEditor = onOpenBudgetEditor,
                onOpenCollectionsEditor = onOpenCollectionsEditor,
                onOpenOpeningEditor = onOpenOpeningEditor,
                onOpenShopSettings = onOpenShopSettings,
                onOpenYieldApprove = onOpenYieldApprove,
                onRecordProductionLoss = onRecordProductionLoss,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun OwnerCostingPlansListBody(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    section: NativeOwnerCostingSection,
    policies: List<NativeOwnerCostingPolicyRow>,
    policiesLoading: Boolean,
    policiesError: String?,
    currentMonthSummary: String?,
    budgets: List<NativeOwnerBudgetRow>,
    targets: List<NativeOwnerCollectionsTargetRow>,
    openingCosts: List<NativeOwnerOpeningCostRow>,
    openingSummary: String?,
    yieldPolicies: List<NativeOwnerYieldPolicyRow>,
    yieldBatches: List<NativeOwnerYieldBatchRow>,
    yieldSummary: String?,
    lossSaving: Boolean,
    lossClassifications: List<String>,
    lossReasons: List<String>,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onSectionChange: (NativeOwnerCostingSection) -> Unit,
    onOpenPolicy: (Long) -> Unit,
    onOpenApprove: () -> Unit,
    onOpenBudgetEditor: () -> Unit,
    onOpenCollectionsEditor: () -> Unit,
    onOpenOpeningEditor: (Long) -> Unit,
    onOpenShopSettings: () -> Unit,
    onOpenYieldApprove: () -> Unit,
    onRecordProductionLoss: (ProductionLossAssessmentInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBackToMenu)

    @Composable
    fun HubHeader() {
        CashierScreenHeader(
            title = "Costing & plans",
            displayName = context.shopName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = context.businessDate,
            postingOpen = context.postingOpen,
        )
        OwnerCostingPlansSectionTabs(
            section = section,
            onSectionChange = onSectionChange,
        )
        val (label, action, tag) = when (section) {
            NativeOwnerCostingSection.Policies -> Triple("Approve policy", onOpenApprove, NativeUiTags.OwnerCostingPlansApprove)
            NativeOwnerCostingSection.Plans -> Triple("New plan", onOpenBudgetEditor, NativeUiTags.OwnerCostingBudgetOpen)
            NativeOwnerCostingSection.Opening -> Triple("Shop settings", onOpenShopSettings, null)
            NativeOwnerCostingSection.Yield -> Triple("Approve baseline", onOpenYieldApprove, NativeUiTags.OwnerCostingYieldApprove)
        }
        com.saaf.milkshop.nativeui.common.NativePaneActionBar(
            label = label,
            onClick = action,
            actionTag = tag,
            enabled = !(policiesLoading && policies.isEmpty() && budgets.isEmpty() && yieldPolicies.isEmpty()) &&
                !(policiesError != null && policies.isEmpty() && budgets.isEmpty() && section == NativeOwnerCostingSection.Policies),
        )
    }

    when {
        policiesLoading && policies.isEmpty() && policiesError == null &&
            budgets.isEmpty() && yieldPolicies.isEmpty() -> {
            hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag(NativeUiTags.OwnerCostingPlansRoot),
                ) {
                    HubHeader()
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerCostingPlansLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        NativeColdLoadingPane(label = "Loading costing plans…")
                    }
                }
            }
        }
        policiesError != null && policies.isEmpty() && budgets.isEmpty() &&
            section == NativeOwnerCostingSection.Policies -> {
            hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag(NativeUiTags.OwnerCostingPlansRoot),
                ) {
                    HubHeader()
                    NativeLoadFailurePane(message = policiesError, onRetry = onRetry,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansError),
                        retryTestTag = NativeUiTags.OwnerCostingPlansRetry)
                }
            }
        }
        section == NativeOwnerCostingSection.Plans -> {
            OwnerCostingPlansBody(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                budgets = budgets,
                targets = targets,
                error = policiesError,
                onOpenCollectionsEditor = onOpenCollectionsEditor,
                hostedScaffold = hostedScaffold,
                modifier = modifier.testTag(NativeUiTags.OwnerCostingPlansRoot),
                header = {
                    HubHeader()
                },
            )
        }
        section == NativeOwnerCostingSection.Opening -> {
            OwnerOpeningCostBasisBody(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                rows = openingCosts,
                summary = openingSummary,
                error = policiesError,
                onOpenEditor = onOpenOpeningEditor,
                onOpenShopSettings = onOpenShopSettings,
                hostedScaffold = hostedScaffold,
                modifier = modifier.testTag(NativeUiTags.OwnerCostingPlansRoot),
                header = { HubHeader() },
            )
        }
        section == NativeOwnerCostingSection.Yield -> {
            OwnerCostingYieldBody(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                policies = yieldPolicies,
                batches = yieldBatches,
                summary = yieldSummary,
                error = policiesError,
                savingAssessment = lossSaving,
                classifications = lossClassifications,
                reasons = lossReasons,
                onSaveAssessment = onRecordProductionLoss,
                hostedScaffold = hostedScaffold,
                modifier = modifier.testTag(NativeUiTags.OwnerCostingPlansRoot),
                header = {
                    HubHeader()
                },
            )
        }
        else -> {
            hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag(NativeUiTags.OwnerCostingPlansRoot),
                ) {
                    HubHeader()
                    currentMonthSummary?.let { summary ->
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag(NativeUiTags.OwnerCostingPlansSummary),
                        )
                    }
                    policiesError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .testTag(NativeUiTags.OwnerCostingPlansError),
                        )
                    }
                    if (policies.isEmpty()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .testTag(NativeUiTags.OwnerCostingPlansEmpty),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No costing policies yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerCostingPlansList),
                        ) {
                            items(policies, key = { it.policyId }) { policy ->
                                OwnerCostingPolicyRow(
                                    policy = policy,
                                    onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenPolicy(policy.policyId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerCostingPlansDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerCostingPolicyDetail,
    onBack: () -> Unit,
    onOpenApproveFromDetail: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val row = detail.row
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCostingPlansDetail),
        ) {
            CashierScreenHeader(
                title = "Costing & plans",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    row.calendarMonth,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansDetailMonth),
                )
                NativeInformationSection {
                    OwnerCostingFact("Status", row.statusLabel)
                    NativeInformationFact("Conversion cost", row.conversionCostLabel, emphasized = true)
                    OwnerCostingFact("Review reason", row.reviewReason)
                    OwnerCostingFact("Normal capacity (kg)", detail.normalCapacityKg)
                    OwnerCostingFact("Approved at", detail.approvedAt)
                    detail.notes?.takeIf { it.isNotBlank() }?.let {
                        NativeInformationFact("Notes", it, prose = true)
                    }
                }
                NativeInformationHeading("Components")
                if (detail.components.isEmpty()) {
                    Text(
                        "No components on this policy snapshot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    NativeInformationSection {
                        detail.components.forEach { component ->
                            OwnerCostingComponentFact(component)
                        }
                    }
                }
                if (row.statusLabel == "Active") {
                    Button(
                        onClick = hapticAction(onOpenApproveFromDetail),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCostingPlansDetailSupersede),
                    ) { Text("Approve corrected policy") }
                } else {
                    Text(
                        "This version is superseded. Approve a new version against the Active policy for this month — in-place edit is not available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansDetailDeferredEdit),
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerCostingPlansApproveScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    policies: List<NativeOwnerCostingPolicyRow>,
    seed: NativeOwnerCostingPolicyDetail?,
    reviewReasons: List<String>,
    componentCodes: List<String>,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (CostingPolicyInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val formKey = seed?.row?.policyId?.let { "supersede-$it" } ?: "new-approve"
    val businessMonth = remember(context.businessDate) {
        YearMonth.from(LocalDate.parse(context.businessDate)).toString()
    }
    val codes = remember(componentCodes) {
        componentCodes.ifEmpty {
            listOf(
                "culture",
                "packaging",
                "variable_energy",
                "direct_labor",
                CostingPolicyService.COMPONENT_FIXED_OVERHEAD,
            )
        }
    }
    val reasons = remember(reviewReasons) {
        reviewReasons.ifEmpty { CostingPolicyService.REVIEW_REASONS.sorted() }
    }

    var calendarMonth by rememberSaveable(formKey) {
        mutableStateOf(seed?.row?.calendarMonth ?: businessMonth)
    }
    var capacityText by rememberSaveable(formKey) {
        mutableStateOf(seed?.normalCapacityKg ?: "100")
    }
    var reviewReason by rememberSaveable(formKey) {
        mutableStateOf(seed?.row?.reviewReason ?: "monthly")
    }
    var notes by rememberSaveable(formKey) {
        mutableStateOf(seed?.notes.orEmpty())
    }
    var draftTouched by rememberSaveable(formKey) { mutableStateOf(false) }

    val seedAmounts = remember(seed) {
        seed?.components?.associate { it.componentCode to paisaToRupeesInput(it.sourceAmountPaisa) }
            .orEmpty()
    }
    val seedBases = remember(seed) {
        seed?.components?.associate { it.componentCode to it.basisQuantityKg }.orEmpty()
    }

    var amountTexts by remember(formKey) {
        mutableStateOf(
            codes.associateWith { code ->
                seedAmounts[code] ?: ""
            },
        )
    }
    var basisTexts by remember(formKey) {
        mutableStateOf(
            codes.associateWith { code ->
                when {
                    seedBases.containsKey(code) -> seedBases.getValue(code)
                    code == CostingPolicyService.COMPONENT_FIXED_OVERHEAD -> capacityText
                    else -> "1"
                }
            },
        )
    }

    fun setCapacity(value: String) {
        capacityText = value
        draftTouched = true
        basisTexts = basisTexts + (CostingPolicyService.COMPONENT_FIXED_OVERHEAD to value)
    }

    val activeForMonth = policies.firstOrNull {
        it.calendarMonth == calendarMonth.trim() && it.statusLabel == "Active"
    }
    val supersedesPolicyId = when {
        seed != null -> seed.row.policyId
        else -> activeForMonth?.policyId
    }

    val capacity = capacityText.trim().toBigDecimalOrNull()
    val monthValid = runCatching { YearMonth.parse(calendarMonth.trim()) }.isSuccess
    val capacityValid = capacity != null && capacity.signum() > 0
    val reasonValid = reviewReason in reasons
    val componentsValid = codes.all { code ->
        val amountOk = parseRupeesToPaisaOrNull(amountTexts[code].orEmpty()) != null
        val basis = basisTexts[code].orEmpty().trim().toBigDecimalOrNull()
        val basisOk = basis != null && basis.signum() > 0
        val fixedOk = code != CostingPolicyService.COMPONENT_FIXED_OVERHEAD ||
            (capacity != null && basis != null && basis.compareTo(capacity) == 0)
        amountOk && basisOk && fixedOk
    }
    val canSave = monthValid && capacityValid && reasonValid && componentsValid && !saving
    val dirty = draftTouched && !saving

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        hostedScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
            { action -> requestLeave(action) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerCostingPlansApproveForm),
            ) {
                CashierScreenHeader(
                    title = "Costing & plans",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (supersedesPolicyId != null) {
                            "Approve corrected policy"
                        } else {
                            "Approve costing policy"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (supersedesPolicyId != null) {
                        Text(
                            "You are correcting standard #$supersedesPolicyId. The earlier version stays in history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansApproveSupersedeHint),
                        )
                    }
                    OutlinedTextField(
                        value = calendarMonth,
                        onValueChange = {
                            calendarMonth = it
                            draftTouched = true
                        },
                        label = { Text("Calendar month (YYYY-MM)") },
                        singleLine = true,
                        enabled = !saving && seed == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCostingPlansApproveMonth),
                    )
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = ::setCapacity,
                        label = { Text("Normal capacity (kg)") },
                        singleLine = true,
                        enabled = !saving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCostingPlansApproveCapacity),
                    )
                    Text(
                        "Review reason",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        reasons.chunked(2).forEach { rowReasons ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowReasons.forEach { reason ->
                                    FilterChip(
                                        selected = reviewReason == reason,
                                        onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                                            reviewReason = reason
                                            draftTouched = true
                                        },
                                        enabled = !saving,
                                        label = { Text(humanizeCode(reason)) },
                                        modifier = Modifier.testTag(
                                            NativeUiTags.ownerCostingReviewReason(reason),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    codes.forEach { code ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                humanizeCode(code),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OutlinedTextField(
                                value = amountTexts[code].orEmpty(),
                                onValueChange = {
                                    amountTexts = amountTexts + (code to it)
                                    draftTouched = true
                                },
                                label = { Text("Expected total cost (PKR)") },
                                singleLine = true,
                                enabled = !saving,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(NativeUiTags.ownerCostingComponentAmount(code)),
                            )
                            OutlinedTextField(
                                value = basisTexts[code].orEmpty(),
                                onValueChange = {
                                    if (code == CostingPolicyService.COMPONENT_FIXED_OVERHEAD) return@OutlinedTextField
                                    basisTexts = basisTexts + (code to it)
                                    draftTouched = true
                                },
                                label = { Text("This cost covers (kg)") },
                                singleLine = true,
                                enabled = !saving &&
                                    code != CostingPolicyService.COMPONENT_FIXED_OVERHEAD,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(NativeUiTags.ownerCostingComponentBasis(code)),
                            )
                            if (code == CostingPolicyService.COMPONENT_FIXED_OVERHEAD) {
                                Text(
                                    "Fixed production overhead must use normal capacity as its basis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            draftTouched = true
                        },
                        label = { Text("Notes (optional)") },
                        enabled = !saving,
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCostingPlansApproveNotes),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                            if (!canSave) return@hapticCommitAction
                            val input = CostingPolicyInput(
                                calendarMonth = calendarMonth.trim(),
                                normalCapacityKg = capacity!!,
                                reviewReason = reviewReason,
                                components = codes.map { code ->
                                    ConversionCostComponentInput(
                                        code = code,
                                        sourceAmountPaisa = parseRupeesToPaisaOrNull(
                                            amountTexts[code].orEmpty(),
                                        )!!,
                                        basisQuantityKg = basisTexts[code].orEmpty().trim()
                                            .toBigDecimal(),
                                    )
                                },
                                notes = notes.trim().ifBlank { null },
                                supersedesPolicyId = supersedesPolicyId,
                            )
                            onSave(input)
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCostingPlansApproveSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                if (supersedesPolicyId != null) {
                                    "Approve corrected standard"
                                } else {
                                    "Approve monthly standard"
                                },
                            )
                        }
                    }
                }
            }
        }
        OwnerCostingPlansDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this costing policy?",
            body = "The values you entered will be cleared.",
            discardTag = NativeUiTags.OwnerCostingPlansApproveDiscard,
            keepTag = NativeUiTags.OwnerCostingPlansApproveKeep,
        )
    }
}

@Composable
private fun OwnerCostingPlansReauthDialog(
    busy: Boolean,
    error: String?,
    onConfirm: (CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansReauth),
        title = { Text("Confirm with passphrase") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your passphrase again to approve this costing change.")
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingPlansReauthPassphrase),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                    val chars = passphrase.toCharArray()
                    passphrase = ""
                    onConfirm(chars)
                },
                enabled = !busy && passphrase.isNotEmpty(),
                modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansReauthConfirm),
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(
                onClick = hapticAction(onCancel),
                enabled = !busy,
                modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansReauthCancel),
            ) { Text("Cancel") }
        },
    )
}

@Composable
internal fun OwnerCostingPlansDiscardDialog(
    leaveGuard: EntryLeaveGuard,
    title: String,
    body: String,
    discardTag: String,
    keepTag: String,
) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = hapticAction(leaveGuard::confirmDiscard),
                modifier = Modifier.testTag(discardTag),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = hapticAction(leaveGuard::keepEditing),
                modifier = Modifier.testTag(keepTag),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
private fun OwnerCostingPolicyRow(
    policy: NativeOwnerCostingPolicyRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Record)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Record)
            .testTag(NativeUiTags.ownerCostingPolicy(policy.policyId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                policy.calendarMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(policy.conversionCostLabel, Modifier.fillMaxWidth(), style = nativeMetricTextStyle(), color = nativeVisualColors.metric)
            Text(policy.statusLabel, style = nativeSupportingTextStyle())
            Text(policy.reviewReason, style = nativeSupportingTextStyle(), color = nativeVisualColors.supporting)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun OwnerCostingComponentFact(component: NativeOwnerCostingComponentRow) {
    NativeInformationHeading(component.componentCode)
    NativeInformationFact("Source amount", component.sourceAmountLabel, emphasized = true)
    NativeInformationFact("Basis quantity", "${component.basisQuantityKg} kg")
}

@Composable
private fun OwnerCostingFact(label: String, value: String) {
    NativeInformationFact(label, value)
}

private fun humanizeCode(code: String): String =
    code.replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { it.titlecase(Locale.US) }
        }
        .ifBlank { code }

private fun paisaToRupeesInput(paisa: Long): String =
    BigDecimal.valueOf(paisa)
        .movePointLeft(2)
        .stripTrailingZeros()
        .toPlainString()

private fun parseRupeesToPaisaOrNull(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0L
    return runCatching {
        BigDecimal(trimmed).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()?.takeIf { it >= 0L }
}
