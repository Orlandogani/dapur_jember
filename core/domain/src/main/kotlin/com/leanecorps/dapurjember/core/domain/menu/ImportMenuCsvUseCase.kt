package com.leanecorps.dapurjember.core.domain.menu

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Bulk-loads a menu from CSV (FR-M6 — "so a 200-item menu isn't typed in on a tablet").
 * Categories are matched by name (case-insensitive) and reused; every CSV run creates fresh
 * items, so this is an import, not a sync — re-running duplicates. Prices use the store's
 * currency scale.
 */
class ImportMenuCsvUseCase @Inject constructor(
    private val menu: MenuRepository,
    private val storeProfiles: StoreProfileRepository,
) {

    data class Summary(val categoriesAdded: Int, val itemsImported: Int, val errors: List<MenuCsvError>)

    suspend operator fun invoke(csvText: String): Summary {
        val minorUnits = storeProfiles.getProfile()?.currencyMinorUnits ?: 0
        val parsed = MenuCsv.parse(csvText, minorUnits)
        if (!parsed.isUsable) return Summary(categoriesAdded = 0, itemsImported = 0, errors = parsed.errors)

        val categoriesByName = menu.observeCategories().first()
            .associateBy { it.name.lowercase() }
            .toMutableMap()

        var categoriesAdded = 0
        parsed.categories.forEach { name ->
            if (name.lowercase() !in categoriesByName) {
                val category = Category(id = UuidV7.generate(), name = name, sortOrder = categoriesByName.size)
                menu.upsertCategory(category)
                categoriesByName[name.lowercase()] = category
                categoriesAdded++
            }
        }

        val rowsByItem = parsed.rows.groupBy { it.category.lowercase() to it.item }
        rowsByItem.forEach { (key, rows) ->
            val itemId = UuidV7.generate()
            menu.saveItemWithVariants(
                item = MenuItem(
                    id = itemId,
                    categoryId = categoriesByName.getValue(key.first).id,
                    name = key.second,
                    available = rows.all { it.available },
                ),
                variants = rows.mapIndexed { index, row ->
                    MenuVariant(
                        id = UuidV7.generate(),
                        menuItemId = itemId,
                        name = row.variant,
                        price = Money(row.priceMinor),
                        sortOrder = index,
                    )
                },
            )
        }

        return Summary(
            categoriesAdded = categoriesAdded,
            itemsImported = rowsByItem.size,
            errors = parsed.errors,
        )
    }
}
