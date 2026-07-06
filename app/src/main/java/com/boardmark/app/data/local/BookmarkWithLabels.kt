package com.boardmark.app.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class BookmarkWithLabels(
    @Embedded val bookmark: BookmarkEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookmarkLabelCrossRef::class,
            parentColumn = "bookmarkId",
            entityColumn = "labelId",
        ),
    )
    val labels: List<LabelEntity>,
)
