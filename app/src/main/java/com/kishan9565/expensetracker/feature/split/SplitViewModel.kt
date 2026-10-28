package com.kishan9565.expensetracker.feature.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.data.db.SplitGroupEntity
import com.kishan9565.expensetracker.data.db.SplitMemberEntity
import com.kishan9565.expensetracker.domain.repository.SplitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitState(
    val groups: List<SplitGroupEntity> = emptyList()
)

sealed interface SplitAction {
    data class CreateGroup(val name: String) : SplitAction
}

sealed interface SplitEvent {
    data class Snack(val message: String) : SplitEvent
}

@HiltViewModel
class SplitViewModel @Inject constructor(
    private val splitRepo: SplitRepository,
    private val clock: AppClock
) : ViewModel() {
    
    val state: StateFlow<SplitState> = splitRepo.observeActiveGroups().map { SplitState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SplitState())
        
    private val _events = Channel<SplitEvent>()
    val events = _events.receiveAsFlow()
    
    fun onAction(action: SplitAction) {
        when (action) {
            is SplitAction.CreateGroup -> {
                viewModelScope.launch {
                    val group = SplitGroupEntity(
                        name = action.name,
                        createdAtMillis = clock.now().toEpochMilli()
                    )
                    // Creating an initial self member automatically for MVP simplicity
                    val self = SplitMemberEntity(
                        groupId = 0, // Gets overwritten inside repository insertGroup
                        name = "You",
                        nameKey = "you",
                        isSelf = true
                    )
                    splitRepo.createGroup(group, listOf(self))
                    _events.send(SplitEvent.Snack("Group created"))
                }
            }
        }
    }
}
