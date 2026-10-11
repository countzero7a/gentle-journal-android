package com.gentlejournal.data.repository

import androidx.room.withTransaction
import com.gentlejournal.data.JournalDatabase
import com.gentlejournal.data.entity.*
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val db: JournalDatabase) {

    private val entryDao = db.entryDao()
    private val tagDao = db.tagDao()

    // ── Entries ──────────────────────────────────────────────────────

    val allEntries: Flow<List<EntryWithTags>> = entryDao.getAllEntriesWithTags()
    val entryCount: Flow<Int> = entryDao.getEntryCount()

    suspend fun getEntry(id: String): EntryWithTags? = entryDao.getEntryWithTags(id)

    suspend fun getEntriesSince(timestampMillis: Long): List<EntryWithTags> =
        entryDao.getEntriesSince(timestampMillis)

    suspend fun saveEntry(
        entry: EntryEntity,
        emotionIds: List<String>,
        actionIds: List<String>
    ) = db.withTransaction {
        entryDao.insertEntry(entry)
        replaceTags(entry.id, emotionIds, actionIds)
    }

    suspend fun updateEntry(
        entry: EntryEntity,
        emotionIds: List<String>,
        actionIds: List<String>
    ) = db.withTransaction {
        entryDao.updateEntry(entry)
        replaceTags(entry.id, emotionIds, actionIds)
    }

    private suspend fun replaceTags(entryId: String, emotionIds: List<String>, actionIds: List<String>) {
        entryDao.deleteEmotionsForEntry(entryId)
        emotionIds.forEach { eid ->
            entryDao.insertEntryEmotion(EntryEmotionCrossRef(entryId, eid))
        }
        entryDao.deleteActionsForEntry(entryId)
        actionIds.forEach { aid ->
            entryDao.insertEntryAction(EntryActionCrossRef(entryId, aid))
        }
    }

    suspend fun deleteEntry(id: String) = entryDao.deleteEntry(id)

    /** Deletes every entry and the insights derived from them. Tags are kept. */
    suspend fun deleteAllEntries() = db.withTransaction {
        entryDao.deleteAllEntries()
        db.insightDao().deleteAllInsights()
        db.engagementDao().deleteAll()
    }

    // ── Tags ────────────────────────────────────────────────────────

    val allEmotions: Flow<List<EmotionEntity>> = tagDao.getAllEmotions()
    val allActions: Flow<List<ActionEntity>> = tagDao.getAllActions()

    suspend fun addEmotion(emotion: EmotionEntity) = tagDao.insertEmotion(emotion)
    suspend fun addAction(action: ActionEntity) = tagDao.insertAction(action)

    suspend fun removeEmotion(emotion: EmotionEntity) = tagDao.deleteEmotion(emotion)
    suspend fun removeAction(action: ActionEntity) = tagDao.deleteAction(action)
}
