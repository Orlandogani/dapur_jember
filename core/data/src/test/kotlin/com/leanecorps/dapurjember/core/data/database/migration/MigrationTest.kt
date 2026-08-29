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

    @Test
    fun `migrate 2 to 3 adds the orders cluster with working foreign keys`() {
        val dbName = "migration-2-3.db"

        helper.createDatabase(dbName, 2).use { v2 ->
            v2.execSQL(
                "INSERT INTO staff (id, name, pin_hash, role, active, created_at, updated_at, device_id, revision) " +
                    "VALUES ('s1', 'Sari', 'h', 'CASHIER', 1, 1, 1, 'dev', 1)",
            )
            v2.execSQL(
                "INSERT INTO shift (id, opened_by, opened_at, opening_float_minor, business_day, " +
                    "created_at, updated_at, device_id, revision) VALUES ('sh1', 's1', 1, 0, '2026-08-29', 1, 1, 'dev', 1)",
            )
            v2.execSQL(
                "INSERT INTO category (id, name, sort_order, active, created_at, updated_at, device_id, revision) " +
                    "VALUES ('c1', 'Rice', 0, 1, 1, 1, 'dev', 1)",
            )
            v2.execSQL(
                "INSERT INTO menu_item (id, category_id, name, sort_order, available, tax_exempt, track_stock, " +
                    "created_at, updated_at, device_id, revision) VALUES ('mi1', 'c1', 'NG', 0, 1, 0, 1, 1, 1, 'dev', 1)",
            )
            v2.execSQL(
                "INSERT INTO menu_variant (id, menu_item_id, name, price_minor, sort_order, " +
                    "created_at, updated_at, device_id, revision) VALUES ('mv1', 'mi1', 'Regular', 15000, 0, 1, 1, 'dev', 1)",
            )
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 3, true, Migration2To3)

        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL(
            "INSERT INTO orders (id, order_number, shift_id, opened_by_staff_id, state, guest_count, business_day, " +
                "subtotal_minor, discount_minor, service_charge_minor, tax_minor, rounding_minor, total_minor, " +
                "created_at, updated_at, device_id, revision) " +
                "VALUES ('o1', 'A-1', 'sh1', 's1', 'DRAFT', 2, '2026-08-29', 0, 0, 0, 0, 0, 0, 1, 1, 'dev', 1)",
        )
        migrated.execSQL(
            "INSERT INTO order_line (id, order_id, menu_variant_id, item_name_snapshot, variant_name_snapshot, " +
                "unit_price_snapshot_minor, qty, course, state, added_by_staff_id, created_at, updated_at, " +
                "device_id, revision) VALUES ('l1', 'o1', 'mv1', 'NG', 'Regular', 15000, 1, 1, 'ACTIVE', 's1', 1, 1, 'dev', 1)",
        )
        migrated.query("SELECT COUNT(*) FROM order_line WHERE order_id = 'o1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun `migrate 3 to 4 adds the audit log and inventory tables`() {
        val dbName = "migration-3-4.db"
        helper.createDatabase(dbName, 3).close()

        val migrated = helper.runMigrationsAndValidate(dbName, 4, true, Migration3To4)
        for (table in listOf("audit_log", "supplier", "ingredient", "recipe_line", "stock_movement")) {
            migrated.query("SELECT COUNT(*) FROM $table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
        migrated.close()
    }

    @Test
    fun `migrate 1 to 4 chained validates`() {
        val dbName = "migration-1-4.db"
        helper.createDatabase(dbName, 1).close()

        helper.runMigrationsAndValidate(dbName, 4, true, Migration1To2, Migration2To3, Migration3To4).close()
    }
}
