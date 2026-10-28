package com.kishan9565.expensetracker.data.repo

import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.time.epochDay
import com.kishan9565.expensetracker.core.time.toEntryMillis
import com.kishan9565.expensetracker.data.db.CategoryTotal
import com.kishan9565.expensetracker.data.db.DayTotal
import com.kishan9565.expensetracker.data.db.TransactionDao
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.NewTransaction
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val clock: AppClock
) : TransactionRepository {

    override suspend fun getById(id: Long): TransactionEntity? = dao.getById(id)

    override suspend fun add(tx: NewTransaction) {
        val now = clock.now()
        val entity = TransactionEntity(
            type = tx.type,
            amountPaise = tx.amountPaise,
            categoryId = tx.categoryId,
            payment = tx.payment,
            note = tx.note,
            occurredAtMillis = tx.date.toEntryMillis(clock.zone(), now),
            localDate = tx.date.epochDay(),
            source = tx.source,
            payeeId = tx.payeeId,
            createdAtMillis = now.toEpochMilli(),
            updatedAtMillis = now.toEpochMilli()
        )
        dao.insert(entity)
    }

    override suspend fun update(tx: TransactionEntity) {
        dao.update(tx.copy(updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun delete(tx: TransactionEntity) {
        dao.deleteById(tx.id)
    }
    
    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
    
    override suspend fun restore(tx: TransactionEntity) {
        dao.insertWithId(tx)
    }

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionWithCategory>> {
        return dao.observeBetween(from.epochDay(), to.epochDay())
    }

    override fun observeTotal(type: TxType, from: LocalDate, to: LocalDate): Flow<Long> {
        return dao.observeTotal(type, from.epochDay(), to.epochDay())
    }

    override fun observeDailyExpense(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> {
        return dao.observeDailyExpense(from.epochDay(), to.epochDay())
    }

    override fun observeDailyIncome(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> {
        return dao.observeDailyIncome(from.epochDay(), to.epochDay())
    }

    override fun observeCategoryTotals(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>> {
        return dao.observeCategoryTotals(from.epochDay(), to.epochDay())
    }
}
