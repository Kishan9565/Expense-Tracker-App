package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "transactions")
class Transaction (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Income" or "Expense"
    val category: String,
    val paymentMethod: String,
    val amount: Double,
    val note: String?,
    val date: Long,
)

