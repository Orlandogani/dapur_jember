package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuBoardItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItemWithVariants
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.domain.menu.Modifier
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroup
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroupWithModifiers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [MenuRepository] for ViewModel/use-case tests. Not thread-safe; not transactional. */
class FakeMenuRepository : MenuRepository {

    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val items = MutableStateFlow<List<MenuItem>>(emptyList())
    private val variants = MutableStateFlow<List<MenuVariant>>(emptyList())
    private val groups = MutableStateFlow<List<ModifierGroup>>(emptyList())
    private val modifiers = MutableStateFlow<List<Modifier>>(emptyList())

    /** itemId -> ordered list of groupIds. */
    private val itemGroups = MutableStateFlow<Map<String, List<String>>>(emptyMap())

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

    override suspend fun saveItemWithVariants(item: MenuItem, newVariants: List<MenuVariant>) {
        items.update { it.filterNot { i -> i.id == item.id } + item }
        val keep = newVariants.map { it.id }.toSet()
        variants.update { current ->
            current.filterNot { it.menuItemId == item.id && it.id !in keep }
                .filterNot { it.id in keep } + newVariants
        }
    }

    override suspend fun setItemAvailability(itemId: String, available: Boolean) =
        items.update { list -> list.map { if (it.id == itemId) it.copy(available = available) else it } }

    override fun observeModifierGroups(): Flow<List<ModifierGroup>> = groups

    override fun observeModifierGroup(groupId: String): Flow<ModifierGroupWithModifiers?> =
        combine(groups, modifiers) { gs, ms ->
            gs.firstOrNull { it.id == groupId }
                ?.let { ModifierGroupWithModifiers(it, ms.filter { m -> m.modifierGroupId == groupId }) }
        }

    override suspend fun saveModifierGroup(group: ModifierGroup, newModifiers: List<Modifier>) {
        groups.update { it.filterNot { g -> g.id == group.id } + group }
        modifiers.update { current ->
            current.filterNot { it.modifierGroupId == group.id } +
                newModifiers.map { it.copy(modifierGroupId = group.id) }
        }
    }

    override suspend fun softDeleteModifierGroup(groupId: String) {
        groups.update { it.filterNot { g -> g.id == groupId } }
        itemGroups.update { map -> map.mapValues { (_, ids) -> ids.filterNot { it == groupId } } }
    }

    override fun observeItemModifierGroups(itemId: String): Flow<List<ModifierGroupWithModifiers>> =
        combine(groups, modifiers, itemGroups) { gs, ms, links ->
            (links[itemId].orEmpty()).mapNotNull { groupId ->
                gs.firstOrNull { it.id == groupId }?.let { group ->
                    ModifierGroupWithModifiers(group, ms.filter { it.modifierGroupId == groupId })
                }
            }
        }

    override suspend fun setItemModifierGroups(itemId: String, groupIds: List<String>) =
        itemGroups.update { it + (itemId to groupIds) }

    override suspend fun softDeleteCategory(categoryId: String) =
        categories.update { it.filterNot { c -> c.id == categoryId } }

    override suspend fun softDeleteItem(itemId: String) =
        items.update { it.filterNot { i -> i.id == itemId } }

    override suspend fun softDeleteVariant(variantId: String) =
        variants.update { it.filterNot { v -> v.id == variantId } }
}
