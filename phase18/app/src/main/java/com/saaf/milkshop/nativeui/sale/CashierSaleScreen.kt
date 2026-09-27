package com.saaf.milkshop.nativeui.sale

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.R
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.sale.NativeSaleAccount
import com.saaf.milkshop.application.sale.NativeSaleEntryMode
import com.saaf.milkshop.application.sale.NativeSalePaymentKind
import com.saaf.milkshop.application.sale.NativeSaleProduct
import com.saaf.milkshop.application.sale.NativeSaleRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import java.util.Locale
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.FlowRow

private enum class EntryMode { Quantity, Amount }
private enum class PaymentChoice { PaidNow, CustomerAccount }
enum class CashierSection { Sell, Collect, Activity, More }

@Composable
fun CashierSaleScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeSaleState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onRecordSale: (NativeSaleRequest) -> Unit,
    onSendAsDelivery: (String) -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeSaleState.Ready)?.saving != true
    LaunchedEffect(state is NativeSaleState.Ready) {
        if (state !is NativeSaleState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onLeaveConfirmed)
    }
    val guardedNavigate: (CashierSection) -> Unit = { section ->
        if (canLeave) leaveGuard.requestLeave { onNavigate(section) }
    }
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        compactNavigation = { CashierNavigation(CashierSection.Sell, guardedNavigate) },
        expandedNavigation = { CashierNavigationRail(CashierSection.Sell, guardedNavigate) },
    ) { padding ->
        when (state) {
            NativeSaleState.Idle,
            NativeSaleState.Loading,
            -> SaleLoadingBody(
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding(),
            )
            is NativeSaleState.LoadFailure -> SaleLoadFailureBody(
                message = state.message,
                onRetry = onRetry,
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding(),
            )
            is NativeSaleState.Ready -> SaleEntryContent(
                identity = identity,
                state = state,
                onClearFeedback = onClearFeedback,
                onRecordSale = onRecordSale,
                onSendAsDelivery = onSendAsDelivery,
                leaveGuard = leaveGuard,
                contentPadding = padding,
            )
        }
    }
    SaleDiscardDialog(leaveGuard)
}

@Composable
internal fun CashierNavigationRail(
    selected: CashierSection,
    onNavigate: (CashierSection) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        CashierRailItem(Icons.Outlined.PointOfSale, stringResource(R.string.native_sell), selected == CashierSection.Sell, NativeUiTags.CashierNavSell) { onNavigate(CashierSection.Sell) }
        CashierRailItem(Icons.Outlined.Payments, stringResource(R.string.native_collect), selected == CashierSection.Collect, NativeUiTags.CashierNavCollect) { onNavigate(CashierSection.Collect) }
        CashierRailItem(Icons.Outlined.History, stringResource(R.string.native_activity), selected == CashierSection.Activity, NativeUiTags.CashierNavActivity) { onNavigate(CashierSection.Activity) }
        CashierRailItem(Icons.Outlined.MoreHoriz, stringResource(R.string.native_more), selected == CashierSection.More, NativeUiTags.CashierNavMore) { onNavigate(CashierSection.More) }
    }
}

@Composable
private fun CashierRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag).heightIn(min = 56.dp),
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, maxLines = 2) },
    )
}

@Composable
internal fun SaleEntryContent(
    identity: NativeSignedInIdentity,
    state: NativeSaleState.Ready,
    onClearFeedback: () -> Unit,
    onRecordSale: (NativeSaleRequest) -> Unit,
    onSendAsDelivery: (String) -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = state.context
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var entryMode by rememberSaveable { mutableStateOf(EntryMode.Quantity) }
    var payment by rememberSaveable { mutableStateOf(PaymentChoice.PaidNow) }
    var entryValue by rememberSaveable { mutableStateOf("2.0") }
    var selectedCustomerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCustomerPicker by rememberSaveable { mutableStateOf(false) }
    var secondaryDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val useStackedLayout = LocalDensity.current.fontScale >= 1.3f

    val product = context.products.firstOrNull { it.productId == selectedProductId }
        ?: context.products.firstOrNull()
    val selectedCustomer = context.customers.firstOrNull { it.accountId == selectedCustomerId }
    val paymentAccount = context.paymentAccounts.firstOrNull {
        it.accountId == context.defaultPaymentAccountId
    }
    val rate = (product?.ratePaisaPerUnit ?: 0L) / 100.0
    val productName = product?.name.orEmpty()
    val quantityUnit = product?.unit?.toDisplayUnit().orEmpty()
    val enteredNumber = entryValue.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val quantity = if (entryMode == EntryMode.Quantity) enteredNumber else {
        if (rate > 0.0) enteredNumber / rate else 0.0
    }
    val total = if (entryMode == EntryMode.Amount) enteredNumber else enteredNumber * rate
    val quantityText = formatQuantity(quantity)
    val totalText = formatAmount(total)
    val paymentSummary = when (payment) {
        PaymentChoice.PaidNow -> paymentAccount?.name ?: "Payment unavailable"
        PaymentChoice.CustomerAccount -> selectedCustomer?.name
            ?: stringResource(R.string.native_customer_account)
    }
    val canRecord = context.postingOpen && product != null && quantity > 0.0 && !state.saving &&
        when (payment) {
            PaymentChoice.PaidNow -> paymentAccount != null
            PaymentChoice.CustomerAccount -> selectedCustomer != null
        }

    LaunchedEffect(state.receipt?.eventId) {
        if (state.receipt != null) {
            selectedProductId = context.products.firstOrNull()?.productId
            entryMode = EntryMode.Quantity
            payment = PaymentChoice.PaidNow
            entryValue = "2.0"
            selectedCustomerId = null
            secondaryDetailsExpanded = false
            leaveGuard.clear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
    ) {
            if (state.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            CashierScreenHeader(
                title = stringResource(R.string.native_new_sale),
                displayName = identity.displayName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag(NativeUiTags.CashierSaleScroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (!context.postingOpen) {
                    StatusMessage(
                        message = "${formatCashierBusinessDate(context.businessDate)} is closed. Reopen the day before recording a sale.",
                        error = true,
                        tag = NativeUiTags.CashierPostingClosed,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                SectionHeading(
                    title = stringResource(R.string.native_product),
                    supporting = stringResource(R.string.native_switch_product),
                    stacked = useStackedLayout,
                )
                ProductSelector(
                    products = context.products,
                    selectedProductId = product?.productId,
                    stacked = useStackedLayout,
                    onSelected = { option ->
                        selectedProductId = option.productId
                        leaveGuard.markDirty()
                        onClearFeedback()
                    },
                )
                if (context.products.isEmpty()) {
                    StatusMessage(
                        message = "No active sale product has a retail rate. Ask an owner or manager to configure one.",
                        error = true,
                        tag = NativeUiTags.CashierProductUnavailable,
                    )
                }

                SectionDivider()
                SectionHeading(title = stringResource(R.string.native_enter_sale))
                EntryModeSelector(
                    selected = entryMode,
                    onSelected = { nextMode ->
                        if (nextMode != entryMode) {
                            entryValue = convertEntryValue(
                                value = entryValue,
                                from = entryMode,
                                to = nextMode,
                                rate = rate,
                            )
                            entryMode = nextMode
                            leaveGuard.markDirty()
                            onClearFeedback()
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                SaleEntryEditor(
                    value = entryValue,
                    unit = if (entryMode == EntryMode.Quantity) {
                        quantityUnit
                    } else {
                        stringResource(R.string.native_currency_unit)
                    },
                    inputDescription = if (entryMode == EntryMode.Quantity) "Sale quantity" else "Sale amount",
                    decreaseDescription = if (entryMode == EntryMode.Quantity) {
                        stringResource(R.string.native_decrease_quantity)
                    } else {
                        stringResource(R.string.native_decrease_amount)
                    },
                    increaseDescription = if (entryMode == EntryMode.Quantity) {
                        stringResource(R.string.native_increase_quantity)
                    } else {
                        stringResource(R.string.native_increase_amount)
                    },
                    stacked = useStackedLayout,
                    onValueChange = { next ->
                        if (isValidDecimalInput(next)) {
                            entryValue = next
                            leaveGuard.markDirty()
                            onClearFeedback()
                        }
                    },
                    onDecrease = {
                        entryValue = stepEntryValue(
                            value = entryValue,
                            delta = if (entryMode == EntryMode.Quantity) -0.5 else -50.0,
                            mode = entryMode,
                        )
                        leaveGuard.markDirty()
                        onClearFeedback()
                    },
                    onIncrease = {
                        entryValue = stepEntryValue(
                            value = entryValue,
                            delta = if (entryMode == EntryMode.Quantity) 0.5 else 50.0,
                            mode = entryMode,
                        )
                        leaveGuard.markDirty()
                        onClearFeedback()
                    },
                )
                Spacer(Modifier.height(6.dp))
                EntryCalculationRow(
                    rate = stringResource(
                        R.string.native_entry_rate_format,
                        formatAmount(rate),
                        quantityUnit,
                    ),
                    result = if (entryMode == EntryMode.Quantity) {
                        stringResource(R.string.native_sale_total_format, totalText)
                    } else {
                        stringResource(
                            R.string.native_quantity_result_format,
                            quantityText,
                            quantityUnit,
                        )
                    },
                    stacked = useStackedLayout,
                )

                SectionDivider()
                SectionHeading(title = stringResource(R.string.native_payment_destination))
                PaymentSelector(
                    paidNowTitle = stringResource(R.string.native_paid_now),
                    paidNowDetail = paymentAccount?.name ?: "Unavailable",
                    customerTitle = stringResource(R.string.native_customer_account),
                    selected = payment,
                    stacked = useStackedLayout,
                    onPaidNow = {
                        focusManager.clearFocus()
                        payment = PaymentChoice.PaidNow
                        leaveGuard.markDirty()
                        onClearFeedback()
                    },
                    onCustomer = {
                        focusManager.clearFocus()
                        showCustomerPicker = true
                    },
                )
                if (context.paymentAccounts.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    StatusMessage(
                        message = "No active Cash/Bank account is available. Ask an owner or manager to configure one.",
                        error = true,
                        tag = NativeUiTags.CashierPaymentUnavailable,
                    )
                }
                if (payment == PaymentChoice.CustomerAccount) {
                    Spacer(Modifier.height(8.dp))
                    CustomerSelectionRow(
                    selectedCustomer = selectedCustomer?.name,
                        onClick = { showCustomerPicker = true },
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecondaryDetails(
                    expanded = secondaryDetailsExpanded,
                    onToggle = { secondaryDetailsExpanded = !secondaryDetailsExpanded },
                )
                Spacer(Modifier.height(10.dp))
                state.receipt?.let { receipt ->
                    StatusMessage(
                        message = "Sale #${receipt.eventId} recorded · ${formatQuantity(receipt.quantity)} $quantityUnit · PKR ${formatAmount(receipt.totalPaisa / 100.0)}",
                        error = false,
                        tag = NativeUiTags.CashierSaleSuccess,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                state.deliveryFollowUp?.let { followUp ->
                    if (!followUp.sent) {
                        SaleDeliveryFollowUpCard(
                            customerName = followUp.customerName,
                            productName = followUp.productName,
                            quantity = followUp.quantity,
                            sending = followUp.sending,
                            error = followUp.error,
                            onSend = onSendAsDelivery,
                        )
                    } else {
                        StatusMessage(
                            message = "Delivery recorded for sale #${followUp.saleEventId}",
                            error = false,
                            tag = NativeUiTags.CashierSaleDeliveryLinked,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                state.error?.let { error ->
                    StatusMessage(
                        message = error,
                        error = true,
                        tag = NativeUiTags.CashierSaleError,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                ReviewDock(
                    summary = stringResource(
                        R.string.native_sale_summary_format,
                        quantityText,
                        quantityUnit,
                        productName,
                        paymentSummary,
                    ),
                    total = stringResource(R.string.native_sale_total_format, totalText),
                    stockEffect = stringResource(
                        R.string.native_stock_effect_format,
                        quantityText,
                        quantityUnit,
                    ),
                    moneyEffect = if (payment == PaymentChoice.PaidNow) {
                        stringResource(R.string.native_cash_effect_format, totalText)
                    } else {
                        stringResource(R.string.native_customer_effect_format, totalText)
                    },
                    stacked = useStackedLayout,
                    enabled = canRecord,
                    saving = state.saving,
                    buttonTag = NativeUiTags.CashierRecordSale,
                    onRecordSale = {
                        focusManager.clearFocus()
                        val selectedProduct = product ?: return@ReviewDock
                        onRecordSale(
                            NativeSaleRequest(
                                productId = selectedProduct.productId,
                                entryMode = if (entryMode == EntryMode.Quantity) {
                                    NativeSaleEntryMode.QUANTITY
                                } else {
                                    NativeSaleEntryMode.AMOUNT
                                },
                                enteredValue = enteredNumber,
                                paymentKind = if (payment == PaymentChoice.PaidNow) {
                                    NativeSalePaymentKind.PAID_NOW
                                } else {
                                    NativeSalePaymentKind.CUSTOMER_ACCOUNT
                                },
                                paymentAccountId = paymentAccount?.accountId,
                                customerAccountId = selectedCustomer?.accountId,
                            ),
                        )
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

    if (showCustomerPicker) {
        CustomerAccountDialog(
            customers = context.customers,
            selectedCustomerId = selectedCustomerId,
            onSelect = { customer ->
                selectedCustomerId = customer.accountId
                payment = PaymentChoice.CustomerAccount
                showCustomerPicker = false
                leaveGuard.markDirty()
                onClearFeedback()
            },
            onDismiss = { showCustomerPicker = false },
        )
    }

}

@Composable
internal fun SaleLoadingBody(
    modifier: Modifier = Modifier,
) {
    NativeColdLoadingPane(label = "Loading today’s sale details…", modifier = modifier)
}

@Composable
internal fun SaleLoadFailureBody(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, modifier = Modifier.testTag(NativeUiTags.CashierRetryLoad)) {
            Text("Try again")
        }
    }
}

@Composable
internal fun SaleDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard this sale draft?") },
        text = { Text("Your unsaved sale entry will be cleared before leaving.") },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(NativeUiTags.CashierDiscardDraft),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(NativeUiTags.CashierKeepDraft),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
private fun StatusMessage(message: String, error: Boolean, tag: String) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(tag),
    ) {
        Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun String.toDisplayUnit(): String = when (trim().lowercase()) {
    "liters", "liter", "litres", "litre", "l" -> "L"
    "kilograms", "kilogram", "kg" -> "kg"
    else -> this
}

private fun stepEntryValue(value: String, delta: Double, mode: EntryMode): String {
    val current = value.toDoubleOrNull() ?: 0.0
    val stepped = (current + delta).coerceAtLeast(0.0)
    return if (mode == EntryMode.Quantity) formatQuantity(stepped) else formatAmountInput(stepped)
}

private fun convertEntryValue(
    value: String,
    from: EntryMode,
    to: EntryMode,
    rate: Double,
): String {
    val entered = value.toDoubleOrNull() ?: return ""
    val converted = when {
        from == EntryMode.Quantity && to == EntryMode.Amount -> entered * rate
        from == EntryMode.Amount && to == EntryMode.Quantity -> entered / rate
        else -> entered
    }
    return if (to == EntryMode.Quantity) formatQuantity(converted) else formatAmountInput(converted)
}

private fun isValidDecimalInput(value: String): Boolean =
    value.length <= 9 &&
        value.count { it == '.' } <= 1 &&
        value.all { it.isDigit() || it == '.' }

private fun formatQuantity(value: Double): String =
    "%.2f".format(Locale.US, value).trimEnd('0').let { formatted ->
        if (formatted.endsWith('.')) "${formatted}0" else formatted
    }

private fun formatAmount(value: Double): String = when {
    value % 1.0 == 0.0 -> "%,.0f".format(Locale.US, value)
    else -> "%,.2f".format(Locale.US, value)
}

private fun formatAmountInput(value: Double): String = when {
    value % 1.0 == 0.0 -> "%.0f".format(Locale.US, value)
    else -> "%.2f".format(Locale.US, value)
}

@Composable
private fun SectionHeading(title: String, supporting: String? = null, stacked: Boolean = false) {
    if (stacked && supporting != null) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (supporting != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductSelector(
    products: List<NativeSaleProduct>,
    selectedProductId: Long?,
    stacked: Boolean,
    onSelected: (NativeSaleProduct) -> Unit,
) {
    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            products.forEach { option ->
                ProductCard(
                    title = option.name,
                    rate = "PKR ${formatAmount(option.ratePaisaPerUnit / 100.0)} / ${option.unit.toDisplayUnit()} · ${option.quantityOnHand} in stock",
                    selected = selectedProductId == option.productId,
                    onClick = { onSelected(option) },
                    modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.cashierProduct(option.name)),
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            products.forEach { option ->
                ProductCard(
                    title = option.name,
                    rate = "PKR ${formatAmount(option.ratePaisaPerUnit / 100.0)} / ${option.unit.toDisplayUnit()} · ${option.quantityOnHand} in stock",
                    selected = selectedProductId == option.productId,
                    onClick = { onSelected(option) },
                    modifier = Modifier.weight(1f).testTag(NativeUiTags.cashierProduct(option.name)),
                )
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 11.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ProductCard(
    title: String,
    rate: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(13.dp),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 68.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = rate,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntryModeSelector(selected: EntryMode, onSelected: (EntryMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(3.dp)
            .selectableGroup(),
    ) {
        Segment(
            text = stringResource(R.string.native_quantity),
            selected = selected == EntryMode.Quantity,
            onClick = { onSelected(EntryMode.Quantity) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .heightIn(min = 48.dp)
                .testTag(NativeUiTags.CashierModeQuantity),
        )
        Segment(
            text = stringResource(R.string.native_amount),
            selected = selected == EntryMode.Amount,
            onClick = { onSelected(EntryMode.Amount) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .heightIn(min = 48.dp)
                .testTag(NativeUiTags.CashierModeAmount),
        )
    }
}

@Composable
private fun SaleEntryEditor(
    value: String,
    unit: String,
    inputDescription: String,
    decreaseDescription: String,
    increaseDescription: String,
    stacked: Boolean,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val editor: @Composable (Modifier) -> Unit = { modifier ->
        QuantityEditor(
            value = value,
            unit = unit,
            inputDescription = inputDescription,
            onValueChange = onValueChange,
            tag = NativeUiTags.CashierEntryInput,
            modifier = modifier,
        )
    }
    val controls: @Composable () -> Unit = {
        StepButton(
            icon = Icons.Outlined.Remove,
            tag = NativeUiTags.CashierDecrease,
            description = decreaseDescription,
            onClick = onDecrease,
        )
        StepButton(
            icon = Icons.Outlined.Add,
            tag = NativeUiTags.CashierIncrease,
            description = increaseDescription,
            onClick = onIncrease,
        )
    }
    if (stacked) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            editor(Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) { controls() }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            editor(Modifier.weight(1f))
            controls()
        }
    }
}

@Composable
private fun Segment(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuantityEditor(
    value: String,
    unit: String,
    inputDescription: String,
    onValueChange: (String) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.heightIn(min = 64.dp),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline()
                        .semantics { contentDescription = inputDescription }
                        .testTag(tag),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 36.sp,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun EntryCalculationRow(
    rate: String,
    result: String,
    stacked: Boolean,
) {
    if (stacked) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(
                text = rate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = result,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rate,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = result,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PaymentSelector(
    paidNowTitle: String,
    paidNowDetail: String,
    customerTitle: String,
    selected: PaymentChoice,
    stacked: Boolean,
    onPaidNow: () -> Unit,
    onCustomer: () -> Unit,
) {
    val paidNow: @Composable (Modifier) -> Unit = { modifier ->
        PaymentCard(
            title = paidNowTitle,
            detail = paidNowDetail,
            selected = selected == PaymentChoice.PaidNow,
            onClick = onPaidNow,
            modifier = modifier.testTag(NativeUiTags.CashierPaymentPaidNow),
        )
    }
    val customer: @Composable (Modifier) -> Unit = { modifier ->
        PaymentCard(
            title = customerTitle,
            detail = null,
            selected = selected == PaymentChoice.CustomerAccount,
            onClick = onCustomer,
            modifier = modifier.testTag(NativeUiTags.CashierPaymentCustomer),
        )
    }
    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            paidNow(Modifier.fillMaxWidth())
            customer(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            paidNow(Modifier.weight(1f))
            customer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .size(48.dp)
            .testTag(tag),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = description)
        }
    }
}

@Composable
private fun PaymentCard(
    title: String,
    detail: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(13.dp),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (detail != null) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CustomerSelectionRow(
    selectedCustomer: String?,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(NativeUiTags.CashierCustomerSelector)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedCustomer
                        ?: stringResource(R.string.native_choose_customer_account),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (selectedCustomer == null) {
                        stringResource(R.string.native_customer_required)
                    } else {
                        stringResource(R.string.native_sale_added_to_balance)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (selectedCustomer == null) {
                    stringResource(R.string.native_choose)
                } else {
                    stringResource(R.string.native_change)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CustomerAccountDialog(
    customers: List<NativeSaleAccount>,
    selectedCustomerId: Long?,
    onSelect: (NativeSaleAccount) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.native_choose_customer_account)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .testTag(NativeUiTags.CashierCustomerList)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (customers.isEmpty()) {
                    item { Text("No active customer accounts are available.") }
                }
                items(customers, key = { it.accountId }) { customer ->
                    Surface(
                        color = if (selectedCustomerId == customer.accountId) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag(NativeUiTags.cashierCustomer(customer.accountId))
                            .selectable(
                                selected = selectedCustomerId == customer.accountId,
                                onClick = { onSelect(customer) },
                                role = Role.RadioButton,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedCustomerId == customer.accountId,
                                onClick = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(customer.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(NativeUiTags.CashierCustomerCancel),
            ) {
                Text(stringResource(R.string.native_cancel))
            }
        },
    )
}

@Composable
private fun SecondaryDetails(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(NativeUiTags.CashierSecondaryDetails)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .clickable(role = Role.Button, onClick = onToggle),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.native_more_sale_details),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (expanded) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SecondaryDetail(
                        label = stringResource(R.string.native_price_type),
                        value = stringResource(R.string.native_standard_price),
                    )
                    SecondaryDetail(
                        label = stringResource(R.string.native_delivery),
                        value = stringResource(R.string.native_counter_pickup),
                    )
                    SecondaryDetail(
                        label = stringResource(R.string.native_note),
                        value = stringResource(R.string.native_no_note),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryDetail(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ReviewDock(
    summary: String,
    total: String,
    stockEffect: String,
    moneyEffect: String,
    stacked: Boolean,
    enabled: Boolean,
    saving: Boolean,
    buttonTag: String,
    onRecordSale: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            if (stacked) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = total,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = total,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            if (stacked) {
                Text(
                    text = stockEffect,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = moneyEffect,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stockEffect,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = moneyEffect,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(9.dp))
            Button(
                onClick = onRecordSale,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag(buttonTag),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
            ) {
                Text(
                    text = if (saving) "Recording…" else stringResource(R.string.native_record_sale),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
internal fun CashierNavigation(
    selected: CashierSection,
    onNavigate: (CashierSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        CashierNavigationItem(
            icon = Icons.Outlined.PointOfSale,
            label = stringResource(R.string.native_sell),
            selected = selected == CashierSection.Sell,
            tag = NativeUiTags.CashierNavSell,
            onClick = { onNavigate(CashierSection.Sell) },
        )
        CashierNavigationItem(
            icon = Icons.Outlined.Payments,
            label = stringResource(R.string.native_collect),
            selected = selected == CashierSection.Collect,
            tag = NativeUiTags.CashierNavCollect,
            onClick = { onNavigate(CashierSection.Collect) },
        )
        CashierNavigationItem(
            icon = Icons.Outlined.History,
            label = stringResource(R.string.native_activity),
            selected = selected == CashierSection.Activity,
            tag = NativeUiTags.CashierNavActivity,
            onClick = { onNavigate(CashierSection.Activity) },
        )
        CashierNavigationItem(
            icon = Icons.Outlined.MoreHoriz,
            label = stringResource(R.string.native_more),
            selected = selected == CashierSection.More,
            tag = NativeUiTags.CashierNavMore,
            onClick = { onNavigate(CashierSection.More) },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CashierNavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, maxLines = 2) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun SaleDeliveryFollowUpCard(
    customerName: String,
    productName: String,
    quantity: String,
    sending: Boolean,
    error: String?,
    onSend: (String) -> Unit,
) {
    var route by rememberSaveable { mutableStateOf("") }
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.CashierSaleDeliveryCard),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Deliver to $customerName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$productName · $quantity", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = route,
                onValueChange = { route = it },
                label = { Text("Route (optional)") },
                singleLine = true,
                enabled = !sending,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.CashierSaleDeliveryRoute),
            )
            Button(
                onClick = { onSend(route) },
                enabled = !sending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(NativeUiTags.CashierSaleDeliverySend),
            ) {
                Text(if (sending) "Recording…" else "Send as delivery", fontWeight = FontWeight.SemiBold)
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
