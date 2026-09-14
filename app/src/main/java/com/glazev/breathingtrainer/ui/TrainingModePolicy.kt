package com.glazev.breathingtrainer.ui

/** Central limits and defaults used by both the settings UI and a running session. */
internal object TrainingModePolicy {
    const val NORMAL_REPETITIONS = 10
    const val NORMAL_CYCLES = 1
    const val WIM_HOF_REPETITIONS = 30
    const val WIM_HOF_CYCLES = 3

    private const val MIN_REPETITIONS = 1
    private const val MAX_REPETITIONS = 999
    private const val MIN_CYCLES = 1
    private const val MAX_CYCLES = 10

    fun repetitions(value: Int): Int = value.coerceIn(MIN_REPETITIONS, MAX_REPETITIONS)

    fun cycles(value: Int): Int = value.coerceIn(MIN_CYCLES, MAX_CYCLES)

    fun stopwatchElapsedSeconds(value: Float): Int = when {
        !value.isFinite() || value <= 0f -> 0
        value >= Int.MAX_VALUE.toFloat() -> Int.MAX_VALUE
        else -> value.toInt()
    }
}
