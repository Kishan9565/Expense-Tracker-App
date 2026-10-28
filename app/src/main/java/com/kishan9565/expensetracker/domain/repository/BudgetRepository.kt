package com.kishan9565.expensetracker.domain.repository

import com.kishan9565.expensetracker.data.db.BudgetEntity
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(): Flow<List<BudgetEntity>>
    suspend fun save(budget: BudgetEntity)
    suspend fun delete(categoryId: Long)
}