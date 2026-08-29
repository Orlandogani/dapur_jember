package com.leanecorps.dapurjember.core.domain.shift

import com.leanecorps.dapurjember.core.common.money.Money
import kotlinx.coroutines.flow.Flow

enum class CashDirection { IN, OUT }

data class Shift(
    val id: String,
    val openedByStaffId: String,
    val openedAt: Long,
    val openingFloat: Money,
    val businessDay: String,
    val closedAt: Long? = null,
    val countedCash: Money? = null,
    val expectedCash: Money? = null,
    val variance: Money? = null,
) {
    val isOpen: Boolean get() = closedAt == null
}

data class CashMovement(
    val id: String,
    val shiftId: String,
    val direction: CashDirection,
    val amount: Money,
    val reason: String,
    val staffId: String,
)

/** The blind Z-report (FR-S3): the counted amount is entered before this is revealed (FR-S4). */
data class ShiftCloseResult(
    val expectedCash: Money,
    val countedCash: Money,
    val variance: Money,
)

/** An unpaid order blocks a shift close (FR-S5). */
class OpenOrdersBlockCloseException(val orderIds: List<String>) :
    IllegalStateException("cannot close: ${orderIds.size} unpaid order(s)")

interface ShiftRepository {

    fun observeOpenShift(): Flow<Shift?>

    suspend fun openShift(openingFloatMinor: Long, staffId: String, businessDay: String): String

    suspend fun recordCashMovement(
        shiftId: String,
        direction: CashDirection,
        amountMinor: Long,
        reason: String,
        staffId: String,
    )

    /** Expected cash = float + cash sales + cash-in − cash-out (for the blind close preview). */
    suspend fun expectedCashMinor(shiftId: String): Long

    /** Closes the shift; throws [OpenOrdersBlockCloseException] if any order is still unpaid. */
    suspend fun closeShift(shiftId: String, countedCashMinor: Long, staffId: String): ShiftCloseResult
}
