package com.kishan9565.expensetracker.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.domain.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class RecurringState(val rules: List<String> = emptyList()) // Mock state to fix compilation

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repo: RecurringRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RecurringState())
    val state = _state.asStateFlow()
}
