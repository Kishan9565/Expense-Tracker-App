package com.kishan9565.expensetracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentMethod { CASH, UPI, CARD, WALLET, OTHER }
enum class TxSource { MANUAL, VOICE, UPI, CSV, RECURRING }

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class, 
            parentColumns = ["id"],
            childColumns = ["categoryId"], 
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("localDate"), 
        Index("categoryId"), 
        Index(value = ["type", "localDate"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountPaise: Long,            // always > 0
    val categoryId: Long,
    val payment: PaymentMethod,
    val note: String = "",
    val occurredAtMillis: Long,       // exact instant
    val localDate: Long,              // epoch day in the user's zone at entry time
    val source: TxSource = TxSource.MANUAL,
    val payeeId: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
