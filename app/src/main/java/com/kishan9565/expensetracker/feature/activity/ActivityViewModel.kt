package com.kishan9565.expensetracker.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.time.TimePeriod
import com.kishan9565.expensetracker.core.time.adjust
import com.kishan9565.expensetracker.core.time.bounds
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class SortOrder { NEWEST, OLDEST, HIGHEST, LOWEST }

data class ActivityState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val query: String = "",
    val filterType: TxType? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val period: TimePeriod = TimePeriod.MONTH,
    val date: LocalDate
)

sealed interface ActivityAction {
    data class Search(val query: String) : ActivityAction
    data class FilterType(val type: TxType?) : ActivityAction
    data class Sort(val order: SortOrder) : ActivityAction
    data class ChangePeriod(val period: TimePeriod) : ActivityAction
    data object PreviousPeriod : ActivityAction
    data object NextPeriod : ActivityAction
    data class Delete(val tx: TransactionEntity) : ActivityAction
    data class UndoDelete(val tx: TransactionEntity) : ActivityAction
}

sealed interface ActivityEvent {
    data class ShowUndo(val tx: TransactionEntity, val message: String) : ActivityEvent
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val clock: AppClock
) : ViewModel() {

    private val today = clock.today()
    
    private val _query = MutableStateFlow("")
    private val _filterType = MutableStateFlow<TxType?>(null)
    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val _period = MutableStateFlow(TimePeriod.MONTH)
    private val _date = MutableStateFlow(today)

    val state: StateFlow<ActivityState> = combine(
        _query, _filterType, _sortOrder, _period, _date
    ) { q, t, s, p, d ->
        ActivityState(transactions = emptyList(), query = q, filterType = t, sortOrder = s, period = p, date = d)
    }.flatMapLatest { currentState ->
        val bounds = currentState.period.bounds(currentState.date)
        repo.observeBetween(bounds.first, bounds.second).map { txs ->
            var filtered = txs
            if (currentState.query.isNotBlank()) {
                filtered = filtered.filter { 
                    it.transaction.note.contains(currentState.query, ignoreCase = true) ||
                    it.category?.name?.contains(currentState.query, ignoreCase = true) == true
                }
            }
            if (currentState.filterType != null) {
                filtered = filtered.filter { it.transaction.type == currentState.filterType }
            }
            
            val sorted = when (currentState.sortOrder) {
                SortOrder.NEWEST -> filtered.sortedByDescending { it.transaction.occurredAtMillis }
                SortOrder.OLDEST -> filtered.sortedBy { it.transaction.occurredAtMillis }
                SortOrder.HIGHEST -> filtered.sortedByDescending { it.transaction.amountPaise }
                SortOrder.LOWEST -> filtered.sortedBy { it.transaction.amountPaise }
            }
            
            currentState.copy(transactions = sorted)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActivityState(date = today)
    )

    private val _events = Channel<ActivityEvent>()
    val events = _events.receiveAsFlow()
    
    fun onAction(action: ActivityAction) {
        when (action) {
            is ActivityAction.Search -> _query.value = action.query
            is ActivityAction.FilterType -> _filterType.value = action.type
            is ActivityAction.Sort -> _sortOrder.value = action.order
            is ActivityAction.ChangePeriod -> {
                _period.value = action.period
                _date.value = today
            }
            is ActivityAction.PreviousPeriod -> _date.update { _period.value.adjust(it, -1) }
            is ActivityAction.NextPeriod -> _date.update { _period.value.adjust(it, 1) }
            is ActivityAction.Delete -> {
                viewModelScope.launch { 
                    repo.deleteById(action.tx.id)
                    _events.send(ActivityEvent.ShowUndo(action.tx, "Transaction deleted"))
                }
            }
            is ActivityAction.UndoDelete -> {
                viewModelScope.launch { 
                    repo.restore(action.tx) 
                }
            }
        }
    }
}
