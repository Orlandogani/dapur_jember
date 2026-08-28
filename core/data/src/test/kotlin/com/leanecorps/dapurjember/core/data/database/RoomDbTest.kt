package com.leanecorps.dapurjember.core.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Base class: a fresh in-memory [DapurJemberDatabase] per test method (stock SQLite, no SQLCipher). */
@RunWith(RobolectricTestRunner::class)
abstract class RoomDbTest {

    protected lateinit var db: DapurJemberDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DapurJemberDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }
}
