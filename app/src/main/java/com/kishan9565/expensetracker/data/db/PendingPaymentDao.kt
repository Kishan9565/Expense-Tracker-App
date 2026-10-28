package com.kishan9565.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPaymentDao {
    @Query("SELECT * FROM pending_payments")
    fun observeAll(): Flow<List<PendingPaymentEntity>>

    @Insert
    suspend fun insert(payment: PendingPaymentEntity)

    @Delete
    suspend fun delete(payment: PendingPaymentEntity)
}
