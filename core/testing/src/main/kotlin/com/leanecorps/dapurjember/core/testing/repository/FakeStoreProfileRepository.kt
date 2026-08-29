package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.config.StoreProfile
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStoreProfileRepository(initial: StoreProfile? = null) : StoreProfileRepository {

    private val profile = MutableStateFlow(initial)

    override fun observeProfile(): Flow<StoreProfile?> = profile

    override suspend fun getProfile(): StoreProfile? = profile.value

    override suspend fun upsertProfile(profile: StoreProfile) {
        this.profile.value = profile
    }
}
