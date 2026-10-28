package com.kishan9565.expensetracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.time.TimePeriod
import com.kishan9565.expensetracker.core.time.adjust
import com.kishan9565.expensetracker.core.time.bounds
import com.kishan9565.expensetracker.data.db.DayTotal
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeState(
    val selectedPeriod: TimePeriod = TimePeriod.MONTH,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val spentThisPeriod: Long = 0L,
    val spentLastPeriod: Long = 0L,
    val incomeThisPeriod: Long = 0L,
    val incomeLastPeriod: Long = 0L,
    val currentPeriodDaily: List<DayTotal> = emptyList(),
    val previousPeriodDaily: List<DayTotal> = emptyList(),
    val recentTransactions: List<TransactionWithCategory> = emptyList()
) {
    val balance: Long get() = incomeThisPeriod - spentThisPeriod
}

sealed interface HomeAction {
    data class ChangePeriod(val period: TimePeriod) : HomeAction
    data object PreviousPeriod : HomeAction
    data object NextPeriod : HomeAction
    data object ResetToCurrentPeriod : HomeAction
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TransactionRepository,
    clock: AppClock
) : ViewModel() {

    private val today = clock.today()
    private val _period = MutableStateFlow(TimePeriod.MONTH)
    private val _date = MutableStateFlow(today)

    val state: StateFlow<HomeState> = combine(_period, _date) { p, d -> p to d }
        .flatMapLatest { (period, date) ->
            val bounds = period.bounds(date)
            val firstDay = bounds.first
            val lastDay = bounds.second
            
            val prevDate = period.adjust(date, -1)
            val prevBounds = period.bounds(prevDate)
            val prevFirstDay = prevBounds.first
            val prevLastDay = prevBounds.second

            combine(
                combine(
                    repo.observeTotal(TxType.EXPENSE, firstDay, lastDay),
                    repo.observeTotal(TxType.EXPENSE, prevFirstDay, prevLastDay),
                    repo.observeTotal(TxType.INCOME, firstDay, lastDay),
                    ::Triple
                ),
                combine(
                    repo.observeTotal(TxType.INCOME, prevFirstDay, prevLastDay),
                    repo.observeDailyExpense(firstDay, lastDay),
                    repo.observeDailyExpense(prevFirstDay, prevLastDay),
                    ::Triple
                ),
                repo.observeBetween(firstDay, lastDay)
            ) { (spentThis, spentLast, incomeThis), (incomeLast, currentDaily, prevDaily), recents ->
                HomeState(
                    selectedPeriod = period,
                    selectedDate = date,
                    today = today,
                    spentThisPeriod = spentThis,
                    spentLastPeriod = spentLast,
                    incomeThisPeriod = incomeThis,
                    incomeLastPeriod = incomeLast,
                    currentPeriodDaily = currentDaily,
                    previousPeriodDaily = prevDaily,
                    recentTransactions = recents.take(5) // Just taking top 5 for recents
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeState(selectedDate = today, today = today)
        )

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.ChangePeriod -> {
                _period.value = action.period
                // Snap to current bounds based on today when switching
                _date.value = today
            }
            HomeAction.PreviousPeriod -> _date.update { _period.value.adjust(it, -1) }
            HomeAction.NextPeriod -> _date.update { _period.value.adjust(it, 1) }
            HomeAction.ResetToCurrentPeriod -> _date.update { today }
        }
    }
}
