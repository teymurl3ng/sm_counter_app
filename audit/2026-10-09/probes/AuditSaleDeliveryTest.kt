package com.saaf.milkshop.data

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.nativeui.sale.NativeSaleDeliveryFollowUp
import com.saaf.milkshop.nativeui.sale.NativeSaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Calls the production follow-up calculation; no network or sales are posted. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AuditSaleDeliveryTest {
    @Test fun wholesaleAmountDeliveryMustUseRecordedSaleQuantity() {
        val db = MilkShopDatabase.createInMemory(driver = null)
        val store = ViewModelStore()
        try {
            val security = UserSecurityService(db)
            val vm = NativeSaleViewModel(NativeSaleFacade(db, security), NativeDeliveriesFacade(db, security)) {}
            store.put("audit", vm)
            val context = NativeSaleContext(
                businessDate = "2026-10-09", postingOpen = true,
                products = listOf(NativeSaleProduct(1, "Milk", "Liters", 40000, "100")),
                customers = listOf(NativeSaleAccount(2, "Audit customer")), paymentAccounts = emptyList(),
                defaultPaymentAccountId = null, defaultPaymentAccountBalancePaisa = null,
                todaySalesPaisa = 0, defaultSurchargeRs = 50.0,
            )
            val request = NativeSaleRequest(
                productId = 1, entryMode = NativeSaleEntryMode.AMOUNT, enteredValue = 1200.0,
                paymentKind = NativeSalePaymentKind.CUSTOMER_ACCOUNT,
                paymentAccountId = null, customerAccountId = 2,
                wholesale = true, wholesaleRatePaisa = 30000,
            )
            val method = NativeSaleViewModel::class.java.getDeclaredMethod(
                "deliveryFollowUpFor", NativeSaleContext::class.java, NativeSaleRequest::class.java,
            ).apply { isAccessible = true }
            val result = method.invoke(vm, context, request) as NativeSaleDeliveryFollowUp
            assertEquals("1200 rupees at the chosen wholesale rate of 300 buys 4 liters", 4.0, result.quantity.toDouble(), 0.000001)
        } finally {
            store.clear()
            db.close()
        }
    }
}
