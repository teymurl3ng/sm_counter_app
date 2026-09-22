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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerExpenseCategoryRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsSnapshot
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.util.Locale

/**
 * Owner Shop settings — read panel + identity/rates/category write depth under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome. Opening writes stay deferred.
 */
@Composable
fun OwnerShopSettingsScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    snapshot: NativeOwnerShopSettingsSnapshot?,
    loading: Boolean,
    error: String?,
    identityEditOpen: Boolean,
    identitySaving: Boolean,
    ratesEditOpen: Boolean,
    ratesSaving: Boolean,
    categoryCreateOpen: Boolean,
    categoryCreating: Boolean,
    categoryDetail: NativeOwnerExpenseCategoryRow?,
    categoryArchiving: Boolean,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenIdentityEdit: () -> Unit,
    onBackFromIdentityEdit: () -> Unit,
    onSaveIdentity: (StoreIdentityInput) -> Unit,
    onOpenRatesEdit: () -> Unit,
    onBackFromRatesEdit: () -> Unit,
    onUpdateRates: (BusinessSettings) -> Unit,
    onOpenCategoryCreate: () -> Unit,
    onBackFromCategoryCreate: () -> Unit,
    onCreateCategory: (String) -> Unit,
    onOpenCategory: (Long) -> Unit,
    onBackFromCategoryDetail: () -> Unit,
    onSetCategoryArchived: (Boolean) -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (identityEditOpen && snapshot != null) {
        OwnerShopSettingsIdentityEditScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            snapshot = snapshot,
            saving = identitySaving,
            error = error,
            onBack = onBackFromIdentityEdit,
            onSave = onSaveIdentity,
            onNavigate = onNavigate,
            modifier = modifier,
        )
        return
    }

    if (ratesEditOpen && snapshot != null) {
        OwnerShopSettingsRatesEditScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            snapshot = snapshot,
            saving = ratesSaving,
            error = error,
            onBack = onBackFromRatesEdit,
            onSave = onUpdateRates,
            onNavigate = onNavigate,
            modifier = modifier,
        )
        return
    }

    if (categoryCreateOpen) {
        OwnerShopSettingsCategoryCreateScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            saving = categoryCreating,
            error = error,
            onBack = onBackFromCategoryCreate,
            onSave = onCreateCategory,
            onNavigate = onNavigate,
            modifier = modifier,
        )
        return
    }

    if (categoryDetail != null) {
        OwnerShopSettingsCategoryDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            category = categoryDetail,
            archiving = categoryArchiving,
            error = error,
            onBack = onBackFromCategoryDetail,
            onSetArchived = onSetCategoryArchived,
            onNavigate = onNavigate,
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBackToMenu)
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerShopSettingsRoot),
        ) {
            CashierScreenHeader(
                title = "Shop settings",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                loading && snapshot == null && error == null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerShopSettingsLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null && snapshot == null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsRetry),
                        ) { Text("Try again") }
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsBack),
                        ) { Text("Back to More") }
                    }
                }
                snapshot != null -> {
                    Column(Modifier.fillMaxSize()) {
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag(NativeUiTags.OwnerShopSettingsBack),
                        ) { Text("Back to More") }
                        error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerShopSettingsError),
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag(NativeUiTags.OwnerShopSettingsContent),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    snapshot.shopName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = onOpenIdentityEdit,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsEditIdentity),
                                ) { Text("Edit identity") }
                            }
                            OwnerShopSettingsFact("Phone", snapshot.phone ?: "Not set")
                            OwnerShopSettingsFact("Address", snapshot.address ?: "Not set")
                            OwnerShopSettingsFact("Currency", snapshot.currencyCode)

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Rates",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsRatesSection),
                                )
                                TextButton(
                                    onClick = onOpenRatesEdit,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsEditRates),
                                ) { Text("Edit rates") }
                            }
                            OwnerShopSettingsFact("Vendor milk rate", snapshot.vendorMilkRateLabel)
                            OwnerShopSettingsFact("Vendor delivery fee", snapshot.vendorDeliveryFeeLabel)
                            OwnerShopSettingsFact("TS standard", snapshot.tsStandardLabel)
                            OwnerShopSettingsFact("Retail milk rate", snapshot.retailMilkRateLabel)
                            OwnerShopSettingsFact("Retail yogurt rate", snapshot.retailYogurtRateLabel)
                            OwnerShopSettingsFact("Default delivery surcharge", snapshot.defaultSurchargeLabel)

                            Text(
                                "Opening state",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsOpeningSection),
                            )
                            OwnerShopSettingsFact("Opening date", snapshot.openingDateLabel)
                            snapshot.openingNotes?.takeIf { it.isNotBlank() }?.let {
                                OwnerShopSettingsFact("Opening notes", it)
                            }
                            Text(
                                "Recording or correcting opening balances comes next.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsOpeningDeferred),
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Expense categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoriesSection),
                                )
                                TextButton(
                                    onClick = onOpenCategoryCreate,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsAddCategory),
                                ) { Text("Add category") }
                            }
                            if (snapshot.expenseCategories.isEmpty()) {
                                Text(
                                    "No expense categories yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                snapshot.expenseCategories.forEach { category ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .semantics { role = Role.Button }
                                            .clickable { onOpenCategory(category.accountId) }
                                            .testTag(NativeUiTags.ownerShopExpenseCategory(category.accountId))
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                category.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Text(
                                                "#${category.accountId} · ${category.statusLabel}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Icon(
                                            Icons.Outlined.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
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
private fun OwnerShopSettingsIdentityEditScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    snapshot: NativeOwnerShopSettingsSnapshot,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (StoreIdentityInput) -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var storeName by rememberSaveable { mutableStateOf(snapshot.shopName) }
    var phone by rememberSaveable { mutableStateOf(snapshot.phone.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(snapshot.address.orEmpty()) }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val nameValid = storeName.trim().isNotEmpty() && !storeName.contains('\n')
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
        OwnerScaffold(
            snackbarHostState = snackbarHostState,
            section = OwnerSection.More,
            onNavigate = { section -> requestLeave { onNavigate(section) } },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerShopSettingsIdentityForm),
            ) {
                CashierScreenHeader(
                    title = "Shop settings",
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
                        modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsIdentityBack),
                    ) { Text("Back to shop settings") }
                    Text(
                        "Edit identity",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = {
                            storeName = it
                            draftTouched = true
                        },
                        label = { Text("Store name") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerShopSettingsIdentityName),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            draftTouched = true
                        },
                        label = { Text("Phone") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerShopSettingsIdentityPhone),
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            draftTouched = true
                        },
                        label = { Text("Address") },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerShopSettingsIdentityAddress),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsError),
                        )
                    }
                    Button(
                        onClick = {
                            if (!canSave) return@Button
                            onSave(
                                StoreIdentityInput(
                                    storeName = storeName,
                                    phone = phone,
                                    address = address,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerShopSettingsIdentitySave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save identity")
                        }
                    }
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsIdentityCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerShopSettingsDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard identity changes?",
            body = "Edits to store name, phone, and address will be cleared.",
            discardTag = NativeUiTags.OwnerShopSettingsIdentityDiscard,
            keepTag = NativeUiTags.OwnerShopSettingsIdentityKeep,
        )
    }
}

@Composable
private fun OwnerShopSettingsRatesEditScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    snapshot: NativeOwnerShopSettingsSnapshot,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (BusinessSettings) -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var milkRateVendor by rememberSaveable {
        mutableStateOf(formatDoubleForEdit(snapshot.milkRateVendor))
    }
    var deliveryFeeVendor by rememberSaveable {
        mutableStateOf(formatDoubleForEdit(snapshot.deliveryFeeVendor))
    }
    var tsStandard by rememberSaveable {
        mutableStateOf(formatDoubleForEdit(snapshot.tsStandard))
    }
    var retailRateMilk by rememberSaveable {
        mutableStateOf(formatDoubleForEdit(snapshot.retailRateMilk))
    }
    var retailRateYogurt by rememberSaveable {
        mutableStateOf(formatDoubleForEdit(snapshot.retailRateYogurt))
    }
    var defaultDeliverySurcharge by rememberSaveable {
        mutableStateOf(formatDoubleForEdit(snapshot.defaultDeliverySurcharge))
    }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val milk = milkRateVendor.trim().toDoubleOrNull()
    val delivery = deliveryFeeVendor.trim().toDoubleOrNull()
    val ts = tsStandard.trim().toDoubleOrNull()
    val retailMilk = retailRateMilk.trim().toDoubleOrNull()
    val retailYogurt = retailRateYogurt.trim().toDoubleOrNull()
    val surcharge = defaultDeliverySurcharge.trim().toDoubleOrNull()
    val ratesValid = listOf(milk, delivery, ts, retailMilk, retailYogurt, surcharge)
        .all { it != null && it > 0.0 && it.isFinite() }
    val canSave = ratesValid && !saving
    val dirty = draftTouched && !saving

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }

    fun touch(setter: (String) -> Unit): (String) -> Unit = {
        setter(it)
        draftTouched = true
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        OwnerScaffold(
            snackbarHostState = snackbarHostState,
            section = OwnerSection.More,
            onNavigate = { section -> requestLeave { onNavigate(section) } },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerShopSettingsRatesForm),
            ) {
                CashierScreenHeader(
                    title = "Shop settings",
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
                        modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsRatesBack),
                    ) { Text("Back to shop settings") }
                    Text(
                        "Edit rates",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OwnerShopSettingsRateField(
                        value = milkRateVendor,
                        onValueChange = touch { milkRateVendor = it },
                        label = "Vendor milk rate (PKR)",
                        enabled = !saving,
                        tag = NativeUiTags.OwnerShopSettingsRatesMilkVendor,
                    )
                    OwnerShopSettingsRateField(
                        value = deliveryFeeVendor,
                        onValueChange = touch { deliveryFeeVendor = it },
                        label = "Vendor delivery fee (PKR)",
                        enabled = !saving,
                        tag = NativeUiTags.OwnerShopSettingsRatesDeliveryFee,
                    )
                    OwnerShopSettingsRateField(
                        value = tsStandard,
                        onValueChange = touch { tsStandard = it },
                        label = "TS standard (%)",
                        enabled = !saving,
                        tag = NativeUiTags.OwnerShopSettingsRatesTsStandard,
                    )
                    OwnerShopSettingsRateField(
                        value = retailRateMilk,
                        onValueChange = touch { retailRateMilk = it },
                        label = "Retail milk rate (PKR)",
                        enabled = !saving,
                        tag = NativeUiTags.OwnerShopSettingsRatesRetailMilk,
                    )
                    OwnerShopSettingsRateField(
                        value = retailRateYogurt,
                        onValueChange = touch { retailRateYogurt = it },
                        label = "Retail yogurt rate (PKR)",
                        enabled = !saving,
                        tag = NativeUiTags.OwnerShopSettingsRatesRetailYogurt,
                    )
                    OwnerShopSettingsRateField(
                        value = defaultDeliverySurcharge,
                        onValueChange = touch { defaultDeliverySurcharge = it },
                        label = "Default delivery surcharge (PKR)",
                        enabled = !saving,
                        tag = NativeUiTags.OwnerShopSettingsRatesSurcharge,
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsError),
                        )
                    }
                    Button(
                        onClick = {
                            if (!canSave) return@Button
                            onSave(
                                BusinessSettings(
                                    milkRateVendor = milk!!,
                                    deliveryFeeVendor = delivery!!,
                                    tsStandard = ts!!,
                                    retailRateMilk = retailMilk!!,
                                    retailRateYogurt = retailYogurt!!,
                                    defaultDeliverySurcharge = surcharge!!,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerShopSettingsRatesSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save rates")
                        }
                    }
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsRatesCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerShopSettingsDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard rate changes?",
            body = "Edits to business rates will be cleared.",
            discardTag = NativeUiTags.OwnerShopSettingsRatesDiscard,
            keepTag = NativeUiTags.OwnerShopSettingsRatesKeep,
        )
    }
}

@Composable
private fun OwnerShopSettingsCategoryCreateScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onNavigate: (OwnerSection) -> Unit,
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
        OwnerScaffold(
            snackbarHostState = snackbarHostState,
            section = OwnerSection.More,
            onNavigate = { section -> requestLeave { onNavigate(section) } },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerShopSettingsCategoryCreateForm),
            ) {
                CashierScreenHeader(
                    title = "Shop settings",
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
                        modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryCreateBack),
                    ) { Text("Back to shop settings") }
                    Text(
                        "Add expense category",
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
                            .testTag(NativeUiTags.OwnerShopSettingsCategoryCreateName),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsError),
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
                            .testTag(NativeUiTags.OwnerShopSettingsCategoryCreateSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save category")
                        }
                    }
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryCreateCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerShopSettingsDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this category?",
            body = "The name you entered will be cleared.",
            discardTag = NativeUiTags.OwnerShopSettingsCategoryCreateDiscard,
            keepTag = NativeUiTags.OwnerShopSettingsCategoryCreateKeep,
        )
    }
}

@Composable
private fun OwnerShopSettingsCategoryDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    category: NativeOwnerExpenseCategoryRow,
    archiving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSetArchived: (Boolean) -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    var archiveConfirmOpen by rememberSaveable { mutableStateOf(false) }
    var unarchiveConfirmOpen by rememberSaveable { mutableStateOf(false) }
    val busy = archiving

    BackHandler {
        when {
            archiveConfirmOpen -> archiveConfirmOpen = false
            unarchiveConfirmOpen -> unarchiveConfirmOpen = false
            else -> onBack()
        }
    }

    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerShopSettingsCategoryDetail),
        ) {
            CashierScreenHeader(
                title = "Shop settings",
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
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryDetailBack),
                ) { Text("Back to shop settings") }
                Text(
                    category.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryDetailName),
                )
                OwnerShopSettingsFact("Account", "#${category.accountId}")
                OwnerShopSettingsFact("Status", category.statusLabel)
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsError),
                    )
                }
                if (category.active) {
                    Button(
                        onClick = { archiveConfirmOpen = true },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerShopSettingsCategoryArchive),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Archive category")
                        }
                    }
                } else {
                    Button(
                        onClick = { unarchiveConfirmOpen = true },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerShopSettingsCategoryUnarchive),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Restore category")
                        }
                    }
                }
            }
        }
    }

    if (archiveConfirmOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) archiveConfirmOpen = false
            },
            title = { Text("Archive this category?") },
            text = { Text("${category.name} will be marked inactive.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        archiveConfirmOpen = false
                        onSetArchived(true)
                    },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryArchiveConfirm),
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(
                    onClick = { archiveConfirmOpen = false },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryArchiveCancel),
                ) { Text("Cancel") }
            },
        )
    }
    if (unarchiveConfirmOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) unarchiveConfirmOpen = false
            },
            title = { Text("Restore this category?") },
            text = { Text("${category.name} will be marked active again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        unarchiveConfirmOpen = false
                        onSetArchived(false)
                    },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryUnarchiveConfirm),
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(
                    onClick = { unarchiveConfirmOpen = false },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoryUnarchiveCancel),
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun OwnerShopSettingsRateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    tag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
    )
}

@Composable
private fun OwnerShopSettingsFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OwnerShopSettingsDiscardDialog(
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
            ) { Text("Discard") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(keepTag),
            ) { Text("Keep editing") }
        },
    )
}

private fun formatDoubleForEdit(value: Double): String =
    String.format(Locale.US, "%s", value)
