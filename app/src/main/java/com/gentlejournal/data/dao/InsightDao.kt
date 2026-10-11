package com.gentlejournal.data.dao

import androidx.room.*
import com.gentlejournal.data.entity.InsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {

    @Query("SELECT * FROM insights ORDER BY generated_at DESC")
    fun getAllInsights(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE period_days = :periodDays ORDER BY generated_at DESC LIMIT 20")
    fun getInsightsForPeriod(periodDays: Int): Flow<List<InsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<InsightEntity>)

    @Query("DELETE FROM insights WHERE period_days = :periodDays AND generated_at >= :since")
    suspend fun deleteInsightsSince(periodDays: Int, since: Long)

    @Query("DELETE FROM insights WHERE period_days = :periodDays AND generated_at < :olderThan")
    suspend fun deleteOldInsights(periodDays: Int, olderThan: Long)

    @Query("DELETE FROM insights")
    suspend fun deleteAllInsights()
}
