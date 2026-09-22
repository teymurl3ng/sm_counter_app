package com.saaf.milkshop.nativeui.manager

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
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
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
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.OwnerCustomersScreen
import com.saaf.milkshop.nativeui.owner.OwnerEmployeesScreen
import com.saaf.milkshop.nativeui.owner.OwnerProductsScreen
import com.saaf.milkshop.nativeui.owner.OwnerSuppliersScreen
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Manager More — bounded shop tools + personal security under ManagerScaffold.
 * Never mounts OwnerMoreScreen; Owner-only rows are omitted (not disabled stubs).
 */
@Composable
fun ManagerMoreScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState,
    onRetry: () -> Unit,
    onOpenPersonalSecurity: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenEmployees: () -> Unit,
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
    onRequestRemoveQuickUnlock: () -> Unit,
    onCancelRemoveQuickUnlock: () -> Unit,
    onRemoveQuickUnlock: (CharArray) -> Unit,
    onEnrollQuickPin: (CharArray, CharArray) -> Unit,
    onEnrollPhoneSecurity: (CharArray, CharArray?) -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val managerShell: HostedScaffold = { snackbar, mod, wrapNav, content ->
        ManagerScaffold(
            snackbarHostState = snackbar,
            section = ManagerSection.More,
            onNavigate = { section -> wrapNav { onNavigate(section) } },
            modifier = mod,
            content = content,
        )
    }

    when (state) {
        NativeOwnerMoreState.Idle,
        NativeOwnerMoreState.Loading,
        -> ManagerScaffold(snackbarHostState, ManagerSection.More, onNavigate, modifier) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .testTag(NativeUiTags.ManagerMoreLoading),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is NativeOwnerMoreState.LoadFailure -> ManagerScaffold(
            snackbarHostState,
            ManagerSection.More,
            onNavigate,
            modifier,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .testTag(NativeUiTags.ManagerMoreRoot),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag(NativeUiTags.ManagerMoreRetryLoad),
                ) {
                    Text("Try again")
                }
            }
        }

        is NativeOwnerMoreState.Ready -> when (state.destination) {
            NativeOwnerMoreDestination.Menu -> ManagerMoreMenuScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = state,
                onOpenPersonalSecurity = onOpenPersonalSecurity,
                onOpenProducts = onOpenProducts,
                onOpenCustomers = onOpenCustomers,
                onOpenSuppliers = onOpenSuppliers,
                onOpenEmployees = onOpenEmployees,
                onSwitchAccount = onSwitchAccount,
                onSignOut = onSignOut,
                onNavigate = onNavigate,
                modifier = modifier,
            )
            NativeOwnerMoreDestination.PersonalSecurity -> ManagerMorePersonalSecurityScreen(
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
            NativeOwnerMoreDestination.Products -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.ManagerProductsRoot),
            ) {
                OwnerProductsScreen(
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
                    hostedScaffold = managerShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.Customers -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.ManagerCustomersRoot),
            ) {
                OwnerCustomersScreen(
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
                    hostedScaffold = managerShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.Suppliers -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.ManagerSuppliersRoot),
            ) {
                OwnerSuppliersScreen(
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
                    hostedScaffold = managerShell,
                    modifier = modifier,
                )
            }
            NativeOwnerMoreDestination.Employees -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(NativeUiTags.ManagerEmployeesRoot),
            ) {
                OwnerEmployeesScreen(
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
                    hostedScaffold = managerShell,
                    modifier = modifier,
                )
            }
            else -> {
                // Owner-only destinations are unreachable from Manager menu; return to menu.
                BackHandler(onBack = onBackToMenu)
                ManagerMoreMenuScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = state.copy(destination = NativeOwnerMoreDestination.Menu),
                    onOpenPersonalSecurity = onOpenPersonalSecurity,
                    onOpenProducts = onOpenProducts,
                    onOpenCustomers = onOpenCustomers,
                    onOpenSuppliers = onOpenSuppliers,
                    onOpenEmployees = onOpenEmployees,
                    onSwitchAccount = onSwitchAccount,
                    onSignOut = onSignOut,
                    onNavigate = onNavigate,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun ManagerMoreMenuScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState.Ready,
    onOpenPersonalSecurity: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenEmployees: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier,
) {
    ManagerScaffold(snackbarHostState, ManagerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.ManagerMoreRoot),
        ) {
            ManagerMoreHeader(identity, state.context)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag(NativeUiTags.ManagerMoreMenu),
            ) {
                ManagerMoreSectionLabel("Things you sell")
                ManagerMoreMenuRow(
                    icon = Icons.Outlined.Inventory2,
                    title = "Products",
                    subtitle = "What you sell and how it is measured",
                    tag = NativeUiTags.ManagerMoreProducts,
                    onClick = onOpenProducts,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ManagerMoreSectionLabel("People and businesses")
                ManagerMoreMenuRow(
                    icon = Icons.Outlined.People,
                    title = "Customers",
                    subtitle = "Customer accounts and balances",
                    tag = NativeUiTags.ManagerMoreCustomers,
                    onClick = onOpenCustomers,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ManagerMoreMenuRow(
                    icon = Icons.Outlined.Business,
                    title = "Suppliers",
                    subtitle = "Supplier accounts and milk receipts",
                    tag = NativeUiTags.ManagerMoreSuppliers,
                    onClick = onOpenSuppliers,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ManagerMoreMenuRow(
                    icon = Icons.Outlined.Person,
                    title = "Employees",
                    subtitle = "Employee profiles and attendance",
                    tag = NativeUiTags.ManagerMoreEmployees,
                    onClick = onOpenEmployees,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ManagerMoreSectionLabel("Your account")
                ManagerMoreMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = "Personal security",
                    subtitle = "Quick unlock for this account on this phone",
                    tag = NativeUiTags.ManagerMorePersonalSecurity,
                    onClick = onOpenPersonalSecurity,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ManagerMoreMenuRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch account",
                    subtitle = "Sign in as a different person on this phone",
                    tag = NativeUiTags.ManagerMoreSwitchAccount,
                    onClick = onSwitchAccount,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ManagerMoreMenuRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "Sign out",
                    subtitle = "End this session on this phone",
                    tag = NativeUiTags.ManagerMoreSignOut,
                    onClick = onSignOut,
                )
            }
        }
    }
}

@Composable
private fun ManagerMorePersonalSecurityScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerMoreState.Ready,
    onBack: () -> Unit,
    onRequestRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onRemove: (CharArray) -> Unit,
    onEnrollPin: (CharArray, CharArray) -> Unit,
    onEnrollPhone: (CharArray, CharArray?) -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val security = state.security
    var passphrase by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }

    if (state.confirmRemoveUnlock) {
        AlertDialog(
            onDismissRequest = onCancelRemove,
            title = { Text("Remove Quick unlock?") },
            text = {
                Text("This clears Quick PIN and phone security for ${identity.displayName} on this phone.")
            },
            confirmButton = {
                TextButton(onClick = { onRemove(passphrase.toCharArray()) }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = onCancelRemove) { Text("Keep") }
            },
        )
    }

    ManagerScaffold(snackbarHostState, ManagerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag(NativeUiTags.ManagerMoreSecurityScreen),
        ) {
            ManagerMoreHeader(identity, state.context)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(NativeUiTags.ManagerMoreBack),
                ) {
                    Text("Back to More")
                }
                Text("Personal security", style = MaterialTheme.typography.headlineMedium)
                if (security == null) {
                    CircularProgressIndicator()
                } else {
                    ManagerMoreSecurityStatus(security)
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Current full passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New Quick PIN (4 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            if (passphrase.isBlank() || pin.length != 4) return@Button
                            onEnrollPin(passphrase.toCharArray(), pin.toCharArray())
                        },
                        enabled = !state.saving && passphrase.isNotBlank() && pin.length == 4,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.saving) "Saving…" else "Save Quick PIN") }
                    if (security.phoneSecurityAvailable) {
                        Button(
                            onClick = {
                                if (passphrase.isBlank()) return@Button
                                onEnrollPhone(
                                    passphrase.toCharArray(),
                                    pin.takeIf { it.length == 4 }?.toCharArray(),
                                )
                            },
                            enabled = !state.saving && passphrase.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Enable ${security.phoneSecurityLabel}") }
                    }
                    if (security.quickPinEnabled || security.phoneSecurityEnabled) {
                        TextButton(
                            onClick = onRequestRemove,
                            enabled = !state.saving && passphrase.isNotBlank(),
                        ) { Text("Remove Quick unlock") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerMoreHeader(identity: NativeSignedInIdentity, context: NativeOwnerMoreContext) {
    CashierScreenHeader(
        title = "More",
        displayName = context.shopName,
        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
        businessDate = context.businessDate,
        postingOpen = context.postingOpen,
    )
}

@Composable
private fun ManagerMoreSectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(NativeUiTags.ManagerMoreSectionLabel),
    )
}

@Composable
private fun ManagerMoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
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
private fun ManagerMoreSecurityStatus(security: NativeMoreSecurityStatus) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            if (security.quickPinEnabled) "Quick PIN is on" else "Quick PIN is off",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            if (security.phoneSecurityEnabled) {
                "${security.phoneSecurityLabel} is on"
            } else {
                "${security.phoneSecurityLabel} is off"
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
