package com.kishan9565.expensetracker.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionWithCategoryTest {
    @Test
    fun fallback_to_category_name_when_note_blank() {
        val txWithCat = TransactionWithCategory(
            transaction = TransactionEntity(1L, TxType.EXPENSE, 1000L, 1L, PaymentMethod.UPI, "", 0L, 0L, TxSource.MANUAL, null, 0L, 0L),
            category = CategoryEntity(1L, "Food", TxType.EXPENSE, 1)
        )
        assertEquals("Food", txWithCat.displayTitle())
    }

    @Test
    fun fallback_to_type_name_when_category_null() {
        val txWithCat = TransactionWithCategory(
            transaction = TransactionEntity(1L, TxType.EXPENSE, 1000L, 1L, PaymentMethod.UPI, "", 0L, 0L, TxSource.MANUAL, null, 0L, 0L),
            category = null
        )
        assertEquals("Expense", txWithCat.displayTitle())
    }

    @Test
    fun note_takes_precedence() {
        val txWithCat = TransactionWithCategory(
            transaction = TransactionEntity(1L, TxType.EXPENSE, 1000L, 1L, PaymentMethod.UPI, "Coffee", 0L, 0L, TxSource.MANUAL, null, 0L, 0L),
            category = CategoryEntity(1L, "Food", TxType.EXPENSE, 1)
        )
        assertEquals("Coffee", txWithCat.displayTitle())
    }
}
