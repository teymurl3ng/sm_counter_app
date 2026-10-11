package com.saaf.milkshop.nativeui

import com.saaf.milkshop.nativeui.common.ReceiptOutcomeMemory
import org.junit.Assert.*
import org.junit.Test

class ReceiptOutcomeMemoryTest {
    @Test fun returningToReceiptDoesNotProduceAnotherSaveSignal() {
        val outcomes = ReceiptOutcomeMemory()
        assertTrue(outcomes.accept("sale", 42))
        assertFalse(outcomes.accept("sale", 42))
        assertTrue(outcomes.accept("sale", 43))
        assertFalse(outcomes.accept("sale", 42))
        assertTrue(outcomes.accept("count", 42))
    }
}
