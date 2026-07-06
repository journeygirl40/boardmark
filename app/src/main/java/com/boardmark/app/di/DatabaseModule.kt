package com.boardmark.app.di

import android.content.Context
import androidx.room.Room
import com.boardmark.app.data.local.AppDatabase
import com.boardmark.app.data.local.BookmarkDao
import com.boardmark.app.data.local.FolderDao
import com.boardmark.app.data.local.LabelDao
import com.boardmark.app.data.local.MIGRATION_1_2
import com.boardmark.app.data.local.MIGRATION_2_3
import com.boardmark.app.data.local.MIGRATION_3_4
import com.boardmark.app.data.local.MIGRATION_4_5
import com.boardmark.app.data.local.MIGRATION_5_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "boardmark.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideLabelDao(database: AppDatabase): LabelDao = database.labelDao()
}
