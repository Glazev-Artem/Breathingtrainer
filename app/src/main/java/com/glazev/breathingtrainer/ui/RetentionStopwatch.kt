package com.glazev.breathingtrainer.ui

import android.os.SystemClock

/** Unbounded, monotonic stopwatch for the user-controlled Wim Hof retention phase. */
internal class RetentionStopwatch(
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime
) {
    private var previousTickMillis = nowMillis()
    private var elapsedMillis = 0L

    fun tick(isPaused: Boolean): Float {
        val now = nowMillis()
        if (!isPaused) {
            elapsedMillis += (now - previousTickMillis).coerceAtLeast(0L)
        }
        previousTickMillis = now
        return elapsedMillis / 1_000f
    }
}
