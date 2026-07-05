package com.boardmark.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boardmark.app.data.local.AppDatabase
import com.boardmark.app.data.local.FolderDao
import com.boardmark.app.data.local.FolderEntity
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FolderDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.folderDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserveAll() = runTest {
        dao.insert(FolderEntity(name = "Travel", createdAt = Instant.now()))

        val results = dao.observeAll().first()

        assertEquals(1, results.size)
        assertEquals("Travel", results[0].name)
    }

    @Test
    fun findById_returnsNullWhenNotExists() = runTest {
        assertNull(dao.findById(999))
    }

    @Test
    fun delete_removesFolder() = runTest {
        val id = dao.insert(FolderEntity(name = "Travel", createdAt = Instant.now()))

        dao.delete(id)

        assertEquals(0, dao.observeAll().first().size)
    }
}
