package com.leanecorps.dapurjember.feature.menu

import com.leanecorps.dapurjember.core.domain.auth.Permission
import com.leanecorps.dapurjember.core.domain.menu.MenuSection

data class MenuUiState(
    val sections: List<MenuSectionUi> = emptyList(),
    val loading: Boolean = true,
    /**
     * Whether the signed-in staff holds [Permission.MANAGE_MENU]. False hides every editing
     * entry point; the sold-out toggle stays available to everyone, because marking a dish
     * unavailable is a floor action mid-service, not a menu edit (FR-M2).
     */
    val canManage: Boolean = false,
) {
    val isEmpty: Boolean get() = !loading && sections.all { it.items.isEmpty() }
}

data class MenuSectionUi(
    val id: String,
    val name: String,
    val items: List<MenuItemUi>,
)

data class MenuItemUi(
    val id: String,
    val name: String,
    val available: Boolean,
)

internal fun MenuSection.toUi() = MenuSectionUi(
    id = category.id,
    name = category.name,
    items = items.map { MenuItemUi(id = it.id, name = it.name, available = it.available) },
)
