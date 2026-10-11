package com.saaf.milkshop.nativeui.admin

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.OwnerCustomersScreen
import com.saaf.milkshop.nativeui.owner.OwnerEmployeesScreen
import com.saaf.milkshop.nativeui.owner.OwnerSuppliersScreen
import com.saaf.milkshop.nativeui.owner.OwnerUsersRolesScreen
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader

/**
 * Admin People — directory hub (staff access + employees/customers/suppliers)
 * under AdminScaffold(People). Reuses catalog / users content via HostedScaffold.
 * Never mounts OwnerMoreScreen or Owner Overview.
 */
@Composable
fun AdminPeopleScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState,
    onRetry: () -> Unit,
    onOpenUsersRoles: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenEmployees: () -> Unit,
    onBackToMenu: () -> Unit,
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
    onSubmitUsersRolesReauth: (CharArray) -> Unit,
    onCancelUsersRolesReauth: () -> Unit,
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
    admScaffold: AdmScaffold,
    modifier: Modifier = Modifier,
) {
    val adminPeopleShell: HostedScaffold = { snackbar, mod, _, content ->
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
                    .testTag(NativeUiTags.AdminPeopleLoading),
            )
        }

        is NativeOwnerMoreState.LoadFailure -> admScaffold(snackbarHostState, modifier) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .testTag(NativeUiTags.AdminPeopleRoot),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = hapticAction(onRetry),
                    modifier = Modifier.testTag(NativeUiTags.AdminPeopleRetryLoad),
                ) {
                    Text("Try again")
                }
            }
        }

        is NativeOwnerMoreState.Ready -> NativeSectionTransition(
            targetState = state.destination,
            contentKey = { it },
        ) { destination -> when (destination) {
            NativeOwnerMoreDestination.Menu -> AdminPeopleHubScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = state.context,
                onOpenUsersRoles = onOpenUsersRoles,
                onOpenCustomers = onOpenCustomers,
                onOpenSuppliers = onOpenSuppliers,
                onOpenEmployees = onOpenEmployees,
                admScaffold = admScaffold,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.UsersRoles -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminUsersRolesRoot),
            ) {
                OwnerUsersRolesScreen(
                    showHeader = false,
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
                    onSubmitReauth = onSubmitUsersRolesReauth,
                    onCancelReauth = onCancelUsersRolesReauth,
                    hostedScaffold = adminPeopleShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.Customers -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminCustomersRoot),
            ) {
                OwnerCustomersScreen(
                    showHeader = false,
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
                    hostedScaffold = adminPeopleShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.Suppliers -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminSuppliersRoot),
            ) {
                OwnerSuppliersScreen(
                    showHeader = false,
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
                    hostedScaffold = adminPeopleShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.Employees -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.AdminEmployeesRoot),
            ) {
                OwnerEmployeesScreen(
                    showHeader = false,
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
                    hostedScaffold = adminPeopleShell,
                    modifier = modifier,
                )
            }
            else -> {
                // Owner-only More destinations are unreachable from Admin People hub.
                BackHandler(onBack = onBackToMenu)
                AdminPeopleHubScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    context = state.context,
                    onOpenUsersRoles = onOpenUsersRoles,
                    onOpenCustomers = onOpenCustomers,
                    onOpenSuppliers = onOpenSuppliers,
                    onOpenEmployees = onOpenEmployees,
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
private fun AdminPeopleHubScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    onOpenUsersRoles: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenEmployees: () -> Unit,
    admScaffold: AdmScaffold,
    modifier: Modifier,
) {
    admScaffold(snackbarHostState, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.AdminPeopleRoot),
        ) {
            CashierScreenHeader(
                title = "People",
                displayName = context.shopName,
                roleLabel = "Admin",
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.AdminPeopleHub),
            ) {
                AdminPeopleSectionLabel("Staff access")
                AdminPeopleMenuRow(
                    icon = Icons.Outlined.ManageAccounts,
                    title = "Users & roles",
                    subtitle = "Login accounts, enrollment, and access state",
                    tag = NativeUiTags.AdminPeopleUsersRoles,
                    onClick = hapticAction(onOpenUsersRoles),
                )

                AdminPeopleSectionLabel("People and businesses")
                AdminPeopleMenuRow(
                    icon = Icons.Outlined.Person,
                    title = "Employees",
                    subtitle = "Employee profiles and attendance accounts",
                    tag = NativeUiTags.AdminPeopleEmployees,
                    onClick = hapticAction(onOpenEmployees),
                )
                AdminPeopleMenuRow(
                    icon = Icons.Outlined.People,
                    title = "Customers",
                    subtitle = "Customer accounts and balances",
                    tag = NativeUiTags.AdminPeopleCustomers,
                    onClick = hapticAction(onOpenCustomers),
                )
                AdminPeopleMenuRow(
                    icon = Icons.Outlined.Business,
                    title = "Suppliers",
                    subtitle = "Supplier accounts and milk receipts",
                    tag = NativeUiTags.AdminPeopleSuppliers,
                    onClick = hapticAction(onOpenSuppliers),
                )
            }
        }
    }
}

@Composable
private fun AdminPeopleSectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag(NativeUiTags.AdminPeopleSectionLabel),
    )
}

@Composable
private fun AdminPeopleMenuRow(
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
