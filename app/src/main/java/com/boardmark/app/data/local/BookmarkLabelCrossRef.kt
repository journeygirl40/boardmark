package com.boardmark.app.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "bookmark_label_cross_ref",
    primaryKeys = ["bookmarkId", "labelId"],
    indices = [Index(value = ["labelId"])],
)
data class BookmarkLabelCrossRef(
    val bookmarkId: Long,
    val labelId: Long,
)
