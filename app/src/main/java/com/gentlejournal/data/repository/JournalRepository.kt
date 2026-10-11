package com.gentlejournal.data.repository

import com.gentlejournal.data.dao.EntryDao
import com.gentlejournal.data.dao.TagDao
import com.gentlejournal.data.entity.*
import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val entryDao: EntryDao,
    private val tagDao: TagDao
) {
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
    ) {
        entryDao.insertEntry(entry)
        // Replace emotion relations
        entryDao.deleteEmotionsForEntry(entry.id)
        emotionIds.forEach { eid ->
            entryDao.insertEntryEmotion(EntryEmotionCrossRef(entry.id, eid))
        }
        // Replace action relations
        entryDao.deleteActionsForEntry(entry.id)
        actionIds.forEach { aid ->
            entryDao.insertEntryAction(EntryActionCrossRef(entry.id, aid))
        }
    }

    suspend fun updateEntry(
        entry: EntryEntity,
        emotionIds: List<String>,
        actionIds: List<String>
    ) {
        entryDao.updateEntry(entry)
        entryDao.deleteEmotionsForEntry(entry.id)
        emotionIds.forEach { eid ->
            entryDao.insertEntryEmotion(EntryEmotionCrossRef(entry.id, eid))
        }
        entryDao.deleteActionsForEntry(entry.id)
        actionIds.forEach { aid ->
            entryDao.insertEntryAction(EntryActionCrossRef(entry.id, aid))
        }
    }

    suspend fun deleteEntry(id: String) = entryDao.deleteEntry(id)

    suspend fun deleteAllEntries() = entryDao.deleteAllEntries()

    // ── Tags ────────────────────────────────────────────────────────

    val allEmotions: Flow<List<EmotionEntity>> = tagDao.getAllEmotions()
    val allActions: Flow<List<ActionEntity>> = tagDao.getAllActions()

    suspend fun addEmotion(emotion: EmotionEntity) = tagDao.insertEmotion(emotion)
    suspend fun addAction(action: ActionEntity) = tagDao.insertAction(action)

    suspend fun removeEmotion(emotion: EmotionEntity) = tagDao.deleteEmotion(emotion)
    suspend fun removeAction(action: ActionEntity) = tagDao.deleteAction(action)
}
