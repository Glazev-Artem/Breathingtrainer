package com.glazev.breathingtrainer.ui

import android.os.SystemClock

/**
 * Tracks one logical training session using a monotonic clock.
 * Finishing is intentionally idempotent: only the first call returns a duration.
 */
internal class TrainingSessionClock(
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime
) {
    private var active = false
    private var runningSinceMillis: Long? = null
    private var accumulatedMillis = 0L

    val isActive: Boolean
        get() = active

    val isPaused: Boolean
        get() = active && runningSinceMillis == null

    fun begin(): Boolean {
        if (active) return false
        active = true
        accumulatedMillis = 0L
        runningSinceMillis = nowMillis()
        return true
    }

    fun pause(): Boolean {
        val runningSince = runningSinceMillis ?: return false
        if (!active) return false
        accumulatedMillis += (nowMillis() - runningSince).coerceAtLeast(0L)
        runningSinceMillis = null
        return true
    }

    fun resume(): Boolean {
        if (!active || runningSinceMillis != null) return false
        runningSinceMillis = nowMillis()
        return true
    }

    fun finishSeconds(): Int? {
        if (!active) return null
        runningSinceMillis?.let { startedAt ->
            accumulatedMillis += (nowMillis() - startedAt).coerceAtLeast(0L)
        }
        val seconds = (accumulatedMillis / 1_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        reset()
        return seconds
    }

    private fun reset() {
        active = false
        runningSinceMillis = null
        accumulatedMillis = 0L
    }
}
