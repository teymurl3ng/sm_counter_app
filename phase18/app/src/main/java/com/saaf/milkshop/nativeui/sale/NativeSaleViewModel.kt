package com.saaf.milkshop.nativeui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.sale.NativeSaleContext
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.application.sale.NativeSaleReceipt
import com.saaf.milkshop.application.sale.NativeSaleRequest
import com.saaf.milkshop.application.sale.NativeSaleEntryMode
import com.saaf.milkshop.application.sale.NativeSalePaymentKind
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.SessionRequestGate
import com.saaf.milkshop.nativeui.DayStateBus
import com.saaf.milkshop.nativeui.DataChangeBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Follow-up offer shown after a credit sale: one delivery, born-linked to
 * the sale, pre-filled with the sale's customer/product/quantity.
 */
data class NativeSaleDeliveryFollowUp(
    val customerId: Long,
    val productId: Long,
    val customerName: String,
    val productName: String,
    val quantity: String,
    val saleEventId: Long,
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
)

sealed interface NativeSaleState {
    data object Idle : NativeSaleState
    data object Loading : NativeSaleState
    data class Ready(
        val context: NativeSaleContext,
        val saving: Boolean = false,
        val receipt: NativeSaleReceipt? = null,
        val deliveryFollowUp: NativeSaleDeliveryFollowUp? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeSaleState
    data class LoadFailure(val message: String) : NativeSaleState
}

class NativeSaleViewModel(
    private val facade: NativeSaleFacade,
    private val deliveriesFacade: NativeDeliveriesFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeSaleState>(NativeSaleState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeSaleState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeSaleState.Idle) {
            // Warm re-entry: revalidate reference data silently — keep the
            // visible form exactly as the user left it.
            silentRefresh()
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    /**
     * Creates a delivery born-linked to the just-recorded credit sale, so the
     * customer pick on a credit sale can dispatch the driver without manual
     * matching later. Customer/product/quantity flow from the sale request.
     */
    /** Sends the follow-up delivery born-linked to the recorded credit sale. */
    fun sendAsDelivery(route: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeSaleState.Ready ?: return
        val followUp = current.deliveryFollowUp ?: return
        if (followUp.sending || followUp.sent) return
        _state.value = current.copy(
            deliveryFollowUp = followUp.copy(sending = true, error = null),
        )
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    deliveriesFacade.createDeliveryForSale(
                        sessionHandle = session,
                        customerAccountId = followUp.customerId,
                        productId = followUp.productId,
                        quantity = followUp.quantity,
                        saleEventId = followUp.saleEventId,
                        route = route.ifBlank { null },
                    )
                }
                val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                _state.value = latest.copy(
                    deliveryFollowUp = followUp.copy(sending = false, sent = true),
                )
                DataChangeBus.notifyDataChanged()
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeSaleState.Idle
                    onSessionExpired()
                } else {
                    val latest = _state.value as? NativeSaleState.Ready
                    if (latest != null) {
                        _state.value = latest.copy(
                            deliveryFollowUp = followUp.copy(sending = false, error = publicMessage(error)),
                        )
                    }
                }
            }
        }
    }


    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeSaleState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeSaleState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                // Merge: a refresh must never wipe the posted receipt or the
                // delivery follow-up card.
                _state.value = previous?.copy(context = context, refreshing = false)
                    ?: NativeSaleState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: "The sale could not be refreshed. Check it and try again.",
                    )
                }
            }
        }
    }

    /**
     * Warm-open reload: revalidates reference data without announcing it —
     * no refreshing flag, no state replacement unless the fresh data differs.
     * Receipt and follow-up card survive; failures leave the form as-is.
     */
    private fun silentRefresh() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeSaleState.Ready ?: return
        scope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) { facade.load(session) }
                if (sessionHandle != session) return@launch
                val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                val merged = latest.copy(context = fresh, refreshing = false)
                if (merged != _state.value) _state.value = merged
            } catch (error: SecurityFailure.SessionExpired) {
                sessionHandle = null
                _state.value = NativeSaleState.Idle
                onSessionExpired()
            } catch (error: Throwable) {
                // Silent: the displayed data stays; it will revalidate on
                // the next entry or day-state change.
            }
        }
    }

    fun record(request: NativeSaleRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeSaleState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        val followUp = deliveryFollowUpFor(current.context, request)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeSaleState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                    deliveryFollowUp = followUp?.copy(
                        saleEventId = submission.receipt.eventId,
                    ),
                )
                // Records/Activity hold the receipt context, not this VM — a
                // self-refresh here would wipe the posted receipt and the
                // delivery follow-up card.
                DataChangeBus.notifyDataChanged()
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeSaleState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }


    private fun deliveryFollowUpFor(
        context: NativeSaleContext,
        request: NativeSaleRequest,
    ): NativeSaleDeliveryFollowUp? {
        if (request.paymentKind != NativeSalePaymentKind.CUSTOMER_ACCOUNT) return null
        val customerId = request.customerAccountId ?: return null
        val product = context.products.firstOrNull { it.productId == request.productId }
            ?: return null
        val quantityText = when (request.entryMode) {
            NativeSaleEntryMode.QUANTITY -> request.enteredValue.toString()
            NativeSaleEntryMode.AMOUNT -> {
                val rate = product.ratePaisaPerUnit / 100.0
                if (rate <= 0.0) return null
                followUpQuantityText(request.enteredValue / rate)
            }
        }
        val customerName = context.customers.firstOrNull { it.accountId == customerId }?.name ?: ""
        return NativeSaleDeliveryFollowUp(
            customerId = customerId,
            productId = request.productId,
            customerName = customerName,
            productName = product.name,
            quantity = quantityText,
            saleEventId = 0L,
        )
    }

    /** Amount-entry credit sales derive the delivery quantity from amount ÷ rate. */
    private fun followUpQuantityText(quantity: Double): String =
        if (quantity == kotlin.math.floor(quantity)) {
            "${quantity.toInt()}.0"
        } else {
            String.format(Locale.US, "%.2f", quantity)
        }

    fun clearFeedback() {
        val current = _state.value as? NativeSaleState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    /**
     * "Start new sale": clears the posted receipt and its follow-up offer so
     * the cheque pane hands back to the entry form. The screen resets the
     * form fields to fresh defaults (quantity included) as part of the same
     * tap — nothing carries over.
     */
    fun startNextSale() {
        val current = _state.value as? NativeSaleState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null, deliveryFollowUp = null)
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeSaleState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeSaleState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeSaleState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Sale access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The sale could not be recorded. Check it and try again."
        else -> "The sale could not be recorded. Check it and try again."
    }

    class Factory(
        private val facade: NativeSaleFacade,
        private val deliveriesFacade: NativeDeliveriesFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeSaleViewModel::class.java))
            return NativeSaleViewModel(facade, deliveriesFacade, onSessionExpired) as T
        }
    }
}
