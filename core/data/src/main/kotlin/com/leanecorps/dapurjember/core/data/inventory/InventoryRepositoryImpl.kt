package com.leanecorps.dapurjember.core.data.inventory

import androidx.room.withTransaction
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.AuditLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.IngredientDao
import com.leanecorps.dapurjember.core.data.database.dao.RecipeLineDao
import com.leanecorps.dapurjember.core.data.database.dao.StockMovementDao
import com.leanecorps.dapurjember.core.data.database.entity.IngredientEntity
import com.leanecorps.dapurjember.core.data.database.entity.RecipeLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.StockMovementEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.inventory.BaseUnit
import com.leanecorps.dapurjember.core.domain.inventory.Ingredient
import com.leanecorps.dapurjember.core.domain.inventory.InventoryRepository
import com.leanecorps.dapurjember.core.domain.inventory.RecipeLine
import com.leanecorps.dapurjember.core.domain.inventory.RecipeLineWithIngredient
import com.leanecorps.dapurjember.core.domain.inventory.StockAdjustment
import com.leanecorps.dapurjember.core.domain.inventory.StockMovement
import com.leanecorps.dapurjember.core.domain.inventory.StockReason
import com.leanecorps.dapurjember.core.domain.inventory.weightedAverageAfterPurchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal fun IngredientEntity.toDomain() = Ingredient(
    id = id,
    name = name,
    baseUnit = runCatching { BaseUnit.valueOf(baseUnit) }.getOrDefault(BaseUnit.G),
    purchaseUnit = purchaseUnit,
    purchaseToBaseFactor = purchaseToBaseFactor,
    currentStockBase = currentStockBase,
    avgCostPerBase = Money(avgCostPerBaseMinor),
    lowStockThresholdBase = lowStockThresholdBase,
    supplierId = supplierId,
)

internal fun Ingredient.toEntity(existing: IngredientEntity?, now: Long, deviceId: String) = IngredientEntity(
    id = id,
    name = name,
    baseUnit = baseUnit.name,
    purchaseUnit = purchaseUnit,
    purchaseToBaseFactor = purchaseToBaseFactor,
    currentStockBase = existing?.currentStockBase ?: currentStockBase,
    avgCostPerBaseMinor = existing?.avgCostPerBaseMinor ?: avgCostPerBase.minor,
    lowStockThresholdBase = lowStockThresholdBase,
    supplierId = supplierId,
    createdAt = existing?.createdAt ?: now,
    updatedAt = now,
    deletedAt = existing?.deletedAt,
    deviceId = existing?.deviceId ?: deviceId,
    revision = (existing?.revision ?: 0) + 1,
)

internal fun RecipeLineEntity.toDomain() = RecipeLine(
    id = id,
    menuVariantId = menuVariantId,
    ingredientId = ingredientId,
    qtyBase = qtyBase,
)

internal fun RecipeLine.toEntity(existing: RecipeLineEntity?, now: Long, deviceId: String) = RecipeLineEntity(
    id = id,
    menuVariantId = menuVariantId,
    ingredientId = ingredientId,
    qtyBase = qtyBase,
    createdAt = existing?.createdAt ?: now,
    updatedAt = now,
    deletedAt = null,
    deviceId = existing?.deviceId ?: deviceId,
    revision = (existing?.revision ?: 0) + 1,
)

internal fun StockMovementEntity.toDomain() = StockMovement(
    id = id,
    ingredientId = ingredientId,
    qtyBaseDelta = qtyBaseDelta,
    reason = runCatching { StockReason.valueOf(reason) }.getOrDefault(StockReason.COUNT_CORRECTION),
    orderLineId = orderLineId,
    unitCost = Money(unitCostMinor),
    createdAt = createdAt,
)

@Suppress("LongParameterList", "TooManyFunctions")
internal class InventoryRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val ingredientDao: IngredientDao,
    private val stockMovementDao: StockMovementDao,
    private val recipeLineDao: RecipeLineDao,
    private val changeLog: ChangeLogRecorder,
    private val auditLog: AuditLogRecorder,
    private val time: TimeProvider,
    private val deviceIds: DeviceIdProvider,
) : InventoryRepository {

    override fun observeIngredients(): Flow<List<Ingredient>> =
        ingredientDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeLowStock(): Flow<List<Ingredient>> =
        ingredientDao.observeLowStock().map { rows -> rows.map { it.toDomain() } }

    override fun observeMovements(ingredientId: String): Flow<List<StockMovement>> =
        stockMovementDao.observeForIngredient(ingredientId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getIngredient(id: String): Ingredient? = ingredientDao.getById(id)?.toDomain()

    override suspend fun upsertIngredient(ingredient: Ingredient) = db.withTransaction {
        val existing = ingredientDao.getById(ingredient.id)
        val now = time.nowMillis()
        ingredientDao.upsert(ingredient.toEntity(existing, now, deviceIds.deviceId()))
        changeLog.record(
            "ingredient",
            ingredient.id,
            if (existing == null) ChangeOp.INSERT else ChangeOp.UPDATE,
            now,
        )
    }

    override suspend fun softDeleteIngredient(id: String) = db.withTransaction {
        val now = time.nowMillis()
        ingredientDao.softDelete(id, now)
        changeLog.record("ingredient", id, ChangeOp.DELETE, now)
    }

    override suspend fun adjustStock(adjustment: StockAdjustment) = db.withTransaction {
        val ingredient = ingredientDao.getById(adjustment.ingredientId) ?: return@withTransaction
        val now = time.nowMillis()

        val newStock = ingredient.currentStockBase + adjustment.qtyBaseDelta
        val newAvgCost = if (adjustment.reason == StockReason.PURCHASE) {
            weightedAverageAfterPurchase(
                currentStockBase = ingredient.currentStockBase,
                currentAvgCostPerBase = Money(ingredient.avgCostPerBaseMinor),
                purchasedQtyBase = adjustment.qtyBaseDelta,
                purchaseUnitCost = adjustment.unitCost,
            ).minor
        } else {
            ingredient.avgCostPerBaseMinor
        }
        ingredientDao.updateStock(adjustment.ingredientId, newStock, newAvgCost, now)

        val movementId = UuidV7.generate()
        stockMovementDao.insert(
            StockMovementEntity(
                id = movementId,
                ingredientId = adjustment.ingredientId,
                qtyBaseDelta = adjustment.qtyBaseDelta,
                reason = adjustment.reason.name,
                orderLineId = null,
                unitCostMinor = adjustment.unitCost.minor,
                staffId = adjustment.staffId,
                createdAt = now,
                updatedAt = now,
                deviceId = deviceIds.deviceId(),
            ),
        )
        changeLog.record("stock_movement", movementId, ChangeOp.INSERT, now)
        changeLog.record("ingredient", adjustment.ingredientId, ChangeOp.UPDATE, now)
        auditLog.record(
            actorStaffId = adjustment.staffId,
            action = "STOCK_ADJUST",
            entityType = "ingredient",
            entityId = adjustment.ingredientId,
            at = now,
            reason = adjustment.reason.name,
        )
    }

    override fun observeRecipe(menuVariantId: String): Flow<List<RecipeLineWithIngredient>> =
        combine(
            recipeLineDao.observeForVariant(menuVariantId),
            ingredientDao.observeAll(),
        ) { lines, ingredients ->
            val byId = ingredients.associateBy { it.id }
            lines.mapNotNull { line ->
                byId[line.ingredientId]?.let { RecipeLineWithIngredient(line.toDomain(), it.toDomain()) }
            }
        }

    override suspend fun getRecipe(menuVariantId: String): List<RecipeLineWithIngredient> =
        recipeLineDao.getForVariant(menuVariantId).mapNotNull { line ->
            ingredientDao.getById(line.ingredientId)?.let {
                RecipeLineWithIngredient(line.toDomain(), it.toDomain())
            }
        }

    override suspend fun saveRecipe(menuVariantId: String, lines: List<RecipeLine>) = db.withTransaction {
        val now = time.nowMillis()
        val device = deviceIds.deviceId()
        val existingByIngredient = recipeLineDao.getForVariant(menuVariantId).associateBy { it.ingredientId }

        lines.forEach { line ->
            val existing = existingByIngredient[line.ingredientId]
            // Reuse the existing row id so the (variant, ingredient) unique index still holds.
            val toSave = line.copy(id = existing?.id ?: line.id, menuVariantId = menuVariantId)
            recipeLineDao.upsert(toSave.toEntity(existing, now, device))
            changeLog.record(
                "recipe_line",
                toSave.id,
                if (existing == null) ChangeOp.INSERT else ChangeOp.UPDATE,
                now,
            )
        }

        val keptIngredients = lines.map { it.ingredientId }.toSet()
        existingByIngredient.values
            .filter { it.ingredientId !in keptIngredients }
            .forEach { removed ->
                recipeLineDao.softDelete(removed.id, now)
                changeLog.record("recipe_line", removed.id, ChangeOp.DELETE, now)
            }
    }

    override suspend fun costOfVariant(menuVariantId: String): Money =
        getRecipe(menuVariantId).fold(Money.ZERO) { acc, line -> acc + line.cost }
}
