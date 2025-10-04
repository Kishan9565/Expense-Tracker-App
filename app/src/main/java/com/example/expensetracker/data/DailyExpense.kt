package com.example.expensetracker.data

import androidx.room.ColumnInfo

data class DailyExpense(
    @ColumnInfo(name = "dayTimestamp") val dayTimestamp: Long,
    @ColumnInfo(name = "totalAmount") val totalAmount: Double
)
