package com.glazev.breathingtrainer.notifications

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTimeCalculatorTest {
    private val zone = ZoneId.of("Asia/Novosibirsk")

    @Test
    fun dailyReminderUsesTodayWhenTimeIsStillAhead() {
        val now = ZonedDateTime.of(2026, 9, 13, 10, 0, 0, 0, zone)
        assertEquals(13, ReminderTimeCalculator.nextDaily(11, 30, now).dayOfMonth)
    }

    @Test
    fun dailyReminderMovesToTomorrowWhenTimeHasPassed() {
        val now = ZonedDateTime.of(2026, 9, 13, 12, 0, 0, 0, zone)
        assertEquals(14, ReminderTimeCalculator.nextDaily(11, 30, now).dayOfMonth)
    }

    @Test
    fun pastCalendarReminderIsRejected() {
        val now = ZonedDateTime.of(2026, 9, 13, 12, 0, 0, 0, zone)
        assertNull(ReminderTimeCalculator.oneTime("2026-09-13", 11, 59, now))
    }

    @Test
    fun differentDatesHaveDifferentRequestCodes() {
        assertNotEquals(
            ReminderTimeCalculator.requestCodeForDate("2026-09-13"),
            ReminderTimeCalculator.requestCodeForDate("2026-09-14")
        )
    }
}
