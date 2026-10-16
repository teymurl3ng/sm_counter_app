package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.horizontalScroll
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
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
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
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeInformationFact
import com.saaf.milkshop.nativeui.common.NativeInformationSection
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.theme.nativeMetricTextStyle
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeVisualColors
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.util.Locale

/**
 * Owner Customers — customer list + detail + create/edit under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome.
 */
private data class OwnerCustomersScreenSnapshot(
    val identity: NativeSignedInIdentity,
    val context: NativeOwnerMoreContext,
    val customers: List<NativeOwnerCustomerRow>,
    val customersLoading: Boolean,
    val customerDetail: NativeOwnerCustomerDetail?,
    val customerCreateOpen: Boolean,
    val customerCreating: Boolean,
    val customerEditOpen: Boolean,
    val customerUpdating: Boolean,
    val customersError: String?,
    val showHeader: Boolean,
)

@Composable
fun OwnerCustomersScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    customers: List<NativeOwnerCustomerRow>,
    customersLoading: Boolean,
    customerDetail: NativeOwnerCustomerDetail?,
    customerCreateOpen: Boolean,
    customerCreating: Boolean,
    customerEditOpen: Boolean,
    customerUpdating: Boolean,
    customersError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateCustomer: (String) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateCustomer: (String, CustomerProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
    onCreditAction: (com.saaf.milkshop.application.owner.CustomerCreditCommand) -> Unit = {},
    communication: CustomerCommunicationAccess = CustomerCommunicationAccess(),
    loadStatement: CustomerStatementLoader = { _, _ -> error("Statement loading is unavailable.") },
    followUps: CustomerFollowUpAccess? = null,
    registerGuard: (EntryLeaveGuard?)->Unit = {},
) {
    var followUpOpen by rememberSaveable(identity.userId) {mutableStateOf(false)}
    var followUpCustomer by rememberSaveable(identity.userId) {mutableStateOf<Long?>(null)}
    var followUpTask by rememberSaveable(identity.userId) {mutableStateOf("detail")}
    var guard by remember {mutableStateOf<EntryLeaveGuard?>(null)}
    val publish=com.saaf.milkshop.nativeui.common.rememberPaneGuardRegistration({guard}) {guard=it;registerGuard(it)}
    val saved=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    LaunchedEffect(customerDetail?.row?.accountId,followUpCustomer,customersLoading,customersError) {
        if(followUpCustomer!=null&&!customersLoading) {
            if(customerDetail?.row?.accountId==followUpCustomer) followUpOpen=false
            else if(customersError!=null) followUpCustomer=null
        }
    }
    if(followUpOpen&&followUps!=null) {
        hostedScaffold(snackbarHostState,modifier,{action->guard?.requestLeave(action) ?: action()}) {padding->
            Column(Modifier.fillMaxSize().padding(padding)) {
                CashierScreenHeader("Customers",context.shopName,formatCashierRoleLabel(identity.role.wireValue),context.businessDate,context.postingOpen)
                customersError?.let {Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(horizontal=16.dp))}
                saved.SaveableStateProvider("followups") {FollowUpWorkspace(followUps.workspace,
                    onBack={followUpOpen=false},onStatement={id->followUpCustomer=id;followUpTask="statement";onOpenCustomer(id)},
                    onReminder={id->followUpCustomer=id;followUpTask="reminder";onOpenCustomer(id)},publishGuard=publish,
                    openingCustomer=followUpCustomer!=null&&(customersLoading||customerDetail?.row?.accountId!=followUpCustomer))}
            }
        }
        return
    }
    val snapshot = OwnerCustomersScreenSnapshot(
        identity = identity,
        context = context,
        customers = customers,
        customersLoading = customersLoading,
        customerDetail = customerDetail,
        customerCreateOpen = customerCreateOpen,
        customerCreating = customerCreating,
        customerEditOpen = customerEditOpen,
        customerUpdating = customerUpdating,
        customersError = customersError,
        showHeader = showHeader,
    )
    saved.SaveableStateProvider("customers") {
    com.saaf.milkshop.nativeui.common.NativeSectionTransition(
        targetState = snapshot,
        contentKey = { pane -> when {
            pane.customerCreateOpen -> "create"
            pane.customerEditOpen -> "edit"
            pane.customerDetail != null -> "detail:${pane.customerDetail.row.accountId}"
            else -> "list"
        } },
        modifier = modifier,
    ) { pane ->
        OwnerCustomersScreenPane(
            snackbarHostState = snackbarHostState,
            identity = pane.identity,
            context = pane.context,
            customers = pane.customers,
            customersLoading = pane.customersLoading,
            customerDetail = pane.customerDetail,
            customerCreateOpen = pane.customerCreateOpen,
            customerCreating = pane.customerCreating,
            customerEditOpen = pane.customerEditOpen,
            customerUpdating = pane.customerUpdating,
            customersError = pane.customersError,
            onBackToMenu = onBackToMenu,
            onRetry = onRetry,
            onOpenCustomer = onOpenCustomer,
            onBackFromDetail = {if(followUpCustomer==pane.customerDetail?.row?.accountId&&followUpCustomer!=null) {
                followUpCustomer=null;followUpTask="detail";followUpOpen=true
            };onBackFromDetail()},
            onOpenCreate = onOpenCreate,
            onBackFromCreate = onBackFromCreate,
            onCreateCustomer = onCreateCustomer,
            onOpenEdit = onOpenEdit,
            onBackFromEdit = onBackFromEdit,
            onUpdateCustomer = onUpdateCustomer,
            onCreditAction = onCreditAction,
            loadStatement = loadStatement,
            communication = communication,
            hostedScaffold = hostedScaffold,
            showHeader = pane.showHeader,
            modifier = modifier,
            followUpEntry = followUps?.let {access->{
                val attention by access.attention.collectAsState(com.saaf.milkshop.sync.FollowUpAttention())
                FollowUpAttentionEntry(attention) {followUpCustomer=null;followUpTask="detail";followUpOpen=true}
            }},
            initialTask = if(followUpCustomer==pane.customerDetail?.row?.accountId&&followUpCustomer!=null) followUpTask else "detail",
            registerGuard = publish,
        )
    }
    }
}

@Composable
private fun OwnerCustomersScreenPane(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    customers: List<NativeOwnerCustomerRow>,
    customersLoading: Boolean,
    customerDetail: NativeOwnerCustomerDetail?,
    customerCreateOpen: Boolean,
    customerCreating: Boolean,
    customerEditOpen: Boolean,
    customerUpdating: Boolean,
    customersError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateCustomer: (String) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateCustomer: (String, CustomerProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
    onCreditAction: (com.saaf.milkshop.application.owner.CustomerCreditCommand) -> Unit = {},
    communication: CustomerCommunicationAccess = CustomerCommunicationAccess(),
    loadStatement: CustomerStatementLoader = { _, _ -> error("Statement loading is unavailable.") },
    followUpEntry: (@Composable ()->Unit)? = null,
    initialTask: String = "detail",
    registerGuard: (EntryLeaveGuard?)->Unit = {},
) {
    if (customerCreateOpen) {
        OwnerCustomersCreateScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            saving = customerCreating,
            error = customersError,
            onBack = onBackFromCreate,
            onSave = onCreateCustomer,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (customerEditOpen && customerDetail != null) {
        OwnerCustomersEditScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = customerDetail,
            saving = customerUpdating,
            error = customersError,
            onBack = onBackFromEdit,
            onSave = onUpdateCustomer,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (customerDetail != null) {
        OwnerCustomersDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = customerDetail,
            error = customersError,
            onBack = onBackFromDetail,
            onOpenEdit = onOpenEdit,
            onCreditAction = onCreditAction,
            loadStatement = loadStatement,
            communication = communication,
            creditBusy = customerUpdating,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
            initialTask = initialTask,
            registerGuard = registerGuard,
        )
        return
    }

    var query by rememberSaveable { mutableStateOf("") }
    var creditFilter by rememberSaveable { mutableStateOf("All") }
    val filteredCustomers = customers.filter { row ->
        (query.isBlank() || listOfNotNull(row.name,row.phone,row.areaRoute).any { it.contains(query.trim(),ignoreCase=true) }) &&
        when(creditFilter) { "All"->true; "Bad"->row.credit?.status?.needsBadFilter==true; else->row.credit?.status?.kind?.let { it.name.replace('_',' ').equals(creditFilter,ignoreCase=true) }==true }
    }
    BackHandler(onBack = onBackToMenu)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCustomersRoot),
        ) {
            if (showHeader) {
                CashierScreenHeader(
                    title = "Customers",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
            }
            when {
                customersLoading && customers.isEmpty() && customersError == null -> {
                        NativeColdLoadingPane(
                            label = "Loading customers…",
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(NativeUiTags.OwnerCustomersLoading),
                        )
                }
                customersError != null && customers.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            customersError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = hapticAction(onRetry),
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersRetry),
                        ) { Text("Try again") }
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp).testTag(NativeUiTags.OwnerCustomersList),
                        verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        item("customer-controls") {
                            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                followUpEntry?.invoke()
                                OutlinedTextField(query,{query=it},label={Text("Search customers")},singleLine=true,
                                    modifier=Modifier.fillMaxWidth().testTag("credit-customer-search"))
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    listOf("All","Watch","Late","Bad","Needs review").forEach {filter->
                                        androidx.compose.material3.FilterChip(creditFilter==filter,onClick=hapticAction {creditFilter=filter},
                                            label={Text(filter)},modifier=Modifier.testTag("credit-filter-$filter"))
                                    }
                                }
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                                    TextButton(onClick=hapticAction(onOpenCreate),modifier=Modifier.testTag(NativeUiTags.OwnerCustomersCreateOpen)) {Text("Add customer")}
                                }
                                customersError?.let {Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag(NativeUiTags.OwnerCustomersError))}
                            }
                        }
                        if(filteredCustomers.isEmpty()) item("empty") {
                            Box(Modifier.fillMaxWidth().padding(24.dp).testTag(NativeUiTags.OwnerCustomersEmpty),contentAlignment=Alignment.Center) {
                                Text(if(customers.isEmpty()) "No customers yet." else "No customers match this filter.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(filteredCustomers,key={it.accountId}) {customer->OwnerCustomerRow(customer,hapticAction {onOpenCustomer(customer.accountId)})}
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerCustomersCreateScreen(
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
                    .testTag(NativeUiTags.OwnerCustomersCreateForm),
            ) {
                CashierScreenHeader(
                    title = "Customers",
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
                        "Add customer",
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
                            .testTag(NativeUiTags.OwnerCustomersCreateName),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                            if (!canSave) return@hapticCommitAction
                            onSave(name)
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersCreateSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save customer")
                        }
                    }
                    TextButton(
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCustomersCreateCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerCustomersDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this customer?",
            body = "The name you entered will be cleared.",
            discardTag = NativeUiTags.OwnerCustomersCreateDiscard,
            keepTag = NativeUiTags.OwnerCustomersCreateKeep,
        )
    }
}

@Composable
private fun OwnerCustomersEditScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerCustomerDetail,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String, CustomerProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val formKey = "edit-${detail.row.accountId}"
    var name by rememberSaveable(formKey) { mutableStateOf(detail.row.name) }
    var phone by rememberSaveable(formKey) { mutableStateOf(detail.row.phone.orEmpty()) }
    var address by rememberSaveable(formKey) { mutableStateOf(detail.address.orEmpty()) }
    var areaRoute by rememberSaveable(formKey) { mutableStateOf(detail.row.areaRoute.orEmpty()) }
    var notes by rememberSaveable(formKey) { mutableStateOf(detail.notes.orEmpty()) }
    var creditLimitText by rememberSaveable(formKey) {
        mutableStateOf(formatOptionalDoubleForEdit(detail.creditLimit))
    }
    var dailyQuotaText by rememberSaveable(formKey) {
        mutableStateOf(formatOptionalDoubleForEdit(detail.dailyQuota))
    }
    var active by rememberSaveable(formKey) { mutableStateOf(detail.active) }
    var draftTouched by rememberSaveable(formKey) { mutableStateOf(false) }

    val creditTrimmed = creditLimitText.trim()
    val creditValue = creditTrimmed.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val creditValid = creditTrimmed.isEmpty() || (creditValue != null && creditValue >= 0.0)
    val quotaTrimmed = dailyQuotaText.trim()
    val quotaValue = quotaTrimmed.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val quotaValid = quotaTrimmed.isEmpty() || (quotaValue != null && quotaValue >= 0.0)
    val nameValid = name.trim().isNotEmpty() && !name.contains('\n')
    val canSave = nameValid && creditValid && quotaValid && !saving
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
                    .testTag(NativeUiTags.OwnerCustomersEditForm),
            ) {
                CashierScreenHeader(
                    title = "Customers",
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
                        "Edit customer",
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
                            .testTag(NativeUiTags.OwnerCustomersEditName),
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
                            .testTag(NativeUiTags.OwnerCustomersEditPhone),
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            draftTouched = true
                        },
                        label = { Text("Address (optional)") },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditAddress),
                    )
                    OutlinedTextField(
                        value = areaRoute,
                        onValueChange = {
                            areaRoute = it
                            draftTouched = true
                        },
                        label = { Text("Area / route (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditAreaRoute),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            draftTouched = true
                        },
                        label = { Text("Notes (optional)") },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditNotes),
                    )
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = {
                            creditLimitText = it
                            draftTouched = true
                        },
                        label = { Text("Credit limit (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        isError = !creditValid,
                        supportingText = if (!creditValid) {
                            { Text("Enter a number ≥ 0, or leave blank") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditCreditLimit),
                    )
                    OutlinedTextField(
                        value = dailyQuotaText,
                        onValueChange = {
                            dailyQuotaText = it
                            draftTouched = true
                        },
                        label = { Text("Daily quota liters (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        isError = !quotaValid,
                        supportingText = if (!quotaValid) {
                            { Text("Enter a number ≥ 0, or leave blank") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditDailyQuota),
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
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersEditActive),
                        )
                    }
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                            if (!canSave) return@hapticCommitAction
                            onSave(
                                name,
                                CustomerProfileInput(
                                    phone = phone.trim().takeIf { it.isNotEmpty() },
                                    areaRoute = areaRoute.trim().takeIf { it.isNotEmpty() },
                                    address = address.trim().takeIf { it.isNotEmpty() },
                                    notes = notes.trim().takeIf { it.isNotEmpty() },
                                    creditLimit = creditValue,
                                    dailyQuota = quotaValue,
                                    active = active,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditSave),
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
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCustomersEditCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerCustomersDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard changes?",
            body = "Unsaved customer edits will be lost.",
            discardTag = NativeUiTags.OwnerCustomersEditDiscard,
            keepTag = NativeUiTags.OwnerCustomersEditKeep,
        )
    }
}

@Composable
private fun OwnerCustomersDiscardDialog(
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
private fun OwnerCustomersDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerCustomerDetail,
    error: String?,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
    onCreditAction: (com.saaf.milkshop.application.owner.CustomerCreditCommand)->Unit,
    creditBusy: Boolean,
    loadStatement: CustomerStatementLoader,
    communication: CustomerCommunicationAccess,
    initialTask: String = "detail",
    registerGuard: (EntryLeaveGuard?)->Unit = {},
) {
    var task by rememberSaveable(identity.userId, detail.row.accountId, initialTask) { mutableStateOf(initialTask) }
    var paneGuard by androidx.compose.runtime.remember { mutableStateOf<EntryLeaveGuard?>(null) }
    val publishGuard=com.saaf.milkshop.nativeui.common.rememberPaneGuardRegistration({paneGuard},{paneGuard=it;registerGuard(it)})
    val panes = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    fun back() {val leave={if(task==initialTask) onBack() else task=if(task=="reminder"||task=="history") "statement" else initialTask};paneGuard?.requestLeave(leave) ?: leave()}
    BackHandler(onBack = {back()})
    val row = detail.row
    hostedScaffold(snackbarHostState, modifier, { action->paneGuard?.requestLeave(action) ?: action() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCustomersDetail),
        ) {
            CashierScreenHeader(
                title = "Customers",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            com.saaf.milkshop.nativeui.common.NativeSectionTransition(
                targetState=task,modifier=Modifier.weight(1f),contentKey={it},
            ) { showing ->
                panes.SaveableStateProvider(showing) {
                when(showing) {
                    "statement"->CustomerStatementScreen(row.accountId,row.name,loadStatement,communication=communication,
                        onReminder={task="reminder"},onHistory={task="history"},backLabel=if(initialTask=="statement") "Back to follow-up" else "Back to customer") {back()}
                    "reminder"->CustomerReminderScreen(row.accountId,communication,{back()},onOpenEdit,publishGuard,backLabel=if(initialTask=="reminder") "Back to follow-up" else "Back to statement")
                    "history"->CustomerPreparationHistory(row.accountId,communication) {task="statement"}
                    else->CustomerDetailOverview(detail,identity.role.wireValue=="owner",creditBusy,error,
                        onOpenStatement={task="statement"},onEdit=onOpenEdit,onCreditAction=onCreditAction)
                }
                }
            }
        }
    }
}

@Composable
private fun OwnerCustomerRow(
    customer: NativeOwnerCustomerRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Entity)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Entity)
            .testTag(NativeUiTags.ownerCustomer(customer.accountId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(customer.balanceLabel, modifier = Modifier.fillMaxWidth(),
                style = nativeMetricTextStyle(), color = nativeVisualColors.metric)
            customer.areaRoute?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = nativeSupportingTextStyle(), color = nativeVisualColors.supporting)
            }
            Text(customer.statusLabel, style = nativeSupportingTextStyle())
            customer.credit?.let { CustomerCreditStatusText(it) }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
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
