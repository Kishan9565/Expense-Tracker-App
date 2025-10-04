package com.example.expensetracker.repository

import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import com.example.expensetracker.data.DailyExpense
import com.example.expensetracker.data.DayTotal
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.TransactionDao


class TransactionRepository(private val dao: TransactionDao) {


    suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
    }

    val dailyExpenses: LiveData<List<DailyExpense>> = dao.getDailyExpenses()


    suspend fun getAllTransactions() = dao.getAllTransactions()


    suspend fun getTotalIncome() = dao.getTotalIncome() ?: 0.0


    suspend fun getTotalExpense() = dao.getTotalExpense() ?: 0.0

    fun getDailyTotalsForRange(start: Long, end: Long): LiveData<List<DayTotal>> =
        dao.getDailyTotalsForRange(start, end)

    fun getTransactionsForRange(start: Long, end: Long): LiveData<List<Transaction>> =
        dao.getTransactionsForRange(start, end)


    fun getExpenseByDay(start: Long, end: Long) = dao.getExpenseByDay(start, end)
    fun getExpenseByWeek(start: Long, end: Long) = dao.getExpenseByWeek(start, end)
    fun getExpenseByMonth(start: Long, end: Long) = dao.getExpenseByMonth(start, end)


    suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
