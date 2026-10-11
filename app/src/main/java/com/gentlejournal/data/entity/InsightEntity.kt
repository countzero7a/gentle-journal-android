package com.gentlejournal.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "generated_at")
    val generatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "period_days")
    val periodDays: Int = 7,

    @ColumnInfo(name = "period_label")
    val periodLabel: String = "week"
)
