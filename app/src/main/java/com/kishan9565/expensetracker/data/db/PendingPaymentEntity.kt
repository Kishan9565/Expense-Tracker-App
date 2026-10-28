package com.kishan9565.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_payments")
data class PendingPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payeeId: Long?, 
    val vpa: String, 
    val amountPaise: Long,
    val categoryId: Long?, 
    val note: String, 
    val startedAtMillis: Long,
)
