package com.saaf.milkshop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.nativeui.sale.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class NativeSaleStateRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val context = NativeSaleContext("2026-10-10", true,
        listOf(NativeSaleProduct(1, "Milk", "Liters", 19000, "10 L")),
        listOf(NativeSaleAccount(7, "Customer")), emptyList(), null, null, 0, 50.0)
    private val request = NativeSaleRequest(1, NativeSaleEntryMode.AMOUNT, 190.0,
        NativeSalePaymentKind.CUSTOMER_ACCOUNT, null, 7, wholesale = true, wholesaleRatePaisa = 10000)
    private val receipt = NativeSaleReceipt(42, 1.2345678, 19000)
    private val records = AtomicInteger()
    private var pendingLoad: CompletableDeferred<NativeSaleContext>? = null
    private var pendingPreview: CompletableDeferred<NativeSaleCreditSummary?>? = null
    private var requireWarning = false
    private val previews = AtomicInteger()
    private val credit = NativeSaleCreditSummary(7, "Customer", 80000, 90000, 99000, 19000,
        com.saaf.milkshop.domain.CreditAge(true, 4), false, "reviewed")
    private lateinit var pendingRecord: CompletableDeferred<NativeSaleSubmission>
    private lateinit var db: MilkShopDatabase
    private lateinit var vm: NativeSaleViewModel
    private val store = ViewModelStore()
    @Before fun setup() {
        db = MilkShopDatabase.createInMemory()
        pendingRecord = CompletableDeferred()
        val port = object : NativeSaleOperations {
            override suspend fun load(sessionHandle: String, date: LocalDate) = pendingLoad?.await() ?: context
            override suspend fun previewCredit(sessionHandle: String, request: NativeSaleRequest, date: LocalDate): NativeSaleCreditSummary? {
                previews.incrementAndGet()
                return pendingPreview?.await() ?: credit.copy(customerId = request.customerAccountId!!)
            }
            override suspend fun record(sessionHandle: String, request: NativeSaleRequest, date: LocalDate): NativeSaleSubmission {
                if (requireWarning && request.creditAcknowledgment != credit.warningKey) {
                    throw CreditWarningRequired(NativeSaleCreditWarning(request, credit))
                }
                records.incrementAndGet(); return pendingRecord.await()
            }
        }
        compose.runOnIdle {
            vm = NativeSaleViewModel(port, NativeDeliveriesFacade(db, UserSecurityService(db))) {}
            store.put("sale", vm)
            vm.open("session")
        }
        compose.waitUntil(5000) { vm.state.value is NativeSaleState.Ready }
    }
    @After fun cleanup() { compose.runOnIdle { store.clear() }; db.close() }
    @Test fun wholesaleFollowUpUsesExactBookedQuantity() {
        compose.runOnIdle { vm.record(request) }
        pendingRecord.complete(NativeSaleSubmission(receipt, context))
        compose.waitUntil(5000) { (vm.state.value as? NativeSaleState.Ready)?.receipt != null }
        assertEquals(receipt.quantity.toString(), (vm.state.value as NativeSaleState.Ready).deliveryFollowUp!!.quantity)
    }
    @Test fun warningCancelDoesNotPostAndContinueGuardsDuplicateTap() {
        requireWarning = true
        pendingPreview = CompletableDeferred()
        compose.runOnIdle { vm.previewCredit(request); vm.record(request) }
        compose.waitUntil(5000) { (vm.state.value as? NativeSaleState.Ready)?.creditWarning != null }
        assertEquals(0, records.get())
        assertFalse((vm.state.value as NativeSaleState.Ready).creditLoading)
        assertEquals(credit, (vm.state.value as NativeSaleState.Ready).creditSummary)
        compose.runOnIdle { vm.clearFeedback() }
        assertNull((vm.state.value as NativeSaleState.Ready).creditWarning)
        compose.runOnIdle { vm.record(request) }
        compose.waitUntil(5000) { (vm.state.value as? NativeSaleState.Ready)?.creditWarning != null }
        compose.runOnIdle {
            vm.record(request.copy(creditAcknowledgment = credit.warningKey))
            vm.record(request.copy(creditAcknowledgment = credit.warningKey))
        }
        compose.waitUntil(5000) { records.get() == 1 }
        pendingRecord.complete(NativeSaleSubmission(receipt, context))
        compose.waitUntil(5000) { (vm.state.value as? NativeSaleState.Ready)?.receipt == receipt }
        assertEquals(1, records.get())
    }
    @Test fun supersededPreviewAndReopenedSessionCannotLeakCustomerDetail() {
        val old = CompletableDeferred<NativeSaleCreditSummary?>()
        pendingPreview = old
        compose.runOnIdle { vm.previewCredit(request) }
        compose.waitUntil(5000) { previews.get() == 1 }
        pendingPreview = null
        compose.runOnIdle { vm.previewCredit(request.copy(customerAccountId = 8)) }
        compose.waitUntil(5000) { (vm.state.value as? NativeSaleState.Ready)?.creditSummary?.customerId == 8L }
        old.complete(credit)
        compose.runOnIdle { assertEquals(8L, (vm.state.value as NativeSaleState.Ready).creditSummary!!.customerId) }
        compose.runOnIdle { vm.close(); vm.open("another-session") }
        compose.waitUntil(5000) { vm.state.value is NativeSaleState.Ready }
        assertNull((vm.state.value as NativeSaleState.Ready).creditSummary)
        assertFalse((vm.state.value as NativeSaleState.Ready).creditLoading)
    }
    @Test fun finishingRefreshCannotReopenSaveGateOrEraseReceipt() {
        pendingLoad = CompletableDeferred()
        compose.runOnIdle { vm.refresh(); vm.record(request) }
        compose.waitUntil(5000) { records.get() == 1 }
        pendingLoad!!.complete(context.copy(todaySalesPaisa = 123))
        compose.waitUntil(5000) { !(vm.state.value as NativeSaleState.Ready).refreshing }
        assertTrue((vm.state.value as NativeSaleState.Ready).saving)
        compose.runOnIdle { vm.record(request) }
        assertEquals(1, records.get())
        pendingLoad = CompletableDeferred()
        compose.runOnIdle { vm.refresh() }
        pendingRecord.complete(NativeSaleSubmission(receipt, context))
        compose.waitUntil(5000) { (vm.state.value as? NativeSaleState.Ready)?.receipt != null }
        pendingLoad!!.complete(context.copy(todaySalesPaisa = 999))
        compose.waitUntil(5000) { (vm.state.value as? NativeSaleState.Ready)?.context?.todaySalesPaisa == 999L }
        assertEquals(receipt, (vm.state.value as NativeSaleState.Ready).receipt)
    }
    @Test fun oldSaveCannotPublishIntoReopenedSameSessionHandle() {
        compose.runOnIdle { vm.record(request) }
        compose.waitUntil(5000) { records.get() == 1 }
        compose.runOnIdle { vm.close(); vm.open("session") }
        compose.waitUntil(5000) { vm.state.value is NativeSaleState.Ready }
        val observer = CoroutineScope(Dispatchers.Main)
        val committed = CompletableDeferred<Unit>()
        try {
            compose.runOnIdle {
                observer.launch(start = CoroutineStart.UNDISPATCHED) {
                    com.saaf.milkshop.nativeui.DataChangeBus.changes.first()
                    committed.complete(Unit)
                }
            }
            pendingRecord.complete(NativeSaleSubmission(receipt, context))
            // The bus notification proves the old job reached its post-commit
            // main-thread continuation; waiting for the Deferred alone did not.
            compose.waitUntil(5000) { committed.isCompleted }
            compose.runOnIdle { assertNull((vm.state.value as NativeSaleState.Ready).receipt) }
        } finally { observer.cancel() }
    }
}
