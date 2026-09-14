package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingSessionClockTest {
    private var now = 0L
    private val clock = TrainingSessionClock { now }

    @Test
    fun finishWithoutBeginDoesNotCreateSession() {
        assertNull(clock.finishSeconds())
    }

    @Test
    fun finishIsIdempotent() {
        assertTrue(clock.begin())
        now = 12_500L

        assertEquals(12, clock.finishSeconds())
        assertNull(clock.finishSeconds())
        assertFalse(clock.isActive)
    }

    @Test
    fun pausedTimeIsExcludedFromDuration() {
        assertTrue(clock.begin())
        now = 4_000L
        assertTrue(clock.pause())
        assertTrue(clock.isPaused)

        now = 64_000L
        assertTrue(clock.resume())
        now = 70_500L

        assertEquals(10, clock.finishSeconds())
    }

    @Test
    fun activeSessionCannotBeReplacedBySecondStart() {
        assertTrue(clock.begin())
        now = 5_000L

        assertFalse(clock.begin())
        assertEquals(5, clock.finishSeconds())
    }
}
