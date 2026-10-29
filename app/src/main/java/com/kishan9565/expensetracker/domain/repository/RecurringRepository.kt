package com.kishan9565.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface RecurringRepository {
    // Stubs to satisfy dagger compilation for now
    fun dummy(): Flow<Int> = emptyFlow()
}
