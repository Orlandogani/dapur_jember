package com.leanecorps.dapurjember.feature.menu.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MENU_ITEM_ID_ARG = "itemId"

@HiltViewModel
class MenuItemEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val menuRepository: MenuRepository,
    storeProfiles: StoreProfileRepository,
) : ViewModel() {

    private val itemId: String? = savedStateHandle.get<String>(MENU_ITEM_ID_ARG)?.takeIf { it.isNotBlank() }

    private val draft = MutableStateFlow<MenuItemDraft?>(null)
    private val done = MutableStateFlow(false)
    private val minorUnits = MutableStateFlow(0)

    val uiState: StateFlow<MenuItemEditorState> = combine(
        menuRepository.observeCategories(),
        menuRepository.observeModifierGroups(),
        draft,
        done,
        minorUnits,
    ) { categories, groups, draftState, isDone, scale ->
        MenuItemEditorState(
            loading = draftState == null,
            isNew = itemId == null,
            categories = categories.map { CategoryOption(it.id, it.name) },
            modifierGroups = groups.map { ModifierGroupOption(it.id, it.name) },
            currencyMinorUnits = scale,
            draft = draftState ?: MenuItemDraft(),
            done = isDone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MenuItemEditorState())

    init {
        viewModelScope.launch {
            val scale = storeProfiles.getProfile()?.currencyMinorUnits ?: 0
            minorUnits.value = scale
            val firstCategory = menuRepository.observeCategories().first().firstOrNull()?.id.orEmpty()
            draft.value = if (itemId == null) {
                MenuItemDraft(categoryId = firstCategory)
            } else {
                val detail = menuRepository.observeItemWithVariants(itemId).first()
                val attachedIds = menuRepository.observeItemModifierGroups(itemId).first().map { it.group.id }
                detail?.toDraft(scale)?.copy(modifierGroupIds = attachedIds)
                    ?: MenuItemDraft(categoryId = firstCategory)
            }
        }
    }

    fun edit(transform: (MenuItemDraft) -> MenuItemDraft) {
        draft.value = draft.value?.let(transform)
    }

    fun addVariant() = edit { it.copy(variants = it.variants + VariantDraft()) }

    fun removeVariant(id: String) = edit { current ->
        if (current.variants.size <= 1) current else current.copy(variants = current.variants.filterNot { it.id == id })
    }

    fun editVariant(id: String, transform: (VariantDraft) -> VariantDraft) = edit { current ->
        current.copy(variants = current.variants.map { if (it.id == id) transform(it) else it })
    }

    fun toggleModifierGroup(groupId: String) = edit { current ->
        current.copy(
            modifierGroupIds = if (groupId in current.modifierGroupIds) {
                current.modifierGroupIds - groupId
            } else {
                current.modifierGroupIds + groupId
            },
        )
    }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        val current = draft.value ?: return
        viewModelScope.launch {
            val newItemId = current.id ?: UuidV7.generate()
            menuRepository.saveItemWithVariants(
                item = current.toItem(newItemId),
                variants = current.toVariants(newItemId, state.currencyMinorUnits),
            )
            menuRepository.setItemModifierGroups(newItemId, current.modifierGroupIds)
            done.value = true
        }
    }

    fun delete() {
        val existingId = itemId ?: return
        viewModelScope.launch {
            menuRepository.softDeleteItem(existingId)
            done.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
