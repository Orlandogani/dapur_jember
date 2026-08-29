package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuBoardItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItemWithVariants
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [MenuRepository] for ViewModel/use-case tests. Not thread-safe; not transactional. */
class FakeMenuRepository : MenuRepository {

    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val items = MutableStateFlow<List<MenuItem>>(emptyList())
    private val variants = MutableStateFlow<List<MenuVariant>>(emptyList())

    override fun observeCategories(): Flow<List<Category>> = categories

    override fun observeItems(categoryId: String): Flow<List<MenuItem>> =
        items.map { list -> list.filter { it.categoryId == categoryId } }

    override fun observeItemWithVariants(itemId: String): Flow<MenuItemWithVariants?> =
        items.map { list ->
            list.firstOrNull { it.id == itemId }?.let { item ->
                MenuItemWithVariants(item, variants.value.filter { it.menuItemId == itemId })
            }
        }

    override fun observeMenuBoard(categoryId: String): Flow<List<MenuBoardItem>> =
        items.map { list ->
            list.filter { it.categoryId == categoryId }.map { item ->
                MenuBoardItem(item, variants.value.filter { it.menuItemId == item.id })
            }
        }

    override suspend fun getItem(itemId: String): MenuItem? = items.value.firstOrNull { it.id == itemId }

    override suspend fun upsertCategory(category: Category) =
        categories.update { it.filterNot { c -> c.id == category.id } + category }

    override suspend fun upsertItem(item: MenuItem) =
        items.update { it.filterNot { i -> i.id == item.id } + item }

    override suspend fun upsertVariant(variant: MenuVariant) =
        variants.update { it.filterNot { v -> v.id == variant.id } + variant }

    override suspend fun setItemAvailability(itemId: String, available: Boolean) =
        items.update { list -> list.map { if (it.id == itemId) it.copy(available = available) else it } }

    override suspend fun softDeleteCategory(categoryId: String) =
        categories.update { it.filterNot { c -> c.id == categoryId } }

    override suspend fun softDeleteItem(itemId: String) =
        items.update { it.filterNot { i -> i.id == itemId } }

    override suspend fun softDeleteVariant(variantId: String) =
        variants.update { it.filterNot { v -> v.id == variantId } }
}
