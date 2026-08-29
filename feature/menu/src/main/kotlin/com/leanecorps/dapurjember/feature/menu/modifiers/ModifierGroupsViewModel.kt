package com.leanecorps.dapurjember.feature.menu.modifiers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ModifierGroupsViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    storeProfiles: StoreProfileRepository,
) : ViewModel() {

    private val minorUnits = MutableStateFlow(0)
    private val editor = MutableStateFlow<ModifierGroupDraft?>(null)

    private val groupsWithModifiers = menuRepository.observeModifierGroups().flatMapLatest { groups ->
        if (groups.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(groups.map { menuRepository.observeModifierGroup(it.id) }) { detail ->
                detail.filterNotNull().toList()
            }
        }
    }

    val uiState: StateFlow<ModifierGroupsUiState> = combine(
        groupsWithModifiers,
        editor,
        minorUnits,
    ) { groups, editorState, scale ->
        ModifierGroupsUiState(
            loading = false,
            currencyMinorUnits = scale,
            groups = groups.map { it.toRowUi() },
            editor = editorState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ModifierGroupsUiState())

    init {
        viewModelScope.launch { minorUnits.value = storeProfiles.getProfile()?.currencyMinorUnits ?: 0 }
    }

    /** The in-progress draft, exposed for tests; the screen reads it from [uiState]. */
    val currentEditor: ModifierGroupDraft? get() = editor.value

    fun startAdd() {
        editor.value = ModifierGroupDraft()
    }

    fun startEdit(groupId: String) {
        viewModelScope.launch {
            editor.value = menuRepository.observeModifierGroup(groupId).first()?.toDraft(minorUnits.value)
        }
    }

    fun closeEditor() {
        editor.value = null
    }

    fun edit(transform: (ModifierGroupDraft) -> ModifierGroupDraft) {
        editor.value = editor.value?.let(transform)
    }

    fun addModifier() = edit { it.copy(modifiers = it.modifiers + ModifierRowDraft()) }

    fun removeModifier(id: String) = edit { current ->
        if (current.modifiers.size <= 1) {
            current
        } else {
            current.copy(modifiers = current.modifiers.filterNot { it.id == id })
        }
    }

    fun editModifier(id: String, transform: (ModifierRowDraft) -> ModifierRowDraft) = edit { current ->
        current.copy(modifiers = current.modifiers.map { if (it.id == id) transform(it) else it })
    }

    fun save() {
        val draft = editor.value ?: return
        if (!draft.canSave) return
        viewModelScope.launch {
            val id = draft.id ?: UuidV7.generate()
            menuRepository.saveModifierGroup(draft.toGroup(id), draft.toModifiers(id, minorUnits.value))
            editor.value = null
        }
    }

    fun delete(groupId: String) {
        viewModelScope.launch { menuRepository.softDeleteModifierGroup(groupId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
