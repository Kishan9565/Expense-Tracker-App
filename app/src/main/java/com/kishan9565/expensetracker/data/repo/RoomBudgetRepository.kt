package com.kishan9565.expensetracker.data.repo

import com.kishan9565.expensetracker.data.db.BudgetDao
import com.kishan9565.expensetracker.data.db.BudgetEntity
import com.kishan9565.expensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomBudgetRepository @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {
    override fun observeAll(): Flow<List<BudgetEntity>> = dao.observeAll()
    override suspend fun save(budget: BudgetEntity) = dao.insert(budget)
    override suspend fun delete(categoryId: Long) = dao.delete(categoryId)
}