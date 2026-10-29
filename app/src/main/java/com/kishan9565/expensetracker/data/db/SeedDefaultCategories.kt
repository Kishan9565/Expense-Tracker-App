package com.kishan9565.expensetracker.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class SeedDefaultCategories : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL("""
            INSERT INTO categories (name, type, sortOrder, archived, iconKey, keywords) VALUES
            ('Food', 'EXPENSE', 1, 0, 'food', 'chai,tea,coffee,lunch,dinner,breakfast,khana,snacks,swiggy,zomato'),
            ('Transport', 'EXPENSE', 2, 0, 'transport', 'auto,bus,metro,cab,uber,ola,petrol,fuel,train'),
            ('Shopping', 'EXPENSE', 3, 0, 'shopping', 'amazon,flipkart,clothes,shopping'),
            ('Bills', 'EXPENSE', 4, 0, 'bills', 'recharge,electricity,bijli,wifi,rent,mobile'),
            ('Other', 'EXPENSE', 99, 0, 'other', ''),
            
            ('Salary', 'INCOME', 1, 0, 'other', 'salary,stipend,paid'),
            ('Cashback', 'INCOME', 2, 0, 'other', 'cashback,refund,reward'),
            ('Other', 'INCOME', 99, 0, 'other', '')
        """)
    }
}
