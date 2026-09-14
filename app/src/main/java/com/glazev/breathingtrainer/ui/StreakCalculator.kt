package com.glazev.breathingtrainer.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal object StreakCalculator {
    fun calculate(
        trainingTimestamps: Iterable<Long>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val trainingDates = trainingTimestamps
            .asSequence()
            .filter { it in 1..nowMillis }
            .map { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
            .filterNot { it.isAfter(today) }
            .toSet()

        if (trainingDates.isEmpty()) return 0
        val latest = trainingDates.maxOrNull() ?: return 0
        if (latest.isBefore(today.minusDays(1))) return 0

        var streak = 0
        var expectedDate: LocalDate = latest
        while (expectedDate in trainingDates) {
            streak++
            expectedDate = expectedDate.minusDays(1)
        }
        return streak
    }
}
