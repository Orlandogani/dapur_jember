package com.leanecorps.dapurjember.core.domain.inventory

import com.leanecorps.dapurjember.core.common.money.Money
import kotlinx.coroutines.flow.Flow

enum class BaseUnit { G, ML, PIECE }

/** Why stock moved (`docs/3-data-model` §3.7). SALE rows are written by the payment transaction. */
enum class StockReason { SALE, PURCHASE, WASTE, SPOILAGE, STAFF_MEAL, COUNT_CORRECTION, OPENING }

/**
 * A stock item. Everything internal is in [baseUnit]; [purchaseUnit]/[purchaseToBaseFactor]
 * exist only for data entry. [avgCostPerBase] is weighted-average (FR-I5). Negative stock is
 * allowed but flagged (FR-I8).
 */
data class Ingredient(
    val id: String,
    val name: String,
    val baseUnit: BaseUnit,
    val purchaseUnit: String,
    val purchaseToBaseFactor: Double,
    val currentStockBase: Double,
    val avgCostPerBase: Money,
    val lowStockThresholdBase: Double,
    val supplierId: String? = null,
) {
    val isLowStock: Boolean get() = currentStockBase <= lowStockThresholdBase
}

data class StockMovement(
    val id: String,
    val ingredientId: String,
    val qtyBaseDelta: Double,
    val reason: StockReason,
    val orderLineId: String?,
    val unitCost: Money,
    val createdAt: Long,
)

/**
 * One ingredient line of a variant's recipe (FR-I2). Recipes attach to the **variant**, not
 * the item, because a Large portion uses more chicken (FR-M4). [qtyBase] is in the
 * ingredient's base unit.
 */
data class RecipeLine(
    val id: String,
    val menuVariantId: String,
    val ingredientId: String,
    val qtyBase: Double,
)

/** A recipe line joined to its ingredient — what the editor and the cost calculation need. */
data class RecipeLineWithIngredient(
    val line: RecipeLine,
    val ingredient: Ingredient,
) {
    /** What this line contributes to the dish's cost at the ingredient's current average cost. */
    val cost: Money get() = Money((line.qtyBase * ingredient.avgCostPerBase.minor).toLong())
}

data class StockAdjustment(
    val ingredientId: String,
    /** Positive adds to stock, negative removes. */
    val qtyBaseDelta: Double,
    val reason: StockReason,
    val staffId: String,
    /** Purchase price per base unit; only used (and only rolls the average) when [reason] is PURCHASE. */
    val unitCost: Money = Money.ZERO,
)

interface InventoryRepository {

    fun observeIngredients(): Flow<List<Ingredient>>

    fun observeLowStock(): Flow<List<Ingredient>>

    fun observeMovements(ingredientId: String): Flow<List<StockMovement>>

    suspend fun getIngredient(id: String): Ingredient?

    suspend fun upsertIngredient(ingredient: Ingredient)

    suspend fun softDeleteIngredient(id: String)

    /**
     * Applies a manual stock adjustment (FR-I6): one `stock_movement` row + the new
     * `current_stock_base` + an `audit_log` row, all in one transaction. A PURCHASE also
     * rolls the weighted-average cost.
     */
    suspend fun adjustStock(adjustment: StockAdjustment)

    // --- Recipes (FR-I2) ---

    fun observeRecipe(menuVariantId: String): Flow<List<RecipeLineWithIngredient>>

    suspend fun getRecipe(menuVariantId: String): List<RecipeLineWithIngredient>

    /** Replaces the variant's recipe in one transaction; lines not in [lines] are soft-deleted. */
    suspend fun saveRecipe(menuVariantId: String, lines: List<RecipeLine>)

    /**
     * Cost per dish = Σ (ingredient qty × ingredient average cost) (FR-I4). Zero for a
     * variant with no recipe — food cost is opt-in per item (Risk R8).
     */
    suspend fun costOfVariant(menuVariantId: String): Money
}
