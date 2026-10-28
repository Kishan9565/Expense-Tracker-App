package com.kishan9565.expensetracker.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionWithCategoryTest {

    @Test
    fun displayTitle_usesNote_ifNotBlank() {
        val tx = TransactionEntity(type = TxType.EXPENSE, amountPaise = 1000, categoryId = 1, payment = PaymentMethod.CASH, note = "Coffee", occurredAtMillis = 0, localDate = 0, createdAtMillis = 0, updatedAtMillis = 0)
        val cat = CategoryEntity(id = 1, name = "Food", type = TxType.EXPENSE, iconKey = "food")
        
        val withCat = TransactionWithCategory(tx, cat)
        assertEquals("Coffee", withCat.displayTitle())
    }

    @Test
    fun displayTitle_usesCategory_ifNoteIsBlank() {
        val tx = TransactionEntity(type = TxType.EXPENSE, amountPaise = 1000, categoryId = 1, payment = PaymentMethod.CASH, note = "   ", occurredAtMillis = 0, localDate = 0, createdAtMillis = 0, updatedAtMillis = 0)
        val cat = CategoryEntity(id = 1, name = "Food", type = TxType.EXPENSE, iconKey = "food")
        
        val withCat = TransactionWithCategory(tx, cat)
        assertEquals("Food", withCat.displayTitle())
    }

    @Test
    fun displayTitle_usesFallback_ifCategoryAndNoteAreMissing() {
        val tx = TransactionEntity(type = TxType.EXPENSE, amountPaise = 1000, categoryId = 1, payment = PaymentMethod.CASH, note = "", occurredAtMillis = 0, localDate = 0, createdAtMillis = 0, updatedAtMillis = 0)
        
        val withCat = TransactionWithCategory(tx, null)
        assertEquals("Expense", withCat.displayTitle())
    }
}