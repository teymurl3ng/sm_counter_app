package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.YieldPolicyInput
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns costing policies, budgets, collection targets, yield, and their re-auth flow. */
internal class OwnerCostingController(
    private val facade: NativeOwnerCostingPlansFacade,
    private val runtime: OwnerMoreRuntime,
) {
    private var pendingCostingApprove: CostingPolicyInput? = null
    private var pendingYieldApprove: YieldPolicyInput? = null

    fun openCostingPlans() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingCostingApprove = null
        pendingYieldApprove = null
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.CostingPlans,
            costingPoliciesLoading = true,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            costingSection = NativeOwnerCostingSection.Policies,
            costingBudgets = emptyList(),
            costingTargets = emptyList(),
            costingBudgetEditorOpen = false,
            costingBudgetSaving = false,
            costingCollectionsEditorOpen = false,
            costingCollectionsSaving = false,
            costingYieldPolicies = emptyList(),
            costingYieldBatches = emptyList(),
            costingYieldActivePolicyId = null,
            costingYieldSummary = null,
            costingYieldApproveOpen = false,
            costingYieldApproving = false,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
            shopSettingsLoading = false,
            shopSettings = null,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
            backupExportOpen = false,
            backupRestoreOpen = false,
            backupConnectOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        runtime.scope.launch {
            try {
                val (snapshot, planning, yieldContext) = withContext(Dispatchers.IO) {
                    Triple(
                        facade.loadPolicies(session, current.context.businessDate),
                        facade.loadPlanning(session),
                        facade.loadYieldContext(session),
                    )
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                runtime.state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingYieldPolicies = yieldContext.policies,
                    costingYieldBatches = yieldContext.batches,
                    costingYieldActivePolicyId = yieldContext.activePolicyId,
                    costingYieldSummary = yieldContext.activeSummary,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshCostingPlans() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        runtime.state.value = current.copy(
            costingPoliciesLoading = true,
            costingPoliciesError = null,
            costingPolicyDetail = null,
        )
        runtime.scope.launch {
            try {
                val (snapshot, planning, yieldContext) = withContext(Dispatchers.IO) {
                    Triple(
                        facade.loadPolicies(session, current.context.businessDate),
                        facade.loadPlanning(session),
                        facade.loadYieldContext(session),
                    )
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                runtime.state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingYieldPolicies = yieldContext.policies,
                    costingYieldBatches = yieldContext.batches,
                    costingYieldActivePolicyId = yieldContext.activePolicyId,
                    costingYieldSummary = yieldContext.activeSummary,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun setCostingSection(section: NativeOwnerCostingSection) {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        if (current.costingApproveOpen ||
            current.costingReauthOpen ||
            current.costingBudgetEditorOpen ||
            current.costingCollectionsEditorOpen ||
            current.costingYieldApproveOpen ||
            current.costingPolicyDetail != null
        ) {
            return
        }
        runtime.state.value = current.copy(costingSection = section, costingPoliciesError = null)
    }

    fun openCostingPolicyDetail(policyId: Long) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        runtime.state.value = current.copy(costingPoliciesLoading = true, costingPoliciesError = null)
        runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, policyId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                runtime.state.value = latest.copy(
                    costingPolicyDetail = detail,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearCostingPolicyDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.costingApproveOpen || current.costingReauthOpen) return
        runtime.state.value = current.copy(
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingPoliciesLoading = false,
        )
    }

    fun openCostingApprove() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        pendingCostingApprove = null
        runtime.state.value = current.copy(
            costingApproveOpen = true,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingReviewReasons = facade.reviewReasons(),
            costingComponentCodes = facade.componentCodes(),
            costingPoliciesError = null,
        )
    }

    fun openCostingApproveFromDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.costingPolicyDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        if (detail.row.statusLabel != "Active") return
        pendingCostingApprove = null
        runtime.state.value = current.copy(
            costingApproveOpen = true,
            costingApproveSeed = detail,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingReviewReasons = facade.reviewReasons(),
            costingComponentCodes = facade.componentCodes(),
            costingPoliciesError = null,
        )
    }

    fun clearCostingApprove() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingCostingApprove = null
        runtime.state.value = current.copy(
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingPoliciesError = null,
        )
    }

    fun approveCostingPolicy(input: CostingPolicyInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingApproveOpen ||
            current.costingApproving
        ) {
            return
        }
        runtime.state.value = current.copy(costingApproving = true, costingPoliciesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.approvePolicy(session, input)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    facade.loadPolicies(session, current.context.businessDate)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                pendingCostingApprove = null
                runtime.state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
                    costingPolicyDetail = null,
                    costingApproveOpen = false,
                    costingApproveSeed = null,
                    costingApproving = false,
                    costingReauthOpen = false,
                    costingReauthBusy = false,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingCostingApprove = input
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        costingApproving = false,
                        costingReauthOpen = true,
                        costingReauthBusy = false,
                        costingPoliciesError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }

    fun submitCostingReauth(passphrase: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingReauthOpen ||
            current.costingReauthBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        val pendingPolicy = pendingCostingApprove
        val pendingYield = pendingYieldApprove
        if (pendingPolicy == null && pendingYield == null) {
            passphrase.fill('\u0000')
            runtime.state.value = current.copy(costingReauthOpen = false, costingPoliciesError = null)
            return
        }
        runtime.state.value = current.copy(costingReauthBusy = true, costingPoliciesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.reauthenticate(session, passphrase)
                }
                if (pendingPolicy != null) {
                    withContext(Dispatchers.IO) {
                        facade.approvePolicy(session, pendingPolicy)
                    }
                    val snapshot = withContext(Dispatchers.IO) {
                        facade.loadPolicies(session, current.context.businessDate)
                    }
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                    pendingCostingApprove = null
                    pendingYieldApprove = null
                    runtime.state.value = latest.copy(
                        costingPolicies = snapshot.policies,
                        costingCurrentMonthSummary = snapshot.currentMonthSummary,
                        costingPolicyDetail = null,
                        costingApproveOpen = false,
                        costingApproveSeed = null,
                        costingApproving = false,
                        costingReauthOpen = false,
                        costingReauthBusy = false,
                        costingPoliciesLoading = false,
                        costingPoliciesError = null,
                    )
                } else if (pendingYield != null) {
                    withContext(Dispatchers.IO) {
                        facade.approveYieldPolicy(session, pendingYield)
                    }
                    val yieldContext = withContext(Dispatchers.IO) {
                        facade.loadYieldContext(session)
                    }
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                    pendingCostingApprove = null
                    pendingYieldApprove = null
                    runtime.state.value = latest.copy(
                        costingYieldPolicies = yieldContext.policies,
                        costingYieldBatches = yieldContext.batches,
                        costingYieldActivePolicyId = yieldContext.activePolicyId,
                        costingYieldSummary = yieldContext.activeSummary,
                        costingYieldApproveOpen = false,
                        costingYieldApproving = false,
                        costingReauthOpen = false,
                        costingReauthBusy = false,
                        costingPoliciesError = null,
                    )
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun cancelCostingReauth() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingCostingApprove = null
        pendingYieldApprove = null
        runtime.state.value = current.copy(
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingApproving = false,
            costingYieldApproving = false,
        )
    }

    fun openCostingBudgetEditor() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        runtime.state.value = current.copy(
            costingSection = NativeOwnerCostingSection.Plans,
            costingBudgetEditorOpen = true,
            costingBudgetSaving = false,
            costingPoliciesError = null,
        )
    }

    fun clearCostingBudgetEditor() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            costingBudgetEditorOpen = false,
            costingBudgetSaving = false,
            costingPoliciesError = null,
        )
    }

    fun upsertCostingBudget(input: BudgetPlanInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingBudgetEditorOpen ||
            current.costingBudgetSaving
        ) {
            return
        }
        runtime.state.value = current.copy(costingBudgetSaving = true, costingPoliciesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.upsertBudget(session, input)
                }
                val planning = withContext(Dispatchers.IO) {
                    facade.loadPlanning(session)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                runtime.state.value = latest.copy(
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingBudgetEditorOpen = false,
                    costingBudgetSaving = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun copyPreviousCostingBudget(category: String, periodStart: LocalDate, periodEndExclusive: LocalDate) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingBudgetEditorOpen ||
            current.costingBudgetSaving
        ) {
            return
        }
        runtime.state.value = current.copy(costingBudgetSaving = true, costingPoliciesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.copyPreviousBudget(session, category, periodStart, periodEndExclusive)
                }
                val planning = withContext(Dispatchers.IO) {
                    facade.loadPlanning(session)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                runtime.state.value = latest.copy(
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingBudgetEditorOpen = false,
                    costingBudgetSaving = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openCostingCollectionsEditor() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        runtime.state.value = current.copy(
            costingSection = NativeOwnerCostingSection.Plans,
            costingCollectionsEditorOpen = true,
            costingCollectionsSaving = false,
            costingPoliciesError = null,
        )
    }

    fun clearCostingCollectionsEditor() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            costingCollectionsEditorOpen = false,
            costingCollectionsSaving = false,
            costingPoliciesError = null,
        )
    }

    fun upsertCostingCollectionsTarget(weekStart: LocalDate, amount: Double) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingCollectionsEditorOpen ||
            current.costingCollectionsSaving
        ) {
            return
        }
        runtime.state.value = current.copy(costingCollectionsSaving = true, costingPoliciesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.createOrReviseCollectionsTarget(session, weekStart, amount)
                }
                val planning = withContext(Dispatchers.IO) {
                    facade.loadPlanning(session)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                runtime.state.value = latest.copy(
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingCollectionsEditorOpen = false,
                    costingCollectionsSaving = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openCostingYieldApprove() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        pendingYieldApprove = null
        runtime.state.value = current.copy(
            costingSection = NativeOwnerCostingSection.Yield,
            costingYieldApproveOpen = true,
            costingYieldApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingPoliciesError = null,
        )
    }

    fun clearCostingYieldApprove() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingYieldApprove = null
        runtime.state.value = current.copy(
            costingYieldApproveOpen = false,
            costingYieldApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingPoliciesError = null,
        )
    }

    fun approveCostingYieldPolicy(input: YieldPolicyInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingYieldApproveOpen ||
            current.costingYieldApproving
        ) {
            return
        }
        runtime.state.value = current.copy(costingYieldApproving = true, costingPoliciesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.approveYieldPolicy(session, input)
                }
                val yieldContext = withContext(Dispatchers.IO) {
                    facade.loadYieldContext(session)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                pendingYieldApprove = null
                runtime.state.value = latest.copy(
                    costingYieldPolicies = yieldContext.policies,
                    costingYieldBatches = yieldContext.batches,
                    costingYieldActivePolicyId = yieldContext.activePolicyId,
                    costingYieldSummary = yieldContext.activeSummary,
                    costingYieldApproveOpen = false,
                    costingYieldApproving = false,
                    costingReauthOpen = false,
                    costingReauthBusy = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingYieldApprove = input
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        costingYieldApproving = false,
                        costingReauthOpen = true,
                        costingReauthBusy = false,
                        costingPoliciesError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }


    fun resetPending() {
        pendingCostingApprove = null
        pendingYieldApprove = null
    }

    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.CostingPlans, error) { current, message ->
            current.copy(
                costingPoliciesLoading = false,
                costingApproving = false,
                costingReauthBusy = false,
                costingBudgetSaving = false,
                costingCollectionsSaving = false,
                costingYieldApproving = false,
                costingPoliciesError = message,
            )
        }
    }
}
