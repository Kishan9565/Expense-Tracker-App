package com.kishan9565.expensetracker.feature.activity

import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.data.db.CategoryTotal
import com.kishan9565.expensetracker.data.db.DayTotal
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.NewTransaction
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun delete_action_removes_transaction_and_emits_undo() = runTest {
        val repo = FakeTransactionRepository()
        val vm = ActivityViewModel(repo, FixedClock(LocalDate.of(2026, 10, 25)))
        
        val tx = TransactionEntity(1L, TxType.EXPENSE, 1000L, 1L, PaymentMethod.UPI, "Coffee", 0L, 0L, createdAtMillis = 0L, updatedAtMillis = 0L)
        
        vm.onAction(ActivityAction.Delete(tx))
        
        testScheduler.advanceUntilIdle()
        
        assertEquals(1, repo.deleted.size)
        assertEquals(tx.id, repo.deleted.first())
    }
    
    @Test
    fun undo_delete_action_restores_transaction() = runTest {
        val repo = FakeTransactionRepository()
        val vm = ActivityViewModel(repo, FixedClock(LocalDate.of(2026, 10, 25)))
        
        val tx = TransactionEntity(1L, TxType.EXPENSE, 1000L, 1L, PaymentMethod.UPI, "Coffee", 0L, 0L, createdAtMillis = 0L, updatedAtMillis = 0L)
        
        vm.onAction(ActivityAction.UndoDelete(tx))
        
        testScheduler.advanceUntilIdle()
        
        assertEquals(1, repo.restored.size)
        assertEquals(tx, repo.restored.first())
    }
}

class FakeTransactionRepository : TransactionRepository {
    val deleted = mutableListOf<Long>()
    val restored = mutableListOf<TransactionEntity>()
    
    override suspend fun getById(id: Long): TransactionEntity? = null
    override suspend fun add(tx: NewTransaction) {}
    override suspend fun update(tx: TransactionEntity) {}
    override suspend fun delete(tx: TransactionEntity) {}
    override suspend fun deleteById(id: Long) { deleted.add(id) }
    override suspend fun restore(tx: TransactionEntity) { restored.add(tx) }
    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionWithCategory>> = emptyFlow()
    override fun observeTotal(type: TxType, from: LocalDate, to: LocalDate): Flow<Long> = emptyFlow()
    override fun observeDailyExpense(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = emptyFlow()
    override fun observeDailyIncome(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = emptyFlow()
    override fun observeCategoryTotals(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>> = emptyFlow()
}

class FixedClock(private val d: LocalDate) : AppClock {
    override fun today(): LocalDate = d
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
