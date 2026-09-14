package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingModePolicyTest {
    @Test
    fun repetitionsAreAlwaysFiniteAndPositive() {
        assertEquals(1, TrainingModePolicy.repetitions(Int.MIN_VALUE))
        assertEquals(1, TrainingModePolicy.repetitions(0))
        assertEquals(30, TrainingModePolicy.repetitions(30))
        assertEquals(999, TrainingModePolicy.repetitions(Int.MAX_VALUE))
    }

    @Test
    fun cyclesStayInsideSupportedWimHofRange() {
        assertEquals(1, TrainingModePolicy.cycles(-1))
        assertEquals(1, TrainingModePolicy.cycles(0))
        assertEquals(3, TrainingModePolicy.cycles(3))
        assertEquals(10, TrainingModePolicy.cycles(11))
    }

    @Test
    fun modeDefaultsRemainExplicitAndIndependent() {
        assertEquals(10, TrainingModePolicy.NORMAL_REPETITIONS)
        assertEquals(1, TrainingModePolicy.NORMAL_CYCLES)
        assertEquals(30, TrainingModePolicy.WIM_HOF_REPETITIONS)
        assertEquals(3, TrainingModePolicy.WIM_HOF_CYCLES)
    }

    @Test
    fun stopwatchResultIsSafeButHasNoConfiguredTimeLimit() {
        assertEquals(0, TrainingModePolicy.stopwatchElapsedSeconds(-1f))
        assertEquals(0, TrainingModePolicy.stopwatchElapsedSeconds(Float.NaN))
        assertEquals(45, TrainingModePolicy.stopwatchElapsedSeconds(45.9f))
        assertEquals(172_800, TrainingModePolicy.stopwatchElapsedSeconds(172_800f))
    }
}
