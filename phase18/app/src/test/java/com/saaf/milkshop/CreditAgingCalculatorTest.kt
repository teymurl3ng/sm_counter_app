package com.saaf.milkshop

import com.saaf.milkshop.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CreditAgingCalculatorTest {
    private val asOf = LocalDate.parse("2026-10-15")
    private fun fact(id: Long, amount: String, date: String = "2026-10-01", type: String = "SALE", parent: Long? = null, uid: String = "event-$id") =
        CreditAgeFact(id, uid, parent, type, date, BigDecimal(amount))
    @Test fun partialFullPaymentAndAdvance() {
        val sales = listOf(fact(1, "100"), fact(2, "200", "2026-10-10"))
        val partial = CustomerCreditAging.calculate(sales + fact(3, "-150", type = "CREDIT_COLLECTION"), 15000, asOf)
        assertEquals(CreditAge(true, 5), partial)
        assertEquals(CreditAge(true), CustomerCreditAging.calculate(sales + fact(3, "-300", type = "CREDIT_COLLECTION"), 0, asOf))
        assertEquals(CreditAge(true), CustomerCreditAging.calculate(listOf(fact(1, "-120", type = "CREDIT_COLLECTION"), fact(2, "100")), -2000, asOf))
    }
    @Test fun openingDebtIsUnknownAndPaidFirst() {
        val rows = listOf(fact(1, "100", type = "OPENING_STATE"), fact(2, "80", "2026-10-10"), fact(3, "-50", type = "CREDIT_COLLECTION"))
        assertEquals(CreditAge(true, 5, 5000), CustomerCreditAging.calculate(rows, 13000, asOf))
    }
    @Test fun correctionFamiliesRetainOriginalAgeAndPaymentMeaning() {
        val rows = listOf(fact(1, "100"), fact(2, "100", "2026-10-10"), fact(3, "60", parent = 1), fact(4, "-120", type = "CREDIT_COLLECTION"))
        assertEquals(CreditAge(true, 14), CustomerCreditAging.calculate(rows, 14000, asOf))
        val collection = listOf(fact(1, "200"), fact(2, "-150", type = "CREDIT_COLLECTION"), fact(3, "50", type = "CREDIT_COLLECTION", parent = 2), fact(4, "0", parent = 1))
        assertEquals(CreditAge(true, 14), CustomerCreditAging.calculate(collection, 10000, asOf))
    }
    @Test fun stableUidOrderingAndSubPaisaDust() {
        val a = listOf(fact(1, "0.004", uid = "a"), fact(2, "1", "2026-10-10", uid = "b"))
        val b = listOf(fact(400, "1", "2026-10-10", uid = "b"), fact(70, "0.004", uid = "a"))
        assertEquals(CreditAge(true, 5), CustomerCreditAging.calculate(a, 100, asOf))
        assertEquals(CustomerCreditAging.calculate(a, 100, asOf), CustomerCreditAging.calculate(b, 100, asOf))
        assertEquals(CreditAge(true, 14), CustomerCreditAging.calculate(listOf(fact(1, "0.005")), 1, asOf))
    }
    @Test fun incompleteOrInconsistentHistoryNeverInventsAge() {
        for (rows in listOf(listOf(fact(1, "100", parent = 99)), listOf(fact(1, "100", parent = 2), fact(2, "0", parent = 1)),
            listOf(fact(1, "100", "bad date")), listOf(fact(1, "100", "2026-10-16")), listOf(fact(1, "100", type = "UNSUPPORTED")),
            listOf(fact(1, "100"), fact(2, "10", "2026-10-10", parent = 1)))) {
            assertFalse(CustomerCreditAging.calculate(rows, 10000, asOf).available)
        }
        assertFalse(CustomerCreditAging.calculate(listOf(fact(1, "100")), 9999, asOf).available)
    }
    @Test fun sharedSaleAmountIncludesSurchargeForQuantityAndAmount() {
        assertEquals(230.0, SaleAmounts.calculate(180.0, 1.0, null, 50.0).total, 0.0)
        assertEquals(230.0, SaleAmounts.calculate(90.0, null, 180.0, 50.0).total, 0.0)
    }
}
