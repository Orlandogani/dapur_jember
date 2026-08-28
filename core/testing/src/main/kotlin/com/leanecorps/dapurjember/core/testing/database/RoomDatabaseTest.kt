package com.leanecorps.dapurjember.core.testing.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Base class for DAO tests: a fresh in-memory [DapurJemberDatabase] per test method.
 * Runs on stock SQLite via Robolectric — SQLCipher is not exercised here.
 */
@RunWith(RobolectricTestRunner::class)
abstract class RoomDatabaseTest {

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
