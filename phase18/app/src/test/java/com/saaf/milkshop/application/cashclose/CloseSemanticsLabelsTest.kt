package com.saaf.milkshop.application.cashclose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM tests for the H8/H9 close-semantics labels (decisions locked
 * 2026-10-06): absorbed-day enumeration, multi-day variance coverage, and
 * the reopen reverse-consequence.
 */
class CloseSemanticsLabelsTest {

    @Test
    fun absorbedDaysLine_enumeratesWithCounts() {
        assertEquals(
            "This close also seals: Mon 5 Oct (2 events), Tue 6 Oct (1 event). " +
                "They keep their events but get no count of their own.",
            CloseSemanticsLabels.absorbedDaysLine(listOf("2026-10-05" to 2, "2026-10-06" to 1)),
        )
    }

    @Test
    fun absorbedDaysLine_nullWhenNothingAbsorbed() {
        assertNull(CloseSemanticsLabels.absorbedDaysLine(emptyList()))
    }

    @Test
    fun absorbedDaysLine_singularEvent() {
        assertEquals(
            "This close also seals: Mon 5 Oct (1 event). " +
                "They keep their events but get no count of their own.",
            CloseSemanticsLabels.absorbedDaysLine(listOf("2026-10-05" to 1)),
        )
    }

    @Test
    fun coverageLine_namesEveryCoveredDay() {
        assertEquals(
            "This count covers Tue 6 Oct + Mon 5 Oct — the variance is multi-day.",
            CloseSemanticsLabels.coverageLine("2026-10-06", listOf("2026-10-05")),
        )
    }

    @Test
    fun coverageLine_nullWhenSingleDay() {
        assertNull(CloseSemanticsLabels.coverageLine("2026-10-06", emptyList()))
    }

    @Test
    fun reopenLine_statesTheReverseConsequence() {
        assertEquals(
            "This also reopens Mon 5 Oct — they were sealed by Tue 6 Oct's close.",
            CloseSemanticsLabels.reopenLine("2026-10-06", listOf("2026-10-05")),
        )
    }

    @Test
    fun reopenLine_nullWhenNoAbsorbedDays() {
        assertNull(CloseSemanticsLabels.reopenLine("2026-10-06", emptyList()))
    }

    @Test
    fun humanizeDate_fallsBackToRawOnGarbage() {
        assertEquals("Mon 5 Oct", CloseSemanticsLabels.humanizeDate("2026-10-05"))
        assertEquals("not-a-date", CloseSemanticsLabels.humanizeDate("not-a-date"))
    }
}
