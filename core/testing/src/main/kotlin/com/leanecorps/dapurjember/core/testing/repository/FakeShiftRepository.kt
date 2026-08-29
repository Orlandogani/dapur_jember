package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.shift.CashDirection
import com.leanecorps.dapurjember.core.domain.shift.OpenOrdersBlockCloseException
import com.leanecorps.dapurjember.core.domain.shift.Shift
import com.leanecorps.dapurjember.core.domain.shift.ShiftCloseResult
import com.leanecorps.dapurjember.core.domain.shift.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicLong

class FakeShiftRepository : ShiftRepository {

    private val openShift = MutableStateFlow<Shift?>(null)
    private val ids = AtomicLong(0)

    /** Extra cash the test says is in the drawer beyond the opening float. */
    var extraCashMinor: Long = 0

    /** Order ids the test says are still unpaid (blocks close). */
    var unpaidOrderIds: List<String> = emptyList()

    override fun observeOpenShift(): Flow<Shift?> = openShift

    override suspend fun openShift(openingFloatMinor: Long, staffId: String, businessDay: String): String {
        val id = "shift-${ids.incrementAndGet()}"
        openShift.value = Shift(
            id = id,
            openedByStaffId = staffId,
            openedAt = 0L,
            openingFloat = Money(openingFloatMinor),
            businessDay = businessDay,
        )
        return id
    }

    override suspend fun recordCashMovement(
        shiftId: String,
        direction: CashDirection,
        amountMinor: Long,
        reason: String,
        staffId: String,
    ) {
        extraCashMinor += if (direction == CashDirection.IN) amountMinor else -amountMinor
    }

    override suspend fun expectedCashMinor(shiftId: String): Long =
        (openShift.value?.openingFloat?.minor ?: 0L) + extraCashMinor

    override suspend fun closeShift(shiftId: String, countedCashMinor: Long, staffId: String): ShiftCloseResult {
        if (unpaidOrderIds.isNotEmpty()) throw OpenOrdersBlockCloseException(unpaidOrderIds)
        val expected = expectedCashMinor(shiftId)
        openShift.value = null
        return ShiftCloseResult(
            expectedCash = Money(expected),
            countedCash = Money(countedCashMinor),
            variance = Money(countedCashMinor - expected),
        )
    }
}
