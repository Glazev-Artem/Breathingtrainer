package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioInterruptionPolicyTest {
    @Test
    fun transientLossResumesPausedTrainingOnlyOnce() {
        val policy = AudioInterruptionPolicy()

        assertTrue(policy.onLoss(isTransient = true, isRunning = true))
        assertTrue(policy.onGain(isPaused = true))
        assertFalse(policy.onGain(isPaused = true))
    }

    @Test
    fun permanentLossDoesNotResumeTraining() {
        val policy = AudioInterruptionPolicy()

        assertTrue(policy.onLoss(isTransient = false, isRunning = true))
        assertFalse(policy.onGain(isPaused = true))
    }

    @Test
    fun manualPauseCancelsAutomaticResume() {
        val policy = AudioInterruptionPolicy()

        policy.onLoss(isTransient = true, isRunning = true)
        policy.clear()

        assertFalse(policy.onGain(isPaused = true))
    }

    @Test
    fun interruptionDoesNothingWhenTrainingIsNotRunning() {
        val policy = AudioInterruptionPolicy()

        assertFalse(policy.onLoss(isTransient = true, isRunning = false))
        assertFalse(policy.onGain(isPaused = false))
    }
}
