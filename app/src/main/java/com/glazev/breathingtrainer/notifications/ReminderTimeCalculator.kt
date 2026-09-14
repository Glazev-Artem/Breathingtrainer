package com.glazev.breathingtrainer.notifications

import java.time.LocalDate
import java.time.ZonedDateTime

internal object ReminderTimeCalculator {
    fun nextDaily(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        require(hour in 0..23 && minute in 0..59)
        val today = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    fun oneTime(
        date: String,
        hour: Int,
        minute: Int,
        now: ZonedDateTime = ZonedDateTime.now()
    ): ZonedDateTime? {
        require(hour in 0..23 && minute in 0..59)
        val scheduled = LocalDate.parse(date).atTime(hour, minute).atZone(now.zone)
        return scheduled.takeIf { it.isAfter(now) }
    }

    fun requestCodeForDate(date: String): Int =
        date.filter(Char::isDigit).toIntOrNull() ?: date.hashCode().and(Int.MAX_VALUE)
}
