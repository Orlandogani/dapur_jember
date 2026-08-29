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

    /** FR-M2 — flip the sold-out toggle in one tap from the order screen. */
    suspend fun setItemAvailability(itemId: String, available: Boolean)

    suspend fun softDeleteCategory(categoryId: String)

    suspend fun softDeleteItem(itemId: String)

    suspend fun softDeleteVariant(variantId: String)
}
