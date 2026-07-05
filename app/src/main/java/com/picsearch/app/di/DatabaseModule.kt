package com.picsearch.app.di

import android.content.Context
import androidx.room.Room
import com.picsearch.app.data.local.AppDatabase
import com.picsearch.app.data.local.BookmarkDao
import com.picsearch.app.data.local.FolderDao
import com.picsearch.app.data.local.MIGRATION_1_2
import com.picsearch.app.data.local.MIGRATION_2_3
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
        Room.databaseBuilder(context, AppDatabase::class.java, "picsearch.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()
}
