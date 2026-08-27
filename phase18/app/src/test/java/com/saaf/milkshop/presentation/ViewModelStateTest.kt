package com.saaf.milkshop.presentation

import com.saaf.milkshop.presentation.dashboard.DashboardViewModel
import com.saaf.milkshop.data.DashboardDataSource
import com.saaf.milkshop.domain.AnalyticsPeriodSummary
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.presentation.records.HistoryCorrectionsViewModel
import com.saaf.milkshop.presentation.transactions.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewModelStateTest {
    @Test
    fun dashboardPeriod_isStoredWithoutUiDependency() {
        val vm = DashboardViewModel(FakeDashboardDataSource)
        vm.selectPeriod(DashboardViewModel.Period.WEEK)
        assertEquals(DashboardViewModel.Period.WEEK, vm.state.period)
        assertEquals(DashboardViewModel.Period.WEEK, vm.stateFlow.value.period)
    }

    @Test
    fun saleState_preservesSelectionsAndModes() {
        val vm = SaleViewModel()
        vm.setReferenceData(
            products = listOf(SaleViewModel.ProductState(1L, "Milk", "L")),
            customers = listOf(SaleViewModel.CustomerState(2L, "Customer")),
            paymentAccounts = listOf(SaleViewModel.PaymentAccountState(3L, "Cash")),
            settings = SaleViewModel.Settings(100.0, 200.0, 10.0),
        )
        vm.setSelections(0, 0, 0)
        vm.setModes(quantityMode = false, creditMode = true, surchargeEnabled = true)
        assertEquals(1L, vm.selectedProductId())
        assertEquals(2L, vm.selectedCustomerAccountId())
        assertEquals(3L, vm.selectedPaymentAccountId())
        assertTrue(vm.state.surchargeEnabled)
        assertTrue(vm.state.creditMode)
        assertEquals(vm.state, vm.stateFlow.value)
    }

    @Test
    fun historyState_tracksSelectedEventAndLoadedDetail() {
        val vm = HistoryCorrectionsViewModel()
        assertEquals(null, vm.state.selectedEventId)
        assertEquals(false, vm.state.detailLoading)
        vm.selectEvent(EventEntity(createdAt = "2026-01-01T00:00:00Z", occurredAt = "2026-01-01", eventType = "test"))
        assertEquals(0L, vm.state.selectedEventId)
        assertTrue(vm.state.detailLoading)
        vm.setDetail("detail")
        assertEquals("detail", vm.state.detailText)
        assertEquals(false, vm.state.detailLoading)
        assertEquals(vm.state, vm.stateFlow.value)
    }
}


private object FakeDashboardDataSource : DashboardDataSource {
    override suspend fun loadSnapshot(): DashboardDataSource.Snapshot =
        error("Not used by this state-only test")

    override suspend fun loadAnalyticsSummary(
        startDate: String,
        endDateExclusive: String,
    ): AnalyticsPeriodSummary = error("Not used by this state-only test")
}
