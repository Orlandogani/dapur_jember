package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.inventory.Ingredient
import com.leanecorps.dapurjember.core.domain.inventory.InventoryRepository
import com.leanecorps.dapurjember.core.domain.inventory.RecipeLine
import com.leanecorps.dapurjember.core.domain.inventory.RecipeLineWithIngredient
import com.leanecorps.dapurjember.core.domain.inventory.StockAdjustment
import com.leanecorps.dapurjember.core.domain.inventory.StockMovement
import com.leanecorps.dapurjember.core.domain.inventory.StockReason
import com.leanecorps.dapurjember.core.domain.inventory.weightedAverageAfterPurchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeInventoryRepository : InventoryRepository {

    private val ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    private val movements = MutableStateFlow<List<StockMovement>>(emptyList())
    private val recipes = MutableStateFlow<List<RecipeLine>>(emptyList())

    override fun observeIngredients(): Flow<List<Ingredient>> = ingredients

    override fun observeLowStock(): Flow<List<Ingredient>> = ingredients.map { list -> list.filter { it.isLowStock } }

    override fun observeMovements(ingredientId: String): Flow<List<StockMovement>> =
        movements.map { list -> list.filter { it.ingredientId == ingredientId } }

    override suspend fun getIngredient(id: String): Ingredient? = ingredients.value.firstOrNull { it.id == id }

    override suspend fun upsertIngredient(ingredient: Ingredient) =
        ingredients.update { it.filterNot { existing -> existing.id == ingredient.id } + ingredient }

    override suspend fun softDeleteIngredient(id: String) =
        ingredients.update { it.filterNot { existing -> existing.id == id } }

    override suspend fun adjustStock(adjustment: StockAdjustment) {
        val ingredient = getIngredient(adjustment.ingredientId) ?: return
        val newAvg = if (adjustment.reason == StockReason.PURCHASE) {
            weightedAverageAfterPurchase(
                ingredient.currentStockBase,
                ingredient.avgCostPerBase,
                adjustment.qtyBaseDelta,
                adjustment.unitCost,
            )
        } else {
            ingredient.avgCostPerBase
        }
        ingredients.update { list ->
            list.map {
                if (it.id == ingredient.id) {
                    it.copy(currentStockBase = it.currentStockBase + adjustment.qtyBaseDelta, avgCostPerBase = newAvg)
                } else {
                    it
                }
            }
        }
        movements.update {
            it + StockMovement(
                id = UuidV7.generate(),
                ingredientId = adjustment.ingredientId,
                qtyBaseDelta = adjustment.qtyBaseDelta,
                reason = adjustment.reason,
                orderLineId = null,
                unitCost = adjustment.unitCost,
                createdAt = 0,
            )
        }
    }

    override fun observeRecipe(menuVariantId: String): Flow<List<RecipeLineWithIngredient>> =
        combine(recipes, ingredients) { lines, all -> joinRecipe(lines, all, menuVariantId) }

    override suspend fun getRecipe(menuVariantId: String): List<RecipeLineWithIngredient> =
        joinRecipe(recipes.value, ingredients.value, menuVariantId)

    override suspend fun saveRecipe(menuVariantId: String, lines: List<RecipeLine>) = recipes.update { current ->
        current.filterNot { it.menuVariantId == menuVariantId } + lines.map { it.copy(menuVariantId = menuVariantId) }
    }

    override suspend fun costOfVariant(menuVariantId: String): Money =
        getRecipe(menuVariantId).fold(Money.ZERO) { acc, line -> acc + line.cost }

    private fun joinRecipe(
        lines: List<RecipeLine>,
        all: List<Ingredient>,
        menuVariantId: String,
    ): List<RecipeLineWithIngredient> {
        val byId = all.associateBy { it.id }
        return lines.filter { it.menuVariantId == menuVariantId }
            .mapNotNull { line -> byId[line.ingredientId]?.let { RecipeLineWithIngredient(line, it) } }
    }
}
