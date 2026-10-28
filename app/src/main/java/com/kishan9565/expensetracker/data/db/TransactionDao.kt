package com.kishan9565.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DayTotal(val day: Long, val totalPaise: Long)
data class CategoryTotal(val categoryId: Long, val totalPaise: Long)

@Dao
interface TransactionDao {
    @Insert suspend fun insert(t: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE) 
    suspend fun insertWithId(t: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE) 
    suspend fun insertAll(t: List<TransactionEntity>)
    
    @Update suspend fun update(t: TransactionEntity)
    
    @Delete suspend fun delete(t: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Transaction
    @Query("SELECT * FROM transactions WHERE localDate BETWEEN :from AND :to ORDER BY occurredAtMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionWithCategory>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = :type AND localDate BETWEEN :from AND :to")
    fun observeTotal(type: TxType, from: Long, to: Long): Flow<Long>

    @Query("""SELECT localDate AS day, SUM(amountPaise) AS totalPaise FROM transactions
              WHERE type = 'EXPENSE' AND localDate BETWEEN :from AND :to
              GROUP BY localDate ORDER BY localDate""")
    fun observeDailyExpense(from: Long, to: Long): Flow<List<DayTotal>>

    @Query("""SELECT localDate AS day, SUM(amountPaise) AS totalPaise FROM transactions
              WHERE type = 'INCOME' AND localDate BETWEEN :from AND :to
              GROUP BY localDate ORDER BY localDate""")
    fun observeDailyIncome(from: Long, to: Long): Flow<List<DayTotal>>

    @Query("""SELECT categoryId, SUM(amountPaise) AS totalPaise FROM transactions
              WHERE type = 'EXPENSE' AND localDate BETWEEN :from AND :to
              GROUP BY categoryId ORDER BY totalPaise DESC""")
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY occurredAtMillis ASC")
    suspend fun getAllSync(): List<TransactionWithCategory>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
