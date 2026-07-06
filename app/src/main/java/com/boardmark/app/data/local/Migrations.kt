package com.boardmark.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `folders` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `folderId` INTEGER DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `description` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `siteName` TEXT DEFAULT NULL")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `manualOrder` REAL NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `bookmarks` SET `manualOrder` = `addedAt`")
        db.execSQL("ALTER TABLE `folders` ADD COLUMN `defaultBrowserPackage` TEXT DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `viewCount` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `labels` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_labels_name` ON `labels` (`name`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bookmark_label_cross_ref` (" +
                "`bookmarkId` INTEGER NOT NULL, " +
                "`labelId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`bookmarkId`, `labelId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_bookmark_label_cross_ref_labelId` " +
                "ON `bookmark_label_cross_ref` (`labelId`)"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `duplicateIgnored` INTEGER NOT NULL DEFAULT 0")
    }
}
