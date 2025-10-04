package com.example.expensetracker.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import androidx.room.*

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE type='Income'")
    suspend fun getTotalIncome(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type='Expense'")
    suspend fun getTotalExpense(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type='Expense' AND date BETWEEN :start AND :end")
    fun getExpenseByDay(start: Long, end: Long): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type='Expense' AND date BETWEEN :start AND :end")
    fun getExpenseByWeek(start: Long, end: Long): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type='Expense' AND date BETWEEN :start AND :end")
    fun getExpenseByMonth(start: Long, end: Long): LiveData<Double?>


    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("""
    SELECT date AS dayTimestamp, SUM(amount) AS totalAmount
    FROM transactions
    WHERE type = 'Expense'
    GROUP BY date
    ORDER BY date ASC
""")
    fun getDailyExpenses(): LiveData<List<DailyExpense>>



    @Query("""
    SELECT CAST(strftime('%d', date / 1000, 'unixepoch') AS INTEGER) AS day,
           SUM(amount) AS totalAmount
    FROM transactions
    WHERE type = 'Expense' 
      AND date BETWEEN :start AND :end
    GROUP BY day
    ORDER BY day ASC
""")
    fun getDailyTotalsForRange(start: Long, end: Long): LiveData<List<DayTotal>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getTransactionsForRange(start: Long, end: Long): LiveData<List<Transaction>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()


}
