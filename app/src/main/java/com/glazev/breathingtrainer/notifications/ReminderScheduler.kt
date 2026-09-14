package com.glazev.breathingtrainer.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import org.json.JSONObject

object ReminderScheduler {
    const val ACTION_SHOW_REMINDER = "com.glazev.breathingtrainer.action.SHOW_REMINDER"
    const val EXTRA_KIND = "reminder_kind"
    const val EXTRA_DATE = "reminder_date"
    const val KIND_DAILY = "daily"
    const val KIND_DATE = "date"
    const val PREFS_NAME = "app_prefs"
    private const val DAILY_REQUEST_CODE = 10_001

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val triggerAt = ReminderTimeCalculator.nextDaily(hour, minute).toInstant().toEpochMilli()
        schedule(context, triggerAt, dailyPendingIntent(context))
    }

    fun scheduleDate(context: Context, date: String, hour: Int, minute: Int): Boolean {
        val triggerAt = runCatching {
            ReminderTimeCalculator.oneTime(date, hour, minute)?.toInstant()?.toEpochMilli()
        }.getOrNull() ?: return false
        schedule(context, triggerAt, datePendingIntent(context, date))
        return true
    }

    fun cancelDaily(context: Context) {
        alarmManager(context).cancel(dailyPendingIntent(context))
    }

    fun cancelDate(context: Context, date: String) {
        alarmManager(context).cancel(datePendingIntent(context, date))
    }

    fun restoreAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString("reminder_time", null)?.parseTime()?.let { (hour, minute) ->
            scheduleDaily(context, hour, minute)
        }

        val reminders = parseDayReminders(prefs.getString("day_reminders", "{}") ?: "{}")
        val validReminders = reminders.filter { (date, time) ->
            time.parseTime()?.let { (hour, minute) -> scheduleDate(context, date, hour, minute) } == true
        }
        if (validReminders.size != reminders.size) saveDayReminders(context, validReminders)
    }

    fun consumeDateReminder(context: Context, date: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminders = parseDayReminders(prefs.getString("day_reminders", "{}") ?: "{}")
            .toMutableMap()
            .apply { remove(date) }
        saveDayReminders(context, reminders)
    }

    private fun schedule(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        alarmManager(context).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun dailyPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_KIND, KIND_DAILY)
        }
        return PendingIntent.getBroadcast(
            context,
            DAILY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun datePendingIntent(context: Context, date: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_KIND, KIND_DATE)
            putExtra(EXTRA_DATE, date)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderTimeCalculator.requestCodeForDate(date),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun String.parseTime(): Pair<Int, Int>? {
        val parts = split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return (hour to minute).takeIf { hour in 0..23 && minute in 0..59 }
    }

    private fun parseDayReminders(json: String): Map<String, String> = runCatching {
        val objectValue = JSONObject(json)
        buildMap {
            objectValue.keys().forEach { date -> put(date, objectValue.getString(date)) }
        }
    }.getOrDefault(emptyMap())

    private fun saveDayReminders(context: Context, reminders: Map<String, String>) {
        val json = JSONObject().apply { reminders.forEach { (date, time) -> put(date, time) } }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString("day_reminders", json.toString())
        }
    }
}
