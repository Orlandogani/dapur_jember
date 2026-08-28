package com.leanecorps.dapurjember.core.data.di

import android.content.Context
import androidx.room.Room
import com.leanecorps.dapurjember.core.data.crypto.DatabasePassphrase
import com.leanecorps.dapurjember.core.data.crypto.KeystoreDatabasePassphrase
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.CategoryDao
import com.leanecorps.dapurjember.core.data.database.dao.ChangeLogDao
import com.leanecorps.dapurjember.core.data.database.dao.ItemModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuItemDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuVariantDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
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
        return Room.databaseBuilder(context, DapurJemberDatabase::class.java, DapurJemberDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase.getOrCreate()))
            .build()
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
}
