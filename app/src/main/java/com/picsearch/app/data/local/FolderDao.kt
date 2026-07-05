package com.picsearch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert
    suspend fun insert(entity: FolderEntity): Long

    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FolderEntity?

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)
}
