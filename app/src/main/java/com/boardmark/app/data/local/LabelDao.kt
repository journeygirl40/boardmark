package com.boardmark.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Insert
    suspend fun insert(entity: LabelEntity): Long

    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun observeAll(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): LabelEntity?

    @Query("DELETE FROM bookmark_label_cross_ref WHERE bookmarkId = :bookmarkId")
    suspend fun clearLabelsForBookmark(bookmarkId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<BookmarkLabelCrossRef>)

    @Query("SELECT COUNT(*) FROM bookmark_label_cross_ref WHERE labelId = :labelId")
    suspend fun countBookmarksForLabel(labelId: Long): Int

    @Query("UPDATE labels SET name = :newName WHERE id = :id")
    suspend fun renameRaw(id: Long, newName: String)

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmark_label_cross_ref WHERE labelId = :labelId")
    suspend fun clearCrossRefsForLabel(labelId: Long)

    /**
     * labelIdを持つ紐付けをすべてtargetIdへ付け替える。同一ブックマークが両方の
     * ラベルを既に持っている場合は一意制約に反するためIGNOREで無視し(重複するだけなので
     * 実害はない)、その取りこぼし分はclearCrossRefsForLabelで別途消す。
     */
    @Query("UPDATE OR IGNORE bookmark_label_cross_ref SET labelId = :targetId WHERE labelId = :sourceId")
    suspend fun reassignCrossRefs(sourceId: Long, targetId: Long)
}
