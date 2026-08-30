package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.auth.AuthRepository
import com.leanecorps.dapurjember.core.domain.auth.Staff
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** In-memory [AuthRepository]. PINs are compared in plaintext; [session] receives the sign-in. */
class FakeAuthRepository(
    private val session: FakeSessionRepository = FakeSessionRepository(initial = null),
) : AuthRepository {

    private data class Record(val staff: Staff, val pin: String)

    private val records = MutableStateFlow<List<Record>>(emptyList())
    private val ids = AtomicLong(0)

    fun sessionRepository(): FakeSessionRepository = session

    fun addStaff(id: String, name: String, pin: String, role: StaffRole = StaffRole.CASHIER) =
        records.update { it + Record(Staff(id, name, role), pin) }

    override fun observeActiveStaff(): Flow<List<Staff>> =
        records.map { list -> list.filter { it.staff.active }.map { it.staff } }

    override suspend fun verifyPin(staffId: String, pin: String): Boolean =
        records.value.any { it.staff.id == staffId && it.pin == pin }

    override suspend fun staffById(staffId: String): Staff? =
        records.value.firstOrNull { it.staff.id == staffId }?.staff

    override suspend fun signIn(staffId: String, pin: String): Boolean {
        if (!verifyPin(staffId, pin)) return false
        session.setCurrentStaff(staffId)
        return true
    }

    override suspend fun signOut() = session.setCurrentStaff(null)

    override suspend fun createStaff(name: String, role: StaffRole, pin: String): String {
        val id = "staff-${ids.incrementAndGet()}"
        addStaff(id, name, pin, role)
        return id
    }

    override fun observeAllStaff(): Flow<List<Staff>> = records.map { list -> list.map { it.staff } }

    override suspend fun updateStaff(staffId: String, name: String, role: StaffRole, actorStaffId: String) =
        mutate(staffId) { it.copy(staff = it.staff.copy(name = name, role = role)) }

    override suspend fun setStaffActive(staffId: String, active: Boolean, actorStaffId: String) =
        mutate(staffId) { it.copy(staff = it.staff.copy(active = active)) }

    override suspend fun resetPin(staffId: String, newPin: String, actorStaffId: String) =
        mutate(staffId) { it.copy(pin = newPin) }

    private inline fun mutate(staffId: String, transform: (Record) -> Record) =
        records.update { list -> list.map { if (it.staff.id == staffId) transform(it) else it } }
}
