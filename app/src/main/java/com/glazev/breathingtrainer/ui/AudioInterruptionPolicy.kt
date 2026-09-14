package com.glazev.breathingtrainer.ui

/** Keeps automatic resume limited to a single transient system interruption. */
internal class AudioInterruptionPolicy {
    private var resumeOnGain = false

    fun onLoss(isTransient: Boolean, isRunning: Boolean): Boolean {
        resumeOnGain = isTransient && isRunning
        return isRunning
    }

    fun onGain(isPaused: Boolean): Boolean {
        val shouldResume = resumeOnGain && isPaused
        resumeOnGain = false
        return shouldResume
    }

    fun clear() {
        resumeOnGain = false
    }
}
