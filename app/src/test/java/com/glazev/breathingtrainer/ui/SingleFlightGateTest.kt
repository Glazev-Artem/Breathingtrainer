package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleFlightGateTest {
    @Test
    fun duplicateStartIsRejectedUntilTerminalCallback() {
        val gate = SingleFlightGate()

        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())
        assertFalse(gate.tryStart())

        gate.finish()
        assertTrue(gate.tryStart())
    }
}
