package com.leanecorps.dapurjember.core.data.auth

import androidx.room.withTransaction
import at.favre.lib.crypto.bcrypt.BCrypt
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.StaffDao
import com.leanecorps.dapurjember.core.data.database.entity.StaffEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.auth.AuthRepository
import com.leanecorps.dapurjember.core.domain.auth.PinHasher
import com.leanecorps.dapurjember.core.domain.auth.Staff
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** bcrypt (pure JVM, no native lib). Cost 10 — fast enough for a 4–6 digit PIN on a tablet. */
internal class BcryptPinHasher @Inject constructor() : PinHasher {
    override fun hash(pin: String): String =
        BCrypt.withDefaults().hashToString(COST, pin.toCharArray())

    override fun verify(pin: String, hash: String): Boolean =
        BCrypt.verifyer().verify(pin.toCharArray(), hash).verified

    private companion object {
        const val COST = 10
    }
}

internal fun StaffEntity.toDomain() = Staff(
    id = id,
    name = name,
    role = runCatching { StaffRole.valueOf(role) }.getOrDefault(StaffRole.WAITER),
    active = active,
)

@Suppress("LongParameterList")
internal class AuthRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val staffDao: StaffDao,
    private val pinHasher: PinHasher,
    private val sessionRepository: SessionRepository,
    private val changeLog: ChangeLogRecorder,
    private val time: TimeProvider,
    private val deviceIds: DeviceIdProvider,
) : AuthRepository {

    override fun observeActiveStaff(): Flow<List<Staff>> =
        staffDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun verifyPin(staffId: String, pin: String): Boolean {
        val staff = staffDao.getById(staffId) ?: return false
        return pinHasher.verify(pin, staff.pinHash)
    }

    override suspend fun signIn(staffId: String, pin: String): Boolean {
        if (!verifyPin(staffId, pin)) return false
        sessionRepository.setCurrentStaff(staffId)
        return true
    }

    override suspend fun signOut() = sessionRepository.setCurrentStaff(null)

    override suspend fun createStaff(name: String, role: StaffRole, pin: String): String = db.withTransaction {
        val id = UuidV7.generate()
        val now = time.nowMillis()
        staffDao.upsert(
            StaffEntity(
                id = id,
                name = name,
                pinHash = pinHasher.hash(pin),
                role = role.name,
                permissionsJson = null,
                active = true,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceIds.deviceId(),
            ),
        )
        changeLog.record("staff", id, ChangeOp.INSERT, now)
        id
    }
}
