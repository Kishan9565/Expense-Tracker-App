package com.kishan9565.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val iconKey: String,
    val keywords: String = "",
    val sortOrder: Int = 0,
    val archived: Boolean = false
)

enum class TxType { EXPENSE, INCOME }
