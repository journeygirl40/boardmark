package com.boardmark.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val defaultBrowserPackage: String? = null,
    // 元のパスワードは保存せず、SHA-256+ソルトのハッシュのみ保持する(FolderPasswordHasher)。
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
)
