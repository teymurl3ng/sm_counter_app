package com.saaf.milkshop.nativeui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPinEntryPolicyTest {
    @Test fun acceptsOnlyFourToTwelveAsciiDigitsWithoutChangingLeadingZeroes() {
        assertFalse(QuickPinEntryPolicy.valid("123"))
        assertTrue(QuickPinEntryPolicy.valid("0004"))
        assertTrue(QuickPinEntryPolicy.valid("000000000012"))
        assertFalse(QuickPinEntryPolicy.valid("1234567890123"))
        assertFalse(QuickPinEntryPolicy.valid("12a4"))
        assertFalse(QuickPinEntryPolicy.valid("１２３４"))
        assertTrue(QuickPinEntryPolicy.valid("0004".toCharArray()))
        assertFalse(QuickPinEntryPolicy.valid(null))
    }

    @Test fun rejectsInvalidPasteInsteadOfSilentlySavingADifferentPin() {
        assertEquals("0004", QuickPinEntryPolicy.accept("0004", "0004a"))
        assertEquals("0004", QuickPinEntryPolicy.accept("0004", "0004567890123"))
        assertEquals("000456", QuickPinEntryPolicy.accept("0004", "000456"))
        assertEquals("", QuickPinEntryPolicy.accept("0004", ""))
    }
}
