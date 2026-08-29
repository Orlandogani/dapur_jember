package com.leanecorps.dapurjember.core.data.config

import androidx.room.withTransaction
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
import com.leanecorps.dapurjember.core.data.database.entity.StoreProfileEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.config.StoreProfile
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal fun StoreProfileEntity.toDomain() = StoreProfile(
    id = id,
    name = name,
    currencyCode = currencyCode,
    currencyMinorUnits = currencyMinorUnits,
    taxRateBasisPoints = taxRateBp,
    taxInclusive = taxInclusive,
    serviceChargeBasisPoints = serviceChargeBp,
    serviceChargeTaxable = serviceChargeTaxable,
    roundingRule = runCatching { RoundingRule.valueOf(roundingMode) }.getOrDefault(RoundingRule.NONE),
    businessDayCutoffMinutes = businessDayCutoffMin,
    timezoneId = timezoneId,
    receiptHeader = receiptHeader,
    receiptFooter = receiptFooter,
)

internal fun StoreProfile.toEntity(existing: StoreProfileEntity?, now: Long, deviceId: String) = StoreProfileEntity(
    id = id,
    name = name,
    address = existing?.address,
    phone = existing?.phone,
    taxId = existing?.taxId,
    currencyCode = currencyCode,
    currencyMinorUnits = currencyMinorUnits,
    taxRateBp = taxRateBasisPoints,
    taxInclusive = taxInclusive,
    serviceChargeBp = serviceChargeBasisPoints,
    serviceChargeTaxable = serviceChargeTaxable,
    roundingMode = roundingRule.name,
    businessDayCutoffMin = businessDayCutoffMinutes,
    timezoneId = timezoneId,
    receiptHeader = receiptHeader,
    receiptFooter = receiptFooter,
    createdAt = existing?.createdAt ?: now,
    updatedAt = now,
    deletedAt = existing?.deletedAt,
    deviceId = existing?.deviceId ?: deviceId,
    revision = (existing?.revision ?: 0) + 1,
)

internal class StoreProfileRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val dao: StoreProfileDao,
    private val changeLog: ChangeLogRecorder,
    private val time: TimeProvider,
    private val deviceIds: DeviceIdProvider,
) : StoreProfileRepository {

    override fun observeProfile(): Flow<StoreProfile?> = dao.observe().map { it?.toDomain() }

    override suspend fun getProfile(): StoreProfile? = dao.get()?.toDomain()

    override suspend fun upsertProfile(profile: StoreProfile) = db.withTransaction {
        val existing = dao.get()
        val now = time.nowMillis()
        dao.upsert(profile.toEntity(existing, now, deviceIds.deviceId()))
        changeLog.record("store_profile", profile.id, if (existing == null) ChangeOp.INSERT else ChangeOp.UPDATE, now)
    }
}
