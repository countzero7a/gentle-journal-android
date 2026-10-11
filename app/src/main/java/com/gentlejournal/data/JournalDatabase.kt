package com.gentlejournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gentlejournal.R
import com.gentlejournal.data.dao.EngagementDao
import com.gentlejournal.data.dao.EntryDao
import com.gentlejournal.data.dao.InsightDao
import com.gentlejournal.data.dao.TagDao
import com.gentlejournal.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EntryEntity::class,
        EmotionEntity::class,
        ActionEntity::class,
        EntryEmotionCrossRef::class,
        EntryActionCrossRef::class,
        InsightEntity::class,
        InsightEngagementEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun tagDao(): TagDao
    abstract fun insightDao(): InsightDao
    abstract fun engagementDao(): EngagementDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        fun getInstance(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): JournalDatabase {
            // Load SQLCipher native library
            System.loadLibrary("sqlcipher")

            return Room.databaseBuilder(
                context.applicationContext,
                JournalDatabase::class.java,
                "gentle_journal.db"
            )
                .openHelperFactory(DatabaseKeyManager.getFactory(context))
                .addCallback(SeedCallback(context.applicationContext))
                // Only pre-release schemas (v1, v2) may be wiped. From v3 on, every
                // version bump needs a Migration passed to addMigrations(), and the
                // exported schema in app/schemas/ must be committed alongside it.
                .fallbackToDestructiveMigrationFrom(1, 2)
                .build()
        }
    }

    /**
     * Seeds default emotion and action tags.
     * Runs on first creation AND after destructive migration.
     * Also checks on every open in case seeding failed previously.
     */
    private class SeedCallback(private val appContext: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            triggerSeed()
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            triggerSeed()
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Safety net: if emotions table is empty, seed again
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val count = database.tagDao().getEmotionCount()
                    if (count == 0) {
                        seedDefaultTags(database.tagDao(), appContext)
                    }
                }
            }
        }

        private fun triggerSeed() {
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedDefaultTags(database.tagDao(), appContext)
                }
            }
        }

        private suspend fun seedDefaultTags(tagDao: TagDao, ctx: Context) {
            val r = ctx.resources
            val defaultEmotions = listOf(
                // Top-level emotions
                EmotionEntity(id = "e_happy",    label = r.getString(R.string.emotion_happy),    emoji = "\uD83D\uDE0A", isDefault = true, sortOrder = 0),
                EmotionEntity(id = "e_calm",     label = r.getString(R.string.emotion_calm),     emoji = "\uD83D\uDE0C", isDefault = true, sortOrder = 1),
                EmotionEntity(id = "e_anxious",  label = r.getString(R.string.emotion_anxious),  emoji = "\uD83D\uDE30", isDefault = true, sortOrder = 2),
                EmotionEntity(id = "e_sad",      label = r.getString(R.string.emotion_sad),      emoji = "\uD83D\uDE22", isDefault = true, sortOrder = 3),
                EmotionEntity(id = "e_angry",    label = r.getString(R.string.emotion_angry),    emoji = "\uD83D\uDE24", isDefault = true, sortOrder = 4),
                EmotionEntity(id = "e_tired",    label = r.getString(R.string.emotion_tired),    emoji = "\uD83D\uDE34", isDefault = true, sortOrder = 5),
                EmotionEntity(id = "e_grateful", label = r.getString(R.string.emotion_grateful), emoji = "\uD83D\uDE4F", isDefault = true, sortOrder = 6),
                EmotionEntity(id = "e_hopeful",  label = r.getString(R.string.emotion_hopeful),  emoji = "\uD83C\uDF31", isDefault = true, sortOrder = 7),
                EmotionEntity(id = "e_confused", label = r.getString(R.string.emotion_confused), emoji = "\uD83D\uDE15", isDefault = true, sortOrder = 8),
                EmotionEntity(id = "e_content",  label = r.getString(R.string.emotion_content),  emoji = "\u263A\uFE0F", isDefault = true, sortOrder = 9),
                // Sub-emotions (v1.2 emotion wheel depth)
                EmotionEntity(id = "e_excited",     label = r.getString(R.string.emotion_excited),     emoji = "\uD83E\uDD29", isDefault = true, sortOrder = 10, parentId = "e_happy"),
                EmotionEntity(id = "e_proud",       label = r.getString(R.string.emotion_proud),       emoji = "\uD83D\uDE0E", isDefault = true, sortOrder = 11, parentId = "e_happy"),
                EmotionEntity(id = "e_peaceful",    label = r.getString(R.string.emotion_peaceful),    emoji = "\uD83E\uDDD8", isDefault = true, sortOrder = 12, parentId = "e_calm"),
                EmotionEntity(id = "e_relaxed",     label = r.getString(R.string.emotion_relaxed),     emoji = "\uD83D\uDE0C", isDefault = true, sortOrder = 13, parentId = "e_calm"),
                EmotionEntity(id = "e_overwhelmed", label = r.getString(R.string.emotion_overwhelmed), emoji = "\uD83D\uDE35", isDefault = true, sortOrder = 14, parentId = "e_anxious"),
                EmotionEntity(id = "e_nervous",     label = r.getString(R.string.emotion_nervous),     emoji = "\uD83D\uDE2C", isDefault = true, sortOrder = 15, parentId = "e_anxious"),
                EmotionEntity(id = "e_lonely",      label = r.getString(R.string.emotion_lonely),      emoji = "\uD83E\uDEE5", isDefault = true, sortOrder = 16, parentId = "e_sad"),
                EmotionEntity(id = "e_melancholy",  label = r.getString(R.string.emotion_melancholy),  emoji = "\uD83C\uDF27\uFE0F", isDefault = true, sortOrder = 17, parentId = "e_sad"),
                EmotionEntity(id = "e_frustrated",  label = r.getString(R.string.emotion_frustrated),  emoji = "\uD83D\uDE20", isDefault = true, sortOrder = 18, parentId = "e_angry"),
                EmotionEntity(id = "e_irritated",   label = r.getString(R.string.emotion_irritated),   emoji = "\uD83D\uDE12", isDefault = true, sortOrder = 19, parentId = "e_angry"),
            )

            val defaultActions = listOf(
                ActionEntity(id = "a_exercise",  label = r.getString(R.string.action_exercise),  emoji = "\uD83C\uDFC3", isDefault = true, sortOrder = 0),
                ActionEntity(id = "a_social",    label = r.getString(R.string.action_social),    emoji = "\uD83D\uDC65", isDefault = true, sortOrder = 1),
                ActionEntity(id = "a_worked",    label = r.getString(R.string.action_worked),    emoji = "\uD83D\uDCBC", isDefault = true, sortOrder = 2),
                ActionEntity(id = "a_rested",    label = r.getString(R.string.action_rested),    emoji = "\uD83D\uDECB\uFE0F", isDefault = true, sortOrder = 3),
                ActionEntity(id = "a_nature",    label = r.getString(R.string.action_nature),    emoji = "\uD83C\uDF3F", isDefault = true, sortOrder = 4),
                ActionEntity(id = "a_creative",  label = r.getString(R.string.action_creative),  emoji = "\uD83C\uDFA8", isDefault = true, sortOrder = 5),
                ActionEntity(id = "a_read",      label = r.getString(R.string.action_read),      emoji = "\uD83D\uDCD6", isDefault = true, sortOrder = 6),
                ActionEntity(id = "a_cooked",    label = r.getString(R.string.action_cooked),    emoji = "\uD83C\uDF73", isDefault = true, sortOrder = 7),
                ActionEntity(id = "a_nothing",   label = r.getString(R.string.action_nothing),   emoji = "\uD83D\uDCA4", isDefault = true, sortOrder = 8),
                ActionEntity(id = "a_meditated", label = r.getString(R.string.action_meditated), emoji = "\uD83E\uDDD8", isDefault = true, sortOrder = 9),
            )

            tagDao.insertEmotions(defaultEmotions)
            tagDao.insertActions(defaultActions)
        }
    }
}
