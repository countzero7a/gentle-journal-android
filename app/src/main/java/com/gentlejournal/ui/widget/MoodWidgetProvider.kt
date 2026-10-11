package com.gentlejournal.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gentlejournal.MainActivity
import com.gentlejournal.R
import com.gentlejournal.data.JournalDatabase
import com.gentlejournal.data.ThemePreferences
import com.gentlejournal.ui.util.LocaleHelper
import com.gentlejournal.ui.util.resolveEmotionLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MoodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Reading the database takes a moment; goAsync keeps the receiver alive until it's done
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildViews(context)
                ids.forEach { id -> manager.updateAppWidget(id, views) }
            } catch (_: Exception) {
                // Keep whatever the widget showed before
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun buildViews(context: Context): RemoteViews {
        val language = ThemePreferences(context.applicationContext).selectedLanguage.first()
        val localized = LocaleHelper.localizedContext(context, language)
        val views = RemoteViews(context.packageName, R.layout.widget_mood)

        // Tap widget -> open app
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)
        views.setTextViewText(R.id.widget_app_name, localized.getString(R.string.app_name))

        // Today's most frequent emotion, by its label in the app language
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val today = JournalDatabase.getInstance(context).entryDao().getEntriesSince(todayStart)
        val counts = mutableMapOf<String, Pair<String, Int>>()
        today.forEach { e ->
            e.emotions.forEach { emo ->
                val label = resolveEmotionLabel(localized, emo.id, emo.label)
                counts[label] = Pair(emo.emoji, (counts[label]?.second ?: 0) + 1)
            }
        }

        val dominant = counts.maxByOrNull { it.value.second }
        if (dominant != null) {
            views.setTextViewText(R.id.widget_emoji, dominant.value.first)
            views.setTextViewText(R.id.widget_label, dominant.key)
        } else {
            views.setTextViewText(R.id.widget_emoji, "🌿")
            views.setTextViewText(R.id.widget_label, localized.getString(R.string.widget_no_entries))
        }
        return views
    }

    companion object {
        /** Refreshes every placed widget, so it doesn't wait for the 30-minute update after entries change. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MoodWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, MoodWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }
}
