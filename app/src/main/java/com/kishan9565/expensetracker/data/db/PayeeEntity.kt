package com.kishan9565.expensetracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "payees", indices = [Index(value = ["vpa"], unique = true)])
data class PayeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val vpa: String,
    val lastUsedMillis: Long = 0,
)
