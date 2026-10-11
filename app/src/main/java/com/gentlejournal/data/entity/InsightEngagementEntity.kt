package com.gentlejournal.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insight_engagement")
data class InsightEngagementEntity(
    @PrimaryKey
    @ColumnInfo(name = "insight_type")
    val insightType: String,

    @ColumnInfo(name = "weight")
    val weight: Float = 1.0f,

    @ColumnInfo(name = "interaction_count")
    val interactionCount: Int = 0,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
