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
import com.gentlejournal.ui.QuickLog
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
        val preferences = ThemePreferences(context.applicationContext)
        val language = preferences.selectedLanguage.first()
        val localized = LocaleHelper.localizedContext(context, language)
        val views = RemoteViews(context.packageName, R.layout.widget_mood)

        // Tap widget -> open app (matching MainActivity's launcher filter)
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClass(context, MainActivity::class.java)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)
        views.setTextViewText(R.id.widget_app_name, localized.getString(R.string.app_name))

        // Each emoji opens a new entry with that feeling chosen (after unlocking, if locked)
        val db = JournalDatabase.getInstance(context)
        val emotions = db.tagDao().getEmotionsOnce().associateBy { it.id }
        QuickLog.WIDGET_EMOTIONS.forEachIndexed { index, emotionId ->
            val emotion = emotions[emotionId] ?: return@forEachIndexed
            val quickLog = Intent(Intent.ACTION_MAIN).apply {
                setClass(context, MainActivity::class.java)
                addCategory(Intent.CATEGORY_LAUNCHER)
                // Delivered to the running app through onNewIntent instead of being dropped
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(QuickLog.EXTRA_EMOTION_ID, emotionId)
            }
            val button = QUICK_LOG_BUTTONS[index]
            views.setTextViewText(button, emotion.emoji)
            views.setContentDescription(button, resolveEmotionLabel(localized, emotion.id, emotion.label))
            views.setOnClickPendingIntent(
                button,
                PendingIntent.getActivity(
                    context, QUICK_LOG_REQUEST_CODE + index, quickLog,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        // With the app lock on, the home screen doesn't reveal how you feel
        if (preferences.appLockEnabled.first()) {
            views.setTextViewText(R.id.widget_emoji, "\uD83D\uDD12")
            views.setTextViewText(R.id.widget_label, localized.getString(R.string.widget_locked))
            return views
        }

        // Today's most frequent emotion, by its label in the app language
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val today = db.entryDao().getEntriesSince(todayStart)
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
            views.setTextViewText(R.id.widget_label, localized.getString(R.string.widget_how_feeling))
        }
        return views
    }

    companion object {
        private const val QUICK_LOG_REQUEST_CODE = 100
        private val QUICK_LOG_BUTTONS = intArrayOf(
            R.id.widget_quick_1, R.id.widget_quick_2, R.id.widget_quick_3, R.id.widget_quick_4, R.id.widget_quick_5
        )

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
