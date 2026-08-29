package com.leanecorps.dapurjember.core.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change ships a hand-written, tested migration plus the committed schema JSON
 * (`docs/3-data-model` §6, CLAUDE.md rule 9). `fallbackToDestructiveMigration()` is never used.
 *
 * The SQL below is copied verbatim from `schemas/…/2.json` (`${'$'}{TABLE_NAME}` replaced with the
 * real table name), so `MigrationTestHelper.runMigrationsAndValidate` accepts it byte-for-byte.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(Migration1To2)

/** v1 → v2: adds the floor, staff and shift tables. No existing table is touched. */
internal object Migration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `floor_area` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`sort_order` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "`deleted_at` INTEGER, `device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `staff` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`pin_hash` TEXT NOT NULL, `role` TEXT NOT NULL, `permissions_json` TEXT, `active` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `dining_table` (`id` TEXT NOT NULL, `floor_area_id` TEXT NOT NULL, " +
                "`label` TEXT NOT NULL, `seats` INTEGER NOT NULL, `pos_x` REAL NOT NULL, `pos_y` REAL NOT NULL, " +
                "`state` TEXT NOT NULL, `type` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `device_id` TEXT NOT NULL, " +
                "`revision` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`floor_area_id`) " +
                "REFERENCES `floor_area`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dining_table_floor_area_id` ON `dining_table` (`floor_area_id`)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shift` (`id` TEXT NOT NULL, `opened_by` TEXT NOT NULL, " +
                "`closed_by` TEXT, `opened_at` INTEGER NOT NULL, `closed_at` INTEGER, " +
                "`opening_float_minor` INTEGER NOT NULL, `counted_cash_minor` INTEGER, " +
                "`expected_cash_minor` INTEGER, `variance_minor` INTEGER, `business_day` TEXT NOT NULL, " +
                "`note` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`opened_by`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`closed_by`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_opened_by` ON `shift` (`opened_by`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_closed_by` ON `shift` (`closed_by`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_business_day` ON `shift` (`business_day`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_closed_at` ON `shift` (`closed_at`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_movement` (`id` TEXT NOT NULL, `shift_id` TEXT NOT NULL, " +
                "`direction` TEXT NOT NULL, `amount_minor` INTEGER NOT NULL, `reason` TEXT NOT NULL, " +
                "`staff_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                "`deleted_at` INTEGER, `device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`shift_id`) REFERENCES `shift`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`staff_id`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movement_shift_id` ON `cash_movement` (`shift_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movement_staff_id` ON `cash_movement` (`staff_id`)")
    }
}
