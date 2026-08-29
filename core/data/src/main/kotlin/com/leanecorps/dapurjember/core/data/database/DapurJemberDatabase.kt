package com.leanecorps.dapurjember.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.leanecorps.dapurjember.core.data.database.dao.AuditLogDao
import com.leanecorps.dapurjember.core.data.database.dao.CashMovementDao
import com.leanecorps.dapurjember.core.data.database.dao.CategoryDao
import com.leanecorps.dapurjember.core.data.database.dao.ChangeLogDao
import com.leanecorps.dapurjember.core.data.database.dao.DiningTableDao
import com.leanecorps.dapurjember.core.data.database.dao.DiscountDao
import com.leanecorps.dapurjember.core.data.database.dao.FloorAreaDao
import com.leanecorps.dapurjember.core.data.database.dao.IngredientDao
import com.leanecorps.dapurjember.core.data.database.dao.ItemModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuItemDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuVariantDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderLineDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderLineModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.PaymentDao
import com.leanecorps.dapurjember.core.data.database.dao.RecipeLineDao
import com.leanecorps.dapurjember.core.data.database.dao.ShiftDao
import com.leanecorps.dapurjember.core.data.database.dao.StaffDao
import com.leanecorps.dapurjember.core.data.database.dao.StockMovementDao
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
import com.leanecorps.dapurjember.core.data.database.dao.SupplierDao
import com.leanecorps.dapurjember.core.data.database.entity.AuditLogEntity
import com.leanecorps.dapurjember.core.data.database.entity.CashMovementEntity
import com.leanecorps.dapurjember.core.data.database.entity.CategoryEntity
import com.leanecorps.dapurjember.core.data.database.entity.ChangeLogEntity
import com.leanecorps.dapurjember.core.data.database.entity.DiningTableEntity
import com.leanecorps.dapurjember.core.data.database.entity.DiscountEntity
import com.leanecorps.dapurjember.core.data.database.entity.FloorAreaEntity
import com.leanecorps.dapurjember.core.data.database.entity.IngredientEntity
import com.leanecorps.dapurjember.core.data.database.entity.ItemModifierGroupEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuItemEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuVariantEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierGroupEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.PaymentEntity
import com.leanecorps.dapurjember.core.data.database.entity.RecipeLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.ShiftEntity
import com.leanecorps.dapurjember.core.data.database.entity.StaffEntity
import com.leanecorps.dapurjember.core.data.database.entity.StockMovementEntity
import com.leanecorps.dapurjember.core.data.database.entity.StoreProfileEntity
import com.leanecorps.dapurjember.core.data.database.entity.SupplierEntity

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
        OrderEntity::class,
        OrderLineEntity::class,
        OrderLineModifierEntity::class,
        PaymentEntity::class,
        DiscountEntity::class,
        AuditLogEntity::class,
        SupplierEntity::class,
        IngredientEntity::class,
        RecipeLineEntity::class,
        StockMovementEntity::class,
    ],
    version = 4,
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
    abstract fun orderDao(): OrderDao
    abstract fun orderLineDao(): OrderLineDao
    abstract fun orderLineModifierDao(): OrderLineModifierDao
    abstract fun paymentDao(): PaymentDao
    abstract fun discountDao(): DiscountDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun supplierDao(): SupplierDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeLineDao(): RecipeLineDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        const val NAME = "dapurjember.db"
    }
}
