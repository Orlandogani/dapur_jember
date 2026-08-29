package com.leanecorps.dapurjember.core.testing.database

import com.leanecorps.dapurjember.core.data.database.entity.CashMovementEntity
import com.leanecorps.dapurjember.core.data.database.entity.DiningTableEntity
import com.leanecorps.dapurjember.core.data.database.entity.FloorAreaEntity
import com.leanecorps.dapurjember.core.data.database.entity.ShiftEntity
import com.leanecorps.dapurjember.core.data.database.entity.StaffEntity

/** Test-data builders for the floor / people / shift entities (schema v2). */
object OperationalEntityFixtures {

    private const val DEVICE = "test-device"

    fun floorArea(
        id: String = "area-1",
        name: String = "Main Hall",
        sortOrder: Int = 0,
        deletedAt: Long? = null,
    ) = FloorAreaEntity(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun diningTable(
        id: String = "table-1",
        floorAreaId: String = "area-1",
        label: String = "T1",
        state: String = "FREE",
        type: String = "DINE_IN",
        deletedAt: Long? = null,
    ) = DiningTableEntity(
        id = id,
        floorAreaId = floorAreaId,
        label = label,
        seats = 4,
        posX = 0.1,
        posY = 0.2,
        state = state,
        type = type,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun staff(
        id: String = "staff-1",
        name: String = "Sari",
        role: String = "CASHIER",
        active: Boolean = true,
        deletedAt: Long? = null,
    ) = StaffEntity(
        id = id,
        name = name,
        pinHash = "argon2id\$placeholder",
        role = role,
        permissionsJson = null,
        active = active,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun shift(
        id: String = "shift-1",
        openedBy: String = "staff-1",
        openedAt: Long = 1L,
        closedAt: Long? = null,
        businessDay: String = "2026-08-29",
        deletedAt: Long? = null,
    ) = ShiftEntity(
        id = id,
        openedBy = openedBy,
        closedBy = if (closedAt == null) null else openedBy,
        openedAt = openedAt,
        closedAt = closedAt,
        openingFloatMinor = 500_000,
        countedCashMinor = null,
        expectedCashMinor = null,
        varianceMinor = null,
        businessDay = businessDay,
        note = null,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun cashMovement(
        id: String = "cash-1",
        shiftId: String = "shift-1",
        staffId: String = "staff-1",
        direction: String = "OUT",
        amountMinor: Long = 100_000,
        createdAt: Long = 1L,
    ) = CashMovementEntity(
        id = id,
        shiftId = shiftId,
        direction = direction,
        amountMinor = amountMinor,
        reason = "bank drop",
        staffId = staffId,
        createdAt = createdAt,
        updatedAt = createdAt,
        deletedAt = null,
        deviceId = DEVICE,
    )
}
