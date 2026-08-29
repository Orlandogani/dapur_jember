package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.session.Session
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionRepository(
    initial: Session? = Session(staffId = "staff-1", shiftId = "shift-1", businessDay = "2026-08-29"),
) : SessionRepository {

    private val session = MutableStateFlow(initial)
    private val staffId = MutableStateFlow(initial?.staffId)

    override fun observeSession(): Flow<Session?> = session

    override suspend fun currentSession(): Session? = session.value

    override fun observeCurrentStaffId(): Flow<String?> = staffId

    override suspend fun currentStaffId(): String? = staffId.value

    override suspend fun currentBusinessDay(): String = "2026-08-29"

    override suspend fun setCurrentStaff(staffId: String?) {
        this.staffId.value = staffId
        session.value = staffId?.let { Session(it, "shift-1", "2026-08-29") }
    }
}
