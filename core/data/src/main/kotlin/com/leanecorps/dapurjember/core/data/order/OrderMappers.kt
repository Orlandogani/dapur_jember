package com.leanecorps.dapurjember.core.data.order

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.data.database.entity.DiscountEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.PaymentEntity
import com.leanecorps.dapurjember.core.domain.order.DiscountKind
import com.leanecorps.dapurjember.core.domain.order.Order
import com.leanecorps.dapurjember.core.domain.order.OrderDiscount
import com.leanecorps.dapurjember.core.domain.order.OrderLine
import com.leanecorps.dapurjember.core.domain.order.OrderLineModifier
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.OrderTotals
import com.leanecorps.dapurjember.core.domain.order.Payment
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.domain.pricing.Discount

private const val LINE_VOIDED = "VOIDED"
private const val DISCOUNT_PERCENT = "PERCENT"

internal fun PaymentEntity.toDomain() = Payment(
    id = id,
    method = PaymentMethod.valueOf(method),
    amount = Money(amountMinor),
    tendered = Money(tenderedMinor),
    change = Money(changeMinor),
    reference = reference,
)

internal fun DiscountEntity.toDomain() = OrderDiscount(
    id = id,
    orderLineId = orderLineId,
    kind = DiscountKind.valueOf(type),
    value = value,
    computed = Money(computedMinor),
    reason = reason,
)

internal fun DiscountEntity.toPricingDiscount(): Discount =
    if (type == DISCOUNT_PERCENT) {
        Discount.Percent(basisPoints = value.toInt(), reason = reason)
    } else {
        Discount.Fixed(amount = Money(value), reason = reason)
    }

internal fun OrderLineModifierEntity.toDomain() = OrderLineModifier(
    id = id,
    modifierId = modifierId,
    name = nameSnapshot,
    priceDelta = Money(priceDeltaSnapshotMinor),
)

internal fun OrderLineEntity.toDomain(modifiers: List<OrderLineModifierEntity>) = OrderLine(
    id = id,
    menuVariantId = menuVariantId,
    itemName = itemNameSnapshot,
    variantName = variantNameSnapshot,
    unitPrice = Money(unitPriceSnapshotMinor),
    quantity = qty,
    modifiers = modifiers.map { it.toDomain() },
    note = lineNote,
    course = course,
    sentAt = sentAt,
    voided = state == LINE_VOIDED,
    voidReason = voidReason,
)

internal fun OrderEntity.toDomain(
    lineRows: List<OrderLineEntity>,
    modifierRows: List<OrderLineModifierEntity>,
    paymentRows: List<PaymentEntity>,
    discountRows: List<DiscountEntity>,
): Order {
    val modsByLine = modifierRows.groupBy { it.orderLineId }
    return Order(
        id = id,
        orderNumber = orderNumber,
        diningTableId = diningTableId,
        shiftId = shiftId,
        openedByStaffId = openedByStaffId,
        state = OrderState.fromStorage(state),
        guestCount = guestCount,
        businessDay = businessDay,
        lines = lineRows.map { it.toDomain(modsByLine[it.id].orEmpty()) },
        payments = paymentRows.map { it.toDomain() },
        discounts = discountRows.map { it.toDomain() },
        totals = OrderTotals(
            subtotal = Money(subtotalMinor),
            discount = Money(discountMinor),
            serviceCharge = Money(serviceChargeMinor),
            tax = Money(taxMinor),
            rounding = Money(roundingMinor),
            total = Money(totalMinor),
        ),
        openedAt = openedAt,
        sentAt = sentAt,
        paidAt = paidAt,
        note = note,
    )
}
