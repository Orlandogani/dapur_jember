package com.leanecorps.dapurjember.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.leanecorps.dapurjember.core.data.database.dao.CashMovementDao
import com.leanecorps.dapurjember.core.data.database.dao.CategoryDao
import com.leanecorps.dapurjember.core.data.database.dao.ChangeLogDao
import com.leanecorps.dapurjember.core.data.database.dao.DiningTableDao
import com.leanecorps.dapurjember.core.data.database.dao.FloorAreaDao
import com.leanecorps.dapurjember.core.data.database.dao.ItemModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuItemDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuVariantDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.dao.ShiftDao
import com.leanecorps.dapurjember.core.data.database.dao.StaffDao
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
import com.leanecorps.dapurjember.core.data.database.entity.CashMovementEntity
import com.leanecorps.dapurjember.core.data.database.entity.CategoryEntity
import com.leanecorps.dapurjember.core.data.database.entity.ChangeLogEntity
import com.leanecorps.dapurjember.core.data.database.entity.DiningTableEntity
import com.leanecorps.dapurjember.core.data.database.entity.FloorAreaEntity
import com.leanecorps.dapurjember.core.data.database.entity.ItemModifierGroupEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuItemEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuVariantEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierGroupEntity
import com.leanecorps.dapurjember.core.data.database.entity.ShiftEntity
import com.leanecorps.dapurjember.core.data.database.entity.StaffEntity
import com.leanecorps.dapurjember.core.data.database.entity.StoreProfileEntity

@Database(
    entities = [
        StoreProfileEntity::class,
        CategoryEntity::class,
        MenuItemEntity::class,
        MenuVariantEntity::class,
        ModifierGroupEntity::class,
        ModifierEntity::class,
        ItemModifierGroupEntity::class,
        ChangeLogEntity::class,
        FloorAreaEntity::class,
        DiningTableEntity::class,
        StaffEntity::class,
        ShiftEntity::class,
        CashMovementEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@Suppress("TooManyFunctions") // one accessor per DAO — that is what a @Database class is
abstract class DapurJemberDatabase : RoomDatabase() {
    abstract fun storeProfileDao(): StoreProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun menuVariantDao(): MenuVariantDao
    abstract fun modifierGroupDao(): ModifierGroupDao
    abstract fun modifierDao(): ModifierDao
    abstract fun itemModifierGroupDao(): ItemModifierGroupDao
    abstract fun changeLogDao(): ChangeLogDao
    abstract fun floorAreaDao(): FloorAreaDao
    abstract fun diningTableDao(): DiningTableDao
    abstract fun staffDao(): StaffDao
    abstract fun shiftDao(): ShiftDao
    abstract fun cashMovementDao(): CashMovementDao

    companion object {
        const val NAME = "dapurjember.db"
    }
}
