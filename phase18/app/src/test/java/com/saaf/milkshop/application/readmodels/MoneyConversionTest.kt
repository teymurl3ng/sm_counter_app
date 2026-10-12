package com.saaf.milkshop.application.readmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyConversionTest {
    @Test
    fun decimalTiesRoundAwayFromZero() {
        listOf(2.125 to 213L, -2.125 to -213L, 1.005 to 101L, -1.005 to -101L,
            0.005 to 1L, -0.005 to -1L, 2.135 to 214L, -2.135 to -214L)
            .forEach { (rupees, paisa) -> assertEquals("Rs $rupees", paisa, money(rupees)) }
    }

    @Test
    fun valuesImmediatelyBesideTiesStayOnTheirOwnSide() {
        assertEquals(212L, money(Math.nextDown(2.125)))
        assertEquals(213L, money(Math.nextUp(2.125)))
        assertEquals(-213L, money(Math.nextDown(-2.125)))
        assertEquals(-212L, money(Math.nextUp(-2.125)))
    }

    @Test
    fun ordinaryAndLargeSafeAmountsRemainExact() {
        listOf(0.0 to 0L, -0.0 to 0L, 76.5 to 7_650L, 61.25 to 6_125L,
            -12.75 to -1_275L, 1_000_000_000_000.0 to 100_000_000_000_000L)
            .forEach { (rupees, paisa) -> assertEquals(paisa, money(rupees)) }
    }

    @Test
    fun nonFiniteAndOverflowAreRejectedInsteadOfSaturated() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { value -> assertThrows(IllegalArgumentException::class.java) { money(value) } }
        listOf(1e17, -1e17, Double.MAX_VALUE, -Double.MAX_VALUE)
            .forEach { value -> assertThrows(ArithmeticException::class.java) { money(value) } }
    }
}
