package com.leanecorps.dapurjember.core.data.di

import android.content.Context
import androidx.room.Room
import com.leanecorps.dapurjember.core.data.crypto.DatabasePassphrase
import com.leanecorps.dapurjember.core.data.crypto.KeystoreDatabasePassphrase
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
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
import com.leanecorps.dapurjember.core.data.database.dao.PrintJobDao
import com.leanecorps.dapurjember.core.data.database.dao.PrinterConfigDao
import com.leanecorps.dapurjember.core.data.database.dao.RecipeLineDao
import com.leanecorps.dapurjember.core.data.database.dao.ReportsDao
import com.leanecorps.dapurjember.core.data.database.dao.ShiftDao
import com.leanecorps.dapurjember.core.data.database.dao.StaffDao
import com.leanecorps.dapurjember.core.data.database.dao.StockMovementDao
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
import com.leanecorps.dapurjember.core.data.database.dao.SupplierDao
import com.leanecorps.dapurjember.core.data.database.migration.ALL_MIGRATIONS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * Wires the SQLCipher-encrypted Room database and its DAOs into the Hilt graph.
 *
 * TODO(M5): instrumented test proving the on-disk file is encrypted (unit tests here run on
 * plain SQLite via Robolectric and do not exercise SQLCipher).
 */
@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabasePassphrase(impl: KeystoreDatabasePassphrase): DatabasePassphrase = impl

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphrase: DatabasePassphrase,
    ): DapurJemberDatabase {
        System.loadLibrary("sqlcipher")
        val builder = Room.databaseBuilder(context, DapurJemberDatabase::class.java, DapurJemberDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase.getOrCreate()))
        ALL_MIGRATIONS.forEach { builder.addMigrations(it) }
        return builder.build()
    }

    @Provides
    fun provideStoreProfileDao(db: DapurJemberDatabase): StoreProfileDao = db.storeProfileDao()

    @Provides
    fun provideCategoryDao(db: DapurJemberDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideMenuItemDao(db: DapurJemberDatabase): MenuItemDao = db.menuItemDao()

    @Provides
    fun provideMenuVariantDao(db: DapurJemberDatabase): MenuVariantDao = db.menuVariantDao()

    @Provides
    fun provideModifierGroupDao(db: DapurJemberDatabase): ModifierGroupDao = db.modifierGroupDao()

    @Provides
    fun provideModifierDao(db: DapurJemberDatabase): ModifierDao = db.modifierDao()

    @Provides
    fun provideItemModifierGroupDao(db: DapurJemberDatabase): ItemModifierGroupDao =
        db.itemModifierGroupDao()

    @Provides
    fun provideChangeLogDao(db: DapurJemberDatabase): ChangeLogDao = db.changeLogDao()

    @Provides
    fun provideFloorAreaDao(db: DapurJemberDatabase): FloorAreaDao = db.floorAreaDao()

    @Provides
    fun provideDiningTableDao(db: DapurJemberDatabase): DiningTableDao = db.diningTableDao()

    @Provides
    fun provideStaffDao(db: DapurJemberDatabase): StaffDao = db.staffDao()

    @Provides
    fun provideShiftDao(db: DapurJemberDatabase): ShiftDao = db.shiftDao()

    @Provides
    fun provideCashMovementDao(db: DapurJemberDatabase): CashMovementDao = db.cashMovementDao()

    @Provides
    fun provideOrderDao(db: DapurJemberDatabase): OrderDao = db.orderDao()

    @Provides
    fun provideOrderLineDao(db: DapurJemberDatabase): OrderLineDao = db.orderLineDao()

    @Provides
    fun provideOrderLineModifierDao(db: DapurJemberDatabase): OrderLineModifierDao =
        db.orderLineModifierDao()

    @Provides
    fun providePaymentDao(db: DapurJemberDatabase): PaymentDao = db.paymentDao()

    @Provides
    fun provideDiscountDao(db: DapurJemberDatabase): DiscountDao = db.discountDao()

    @Provides
    fun provideAuditLogDao(db: DapurJemberDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideSupplierDao(db: DapurJemberDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun provideIngredientDao(db: DapurJemberDatabase): IngredientDao = db.ingredientDao()

    @Provides
    fun provideRecipeLineDao(db: DapurJemberDatabase): RecipeLineDao = db.recipeLineDao()

    @Provides
    fun provideStockMovementDao(db: DapurJemberDatabase): StockMovementDao = db.stockMovementDao()

    @Provides
    fun providePrintJobDao(db: DapurJemberDatabase): PrintJobDao = db.printJobDao()

    @Provides
    fun providePrinterConfigDao(db: DapurJemberDatabase): PrinterConfigDao = db.printerConfigDao()

    @Provides
    fun provideReportsDao(db: DapurJemberDatabase): ReportsDao = db.reportsDao()
}
