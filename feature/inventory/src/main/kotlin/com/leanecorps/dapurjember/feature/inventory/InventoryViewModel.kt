package com.leanecorps.dapurjember.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.Permission
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.inventory.BaseUnit
import com.leanecorps.dapurjember.core.domain.inventory.InventoryRepository
import com.leanecorps.dapurjember.core.domain.inventory.StockReason
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventory: InventoryRepository,
    private val session: SessionRepository,
    private val authorise: AuthoriseUseCase,
    storeProfiles: StoreProfileRepository,
) : ViewModel() {

    private val editor = MutableStateFlow<IngredientDraft?>(null)
    private val adjust = MutableStateFlow<AdjustDraft?>(null)
    private val minorUnits = MutableStateFlow(0)
    private val canAdjust = MutableStateFlow(false)

    val uiState: StateFlow<InventoryUiState> = combine(
        inventory.observeIngredients(),
        editor,
        adjust,
        minorUnits,
        canAdjust,
    ) { ingredients, editorState, adjustState, scale, allowed ->
        InventoryUiState(
            loading = false,
            canAdjust = allowed,
            currencyMinorUnits = scale,
            ingredients = ingredients.map { it.toRowUi() },
            editor = editorState,
            adjust = adjustState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), InventoryUiState())

    init {
        viewModelScope.launch {
            minorUnits.value = storeProfiles.getProfile()?.currencyMinorUnits ?: 0
            canAdjust.value = authorise.currentUserCan(Permission.ADJUST_STOCK)
        }
    }

    fun startAdd() {
        editor.value = IngredientDraft()
    }

    fun startEdit(id: String) {
        viewModelScope.launch { editor.value = inventory.getIngredient(id)?.toDraft() }
    }

    fun editIngredient(transform: (IngredientDraft) -> IngredientDraft) {
        editor.value = editor.value?.let(transform)
    }

    fun closeEditor() {
        editor.value = null
    }

    fun saveIngredient() {
        val draft = editor.value ?: return
        if (!draft.canSave) return
        viewModelScope.launch {
            inventory.upsertIngredient(draft.toIngredient(newIngredientId()))
            editor.value = null
        }
    }

    fun deleteIngredient(id: String) {
        viewModelScope.launch { inventory.softDeleteIngredient(id) }
        editor.value = null
    }

    fun startAdjust(id: String) {
        viewModelScope.launch {
            val ingredient = inventory.getIngredient(id) ?: return@launch
            adjust.value = AdjustDraft(
                ingredientId = ingredient.id,
                ingredientName = ingredient.name,
                baseUnit = ingredient.baseUnit,
            )
        }
    }

    fun editAdjust(transform: (AdjustDraft) -> AdjustDraft) {
        adjust.value = adjust.value?.let(transform)
    }

    fun closeAdjust() {
        adjust.value = null
    }

    fun applyAdjust() {
        // FR-I6 stock adjustments are audit-logged and privileged — enforce, don't just hide.
        val draft = adjust.value?.takeIf { it.canApply && canAdjust.value } ?: return
        viewModelScope.launch {
            val staffId = session.currentStaffId() ?: return@launch
            inventory.adjustStock(draft.toAdjustment(staffId, minorUnits.value))
            adjust.value = null
        }
    }

    companion object {
        val BASE_UNITS = BaseUnit.entries
        val ADJUST_REASONS = listOf(
            StockReason.PURCHASE,
            StockReason.WASTE,
            StockReason.SPOILAGE,
            StockReason.STAFF_MEAL,
            StockReason.COUNT_CORRECTION,
        )
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
