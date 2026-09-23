package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.owner.NativeOwnerBackupInspection
import com.saaf.milkshop.application.owner.NativeOwnerBackupSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerBudgetRow
import com.saaf.milkshop.application.owner.NativeOwnerCollectionsTargetRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyDetail
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeDetail
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeRow
import com.saaf.milkshop.application.owner.NativeOwnerExpenseCategoryRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerOpeningCostRow
import com.saaf.milkshop.application.owner.NativeOwnerPendingEnrollment
import com.saaf.milkshop.application.owner.NativeOwnerProductDetail
import com.saaf.milkshop.application.owner.NativeOwnerProductRow
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditDetail
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditRow
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerSupplierDetail
import com.saaf.milkshop.application.owner.NativeOwnerSupplierRow
import com.saaf.milkshop.application.owner.NativeOwnerUserDetail
import com.saaf.milkshop.application.owner.NativeOwnerUserRow
import com.saaf.milkshop.application.owner.NativeOwnerYieldBatchRow
import com.saaf.milkshop.application.owner.NativeOwnerYieldPolicyRow
import com.saaf.milkshop.domain.AppRole

enum class NativeOwnerMoreDestination {
    Menu,
    PersonalSecurity,
    Products,
    Customers,
    Suppliers,
    Employees,
    CostingPlans,
    ShopSettings,
    UsersRoles,
    SecurityHistory,
    Backup,
}

enum class NativeOwnerCostingSection {
    Policies,
    Plans,
    Opening,
    Yield,
}

data class OwnerPersonalSecurityState(
    val saving: Boolean = false,
    val confirmRemoveUnlock: Boolean = false,
    val error: String? = null,
)

data class OwnerProductsState(
    val products: List<NativeOwnerProductRow> = emptyList(),
    val productsLoading: Boolean = false,
    val productsShowArchived: Boolean = false,
    val productDetail: NativeOwnerProductDetail? = null,
    val productCreateOpen: Boolean = false,
    val productCreating: Boolean = false,
    val productEditOpen: Boolean = false,
    val productUpdating: Boolean = false,
    val productArchiving: Boolean = false,
    val productUnarchiving: Boolean = false,
    val productsError: String? = null,
)

data class OwnerCustomersState(
    val customers: List<NativeOwnerCustomerRow> = emptyList(),
    val customersLoading: Boolean = false,
    val customerDetail: NativeOwnerCustomerDetail? = null,
    val customerCreateOpen: Boolean = false,
    val customerCreating: Boolean = false,
    val customerEditOpen: Boolean = false,
    val customerUpdating: Boolean = false,
    val customersError: String? = null,
)

data class OwnerSuppliersState(
    val suppliers: List<NativeOwnerSupplierRow> = emptyList(),
    val suppliersLoading: Boolean = false,
    val supplierDetail: NativeOwnerSupplierDetail? = null,
    val supplierCreateOpen: Boolean = false,
    val supplierCreating: Boolean = false,
    val supplierEditOpen: Boolean = false,
    val supplierUpdating: Boolean = false,
    val suppliersError: String? = null,
)

data class OwnerEmployeesState(
    val employees: List<NativeOwnerEmployeeRow> = emptyList(),
    val employeesLoading: Boolean = false,
    val employeeDetail: NativeOwnerEmployeeDetail? = null,
    val employeeCreateOpen: Boolean = false,
    val employeeCreating: Boolean = false,
    val employeeEditOpen: Boolean = false,
    val employeeUpdating: Boolean = false,
    val employeesError: String? = null,
)

data class OwnerCostingState(
    val costingPolicies: List<NativeOwnerCostingPolicyRow> = emptyList(),
    val costingPoliciesLoading: Boolean = false,
    val costingPolicyDetail: NativeOwnerCostingPolicyDetail? = null,
    val costingPoliciesError: String? = null,
    val costingCurrentMonthSummary: String? = null,
    val costingSection: NativeOwnerCostingSection = NativeOwnerCostingSection.Policies,
    val costingBudgets: List<NativeOwnerBudgetRow> = emptyList(),
    val costingTargets: List<NativeOwnerCollectionsTargetRow> = emptyList(),
    val costingBudgetCategories: List<String> = emptyList(),
    val costingDefaultMilkWeekStart: String = "",
    val costingDefaultOperatingMonthStart: String = "",
    val costingBudgetEditorOpen: Boolean = false,
    val costingBudgetSaving: Boolean = false,
    val costingCollectionsEditorOpen: Boolean = false,
    val costingCollectionsSaving: Boolean = false,
    val costingOpeningCosts: List<NativeOwnerOpeningCostRow> = emptyList(),
    val costingOpeningSummary: String? = null,
    val costingOpeningBasisKinds: List<String> = emptyList(),
    val costingOpeningEditor: NativeOwnerOpeningCostRow? = null,
    val costingOpeningSaving: Boolean = false,
    val costingYieldPolicies: List<NativeOwnerYieldPolicyRow> = emptyList(),
    val costingYieldBatches: List<NativeOwnerYieldBatchRow> = emptyList(),
    val costingYieldActivePolicyId: Long? = null,
    val costingYieldSummary: String? = null,
    val costingYieldApproveOpen: Boolean = false,
    val costingYieldApproving: Boolean = false,
    val costingApproveOpen: Boolean = false,
    val costingApproveSeed: NativeOwnerCostingPolicyDetail? = null,
    val costingApproving: Boolean = false,
    val costingReauthOpen: Boolean = false,
    val costingReauthBusy: Boolean = false,
    val costingReviewReasons: List<String> = emptyList(),
    val costingComponentCodes: List<String> = emptyList(),
)

data class OwnerShopSettingsState(
    val shopSettings: NativeOwnerShopSettingsSnapshot? = null,
    val shopSettingsLoading: Boolean = false,
    val shopSettingsIdentityEditOpen: Boolean = false,
    val shopSettingsIdentitySaving: Boolean = false,
    val shopSettingsRatesEditOpen: Boolean = false,
    val shopSettingsRatesSaving: Boolean = false,
    val shopSettingsCategoryCreateOpen: Boolean = false,
    val shopSettingsCategoryCreating: Boolean = false,
    val shopSettingsCategoryDetail: NativeOwnerExpenseCategoryRow? = null,
    val shopSettingsCategoryArchiving: Boolean = false,
    val shopSettingsError: String? = null,
    val shopSettingsOpeningEditOpen: Boolean = false,
    val shopSettingsOpeningCorrection: Boolean = false,
    val shopSettingsOpeningSaving: Boolean = false,
    val shopSettingsOpeningReauthOpen: Boolean = false,
    val shopSettingsOpeningReauthBusy: Boolean = false,
)

data class OwnerUsersRolesState(
    val users: List<NativeOwnerUserRow> = emptyList(),
    val usersLoading: Boolean = false,
    val userDetail: NativeOwnerUserDetail? = null,
    val userEnrollOpen: Boolean = false,
    val userEnrollPending: NativeOwnerPendingEnrollment? = null,
    val userEnrollBusy: Boolean = false,
    val userRoleChangeOpen: Boolean = false,
    val userRoleChanging: Boolean = false,
    val userEnableConfirmOpen: Boolean = false,
    val userEnableTargetEnabled: Boolean = false,
    val userEnableBusy: Boolean = false,
    val userReauthOpen: Boolean = false,
    val userReauthBusy: Boolean = false,
    val enrollableRoles: List<AppRole> = AppRole.entries.toList(),
    val usersError: String? = null,
)

data class OwnerSecurityHistoryState(
    val securityAudits: List<NativeOwnerSecurityAuditRow> = emptyList(),
    val securityHistoryLoading: Boolean = false,
    val securityAuditDetail: NativeOwnerSecurityAuditDetail? = null,
    val securityHistoryError: String? = null,
)

data class OwnerBackupState(
    val backup: NativeOwnerBackupSnapshot? = null,
    val backupLoading: Boolean = false,
    val backupError: String? = null,
    val backupExportOpen: Boolean = false,
    val backupRestoreOpen: Boolean = false,
    val backupConnectOpen: Boolean = false,
    val backupInspection: NativeOwnerBackupInspection? = null,
    val backupBusy: Boolean = false,
    val backupReauthOpen: Boolean = false,
    val backupReauthBusy: Boolean = false,
    val backupDisconnectConfirmOpen: Boolean = false,
    val backupCloudOpen: Boolean = false,
)

sealed interface NativeOwnerMoreState {
    data object Idle : NativeOwnerMoreState
    data object Loading : NativeOwnerMoreState
    data class Ready(
        val context: NativeOwnerMoreContext,
        val destination: NativeOwnerMoreDestination = NativeOwnerMoreDestination.Menu,
        val security: NativeMoreSecurityStatus? = null,
        val personalSecurity: OwnerPersonalSecurityState = OwnerPersonalSecurityState(),
        val productsState: OwnerProductsState = OwnerProductsState(),
        val customersState: OwnerCustomersState = OwnerCustomersState(),
        val suppliersState: OwnerSuppliersState = OwnerSuppliersState(),
        val employeesState: OwnerEmployeesState = OwnerEmployeesState(),
        val costingState: OwnerCostingState = OwnerCostingState(),
        val shopSettingsState: OwnerShopSettingsState = OwnerShopSettingsState(),
        val usersRolesState: OwnerUsersRolesState = OwnerUsersRolesState(),
        val securityHistoryState: OwnerSecurityHistoryState = OwnerSecurityHistoryState(),
        val backupState: OwnerBackupState = OwnerBackupState(),
    ) : NativeOwnerMoreState {
        val products: List<NativeOwnerProductRow>
            get() = productsState.products

        val productsLoading: Boolean
            get() = productsState.productsLoading

        val productsShowArchived: Boolean
            get() = productsState.productsShowArchived

        val productDetail: NativeOwnerProductDetail?
            get() = productsState.productDetail

        val productCreateOpen: Boolean
            get() = productsState.productCreateOpen

        val productCreating: Boolean
            get() = productsState.productCreating

        val productEditOpen: Boolean
            get() = productsState.productEditOpen

        val productUpdating: Boolean
            get() = productsState.productUpdating

        val productArchiving: Boolean
            get() = productsState.productArchiving

        val productUnarchiving: Boolean
            get() = productsState.productUnarchiving

        val productsError: String?
            get() = productsState.productsError

        val customers: List<NativeOwnerCustomerRow>
            get() = customersState.customers

        val customersLoading: Boolean
            get() = customersState.customersLoading

        val customerDetail: NativeOwnerCustomerDetail?
            get() = customersState.customerDetail

        val customerCreateOpen: Boolean
            get() = customersState.customerCreateOpen

        val customerCreating: Boolean
            get() = customersState.customerCreating

        val customerEditOpen: Boolean
            get() = customersState.customerEditOpen

        val customerUpdating: Boolean
            get() = customersState.customerUpdating

        val customersError: String?
            get() = customersState.customersError

        val suppliers: List<NativeOwnerSupplierRow>
            get() = suppliersState.suppliers

        val suppliersLoading: Boolean
            get() = suppliersState.suppliersLoading

        val supplierDetail: NativeOwnerSupplierDetail?
            get() = suppliersState.supplierDetail

        val supplierCreateOpen: Boolean
            get() = suppliersState.supplierCreateOpen

        val supplierCreating: Boolean
            get() = suppliersState.supplierCreating

        val supplierEditOpen: Boolean
            get() = suppliersState.supplierEditOpen

        val supplierUpdating: Boolean
            get() = suppliersState.supplierUpdating

        val suppliersError: String?
            get() = suppliersState.suppliersError

        val employees: List<NativeOwnerEmployeeRow>
            get() = employeesState.employees

        val employeesLoading: Boolean
            get() = employeesState.employeesLoading

        val employeeDetail: NativeOwnerEmployeeDetail?
            get() = employeesState.employeeDetail

        val employeeCreateOpen: Boolean
            get() = employeesState.employeeCreateOpen

        val employeeCreating: Boolean
            get() = employeesState.employeeCreating

        val employeeEditOpen: Boolean
            get() = employeesState.employeeEditOpen

        val employeeUpdating: Boolean
            get() = employeesState.employeeUpdating

        val employeesError: String?
            get() = employeesState.employeesError

        val costingPolicies: List<NativeOwnerCostingPolicyRow>
            get() = costingState.costingPolicies

        val costingPoliciesLoading: Boolean
            get() = costingState.costingPoliciesLoading

        val costingPolicyDetail: NativeOwnerCostingPolicyDetail?
            get() = costingState.costingPolicyDetail

        val costingPoliciesError: String?
            get() = costingState.costingPoliciesError

        val costingCurrentMonthSummary: String?
            get() = costingState.costingCurrentMonthSummary

        val costingSection: NativeOwnerCostingSection
            get() = costingState.costingSection

        val costingBudgets: List<NativeOwnerBudgetRow>
            get() = costingState.costingBudgets

        val costingTargets: List<NativeOwnerCollectionsTargetRow>
            get() = costingState.costingTargets

        val costingBudgetCategories: List<String>
            get() = costingState.costingBudgetCategories

        val costingDefaultMilkWeekStart: String
            get() = costingState.costingDefaultMilkWeekStart

        val costingDefaultOperatingMonthStart: String
            get() = costingState.costingDefaultOperatingMonthStart

        val costingBudgetEditorOpen: Boolean
            get() = costingState.costingBudgetEditorOpen

        val costingBudgetSaving: Boolean
            get() = costingState.costingBudgetSaving

        val costingCollectionsEditorOpen: Boolean
            get() = costingState.costingCollectionsEditorOpen

        val costingCollectionsSaving: Boolean
            get() = costingState.costingCollectionsSaving

        val costingOpeningCosts: List<NativeOwnerOpeningCostRow>
            get() = costingState.costingOpeningCosts

        val costingOpeningSummary: String?
            get() = costingState.costingOpeningSummary

        val costingOpeningBasisKinds: List<String>
            get() = costingState.costingOpeningBasisKinds

        val costingOpeningEditor: NativeOwnerOpeningCostRow?
            get() = costingState.costingOpeningEditor

        val costingOpeningSaving: Boolean
            get() = costingState.costingOpeningSaving

        val costingYieldPolicies: List<NativeOwnerYieldPolicyRow>
            get() = costingState.costingYieldPolicies

        val costingYieldBatches: List<NativeOwnerYieldBatchRow>
            get() = costingState.costingYieldBatches

        val costingYieldActivePolicyId: Long?
            get() = costingState.costingYieldActivePolicyId

        val costingYieldSummary: String?
            get() = costingState.costingYieldSummary

        val costingYieldApproveOpen: Boolean
            get() = costingState.costingYieldApproveOpen

        val costingYieldApproving: Boolean
            get() = costingState.costingYieldApproving

        val costingApproveOpen: Boolean
            get() = costingState.costingApproveOpen

        val costingApproveSeed: NativeOwnerCostingPolicyDetail?
            get() = costingState.costingApproveSeed

        val costingApproving: Boolean
            get() = costingState.costingApproving

        val costingReauthOpen: Boolean
            get() = costingState.costingReauthOpen

        val costingReauthBusy: Boolean
            get() = costingState.costingReauthBusy

        val costingReviewReasons: List<String>
            get() = costingState.costingReviewReasons

        val costingComponentCodes: List<String>
            get() = costingState.costingComponentCodes

        val shopSettings: NativeOwnerShopSettingsSnapshot?
            get() = shopSettingsState.shopSettings

        val shopSettingsLoading: Boolean
            get() = shopSettingsState.shopSettingsLoading

        val shopSettingsIdentityEditOpen: Boolean
            get() = shopSettingsState.shopSettingsIdentityEditOpen

        val shopSettingsIdentitySaving: Boolean
            get() = shopSettingsState.shopSettingsIdentitySaving

        val shopSettingsRatesEditOpen: Boolean
            get() = shopSettingsState.shopSettingsRatesEditOpen

        val shopSettingsRatesSaving: Boolean
            get() = shopSettingsState.shopSettingsRatesSaving

        val shopSettingsCategoryCreateOpen: Boolean
            get() = shopSettingsState.shopSettingsCategoryCreateOpen

        val shopSettingsCategoryCreating: Boolean
            get() = shopSettingsState.shopSettingsCategoryCreating

        val shopSettingsCategoryDetail: NativeOwnerExpenseCategoryRow?
            get() = shopSettingsState.shopSettingsCategoryDetail

        val shopSettingsCategoryArchiving: Boolean
            get() = shopSettingsState.shopSettingsCategoryArchiving

        val shopSettingsError: String?
            get() = shopSettingsState.shopSettingsError

        val shopSettingsOpeningEditOpen: Boolean
            get() = shopSettingsState.shopSettingsOpeningEditOpen

        val shopSettingsOpeningCorrection: Boolean
            get() = shopSettingsState.shopSettingsOpeningCorrection

        val shopSettingsOpeningSaving: Boolean
            get() = shopSettingsState.shopSettingsOpeningSaving

        val shopSettingsOpeningReauthOpen: Boolean
            get() = shopSettingsState.shopSettingsOpeningReauthOpen

        val shopSettingsOpeningReauthBusy: Boolean
            get() = shopSettingsState.shopSettingsOpeningReauthBusy

        val users: List<NativeOwnerUserRow>
            get() = usersRolesState.users

        val usersLoading: Boolean
            get() = usersRolesState.usersLoading

        val userDetail: NativeOwnerUserDetail?
            get() = usersRolesState.userDetail

        val userEnrollOpen: Boolean
            get() = usersRolesState.userEnrollOpen

        val userEnrollPending: NativeOwnerPendingEnrollment?
            get() = usersRolesState.userEnrollPending

        val userEnrollBusy: Boolean
            get() = usersRolesState.userEnrollBusy

        val userRoleChangeOpen: Boolean
            get() = usersRolesState.userRoleChangeOpen

        val userRoleChanging: Boolean
            get() = usersRolesState.userRoleChanging

        val userEnableConfirmOpen: Boolean
            get() = usersRolesState.userEnableConfirmOpen

        val userEnableTargetEnabled: Boolean
            get() = usersRolesState.userEnableTargetEnabled

        val userEnableBusy: Boolean
            get() = usersRolesState.userEnableBusy

        val userReauthOpen: Boolean
            get() = usersRolesState.userReauthOpen

        val userReauthBusy: Boolean
            get() = usersRolesState.userReauthBusy

        val enrollableRoles: List<AppRole>
            get() = usersRolesState.enrollableRoles

        val usersError: String?
            get() = usersRolesState.usersError

        val securityAudits: List<NativeOwnerSecurityAuditRow>
            get() = securityHistoryState.securityAudits

        val securityHistoryLoading: Boolean
            get() = securityHistoryState.securityHistoryLoading

        val securityAuditDetail: NativeOwnerSecurityAuditDetail?
            get() = securityHistoryState.securityAuditDetail

        val securityHistoryError: String?
            get() = securityHistoryState.securityHistoryError

        val backup: NativeOwnerBackupSnapshot?
            get() = backupState.backup

        val backupLoading: Boolean
            get() = backupState.backupLoading

        val backupError: String?
            get() = backupState.backupError

        val backupExportOpen: Boolean
            get() = backupState.backupExportOpen

        val backupRestoreOpen: Boolean
            get() = backupState.backupRestoreOpen

        val backupConnectOpen: Boolean
            get() = backupState.backupConnectOpen

        val backupInspection: NativeOwnerBackupInspection?
            get() = backupState.backupInspection

        val backupBusy: Boolean
            get() = backupState.backupBusy

        val backupReauthOpen: Boolean
            get() = backupState.backupReauthOpen

        val backupReauthBusy: Boolean
            get() = backupState.backupReauthBusy

        val backupDisconnectConfirmOpen: Boolean
            get() = backupState.backupDisconnectConfirmOpen

        val backupCloudOpen: Boolean
            get() = backupState.backupCloudOpen

        val saving: Boolean
            get() = personalSecurity.saving

        val confirmRemoveUnlock: Boolean
            get() = personalSecurity.confirmRemoveUnlock

        val error: String?
            get() = personalSecurity.error
    }
    data class LoadFailure(val message: String) : NativeOwnerMoreState
}

/**
 * Compatibility mutation boundary while screens and controllers migrate to the
 * grouped feature-state properties above. State is stored by feature, and this
 * adapter keeps the established named-argument API behavior-preserving.
 */
internal fun NativeOwnerMoreState.Ready.copy(
    context: NativeOwnerMoreContext = this.context,
    destination: NativeOwnerMoreDestination = this.destination,
    security: NativeMoreSecurityStatus? = this.security,
    products: List<NativeOwnerProductRow> = this.products,
    productsLoading: Boolean = this.productsLoading,
    productsShowArchived: Boolean = this.productsShowArchived,
    productDetail: NativeOwnerProductDetail? = this.productDetail,
    productCreateOpen: Boolean = this.productCreateOpen,
    productCreating: Boolean = this.productCreating,
    productEditOpen: Boolean = this.productEditOpen,
    productUpdating: Boolean = this.productUpdating,
    productArchiving: Boolean = this.productArchiving,
    productUnarchiving: Boolean = this.productUnarchiving,
    productsError: String? = this.productsError,
    customers: List<NativeOwnerCustomerRow> = this.customers,
    customersLoading: Boolean = this.customersLoading,
    customerDetail: NativeOwnerCustomerDetail? = this.customerDetail,
    customerCreateOpen: Boolean = this.customerCreateOpen,
    customerCreating: Boolean = this.customerCreating,
    customerEditOpen: Boolean = this.customerEditOpen,
    customerUpdating: Boolean = this.customerUpdating,
    customersError: String? = this.customersError,
    suppliers: List<NativeOwnerSupplierRow> = this.suppliers,
    suppliersLoading: Boolean = this.suppliersLoading,
    supplierDetail: NativeOwnerSupplierDetail? = this.supplierDetail,
    supplierCreateOpen: Boolean = this.supplierCreateOpen,
    supplierCreating: Boolean = this.supplierCreating,
    supplierEditOpen: Boolean = this.supplierEditOpen,
    supplierUpdating: Boolean = this.supplierUpdating,
    suppliersError: String? = this.suppliersError,
    employees: List<NativeOwnerEmployeeRow> = this.employees,
    employeesLoading: Boolean = this.employeesLoading,
    employeeDetail: NativeOwnerEmployeeDetail? = this.employeeDetail,
    employeeCreateOpen: Boolean = this.employeeCreateOpen,
    employeeCreating: Boolean = this.employeeCreating,
    employeeEditOpen: Boolean = this.employeeEditOpen,
    employeeUpdating: Boolean = this.employeeUpdating,
    employeesError: String? = this.employeesError,
    costingPolicies: List<NativeOwnerCostingPolicyRow> = this.costingPolicies,
    costingPoliciesLoading: Boolean = this.costingPoliciesLoading,
    costingPolicyDetail: NativeOwnerCostingPolicyDetail? = this.costingPolicyDetail,
    costingPoliciesError: String? = this.costingPoliciesError,
    costingCurrentMonthSummary: String? = this.costingCurrentMonthSummary,
    costingSection: NativeOwnerCostingSection = this.costingSection,
    costingBudgets: List<NativeOwnerBudgetRow> = this.costingBudgets,
    costingTargets: List<NativeOwnerCollectionsTargetRow> = this.costingTargets,
    costingBudgetCategories: List<String> = this.costingBudgetCategories,
    costingDefaultMilkWeekStart: String = this.costingDefaultMilkWeekStart,
    costingDefaultOperatingMonthStart: String = this.costingDefaultOperatingMonthStart,
    costingBudgetEditorOpen: Boolean = this.costingBudgetEditorOpen,
    costingBudgetSaving: Boolean = this.costingBudgetSaving,
    costingCollectionsEditorOpen: Boolean = this.costingCollectionsEditorOpen,
    costingCollectionsSaving: Boolean = this.costingCollectionsSaving,
    costingOpeningCosts: List<NativeOwnerOpeningCostRow> = this.costingOpeningCosts,
    costingOpeningSummary: String? = this.costingOpeningSummary,
    costingOpeningBasisKinds: List<String> = this.costingOpeningBasisKinds,
    costingOpeningEditor: NativeOwnerOpeningCostRow? = this.costingOpeningEditor,
    costingOpeningSaving: Boolean = this.costingOpeningSaving,
    costingYieldPolicies: List<NativeOwnerYieldPolicyRow> = this.costingYieldPolicies,
    costingYieldBatches: List<NativeOwnerYieldBatchRow> = this.costingYieldBatches,
    costingYieldActivePolicyId: Long? = this.costingYieldActivePolicyId,
    costingYieldSummary: String? = this.costingYieldSummary,
    costingYieldApproveOpen: Boolean = this.costingYieldApproveOpen,
    costingYieldApproving: Boolean = this.costingYieldApproving,
    costingApproveOpen: Boolean = this.costingApproveOpen,
    costingApproveSeed: NativeOwnerCostingPolicyDetail? = this.costingApproveSeed,
    costingApproving: Boolean = this.costingApproving,
    costingReauthOpen: Boolean = this.costingReauthOpen,
    costingReauthBusy: Boolean = this.costingReauthBusy,
    costingReviewReasons: List<String> = this.costingReviewReasons,
    costingComponentCodes: List<String> = this.costingComponentCodes,
    shopSettings: NativeOwnerShopSettingsSnapshot? = this.shopSettings,
    shopSettingsLoading: Boolean = this.shopSettingsLoading,
    shopSettingsIdentityEditOpen: Boolean = this.shopSettingsIdentityEditOpen,
    shopSettingsIdentitySaving: Boolean = this.shopSettingsIdentitySaving,
    shopSettingsRatesEditOpen: Boolean = this.shopSettingsRatesEditOpen,
    shopSettingsRatesSaving: Boolean = this.shopSettingsRatesSaving,
    shopSettingsCategoryCreateOpen: Boolean = this.shopSettingsCategoryCreateOpen,
    shopSettingsCategoryCreating: Boolean = this.shopSettingsCategoryCreating,
    shopSettingsCategoryDetail: NativeOwnerExpenseCategoryRow? = this.shopSettingsCategoryDetail,
    shopSettingsCategoryArchiving: Boolean = this.shopSettingsCategoryArchiving,
    shopSettingsError: String? = this.shopSettingsError,
    shopSettingsOpeningEditOpen: Boolean = this.shopSettingsOpeningEditOpen,
    shopSettingsOpeningCorrection: Boolean = this.shopSettingsOpeningCorrection,
    shopSettingsOpeningSaving: Boolean = this.shopSettingsOpeningSaving,
    shopSettingsOpeningReauthOpen: Boolean = this.shopSettingsOpeningReauthOpen,
    shopSettingsOpeningReauthBusy: Boolean = this.shopSettingsOpeningReauthBusy,
    users: List<NativeOwnerUserRow> = this.users,
    usersLoading: Boolean = this.usersLoading,
    userDetail: NativeOwnerUserDetail? = this.userDetail,
    userEnrollOpen: Boolean = this.userEnrollOpen,
    userEnrollPending: NativeOwnerPendingEnrollment? = this.userEnrollPending,
    userEnrollBusy: Boolean = this.userEnrollBusy,
    userRoleChangeOpen: Boolean = this.userRoleChangeOpen,
    userRoleChanging: Boolean = this.userRoleChanging,
    userEnableConfirmOpen: Boolean = this.userEnableConfirmOpen,
    userEnableTargetEnabled: Boolean = this.userEnableTargetEnabled,
    userEnableBusy: Boolean = this.userEnableBusy,
    userReauthOpen: Boolean = this.userReauthOpen,
    userReauthBusy: Boolean = this.userReauthBusy,
    enrollableRoles: List<AppRole> = this.enrollableRoles,
    usersError: String? = this.usersError,
    securityAudits: List<NativeOwnerSecurityAuditRow> = this.securityAudits,
    securityHistoryLoading: Boolean = this.securityHistoryLoading,
    securityAuditDetail: NativeOwnerSecurityAuditDetail? = this.securityAuditDetail,
    securityHistoryError: String? = this.securityHistoryError,
    backup: NativeOwnerBackupSnapshot? = this.backup,
    backupLoading: Boolean = this.backupLoading,
    backupError: String? = this.backupError,
    backupExportOpen: Boolean = this.backupExportOpen,
    backupRestoreOpen: Boolean = this.backupRestoreOpen,
    backupConnectOpen: Boolean = this.backupConnectOpen,
    backupInspection: NativeOwnerBackupInspection? = this.backupInspection,
    backupBusy: Boolean = this.backupBusy,
    backupReauthOpen: Boolean = this.backupReauthOpen,
    backupReauthBusy: Boolean = this.backupReauthBusy,
    backupDisconnectConfirmOpen: Boolean = this.backupDisconnectConfirmOpen,
    backupCloudOpen: Boolean = this.backupCloudOpen,
    saving: Boolean = this.saving,
    confirmRemoveUnlock: Boolean = this.confirmRemoveUnlock,
    error: String? = this.error,
): NativeOwnerMoreState.Ready =
    copy(
        context = context,
        destination = destination,
        security = security,
        personalSecurity = personalSecurity.copy(
            saving = saving,
            confirmRemoveUnlock = confirmRemoveUnlock,
            error = error,
        ),
        productsState = productsState.copy(
            products = products,
            productsLoading = productsLoading,
            productsShowArchived = productsShowArchived,
            productDetail = productDetail,
            productCreateOpen = productCreateOpen,
            productCreating = productCreating,
            productEditOpen = productEditOpen,
            productUpdating = productUpdating,
            productArchiving = productArchiving,
            productUnarchiving = productUnarchiving,
            productsError = productsError,
        ),
        customersState = customersState.copy(
            customers = customers,
            customersLoading = customersLoading,
            customerDetail = customerDetail,
            customerCreateOpen = customerCreateOpen,
            customerCreating = customerCreating,
            customerEditOpen = customerEditOpen,
            customerUpdating = customerUpdating,
            customersError = customersError,
        ),
        suppliersState = suppliersState.copy(
            suppliers = suppliers,
            suppliersLoading = suppliersLoading,
            supplierDetail = supplierDetail,
            supplierCreateOpen = supplierCreateOpen,
            supplierCreating = supplierCreating,
            supplierEditOpen = supplierEditOpen,
            supplierUpdating = supplierUpdating,
            suppliersError = suppliersError,
        ),
        employeesState = employeesState.copy(
            employees = employees,
            employeesLoading = employeesLoading,
            employeeDetail = employeeDetail,
            employeeCreateOpen = employeeCreateOpen,
            employeeCreating = employeeCreating,
            employeeEditOpen = employeeEditOpen,
            employeeUpdating = employeeUpdating,
            employeesError = employeesError,
        ),
        costingState = costingState.copy(
            costingPolicies = costingPolicies,
            costingPoliciesLoading = costingPoliciesLoading,
            costingPolicyDetail = costingPolicyDetail,
            costingPoliciesError = costingPoliciesError,
            costingCurrentMonthSummary = costingCurrentMonthSummary,
            costingSection = costingSection,
            costingBudgets = costingBudgets,
            costingTargets = costingTargets,
            costingBudgetCategories = costingBudgetCategories,
            costingDefaultMilkWeekStart = costingDefaultMilkWeekStart,
            costingDefaultOperatingMonthStart = costingDefaultOperatingMonthStart,
            costingBudgetEditorOpen = costingBudgetEditorOpen,
            costingBudgetSaving = costingBudgetSaving,
            costingCollectionsEditorOpen = costingCollectionsEditorOpen,
            costingCollectionsSaving = costingCollectionsSaving,
            costingOpeningCosts = costingOpeningCosts,
            costingOpeningSummary = costingOpeningSummary,
            costingOpeningBasisKinds = costingOpeningBasisKinds,
            costingOpeningEditor = costingOpeningEditor,
            costingOpeningSaving = costingOpeningSaving,
            costingYieldPolicies = costingYieldPolicies,
            costingYieldBatches = costingYieldBatches,
            costingYieldActivePolicyId = costingYieldActivePolicyId,
            costingYieldSummary = costingYieldSummary,
            costingYieldApproveOpen = costingYieldApproveOpen,
            costingYieldApproving = costingYieldApproving,
            costingApproveOpen = costingApproveOpen,
            costingApproveSeed = costingApproveSeed,
            costingApproving = costingApproving,
            costingReauthOpen = costingReauthOpen,
            costingReauthBusy = costingReauthBusy,
            costingReviewReasons = costingReviewReasons,
            costingComponentCodes = costingComponentCodes,
        ),
        shopSettingsState = shopSettingsState.copy(
            shopSettings = shopSettings,
            shopSettingsLoading = shopSettingsLoading,
            shopSettingsIdentityEditOpen = shopSettingsIdentityEditOpen,
            shopSettingsIdentitySaving = shopSettingsIdentitySaving,
            shopSettingsRatesEditOpen = shopSettingsRatesEditOpen,
            shopSettingsRatesSaving = shopSettingsRatesSaving,
            shopSettingsCategoryCreateOpen = shopSettingsCategoryCreateOpen,
            shopSettingsCategoryCreating = shopSettingsCategoryCreating,
            shopSettingsCategoryDetail = shopSettingsCategoryDetail,
            shopSettingsCategoryArchiving = shopSettingsCategoryArchiving,
            shopSettingsError = shopSettingsError,
            shopSettingsOpeningEditOpen = shopSettingsOpeningEditOpen,
            shopSettingsOpeningCorrection = shopSettingsOpeningCorrection,
            shopSettingsOpeningSaving = shopSettingsOpeningSaving,
            shopSettingsOpeningReauthOpen = shopSettingsOpeningReauthOpen,
            shopSettingsOpeningReauthBusy = shopSettingsOpeningReauthBusy,
        ),
        usersRolesState = usersRolesState.copy(
            users = users,
            usersLoading = usersLoading,
            userDetail = userDetail,
            userEnrollOpen = userEnrollOpen,
            userEnrollPending = userEnrollPending,
            userEnrollBusy = userEnrollBusy,
            userRoleChangeOpen = userRoleChangeOpen,
            userRoleChanging = userRoleChanging,
            userEnableConfirmOpen = userEnableConfirmOpen,
            userEnableTargetEnabled = userEnableTargetEnabled,
            userEnableBusy = userEnableBusy,
            userReauthOpen = userReauthOpen,
            userReauthBusy = userReauthBusy,
            enrollableRoles = enrollableRoles,
            usersError = usersError,
        ),
        securityHistoryState = securityHistoryState.copy(
            securityAudits = securityAudits,
            securityHistoryLoading = securityHistoryLoading,
            securityAuditDetail = securityAuditDetail,
            securityHistoryError = securityHistoryError,
        ),
        backupState = backupState.copy(
            backup = backup,
            backupLoading = backupLoading,
            backupError = backupError,
            backupExportOpen = backupExportOpen,
            backupRestoreOpen = backupRestoreOpen,
            backupConnectOpen = backupConnectOpen,
            backupInspection = backupInspection,
            backupBusy = backupBusy,
            backupReauthOpen = backupReauthOpen,
            backupReauthBusy = backupReauthBusy,
            backupDisconnectConfirmOpen = backupDisconnectConfirmOpen,
            backupCloudOpen = backupCloudOpen,
        ),
    )
