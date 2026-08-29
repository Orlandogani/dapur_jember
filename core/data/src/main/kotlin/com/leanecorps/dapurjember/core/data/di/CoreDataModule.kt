package com.leanecorps.dapurjember.core.data.di

import com.leanecorps.dapurjember.core.common.time.SystemTimeProvider
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreDataModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
}
