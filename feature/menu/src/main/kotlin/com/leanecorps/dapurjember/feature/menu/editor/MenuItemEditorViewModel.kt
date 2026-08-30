package com.leanecorps.dapurjember.feature.menu.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.inventory.InventoryRepository
import com.leanecorps.dapurjember.core.domain.inventory.RecipeLine
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
    private val inventory: InventoryRepository,
    storeProfiles: StoreProfileRepository,
) : ViewModel() {

    private val itemId: String? = savedStateHandle.get<String>(MENU_ITEM_ID_ARG)?.takeIf { it.isNotBlank() }

    private val draft = MutableStateFlow<MenuItemDraft?>(null)
    private val recipe = MutableStateFlow<RecipeEditorUi?>(null)
    private val done = MutableStateFlow(false)
    private val minorUnits = MutableStateFlow(0)

    private val editorInputs = combine(draft, recipe, done, minorUnits) { d, r, isDone, scale ->
        EditorInputs(d, r, isDone, scale)
    }

    val uiState: StateFlow<MenuItemEditorState> = combine(
        menuRepository.observeCategories(),
        menuRepository.observeModifierGroups(),
        inventory.observeIngredients(),
        editorInputs,
    ) { categories, groups, ingredients, inputs ->
        MenuItemEditorState(
            loading = inputs.draft == null,
            isNew = itemId == null,
            categories = categories.map { CategoryOption(it.id, it.name) },
            modifierGroups = groups.map { ModifierGroupOption(it.id, it.name) },
            ingredients = ingredients.map { IngredientOption(it.id, it.name, it.baseUnit.name.lowercase()) },
            currencyMinorUnits = inputs.minorUnits,
            draft = inputs.draft ?: MenuItemDraft(),
            recipe = inputs.recipe,
            done = inputs.done,
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

    // --- Recipe (FR-I2) ---

    /** Opens the recipe sheet for a variant. Only meaningful once the item has been saved. */
    fun openRecipe(variantId: String) {
        val variant = draft.value?.variants?.firstOrNull { it.id == variantId } ?: return
        viewModelScope.launch {
            val existing = inventory.getRecipe(variantId)
            recipe.value = RecipeEditorUi(
                variantId = variantId,
                variantName = variant.name,
                rows = existing.map { RecipeRowDraft(it.line.ingredientId, it.line.qtyBase.toString()) }
                    .ifEmpty { listOf(RecipeRowDraft()) },
                costMinor = inventory.costOfVariant(variantId).minor,
            )
        }
    }

    fun closeRecipe() {
        recipe.value = null
    }

    fun addRecipeRow() = recipe.update { it.copy(rows = it.rows + RecipeRowDraft()) }

    fun removeRecipeRow(index: Int) = recipe.update { current ->
        current.copy(rows = current.rows.filterIndexed { i, _ -> i != index }.ifEmpty { listOf(RecipeRowDraft()) })
    }

    fun editRecipeRow(index: Int, transform: (RecipeRowDraft) -> RecipeRowDraft) = recipe.update { current ->
        current.copy(rows = current.rows.mapIndexed { i, row -> if (i == index) transform(row) else row })
    }

    fun saveRecipe() {
        val current = recipe.value ?: return
        if (!current.canSave) return
        viewModelScope.launch {
            inventory.saveRecipe(
                menuVariantId = current.variantId,
                lines = current.rows
                    .filter { it.ingredientId.isNotBlank() && it.qty != null }
                    .map {
                        RecipeLine(
                            id = UuidV7.generate(),
                            menuVariantId = current.variantId,
                            ingredientId = it.ingredientId,
                            qtyBase = it.qty ?: 0.0,
                        )
                    },
            )
            recipe.value = null
        }
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

    private inline fun MutableStateFlow<RecipeEditorUi?>.update(transform: (RecipeEditorUi) -> RecipeEditorUi) {
        value = value?.let(transform)
    }

    private data class EditorInputs(
        val draft: MenuItemDraft?,
        val recipe: RecipeEditorUi?,
        val done: Boolean,
        val minorUnits: Int,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
