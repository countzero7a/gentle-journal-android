package com.gentlejournal.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "entry_actions",
    primaryKeys = ["entry_id", "action_id"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["action_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("entry_id"),
        Index("action_id")
    ]
)
data class EntryActionCrossRef(
    @ColumnInfo(name = "entry_id")
    val entryId: String,

    @ColumnInfo(name = "action_id")
    val actionId: String
)
