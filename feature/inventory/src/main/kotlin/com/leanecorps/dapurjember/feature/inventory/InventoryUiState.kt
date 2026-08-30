package com.leanecorps.dapurjember.feature.inventory

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.inventory.BaseUnit
import com.leanecorps.dapurjember.core.domain.inventory.Ingredient
import com.leanecorps.dapurjember.core.domain.inventory.StockAdjustment
import com.leanecorps.dapurjember.core.domain.inventory.StockReason
import java.math.BigDecimal
import java.math.RoundingMode

data class InventoryUiState(
    val loading: Boolean = true,
    /** ADJUST_STOCK — a waiter may look at stock levels but not change them. */
    val canAdjust: Boolean = false,
    val currencyMinorUnits: Int = 0,
    val ingredients: List<IngredientRowUi> = emptyList(),
    val editor: IngredientDraft? = null,
    val adjust: AdjustDraft? = null,
)

data class IngredientRowUi(
    val id: String,
    val name: String,
    val stockLabel: String,
    val lowStock: Boolean,
)

data class IngredientDraft(
    val id: String? = null,
    val name: String = "",
    val baseUnit: BaseUnit = BaseUnit.G,
    val purchaseUnit: String = "",
    val purchaseToBaseFactorText: String = "1",
    val lowStockThresholdText: String = "0",
) {
    val factor: Double? get() = purchaseToBaseFactorText.trim().toDoubleOrNull()?.takeIf { it > 0 }
    val threshold: Double? get() = lowStockThresholdText.trim().toDoubleOrNull()?.takeIf { it >= 0 }
    val isNew: Boolean get() = id == null
    val canSave: Boolean get() = name.isNotBlank() && purchaseUnit.isNotBlank() && factor != null && threshold != null

    fun toIngredient(newId: String) = Ingredient(
        id = id ?: newId,
        name = name.trim(),
        baseUnit = baseUnit,
        purchaseUnit = purchaseUnit.trim(),
        purchaseToBaseFactor = factor ?: 1.0,
        currentStockBase = 0.0,
        avgCostPerBase = Money.ZERO,
        lowStockThresholdBase = threshold ?: 0.0,
    )
}

internal fun Ingredient.toDraft() = IngredientDraft(
    id = id,
    name = name,
    baseUnit = baseUnit,
    purchaseUnit = purchaseUnit,
    purchaseToBaseFactorText = purchaseToBaseFactor.trimZeros(),
    lowStockThresholdText = lowStockThresholdBase.trimZeros(),
)

data class AdjustDraft(
    val ingredientId: String,
    val ingredientName: String,
    val baseUnit: BaseUnit,
    val qtyText: String = "",
    val reason: StockReason = StockReason.PURCHASE,
    val unitCostText: String = "0",
) {
    val qty: Double? get() = qtyText.trim().toDoubleOrNull()?.takeIf { it != 0.0 }
    val isPurchase: Boolean get() = reason == StockReason.PURCHASE
    val canApply: Boolean get() = qty != null

    fun toAdjustment(staffId: String, minorUnits: Int): StockAdjustment = StockAdjustment(
        ingredientId = ingredientId,
        qtyBaseDelta = qty ?: 0.0,
        reason = reason,
        staffId = staffId,
        unitCost = if (isPurchase) {
            Money(
                (unitCostText.trim().replace(',', '.').toBigDecimalOrNull() ?: BigDecimal.ZERO)
                    .movePointRight(minorUnits).setScale(0, RoundingMode.HALF_UP).toLong(),
            )
        } else {
            Money.ZERO
        },
    )
}

internal fun Ingredient.toRowUi() = IngredientRowUi(
    id = id,
    name = name,
    stockLabel = "${currentStockBase.trimZeros()} ${baseUnit.name.lowercase()}",
    lowStock = isLowStock,
)

internal fun newIngredientId(): String = UuidV7.generate()

private fun Double.trimZeros(): String =
    BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
