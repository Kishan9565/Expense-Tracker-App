package com.kishan9565.expensetracker.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class BudgetsState(val budgets: List<String> = emptyList()) // Mock state to fix compilation

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repo: BudgetRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetsState())
    val state = _state.asStateFlow()
}
