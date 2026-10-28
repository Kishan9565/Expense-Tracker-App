package com.kishan9565.expensetracker.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
) {
    fun displayTitle(): String {
        return transaction.note.ifBlank {
            category?.name ?: if (transaction.type == TxType.EXPENSE) "Expense" else "Income"
        }
    }
}
