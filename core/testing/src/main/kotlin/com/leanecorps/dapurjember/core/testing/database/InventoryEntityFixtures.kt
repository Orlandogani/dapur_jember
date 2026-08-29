package com.leanecorps.dapurjember.core.testing.database

import com.leanecorps.dapurjember.core.data.database.entity.AuditLogEntity
import com.leanecorps.dapurjember.core.data.database.entity.IngredientEntity
import com.leanecorps.dapurjember.core.data.database.entity.RecipeLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.StockMovementEntity
import com.leanecorps.dapurjember.core.data.database.entity.SupplierEntity

/** Test-data builders for the audit-log + inventory entities (schema v4). */
object InventoryEntityFixtures {

    private const val DEVICE = "test-device"

    fun supplier(id: String = "sup-1", name: String = "Pasar Induk", deletedAt: Long? = null) = SupplierEntity(
        id = id,
        name = name,
        phone = null,
        note = null,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun ingredient(
        id: String = "ing-1",
        name: String = "Chicken",
        currentStockBase: Double = 5_000.0,
        lowStockThresholdBase: Double = 1_000.0,
        supplierId: String? = null,
        deletedAt: Long? = null,
    ) = IngredientEntity(
        id = id,
        name = name,
        baseUnit = "G",
        purchaseUnit = "kg",
        purchaseToBaseFactor = 1_000.0,
        currentStockBase = currentStockBase,
        avgCostPerBaseMinor = 5,
        lowStockThresholdBase = lowStockThresholdBase,
        supplierId = supplierId,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun recipeLine(
        id: String = "rl-1",
        menuVariantId: String = "var-1",
        ingredientId: String = "ing-1",
        qtyBase: Double = 150.0,
        deletedAt: Long? = null,
    ) = RecipeLineEntity(
        id = id,
        menuVariantId = menuVariantId,
        ingredientId = ingredientId,
        qtyBase = qtyBase,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun stockMovement(
        id: String = "sm-1",
        ingredientId: String = "ing-1",
        qtyBaseDelta: Double = -150.0,
        reason: String = "SALE",
        staffId: String = "staff-1",
        orderLineId: String? = null,
        createdAt: Long = 1L,
    ) = StockMovementEntity(
        id = id,
        ingredientId = ingredientId,
        qtyBaseDelta = qtyBaseDelta,
        reason = reason,
        orderLineId = orderLineId,
        unitCostMinor = 5,
        staffId = staffId,
        createdAt = createdAt,
        updatedAt = createdAt,
        deletedAt = null,
        deviceId = DEVICE,
    )

    fun auditLog(
        id: String = "al-1",
        actorStaffId: String = "staff-1",
        action: String = "VOID_LINE",
        entityType: String = "order_line",
        entityId: String = "line-1",
        createdAt: Long = 1L,
    ) = AuditLogEntity(
        id = id,
        actorStaffId = actorStaffId,
        action = action,
        entityType = entityType,
        entityId = entityId,
        beforeJson = null,
        afterJson = null,
        reason = "quality issue",
        createdAt = createdAt,
    )
}
