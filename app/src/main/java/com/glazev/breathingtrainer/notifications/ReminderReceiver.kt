package com.glazev.breathingtrainer.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.glazev.breathingtrainer.MainActivity
import com.glazev.breathingtrainer.R
import android.media.AudioAttributes
import androidx.core.net.toUri

class ReminderReceiver : BroadcastReceiver() {
    private val messages = listOf(
        "Воздух бесплатный, а польза бесценна. Подышим?",
        "Твой мозг просит кислорода. Не отказывай ему!",
        "Сделай глубокий вдох и выдох. Чувствуешь спокойствие?",
        "Даже супергерои делают паузу на дыхание. Твоя очередь!",
        "Спокойствие начинается с одного вдоха. Начнём?"
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderScheduler.restoreAll(context)
            return
        }

        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND)
        if (kind == ReminderScheduler.KIND_DAILY) {
            val time = context.getSharedPreferences(ReminderScheduler.PREFS_NAME, Context.MODE_PRIVATE)
                .getString("reminder_time", null)
                ?.split(':')
            if (time?.size == 2) {
                val hour = time[0].toIntOrNull()
                val minute = time[1].toIntOrNull()
                if (hour != null && minute != null) ReminderScheduler.scheduleDaily(context, hour, minute)
            }
        } else if (kind == ReminderScheduler.KIND_DATE) {
            intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.let {
                ReminderScheduler.consumeDateReminder(context, it)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "breathing_trainer_reminder_v2"
        val soundUri = "android.resource://${context.packageName}/${R.raw.push_sound}".toUri()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        val channel = NotificationChannel(
            channelId,
            "Напоминания о тренировках",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { setSound(soundUri, audioAttributes) }
        notificationManager.createNotificationChannel(channel)

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.hashCode() ?: 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Время подышать!")
            .setContentText(messages.random())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.hashCode() ?: 1001
        notificationManager.notify(notificationId, notification)
    }
}
