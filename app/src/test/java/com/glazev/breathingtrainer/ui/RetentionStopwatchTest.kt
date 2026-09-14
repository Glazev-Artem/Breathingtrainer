package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RetentionStopwatchTest {
    private var now = 1_000L
    private val stopwatch = RetentionStopwatch { now }

    @Test
    fun startsAtZeroAndCountsForwardWithoutDurationLimit() {
        assertEquals(0f, stopwatch.tick(isPaused = false))
        now += 45_500L
        assertEquals(45.5f, stopwatch.tick(isPaused = false))
        now += 172_800_000L
        assertEquals(172_845.5f, stopwatch.tick(isPaused = false))
    }

    @Test
    fun pausedTimeIsNotCounted() {
        now += 5_000L
        assertEquals(0f, stopwatch.tick(isPaused = true))
        now += 2_500L
        assertEquals(2.5f, stopwatch.tick(isPaused = false))
    }

    @Test
    fun clockCorrectionCannotMoveStopwatchBackwards() {
        now += 4_000L
        assertEquals(4f, stopwatch.tick(isPaused = false))
        now -= 2_000L
        assertEquals(4f, stopwatch.tick(isPaused = false))
    }
}
