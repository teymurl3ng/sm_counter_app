package com.saaf.milkshop.nativeui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.sale.NativeSaleContext
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.application.sale.NativeSaleOperations
import com.saaf.milkshop.application.sale.NativeSaleReceipt
import com.saaf.milkshop.application.sale.NativeSaleRequest
import com.saaf.milkshop.application.sale.NativeSalePaymentKind
import com.saaf.milkshop.application.sale.NativeSaleCreditSummary
import com.saaf.milkshop.application.sale.NativeSaleCreditWarning
import com.saaf.milkshop.application.sale.CreditWarningRequired
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
import kotlinx.coroutines.CancellationException

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
        val creditSummary: NativeSaleCreditSummary? = null,
        val creditLoading: Boolean = false,
        val creditError: Boolean = false,
        val creditWarning: NativeSaleCreditWarning? = null,
    ) : NativeSaleState
    data class LoadFailure(val message: String) : NativeSaleState
}

class NativeSaleViewModel(
    private val facade: NativeSaleOperations,
    private val deliveriesFacade: NativeDeliveriesFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeSaleState>(NativeSaleState.Idle)
    private var sessionHandle: String? = null
    private var sessionGeneration = 0L
    private val loadGate = SessionRequestGate()
    private var creditJob: kotlinx.coroutines.Job? = null
    private var creditRequestId = 0L
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
        sessionGeneration += 1
        creditJob?.cancel()
        creditRequestId++
        _state.value = NativeSaleState.Idle
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
        val generation = sessionGeneration
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
                DataChangeBus.notifyDataChanged()
                if (generation != sessionGeneration || sessionHandle != session) return@launch
                val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                if (latest.deliveryFollowUp?.saleEventId != followUp.saleEventId) return@launch
                _state.value = latest.copy(
                    deliveryFollowUp = followUp.copy(sending = false, sent = true),
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (generation != sessionGeneration || sessionHandle != session) return@launch
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeSaleState.Idle
                    onSessionExpired()
                } else {
                    val latest = _state.value as? NativeSaleState.Ready
                    if (latest != null && latest.deliveryFollowUp?.saleEventId == followUp.saleEventId) {
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
                _state.value = (_state.value as? NativeSaleState.Ready)?.copy(context = context, refreshing = false)
                    ?: NativeSaleState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                    _state.value = latest.copy(
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
        if (_state.value !is NativeSaleState.Ready) return
        val request = loadGate.begin(session)
        scope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                val merged = latest.copy(context = fresh, refreshing = false)
                if (merged != _state.value) _state.value = merged
            } catch (error: SecurityFailure.SessionExpired) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                sessionHandle = null
                _state.value = NativeSaleState.Idle
                onSessionExpired()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                // Silent: the displayed data stays; it will revalidate on
                // the next entry or day-state change.
            }
        }
    }

    fun record(request: NativeSaleRequest) {
        val session = sessionHandle ?: return
        val generation = sessionGeneration
        val current = _state.value as? NativeSaleState.Ready ?: return
        if (current.saving) return
        creditJob?.cancel()
        creditRequestId++
        _state.value = current.copy(saving = true, receipt = null, error = null, creditWarning = null, creditLoading = false)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                DataChangeBus.notifyDataChanged()
                if (generation != sessionGeneration || sessionHandle != session) return@launch
                _state.value = NativeSaleState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                    deliveryFollowUp = deliveryFollowUpFor(current.context, request, submission.receipt)?.copy(
                        saleEventId = submission.receipt.eventId,
                    ),
                )
                // Records/Activity hold the receipt context, not this VM — a
                // self-refresh here would wipe the posted receipt and the
                // delivery follow-up card.
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (generation != sessionGeneration || sessionHandle != session) return@launch
                if (error is CreditWarningRequired) {
                    val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                    _state.value = latest.copy(saving = false, creditWarning = error.warning,
                        creditSummary = error.warning.summary, creditLoading = false, creditError = false)
                    return@launch
                }
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeSaleState.Idle
                    onSessionExpired()
                } else {
                    val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                    _state.value = latest.copy(saving = false, error = publicMessage(error))
                }
            }
        }
    }


    private fun deliveryFollowUpFor(
        context: NativeSaleContext,
        request: NativeSaleRequest,
        receipt: NativeSaleReceipt,
    ): NativeSaleDeliveryFollowUp? {
        if (request.paymentKind != NativeSalePaymentKind.CUSTOMER_ACCOUNT) return null
        val customerId = request.customerAccountId ?: return null
        val product = context.products.firstOrNull { it.productId == request.productId }
            ?: return null
        val quantityText = receipt.quantity.toString()
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

    fun clearFeedback() {
        val current = _state.value as? NativeSaleState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null, creditWarning = null)
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
        creditJob?.cancel()
        creditRequestId++
        sessionGeneration += 1
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeSaleState.Idle
    }

    /** Selected-customer detail only; superseded keystrokes/session results cannot overwrite a new draft. */
    fun previewCredit(request: NativeSaleRequest?) {
        creditJob?.cancel()
        val requestId = ++creditRequestId
        val session = sessionHandle ?: return
        val generation = sessionGeneration
        val current = _state.value as? NativeSaleState.Ready ?: return
        _state.value = current.copy(creditSummary = null, creditLoading = request != null, creditError = false)
        if (request == null) return
        creditJob = scope.launch {
            try {
                kotlinx.coroutines.delay(150)
                val summary = withContext(Dispatchers.IO) { facade.previewCredit(session, request) }
                if (generation != sessionGeneration || sessionHandle != session || requestId != creditRequestId) return@launch
                val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                _state.value = latest.copy(creditSummary = summary, creditLoading = false, creditError = summary == null)
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) {
                if (generation != sessionGeneration || sessionHandle != session || requestId != creditRequestId) return@launch
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeSaleState.Idle
                    onSessionExpired()
                } else {
                    val latest = _state.value as? NativeSaleState.Ready ?: return@launch
                    _state.value = latest.copy(creditLoading = false, creditError = true)
                }
            }
        }
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
