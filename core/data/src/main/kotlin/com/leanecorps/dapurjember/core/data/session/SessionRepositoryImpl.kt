package com.leanecorps.dapurjember.core.data.session

import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.config.toDomain
import com.leanecorps.dapurjember.core.data.database.dao.ShiftDao
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
import com.leanecorps.dapurjember.core.domain.config.BusinessDayCalculator
import com.leanecorps.dapurjember.core.domain.session.Session
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session state. The signed-in staff id is held in memory for now (a PIN unlock is the
 * app's resting state, so a process-death sign-out is acceptable — v1). The shift comes
 * from the one open `shift` row; the business day is computed from the store profile.
 */
@Singleton
internal class SessionRepositoryImpl @Inject constructor(
    private val shiftDao: ShiftDao,
    private val storeProfileDao: StoreProfileDao,
    private val businessDayCalculator: BusinessDayCalculator,
    private val time: TimeProvider,
) : SessionRepository {

    private val currentStaffId = MutableStateFlow<String?>(null)

    override fun observeCurrentStaffId(): Flow<String?> = currentStaffId

    override suspend fun currentStaffId(): String? = currentStaffId.value

    override fun observeSession(): Flow<Session?> =
        combine(currentStaffId, shiftDao.observeOpenShift()) { staffId, shift ->
            if (staffId == null || shift == null) {
                null
            } else {
                Session(staffId = staffId, shiftId = shift.id, businessDay = businessDay())
            }
        }

    override suspend fun currentSession(): Session? {
        val staffId = currentStaffId.value
        val shift = shiftDao.observeOpenShift().first()
        return if (staffId != null && shift != null) {
            Session(staffId = staffId, shiftId = shift.id, businessDay = businessDay())
        } else {
            null
        }
    }

    override suspend fun setCurrentStaff(staffId: String?) {
        currentStaffId.value = staffId
    }

    override suspend fun currentBusinessDay(): String = businessDay()

    private suspend fun businessDay(): String {
        val profile = storeProfileDao.get()?.toDomain()
        val now = time.nowMillis()
        return if (profile == null) {
            businessDayCalculator.businessDay(now, cutoffMinutes = 0, zoneId = "UTC")
        } else {
            businessDayCalculator.businessDay(now, profile)
        }
    }
}
