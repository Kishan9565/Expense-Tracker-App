package com.kishan9565.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val sortOrder: Int,
    val archived: Boolean = false,
    val iconKey: String = "other",
    val keywords: String = ""
)
