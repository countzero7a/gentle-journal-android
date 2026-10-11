package com.gentlejournal.data.dao

import androidx.room.*
import com.gentlejournal.data.entity.InsightEngagementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EngagementDao {
    @Query("SELECT * FROM insight_engagement")
    fun getAllEngagement(): Flow<List<InsightEngagementEntity>>

    @Query("SELECT * FROM insight_engagement WHERE insight_type = :type")
    suspend fun getEngagement(type: String): InsightEngagementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEngagement(entity: InsightEngagementEntity)

    @Query("DELETE FROM insight_engagement")
    suspend fun deleteAll()
}
