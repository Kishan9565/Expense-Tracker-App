package com.kishan9565.expensetracker.domain.repository

import com.kishan9565.expensetracker.data.db.CategoryTotal
import com.kishan9565.expensetracker.data.db.DayTotal
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.NewTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    suspend fun getById(id: Long): TransactionEntity?
    suspend fun add(tx: NewTransaction)
    suspend fun update(tx: TransactionEntity)
    suspend fun delete(tx: TransactionEntity)
    suspend fun deleteById(id: Long)
    suspend fun restore(tx: TransactionEntity)
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionWithCategory>>
    fun observeTotal(type: TxType, from: LocalDate, to: LocalDate): Flow<Long>
    fun observeDailyExpense(from: LocalDate, to: LocalDate): Flow<List<DayTotal>>
    fun observeDailyIncome(from: LocalDate, to: LocalDate): Flow<List<DayTotal>>
    fun observeCategoryTotals(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>>
}
