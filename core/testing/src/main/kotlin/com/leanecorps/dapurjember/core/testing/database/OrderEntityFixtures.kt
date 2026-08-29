package com.leanecorps.dapurjember.core.testing.database

import com.leanecorps.dapurjember.core.data.database.entity.DiscountEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.PaymentEntity

/**
 * Test-data builders for the orders cluster (schema v3). Default ids line up with
 * [MenuEntityFixtures] / [OperationalEntityFixtures] so a seeded DB satisfies the FKs.
 */
object OrderEntityFixtures {

    private const val DEVICE = "test-device"

    fun order(
        id: String = "order-1",
        shiftId: String = "shift-1",
        openedByStaffId: String = "staff-1",
        diningTableId: String? = "table-1",
        state: String = "DRAFT",
        businessDay: String = "2026-08-29",
        openedAt: Long? = 1L,
        deletedAt: Long? = null,
    ) = OrderEntity(
        id = id,
        orderNumber = "A-001",
        diningTableId = diningTableId,
        shiftId = shiftId,
        openedByStaffId = openedByStaffId,
        closedByStaffId = null,
        state = state,
        guestCount = 2,
        businessDay = businessDay,
        openedAt = openedAt,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun orderLine(
        id: String = "line-1",
        orderId: String = "order-1",
        menuVariantId: String = "var-1",
        addedByStaffId: String = "staff-1",
        qty: Int = 1,
        course: Int = 1,
        sentAt: Long? = null,
        state: String = "ACTIVE",
        deletedAt: Long? = null,
    ) = OrderLineEntity(
        id = id,
        orderId = orderId,
        menuVariantId = menuVariantId,
        itemNameSnapshot = "Nasi Goreng Ayam",
        variantNameSnapshot = "Regular",
        unitPriceSnapshotMinor = 15_000,
        qty = qty,
        lineNote = null,
        course = course,
        sentAt = sentAt,
        state = state,
        voidReason = null,
        addedByStaffId = addedByStaffId,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun orderLineModifier(
        id: String = "olm-1",
        orderLineId: String = "line-1",
        modifierId: String = "mod-1",
    ) = OrderLineModifierEntity(
        id = id,
        orderLineId = orderLineId,
        modifierId = modifierId,
        nameSnapshot = "Extra hot",
        priceDeltaSnapshotMinor = 0,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
        deviceId = DEVICE,
    )

    fun payment(
        id: String = "pay-1",
        orderId: String = "order-1",
        staffId: String = "staff-1",
        method: String = "CASH",
        amountMinor: Long = 15_000,
        createdAt: Long = 1L,
    ) = PaymentEntity(
        id = id,
        orderId = orderId,
        method = method,
        amountMinor = amountMinor,
        tenderedMinor = amountMinor,
        changeMinor = 0,
        reference = null,
        staffId = staffId,
        createdAt = createdAt,
        updatedAt = createdAt,
        deletedAt = null,
        deviceId = DEVICE,
    )

    fun discount(
        id: String = "disc-1",
        orderId: String = "order-1",
        orderLineId: String? = null,
        type: String = "PERCENT",
        value: Long = 1_000,
        computedMinor: Long = 1_500,
        authorisedByStaffId: String = "staff-1",
    ) = DiscountEntity(
        id = id,
        orderId = orderId,
        orderLineId = orderLineId,
        type = type,
        value = value,
        computedMinor = computedMinor,
        reason = "promo",
        authorisedByStaffId = authorisedByStaffId,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
        deviceId = DEVICE,
    )
}
