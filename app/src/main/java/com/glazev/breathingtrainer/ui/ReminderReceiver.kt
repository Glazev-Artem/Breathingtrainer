package com.glazev.breathingtrainer.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.glazev.breathingtrainer.MainActivity
import com.glazev.breathingtrainer.R
import kotlin.random.Random

class ReminderReceiver : BroadcastReceiver() {
    
    private val messages = listOf(
        "Воздух бесплатный, а польза бесценна. Подышим?",
        "Твой мозг просит кислорода. Не отказывай ему!",
        "Сделай глубокий вдох... и выдох. Чувствуешь? Это спокойствие.",
        "Даже супергерои делают паузу на дыхание. Твоя очередь!",
        "Минута дыхания — час продуктивности. Погнали?",
        "Твой 'огонёк' 🔥 скучает без тебя. Давай сохраним серию!",
        "Дыши глубже, живи дольше. Всего пару минут!",
        "Спокойствие начинается с одного вдоха. Начнем?",
        "Энергия на нуле? Глубокое дыхание — твоя зарядка!",
        "Просто подыши. Всё остальное подождет."
    )

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "breathing_trainer_reminder_v2" // Updated ID to ensure sound change takes effect
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.push_sound}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Напоминания о тренировках",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val randomMessage = messages[Random.nextInt(messages.size)]

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Время подышать!")
            .setContentText(randomMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
