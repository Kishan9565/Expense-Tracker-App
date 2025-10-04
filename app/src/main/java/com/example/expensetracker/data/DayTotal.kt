package com.example.expensetracker.data

import androidx.room.ColumnInfo

data class DayTotal(
    @ColumnInfo(name = "day") val dayTimestamp: Long, // yaha name match karo query se
    @ColumnInfo(name = "totalAmount") val totalAmount: Double
)