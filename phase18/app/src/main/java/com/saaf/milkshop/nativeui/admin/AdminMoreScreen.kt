package com.saaf.milkshop.nativeui.admin

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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import android.net.Uri
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.owner.NativeOwnerCostingSection
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.OwnerBackupScreen
import com.saaf.milkshop.nativeui.owner.OwnerSyncScreen
import com.saaf.milkshop.nativeui.owner.OwnerCostingPlansActions
import com.saaf.milkshop.nativeui.owner.OwnerCostingPlansScreen
import com.saaf.milkshop.nativeui.owner.OwnerProductsScreen
import com.saaf.milkshop.nativeui.owner.OwnerShopSettingsScreen
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Admin More — system tools + local encrypted export under AdminScaffold.
 * Omits Owner Overview, Drive/restore/recovery, and privileged-user controls.
 */
@Composable
fun AdminMoreScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState,
    onRetry: () -> Unit,
    onOpenPersonalSecurity: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCostingPlans: () -> Unit,
    onOpenShopSettings: () -> Unit,
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
    onRetryBackup: () -> Unit,
    onOpenBackupExport: () -> Unit,
    onBackFromBackupExport: () -> Unit,
    onExportBackup: (Uri, CharArray) -> Unit,
    onRequestRemoveQuickUnlock: () -> Unit,
    onCancelRemoveQuickUnlock: () -> Unit,
    onRemoveQuickUnlock: (CharArray) -> Unit,
    onEnrollQuickPin: (CharArray, CharArray) -> Unit,
    onEnrollPhoneSecurity: (CharArray, CharArray?) -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    admScaffold: AdmScaffold,
    modifier: Modifier = Modifier,
) {
    val adminMoreShell: HostedScaffold = { snackbar, mod, _, content ->
        admScaffold(snackbar, mod, content)
    }

    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeOwnerMoreState.Idle || it is NativeOwnerMoreState.Loading) NativeOwnerMoreState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeOwnerMoreState.Idle,
        NativeOwnerMoreState.Loading,
        -> admScaffold(snackbarHostState, modifier) {
            NativeColdLoadingPane(
                label = "Loading…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .testTag(NativeUiTags.AdminMoreLoading),
            )
        }

        is NativeOwnerMoreState.LoadFailure -> admScaffold(snackbarHostState, modifier) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .testTag(NativeUiTags.AdminMoreRoot),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = hapticAction(onRetry),
                    modifier = Modifier.testTag(NativeUiTags.AdminMoreRetryLoad),
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
            NativeOwnerMoreDestination.Menu -> AdminMoreMenuScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = state,
                onOpenPersonalSecurity = onOpenPersonalSecurity,
                onOpenProducts = onOpenProducts,
                onOpenCostingPlans = onOpenCostingPlans,
                onOpenShopSettings = onOpenShopSettings,
                onOpenBackup = onOpenBackup,
                onSwitchAccount = onSwitchAccount,
                onSignOut = onSignOut,
                admScaffold = admScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.PersonalSecurity -> AdminMorePersonalSecurityScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = state,
                onBack = onBackToMenu,
                onRequestRemove = onRequestRemoveQuickUnlock,
                onCancelRemove = onCancelRemoveQuickUnlock,
                onRemove = onRemoveQuickUnlock,
                onEnrollPin = onEnrollQuickPin,
                onEnrollPhone = onEnrollPhoneSecurity,
                admScaffold = admScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.Products -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminProductsRoot),
            ) {
                OwnerProductsScreen(
                    showHeader = false,
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
                    hostedScaffold = adminMoreShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.CostingPlans -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminCostingPlansRoot),
            ) {
                OwnerCostingPlansScreen(
                    showHeader = false,
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
                    onSubmitReauth = costingActions.onSubmitReauth,
                    onCancelReauth = costingActions.onCancelReauth,
                    hostedScaffold = adminMoreShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.ShopSettings -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminShopSettingsRoot),
            ) {
                OwnerShopSettingsScreen(
                    showHeader = false,
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
                    hostedScaffold = adminMoreShell,
                    modifier = modifier,
                )
            }

            NativeOwnerMoreDestination.Backup -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminLocalExportRoot),
            ) {
                OwnerBackupScreen(
                    showHeader = false,
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    context = state.context,
                    snapshot = state.backup,
                    loading = state.backupLoading,
                    error = state.backupError,
                    exportOpen = state.backupExportOpen,
                    restoreOpen = false,
                    connectOpen = false,
                    inspection = null,
                    busy = state.backupBusy,
                    reauthOpen = false,
                    reauthBusy = false,
                    disconnectConfirmOpen = false,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetryBackup,
                    onOpenExport = onOpenBackupExport,
                    onBackFromExport = onBackFromBackupExport,
                    onExport = onExportBackup,
                    onOpenRestore = {},
                    onBackFromRestore = {},
                    onInspectRestore = { _, _ -> },
                    onConfirmRestore = {},
                    onOpenConnect = {},
                    onBackFromConnect = {},
                    onConnectDrive = { _, _ -> },
                    onSetDriveEnabled = {},
                    onRunDriveNow = {},
                    onRequestDisconnect = {},
                    onConfirmDisconnect = {},
                    onCancelDisconnect = {},
                    onSubmitReauth = {},
                    onCancelReauth = {},
                    hostedScaffold = adminMoreShell,
                    localExportOnly = true,
                    modifier = modifier,
                )
            }
            else -> {
                BackHandler(onBack = onBackToMenu)
                AdminMoreMenuScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = state.copy(destination = NativeOwnerMoreDestination.Menu),
                    onOpenPersonalSecurity = onOpenPersonalSecurity,
                    onOpenProducts = onOpenProducts,
                    onOpenCostingPlans = onOpenCostingPlans,
                    onOpenShopSettings = onOpenShopSettings,
                    onOpenBackup = onOpenBackup,
                    onSwitchAccount = onSwitchAccount,
                    onSignOut = onSignOut,
                    admScaffold = admScaffold,
                    modifier = modifier,
                )
            }
            }
        }
    }
    }
}

@Composable
private fun AdminMoreMenuScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState.Ready,
    onOpenPersonalSecurity: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCostingPlans: () -> Unit,
    onOpenShopSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    admScaffold: AdmScaffold,
    modifier: Modifier,
) {
    admScaffold(snackbarHostState, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.AdminMoreRoot),
        ) {
            AdminMoreHeader(identity, state.context)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .nativeRefreshOverlay(state.refreshing)
                    .testTag(NativeUiTags.AdminMoreMenu),
            ) {
                AdminMoreSectionLabel("System tools")
                AdminMoreMenuRow(
                    icon = Icons.Outlined.Tune,
                    title = "Costing & plans",
                    subtitle = "Review and approve conversion costing",
                    tag = NativeUiTags.AdminMoreCosting,
                    onClick = hapticAction(onOpenCostingPlans),
                )
                AdminMoreMenuRow(
                    icon = Icons.Outlined.Settings,
                    title = "Shop settings",
                    subtitle = "Identity, rates, and expense categories",
                    tag = NativeUiTags.AdminMoreShopSettings,
                    onClick = hapticAction(onOpenShopSettings),
                )
                AdminMoreMenuRow(
                    icon = Icons.Outlined.Inventory2,
                    title = "Products",
                    subtitle = "Catalog and measurement units",
                    tag = NativeUiTags.AdminMoreProducts,
                    onClick = hapticAction(onOpenProducts),
                )
                AdminMoreMenuRow(
                    icon = Icons.Outlined.Backup,
                    title = "Local export",
                    subtitle = "Encrypted backup file on this phone",
                    tag = NativeUiTags.AdminMoreLocalExport,
                    onClick = hapticAction(onOpenBackup),
                )

                AdminMoreSectionLabel("Account")
                AdminMoreMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = "Personal security",
                    subtitle = "Quick unlock for this account on this phone",
                    tag = NativeUiTags.AdminMorePersonalSecurity,
                    onClick = hapticAction(onOpenPersonalSecurity),
                )
                AdminMoreMenuRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch account",
                    subtitle = "Choose another person on this phone",
                    tag = NativeUiTags.AdminMoreSwitchAccount,
                    onClick = hapticAction(onSwitchAccount),
                )
                AdminMoreMenuRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = com.saaf.milkshop.nativeui.common.NativeAccessCopy.LOCK_TITLE,
                    subtitle = com.saaf.milkshop.nativeui.common.NativeAccessCopy.LOCK_DETAIL,
                    tag = NativeUiTags.AdminMoreSignOut,
                    onClick = hapticAction(onSignOut),
                )
            }
        }
    }
}

@Composable
internal fun AdminMorePersonalSecurityScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState.Ready,
    onBack: () -> Unit,
    onRequestRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onRemove: (CharArray) -> Unit,
    onEnrollPin: (CharArray, CharArray) -> Unit,
    onEnrollPhone: (CharArray, CharArray?) -> Unit,
    admScaffold: AdmScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    admScaffold(snackbarHostState, modifier) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().testTag(NativeUiTags.AdminMoreSecurityScreen),
        ) {
            AdminMoreHeader(identity, state.context)
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
                        status = NativeUiTags.AdminMoreSecurityStatus,
                        passphrase = NativeUiTags.AdminMoreSecurityPassphrase,
                        pin = NativeUiTags.AdminMoreSecurityPin,
                        savePin = NativeUiTags.AdminMoreSecurityEnrollPin,
                        enablePhone = NativeUiTags.AdminMoreSecurityEnrollPhone,
                        remove = NativeUiTags.AdminMoreSecurityRemove,
                        confirmRemove = NativeUiTags.AdminMoreSecurityRemoveConfirm,
                        error = NativeUiTags.AdminMoreSecurityError,
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
private fun AdminMoreHeader(identity: NativeSignedInIdentity, context: NativeOwnerMoreContext) {
    CashierScreenHeader(
        title = "More",
        displayName = context.shopName,
        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
        businessDate = context.businessDate,
        postingOpen = context.postingOpen,
    )
}

@Composable
internal fun AdminMoreSectionLabel(label: String) {
    Text(
        label,
        style = nativeGroupLabelTextStyle(),
        color = nativeVisualColors.supporting,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag(NativeUiTags.AdminMoreSectionLabel),
    )
}

@Composable
internal fun AdminMoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Navigation)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
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
