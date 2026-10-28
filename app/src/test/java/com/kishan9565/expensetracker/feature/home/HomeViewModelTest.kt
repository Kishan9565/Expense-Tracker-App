package com.kishan9565.expensetracker.feature.home

import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.data.db.CategoryTotal
import com.kishan9565.expensetracker.data.db.DayTotal
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.NewTransaction
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun balance_is_income_minus_expense() = runTest {
        val repo = FakeHomeTransactionRepository()
        val vm = HomeViewModel(repo, FixedClock(LocalDate.of(2026, 10, 25)))
        
        testScheduler.advanceUntilIdle()
        
        val state = vm.state.value
        assertEquals(30000L, state.incomeThisPeriod)
        assertEquals(10000L, state.spentThisPeriod)
        assertEquals(20000L, state.balance)
    }
}

class FakeHomeTransactionRepository : TransactionRepository {
    override suspend fun getById(id: Long): TransactionEntity? = null
    override suspend fun add(tx: NewTransaction) {}
    override suspend fun update(tx: TransactionEntity) {}
    override suspend fun delete(tx: TransactionEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun restore(tx: TransactionEntity) {}
    
    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionWithCategory>> = emptyFlow()
    
    override fun observeTotal(type: TxType, from: LocalDate, to: LocalDate): Flow<Long> {
        return when (type) {
            TxType.INCOME -> flowOf(30000L)
            TxType.EXPENSE -> flowOf(10000L)
        }
    }
    
    override fun observeDailyExpense(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = emptyFlow()
    override fun observeDailyIncome(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = emptyFlow()
    override fun observeCategoryTotals(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>> = emptyFlow()
}

class FixedClock(private val d: LocalDate) : AppClock {
    override fun today(): LocalDate = d
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
