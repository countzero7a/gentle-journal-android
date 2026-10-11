package com.gentlejournal.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gentlejournal.MainActivity
import com.gentlejournal.R
import com.gentlejournal.data.JournalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MoodWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidget(context, manager, id) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_mood)

        // Tap widget -> open app
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)

        // Set app name immediately
        views.setTextViewText(R.id.widget_app_name, context.getString(R.string.app_name))

        // Default state
        views.setTextViewText(R.id.widget_emoji, "🌿")
        views.setTextViewText(R.id.widget_label, context.getString(R.string.widget_no_entries))
        manager.updateAppWidget(widgetId, views)

        // Fetch today's mood asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = JournalDatabase.getInstance(context)
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val today = db.entryDao().getEntriesSince(todayStart)
                val counts = mutableMapOf<String, Pair<String, Int>>()
                today.forEach { e ->
                    e.emotions.forEach { emo ->
                        val c = counts[emo.label]
                        counts[emo.label] = Pair(emo.emoji, (c?.second ?: 0) + 1)
                    }
                }

                val dominant = counts.maxByOrNull { it.value.second }
                if (dominant != null) {
                    views.setTextViewText(R.id.widget_emoji, dominant.value.first)
                    views.setTextViewText(R.id.widget_label, dominant.key)
                }
                manager.updateAppWidget(widgetId, views)
            } catch (_: Exception) {
                // Keep defaults on error
            }
        }
    }
}
