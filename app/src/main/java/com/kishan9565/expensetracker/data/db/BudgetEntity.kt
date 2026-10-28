package com.kishan9565.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val categoryId: Long,
    val monthlyLimitPaise: Long
)
