package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * One ingredient line of a variant's recipe (FR-I2). Recipes attach to the **variant**, not
 * the item, because a Large portion uses more chicken (FR-M4). `qtyBase` is in the
 * ingredient's base unit.
 */
@Entity(
    tableName = "recipe_line",
    foreignKeys = [
        ForeignKey(
            entity = MenuVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["menu_variant_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredient_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("menu_variant_id"),
        Index("ingredient_id"),
        Index(value = ["menu_variant_id", "ingredient_id"], unique = true),
    ],
)
data class RecipeLineEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("menu_variant_id") val menuVariantId: String,
    @ColumnInfo("ingredient_id") val ingredientId: String,
    @ColumnInfo("qty_base") val qtyBase: Double,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
