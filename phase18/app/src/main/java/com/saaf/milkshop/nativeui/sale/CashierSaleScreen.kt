package com.saaf.milkshop.nativeui.sale

import com.saaf.milkshop.nativeui.common.NativeBottomNavigation
import com.saaf.milkshop.nativeui.common.NativeNavigationLabel
import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.nativeListItem

import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
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
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
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
import com.saaf.milkshop.application.sale.NativeSaleReceipt
import com.saaf.milkshop.application.sale.NativeSaleRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.hapticCommit
import com.saaf.milkshop.nativeui.common.hapticConfirm
import com.saaf.milkshop.nativeui.common.hapticSelect
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.imePaddingExcludingNavBar
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import java.util.Locale
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.FlowRow

private enum class EntryMode { Quantity, Amount }
private enum class PaymentChoice { PaidNow, CustomerAccount }
enum class CashierSection { Sell, Collect, Activity, More }

private data class SaleReceiptPresentation(
    val productName: String,
    val quantityUnit: String,
    val paymentSummary: String,
    val priceLabel: String?,
    val note: String?,
)

@Composable
fun CashierSaleScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeSaleState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onRecordSale: (NativeSaleRequest) -> Unit,
    onSendAsDelivery: (String) -> Unit,
    onStartNextSale: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onRegisterGuard: (EntryLeaveGuard?) -> Unit = {},
    onPreviewCredit: (NativeSaleRequest?) -> Unit = {},
) {
    val leaveGuard = rememberEntryLeaveGuard()
    LaunchedEffect(state is NativeSaleState.Ready) {
        if (state !is NativeSaleState.Ready) leaveGuard.clear()
    }
    DisposableEffect(leaveGuard) {
        onRegisterGuard(leaveGuard)
        onDispose { onRegisterGuard(null) }
    }
    // T17: back at the Sell root is swallowed, dirty draft included —
    // back must never discard a sale or end the session.
    BackHandler { }
    Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
        NativeSectionTransition(
            targetState = state,
            contentKey = { if (it is NativeSaleState.Idle || it is NativeSaleState.Loading) NativeSaleState.Loading::class else it::class },
        ) { state -> when (state) {
            NativeSaleState.Idle,
            NativeSaleState.Loading,
            -> SaleLoadingBody()
            is NativeSaleState.LoadFailure -> SaleLoadFailureBody(
                message = state.message,
                onRetry = onRetry,
            )
            is NativeSaleState.Ready -> SaleEntryContent(
                identity = identity,
                state = state,
                onClearFeedback = onClearFeedback,
                onRecordSale = onRecordSale,
                onPreviewCredit = onPreviewCredit,
                onSendAsDelivery = onSendAsDelivery,
                onStartNextSale = onStartNextSale,
                leaveGuard = leaveGuard,
                contentPadding = PaddingValues(0.dp),
                showHeader = false,
            )
        }
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
    val context = LocalContext.current
    NavigationRailItem(
        selected = selected,
        onClick = { context.hapticTap(); onClick() },
        modifier = Modifier.testTag(tag).heightIn(min = 56.dp),
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        icon = { Icon(icon, contentDescription = null) },
        label = { NativeNavigationLabel(label) },
    )
}

@Composable
internal fun SaleEntryContent(
    identity: NativeSignedInIdentity,
    state: NativeSaleState.Ready,
    onClearFeedback: () -> Unit,
    onRecordSale: (NativeSaleRequest) -> Unit,
    onSendAsDelivery: (String) -> Unit,
    onStartNextSale: () -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onPreviewCredit: (NativeSaleRequest?) -> Unit = {},
) {
    val context = state.context
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var entryMode by rememberSaveable { mutableStateOf(EntryMode.Quantity) }
    var payment by rememberSaveable { mutableStateOf(PaymentChoice.PaidNow) }
    var entryValue by rememberSaveable { mutableStateOf("") }
    var selectedCustomerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCustomerPicker by rememberSaveable { mutableStateOf(false) }
    var paidNowAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPaymentPicker by rememberSaveable { mutableStateOf(false) }
    var secondaryDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    var saleNote by rememberSaveable { mutableStateOf("") }
    var wholesale by rememberSaveable { mutableStateOf(false) }
    var wholesaleRateText by rememberSaveable { mutableStateOf("") }
    // H7: Delivery Surcharge (spec §7) — optional toggle, editable amount,
    // prefilled from the shop's default_surcharge setting.
    var surchargeEnabled by rememberSaveable { mutableStateOf(false) }
    var surchargeText by rememberSaveable { mutableStateOf("") }
    DisposableEffect(leaveGuard) {
        leaveGuard.onDiscard = {
            selectedProductId = null
            entryMode = EntryMode.Quantity
            payment = PaymentChoice.PaidNow
            entryValue = ""
            selectedCustomerId = null
            showCustomerPicker = false
            paidNowAccountId = null
            showPaymentPicker = false
            secondaryDetailsExpanded = false
            saleNote = ""
            wholesale = false
            wholesaleRateText = ""
            surchargeEnabled = false
            surchargeText = ""
            onClearFeedback()
        }
        onDispose { leaveGuard.onDiscard = null }
    }
    val focusManager = LocalFocusManager.current
    val useStackedLayout = LocalDensity.current.fontScale >= 1.3f

    val product = context.products.firstOrNull { it.productId == selectedProductId }
        ?: context.products.firstOrNull()
    val selectedCustomer = context.customers.firstOrNull { it.accountId == selectedCustomerId }
    // Paid-now destination: the cashier's explicit choice (Cash, Bank,
    // JazzCash…) falling back to the shop default. Sales are no longer
    // locked to whichever account happens to be named "Cash".
    val paidNowAccount = context.paymentAccounts.firstOrNull {
        it.accountId == (paidNowAccountId ?: context.defaultPaymentAccountId)
    } ?: context.paymentAccounts.firstOrNull()
    val wholesaleRate = wholesaleRateText.toDoubleOrNull()?.takeIf { it > 0.0 }
    val wholesaleRatePaisa = wholesaleRate?.let { (it * 100.0).let { v -> kotlin.math.round(v).toLong() } }
    val activeRatePaisa = if (wholesale) wholesaleRatePaisa ?: product?.ratePaisaPerUnit ?: 0L
    else product?.ratePaisaPerUnit ?: 0L
    val rate = activeRatePaisa / 100.0
    val productName = product?.name.orEmpty()
    val quantityUnit = product?.unit?.toDisplayUnit().orEmpty()
    val enteredNumber = entryValue.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val quantity = if (entryMode == EntryMode.Quantity) enteredNumber else {
        if (rate > 0.0) enteredNumber / rate else 0.0
    }
    val previewAmounts = runCatching {
        com.saaf.milkshop.domain.SaleAmounts.calculate(rate,
            enteredNumber.takeIf { entryMode == EntryMode.Quantity },
            enteredNumber.takeIf { entryMode == EntryMode.Amount },
            if (surchargeEnabled) surchargeText.toDoubleOrNull() ?: context.defaultSurchargeRs else 0.0)
    }.getOrNull()
    val total = previewAmounts?.total ?: 0.0
    val quantityText = formatQuantity(quantity)
    val totalText = formatAmount(total)
    val paymentSummary = when (payment) {
        PaymentChoice.PaidNow -> paidNowAccount?.name ?: "Payment unavailable"
        PaymentChoice.CustomerAccount -> selectedCustomer?.name
            ?: stringResource(R.string.native_customer_account)
    }
    val canRecord = context.postingOpen && product != null && quantity > 0.0 && activeRatePaisa > 0L && !state.saving && previewAmounts != null &&
        (!wholesale || wholesaleRatePaisa != null) &&
        when (payment) {
            PaymentChoice.PaidNow -> paidNowAccount != null
            PaymentChoice.CustomerAccount -> selectedCustomer != null
        }

    val draftRequest = product?.let { selected -> NativeSaleRequest(
        productId = selected.productId,
        entryMode = if (entryMode == EntryMode.Quantity) NativeSaleEntryMode.QUANTITY else NativeSaleEntryMode.AMOUNT,
        enteredValue = enteredNumber,
        paymentKind = if (payment == PaymentChoice.PaidNow) NativeSalePaymentKind.PAID_NOW else NativeSalePaymentKind.CUSTOMER_ACCOUNT,
        paymentAccountId = paidNowAccount?.accountId,
        customerAccountId = selectedCustomer?.accountId,
        notes = saleNote.trim().takeIf { it.isNotEmpty() },
        wholesale = wholesale, wholesaleRatePaisa = wholesaleRatePaisa,
        surchargeEnabled = surchargeEnabled,
        surchargeAmountRs = surchargeText.toDoubleOrNull()?.takeIf { surchargeEnabled }) }
    val creditPreviewRequest = draftRequest?.takeIf {
        payment == PaymentChoice.CustomerAccount && selectedCustomer != null && activeRatePaisa > 0 &&
            enteredNumber.isFinite() && enteredNumber >= 0 && (!wholesale || wholesaleRatePaisa != null) && state.receipt == null
    }
    LaunchedEffect(creditPreviewRequest, activeRatePaisa, context.businessDate, context.defaultSurchargeRs) {
        onPreviewCredit(creditPreviewRequest)
    }

    // Keep the recorded draft while showing its receipt. Next sale explicitly
    // clears the draft; the outgoing receipt retains its own presentation
    // snapshot throughout the fade. Saved values do not trigger a leave warning.
    LaunchedEffect(state.receipt?.eventId) {
        if (state.receipt != null) {
            leaveGuard.clear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize().nativeRefreshOverlay(state.refreshing)
            .padding(contentPadding)
            .imePaddingExcludingNavBar(),
    ) {
            if (showHeader) CashierScreenHeader(
                title = stringResource(R.string.native_new_sale),
                displayName = identity.displayName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            NativeSectionTransition(
            targetState = state to SaleReceiptPresentation(
                productName, quantityUnit, paymentSummary,
                if (wholesale && rate > 0.0) "Wholesale · ${formatAmount(rate)} / $quantityUnit" else null,
                saleNote.trim().takeIf { it.isNotEmpty() },
            ),
            contentKey = { it.first.receipt != null },
            modifier = Modifier.weight(1f),
        ) { pane ->
            val state = pane.first
            val presentation = pane.second
        if (state.receipt != null) {
                // Confirmation cheque: the receipt is its own screen. The form
                // state stays frozen at the recorded values while the pane is
                // up, so the cheque can show exactly what was booked.
                SaleChequePane(
                    receipt = state.receipt,
                    followUp = state.deliveryFollowUp,
                    productName = presentation.productName,
                    quantityUnit = presentation.quantityUnit,
                    paymentSummary = presentation.paymentSummary,
                    priceLabel = presentation.priceLabel,
                    note = presentation.note,
                    onStartNextSale = {
                        selectedProductId = null
                        entryMode = EntryMode.Quantity
                        payment = PaymentChoice.PaidNow
                        entryValue = ""
                        selectedCustomerId = null
                        paidNowAccountId = null
                        secondaryDetailsExpanded = false
                        saleNote = ""
                        wholesale = false
                        wholesaleRateText = ""
                        surchargeEnabled = false
                        surchargeText = ""
                        leaveGuard.clear()
                        onStartNextSale()
                    },
                    onSendAsDelivery = onSendAsDelivery,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                ProductSelector(
                    products = context.products,
                    selectedProductId = product?.productId,
                    stacked = useStackedLayout,
                    wholesale = wholesale,
                    wholesaleRatePaisa = wholesaleRatePaisa,
                    onSelected = { option ->
                        selectedProductId = option.productId
                        leaveGuard.markDirty()
                        onClearFeedback()
                    },
                )


                SectionDivider()
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
                EntryCalculationResult(
                    result = if (entryMode == EntryMode.Quantity) {
                        stringResource(R.string.native_sale_total_format, totalText)
                    } else {
                        stringResource(
                            R.string.native_quantity_result_format,
                            quantityText,
                            quantityUnit,
                        )
                    },
                )

                SectionDivider()
                PaymentSelector(
                    paidNowTitle = stringResource(R.string.native_paid_now),
                    paidNowDetail = paidNowAccount?.name ?: "Unavailable",
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
                if (payment == PaymentChoice.PaidNow && paidNowAccount != null) {
                    Spacer(Modifier.height(8.dp))
                    PaymentAccountSelectionRow(
                        accountName = paidNowAccount.name,
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { showPaymentPicker = true },
                    )
                }
                if (payment == PaymentChoice.CustomerAccount) {
                    Spacer(Modifier.height(8.dp))
                    CustomerSelectionRow(
                    selectedCustomer = selectedCustomer?.name,
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { showCustomerPicker = true },
                    )
                }
                Spacer(Modifier.height(8.dp))
                // H7: Delivery Surcharge — first-class input per spec §7.
                if (payment == PaymentChoice.CustomerAccount && selectedCustomer != null) {
                    Spacer(Modifier.height(8.dp))
                    CreditSummaryContent(state.creditSummary?.takeIf { it.customerId == selectedCustomer.accountId }, state.creditLoading)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.native_sale_surcharge_label))
                        if (surchargeEnabled) {
                            OutlinedTextField(
                                value = surchargeText,
                                onValueChange = { next ->
                                    if (isValidDecimalInput(next)) {
                                        surchargeText = next
                                        leaveGuard.markDirty()
                                        onClearFeedback()
                                    }
                                },
                                enabled = surchargeEnabled,
                                singleLine = true,
                                label = { Text(stringResource(R.string.native_sale_surcharge_amount)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(NativeUiTags.CashierSurchargeAmount),
                            )
                        }
                    }
                    Switch(
                        checked = surchargeEnabled,
                        onCheckedChange = { next ->
                            surchargeEnabled = next
                            if (next && surchargeText.isBlank()) {
                                surchargeText = formatAmount(context.defaultSurchargeRs)
                            }
                            leaveGuard.markDirty()
                            onClearFeedback()
                        },
                        modifier = Modifier.testTag(NativeUiTags.CashierSurchargeToggle),
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecondaryDetails(
                    expanded = secondaryDetailsExpanded,
                    onToggle = { secondaryDetailsExpanded = !secondaryDetailsExpanded },
                    unit = quantityUnit,
                    retailRateText = product?.ratePaisaPerUnit?.let { formatAmount(it / 100.0) } ?: "",
                    wholesale = wholesale,
                    onWholesaleSelected = { next ->
                        if (next != wholesale) {
                            if (next && wholesaleRateText.isBlank()) {
                                wholesaleRateText = product?.ratePaisaPerUnit
                                    ?.let { formatAmount(it / 100.0) } ?: ""
                            }
                            wholesale = next
                            leaveGuard.markDirty()
                            onClearFeedback()
                        }
                    },
                    wholesaleRateText = wholesaleRateText,
                    onWholesaleRateChange = { next ->
                        if (isValidDecimalInput(next)) {
                            wholesaleRateText = next
                            leaveGuard.markDirty()
                            onClearFeedback()
                        }
                    },
                    note = saleNote,
                    onNoteChange = { next ->
                        saleNote = next.take(240)
                        leaveGuard.markDirty()
                        onClearFeedback()
                    },
                )
                Spacer(Modifier.height(10.dp))
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
                        stringResource(
                            R.string.native_paid_account_effect_format,
                            paidNowAccount?.name ?: stringResource(R.string.native_cash),
                            totalText,
                        )
                    } else {
                        stringResource(R.string.native_customer_effect_format, totalText)
                    },
                    stacked = useStackedLayout,
                    enabled = canRecord,
                    saving = state.saving,
                    buttonTag = NativeUiTags.CashierRecordSale,
                    onRecordSale = {
                        focusManager.clearFocus()
                        draftRequest?.let(onRecordSale)
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
            }
        }
        }

    state.creditWarning?.let { warning ->
        val summary = warning.summary
        AlertDialog(
            modifier = Modifier.testTag(NativeUiTags.SaleCreditWarning),
            onDismissRequest = onClearFeedback,
            title = { Text(if (summary.checkUnavailable) "Credit check unavailable" else "Above credit limit") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(summary.customerName, style = MaterialTheme.typography.titleMedium)
                    Text("This sale has not been saved.")
                    Text("Current balance: ${creditBalanceText(summary.balancePaisa)}")
                    Text("This sale: PKR ${formatAmount(summary.saleTotalPaisa / 100.0)}")
                    Text("After sale: ${creditBalanceText(summary.projectedBalancePaisa)}")
                    Text("Credit limit: ${if (summary.checkUnavailable) "Unavailable" else creditBalanceText(summary.limitPaisa)}")
                    Text("You can go back or record this credit sale deliberately. Based on records on this phone.")
                }
            },
            confirmButton = {
                TextButton(enabled = !state.saving,
                    modifier = Modifier.testTag(NativeUiTags.SaleCreditWarningContinue),
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                        onRecordSale(warning.request.copy(creditAcknowledgment = summary.warningKey))
                    }) { Text("Record credit sale") }
            },
            dismissButton = {
                TextButton(onClick = com.saaf.milkshop.nativeui.common.hapticAction(onClearFeedback),
                    modifier = Modifier.testTag(NativeUiTags.SaleCreditWarningBack)) { Text("Back") }
            })
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

    if (showPaymentPicker) {
        SalePaymentPickerDialog(
            accounts = context.paymentAccounts,
            selectedId = paidNowAccount?.accountId,
            onSelect = { account ->
                paidNowAccountId = account.accountId
                payment = PaymentChoice.PaidNow
                showPaymentPicker = false
                leaveGuard.markDirty()
                onClearFeedback()
            },
            onDismiss = { showPaymentPicker = false },
        )
    }

}

@Composable
private fun PaymentAccountSelectionRow(
    accountName: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(NativeUiTags.CashierPaymentAccountSelector)
            .clickable(role = Role.Button) { context.hapticSelect(); onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = accountName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.native_change),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SalePaymentPickerDialog(
    accounts: List<NativeSaleAccount>,
    selectedId: Long?,
    onSelect: (NativeSaleAccount) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.native_paid_now)) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Selection, selected = account.accountId == selectedId)
                            .clickable(onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelect(account) })
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = if (account.accountId == selectedId) {
                                    "${account.name}, selected"
                                } else account.name
                            }
                            .testTag(NativeUiTags.cashierPaymentAccount(account.accountId))
                            .nativeListItemContent(NativeListStyle.Selection),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (account.accountId == selectedId) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticAction(onDismiss),
                modifier = Modifier.testTag(NativeUiTags.CashierPaymentCancel),
            ) { Text(stringResource(R.string.native_cancel)) }
        },
    )
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
    NativeLoadFailurePane(
        message = message,
        onRetry = onRetry,
        modifier = modifier,
        retryTestTag = NativeUiTags.CashierRetryLoad,
    )
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
                onClick = com.saaf.milkshop.nativeui.common.hapticDestructiveAction(leaveGuard::confirmDiscard),
                modifier = Modifier.testTag(NativeUiTags.CashierDiscardDraft),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticAction(leaveGuard::keepEditing),
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

internal fun formatAmount(value: Double): String = when {
    value % 1.0 == 0.0 -> "%,.0f".format(Locale.US, value)
    else -> "%,.2f".format(Locale.US, value)
}

private fun formatAmountInput(value: Double): String = when {
    value % 1.0 == 0.0 -> "%.0f".format(Locale.US, value)
    else -> "%.2f".format(Locale.US, value)
}

@Composable
private fun ProductSelector(
    products: List<NativeSaleProduct>,
    selectedProductId: Long?,
    stacked: Boolean,
    wholesale: Boolean,
    wholesaleRatePaisa: Long?,
    onSelected: (NativeSaleProduct) -> Unit,
) {
    if (products.isEmpty()) {
        Text("No active products. Ask an Owner or Admin to add a product in Products.",
            modifier = Modifier.testTag(NativeUiTags.CashierProductUnavailable),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    if (products.size > 6) {
        val pickerFocus = LocalFocusManager.current
        var pickerOpen by rememberSaveable { mutableStateOf(false) }
        var search by rememberSaveable { mutableStateOf("") }
        val selected = products.firstOrNull { it.productId == selectedProductId } ?: products.first()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProductCard(selected, selected = true,
                wholesale = wholesale, wholesaleRatePaisa = wholesaleRatePaisa, opensPicker = true,
                onClick = com.saaf.milkshop.nativeui.common.hapticAction { search = ""; pickerOpen = true },
                modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.CashierProductPickerOpen))
        }
        if (pickerOpen) {
            AlertDialog(
                onDismissRequest = { pickerOpen = false },
                title = { Text("Choose product") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(search, onValueChange = { search = it },
                            label = { Text("Search products") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { pickerFocus.clearFocus() }),
                            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.CashierProductSearch))
                        val visible = products.filter { it.name.contains(search.trim(), ignoreCase = true) }
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (visible.isEmpty()) item { Text("No matching products") }
                            items(visible, key = { it.productId }) { option ->
                                ProductCard(option, option.productId == selectedProductId,
                                    wholesale = wholesale, wholesaleRatePaisa = wholesaleRatePaisa,
                                    onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction {
                                        onSelected(option)
                                        pickerOpen = false
                                    }, modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.cashierProductPick(option.productId)))
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = com.saaf.milkshop.nativeui.common.hapticAction { pickerOpen = false },
                    modifier = Modifier.testTag(NativeUiTags.CashierProductPickerCancel)) { Text("Cancel") } },
            )
        }
        return
    }
    if (stacked || products.size > 2) {
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            products.forEach { option ->
                ProductCard(
                    product = option,
                    selected = selectedProductId == option.productId,
                    wholesale = wholesale,
                    wholesaleRatePaisa = wholesaleRatePaisa,
                    onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelected(option) },
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
                    product = option,
                    selected = selectedProductId == option.productId,
                    wholesale = wholesale,
                    wholesaleRatePaisa = wholesaleRatePaisa,
                    onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelected(option) },
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
    product: NativeSaleProduct,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    wholesale: Boolean = false,
    wholesaleRatePaisa: Long? = null,
    opensPicker: Boolean = false,
) {
    val context = LocalContext.current
    val usesWholesale = selected && wholesale
    val displayedRatePaisa = if (usesWholesale) wholesaleRatePaisa else product.ratePaisaPerUnit
    val hasPrice = displayedRatePaisa != null && displayedRatePaisa > 0L
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .heightIn(min = 72.dp)
            .selectable(
                selected = selected,
                onClick = { context.hapticSelect(); onClick() },
                role = Role.RadioButton,
            ),
    ) {
        Column(
            modifier = Modifier.nativeListItemContent(NativeListStyle.Entity),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text(
                text = when {
                    usesWholesale && !hasPrice -> stringResource(R.string.native_enter_wholesale_rate)
                    !hasPrice -> "No retail price. Set a price in Products."
                    usesWholesale -> stringResource(R.string.native_wholesale_price_format,
                        formatAmount(requireNotNull(displayedRatePaisa) / 100.0), product.unit.toDisplayUnit())
                    else -> "PKR ${formatAmount(requireNotNull(displayedRatePaisa) / 100.0)} / ${product.unit.toDisplayUnit()}"
                },
                style = if (!hasPrice) MaterialTheme.typography.bodyMedium
                    else com.saaf.milkshop.nativeui.theme.nativeMetricTextStyle(),
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else if (!hasPrice) MaterialTheme.colorScheme.onSurfaceVariant
                    else com.saaf.milkshop.nativeui.theme.nativeVisualColors.metric,
            )
            Text("${product.quantityOnHand} in stock",
                style = com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle(),
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    else com.saaf.milkshop.nativeui.theme.nativeVisualColors.supporting)
            if (opensPicker) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.native_change_product),
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                }
            }
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
            onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelected(EntryMode.Quantity) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .heightIn(min = 48.dp)
                .testTag(NativeUiTags.CashierModeQuantity),
        )
        Segment(
            text = stringResource(R.string.native_amount),
            selected = selected == EntryMode.Amount,
            onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelected(EntryMode.Amount) },
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
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton) { context.hapticSelect(); onClick() }
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
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                )
                            }
                            inner()
                        }
                    },
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
private fun EntryCalculationResult(
    result: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
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
    val context = LocalContext.current
    Surface(
        onClick = { context.hapticTap(); onClick() },
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
    val context = LocalContext.current
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(13.dp),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton) { context.hapticSelect(); onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerSelectionRow(
    selectedCustomer: String?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(NativeUiTags.CashierCustomerSelector)
            .clickable(role = Role.Button) { context.hapticSelect(); onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
    val pickerFocus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()
    val digits = trimmed.filter(Char::isDigit)
    val nameCounts = remember(customers) {
        customers.groupingBy { it.name.trim().lowercase(Locale.ROOT) }.eachCount()
    }
    val matches = customers.filter { customer ->
        trimmed.isBlank() || customer.name.contains(trimmed, ignoreCase = true) ||
            (digits.isNotBlank() && trimmed.none(Char::isLetter) && customer.phone?.filter(Char::isDigit)?.contains(digits) == true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.native_choose_customer_account)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Search name or phone") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { pickerFocus.clearFocus() }),
                trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" },
                    modifier = Modifier.testTag(NativeUiTags.SaleCreditSearchClear)) { Text("Clear") } },
                modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.SaleCreditSearch))
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
                if (customers.isNotEmpty() && matches.isEmpty()) item { Text("No customers match that search.") }
                items(matches, key = { it.accountId }) { customer ->
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .nativeListItem(NativeListStyle.Selection, selected = selectedCustomerId == customer.accountId)
                            .testTag(NativeUiTags.cashierCustomer(customer.accountId))
                            .selectable(
                                selected = selectedCustomerId == customer.accountId,
                                onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelect(customer) },
                                role = Role.RadioButton,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().nativeListItemContent(NativeListStyle.Selection),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedCustomerId == customer.accountId,
                                onClick = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(customer.name, style = MaterialTheme.typography.titleMedium)
                                Text(listOfNotNull(customer.phone?.takeIf { it.isNotBlank() }, customer.areaRoute?.takeIf { it.isNotBlank() },
                                    "Customer #${customer.accountId}".takeIf { (nameCounts[customer.name.trim().lowercase(Locale.ROOT)] ?: 0) > 1 }).joinToString(" · ")
                                    .ifBlank { "Customer #${customer.accountId}" }, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            }
        },
        confirmButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticAction(onDismiss),
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
    unit: String,
    retailRateText: String,
    wholesale: Boolean,
    onWholesaleSelected: (Boolean) -> Unit,
    wholesaleRateText: String,
    onWholesaleRateChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
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
                .clickable(role = Role.Button, onClick = com.saaf.milkshop.nativeui.common.hapticAction(onToggle)),
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.native_price_type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectableGroup()
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Segment(
                                text = stringResource(R.string.native_standard_price),
                                selected = !wholesale,
                                onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onWholesaleSelected(false) },
                                modifier = Modifier.weight(1f),
                            )
                            Segment(
                                text = "Wholesale",
                                selected = wholesale,
                                onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onWholesaleSelected(true) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (wholesale) {
                        OutlinedTextField(
                            value = wholesaleRateText,
                            onValueChange = onWholesaleRateChange,
                            label = { Text("Wholesale rate (PKR / $unit)") },
                            placeholder = { Text(retailRateText) },
                            supportingText = { Text("Prefilled with the retail rate — adjust per sale.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.CashierWholesaleRate),
                        )
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        label = { Text(stringResource(R.string.native_note)) },
                        supportingText = { Text("Optional · ${note.length}/240") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.CashierSaleNote),
                    )
                    Text(
                        text = stringResource(R.string.native_delivery_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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
    val context = LocalContext.current
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
            val press2 = rememberPressSource()
            Button(
                onClick = { context.hapticCommit(); onRecordSale() },
                enabled = enabled,
                modifier = Modifier
                    .pressScale(press2)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(buttonTag),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
                interactionSource = press2,
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
    NativeBottomNavigation {
        CashierNavigationItem(
            icon = Icons.Outlined.PointOfSale,
            label = stringResource(R.string.native_sell),
            selected = selected == CashierSection.Sell,
            tag = NativeUiTags.CashierNavSell,
            onClick = com.saaf.milkshop.nativeui.common.hapticAction { onNavigate(CashierSection.Sell) },
        )
        CashierNavigationItem(
            icon = Icons.Outlined.Payments,
            label = stringResource(R.string.native_collect),
            selected = selected == CashierSection.Collect,
            tag = NativeUiTags.CashierNavCollect,
            onClick = com.saaf.milkshop.nativeui.common.hapticAction { onNavigate(CashierSection.Collect) },
        )
        CashierNavigationItem(
            icon = Icons.Outlined.History,
            label = stringResource(R.string.native_activity),
            selected = selected == CashierSection.Activity,
            tag = NativeUiTags.CashierNavActivity,
            onClick = com.saaf.milkshop.nativeui.common.hapticAction { onNavigate(CashierSection.Activity) },
        )
        CashierNavigationItem(
            icon = Icons.Outlined.MoreHoriz,
            label = stringResource(R.string.native_more),
            selected = selected == CashierSection.More,
            tag = NativeUiTags.CashierNavMore,
            onClick = com.saaf.milkshop.nativeui.common.hapticAction { onNavigate(CashierSection.More) },
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
    val context = LocalContext.current
    NavigationBarItem(
        selected = selected,
        onClick = { context.hapticTap(); onClick() },
        modifier = Modifier.testTag(tag),
        icon = { Icon(icon, contentDescription = null) },
        label = { NativeNavigationLabel(label) },
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
    val context = LocalContext.current
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.CashierSaleDeliveryCard),
    ) {
        Column(
            Modifier.padding(16.dp),
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
            val press1 = rememberPressSource()
            Button(
                onClick = { context.hapticCommit(); onSend(route) },
                enabled = !sending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
                modifier = Modifier
                    .pressScale(press1)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(NativeUiTags.CashierSaleDeliverySend),
                interactionSource = press1,
            ) {
                Text(if (sending) "Recording…" else "Send as delivery", fontWeight = FontWeight.SemiBold)
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Confirmation cheque shown in place of the entry form while a recorded sale
 * is on screen. Mirrors CollectionSuccess: big check, total, receipt rows,
 * then the delivery offer and the next action. "Start new sale" clears the
 * cheque and resets the form to fresh defaults — quantity included — so the
 * next sale always starts clean.
 */
@Composable
private fun SaleChequePane(
    receipt: NativeSaleReceipt,
    followUp: NativeSaleDeliveryFollowUp?,
    productName: String,
    quantityUnit: String,
    paymentSummary: String,
    priceLabel: String?,
    note: String?,
    onStartNextSale: () -> Unit,
    onSendAsDelivery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    com.saaf.milkshop.nativeui.common.NativeReceiptFeedback("sale", receipt.eventId)
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(NativeUiTags.CashierSaleCheque),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text("Sale recorded", style = MaterialTheme.typography.headlineMedium)
        Text(
            "PKR ${formatAmount(receipt.totalPaisa / 100.0)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        com.saaf.milkshop.nativeui.common.NativeInformationSection {
                com.saaf.milkshop.nativeui.common.NativeInformationFact("Sale", "#S-${receipt.eventId}")
                com.saaf.milkshop.nativeui.common.NativeInformationFact("Product", productName)
                com.saaf.milkshop.nativeui.common.NativeInformationFact("Quantity", "${formatQuantity(receipt.quantity)} $quantityUnit", emphasized = true)
                com.saaf.milkshop.nativeui.common.NativeInformationFact("Payment", paymentSummary)
                if (priceLabel != null) {
                    com.saaf.milkshop.nativeui.common.NativeInformationFact("Price", priceLabel, emphasized = true)
                }
                if (note != null) {
                    com.saaf.milkshop.nativeui.common.NativeInformationFact("Note", note, prose = true)
                }
        }
        followUp?.let { followUp ->
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
        }
        val press = rememberPressSource()
        Button(
            onClick = { context.hapticCommit(); onStartNextSale() },
            colors = ButtonDefaults.buttonColors(
                containerColor = SaafYellow,
                contentColor = SaafInk,
            ),
            modifier = Modifier
                .pressScale(press)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(NativeUiTags.CashierSaleNext),
            interactionSource = press,
        ) {
            Text(
                "Start new sale",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
