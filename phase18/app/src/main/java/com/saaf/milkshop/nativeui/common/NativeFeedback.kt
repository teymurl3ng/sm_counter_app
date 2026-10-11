package com.saaf.milkshop.nativeui.common

import kotlinx.coroutines.flow.MutableSharedFlow

data class NativeFeedback(val message: String, val success: Boolean)

/** Consumable outcomes, rather than receipts replayed whenever a screen mounts. */
object NativeFeedbackBus {
    val outcomes = MutableSharedFlow<NativeFeedback>(extraBufferCapacity = 16)
    fun saved(message: String) { outcomes.tryEmit(NativeFeedback(message, true)) }
    private var lastFailure: Pair<String, Long>? = null
    @Synchronized fun failed(message: String) {
        val now = android.os.SystemClock.uptimeMillis()
        val last = lastFailure
        if (last != null && last.first == message && now - last.second < 500) return
        lastFailure = message to now
        outcomes.tryEmit(NativeFeedback(message, false))
    }
}


@androidx.compose.runtime.Composable
internal fun NativeFailureFeedback(state: Any?, active: Boolean) {
    val message = nativeFailureMessage(state)
    androidx.compose.runtime.LaunchedEffect(message, active) {
        if (active && !message.isNullOrBlank()) NativeFeedbackBus.failed(message)
    }
}

private fun nativeFailureMessage(state: Any?): String? = when (state) {
    is com.saaf.milkshop.nativeui.activity.NativeActivityState.Ready -> state.error
    is com.saaf.milkshop.nativeui.activity.NativeActivityState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.admin.NativeAdminWorkState.Ready -> state.error
    is com.saaf.milkshop.nativeui.admin.NativeAdminWorkState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.auth.NativeAuthenticationState.OwnerSetup -> state.error
    is com.saaf.milkshop.nativeui.auth.NativeAuthenticationState.AccountPicker -> state.error
    is com.saaf.milkshop.nativeui.auth.NativeAuthenticationState.Passphrase -> state.error
    is com.saaf.milkshop.nativeui.auth.NativeAuthenticationState.MethodPicker -> state.error
    is com.saaf.milkshop.nativeui.auth.NativeAuthenticationState.QuickPin -> state.error
    is com.saaf.milkshop.nativeui.auth.NativeAuthenticationState.StartupFailure -> state.message
    is com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState.Ready -> state.error
    is com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.collection.NativeCollectionState.Ready -> state.error
    is com.saaf.milkshop.nativeui.collection.NativeCollectionState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.drawing.NativeDrawingState.Ready -> state.error
    is com.saaf.milkshop.nativeui.drawing.NativeDrawingState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.expense.NativeExpenseState.Ready -> state.error
    is com.saaf.milkshop.nativeui.expense.NativeExpenseState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.manager.NativeManagerWorkState.Ready -> state.error
    is com.saaf.milkshop.nativeui.manager.NativeManagerWorkState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.more.NativeMoreState.Ready -> state.error
    is com.saaf.milkshop.nativeui.more.NativeMoreState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.operations.NativeAttendanceState.Ready -> state.error
    is com.saaf.milkshop.nativeui.operations.NativeAttendanceState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.operations.NativeDeliveriesState.Ready -> state.error
    is com.saaf.milkshop.nativeui.operations.NativeDeliveriesState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.operations.NativeMakeYogurtState.Ready -> state.error
    is com.saaf.milkshop.nativeui.operations.NativeMakeYogurtState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.operations.NativeOperationsMoreState.Ready -> state.error
    is com.saaf.milkshop.nativeui.operations.NativeOperationsMoreState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.operations.NativeOperationsWorkState.Ready -> state.error
    is com.saaf.milkshop.nativeui.operations.NativeOperationsWorkState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.operations.NativeReceiveMilkState.Ready -> state.error
    is com.saaf.milkshop.nativeui.operations.NativeReceiveMilkState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageState.Ready -> state.error
    is com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState.Ready -> state.error ?: when (state.destination) {
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.Products -> state.productsError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.Customers -> state.customersError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.Suppliers -> state.suppliersError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.Employees -> state.employeesError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.CostingPlans -> state.costingPoliciesError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.ShopSettings -> state.shopSettingsError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.UsersRoles -> state.usersError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.SecurityHistory -> state.securityHistoryError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.Backup -> state.backupError
        com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination.Sync -> state.syncState.error
        else -> state.error
    }
    is com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.owner.NativeOwnerOverviewState.Ready -> state.error
    is com.saaf.milkshop.nativeui.owner.NativeOwnerOverviewState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState.Ready -> state.error ?: state.correction?.error
    is com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.owner.NativeOwnerReportsState.Ready -> state.error
    is com.saaf.milkshop.nativeui.owner.NativeOwnerReportsState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.owner.NativeOwnerWorkState.Ready -> state.error
    is com.saaf.milkshop.nativeui.owner.NativeOwnerWorkState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.salarypayment.NativeSalaryPaymentState.Ready -> state.error
    is com.saaf.milkshop.nativeui.salarypayment.NativeSalaryPaymentState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.sale.NativeSaleState.Ready -> state.error
    is com.saaf.milkshop.nativeui.sale.NativeSaleState.LoadFailure -> state.message
    is com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState.Ready -> state.error
    is com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState.LoadFailure -> state.message
    else -> null
}

internal fun savedReadbackFailure(saved: Boolean, error: Throwable): Throwable =
    if (saved && error !is com.saaf.milkshop.domain.SecurityFailure)
        IllegalStateException("Saved on this phone. The latest view could not refresh; try Refresh.", error)
    else error
