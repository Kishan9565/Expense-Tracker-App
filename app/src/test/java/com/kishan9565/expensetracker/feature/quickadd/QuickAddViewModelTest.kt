package com.kishan9565.expensetracker.feature.quickadd

import com.kishan9565.expensetracker.core.money.AmountError
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.data.db.CategoryEntity
import com.kishan9565.expensetracker.data.db.CategoryTotal
import com.kishan9565.expensetracker.data.db.DayTotal
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionWithCategory
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.NewTransaction
import com.kishan9565.expensetracker.domain.repository.CategoryRepository
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import com.kishan9565.expensetracker.domain.repository.UserPrefs
import com.kishan9565.expensetracker.domain.repository.UserPrefsRepository
import com.kishan9565.expensetracker.core.designsystem.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

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
    fun zero_amount_shows_error_and_saves_nothing() = runTest {
        val repo = FakeTransactionRepository()
        val vm = QuickAddViewModel(repo, FakeCategoryRepository(), FakeUserPrefs(), FixedClock(LocalDate.of(2026, 10, 25)))
        
        testScheduler.advanceUntilIdle() // Ensure initial blank state is applied
        
        vm.onAction(QuickAddAction.OnKey('0'))
        vm.onAction(QuickAddAction.OnSave)
        
        testScheduler.advanceUntilIdle()
        
        assertEquals(AmountError.ZERO, vm.state.value.amountError)
        assertTrue(repo.saved.isEmpty())
        assertFalse(vm.state.value.isSaving)
        assertEquals("0", vm.state.value.amountText)
    }

    @Test
    fun save_success_resets_form_state() = runTest {
        val repo = FakeTransactionRepository()
        val vm = QuickAddViewModel(repo, FakeCategoryRepository(), FakeUserPrefs(), FixedClock(LocalDate.of(2026, 10, 25)))
        
        testScheduler.advanceUntilIdle() // Ensure initial blank state is applied
        
        vm.onAction(QuickAddAction.OnCategory(1L))
        vm.onAction(QuickAddAction.OnKey('1'))
        vm.onAction(QuickAddAction.OnKey('0'))
        vm.onAction(QuickAddAction.OnKey('0'))
        vm.onAction(QuickAddAction.OnNote("Test note"))
        
        vm.onAction(QuickAddAction.OnSave)
        
        testScheduler.advanceUntilIdle()
        
        assertTrue(repo.saved.isNotEmpty())
        assertEquals(1, repo.saved.size)
        
        // Assert that state was reset
        assertFalse(vm.state.value.isSaving)
        assertEquals("", vm.state.value.amountText)
        assertEquals("", vm.state.value.note)
        assertNull(vm.state.value.amountError)
    }
}

class FakeTransactionRepository : TransactionRepository {
    val saved = mutableListOf<NewTransaction>()
    override suspend fun getById(id: Long): TransactionEntity? = null
    override suspend fun add(tx: NewTransaction) { saved.add(tx) }
    override suspend fun update(tx: TransactionEntity) {}
    override suspend fun delete(tx: TransactionEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun restore(tx: TransactionEntity) {}
    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionWithCategory>> = emptyFlow()
    override fun observeTotal(type: TxType, from: LocalDate, to: LocalDate): Flow<Long> = emptyFlow()
    override fun observeDailyExpense(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = emptyFlow()
    override fun observeDailyIncome(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = emptyFlow()
    override fun observeCategoryTotals(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>> = emptyFlow()
}

class FakeCategoryRepository : CategoryRepository {
    override fun observeAll(): Flow<List<CategoryEntity>> = emptyFlow()
    override suspend fun add(category: CategoryEntity) {}
    override suspend fun archive(id: Long) {}
}

class FakeUserPrefs : UserPrefsRepository {
    override val lastUsed = MutableStateFlow(UserPrefs(null, PaymentMethod.UPI, ThemeMode.DARK))
    override suspend fun saveLastUsed(categoryId: Long, payment: PaymentMethod) {}
    override suspend fun saveTheme(themeMode: ThemeMode) {}
}

class FixedClock(private val d: LocalDate) : AppClock {
    override fun today(): LocalDate = d
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
    override fun todayFlow(): Flow<LocalDate> = flowOf(d)
}
