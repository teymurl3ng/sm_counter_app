package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.NativePersonalSecurityPanel
import com.saaf.milkshop.nativeui.common.NativePersonalSecurityTags
import androidx.compose.foundation.layout.size
import com.saaf.milkshop.nativeui.theme.nativeNavigationTitleTextStyle
import com.saaf.milkshop.nativeui.theme.nativeGroupLabelTextStyle
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeValueTextStyle
import com.saaf.milkshop.nativeui.theme.nativeVisualColors

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.sync.NativeSyncOwnerChoice

/**
 * Owner More — leadership business-control menu under OwnerScaffold.
 * Reuses Cashier/Ops More menu-row and personal-security patterns; never mounts
 * Cashier or Ops More shells.
 */
@Composable
fun OwnerMoreScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState,
    onRetry: () -> Unit,
    onOpenPersonalSecurity: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCostingPlans: () -> Unit,
    onOpenShopSettings: () -> Unit,
    onOpenUsersRoles: () -> Unit,
    onOpenSecurityHistory: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSync: () -> Unit,
    onBackToMenu: () -> Unit,
    onRetryProducts: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onBackFromProductDetail: () -> Unit,
    onOpenProductCreate: () -> Unit,
    onBackFromProductCreate: () -> Unit,
    onCreateProduct: (ProductCatalogInput) -> Unit,
    onOpenProductEdit: () -> Unit,
    onBackFromProductEdit: () -> Unit,
    onUpdateProduct: (ProductCatalogInput) -> Unit,
    onArchiveProduct: () -> Unit,
    onUnarchiveProduct: () -> Unit,
    onProductsShowArchivedChange: (Boolean) -> Unit,
    onRetryCustomers: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onBackFromCustomerDetail: () -> Unit,
    onOpenCustomerCreate: () -> Unit,
    onBackFromCustomerCreate: () -> Unit,
    onCreateCustomer: (String) -> Unit,
    onOpenCustomerEdit: () -> Unit,
    onBackFromCustomerEdit: () -> Unit,
    onUpdateCustomer: (String, CustomerProfileInput) -> Unit,
    onRetrySuppliers: () -> Unit,
    onOpenSupplier: (Long) -> Unit,
    onBackFromSupplierDetail: () -> Unit,
    onOpenSupplierCreate: () -> Unit,
    onBackFromSupplierCreate: () -> Unit,
    onCreateSupplier: (String) -> Unit,
    onOpenSupplierEdit: () -> Unit,
    onBackFromSupplierEdit: () -> Unit,
    onUpdateSupplier: (String, VendorProfileInput) -> Unit,
    onRetryEmployees: () -> Unit,
    onOpenEmployee: (Long) -> Unit,
    onBackFromEmployeeDetail: () -> Unit,
    onOpenEmployeeCreate: () -> Unit,
    onBackFromEmployeeCreate: () -> Unit,
    onCreateEmployee: (String) -> Unit,
    onOpenEmployeeEdit: () -> Unit,
    onBackFromEmployeeEdit: () -> Unit,
    onUpdateEmployee: (String, EmployeeProfileInput) -> Unit,
    costingActions: OwnerCostingPlansActions = OwnerCostingPlansActions(),
    onRetryShopSettings: () -> Unit,
    onOpenShopIdentityEdit: () -> Unit,
    onBackFromShopIdentityEdit: () -> Unit,
    onSaveShopIdentity: (StoreIdentityInput) -> Unit,
    onOpenShopRatesEdit: () -> Unit,
    onBackFromShopRatesEdit: () -> Unit,
    onUpdateShopRates: (BusinessSettings) -> Unit,
    onOpenShopCategoryCreate: () -> Unit,
    onBackFromShopCategoryCreate: () -> Unit,
    onCreateShopExpenseCategory: (String) -> Unit,
    onOpenShopCategory: (Long) -> Unit,
    onBackFromShopCategoryDetail: () -> Unit,
    onSetShopExpenseCategoryArchived: (Boolean) -> Unit,
    onOpenOpeningEdit: (Boolean) -> Unit = {},
    onBackFromOpeningEdit: () -> Unit = {},
    onSaveOpening: (com.saaf.milkshop.domain.OpeningStateInput) -> Unit = {},
    onSubmitOpeningReauth: (CharArray) -> Unit = {},
    onCancelOpeningReauth: () -> Unit = {},
    onRetryUsersRoles: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onBackFromUserDetail: () -> Unit,
    onOpenUserEnroll: () -> Unit,
    onBackFromUserEnroll: () -> Unit,
    onBeginUserEnroll: (String, String, String) -> Unit,
    onCompleteUserEnroll: (CharArray, CharArray) -> Unit,
    onOpenUserRoleChange: () -> Unit,
    onBackFromUserRoleChange: () -> Unit,
    onChangeUserRole: (String) -> Unit,
    onRequestSetUserEnabled: (Boolean) -> Unit,
    onConfirmSetUserEnabled: () -> Unit,
    onCancelSetUserEnabled: () -> Unit,
    onBeginCredentialReset: () -> Unit = {},
    onResumeCredentialSetup: () -> Unit = {},
    onSubmitUsersRolesReauth: (CharArray) -> Unit,
    onCancelUsersRolesReauth: () -> Unit,
    onRetrySecurityHistory: () -> Unit,
    onOpenSecurityAudit: (Long) -> Unit,
    onBackFromSecurityAuditDetail: () -> Unit,
    onRetryBackup: () -> Unit,
    onOpenBackupExport: () -> Unit,
    onBackFromBackupExport: () -> Unit,
    onExportBackup: (android.net.Uri, CharArray) -> Unit,
    onOpenBackupRestore: () -> Unit,
    onBackFromBackupRestore: () -> Unit,
    onInspectBackupRestore: (android.net.Uri, CharArray) -> Unit,
    onConfirmBackupRestore: (Boolean) -> Unit,
    onOpenBackupConnect: () -> Unit,
    onRefreshSync: () -> Unit,
    onEnrollSync: (String, CharArray, String, Long?) -> Unit,
    onEnrollSyncInShop: (String, String, CharArray, String, Long?) -> Unit,
    ownerChoices: List<NativeSyncOwnerChoice> = emptyList(),
    onEnrollAs: (Long, String, CharArray, String) -> Unit = { _, _, _, _ -> },
    onSyncNow: () -> Unit,
    onResolveHoldSync: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onUnenrollSync: () -> Unit,
    onBackFromBackupConnect: () -> Unit,
    onConnectDriveBackup: (CharArray, Boolean) -> Unit,
    onSetDriveBackupEnabled: (Boolean) -> Unit,
    onRunDriveBackupNow: () -> Unit,
    onRequestBackupDisconnect: () -> Unit,
    onConfirmBackupDisconnect: () -> Unit,
    onCancelBackupDisconnect: () -> Unit,
    onSubmitBackupReauth: (CharArray) -> Unit,
    onCancelBackupReauth: () -> Unit,
    onOpenBackupCloud: () -> Unit = {},
    onBackFromBackupCloud: () -> Unit = {},
    onRefreshCloudBackups: () -> Unit = {},
    onInspectCloudBackup: (String, CharArray?) -> Unit = { _, _ -> },
    onConfirmCloudBackupRestore: (Boolean) -> Unit = {},
    onRequestRemoveQuickUnlock: () -> Unit,
    onCancelRemoveQuickUnlock: () -> Unit,
    onRemoveQuickUnlock: (CharArray) -> Unit,
    onEnrollQuickPin: (CharArray, CharArray) -> Unit,
    onEnrollPhoneSecurity: (CharArray, CharArray?) -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
    syncAttention: OwnerSyncAttention? = null,
    onCustomerCreditAction: (com.saaf.milkshop.application.owner.CustomerCreditCommand) -> Unit = {},
    customerCommunication: CustomerCommunicationAccess = CustomerCommunicationAccess(),
    customerFollowUps: CustomerFollowUpAccess? = null,
    loadCustomerStatement: suspend (Long, com.saaf.milkshop.application.owner.CustomerStatementPeriod) -> com.saaf.milkshop.application.owner.CustomerStatement = { _, _ -> error("Statement loading is unavailable.") },
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeOwnerMoreState.Idle || it is NativeOwnerMoreState.Loading) NativeOwnerMoreState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeOwnerMoreState.Idle,
        NativeOwnerMoreState.Loading,
        -> hostedScaffold(snackbarHostState, modifier, { it() }) {
            NativeColdLoadingPane(
                label = "Loading…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .testTag(NativeUiTags.OwnerMoreLoading),
            )
        }

        is NativeOwnerMoreState.LoadFailure -> hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .testTag(NativeUiTags.OwnerMoreRoot),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = hapticAction(onRetry),
                    modifier = Modifier.testTag(NativeUiTags.OwnerMoreRetryLoad),
                ) {
                    Text("Try again")
                }
            }
        }

        is NativeOwnerMoreState.Ready -> NativeSectionTransition(
            targetState = state.destination,
            contentKey = { it },
        ) { destination ->
            when (destination) {
            NativeOwnerMoreDestination.Menu -> OwnerMoreMenuScreen(
                syncAttention = syncAttention,
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = state,
                onOpenPersonalSecurity = onOpenPersonalSecurity,
                onOpenProducts = onOpenProducts,
                onOpenCustomers = onOpenCustomers,
                onOpenSuppliers = onOpenSuppliers,
                onOpenEmployees = onOpenEmployees,
                onOpenCostingPlans = onOpenCostingPlans,
                onOpenShopSettings = onOpenShopSettings,
                onOpenUsersRoles = onOpenUsersRoles,
                onOpenSecurityHistory = onOpenSecurityHistory,
                onOpenBackup = onOpenBackup,
                onOpenSync = onOpenSync,
                onSwitchAccount = onSwitchAccount,
                onSignOut = onSignOut,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.PersonalSecurity -> OwnerMorePersonalSecurityScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = state,
                onBack = onBackToMenu,
                onRequestRemove = onRequestRemoveQuickUnlock,
                onCancelRemove = onCancelRemoveQuickUnlock,
                onRemove = onRemoveQuickUnlock,
                onEnrollPin = onEnrollQuickPin,
                onEnrollPhone = onEnrollPhoneSecurity,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.Products -> OwnerProductsScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                products = state.products,
                productsLoading = state.productsLoading,
                productsShowArchived = state.productsShowArchived,
                productDetail = state.productDetail,
                productCreateOpen = state.productCreateOpen,
                productCreating = state.productCreating,
                productEditOpen = state.productEditOpen,
                productUpdating = state.productUpdating,
                productArchiving = state.productArchiving,
                productUnarchiving = state.productUnarchiving,
                productsError = state.productsError,
                onBackToMenu = onBackToMenu,
                onRetry = onRetryProducts,
                onShowArchivedChange = onProductsShowArchivedChange,
                onOpenProduct = onOpenProduct,
                onBackFromDetail = onBackFromProductDetail,
                onOpenCreate = onOpenProductCreate,
                onBackFromCreate = onBackFromProductCreate,
                onCreateProduct = onCreateProduct,
                onOpenEdit = onOpenProductEdit,
                onBackFromEdit = onBackFromProductEdit,
                onUpdateProduct = onUpdateProduct,
                onArchiveProduct = onArchiveProduct,
                onUnarchiveProduct = onUnarchiveProduct,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.Customers -> OwnerCustomersScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                customers = state.customers,
                customersLoading = state.customersLoading,
                customerDetail = state.customerDetail,
                customerCreateOpen = state.customerCreateOpen,
                customerCreating = state.customerCreating,
                customerEditOpen = state.customerEditOpen,
                customerUpdating = state.customerUpdating,
                customersError = state.customersError,
                onBackToMenu = onBackToMenu,
                onRetry = onRetryCustomers,
                onOpenCustomer = onOpenCustomer,
                onBackFromDetail = onBackFromCustomerDetail,
                onOpenCreate = onOpenCustomerCreate,
                onBackFromCreate = onBackFromCustomerCreate,
                onCreateCustomer = onCreateCustomer,
                onOpenEdit = onOpenCustomerEdit,
                onBackFromEdit = onBackFromCustomerEdit,
                onUpdateCustomer = onUpdateCustomer,
                onCreditAction = onCustomerCreditAction,
                loadStatement = loadCustomerStatement,
                communication = customerCommunication,
                followUps = customerFollowUps,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.Suppliers -> OwnerSuppliersScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                suppliers = state.suppliers,
                suppliersLoading = state.suppliersLoading,
                supplierDetail = state.supplierDetail,
                supplierCreateOpen = state.supplierCreateOpen,
                supplierCreating = state.supplierCreating,
                supplierEditOpen = state.supplierEditOpen,
                supplierUpdating = state.supplierUpdating,
                suppliersError = state.suppliersError,
                onBackToMenu = onBackToMenu,
                onRetry = onRetrySuppliers,
                onOpenSupplier = onOpenSupplier,
                onBackFromDetail = onBackFromSupplierDetail,
                onOpenCreate = onOpenSupplierCreate,
                onBackFromCreate = onBackFromSupplierCreate,
                onCreateSupplier = onCreateSupplier,
                onOpenEdit = onOpenSupplierEdit,
                onBackFromEdit = onBackFromSupplierEdit,
                onUpdateSupplier = onUpdateSupplier,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.Employees -> OwnerEmployeesScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                employees = state.employees,
                employeesLoading = state.employeesLoading,
                employeeDetail = state.employeeDetail,
                employeeCreateOpen = state.employeeCreateOpen,
                employeeCreating = state.employeeCreating,
                employeeEditOpen = state.employeeEditOpen,
                employeeUpdating = state.employeeUpdating,
                employeesError = state.employeesError,
                onBackToMenu = onBackToMenu,
                onRetry = onRetryEmployees,
                onOpenEmployee = onOpenEmployee,
                onBackFromDetail = onBackFromEmployeeDetail,
                onOpenCreate = onOpenEmployeeCreate,
                onBackFromCreate = onBackFromEmployeeCreate,
                onCreateEmployee = onCreateEmployee,
                onOpenEdit = onOpenEmployeeEdit,
                onBackFromEdit = onBackFromEmployeeEdit,
                onUpdateEmployee = onUpdateEmployee,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.CostingPlans -> OwnerCostingPlansScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                policies = state.costingPolicies,
                policiesLoading = state.costingPoliciesLoading,
                policyDetail = state.costingPolicyDetail,
                policiesError = state.costingPoliciesError,
                currentMonthSummary = state.costingCurrentMonthSummary,
                section = state.costingSection,
                budgets = state.costingBudgets,
                targets = state.costingTargets,
                budgetCategories = state.costingBudgetCategories,
                defaultMilkWeekStart = state.costingDefaultMilkWeekStart,
                defaultOperatingMonthStart = state.costingDefaultOperatingMonthStart,
                budgetEditorOpen = state.costingBudgetEditorOpen,
                budgetSaving = state.costingBudgetSaving,
                collectionsEditorOpen = state.costingCollectionsEditorOpen,
                collectionsSaving = state.costingCollectionsSaving,
                openingCosts = state.costingOpeningCosts,
                openingSummary = state.costingOpeningSummary,
                openingBasisKinds = state.costingOpeningBasisKinds,
                openingEditor = state.costingOpeningEditor,
                openingSaving = state.costingOpeningSaving,
                yieldPolicies = state.costingYieldPolicies,
                yieldBatches = state.costingYieldBatches,
                yieldActivePolicyId = state.costingYieldActivePolicyId,
                yieldSummary = state.costingYieldSummary,
                yieldApproveOpen = state.costingYieldApproveOpen,
                yieldApproving = state.costingYieldApproving,
                lossSaving = state.costingLossSaving,
                lossClassifications = state.costingLossClassifications,
                lossReasons = state.costingLossReasons,
                approveOpen = state.costingApproveOpen,
                approveSeed = state.costingApproveSeed,
                approving = state.costingApproving,
                reauthOpen = state.costingReauthOpen,
                reauthBusy = state.costingReauthBusy,
                reviewReasons = state.costingReviewReasons,
                componentCodes = state.costingComponentCodes,
                onBackToMenu = onBackToMenu,
                onRetry = costingActions.onRetry,
                onSectionChange = costingActions.onSectionChange,
                onOpenPolicy = costingActions.onOpenPolicy,
                onBackFromDetail = costingActions.onBackFromDetail,
                onOpenApprove = costingActions.onOpenApprove,
                onOpenApproveFromDetail = costingActions.onOpenApproveFromDetail,
                onBackFromApprove = costingActions.onBackFromApprove,
                onApprovePolicy = costingActions.onApprovePolicy,
                onOpenBudgetEditor = costingActions.onOpenBudgetEditor,
                onBackFromBudgetEditor = costingActions.onBackFromBudgetEditor,
                onSaveBudget = costingActions.onSaveBudget,
                onCopyPreviousBudget = costingActions.onCopyPreviousBudget,
                onOpenCollectionsEditor = costingActions.onOpenCollectionsEditor,
                onBackFromCollectionsEditor = costingActions.onBackFromCollectionsEditor,
                onSaveCollectionsTarget = costingActions.onSaveCollectionsTarget,
                onOpenOpeningEditor = costingActions.onOpenOpeningEditor,
                onOpenShopSettings = onOpenShopSettings,
                onBackFromOpeningEditor = costingActions.onBackFromOpeningEditor,
                onSaveOpeningCost = costingActions.onSaveOpeningCost,
                onOpenYieldApprove = costingActions.onOpenYieldApprove,
                onBackFromYieldApprove = costingActions.onBackFromYieldApprove,
                onApproveYieldPolicy = costingActions.onApproveYieldPolicy,
                onRecordProductionLoss = costingActions.onRecordProductionLoss,
                onSubmitReauth = costingActions.onSubmitReauth,
                onCancelReauth = costingActions.onCancelReauth,
                periodEndExclusive = { category, start ->
                    // Matches BudgetManagementService period rules.
                    if (category == com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES) {
                        start.plusDays(7)
                    } else {
                        start.plusMonths(1)
                    }
                },
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.ShopSettings -> OwnerShopSettingsScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                snapshot = state.shopSettings,
                loading = state.shopSettingsLoading,
                error = state.shopSettingsError,
                identityEditOpen = state.shopSettingsIdentityEditOpen,
                identitySaving = state.shopSettingsIdentitySaving,
                ratesEditOpen = state.shopSettingsRatesEditOpen,
                ratesSaving = state.shopSettingsRatesSaving,
                categoryCreateOpen = state.shopSettingsCategoryCreateOpen,
                categoryCreating = state.shopSettingsCategoryCreating,
                categoryDetail = state.shopSettingsCategoryDetail,
                categoryArchiving = state.shopSettingsCategoryArchiving,
                onBackToMenu = onBackToMenu,
                onRetry = onRetryShopSettings,
                onOpenIdentityEdit = onOpenShopIdentityEdit,
                onBackFromIdentityEdit = onBackFromShopIdentityEdit,
                onSaveIdentity = onSaveShopIdentity,
                onOpenRatesEdit = onOpenShopRatesEdit,
                onBackFromRatesEdit = onBackFromShopRatesEdit,
                onUpdateRates = onUpdateShopRates,
                onOpenCategoryCreate = onOpenShopCategoryCreate,
                onBackFromCategoryCreate = onBackFromShopCategoryCreate,
                onCreateCategory = onCreateShopExpenseCategory,
                onOpenCategory = onOpenShopCategory,
                onBackFromCategoryDetail = onBackFromShopCategoryDetail,
                onSetCategoryArchived = onSetShopExpenseCategoryArchived,
                openingEditOpen = state.shopSettingsOpeningEditOpen,
                openingCorrection = state.shopSettingsOpeningCorrection,
                openingSaving = state.shopSettingsOpeningSaving,
                openingReauthOpen = state.shopSettingsOpeningReauthOpen,
                openingReauthBusy = state.shopSettingsOpeningReauthBusy,
                onOpenOpeningEdit = onOpenOpeningEdit,
                onBackFromOpeningEdit = onBackFromOpeningEdit,
                onSaveOpening = onSaveOpening,
                onSubmitOpeningReauth = onSubmitOpeningReauth,
                onCancelOpeningReauth = onCancelOpeningReauth,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.UsersRoles -> OwnerUsersRolesScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                users = state.users,
                usersLoading = state.usersLoading,
                userDetail = state.userDetail,
                userEnrollOpen = state.userEnrollOpen,
                userEnrollPending = state.userEnrollPending,
                userEnrollBusy = state.userEnrollBusy,
                userRoleChangeOpen = state.userRoleChangeOpen,
                userRoleChanging = state.userRoleChanging,
                userEnableConfirmOpen = state.userEnableConfirmOpen,
                userEnableBusy = state.userEnableBusy,
                userReauthOpen = state.userReauthOpen,
                userReauthBusy = state.userReauthBusy,
                usersError = state.usersError,
                enrollableRoles = state.enrollableRoles,
                onBackToMenu = onBackToMenu,
                onRetry = onRetryUsersRoles,
                onOpenUser = onOpenUser,
                onBackFromDetail = onBackFromUserDetail,
                onOpenEnroll = onOpenUserEnroll,
                onBackFromEnroll = onBackFromUserEnroll,
                onBeginEnroll = onBeginUserEnroll,
                onCompleteEnroll = onCompleteUserEnroll,
                onOpenRoleChange = onOpenUserRoleChange,
                onBackFromRoleChange = onBackFromUserRoleChange,
                onChangeRole = onChangeUserRole,
                onRequestSetEnabled = onRequestSetUserEnabled,
                onConfirmSetEnabled = onConfirmSetUserEnabled,
                onCancelSetEnabled = onCancelSetUserEnabled,
                onBeginCredentialReset = onBeginCredentialReset,
                onResumeCredentialSetup = onResumeCredentialSetup,
                onSubmitReauth = onSubmitUsersRolesReauth,
                onCancelReauth = onCancelUsersRolesReauth,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.SecurityHistory -> OwnerSecurityHistoryScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                audits = state.securityAudits,
                auditsLoading = state.securityHistoryLoading,
                auditDetail = state.securityAuditDetail,
                auditsError = state.securityHistoryError,
                onBackToMenu = onBackToMenu,
                onRetry = onRetrySecurityHistory,
                onOpenAudit = onOpenSecurityAudit,
                onBackFromDetail = onBackFromSecurityAuditDetail,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.Sync -> OwnerSyncScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                status = state.syncState.status,
                shopChoices = state.syncState.shopChoices,
                ownerChoices = state.syncState.ownerChoices,
                busy = state.syncState.busy,
                error = state.syncState.error,
                notice = state.syncState.notice,
                onRefresh = onRefreshSync,
                onEnroll = { username, chars, env -> onEnrollSync(username, chars, env, null) },
                onEnrollInShop = { shopId, username, chars, env, localUserId -> onEnrollSyncInShop(shopId, username, chars, env, localUserId) },
                onEnrollAs = onEnrollAs,
                onSyncNow = onSyncNow,
                holds = syncAttention?.holds ?: state.syncState.holds,
                reviewUnavailable = syncAttention?.let { it.unavailable || it.holdsUnavailable } ?: false,
                onResolveHold = onResolveHoldSync,
                onUnenroll = onUnenrollSync,
                allowDevEnvironment = com.saaf.milkshop.BuildConfig.DEBUG,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )

            NativeOwnerMoreDestination.Backup -> OwnerBackupScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                snapshot = state.backup,
                loading = state.backupLoading,
                error = state.backupError,
                exportOpen = state.backupExportOpen,
                restoreOpen = state.backupRestoreOpen,
                connectOpen = state.backupConnectOpen,
                inspection = state.backupInspection,
                busy = state.backupBusy,
                reauthOpen = state.backupReauthOpen,
                reauthBusy = state.backupReauthBusy,
                disconnectConfirmOpen = state.backupDisconnectConfirmOpen,
                cloudOpen = state.backupCloudOpen,
                onBackToMenu = onBackToMenu,
                onRetry = onRetryBackup,
                onOpenExport = onOpenBackupExport,
                onBackFromExport = onBackFromBackupExport,
                onExport = onExportBackup,
                onOpenRestore = onOpenBackupRestore,
                onBackFromRestore = onBackFromBackupRestore,
                onInspectRestore = onInspectBackupRestore,
                onConfirmRestore = onConfirmBackupRestore,
                onOpenConnect = onOpenBackupConnect,
                onBackFromConnect = onBackFromBackupConnect,
                onConnectDrive = onConnectDriveBackup,
                onSetDriveEnabled = onSetDriveBackupEnabled,
                onRunDriveNow = onRunDriveBackupNow,
                onRequestDisconnect = onRequestBackupDisconnect,
                onConfirmDisconnect = onConfirmBackupDisconnect,
                onCancelDisconnect = onCancelBackupDisconnect,
                onSubmitReauth = onSubmitBackupReauth,
                onCancelReauth = onCancelBackupReauth,
                onOpenCloud = onOpenBackupCloud,
                onBackFromCloud = onBackFromBackupCloud,
                onRefreshCloud = onRefreshCloudBackups,
                onInspectCloud = onInspectCloudBackup,
                onConfirmCloudRestore = onConfirmCloudBackupRestore,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            }
        }
    }
    }
}

@Composable
private fun OwnerMoreMenuScreen(
    syncAttention: OwnerSyncAttention?,
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState.Ready,
    onOpenPersonalSecurity: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCostingPlans: () -> Unit,
    onOpenShopSettings: () -> Unit,
    onOpenUsersRoles: () -> Unit,
    onOpenSecurityHistory: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSync: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerMoreRoot),
        ) {
            OwnerMoreHeader(identity, state.context)
            Column(
                Modifier
                    .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.OwnerMoreMenu),
            ) {
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OwnerMoreSectionLabel("Things you sell")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Inventory2,
                    title = "Products",
                    subtitle = "What you sell and how it is measured",
                    tag = NativeUiTags.OwnerMoreProducts,
                    onClick = hapticAction(onOpenProducts),
                )

                OwnerMoreSectionLabel("People and businesses")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.People,
                    title = "Customers",
                    subtitle = "Customer accounts and balances",
                    tag = NativeUiTags.OwnerMoreCustomers,
                    onClick = hapticAction(onOpenCustomers),
                )
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Business,
                    title = "Suppliers",
                    subtitle = "Supplier accounts and milk receipts",
                    tag = NativeUiTags.OwnerMoreSuppliers,
                    onClick = hapticAction(onOpenSuppliers),
                )
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Person,
                    title = "Employees",
                    subtitle = "Employee profiles and attendance",
                    tag = NativeUiTags.OwnerMoreEmployees,
                    onClick = hapticAction(onOpenEmployees),
                )

                OwnerMoreSectionLabel("Planning and setup")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Tune,
                    title = "Costing & plans",
                    subtitle = "Cost coverage and planning",
                    tag = NativeUiTags.OwnerMoreCosting,
                    onClick = hapticAction(onOpenCostingPlans),
                )
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Settings,
                    title = "Shop settings",
                    subtitle = "Store name, opening state, categories",
                    tag = NativeUiTags.OwnerMoreShopSettings,
                    onClick = hapticAction(onOpenShopSettings),
                )

                OwnerMoreSectionLabel("Your account and staff access")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = "Personal security",
                    subtitle = "Quick unlock for this account on this phone",
                    tag = NativeUiTags.OwnerMorePersonalSecurity,
                    onClick = hapticAction(onOpenPersonalSecurity),
                )
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.People,
                    title = "Users & roles",
                    subtitle = "Staff access and role assignment",
                    tag = NativeUiTags.OwnerMoreUsersRoles,
                    onClick = hapticAction(onOpenUsersRoles),
                )
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.History,
                    title = "Security history",
                    subtitle = "Audit of sensitive account actions",
                    tag = NativeUiTags.OwnerMoreSecurityHistory,
                    onClick = hapticAction(onOpenSecurityHistory),
                )

                OwnerMoreSectionLabel("Safety and recovery")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Backup,
                    title = "Backup & restore",
                    subtitle = "Local export, Drive backup, restore",
                    tag = NativeUiTags.OwnerMoreBackup,
                    onClick = hapticAction(onOpenBackup),
                )
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.CloudSync,
                    title = "Sync",
                    subtitle = syncAttention?.description ?: "Upload entries to your shop's online record",
                    tag = NativeUiTags.OwnerMoreSync,
                    onClick = hapticAction(onOpenSync),
                )

                OwnerMoreSectionLabel("Account")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch account",
                    subtitle = "Choose another person on this phone",
                    tag = NativeUiTags.OwnerMoreSwitchAccount,
                    onClick = hapticAction(onSwitchAccount),
                )
                OwnerMoreMenuRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = com.saaf.milkshop.nativeui.common.NativeAccessCopy.LOCK_TITLE,
                    subtitle = com.saaf.milkshop.nativeui.common.NativeAccessCopy.LOCK_DETAIL,
                    tag = NativeUiTags.OwnerMoreSignOut,
                    onClick = hapticAction(onSignOut),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OwnerMorePersonalSecurityScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState.Ready,
    onBack: () -> Unit,
    onRequestRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onRemove: (CharArray) -> Unit,
    onEnrollPin: (CharArray, CharArray) -> Unit,
    onEnrollPhone: (CharArray, CharArray?) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().testTag(NativeUiTags.OwnerMoreSecurityScreen),
        ) {
            OwnerMoreHeader(identity, state.context)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NativePersonalSecurityPanel(
                    identityKey = identity.userId,
                    identityName = identity.displayName,
                    security = state.security,
                    saving = state.saving,
                    error = state.personalSecurity.error,
                    confirmRemove = state.confirmRemoveUnlock,
                    tags = NativePersonalSecurityTags(
                        status = NativeUiTags.OwnerMoreSecurityStatus,
                        passphrase = NativeUiTags.OwnerMoreSecurityPassphrase,
                        pin = NativeUiTags.OwnerMoreSecurityPin,
                        savePin = NativeUiTags.OwnerMoreSecurityEnrollPin,
                        enablePhone = NativeUiTags.OwnerMoreSecurityEnrollPhone,
                        remove = NativeUiTags.OwnerMoreSecurityRemove,
                        confirmRemove = NativeUiTags.OwnerMoreSecurityRemoveConfirm,
                        error = NativeUiTags.OwnerMoreError,
                    ),
                    onRequestRemove = onRequestRemove,
                    onCancelRemove = onCancelRemove,
                    onRemove = onRemove,
                    onEnrollPin = onEnrollPin,
                    onEnrollPhone = onEnrollPhone,
                )
            }
        }
    }
}
@Composable
private fun OwnerMoreHeader(identity: NativeSignedInIdentity, context: NativeOwnerMoreContext) {
    CashierScreenHeader(
        title = "More",
        displayName = context.shopName,
        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
        businessDate = context.businessDate,
        postingOpen = context.postingOpen,
    )
}

@Composable
internal fun OwnerMoreSectionLabel(label: String) {
    Text(
        label,
        style = nativeGroupLabelTextStyle(),
        color = nativeVisualColors.supporting,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag(NativeUiTags.OwnerMoreSectionLabel),
    )
}

@Composable
internal fun OwnerMoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Navigation)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Navigation)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = nativeNavigationTitleTextStyle())
            Text(
                subtitle,
                style = nativeSupportingTextStyle(),
                color = nativeVisualColors.supporting,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}
