package com.example.expensetracker.viewmodel

import androidx.lifecycle.*
import com.example.expensetracker.data.DailyExpense
import com.example.expensetracker.data.DayTotal
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.DummyDataProvider
import com.example.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {


    private var currentTransactionSource: LiveData<List<Transaction>>? = null
    private var currentDailyExpenseSource: LiveData<List<DayTotal>>? = null


    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> get() = _transactions

    private val _totalBalance = MutableLiveData<Double>()
    val totalBalance: LiveData<Double> get() = _totalBalance

    private val _selectedMonthRange = MutableLiveData<Pair<Long, Long>>()
    val selectedMonthRange: LiveData<Pair<Long, Long>> get() = _selectedMonthRange

    val transactionsForMonth = MediatorLiveData<List<Transaction>>()
    val dailyExpensesForChart = MediatorLiveData<List<DailyExpense>>()
    val allDailyExpenses: LiveData<List<DailyExpense>> = repository.dailyExpenses

    init {
        _selectedMonthRange.observeForever { range ->
            loadTransactionsForMonth(range.first, range.second)
            loadDailyExpensesForChart(range.first, range.second)
        }

        dailyExpensesForChart.addSource(allDailyExpenses) { realData ->
            dailyExpensesForChart.value = mergeDummyWithReal(realData)
        }

        val now = Calendar.getInstance()
        setSelectedMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
    }

    fun setSelectedMonth(year: Int, monthZeroBased: Int) {
        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendarStart.timeInMillis

        val calendarEnd = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val end = calendarEnd.timeInMillis

        _selectedMonthRange.value = start to end
    }

    fun getCurrentWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return start to end
    }


    private fun loadTransactionsForMonth(start: Long, end: Long) {
        val newSource = repository.getTransactionsForRange(start, end)

        currentTransactionSource?.let { transactionsForMonth.removeSource(it) }

        currentTransactionSource = newSource

        transactionsForMonth.addSource(newSource) { list ->
            transactionsForMonth.value = list
        }
    }


    private fun loadDailyExpensesForChart(start: Long, end: Long) {
        val newSource = repository.getDailyTotalsForRange(start, end)

        currentDailyExpenseSource?.let { dailyExpensesForChart.removeSource(it) }

        currentDailyExpenseSource = newSource

        dailyExpensesForChart.addSource(newSource) { list ->
            val convertedList = list.map { dayTotal ->
                DailyExpense(
                    dayTimestamp = dayTotal.dayTimestamp,
                    totalAmount = dayTotal.totalAmount
                )
            }
            dailyExpensesForChart.value = mergeDummyWithReal(convertedList)
        }
    }


    fun addTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.insertTransaction(transaction)
        loadTransactions()
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
        loadTransactions()
    }

    private fun mergeDummyWithReal(realData: List<DailyExpense>): List<DailyExpense> {
        val dummyData = DummyDataProvider.getDummyDailyExpenses()
        return when {
            realData.isEmpty() -> dummyData
            realData.size < dummyData.size -> {
                val remainingDummy = dummyData.take(dummyData.size - realData.size)
                remainingDummy + realData
            }
            else -> realData
        }
    }

    fun loadTransactions() = viewModelScope.launch {
        val all = repository.getAllTransactions()
        _transactions.value = all
        calculateBalance()
    }

    private suspend fun calculateBalance() {
        val income = repository.getTotalIncome()
        val expense = repository.getTotalExpense()
        _totalBalance.postValue(income - expense)
    }


    fun getExpenseByDay(start: Long, end: Long): LiveData<Double?> =
        repository.getExpenseByDay(start, end).map { it ?: 0.0 }

    fun getExpenseByWeek(start: Long, end: Long): LiveData<Double?> =
        repository.getExpenseByWeek(start, end).map { it ?: 0.0 }

    fun getExpenseByMonth(start: Long, end: Long): LiveData<Double?> =
        repository.getExpenseByMonth(start, end).map { it ?: 0.0 }


    fun getTransactionsForRange(start: Long, end: Long): LiveData<List<Transaction>> {
        return repository.getTransactionsForRange(start, end)
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAll()
            loadTransactions()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun getMonthList(): List<String> {
        val months = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        for (i in 0..11) {
            val fullMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
            val label = if (fullMonth.length > 6) {
                SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
            } else {
                fullMonth
            }
            months.add(label)
            calendar.add(Calendar.MONTH, 1)
        }

        return months
    }


}

