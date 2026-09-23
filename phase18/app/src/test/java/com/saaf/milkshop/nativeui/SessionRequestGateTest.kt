package com.saaf.milkshop.nativeui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRequestGateTest {
    @Test
    fun newerRefreshInvalidatesOlderResult() {
        val gate = SessionRequestGate()
        val stale = gate.begin("owner-session")
        val current = gate.begin("owner-session")

        assertFalse(gate.isCurrent(stale, "owner-session"))
        assertTrue(gate.isCurrent(current, "owner-session"))
    }

    @Test
    fun sessionReplacementRejectsOldSessionResult() {
        val gate = SessionRequestGate()
        val oldSession = gate.begin("old-session")
        val newSession = gate.begin("new-session")

        assertFalse(gate.isCurrent(oldSession, "new-session"))
        assertTrue(gate.isCurrent(newSession, "new-session"))
    }

    @Test
    fun teardownInvalidatesInFlightResult() {
        val gate = SessionRequestGate()
        val request = gate.begin("owner-session")

        gate.invalidate()

        assertFalse(gate.isCurrent(request, null))
    }
}
