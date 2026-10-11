package com.gentlejournal.data.dao

import androidx.room.*
import com.gentlejournal.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    // ── Entries ──────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun getAllEntriesWithTags(): Flow<List<EntryWithTags>>

    @Transaction
    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    suspend fun getAllEntriesWithTagsOnce(): List<EntryWithTags>

    @Query("SELECT * FROM entries")
    suspend fun getAllEntriesOnce(): List<EntryEntity>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :entryId")
    suspend fun getEntryWithTags(entryId: String): EntryWithTags?

    @Transaction
    @Query("SELECT * FROM entries WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEntriesSince(since: Long): List<EntryWithTags>

    @Query("SELECT COUNT(*) FROM entries")
    fun getEntryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity)

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()

    // ── Entry-Emotion relations ─────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryEmotion(crossRef: EntryEmotionCrossRef)

    @Query("DELETE FROM entry_emotions WHERE entry_id = :entryId")
    suspend fun deleteEmotionsForEntry(entryId: String)

    // ── Entry-Action relations ──────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryAction(crossRef: EntryActionCrossRef)

    @Query("DELETE FROM entry_actions WHERE entry_id = :entryId")
    suspend fun deleteActionsForEntry(entryId: String)
}
