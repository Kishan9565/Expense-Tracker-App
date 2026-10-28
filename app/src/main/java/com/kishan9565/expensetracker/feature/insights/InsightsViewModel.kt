package com.kishan9565.expensetracker.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.time.epochRange
import com.kishan9565.expensetracker.domain.repository.CategoryRepository
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class CategorySummary(
    val categoryId: Long,
    val name: String,
    val amountPaise: Long,
    val percentage: Float
)

data class InsightsState(
    val selectedMonth: YearMonth,
    val totalSpend: Long = 0L,
    val categories: List<CategorySummary> = emptyList()
)

sealed interface InsightsAction {
    data object PreviousMonth : InsightsAction
    data object NextMonth : InsightsAction
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val txRepo: TransactionRepository,
    private val catRepo: CategoryRepository,
    clock: AppClock
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.from(clock.today()))

    val state: StateFlow<InsightsState> = _month.flatMapLatest { month ->
        val firstDay = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        
        combine(
            txRepo.observeCategoryTotals(firstDay, lastDay),
            catRepo.observeAll()
        ) { totals, categories ->
            val catMap = categories.associateBy { it.id }
            val totalSpend = totals.sumOf { it.totalPaise }
            
            val summaries = totals.map { total ->
                val pct = if (totalSpend > 0) (total.totalPaise.toFloat() / totalSpend.toFloat()) else 0f
                CategorySummary(
                    categoryId = total.categoryId,
                    name = catMap[total.categoryId]?.name ?: "Unknown",
                    amountPaise = total.totalPaise,
                    percentage = pct
                )
            }.sortedByDescending { it.amountPaise }
            
            InsightsState(
                selectedMonth = month,
                totalSpend = totalSpend,
                categories = summaries
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsState(selectedMonth = _month.value)
    )

    fun onAction(action: InsightsAction) {
        when (action) {
            InsightsAction.PreviousMonth -> _month.update { it.minusMonths(1) }
            InsightsAction.NextMonth -> _month.update { it.plusMonths(1) }
        }
    }
}
