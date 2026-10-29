package com.kishan9565.expensetracker.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class TxType { INCOME, EXPENSE }
enum class PaymentMethod { CASH, UPI, CARD, WALLET, OTHER }
enum class TxSource { MANUAL, VOICE, SHARE, RECURRING, UPI }

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
        Index(value = ["type", "localDate"]),
        Index("categoryId"),
        Index("localDate")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountPaise: Long,
    val categoryId: Long,
    val payment: PaymentMethod,
    val note: String,
    val occurredAtMillis: Long,
    val localDate: Long,
    val source: TxSource,
    val payeeId: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
) {
    fun displayTitle(): String {
        if (transaction.note.isNotBlank()) return transaction.note
        if (category != null) return category.name
        return if (transaction.type == TxType.INCOME) "Income" else "Expense"
    }
}
