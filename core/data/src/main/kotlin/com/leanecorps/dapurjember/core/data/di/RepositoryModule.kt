package com.leanecorps.dapurjember.core.data.di

import com.leanecorps.dapurjember.core.data.auth.AuthRepositoryImpl
import com.leanecorps.dapurjember.core.data.auth.BcryptPinHasher
import com.leanecorps.dapurjember.core.data.config.StoreProfileRepositoryImpl
import com.leanecorps.dapurjember.core.data.floor.FloorRepositoryImpl
import com.leanecorps.dapurjember.core.data.menu.MenuRepositoryImpl
import com.leanecorps.dapurjember.core.data.order.OrderRepositoryImpl
import com.leanecorps.dapurjember.core.data.order.SequentialOrderNumberGenerator
import com.leanecorps.dapurjember.core.data.printing.PrintQueueImpl
import com.leanecorps.dapurjember.core.data.printing.PrinterRepositoryImpl
import com.leanecorps.dapurjember.core.data.session.SessionRepositoryImpl
import com.leanecorps.dapurjember.core.data.shift.ShiftRepositoryImpl
import com.leanecorps.dapurjember.core.domain.auth.AuthRepository
import com.leanecorps.dapurjember.core.domain.auth.PinHasher
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.floor.FloorRepository
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.order.OrderNumberGenerator
import com.leanecorps.dapurjember.core.domain.order.OrderRepository
import com.leanecorps.dapurjember.core.domain.printing.PrintQueue
import com.leanecorps.dapurjember.core.domain.printing.PrinterRepository
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import com.leanecorps.dapurjember.core.domain.shift.ShiftRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindMenuRepository(impl: MenuRepositoryImpl): MenuRepository

    @Binds
    abstract fun bindStoreProfileRepository(impl: StoreProfileRepositoryImpl): StoreProfileRepository

    @Binds
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    abstract fun bindFloorRepository(impl: FloorRepositoryImpl): FloorRepository

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindOrderNumberGenerator(impl: SequentialOrderNumberGenerator): OrderNumberGenerator

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindPinHasher(impl: BcryptPinHasher): PinHasher

    @Binds
    abstract fun bindShiftRepository(impl: ShiftRepositoryImpl): ShiftRepository

    @Binds
    abstract fun bindPrintQueue(impl: PrintQueueImpl): PrintQueue

    @Binds
    abstract fun bindPrinterRepository(impl: PrinterRepositoryImpl): PrinterRepository
}
