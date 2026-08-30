package com.leanecorps.dapurjember.core.data.inventory

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.dao.IngredientDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderLineDao
import com.leanecorps.dapurjember.core.data.database.dao.RecipeLineDao
import com.leanecorps.dapurjember.core.data.database.dao.StockMovementDao
import com.leanecorps.dapurjember.core.data.database.entity.StockMovementEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.inventory.StockReason
import javax.inject.Inject

private const val LINE_ACTIVE = "ACTIVE"

/**
 * Deducts recipe quantities from stock when an order is paid (FR-I3). Deliberately on
 * **payment**, not send-to-kitchen: a sent-then-voided order would otherwise leak stock
 * (architecture §5.3).
 *
 * Called by `OrderRepositoryImpl` from *inside* the same transaction that moves the order to
 * PAID, so stock and the sale commit together. Every deduction writes a `stock_movement` row
 * referencing its `order_line_id`, which is what makes any stock level explainable. Stock is
 * allowed to go negative (FR-I8) — blocking a sale over bad data is worse than bad data.
 *
 * Idempotent: a line that already has SALE movements is skipped, so re-entering PAID cannot
 * double-deduct.
 */
@Suppress("LongParameterList")
internal class StockDeductor @Inject constructor(
    private val orderLineDao: OrderLineDao,
    private val recipeLineDao: RecipeLineDao,
    private val ingredientDao: IngredientDao,
    private val stockMovementDao: StockMovementDao,
    private val changeLog: ChangeLogRecorder,
    private val deviceIds: DeviceIdProvider,
) {

    suspend fun deductForOrder(orderId: String, staffId: String, now: Long) {
        val lines = orderLineDao.getForOrder(orderId).filter { it.state == LINE_ACTIVE }
        for (line in lines) {
            if (stockMovementDao.getForOrderLine(line.id).isNotEmpty()) continue

            val recipe = recipeLineDao.getForVariant(line.menuVariantId)
            for (recipeLine in recipe) {
                val ingredient = ingredientDao.getById(recipeLine.ingredientId) ?: continue
                val delta = -(recipeLine.qtyBase * line.qty)

                // A sale consumes stock at the current average; it never moves the average itself.
                ingredientDao.updateStock(
                    id = ingredient.id,
                    stockBase = ingredient.currentStockBase + delta,
                    avgCostMinor = ingredient.avgCostPerBaseMinor,
                    updatedAt = now,
                )

                val movementId = UuidV7.generate()
                stockMovementDao.insert(
                    StockMovementEntity(
                        id = movementId,
                        ingredientId = ingredient.id,
                        qtyBaseDelta = delta,
                        reason = StockReason.SALE.name,
                        orderLineId = line.id,
                        unitCostMinor = ingredient.avgCostPerBaseMinor,
                        staffId = staffId,
                        createdAt = now,
                        updatedAt = now,
                        deviceId = deviceIds.deviceId(),
                    ),
                )
                changeLog.record("stock_movement", movementId, ChangeOp.INSERT, now)
                changeLog.record("ingredient", ingredient.id, ChangeOp.UPDATE, now)
            }
        }
    }
}
