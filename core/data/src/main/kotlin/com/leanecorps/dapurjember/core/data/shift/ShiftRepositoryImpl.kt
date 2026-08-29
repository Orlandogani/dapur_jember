package com.leanecorps.dapurjember.core.data.shift

import androidx.room.withTransaction
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.CashMovementDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderDao
import com.leanecorps.dapurjember.core.data.database.dao.PaymentDao
import com.leanecorps.dapurjember.core.data.database.dao.ShiftDao
import com.leanecorps.dapurjember.core.data.database.entity.CashMovementEntity
import com.leanecorps.dapurjember.core.data.database.entity.ShiftEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.shift.CashDirection
import com.leanecorps.dapurjember.core.domain.shift.OpenOrdersBlockCloseException
import com.leanecorps.dapurjember.core.domain.shift.Shift
import com.leanecorps.dapurjember.core.domain.shift.ShiftCloseResult
import com.leanecorps.dapurjember.core.domain.shift.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val CASH = "CASH"

internal fun ShiftEntity.toDomain() = Shift(
    id = id,
    openedByStaffId = openedBy,
    openedAt = openedAt,
    openingFloat = Money(openingFloatMinor),
    businessDay = businessDay,
    closedAt = closedAt,
    countedCash = countedCashMinor?.let { Money(it) },
    expectedCash = expectedCashMinor?.let { Money(it) },
    variance = varianceMinor?.let { Money(it) },
)

@Suppress("LongParameterList")
internal class ShiftRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val shiftDao: ShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val paymentDao: PaymentDao,
    private val orderDao: OrderDao,
    private val changeLog: ChangeLogRecorder,
    private val time: TimeProvider,
    private val deviceIds: DeviceIdProvider,
) : ShiftRepository {

    override fun observeOpenShift(): Flow<Shift?> = shiftDao.observeOpenShift().map { it?.toDomain() }

    override suspend fun openShift(openingFloatMinor: Long, staffId: String, businessDay: String): String =
        db.withTransaction {
            val id = UuidV7.generate()
            val now = time.nowMillis()
            shiftDao.upsert(
                ShiftEntity(
                    id = id,
                    openedBy = staffId,
                    openedAt = now,
                    openingFloatMinor = openingFloatMinor,
                    businessDay = businessDay,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceIds.deviceId(),
                ),
            )
            changeLog.record("shift", id, ChangeOp.INSERT, now)
            id
        }

    override suspend fun recordCashMovement(
        shiftId: String,
        direction: CashDirection,
        amountMinor: Long,
        reason: String,
        staffId: String,
    ) = db.withTransaction {
        val id = UuidV7.generate()
        val now = time.nowMillis()
        cashMovementDao.insert(
            CashMovementEntity(
                id = id,
                shiftId = shiftId,
                direction = direction.name,
                amountMinor = amountMinor,
                reason = reason,
                staffId = staffId,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceIds.deviceId(),
            ),
        )
        changeLog.record("cash_movement", id, ChangeOp.INSERT, now)
    }

    override suspend fun expectedCashMinor(shiftId: String): Long {
        val shift = shiftDao.getById(shiftId) ?: return 0L
        val cashSales = paymentDao.totalForShiftByMethod(shiftId, CASH)
        val cashIn = cashMovementDao.totalForShift(shiftId, CashDirection.IN.name)
        val cashOut = cashMovementDao.totalForShift(shiftId, CashDirection.OUT.name)
        return shift.openingFloatMinor + cashSales + cashIn - cashOut
    }

    override suspend fun closeShift(shiftId: String, countedCashMinor: Long, staffId: String): ShiftCloseResult =
        db.withTransaction {
            val unpaid = orderDao.unpaidOrderIdsForShift(shiftId)
            if (unpaid.isNotEmpty()) throw OpenOrdersBlockCloseException(unpaid)

            val expected = expectedCashMinor(shiftId)
            val variance = countedCashMinor - expected
            val now = time.nowMillis()
            shiftDao.close(
                id = shiftId,
                closedAt = now,
                closedBy = staffId,
                countedCash = countedCashMinor,
                expectedCash = expected,
                variance = variance,
            )
            changeLog.record("shift", shiftId, ChangeOp.UPDATE, now)
            ShiftCloseResult(
                expectedCash = Money(expected),
                countedCash = Money(countedCashMinor),
                variance = Money(variance),
            )
        }
}
