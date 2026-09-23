package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.saaf.milkshop.nativeui.common.NativeSecondaryTransition
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

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
    onNavigate: (OwnerSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeOwnerMoreState.Idle,
        NativeOwnerMoreState.Loading,
        -> OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .testTag(NativeUiTags.OwnerMoreLoading),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is NativeOwnerMoreState.LoadFailure -> OwnerScaffold(
            snackbarHostState,
            OwnerSection.More,
            onNavigate,
            modifier,
        ) { padding ->
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
                    onClick = onRetry,
                    modifier = Modifier.testTag(NativeUiTags.OwnerMoreRetryLoad),
                ) {
                    Text("Try again")
                }
            }
        }

        is NativeOwnerMoreState.Ready -> NativeSecondaryTransition(state.destination) { destination ->
            when (destination) {
            NativeOwnerMoreDestination.Menu -> OwnerMoreMenuScreen(
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
                onSwitchAccount = onSwitchAccount,
                onSignOut = onSignOut,
                onNavigate = onNavigate,
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
                onNavigate = onNavigate,
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                onSubmitReauth = onSubmitUsersRolesReauth,
                onCancelReauth = onCancelUsersRolesReauth,
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
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
                hostedScaffold = { snackbar, mod, wrapNav, content ->
                    OwnerScaffold(
                        snackbarHostState = snackbar,
                        section = OwnerSection.More,
                        onNavigate = { section -> wrapNav { onNavigate(section) } },
                        modifier = mod,
                        content = content,
                    )
                },
                modifier = modifier,
            )
            }
        }
    }
}

@Composable
private fun OwnerMoreMenuScreen(
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
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerMoreRoot),
        ) {
            OwnerMoreHeader(identity, state.context)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag(NativeUiTags.OwnerMoreMenu),
            ) {
                OwnerMoreSectionLabel("Things you sell")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Inventory2,
                    title = "Products",
                    subtitle = "What you sell and how it is measured",
                    tag = NativeUiTags.OwnerMoreProducts,
                    onClick = onOpenProducts,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OwnerMoreSectionLabel("People and businesses")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.People,
                    title = "Customers",
                    subtitle = "Customer accounts and balances",
                    tag = NativeUiTags.OwnerMoreCustomers,
                    onClick = onOpenCustomers,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Business,
                    title = "Suppliers",
                    subtitle = "Supplier accounts and milk receipts",
                    tag = NativeUiTags.OwnerMoreSuppliers,
                    onClick = onOpenSuppliers,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Person,
                    title = "Employees",
                    subtitle = "Employee profiles and attendance",
                    tag = NativeUiTags.OwnerMoreEmployees,
                    onClick = onOpenEmployees,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OwnerMoreSectionLabel("Planning and setup")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Tune,
                    title = "Costing & plans",
                    subtitle = "Cost coverage and planning",
                    tag = NativeUiTags.OwnerMoreCosting,
                    onClick = onOpenCostingPlans,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Settings,
                    title = "Shop settings",
                    subtitle = "Store name, opening state, categories",
                    tag = NativeUiTags.OwnerMoreShopSettings,
                    onClick = onOpenShopSettings,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OwnerMoreSectionLabel("Your account and staff access")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = "Personal security",
                    subtitle = "Quick unlock for this account on this phone",
                    tag = NativeUiTags.OwnerMorePersonalSecurity,
                    onClick = onOpenPersonalSecurity,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.People,
                    title = "Users & roles",
                    subtitle = "Staff access and role assignment",
                    tag = NativeUiTags.OwnerMoreUsersRoles,
                    onClick = onOpenUsersRoles,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.History,
                    title = "Security history",
                    subtitle = "Audit of sensitive account actions",
                    tag = NativeUiTags.OwnerMoreSecurityHistory,
                    onClick = onOpenSecurityHistory,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OwnerMoreSectionLabel("Safety and recovery")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.Backup,
                    title = "Backup & restore",
                    subtitle = "Local export, Drive backup, restore",
                    tag = NativeUiTags.OwnerMoreBackup,
                    onClick = onOpenBackup,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OwnerMoreSectionLabel("Account")
                OwnerMoreMenuRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch account",
                    subtitle = "Choose another person on this phone",
                    tag = NativeUiTags.OwnerMoreSwitchAccount,
                    onClick = onSwitchAccount,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OwnerMoreMenuRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "Sign out",
                    subtitle = "Return to Welcome",
                    tag = NativeUiTags.OwnerMoreSignOut,
                    onClick = onSignOut,
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
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val security = state.security
    var passphrase by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    if (state.confirmRemoveUnlock) {
        AlertDialog(
            onDismissRequest = onCancelRemove,
            title = { Text("Remove Quick unlock?") },
            text = {
                Text("This clears Quick PIN and phone security for ${identity.displayName} on this phone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val secret = passphrase.toCharArray()
                        passphrase = ""
                        pin = ""
                        onRemove(secret)
                    },
                    modifier = Modifier.testTag(NativeUiTags.OwnerMoreSecurityRemoveConfirm),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = onCancelRemove) { Text("Keep") }
            },
        )
    }

    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag(NativeUiTags.OwnerMoreSecurityScreen),
        ) {
            OwnerMoreHeader(identity, state.context)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(NativeUiTags.OwnerMoreBack),
                ) {
                    Text("Back to More")
                }
                Text("Personal security", style = MaterialTheme.typography.headlineMedium)
                if (security == null) {
                    CircularProgressIndicator()
                } else {
                    OwnerMoreSecurityStatus(security)
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerMoreSecurityPassphrase),
                        label = { Text("Current full passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerMoreSecurityPin),
                        label = { Text("New Quick PIN (4 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerMoreError),
                        )
                    }
                    Button(
                        onClick = {
                            if (passphrase.isBlank() || pin.length != 4) return@Button
                            val secret = passphrase.toCharArray()
                            val quickPin = pin.toCharArray()
                            passphrase = ""
                            pin = ""
                            onEnrollPin(secret, quickPin)
                        },
                        enabled = !state.saving && passphrase.isNotBlank() && pin.length == 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerMoreSecurityEnrollPin),
                    ) { Text(if (state.saving) "Saving…" else "Save Quick PIN") }
                    if (security.phoneSecurityAvailable) {
                        Button(
                            onClick = {
                                if (passphrase.isBlank()) return@Button
                                val secret = passphrase.toCharArray()
                                val quickPin = pin.takeIf { it.length == 4 }?.toCharArray()
                                passphrase = ""
                                pin = ""
                                onEnrollPhone(secret, quickPin)
                            },
                            enabled = !state.saving && passphrase.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OwnerMoreSecurityEnrollPhone),
                        ) { Text("Enable ${security.phoneSecurityLabel}") }
                    }
                    if (security.quickPinEnabled || security.phoneSecurityEnabled) {
                        TextButton(
                            onClick = onRequestRemove,
                            enabled = !state.saving && passphrase.isNotBlank(),
                            modifier = Modifier.testTag(NativeUiTags.OwnerMoreSecurityRemove),
                        ) { Text("Remove Quick unlock") }
                    }
                }
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
private fun OwnerMoreSectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(NativeUiTags.OwnerMoreSectionLabel),
    )
}

@Composable
private fun OwnerMoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun OwnerMoreSecurityStatus(security: NativeMoreSecurityStatus) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OwnerMoreSecurityStatus),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Quick PIN: ${if (security.quickPinEnabled) "On" else "Off"}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "${security.phoneSecurityLabel}: ${
                when {
                    !security.phoneSecurityAvailable -> "Unavailable on this phone"
                    security.phoneSecurityEnabled -> "On"
                    else -> "Off"
                }
            }",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
