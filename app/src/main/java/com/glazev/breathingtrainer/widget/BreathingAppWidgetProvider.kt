package com.glazev.breathingtrainer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.glazev.breathingtrainer.MainActivity
import com.glazev.breathingtrainer.R
import com.glazev.breathingtrainer.model.DefaultTechniques

class BreathingAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV_TECHNIQUE = "com.glazev.breathingtrainer.ACTION_PREV_TECHNIQUE"
        const val ACTION_NEXT_TECHNIQUE = "com.glazev.breathingtrainer.ACTION_NEXT_TECHNIQUE"
        const val ACTION_START_WIDGET_TECHNIQUE = "com.glazev.breathingtrainer.ACTION_START_WIDGET_TECHNIQUE"
        const val ACTION_START_SOS = "com.glazev.breathingtrainer.ACTION_START_SOS"

        const val EXTRA_TECHNIQUE_ID = "extra_technique_id"
        const val EXTRA_IS_SOS = "extra_is_sos"

        private const val PREFS_NAME = "breathing_widget_prefs"
        private const val PREF_KEY_INDEX = "widget_technique_index"

        fun getSelectedTechniqueIndex(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(PREF_KEY_INDEX, 0)
        }

        fun setSelectedTechniqueIndex(context: Context, index: Int) {
            val size = DefaultTechniques.list.size
            val normalizedIndex = if (size > 0) (index % size + size) % size else 0
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(PREF_KEY_INDEX, normalizedIndex)
                .apply()
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, BreathingAppWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_breathing_4x1)
            val index = getSelectedTechniqueIndex(context)
            val techniques = DefaultTechniques.list
            val selectedTechnique = if (index in techniques.indices) techniques[index] else DefaultTechniques.SquareBreathing

            // Отображаем название и параметры
            val displayTitle = "${selectedTechnique.name} (${selectedTechnique.description})"
            views.setTextViewText(R.id.widget_tv_technique_title, displayTitle)

            // Кнопка < Предыдущая техника
            val prevIntent = Intent(context, BreathingAppWidgetProvider::class.java).apply {
                action = ACTION_PREV_TECHNIQUE
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context, 101, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev_technique, prevPendingIntent)

            // Кнопка > Следующая техника
            val nextIntent = Intent(context, BreathingAppWidgetProvider::class.java).apply {
                action = ACTION_NEXT_TECHNIQUE
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, 102, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next_technique, nextPendingIntent)

            // Кнопка ЗАПУСК
            val startIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_START_WIDGET_TECHNIQUE
                putExtra(EXTRA_TECHNIQUE_ID, selectedTechnique.id)
                putExtra(EXTRA_IS_SOS, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val startPendingIntent = PendingIntent.getActivity(
                context, 201, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_start, startPendingIntent)

            // Кнопка 🆘 SOS (Запуск техники 4-7-8 БЕЗ РЕКЛАМЫ)
            val sosIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_START_SOS
                putExtra(EXTRA_TECHNIQUE_ID, "478")
                putExtra(EXTRA_IS_SOS, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val sosPendingIntent = PendingIntent.getActivity(
                context, 202, sosIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_sos, sosPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV_TECHNIQUE -> {
                val currentIndex = getSelectedTechniqueIndex(context)
                setSelectedTechniqueIndex(context, currentIndex - 1)
                updateAllWidgets(context)
            }
            ACTION_NEXT_TECHNIQUE -> {
                val currentIndex = getSelectedTechniqueIndex(context)
                setSelectedTechniqueIndex(context, currentIndex + 1)
                updateAllWidgets(context)
            }
        }
    }
}
