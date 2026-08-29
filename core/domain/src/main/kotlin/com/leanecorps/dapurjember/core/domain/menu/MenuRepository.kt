package com.leanecorps.dapurjember.core.domain.menu

import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the menu. Implemented in `:core:data` over the Room DAOs; every mutation
 * writes a `change_log` row in the same transaction (CLAUDE.md rule 5) and stamps the sync
 * envelope. All reads exclude soft-deleted rows.
 */
interface MenuRepository {

    fun observeCategories(): Flow<List<Category>>

    fun observeItems(categoryId: String): Flow<List<MenuItem>>

    fun observeItemWithVariants(itemId: String): Flow<MenuItemWithVariants?>

    /** The orderable tiles for a category — items with their variants (order screen grid). */
    fun observeMenuBoard(categoryId: String): Flow<List<MenuBoardItem>>

    suspend fun getItem(itemId: String): MenuItem?

    suspend fun upsertCategory(category: Category)

    suspend fun upsertItem(item: MenuItem)

    suspend fun upsertVariant(variant: MenuVariant)

    /**
     * Saves an item and its full variant list in one transaction (S14 item editor): variants
     * not in [variants] are soft-deleted. FR-M4 — an item always keeps at least one variant.
     */
    suspend fun saveItemWithVariants(item: MenuItem, variants: List<MenuVariant>)

    /** FR-M2 — flip the sold-out toggle in one tap from the order screen. */
    suspend fun setItemAvailability(itemId: String, available: Boolean)

    // --- Modifier groups (FR-M3) ---

    fun observeModifierGroups(): Flow<List<ModifierGroup>>

    fun observeModifierGroup(groupId: String): Flow<ModifierGroupWithModifiers?>

    /** Saves a group and its full modifier list in one transaction; dropped modifiers are soft-deleted. */
    suspend fun saveModifierGroup(group: ModifierGroup, modifiers: List<Modifier>)

    suspend fun softDeleteModifierGroup(groupId: String)

    /** The modifier groups attached to [itemId], in their configured order, each with its modifiers. */
    fun observeItemModifierGroups(itemId: String): Flow<List<ModifierGroupWithModifiers>>

    /** Reconciles the item↔group links so exactly [groupIds] are attached, in that order. */
    suspend fun setItemModifierGroups(itemId: String, groupIds: List<String>)

    suspend fun softDeleteCategory(categoryId: String)

    suspend fun softDeleteItem(itemId: String)

    suspend fun softDeleteVariant(variantId: String)
}
