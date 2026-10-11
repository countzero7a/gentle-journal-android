package com.gentlejournal.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "entry_emotions",
    primaryKeys = ["entry_id", "emotion_id"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EmotionEntity::class,
            parentColumns = ["id"],
            childColumns = ["emotion_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("entry_id"),
        Index("emotion_id")
    ]
)
data class EntryEmotionCrossRef(
    @ColumnInfo(name = "entry_id")
    val entryId: String,

    @ColumnInfo(name = "emotion_id")
    val emotionId: String
)
