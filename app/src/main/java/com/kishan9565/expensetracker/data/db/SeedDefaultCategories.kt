package com.kishan9565.expensetracker.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

val SeedDefaultCategories = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val defaultCategories = listOf(
            "('Food', 'EXPENSE', 'ic_food', '', 0, 1)",
            "('Transport', 'EXPENSE', 'ic_transport', '', 0, 2)",
            "('Shopping', 'EXPENSE', 'ic_shopping', '', 0, 3)",
            "('Bills', 'EXPENSE', 'ic_bills', '', 0, 4)",
            "('Entertainment', 'EXPENSE', 'ic_entertainment', '', 0, 5)",
            "('Health', 'EXPENSE', 'ic_health', '', 0, 6)",
            "('Education', 'EXPENSE', 'ic_education', '', 0, 7)",
            "('Other', 'EXPENSE', 'ic_other', '', 0, 8)",
            "('Salary', 'INCOME', 'ic_salary', '', 0, 9)",
            "('Business', 'INCOME', 'ic_business', '', 0, 10)",
            "('Gift', 'INCOME', 'ic_gift', '', 0, 11)",
            "('Other', 'INCOME', 'ic_other', '', 0, 12)"
        )
        val query = "INSERT INTO categories (name, type, iconKey, keywords, archived, sortOrder) VALUES ${defaultCategories.joinToString(", ")}"
        db.execSQL(query)
    }
}
