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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeDetail
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.time.LocalDate
import java.util.Locale

/**
 * Owner Employees — Salary-account list + detail + create/edit under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome or Users & roles.
 */
@Composable
fun OwnerEmployeesScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    employees: List<NativeOwnerEmployeeRow>,
    employeesLoading: Boolean,
    employeeDetail: NativeOwnerEmployeeDetail?,
    employeeCreateOpen: Boolean,
    employeeCreating: Boolean,
    employeeEditOpen: Boolean,
    employeeUpdating: Boolean,
    employeesError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenEmployee: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateEmployee: (String) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateEmployee: (String, EmployeeProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
) {
    if (employeeCreateOpen) {
        OwnerEmployeesCreateScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            saving = employeeCreating,
            error = employeesError,
            onBack = onBackFromCreate,
            onSave = onCreateEmployee,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (employeeEditOpen && employeeDetail != null) {
        OwnerEmployeesEditScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = employeeDetail,
            saving = employeeUpdating,
            error = employeesError,
            onBack = onBackFromEdit,
            onSave = onUpdateEmployee,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (employeeDetail != null) {
        OwnerEmployeesDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = employeeDetail,
            error = employeesError,
            onBack = onBackFromDetail,
            onOpenEdit = onOpenEdit,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBackToMenu)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerEmployeesRoot),
        ) {
            CashierScreenHeader(
                title = "Employees",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                employeesLoading && employees.isEmpty() && employeesError == null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerEmployeesLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                employeesError != null && employees.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            employeesError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesRetry),
                        ) { Text("Try again") }
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesBack),
                        ) { Text("Back to More") }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = onBackToMenu,
                                modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesBack),
                            ) { Text("Back to More") }
                            TextButton(
                                onClick = onOpenCreate,
                                modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesCreateOpen),
                            ) { Text("Add employee") }
                        }
                        employeesError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerEmployeesError),
                            )
                        }
                        if (employees.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .testTag(NativeUiTags.OwnerEmployeesEmpty),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No employees yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .testTag(NativeUiTags.OwnerEmployeesList),
                            ) {
                                items(employees, key = { it.accountId }) { employee ->
                                    OwnerEmployeeRow(
                                        employee = employee,
                                        onClick = { onOpenEmployee(employee.accountId) },
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerEmployeesCreateScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var name by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val nameValid = name.trim().isNotEmpty() && !name.contains('\n')
    val canSave = nameValid && !saving
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
                    .testTag(NativeUiTags.OwnerEmployeesCreateForm),
            ) {
                CashierScreenHeader(
                    title = "Employees",
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
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesCreateBack),
                    ) { Text("Back to employees") }
                    Text(
                        "Add employee",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            draftTouched = true
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesCreateName),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesError),
                        )
                    }
                    Button(
                        onClick = {
                            if (!canSave) return@Button
                            onSave(name)
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesCreateSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save employee")
                        }
                    }
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesCreateCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerEmployeesDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this employee?",
            body = "The name you entered will be cleared.",
            discardTag = NativeUiTags.OwnerEmployeesCreateDiscard,
            keepTag = NativeUiTags.OwnerEmployeesCreateKeep,
        )
    }
}

@Composable
private fun OwnerEmployeesEditScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerEmployeeDetail,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String, EmployeeProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val formKey = "edit-${detail.row.accountId}"
    var name by rememberSaveable(formKey) { mutableStateOf(detail.row.name) }
    var role by rememberSaveable(formKey) { mutableStateOf(detail.row.role.orEmpty()) }
    var phone by rememberSaveable(formKey) { mutableStateOf(detail.row.phone.orEmpty()) }
    var referenceSalaryText by rememberSaveable(formKey) {
        mutableStateOf(formatOptionalDoubleForEdit(detail.referenceSalary))
    }
    var salaryPeriod by rememberSaveable(formKey) { mutableStateOf(detail.salaryPeriod.orEmpty()) }
    var joinDate by rememberSaveable(formKey) { mutableStateOf(detail.joinDate.orEmpty()) }
    var active by rememberSaveable(formKey) { mutableStateOf(detail.active) }
    var draftTouched by rememberSaveable(formKey) { mutableStateOf(false) }

    val salaryTrimmed = referenceSalaryText.trim()
    val salaryValue = salaryTrimmed.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val salaryValid = salaryTrimmed.isEmpty() || (salaryValue != null && salaryValue >= 0.0)
    val joinTrimmed = joinDate.trim()
    val joinValid = joinTrimmed.isEmpty() || runCatching { LocalDate.parse(joinTrimmed) }.isSuccess
    val nameValid = name.trim().isNotEmpty() && !name.contains('\n')
    val canSave = nameValid && salaryValid && joinValid && !saving
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
                    .testTag(NativeUiTags.OwnerEmployeesEditForm),
            ) {
                CashierScreenHeader(
                    title = "Employees",
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
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesEditBack),
                    ) { Text("Back to detail") }
                    Text(
                        "Edit employee",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            draftTouched = true
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesEditName),
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = {
                            role = it
                            draftTouched = true
                        },
                        label = { Text("Role (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesEditRole),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            draftTouched = true
                        },
                        label = { Text("Phone (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesEditPhone),
                    )
                    OutlinedTextField(
                        value = referenceSalaryText,
                        onValueChange = {
                            referenceSalaryText = it
                            draftTouched = true
                        },
                        label = { Text("Reference salary (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        isError = !salaryValid,
                        supportingText = if (!salaryValid) {
                            { Text("Enter a number ≥ 0, or leave blank") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesEditReferenceSalary),
                    )
                    OutlinedTextField(
                        value = salaryPeriod,
                        onValueChange = {
                            salaryPeriod = it
                            draftTouched = true
                        },
                        label = { Text("Salary period (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesEditSalaryPeriod),
                    )
                    OutlinedTextField(
                        value = joinDate,
                        onValueChange = {
                            joinDate = it
                            draftTouched = true
                        },
                        label = { Text("Join date (yyyy-MM-dd, optional)") },
                        singleLine = true,
                        enabled = !saving,
                        isError = !joinValid,
                        supportingText = if (!joinValid) {
                            { Text("Use yyyy-MM-dd, or leave blank") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesEditJoinDate),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Active", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = active,
                            onCheckedChange = {
                                active = it
                                draftTouched = true
                            },
                            enabled = !saving,
                            modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesEditActive),
                        )
                    }
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesError),
                        )
                    }
                    Button(
                        onClick = {
                            if (!canSave) return@Button
                            onSave(
                                name,
                                EmployeeProfileInput(
                                    role = role.trim().takeIf { it.isNotEmpty() },
                                    phone = phone.trim().takeIf { it.isNotEmpty() },
                                    referenceSalary = salaryValue,
                                    salaryPeriod = salaryPeriod.trim().takeIf { it.isNotEmpty() },
                                    joinDate = joinTrimmed.takeIf { it.isNotEmpty() },
                                    active = active,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerEmployeesEditSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save changes")
                        }
                    }
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesEditCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerEmployeesDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard changes?",
            body = "Unsaved employee edits will be lost.",
            discardTag = NativeUiTags.OwnerEmployeesEditDiscard,
            keepTag = NativeUiTags.OwnerEmployeesEditKeep,
        )
    }
}

@Composable
private fun OwnerEmployeesDiscardDialog(
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
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(discardTag),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(keepTag),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
private fun OwnerEmployeesDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerEmployeeDetail,
    error: String?,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
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
                .testTag(NativeUiTags.OwnerEmployeesDetail),
        ) {
            CashierScreenHeader(
                title = "Employees",
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
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesDetailBack),
                ) { Text("Back to employees") }
                Text(
                    row.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesDetailName),
                )
                OwnerEmployeeFact("Status", row.statusLabel)
                OwnerEmployeeFact("Balance", row.balanceLabel)
                row.role?.takeIf { it.isNotBlank() }?.let { OwnerEmployeeFact("Role", it) }
                row.phone?.takeIf { it.isNotBlank() }?.let { OwnerEmployeeFact("Phone", it) }
                detail.referenceSalaryLabel?.let { OwnerEmployeeFact("Reference salary", it) }
                detail.salaryPeriod?.takeIf { it.isNotBlank() }?.let {
                    OwnerEmployeeFact("Salary period", it)
                }
                detail.joinDate?.takeIf { it.isNotBlank() }?.let {
                    OwnerEmployeeFact("Join date", it)
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerEmployeesError),
                    )
                }
                Button(
                    onClick = onOpenEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerEmployeesDetailEdit),
                ) { Text("Edit employee") }
            }
        }
    }
}

@Composable
private fun OwnerEmployeeRow(
    employee: NativeOwnerEmployeeRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(NativeUiTags.ownerEmployee(employee.accountId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                employee.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(employee.statusLabel)
                    append(" · ")
                    append(employee.balanceLabel)
                    employee.role?.takeIf { it.isNotBlank() }?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun OwnerEmployeeFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatOptionalDoubleForEdit(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%s", value)
    }
}
