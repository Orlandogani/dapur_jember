package com.leanecorps.dapurjember.core.data.database.migration

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DapurJemberDatabase::class.java,
    )

    @Test
    fun `migrate 1 to 2 keeps existing rows and adds the new tables`() {
        val dbName = "migration-1-2.db"

        helper.createDatabase(dbName, 1).use { v1 ->
            v1.execSQL(
                "INSERT INTO category (id, name, sort_order, active, created_at, updated_at, device_id, revision) " +
                    "VALUES ('c1', 'Rice', 0, 1, 1, 1, 'dev', 1)",
            )
        }

        // runMigrationsAndValidate also asserts the result matches schemas/…/2.json byte-for-byte.
        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, Migration1To2)

        migrated.query("SELECT name FROM category WHERE id = 'c1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Rice", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM cash_movement").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }
}
