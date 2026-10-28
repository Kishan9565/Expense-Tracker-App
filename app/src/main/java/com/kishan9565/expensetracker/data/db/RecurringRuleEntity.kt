package com.kishan9565.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Frequency { WEEKLY, MONTHLY, YEARLY }

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, 
    val amountPaise: Long?, 
    val categoryId: Long, 
    val payment: PaymentMethod,
    val frequency: Frequency, 
    val dayOfMonth: Int?, 
    val nextDueEpochDay: Long, 
    val active: Boolean = true
)
