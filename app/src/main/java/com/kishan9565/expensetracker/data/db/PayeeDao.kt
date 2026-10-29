package com.kishan9565.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeDao {
    @Query("SELECT * FROM payees ORDER BY lastUsedMillis DESC")
    fun observeAll(): Flow<List<PayeeEntity>>

    @Query("SELECT * FROM payees WHERE vpa = :vpa LIMIT 1")
    suspend fun findByVpa(vpa: String): PayeeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNew(payee: PayeeEntity): Long

    @Update
    suspend fun update(payee: PayeeEntity)

    @Transaction
    suspend fun upsert(name: String, vpa: String, now: Long): Long {
        val key = vpa.trim().lowercase()
        val existing = findByVpa(key)
        return if (existing != null) {
            update(existing.copy(displayName = name.trim(), lastUsedMillis = now))
            existing.id
        } else {
            insertNew(PayeeEntity(displayName = name.trim(), vpa = key, lastUsedMillis = now))
        }
    }
}
