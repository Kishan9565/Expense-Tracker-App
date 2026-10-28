package com.kishan9565.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeDao {
    @Query("SELECT * FROM payees ORDER BY lastUsedMillis DESC")
    fun observeAll(): Flow<List<PayeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payee: PayeeEntity): Long
}
