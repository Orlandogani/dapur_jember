package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.StoreProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreProfileDao {
    @Upsert
    suspend fun upsert(entity: StoreProfileEntity)

    @Query("SELECT * FROM store_profile LIMIT 1")
    fun observe(): Flow<StoreProfileEntity?>

    @Query("SELECT * FROM store_profile LIMIT 1")
    suspend fun get(): StoreProfileEntity?
}
