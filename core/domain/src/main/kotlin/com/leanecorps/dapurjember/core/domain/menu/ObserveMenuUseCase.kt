package com.leanecorps.dapurjember.core.domain.menu

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** A category together with its live menu items. */
data class MenuSection(
    val category: Category,
    val items: List<MenuItem>,
)

/** Emits the full menu grouped by category, reacting to any change to either. */
class ObserveMenuUseCase @Inject constructor(
    private val repository: MenuRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<MenuSection>> =
        repository.observeCategories().flatMapLatest { categories ->
            if (categories.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    categories.map { category ->
                        repository.observeItems(category.id).map { items -> MenuSection(category, items) }
                    },
                ) { sections -> sections.toList() }
            }
        }
}
