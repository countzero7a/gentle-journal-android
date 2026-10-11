package com.gentlejournal.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class EntryWithTags(
    @Embedded
    val entry: EntryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryEmotionCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "emotion_id"
        )
    )
    val emotions: List<EmotionEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryActionCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "action_id"
        )
    )
    val actions: List<ActionEntity>
)
