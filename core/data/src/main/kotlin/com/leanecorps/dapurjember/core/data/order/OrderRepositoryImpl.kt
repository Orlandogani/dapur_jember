package com.leanecorps.dapurjember.core.data.order

import androidx.room.withTransaction
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.config.toDomain
import com.leanecorps.dapurjember.core.data.database.AuditLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.DiscountDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuItemDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuVariantDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderLineDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderLineModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.PaymentDao
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
import com.leanecorps.dapurjember.core.data.database.entity.DiscountEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.PaymentEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.order.AddLineParams
import com.leanecorps.dapurjember.core.domain.order.ApplyDiscountParams
import com.leanecorps.dapurjember.core.domain.order.DiscountKind
import com.leanecorps.dapurjember.core.domain.order.OpenOrderParams
import com.leanecorps.dapurjember.core.domain.order.Order
import com.leanecorps.dapurjember.core.domain.order.OrderEvent
import com.leanecorps.dapurjember.core.domain.order.OrderLine
import com.leanecorps.dapurjember.core.domain.order.OrderRepository
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.OrderStateMachine
import com.leanecorps.dapurjember.core.domain.order.RecordPaymentParams
import com.leanecorps.dapurjember.core.domain.pricing.PricingConfig
import com.leanecorps.dapurjember.core.domain.pricing.PricingEngine
import com.leanecorps.dapurjember.core.domain.pricing.PricingLine
import com.leanecorps.dapurjember.core.domain.pricing.PricingRequest
import com.leanecorps.dapurjember.core.domain.pricing.TaxConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

private const val LINE_ACTIVE = "ACTIVE"

@Suppress("LongParameterList")
internal class OrderRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val orderDao: OrderDao,
    private val lineDao: OrderLineDao,
    private val lineModifierDao: OrderLineModifierDao,
    private val paymentDao: PaymentDao,
    private val discountDao: DiscountDao,
    private val menuVariantDao: MenuVariantDao,
    private val menuItemDao: MenuItemDao,
    private val modifierDao: ModifierDao,
    private val storeProfileDao: StoreProfileDao,
    private val changeLog: ChangeLogRecorder,
    private val auditLog: AuditLogRecorder,
    private val time: TimeProvider,
    private val deviceIds: DeviceIdProvider,
) : OrderRepository {

    override fun observeOrder(orderId: String): Flow<Order?> =
        combine(
            orderDao.observeById(orderId),
            lineDao.observeForOrder(orderId),
            lineModifierDao.observeForOrder(orderId),
            paymentDao.observeForOrder(orderId),
            discountDao.observeForOrder(orderId),
        ) { order, lines, modifiers, payments, discounts ->
            order?.toDomain(lines, modifiers, payments, discounts)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveOrderForTable(tableId: String): Flow<Order?> =
        orderDao.observeActiveForTable(tableId).flatMapLatest { entity ->
            if (entity == null) flowOf(null) else observeOrder(entity.id)
        }

    override suspend fun getOrder(orderId: String): Order? {
        val order = orderDao.getById(orderId) ?: return null
        val lines = lineDao.getForOrder(orderId)
        val modifiers = lines.flatMap { lineModifierDao.getForLine(it.id) }
        val payments = paymentDao.getForOrder(orderId)
        val discounts = discountDao.getForOrder(orderId)
        return order.toDomain(lines, modifiers, payments, discounts)
    }

    override suspend fun openOrder(params: OpenOrderParams): String = db.withTransaction {
        val now = time.nowMillis()
        val id = UuidV7.generate()
        orderDao.upsert(
            OrderEntity(
                id = id,
                orderNumber = params.orderNumber,
                diningTableId = params.diningTableId,
                shiftId = params.shiftId,
                openedByStaffId = params.openedByStaffId,
                state = OrderState.DRAFT.storageValue,
                guestCount = params.guestCount,
                businessDay = params.businessDay,
                openedAt = now,
                note = params.note,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceIds.deviceId(),
            ),
        )
        changeLog.record("orders", id, ChangeOp.INSERT, now)
        id
    }

    override suspend fun addLine(params: AddLineParams): String = db.withTransaction {
        val now = time.nowMillis()
        val variant = requireNotNull(menuVariantDao.getById(params.menuVariantId)) {
            "unknown menu variant ${params.menuVariantId}"
        }
        val item = requireNotNull(menuItemDao.getById(variant.menuItemId)) {
            "unknown menu item ${variant.menuItemId}"
        }
        val lineId = UuidV7.generate()
        lineDao.upsert(
            OrderLineEntity(
                id = lineId,
                orderId = params.orderId,
                menuVariantId = variant.id,
                itemNameSnapshot = item.name,
                variantNameSnapshot = variant.name,
                unitPriceSnapshotMinor = variant.priceMinor,
                qty = params.quantity,
                lineNote = params.note,
                course = params.course,
                addedByStaffId = params.addedByStaffId,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceIds.deviceId(),
            ),
        )
        for (modifierId in params.modifierIds) {
            val modifier = requireNotNull(modifierDao.getById(modifierId)) { "unknown modifier $modifierId" }
            lineModifierDao.insert(
                OrderLineModifierEntity(
                    id = UuidV7.generate(),
                    orderLineId = lineId,
                    modifierId = modifier.id,
                    nameSnapshot = modifier.name,
                    priceDeltaSnapshotMinor = modifier.priceDeltaMinor,
                    createdAt = now,
                    updatedAt = now,
                    deviceId = deviceIds.deviceId(),
                ),
            )
        }
        changeLog.record("order_line", lineId, ChangeOp.INSERT, now)
        recomputeTotals(params.orderId, now)
        lineId
    }

    override suspend fun setLineQuantity(lineId: String, quantity: Int) = db.withTransaction {
        require(quantity > 0) { "quantity must be > 0, was $quantity" }
        val existing = lineDao.getById(lineId) ?: return@withTransaction
        val now = time.nowMillis()
        lineDao.upsert(existing.copy(qty = quantity, updatedAt = now, revision = existing.revision + 1))
        changeLog.record("order_line", lineId, ChangeOp.UPDATE, now)
        recomputeTotals(existing.orderId, now)
    }

    override suspend fun voidLine(lineId: String, reason: String, actorStaffId: String) = db.withTransaction {
        val existing = lineDao.getById(lineId) ?: return@withTransaction
        val now = time.nowMillis()
        lineDao.voidLine(lineId, reason, now)
        changeLog.record("order_line", lineId, ChangeOp.UPDATE, now)
        auditLog.record(actorStaffId, "VOID_LINE", "order_line", lineId, at = now, reason = reason)
        recomputeTotals(existing.orderId, now)
    }

    override suspend fun sendToKitchen(orderId: String): List<OrderLine> = db.withTransaction {
        val now = time.nowMillis()
        val unsent = lineDao.getUnsent(orderId)
        if (unsent.isNotEmpty()) {
            lineDao.markSent(unsent.map { it.id }, now)
            unsent.forEach { changeLog.record("order_line", it.id, ChangeOp.UPDATE, now) }
        }
        val order = orderDao.getById(orderId)
        if (order != null && order.state == OrderState.DRAFT.storageValue) {
            orderDao.updateState(orderId, OrderState.SENT.storageValue, now)
            orderDao.markSent(orderId, now)
            changeLog.record("orders", orderId, ChangeOp.UPDATE, now)
        }
        unsent.map { row -> row.toDomain(lineModifierDao.getForLine(row.id)) }
    }

    override suspend fun applyEvent(orderId: String, event: OrderEvent) = db.withTransaction {
        val order = requireNotNull(orderDao.getById(orderId)) { "unknown order $orderId" }
        val target = OrderStateMachine.transition(OrderState.fromStorage(order.state), event)
        val now = time.nowMillis()
        orderDao.updateState(orderId, target.storageValue, now)
        changeLog.record("orders", orderId, ChangeOp.UPDATE, now)
    }

    override suspend fun applyDiscount(params: ApplyDiscountParams): String = db.withTransaction {
        val now = time.nowMillis()
        val id = UuidV7.generate()
        val computed = discountSnapshot(params)
        discountDao.insert(
            DiscountEntity(
                id = id,
                orderId = params.orderId,
                orderLineId = params.orderLineId,
                type = params.kind.name,
                value = params.value,
                computedMinor = computed.minor,
                reason = params.reason,
                authorisedByStaffId = params.authorisedByStaffId,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceIds.deviceId(),
            ),
        )
        changeLog.record("discount", id, ChangeOp.INSERT, now)
        auditLog.record(params.authorisedByStaffId, "APPLY_DISCOUNT", "discount", id, at = now, reason = params.reason)
        recomputeTotals(params.orderId, now)
        id
    }

    override suspend fun removeDiscount(discountId: String, actorStaffId: String) = db.withTransaction {
        val existing = discountDao.getById(discountId) ?: return@withTransaction
        val now = time.nowMillis()
        discountDao.softDelete(discountId, now)
        changeLog.record("discount", discountId, ChangeOp.DELETE, now)
        auditLog.record(actorStaffId, "REMOVE_DISCOUNT", "discount", discountId, at = now)
        recomputeTotals(existing.orderId, now)
    }

    override suspend fun recordPayment(params: RecordPaymentParams): String = db.withTransaction {
        val now = time.nowMillis()
        val id = UuidV7.generate()
        val change = (params.tenderedMinor - params.amountMinor).coerceAtLeast(0L)
        paymentDao.insert(
            PaymentEntity(
                id = id,
                orderId = params.orderId,
                method = params.method.name,
                amountMinor = params.amountMinor,
                tenderedMinor = params.tenderedMinor,
                changeMinor = change,
                reference = params.reference,
                staffId = params.staffId,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceIds.deviceId(),
            ),
        )
        changeLog.record("payment", id, ChangeOp.INSERT, now)
        id
    }

    override suspend fun isFullySettled(orderId: String): Boolean {
        val order = orderDao.getById(orderId) ?: return false
        return paymentDao.totalPaidMinor(orderId) >= order.totalMinor
    }

    /** A snapshot amount for the `discount` row (informational — the engine owns the order total). */
    private suspend fun discountSnapshot(params: ApplyDiscountParams): Money {
        val lineId = params.orderLineId
        val base = if (lineId == null) {
            Money(orderDao.getById(params.orderId)?.subtotalMinor ?: 0L)
        } else {
            val line = lineDao.getById(lineId)
            Money((line?.unitPriceSnapshotMinor ?: 0L) * (line?.qty ?: 0))
        }
        return when (params.kind) {
            DiscountKind.PERCENT -> base.percent(params.value.toInt())
            DiscountKind.FIXED -> minOf(Money(params.value), base).coerceAtLeast(Money.ZERO)
        }
    }

    /** Recompute + persist the denormalised totals from the current active lines (§3.5). */
    private suspend fun recomputeTotals(orderId: String, now: Long) {
        val config = storeProfileDao.get()?.toDomain()?.pricingConfig()
            ?: PricingConfig(tax = TaxConfig.NONE)
        val discountRows = discountDao.getForOrder(orderId)
        val lineDiscounts = discountRows.filter { it.orderLineId != null }.groupBy { it.orderLineId!! }
        val billDiscounts = discountRows.filter { it.orderLineId == null }.map { it.toPricingDiscount() }
        val exemptByVariant = mutableMapOf<String, Boolean>()
        val pricingLines = lineDao.getForOrder(orderId)
            .filter { it.state == LINE_ACTIVE }
            .map { row ->
                val exempt = exemptByVariant.getOrPut(row.menuVariantId) {
                    // TODO: snapshot tax_exempt onto order_line (needs a migration); live lookup for now.
                    menuVariantDao.getById(row.menuVariantId)
                        ?.let { menuItemDao.getById(it.menuItemId)?.taxExempt } ?: false
                }
                PricingLine(
                    unitPrice = Money(row.unitPriceSnapshotMinor),
                    quantity = row.qty,
                    modifierDeltas = lineModifierDao.getForLine(row.id).map { Money(it.priceDeltaSnapshotMinor) },
                    taxExempt = exempt,
                    discounts = lineDiscounts[row.id].orEmpty().map { it.toPricingDiscount() },
                )
            }
        val bill = PricingEngine.price(
            PricingRequest(lines = pricingLines, billDiscounts = billDiscounts, config = config),
        )
        orderDao.updateTotals(
            id = orderId,
            subtotal = bill.subtotal.minor,
            discount = bill.discountTotal.minor,
            serviceCharge = bill.serviceCharge.minor,
            tax = bill.tax.minor,
            rounding = bill.roundingAdjustment.minor,
            total = bill.total.minor,
            updatedAt = now,
        )
    }
}
