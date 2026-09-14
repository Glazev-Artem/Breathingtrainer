package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StreakCalculatorTest {
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `calendar streak survives 23 hour daylight saving day`() {
        val beforeDst = millis(2026, 3, 7, 9)
        val dstDay = millis(2026, 3, 8, 9)
        val today = millis(2026, 3, 9, 9)

        assertEquals(3, StreakCalculator.calculate(listOf(beforeDst, dstDay, today), today, newYork))
    }

    @Test
    fun `several trainings on one day count once`() {
        val yesterdayMorning = millis(2026, 5, 3, 8)
        val yesterdayEvening = millis(2026, 5, 3, 20)
        val today = millis(2026, 5, 4, 10)

        assertEquals(
            2,
            StreakCalculator.calculate(listOf(yesterdayMorning, yesterdayEvening, today), today, newYork)
        )
    }

    @Test
    fun `streak resets after a missed calendar day`() {
        val oldTraining = millis(2026, 5, 1, 10)
        val today = millis(2026, 5, 3, 10)

        assertEquals(0, StreakCalculator.calculate(listOf(oldTraining), today, newYork))
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int): Long =
        LocalDateTime.of(year, month, day, hour, 0)
            .atZone(newYork)
            .toInstant()
            .toEpochMilli()
}
