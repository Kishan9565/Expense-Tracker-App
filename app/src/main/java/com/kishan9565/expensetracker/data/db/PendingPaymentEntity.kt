package com.kishan9565.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_payments")
data class PendingPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payeeId: Long,
    val vpa: String,
    val amountPaise: Long,
    val categoryId: Long,
    val note: String,
    val startedAtMillis: Long
)

@Dao
interface PendingDao {
    @Query("SELECT COUNT(*) FROM pending_payments")
    suspend fun countOpen(): Int
    
    @Query("SELECT * FROM pending_payments ORDER BY startedAtMillis ASC")
    fun observeAll(): Flow<List<PendingPaymentEntity>>
    
    @Insert
    suspend fun insert(pending: PendingPaymentEntity): Long
    
    @Delete
    suspend fun delete(pending: PendingPaymentEntity)
}
