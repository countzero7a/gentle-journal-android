package com.gentlejournal.data.dao

import androidx.room.*
import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // ── Emotions ────────────────────────────────────────────────────

    @Query("SELECT * FROM emotions ORDER BY sort_order ASC, label ASC")
    fun getAllEmotions(): Flow<List<EmotionEntity>>

    @Query("SELECT * FROM emotions ORDER BY sort_order ASC, label ASC")
    suspend fun getEmotionsOnce(): List<EmotionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmotion(emotion: EmotionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmotions(emotions: List<EmotionEntity>)

    @Update
    suspend fun updateEmotion(emotion: EmotionEntity)

    @Delete
    suspend fun deleteEmotion(emotion: EmotionEntity)

    @Query("SELECT COUNT(*) FROM emotions")
    suspend fun getEmotionCount(): Int

    // ── Actions ─────────────────────────────────────────────────────

    @Query("SELECT * FROM actions ORDER BY sort_order ASC, label ASC")
    fun getAllActions(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions ORDER BY sort_order ASC, label ASC")
    suspend fun getActionsOnce(): List<ActionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAction(action: ActionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActions(actions: List<ActionEntity>)

    @Update
    suspend fun updateAction(action: ActionEntity)

    @Delete
    suspend fun deleteAction(action: ActionEntity)

    @Query("SELECT COUNT(*) FROM actions")
    suspend fun getActionCount(): Int
}
