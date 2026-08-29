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
val ALL_MIGRATIONS: Array<Migration> = arrayOf(Migration1To2, Migration2To3)

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

/** v2 → v3: adds the orders cluster — orders, lines, line modifiers, payments, discounts. */
internal object Migration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `orders` (`id` TEXT NOT NULL, `order_number` TEXT NOT NULL, " +
                "`dining_table_id` TEXT, `shift_id` TEXT NOT NULL, `opened_by_staff_id` TEXT NOT NULL, " +
                "`closed_by_staff_id` TEXT, `state` TEXT NOT NULL, `guest_count` INTEGER NOT NULL, " +
                "`business_day` TEXT NOT NULL, `opened_at` INTEGER, `sent_at` INTEGER, `paid_at` INTEGER, " +
                "`closed_at` INTEGER, `subtotal_minor` INTEGER NOT NULL, `discount_minor` INTEGER NOT NULL, " +
                "`service_charge_minor` INTEGER NOT NULL, `tax_minor` INTEGER NOT NULL, " +
                "`rounding_minor` INTEGER NOT NULL, `total_minor` INTEGER NOT NULL, `void_reason` TEXT, " +
                "`note` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`dining_table_id`) REFERENCES `dining_table`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`shift_id`) REFERENCES `shift`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`opened_by_staff_id`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`closed_by_staff_id`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_business_day` ON `orders` (`business_day`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_state` ON `orders` (`state`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_orders_dining_table_id_state` ON `orders` (`dining_table_id`, `state`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_shift_id` ON `orders` (`shift_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_opened_by_staff_id` ON `orders` (`opened_by_staff_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_closed_by_staff_id` ON `orders` (`closed_by_staff_id`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `order_line` (`id` TEXT NOT NULL, `order_id` TEXT NOT NULL, " +
                "`menu_variant_id` TEXT NOT NULL, `item_name_snapshot` TEXT NOT NULL, " +
                "`variant_name_snapshot` TEXT NOT NULL, `unit_price_snapshot_minor` INTEGER NOT NULL, " +
                "`qty` INTEGER NOT NULL, `line_note` TEXT, `course` INTEGER NOT NULL, `sent_at` INTEGER, " +
                "`state` TEXT NOT NULL, `void_reason` TEXT, `added_by_staff_id` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`order_id`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`menu_variant_id`) REFERENCES `menu_variant`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`added_by_staff_id`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_line_order_id` ON `order_line` (`order_id`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_order_line_order_id_sent_at` ON `order_line` (`order_id`, `sent_at`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_line_menu_variant_id` ON `order_line` (`menu_variant_id`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_order_line_added_by_staff_id` ON `order_line` (`added_by_staff_id`)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `order_line_modifier` (`id` TEXT NOT NULL, `order_line_id` TEXT NOT NULL, " +
                "`modifier_id` TEXT NOT NULL, `name_snapshot` TEXT NOT NULL, " +
                "`price_delta_snapshot_minor` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `device_id` TEXT NOT NULL, " +
                "`revision` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`order_line_id`) " +
                "REFERENCES `order_line`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`modifier_id`) REFERENCES `modifier`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_order_line_modifier_order_line_id` " +
                "ON `order_line_modifier` (`order_line_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_order_line_modifier_modifier_id` " +
                "ON `order_line_modifier` (`modifier_id`)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payment` (`id` TEXT NOT NULL, `order_id` TEXT NOT NULL, " +
                "`method` TEXT NOT NULL, `amount_minor` INTEGER NOT NULL, `tendered_minor` INTEGER NOT NULL, " +
                "`change_minor` INTEGER NOT NULL, `reference` TEXT, `staff_id` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`order_id`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`staff_id`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_order_id` ON `payment` (`order_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_staff_id` ON `payment` (`staff_id`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `discount` (`id` TEXT NOT NULL, `order_id` TEXT NOT NULL, " +
                "`order_line_id` TEXT, `type` TEXT NOT NULL, `value` INTEGER NOT NULL, " +
                "`computed_minor` INTEGER NOT NULL, `reason` TEXT NOT NULL, `authorised_by_staff_id` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, " +
                "`device_id` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`order_id`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`order_line_id`) REFERENCES `order_line`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`authorised_by_staff_id`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_discount_order_id` ON `discount` (`order_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_discount_order_line_id` ON `discount` (`order_line_id`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_discount_authorised_by_staff_id` " +
                "ON `discount` (`authorised_by_staff_id`)",
        )
    }
}
