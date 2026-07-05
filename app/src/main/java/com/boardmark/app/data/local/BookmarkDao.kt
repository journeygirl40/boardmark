package com.boardmark.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.boardmark.app.domain.model.FetchStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert
    suspend fun insert(entity: BookmarkEntity): Long

    @Update
    suspend fun update(entity: BookmarkEntity)

    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun observeAllRaw(): Flow<List<BookmarkEntity>>

    @Query(
        "SELECT * FROM bookmarks WHERE folderId = :folderId AND " +
            "(title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') ORDER BY addedAt DESC"
    )
    fun observeByFolder(folderId: Long, query: String): Flow<List<BookmarkEntity>>

    @Query("UPDATE bookmarks SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<Long>, folderId: Long?)

    @Query("UPDATE bookmarks SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: Long)

    @Query("DELETE FROM bookmarks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE originalUrl = :originalUrl LIMIT 1")
    suspend fun findByOriginalUrl(originalUrl: String): BookmarkEntity?

    @Query("UPDATE bookmarks SET addedAt = :addedAt WHERE id = :id")
    suspend fun updateAddedAt(id: Long, addedAt: Instant)

    @Query("UPDATE bookmarks SET ogImageUrl = :imageUrl, fetchStatus = :status WHERE id = :id")
    suspend fun updateThumbnail(id: Long, imageUrl: String, status: FetchStatus)

    @Query(
        "UPDATE bookmarks SET fetchStatus = :status, url = COALESCE(:finalUrl, url), " +
            "title = COALESCE(:title, title), ogImageUrl = :ogImageUrl, faviconUrl = :faviconUrl, " +
            "description = :description, siteName = :siteName WHERE id = :id"
    )
    suspend fun updateFetchResult(
        id: Long,
        status: FetchStatus,
        finalUrl: String?,
        title: String?,
        ogImageUrl: String?,
        faviconUrl: String?,
        description: String?,
        siteName: String?,
    )
}
